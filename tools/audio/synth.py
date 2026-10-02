"""Synthesises every sound in the game from oscillators and filtered noise (no samples).

    python3 tools/audio/synth.py               # writes every app/src/main/res/raw/*.ogg
    python3 tools/audio/synth.py sfx_notify    # writes only the named sounds
(needs numpy, scipy and ffmpeg)
"""
from __future__ import annotations

import os
import subprocess
import sys
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


# ------------------------------------------------------------------ phone UI effects
# Each uses its own seeded generator, so adding sounds never changes the ones above.

def _rng(seed: int) -> np.random.Generator:
    return np.random.default_rng(seed)


def _tone(freq: float, seconds: float, decay: float, attack: float = 0.004) -> np.ndarray:
    t = t_axis(seconds)
    return np.sin(2 * np.pi * freq * t) * np.exp(-t * decay) * np.clip(t / attack, 0, 1)


def msg_in() -> np.ndarray:
    out = np.zeros(int(0.42 * SR))
    mix_at(out, _tone(1318.5, 0.3, 18) * 0.7 + _tone(2637.0, 0.3, 30) * 0.12, 0.0)
    mix_at(out, _tone(1760.0, 0.34, 14) * 0.8 + _tone(3520.0, 0.34, 26) * 0.1, 0.075)
    return normalise(fade(out, 0.0005, 0.08), 0.34)


def msg_out() -> np.ndarray:
    seconds = 0.2
    n = int(seconds * SR)
    rng = _rng(31)
    air = rng.standard_normal(n)
    out = np.zeros(n)
    hop = 256
    win = np.hanning(hop * 2)
    for i in range(0, n - hop * 2, hop):
        f = 1200 + (4200 - 1200) * (i / n)
        out[i:i + hop * 2] += band(air[i:i + hop * 2], f * 0.7, min(f * 1.5, 9000), 2) * win
    out *= np.sin(np.linspace(0, np.pi, n)) ** 2
    pop = _tone(880.0, seconds, 45) * 0.5
    return normalise(fade(out * 0.6 + pop, 0.001, 0.03), 0.3)


def notify() -> np.ndarray:
    out = np.zeros(int(1.1 * SR))
    for i, f in enumerate((1046.5, 1318.5, 1568.0)):
        note = _tone(f, 0.9, 5.5) * 0.6 + _tone(f * 2, 0.9, 9) * 0.12
        mix_at(out, note * (0.9 - i * 0.1), i * 0.085)
    return normalise(fade(out, 0.0005, 0.2), 0.32)


def _swish(rising: bool, seed: int) -> np.ndarray:
    seconds = 0.26
    n = int(seconds * SR)
    air = _rng(seed).standard_normal(n)
    out = np.zeros(n)
    hop = 256
    win = np.hanning(hop * 2)
    for i in range(0, n - hop * 2, hop):
        p = i / n if rising else 1 - i / n
        f = 500 + 2600 * p ** 1.4
        out[i:i + hop * 2] += band(air[i:i + hop * 2], f * 0.7, f * 1.6, 2) * win
    shape = np.sin(np.linspace(0, np.pi, n)) ** (1.2 if rising else 2.0)
    return normalise(fade(out * shape, 0.004, 0.04), 0.22)


def app_open() -> np.ndarray:
    return _swish(True, 41)


def app_close() -> np.ndarray:
    return _swish(False, 43)


def offline() -> np.ndarray:
    out = np.zeros(int(0.6 * SR))
    mix_at(out, _tone(659.3, 0.45, 9) * 0.7, 0.0)
    mix_at(out, _tone(493.9, 0.5, 8) * 0.7, 0.13)
    return normalise(band(fade(out, 0.001, 0.1), None, 3000, 2), 0.26)


# ------------------------------------------------------------------ Phone app: keypad, ringing, hang-up
# Pure tones, so they need no random generator either.

# Each key sounds its row's low tone with its column's high tone, as on a real keypad.
KEYS = {
    "1": (697, 1209), "2": (697, 1336), "3": (697, 1477),
    "4": (770, 1209), "5": (770, 1336), "6": (770, 1477),
    "7": (852, 1209), "8": (852, 1336), "9": (852, 1477),
    "star": (941, 1209), "0": (941, 1336), "hash": (941, 1477),
}


def _line(x: np.ndarray) -> np.ndarray:
    """Narrowed to a phone line's band."""
    return band(x, 300, 3400, 2)


def key_tone(low: float, high: float) -> np.ndarray:
    t = t_axis(0.16)
    pair = np.sin(2 * np.pi * low * t) + np.sin(2 * np.pi * high * t)
    return normalise(fade(pair, 0.004, 0.03), 0.2)


def ringback() -> np.ndarray:
    """One ring as the caller hears it: 400 and 450 Hz together, twice, 0.4 s on and 0.2 s off."""
    out = np.zeros(int(1.0 * SR))
    t = t_axis(0.4)
    burst = fade(np.sin(2 * np.pi * 400 * t) + np.sin(2 * np.pi * 450 * t), 0.012, 0.025)
    mix_at(out, burst, 0.0)
    mix_at(out, burst, 0.6)
    return normalise(_line(out), 0.26)


def call_end() -> np.ndarray:
    """Three short pips as the line drops."""
    out = np.zeros(int(0.75 * SR))
    t = t_axis(0.13)
    pip = fade(np.sin(2 * np.pi * 480 * t) + 0.3 * np.sin(2 * np.pi * 960 * t), 0.004, 0.02)
    for k in range(3):
        mix_at(out, pip, k * 0.22)
    return normalise(_line(out), 0.26)


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


SOUNDS = {
    "sfx_ignite": ignite, "sfx_tap": tap, "sfx_paper": paper, "sfx_pin": pin, "sfx_pickup": pickup,
    "sfx_putdown": putdown, "sfx_denied": denied, "sfx_scribble": scribble,
    "sfx_msg_in": msg_in, "sfx_msg_out": msg_out, "sfx_notify": notify, "sfx_app_open": app_open,
    "sfx_app_close": app_close, "sfx_offline": offline,
    **{f"sfx_key_{key}": (lambda low=low, high=high: key_tone(low, high)) for key, (low, high) in KEYS.items()},
    "sfx_ringback": ringback, "sfx_call_end": call_end,
}


def main() -> None:
    every = [*SOUNDS, "amb_lake"]
    wanted = set(sys.argv[1:] or every)
    unknown = wanted.difference(every)
    if unknown:
        sys.exit(f"unknown sound: {', '.join(sorted(unknown))} (choose from {', '.join(every)})")
    # Every effect is rendered, in order, even when only some are written: the early effects and the
    # ambience draw from one seeded generator, so each sound comes out the same either way.
    for name, fn in SOUNDS.items():
        data = fn()
        if name in wanted:
            write_ogg(name, data)
    if "amb_lake" in wanted:
        write_ogg("amb_lake", ambience(), quality=3)


if __name__ == "__main__":
    main()
