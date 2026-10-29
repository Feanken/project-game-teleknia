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

/** All decoding, audio device access and settings IO run off the JavaFX thread. */
public final class AudioService implements AutoCloseable {
    public enum Sound {
        CLICK("click"), ACTION("action"), DAMAGE("damage"), POTION("potion"),
        REWARD("reward"), LEVEL_UP("level_up"), DISCOVERY("discovery"), QUEST_COMPLETE("quest_complete");
        final String file;
        Sound(String file) { this.file = file; }
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
    public void playRegion(String region) { playTrack(region.equals("Frostveil") ? "frostveil" : region.equals("Drowned Coast") ? "drowned" : "journey"); }
    private void playTrack(String next) {
        submit(() -> {
            if (!next.equals(currentMusic)) {
                Clip previous = clips.get(currentMusic);
                if (previous != null) previous.stop();
                currentMusic = next;
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

    private void updateMusic() {
        if (currentMusic == null) return;
        Clip clip = clips.get(currentMusic);
        if (muted || musicVolume == 0) {
            if (clip != null) clip.stop();
            return;
        }
        if (clip == null) clip = clip(currentMusic);
        if (clip != null) {
            gain(clip, musicVolume);
            if (!clip.isRunning()) clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    private Clip clip(String name) {
        if (unavailable) return null;
        if (clips.containsKey(name)) return clips.get(name);
        Clip opened = null;
        try {
            var resource = AudioService.class.getResourceAsStream("/audio/" + name + ".wav");
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

    private static void gain(Clip clip, double volume) {
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float decibels = (float) (20 * Math.log10(Math.max(0.0001, volume)));
            control.setValue(Math.max(control.getMinimum(), Math.min(control.getMaximum(), decibels)));
        }
    }

    private void updateSettings() {
        submit(() -> {
            updateMusic();
            for (var entry : clips.entrySet()) {
                if (java.util.Set.of("embers","journey","frostveil","drowned").contains(entry.getKey())) continue;
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
        worker.execute(() -> { clips.values().forEach(clip -> { clip.stop(); clip.close(); }); clips.clear(); });
        worker.shutdown();
        try { if (!worker.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) worker.shutdownNow(); }
        catch (InterruptedException error) { worker.shutdownNow(); Thread.currentThread().interrupt(); }
    }
}
