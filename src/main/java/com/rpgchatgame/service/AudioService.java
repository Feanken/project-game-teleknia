package com.rpgchatgame.service;

import com.rpgchatgame.util.Constants;
import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/** Streaming MP3 music on JavaFX; short WAV effects and settings IO on a worker. */
public final class AudioService implements AutoCloseable {
    public enum Sound {
        CLICK("click"), ACTION("action"), DAMAGE("damage"), POTION("potion"),
        REWARD("reward"), LEVEL_UP("level_up"), DISCOVERY("discovery"), QUEST_COMPLETE("quest_complete");
        final String file;
        Sound(String file) { this.file = file; }
    }
    public static final java.util.Set<String> MUSIC_TRACKS = java.util.Set.of(
        "embers", "ashen", "journey", "frostveil", "drowned",
        "battle_ashen", "battle_emberwood", "battle_frostveil", "battle_drowned",
        "boss_wolf", "boss_guardian", "boss_warden", "boss_rime_sentinel", "boss_drowned_captain");

    /** Pure selector shared by playback and checks; encounters always take priority. */
    public static String sceneTrack(String region, String enemyId) {
        if (enemyId != null && MUSIC_TRACKS.contains("boss_" + enemyId)) return "boss_" + enemyId;
        String zone = switch (region == null ? "" : region) {
            case "Ashen Reach" -> "ashen";
            case "Frostveil" -> "frostveil";
            case "Drowned Coast" -> "drowned";
            default -> "emberwood";
        };
        if (enemyId != null) return "battle_" + zone;
        return zone.equals("emberwood") ? "journey" : zone;
    }
    public void playScene(String region, String enemyId) { playTrack(sceneTrack(region, enemyId)); }
    public static final Map<String, String> MUSIC_FILES = Map.ofEntries(
        Map.entry("embers", "Journey of a Lifetime - Frieren Beyond Journey's End OST Disc 1 - 1.mp3"),
        Map.entry("ashen", "The End of One Journey - Frieren Beyond Journey's End OST Disc 1 - 2.mp3"),
        Map.entry("journey", "Departures - Frieren Beyond Journey's End OST Disc 1 - 8.mp3"),
        Map.entry("frostveil", "For 1000 Years - Frieren Beyond Journey's End OST Disc 1 - 5.mp3"),
        Map.entry("drowned", "Time Flows Ever Onward - Frieren Beyond Journey's End OST Disc 1 - 9.mp3"),
        Map.entry("battle_ashen", "One Last Adventure - Frieren Beyond Journey's End OST Disc 1 - 6.mp3"),
        Map.entry("battle_emberwood", "The Party of Heroes Returns - Frieren Beyond Journey's End OST Disc 1 - 3.mp3"),
        Map.entry("battle_frostveil", "Farewell, My Friend - Frieren Beyond Journey's End OST Disc 1 - 7.mp3"),
        Map.entry("battle_drowned", "A Well Earned Celebration - Frieren Beyond Journey's End OST disc 1 - 4.mp3"),
        Map.entry("boss_wolf", "One Last Adventure - Frieren Beyond Journey's End OST Disc 1 - 6.mp3"),
        Map.entry("boss_guardian", "Heiter's Request - Frieren Beyond Journey's End OST Disc 1 - 10.mp3"),
        Map.entry("boss_warden", "The Party of Heroes Returns - Frieren Beyond Journey's End OST Disc 1 - 3.mp3"),
        Map.entry("boss_rime_sentinel", "Farewell, My Friend - Frieren Beyond Journey's End OST Disc 1 - 7.mp3"),
        Map.entry("boss_drowned_captain", "A Well Earned Celebration - Frieren Beyond Journey's End OST disc 1 - 4.mp3")
    );
    public static java.net.URL musicResource(String track) {
        String file = MUSIC_FILES.get(track);
        return file == null ? null : AudioService.class.getResource("/audio_frieren/" + file);
    }
    private static final System.Logger LOG = System.getLogger(AudioService.class.getName());
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "teleknia-audio");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<String, Clip> clips = new HashMap<>();
    private final Path settingsFile = Path.of(Constants.DATABASE_DIR, "audio.properties");
    private volatile double musicVolume = 0.30;
    private volatile double effectsVolume = 0.60;
    private volatile boolean muted;
    private volatile boolean closed;
    private String currentMusic;
    private javafx.scene.media.MediaPlayer musicPlayer;
    private boolean unavailable;

    public AudioService() {
        // Tiny settings file; read before controls are built so they show saved values.
        Properties settings = new Properties();
        if (Files.isRegularFile(settingsFile)) {
            try (var input = Files.newInputStream(settingsFile)) {
                settings.load(input);
                musicVolume = parseVolume(settings.getProperty("music"), musicVolume);
                effectsVolume = parseVolume(settings.getProperty("effects"), effectsVolume);
                muted = Boolean.parseBoolean(settings.getProperty("muted", "false"));
            } catch (Exception error) {
                LOG.log(System.Logger.Level.WARNING, "Unable to load audio settings", error);
            }
        }
    }

    private static double parseVolume(String value, double fallback) {
        try { double parsed = Double.parseDouble(value); return Double.isFinite(parsed) ? clamp(parsed) : fallback; }
        catch (Exception ignored) { return fallback; }
    }
    private static double clamp(double value) { return Math.max(0, Math.min(1, value)); }
    public double getMusicVolume() { return musicVolume; }
    public double getEffectsVolume() { return effectsVolume; }
    public boolean isMuted() { return muted; }

    public void setMusicVolume(double value) {
        if (!Double.isFinite(value)) return;
        musicVolume = clamp(value); updateSettings();
    }
    public void setEffectsVolume(double value) {
        if (!Double.isFinite(value)) return;
        effectsVolume = clamp(value); updateSettings();
    }
    public void setMuted(boolean value) { muted = value; updateSettings(); }

    public void playMusic(boolean home) { playTrack(home ? "embers" : "journey"); }
    public void playRegion(String region) { playScene(region, null); }
    private static void onFx(Runnable action) {
        if (javafx.application.Platform.isFxApplicationThread()) action.run();
        else javafx.application.Platform.runLater(action);
    }
    private void playTrack(String next) {
        if (closed) return;
        onFx(() -> {
            if (closed) return;
            // Repainting the same scene must not restart the song.
            if (!next.equals(currentMusic)) {
                if (musicPlayer != null) { musicPlayer.dispose(); musicPlayer = null; }
                currentMusic = next;
            }
            if (musicPlayer == null && !muted && musicVolume > 0) {
                try {
                    var resource = musicResource(next);
                    if (resource == null) throw new IllegalStateException("Missing music: " + next);
                    musicPlayer = new javafx.scene.media.MediaPlayer(new javafx.scene.media.Media(resource.toExternalForm()));
                    musicPlayer.setCycleCount(javafx.scene.media.MediaPlayer.INDEFINITE);
                    var player = musicPlayer;
                    player.setOnError(() -> LOG.log(System.Logger.Level.WARNING, "Music could not play: " + next, player.getError()));
                } catch (Exception error) {
                    LOG.log(System.Logger.Level.WARNING, "Music unavailable; SFX remain enabled", error);
                }
            }
            updateMusic();
        });
    }

    public void play(Sound sound) {
        if (muted || effectsVolume == 0) return;
        submit(() -> {
            if (muted || effectsVolume == 0) return;
            Clip clip = clip(sound.file);
            if (clip != null) {
                clip.stop(); clip.setFramePosition(0);
                gain(clip, effectsVolume); clip.start();
            }
        });
    }

    /** Called only on JavaFX thread; only one streaming MP3 player exists. */
    private void updateMusic() {
        if (musicPlayer == null) return;
        musicPlayer.setVolume(musicVolume);
        if (muted || musicVolume == 0) musicPlayer.pause();
        else musicPlayer.play();
    }

    private Clip clip(String name) {
        if (unavailable) return null;
        if (clips.containsKey(name)) return clips.get(name);
        Clip opened = null;
        try {
            var resource = openAudio(name);
            if (resource == null) throw new IllegalStateException("Missing audio: " + name);
            try (var stream = new BufferedInputStream(resource);
                 var audio = AudioSystem.getAudioInputStream(stream)) {
                opened = AudioSystem.getClip();
                opened.open(audio);
                clips.put(name, opened);
                return opened;
            }
        } catch (Exception error) {
            if (opened != null) opened.close();
            unavailable = true;
            clips.values().forEach(clip -> { clip.stop(); clip.close(); });
            clips.clear();
            LOG.log(System.Logger.Level.WARNING, "Audio unavailable; game continues silently", error);
            return null;
        }
    }

    public static java.io.InputStream openAudio(String name) {
        return AudioService.class.getResourceAsStream("/audio/" + name + ".wav");
    }

    private static void gain(Clip clip, double volume) {
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float decibels = (float) (20 * Math.log10(Math.max(0.0001, volume)));
            control.setValue(Math.max(control.getMinimum(), Math.min(control.getMaximum(), decibels)));
        }
    }

    private void updateSettings() {
        if (currentMusic != null) playTrack(currentMusic);
        submit(() -> {
            for (var entry : clips.entrySet()) {
                if (MUSIC_TRACKS.contains(entry.getKey())) continue;
                if (muted || effectsVolume == 0) entry.getValue().stop();
                else gain(entry.getValue(), effectsVolume);
            }
            Properties settings = new Properties();
            settings.setProperty("music", Double.toString(musicVolume));
            settings.setProperty("effects", Double.toString(effectsVolume));
            settings.setProperty("muted", Boolean.toString(muted));
            try {
                Files.createDirectories(settingsFile.getParent());
                try (var output = Files.newOutputStream(settingsFile)) { settings.store(output, "Teleknia audio"); }
            } catch (Exception error) { LOG.log(System.Logger.Level.WARNING, "Unable to save audio settings", error); }
        });
    }

    private void submit(Runnable action) {
        if (!closed) {
            try { worker.execute(action); }
            catch (RejectedExecutionException ignored) { /* Window is closing. */ }
        }
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        onFx(() -> { if (musicPlayer != null) { musicPlayer.dispose(); musicPlayer = null; } });
        worker.execute(() -> { clips.values().forEach(clip -> { clip.stop(); clip.close(); }); clips.clear(); });
        worker.shutdown();
        try { if (!worker.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) worker.shutdownNow(); }
        catch (InterruptedException error) { worker.shutdownNow(); Thread.currentThread().interrupt(); }
    }
}
