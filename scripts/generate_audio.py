"""Teleknia synthesized SFX (background music is supplied separately). Python standard library only.

Run from any directory: python scripts/generate_audio.py
Writes mono PCM16 WAV resources; no samples or third-party music used.
"""
import struct
import wave
from pathlib import Path

RATE = 22050
OUT = Path(__file__).resolve().parents[1] / "src/main/resources/audio"
OUT.mkdir(parents=True, exist_ok=True)


def note(track, start, duration, midi, volume=0.15, voice="triangle"):
    frequency = 440 * 2 ** ((midi - 69) / 12)
    for n in range(int(duration * RATE)):
        index = int(start * RATE) + n
        if index >= len(track):
            break
        t = n / RATE
        phase = (t * frequency) % 1
        if voice == "pulse":
            value = 0.65 if phase < 0.25 else -0.22
        else:
            value = 1 - 4 * abs(phase - 0.5)
        envelope = min(1, t / 0.012) * min(1, (duration - t) / 0.08)
        track[index] += value * volume * max(0, envelope)


def write(name, samples):
    assert max(abs(x) for x in samples) < 1, f"Clipping in {name}"
    with wave.open(str(OUT / (name + ".wav")), "wb") as output:
        output.setparams((1, 2, RATE, 0, "NONE", "not compressed"))
        output.writeframes(b"".join(struct.pack("<h", round(x * 32767)) for x in samples))


def effect(name, pitches, spacing=0.09, duration=0.14, voice="pulse"):
    track = [0.0] * int((len(pitches) * spacing + duration) * RATE)
    for i, pitch in enumerate(pitches):
        note(track, i * spacing, duration, pitch, 0.25, voice)
    write(name, track)


effect("click", [76], 0.02, 0.09)
effect("action", [57, 64, 69])
effect("potion", [72, 76, 79, 84], 0.075, 0.18, "triangle")
effect("reward", [76, 79, 84], 0.10, 0.20)
effect("level_up", [69, 72, 76, 81, 76, 81, 84], 0.12, 0.23)
effect("damage", [45, 40, 33], 0.055, 0.16)
effect("discovery", [76, 81, 83, 88], 0.10, 0.22, "triangle")
effect("quest_complete", [72, 76, 79, 84, 88], 0.10, 0.25)
print(f"Generated {len(list(OUT.glob('*.wav')))} original WAV assets in {OUT}")
