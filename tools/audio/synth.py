"""Synthesises every sound in the game from oscillators and filtered noise (no samples).

    python3 tools/audio/synth.py   # writes app/src/main/res/raw/*.ogg (needs numpy, scipy, ffmpeg)
"""
from __future__ import annotations

import os
import subprocess
import tempfile

import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, sosfilt

SR = 44100
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "app", "src", "main", "res", "raw")
rng = np.random.default_rng(1234)


def t_axis(seconds: float) -> np.ndarray:
    return np.arange(int(seconds * SR)) / SR


def noise(seconds: float) -> np.ndarray:
    return rng.standard_normal(int(seconds * SR))


def band(x: np.ndarray, lo: float | None, hi: float | None, order: int = 4) -> np.ndarray:
    if lo and hi:
        sos = butter(order, [lo, hi], btype="bandpass", fs=SR, output="sos")
    elif lo:
        sos = butter(order, lo, btype="highpass", fs=SR, output="sos")
    else:
        sos = butter(order, hi, btype="lowpass", fs=SR, output="sos")
    return sosfilt(sos, x)


def env(n: int, attack: float, decay: float, curve: float = 4.0) -> np.ndarray:
    """Exponential attack/decay envelope over n samples (times in seconds)."""
    t = np.arange(n) / SR
    a = np.clip(t / max(attack, 1e-4), 0, 1)
    d = np.exp(-np.clip(t - attack, 0, None) * curve / max(decay, 1e-4))
    return a * d


def fade(x: np.ndarray, fin: float = 0.002, fout: float = 0.02) -> np.ndarray:
    n = len(x)
    i, o = int(fin * SR), int(fout * SR)
    w = np.ones(n)
    if i:
        w[:i] = np.linspace(0, 1, i)
    if o:
        w[-o:] = np.linspace(1, 0, o)
    return x * w


def normalise(x: np.ndarray, peak: float) -> np.ndarray:
    m = np.max(np.abs(x))
    return x if m == 0 else x / m * peak


def mix_at(dst: np.ndarray, src: np.ndarray, at: float) -> None:
    i = int(at * SR)
    j = min(len(dst), i + len(src))
    dst[i:j] += src[: j - i]


# ------------------------------------------------------------------ effects

def ignite() -> np.ndarray:
    out = np.zeros(int(2.4 * SR))
    strike = band(noise(0.09), 1800, 7000) * env(int(0.09 * SR), 0.002, 0.05)
    mix_at(out, strike * 0.6, 0.0)
    whoomp = band(noise(0.9), 150, 1400) * env(int(0.9 * SR), 0.12, 0.6, 3.0)
    mix_at(out, whoomp * 0.55, 0.05)
    t = t_axis(2.2)
    chime = sum(a * np.sin(2 * np.pi * f * t) * np.exp(-t * d) for f, a, d in
                ((523.25, 0.5, 1.6), (784.0, 0.32, 2.0), (1046.5, 0.18, 2.6), (1568.0, 0.07, 3.4)))
    chime *= np.clip(t / 0.03, 0, 1)
    mix_at(out, chime * 0.32, 0.12)
    return normalise(fade(out, 0.001, 0.3), 0.6)


def tap() -> np.ndarray:
    n = int(0.07 * SR)
    t = t_axis(0.07)
    body = np.sin(2 * np.pi * 1650 * t) * np.exp(-t * 90)
    click = band(noise(0.07), 2500, 9000) * env(n, 0.0005, 0.006, 6)
    return normalise(fade(body * 0.7 + click * 0.5, 0.0005, 0.01), 0.35)


def paper() -> np.ndarray:
    seconds = 0.26
    n = int(seconds * SR)
    x = band(noise(seconds), 1400, 7000)
    shape = np.zeros(n)
    for at, a in ((0.0, 1.0), (0.05, 0.6), (0.11, 0.8), (0.17, 0.35)):
        seg = env(int(0.06 * SR), 0.004, 0.04, 5)
        mix_at(shape, seg * a, at)
    return normalise(fade(x * shape), 0.32)


def pin() -> np.ndarray:
    t = t_axis(0.12)
    n = len(t)
    tick = band(noise(0.12), 3000, 9000) * env(n, 0.0003, 0.004, 6)
    ring = np.sin(2 * np.pi * 980 * t) * np.exp(-t * 70)
    thump = np.sin(2 * np.pi * 150 * t) * np.exp(-t * 55)
    return normalise(fade(tick * 0.6 + ring * 0.35 + thump * 0.5), 0.34)


def pickup() -> np.ndarray:
    seconds = 0.5
    n = int(seconds * SR)
    x = noise(seconds)
    # rising band sweep, rendered in short overlapping chunks
    out = np.zeros(n)
    hop = 512
    win = np.hanning(hop * 2)
    for i in range(0, n - hop * 2, hop):
        f = 380 + (2200 - 380) * (i / n) ** 1.3
        seg = band(x[i:i + hop * 2], f * 0.7, f * 1.6, 2) * win
        out[i:i + hop * 2] += seg
    shape = np.sin(np.linspace(0, np.pi, n)) ** 1.6
    return normalise(fade(out * shape, 0.01, 0.06), 0.3)


def putdown() -> np.ndarray:
    t = t_axis(0.22)
    n = len(t)
    thud = np.sin(2 * np.pi * 95 * t) * np.exp(-t * 32)
    knock = np.sin(2 * np.pi * 260 * t) * np.exp(-t * 45)
    clack = band(noise(0.22), 900, 4000) * env(n, 0.0005, 0.012, 6)
    return normalise(fade(thud * 0.8 + knock * 0.35 + clack * 0.45), 0.42)


def denied() -> np.ndarray:
    out = np.zeros(int(0.24 * SR))
    t = t_axis(0.06)
    square = np.sign(np.sin(2 * np.pi * 210 * t))
    blip = band(square, None, 900, 2) * np.exp(-t * 60)
    mix_at(out, blip, 0.0)
    mix_at(out, blip * 0.8, 0.095)
    return normalise(fade(out), 0.36)


def scribble() -> np.ndarray:
    seconds = 0.7
    n = int(seconds * SR)
    t = t_axis(seconds)
    x = band(noise(seconds), 1800, 5200)
    strokes = 0.55 + 0.45 * np.sin(2 * np.pi * 11.5 * t) ** 2
    shape = np.sin(np.linspace(0, np.pi, n)) ** 0.6
    return normalise(fade(x * strokes * shape, 0.01, 0.08), 0.24)


# ------------------------------------------------------------------ ambience

def ambience(seconds: float = 48.0) -> np.ndarray:
    """A still lake at night: low drone, slow minor pads, water lapping, distant crickets.
    Rendered a little long and crossfaded onto itself so the loop point is seamless."""
    xf = 4.0
    total = seconds + xf
    n = int(total * SR)
    t = np.arange(n) / SR
    left = np.zeros(n)
    right = np.zeros(n)

    def lfo(period: float, phase: float = 0.0) -> np.ndarray:
        # periods divide the loop length so slow motion lines up at the seam
        return 0.5 + 0.5 * np.sin(2 * np.pi * (t / period) + phase)

    loop = seconds
    drone = (np.sin(2 * np.pi * 55.0 * t) * 0.5 + np.sin(2 * np.pi * 55.0 * 1.003 * t) * 0.35 +
             np.sin(2 * np.pi * 82.41 * t) * 0.22) * (0.55 + 0.45 * lfo(loop / 3))
    left += drone * 0.20
    right += drone * 0.20

    chords = [(110.0, 130.81, 164.81), (98.0, 123.47, 146.83), (87.31, 110.0, 130.81), (98.0, 116.54, 146.83)]
    seg = loop / len(chords)
    pad_l = np.zeros(n)
    pad_r = np.zeros(n)
    for k, chord in enumerate(chords + chords[:1]):
        start = k * seg
        w = np.clip(1 - np.abs((t - start - seg / 2) / (seg * 0.85)), 0, 1) ** 1.5
        for j, f in enumerate(chord):
            pad_l += w * np.sin(2 * np.pi * f * t + j) * (0.16 - j * 0.03)
            pad_r += w * np.sin(2 * np.pi * f * 1.004 * t + j * 1.7) * (0.16 - j * 0.03)
    left += band(pad_l, None, 900, 2) * 0.9
    right += band(pad_r, None, 900, 2) * 0.9

    for side, phase in ((left, 0.0), (right, 1.9)):
        water = band(rng.standard_normal(n), 120, 650, 2)
        lap = (0.35 + 0.65 * lfo(loop / 17, phase) * lfo(loop / 29, phase * 2)) ** 2
        side += water * lap * 0.10

    chirp_t = t_axis(0.03)
    pulse = np.sin(2 * np.pi * 4650 * chirp_t) * np.sin(np.linspace(0, np.pi, len(chirp_t)))
    at = 1.2
    while at < total - 1.5:
        pan = rng.uniform(0.15, 0.85)
        level = rng.uniform(0.006, 0.016)
        for p in range(rng.integers(2, 5)):
            mix_at(left, pulse * level * (1 - pan), at + p * 0.045)
            mix_at(right, pulse * level * pan, at + p * 0.045)
        at += rng.uniform(0.7, 2.6)

    stereo = np.stack([left, right], axis=1)
    m = int(xf * SR)
    head, body = stereo[:m], stereo[m:]
    ramp = np.linspace(0, 1, m)[:, None]
    body[-m:] = body[-m:] * (1 - ramp) + head * ramp
    return normalise(body, 0.5)


# ------------------------------------------------------------------ export

def write_ogg(name: str, data: np.ndarray, quality: int = 4) -> None:
    os.makedirs(OUT, exist_ok=True)
    pcm = (np.clip(data, -1, 1) * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as fh:
        wav = fh.name
    wavfile.write(wav, SR, pcm)
    dst = os.path.join(OUT, name + ".ogg")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", str(quality), dst],
                   check=True)
    os.unlink(wav)
    print(f"{name}.ogg  {os.path.getsize(dst) // 1024} KB")


def main() -> None:
    for name, fn in (("sfx_ignite", ignite), ("sfx_tap", tap), ("sfx_paper", paper), ("sfx_pin", pin),
                     ("sfx_pickup", pickup), ("sfx_putdown", putdown), ("sfx_denied", denied),
                     ("sfx_scribble", scribble)):
        write_ogg(name, fn())
    write_ogg("amb_lake", ambience(), quality=3)


if __name__ == "__main__":
    main()
