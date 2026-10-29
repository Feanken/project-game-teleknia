package com.rpgchatgame;

import com.rpgchatgame.service.AudioService;
import java.nio.file.*;
import java.util.*;
import javax.sound.sampled.AudioSystem;

/** No audio device or real player settings needed. */
public final class AudioChecks {
    static int count;
    static void check(boolean ok, String message) { count++; if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        var regions = List.of("Ashen Reach", "Emberwood", "Frostveil", "Drowned Coast");
        Set<String> exploration = new HashSet<>(), battle = new HashSet<>(), hashes = new HashSet<>();
        for (String region : regions) {
            String field = AudioService.sceneTrack(region, null);
            exploration.add(field);
            String fight = AudioService.sceneTrack(region, "unknown_encounter");
            battle.add(fight);
            check(!field.equals(fight), "Battle overrides region music");
            check(AudioService.sceneTrack(region, null).equals(field), "Return to field after encounter");
        }
        check(exploration.size() == 4 && battle.size() == 4, "Each region has distinct field and battle tracks");
        Set<String> bosses = new HashSet<>();
        for (String id : List.of("wolf", "guardian", "warden", "rime_sentinel", "drowned_captain")) {
            String theme = AudioService.sceneTrack("Frostveil", id);
            bosses.add(theme);
            check(theme.equals(AudioService.sceneTrack("Drowned Coast", id)), "Encounter theme has priority");
        }
        check(bosses.size() == 5, "Distinct encounter themes");
        check(AudioService.MUSIC_FILES.keySet().equals(AudioService.MUSIC_TRACKS), "Every music context is mapped");
        Set<String> songs = new HashSet<>(AudioService.MUSIC_FILES.values());
        check(songs.size() == 10, "All ten supplied songs are used");
        for (String name : AudioService.MUSIC_TRACKS) {
            var resource = AudioService.musicResource(name);
            check(resource != null && resource.toString().contains("audio_frieren"), "Supplied MP3 mapped: " + name);
        }
        for (String file : songs) {
            try (var input = AudioChecks.class.getResourceAsStream("/audio_frieren/" + file)) {
                byte[] bytes = input.readAllBytes();
                check(bytes.length > 1000, "Nonempty supplied MP3: " + file);
                hashes.add(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)));
            }
        }
        check(hashes.size() == 10, "Ten different supplied recordings");
        for (String name : AudioService.MUSIC_TRACKS)
            check(AudioChecks.class.getResource("/audio/" + name + ".wav") == null, "Old music removed: " + name);
        check(AudioService.sceneTrack(null, null).equals("journey"), "Unknown region fallback");
        System.out.println("PASS / " + count + " audio checks: region/encounter routing, ten supplied MP3s, complete routing, old music removed.");
    }
}
