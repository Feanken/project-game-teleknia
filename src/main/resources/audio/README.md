# Teleknia audio — 1.4.1

Background music now uses all ten supplied MP3 files in `../audio_frieren/`.
Old synthesized music WAVs have been removed. Eight synthesized WAV sound effects
remain in this directory. `scripts/generate_audio.py` regenerates SFX only.

| Context | Supplied song |
| --- | --- |
| Menu | Journey of a Lifetime |
| Ashen Reach | The End of One Journey |
| Emberwood | Departures |
| Frostveil | For 1000 Years |
| Drowned Coast | Time Flows Ever Onward |
| Wolf / Ashen battle | One Last Adventure |
| Guardian | Heiter's Request |
| Warden / Emberwood battle | The Party of Heroes Returns |
| Rime Sentinel / Frostveil battle | Farewell, My Friend |
| Drowned Captain / coast battle | A Well Earned Celebration |

Music streams directly through JavaFX Media (MP3); files keep their original
names. One player is active at a time and is disposed when the scene's theme
changes. Music loops, survives UI repaints without restarting, pauses when muted,
and returns to exploration after leaving battle. Loading a battle save restores
its encounter theme. Music volume and SFX volume remain separate. No conversion
or download is required. `data/music` WAV overrides from the earlier implementation
are no longer used. Save data is unchanged.

WAV sound effects: click, action, damage, potion, reward, level_up, discovery,
quest_complete. These still use Java Sound on a background worker. A music decoder
error does not disable sound effects.

Validation: `scripts/check-game.ps1` checks mappings/resources; the optional desktop
`scripts/check-audio.ps1` decodes and starts all ten MP3s at zero volume using the
actual JavaFX player. `scripts/check-ui.ps1` checks the game UI with audio muted.
