"""Original Teleknia chiptune score and SFX. Python standard library only.

Run from any directory: python scripts/generate_audio.py
Writes mono PCM16 WAV resources; no samples or third-party music used.
"""
import math
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


def music(name, adventurous=False, region=None):
    beat = 60 / (84 if region == "frost" else 96 if region == "coast" else 104 if adventurous else 88)
    track = [0.0] * round(16 * 4 * beat * RATE)
    chords = [(45, 52, 57, 60), (41, 48, 53, 57), (48, 55, 60, 64), (43, 50, 55, 59)]
    melody = [76, 72, 71, 69, 72, 76, 79, 76, 77, 76, 72, 69, 67, 71, 72, 71]
    if region == "frost":
        chords = [(38,45,50,53),(34,41,46,50),(41,48,53,57),(36,43,48,52)]
        melody = [74,77,76,74,69,72,74,77,79,77,74,72,69,65,69,72]
    elif region == "coast":
        chords = [(40,47,52,55),(36,43,48,52),(43,50,55,59),(38,45,50,54)]
        melody = [71,76,79,78,76,74,71,67,69,74,78,76,74,71,69,66]
    for bar in range(16):
        chord = chords[bar % 4]
        offset = bar * 4 * beat
        for step in range(8):
            note(track, offset + step * beat / 2, beat * 0.46,
                 chord[step % 4] + 12, 0.08, "pulse")
        for step in range(4):
            note(track, offset + step * beat, beat * 0.8, chord[0] - 12, 0.13)
        note(track, offset, beat * 1.8, melody[bar], 0.14)
        note(track, offset + beat * 2.5, beat * 1.2,
             melody[(bar + 1) % 16], 0.11)
        if adventurous:
            for step in range(4):
                start = int((offset + step * beat) * RATE)
                for n in range(int(RATE * 0.10)):
                    if start + n < len(track):
                        t = n / RATE
                        track[start + n] += 0.09 * math.sin(2 * math.pi * (90*t - 200*t*t)) * math.exp(-35*t)
    write(name, track)


def effect(name, pitches, spacing=0.09, duration=0.14, voice="pulse"):
    track = [0.0] * int((len(pitches) * spacing + duration) * RATE)
    for i, pitch in enumerate(pitches):
        note(track, i * spacing, duration, pitch, 0.25, voice)
    write(name, track)


music("embers")
music("journey", True)
music("frostveil", region="frost")
music("drowned", True, region="coast")
effect("click", [76], 0.02, 0.09)
effect("action", [57, 64, 69])
effect("potion", [72, 76, 79, 84], 0.075, 0.18, "triangle")
effect("reward", [76, 79, 84], 0.10, 0.20)
effect("level_up", [69, 72, 76, 81, 76, 81, 84], 0.12, 0.23)
effect("damage", [45, 40, 33], 0.055, 0.16)
effect("discovery", [76, 81, 83, 88], 0.10, 0.22, "triangle")
effect("quest_complete", [72, 76, 79, 84, 88], 0.10, 0.25)
print(f"Generated {len(list(OUT.glob('*.wav')))} original WAV assets in {OUT}")
