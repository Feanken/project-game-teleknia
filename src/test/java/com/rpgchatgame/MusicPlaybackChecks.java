package com.rpgchatgame;

import com.rpgchatgame.service.AudioService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.*;

/** Desktop codec check: actually decode/play each supplied MP3 at zero volume. */
public final class MusicPlaybackChecks {
    private static Throwable failure;
    public static void main(String[] args) { Application.launch(PlayerCheck.class, args); if (failure != null) throw new AssertionError("MP3 playback failed", failure); }
    public static final class PlayerCheck extends Application {
    private final Iterator<String> songs = new TreeSet<>(AudioService.MUSIC_FILES.values()).iterator();
    private MediaPlayer player;
    private PauseTransition timeout;
    private int count;

    @Override public void start(Stage ignored) { Platform.setImplicitExit(false); next(); }
    private void fail(Throwable error) { failure = error; if (timeout != null) timeout.stop(); if (player != null) player.dispose(); Platform.exit(); }
    private void next() {
        if (!songs.hasNext()) { System.out.println("PASS / " + count + " supplied MP3 files decoded and played (muted)."); Platform.exit(); return; }
        String file = songs.next();
        try {
            var resource = getClass().getResource("/audio_frieren/" + file);
            player = new MediaPlayer(new Media(resource.toExternalForm()));
            player.setVolume(0);
            timeout = new PauseTransition(Duration.seconds(30));
            timeout.setOnFinished(e -> fail(new AssertionError("Playback timed out: " + file)));
            timeout.play();
            player.setOnError(() -> fail(player.getError()));
            player.setOnReady(() -> {
                if (player.getTotalDuration().toSeconds() <= 0) { fail(new AssertionError("No duration: " + file)); return; }
                player.play();
            });
            player.setOnPlaying(() -> {
                System.out.println("OK / " + file + " / " + Math.round(player.getTotalDuration().toSeconds()) + "s");
                timeout.stop(); player.dispose(); count++; Platform.runLater(this::next);
            });
        } catch (Throwable error) { fail(error); }
    }
}

}
