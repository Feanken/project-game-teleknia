# Teleknia original audio

Original instrumental chiptune composition and synthesized effects created for
this project. No third-party recordings, samples, vocals, or music downloads.
Source: `scripts/generate_audio.py` at the project root.
Format: 22,050 Hz, mono, signed 16-bit PCM WAV.

| File | Role |
| --- | --- |
| embers.wav | 16-bar A-minor menu loop, 88 BPM |
| journey.wav | 16-bar A-minor exploration loop, 104 BPM |
| frostveil.wav | 16-bar D-minor mountain loop, 84 BPM |
| drowned.wav | 16-bar E-minor coast loop, 96 BPM |
| discovery.wav | Location/codex discovery |
| quest_complete.wav | Quest milestone completed |
| click.wav | Button activation |
| action.wav | Story action |
| damage.wav | Incoming damage |
| potion.wav | Consumed potion |
| reward.wav | Gold/item reward |
| level_up.wav | Level-up fanfare |

The generator applies attack/release envelopes and asserts that samples do not
clip. Clips are cached and mixed at runtime with independent music/SFX volume.
