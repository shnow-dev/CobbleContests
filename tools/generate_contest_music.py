#!/usr/bin/env python3
"""Generate the two original, distributable CobbleContests music loops."""

from __future__ import annotations

import math
import random
import struct
import sys
import wave
from pathlib import Path

RATE = 22_050


def frequency(midi: int) -> float:
    return 440.0 * 2.0 ** ((midi - 69) / 12.0)


class Song:
    def __init__(self, seconds: float) -> None:
        self.frames = int(seconds * RATE)
        self.left = [0.0] * self.frames
        self.right = [0.0] * self.frames

    def tone(self, start: float, duration: float, midi: int, amplitude: float,
             pan: float = 0.0, bright: bool = False) -> None:
        first = max(0, int(start * RATE))
        last = min(self.frames, int((start + duration) * RATE))
        attack = max(1, int(min(0.045, duration * 0.18) * RATE))
        release = max(1, int(min(0.11, duration * 0.28) * RATE))
        hz = frequency(midi)
        left_gain = math.sqrt((1.0 - pan) * 0.5)
        right_gain = math.sqrt((1.0 + pan) * 0.5)
        for sample in range(first, last):
            age = sample - first
            remaining = last - sample
            envelope = min(1.0, age / attack, remaining / release)
            phase = 2.0 * math.pi * hz * age / RATE
            value = math.sin(phase)
            if bright:
                value += math.sin(phase * 2.0) * 0.28
                value += math.sin(phase * 3.0) * 0.10
            value *= amplitude * envelope
            self.left[sample] += value * left_gain
            self.right[sample] += value * right_gain

    def drum(self, start: float, amplitude: float, snare: bool = False) -> None:
        rng = random.Random(8_117 + int(start * 1_000) + (17 if snare else 0))
        first = int(start * RATE)
        length = int((0.12 if snare else 0.16) * RATE)
        for age in range(length):
            sample = first + age
            if sample >= self.frames:
                break
            t = age / RATE
            envelope = math.exp(-t * (28.0 if snare else 35.0))
            if snare:
                value = (rng.random() * 2.0 - 1.0) * envelope * amplitude
            else:
                hz = 95.0 - 55.0 * min(1.0, t / 0.14)
                value = math.sin(2.0 * math.pi * hz * t) * envelope * amplitude
            self.left[sample] += value * 0.70
            self.right[sample] += value * 0.70

    def write(self, path: Path) -> None:
        peak = max(0.01, *(abs(value) for value in self.left),
                   *(abs(value) for value in self.right))
        gain = min(0.92 / peak, 1.0)
        path.parent.mkdir(parents=True, exist_ok=True)
        with wave.open(str(path), "wb") as output:
            output.setnchannels(2)
            output.setsampwidth(2)
            output.setframerate(RATE)
            block = bytearray()
            for left, right in zip(self.left, self.right):
                block.extend(struct.pack("<hh", int(left * gain * 32767),
                                         int(right * gain * 32767)))
            output.writeframes(block)


def menu_song() -> Song:
    beat = 60.0 / 96.0
    song = Song(beat * 32.0)
    chords = [
        (60, 64, 67, 71), (57, 60, 64, 67),
        (53, 57, 60, 64), (55, 59, 62, 67),
    ]
    melody = [72, 76, 79, 76, 71, 72, 76, 79,
              69, 72, 76, 72, 67, 71, 74, 79]
    for bar in range(8):
        start = bar * beat * 4.0
        chord = chords[bar % len(chords)]
        for index, note in enumerate(chord):
            song.tone(start, beat * 3.85, note, 0.055, -0.55 + index * 0.36)
        for quarter in range(4):
            song.tone(start + quarter * beat, beat * 0.82,
                      chord[0] - 12, 0.075, -0.12)
        for eighth in range(8):
            note = melody[(bar * 2 + eighth // 4) % len(melody)]
            if eighth % 2 == 0:
                song.tone(start + eighth * beat / 2.0, beat * 0.40,
                          note + (eighth % 4) * 2, 0.050, 0.28, True)
        song.drum(start, 0.07)
        song.drum(start + beat * 2.0, 0.055)
    return song


def challenge_song() -> Song:
    beat = 60.0 / 128.0
    song = Song(beat * 32.0)
    chords = [(62, 65, 69), (58, 62, 65), (65, 69, 72), (60, 64, 67)]
    lead = [74, 77, 81, 84, 81, 77, 76, 79,
            82, 86, 82, 79, 72, 76, 79, 84]
    for bar in range(8):
        start = bar * beat * 4.0
        chord = chords[bar % len(chords)]
        for index, note in enumerate(chord):
            song.tone(start, beat * 3.9, note, 0.045, -0.45 + index * 0.45, True)
        for eighth in range(8):
            note = chord[eighth % len(chord)] + 12
            song.tone(start + eighth * beat / 2.0, beat * 0.36,
                      note, 0.060, 0.30 if eighth % 2 else -0.30, True)
        for quarter in range(4):
            song.tone(start + quarter * beat, beat * 0.72,
                      chord[0] - 12, 0.095, -0.08, True)
            song.drum(start + quarter * beat, 0.12)
            if quarter in (1, 3):
                song.drum(start + quarter * beat, 0.060, True)
        for half in range(2):
            song.tone(start + half * beat * 2.0, beat * 1.35,
                      lead[(bar * 2 + half) % len(lead)], 0.070, 0.18, True)
    return song


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("usage: generate_contest_music.py OUTPUT_DIRECTORY")
    destination = Path(sys.argv[1])
    menu_song().write(destination / "menu.wav")
    challenge_song().write(destination / "challenge.wav")


if __name__ == "__main__":
    main()
