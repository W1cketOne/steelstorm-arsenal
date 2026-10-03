#!/usr/bin/env python3
"""Synthesizes every Steelstorm Arsenal sound effect from scratch and writes OGG Vorbis files.

Run from the repository root:  python3 tools/gen_sounds.py
Requires: pip install numpy scipy soundfile

Nothing here is sampled: whooshes are filtered noise, clangs are sums of inharmonic partials,
booms are pitch-swept sines with filtered noise, and so on. Output goes to
src/main/resources/assets/steelstorm/sounds/ together with sounds.json.
"""
import json
import math
import os

import numpy as np
import soundfile as sf
from scipy.signal import butter, fftconvolve, sosfilt

SR = 44100
ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "steelstorm")
OUT = os.path.join(ROOT, "sounds")


# ----------------------------------------------------------------------------- primitives

def n_of(d):
    return int(SR * d)


def times(d):
    return np.arange(n_of(d)) / SR


def white(d, rng):
    return rng.uniform(-1.0, 1.0, n_of(d))


def brown(d, rng):
    x = np.cumsum(rng.normal(0, 1, n_of(d)))
    x = highpass(x, 20.0)
    return x / (np.max(np.abs(x)) + 1e-9)


def _sos(kind, f, order=2):
    return butter(order, f, btype=kind, fs=SR, output="sos")


def lowpass(x, fc, order=2):
    return sosfilt(_sos("low", min(fc, SR * 0.45), order), x)


def highpass(x, fc, order=2):
    return sosfilt(_sos("high", fc, order), x)


def bandpass(x, lo, hi, order=2):
    return sosfilt(_sos("band", [lo, min(hi, SR * 0.45)], order), x)


def svf(x, fc, damp=0.6, mode="band"):
    """Time-varying state-variable filter. fc may be an array (one cutoff per sample)."""
    fc = np.broadcast_to(np.asarray(fc, dtype=float), x.shape)
    f = 2.0 * np.sin(np.pi * np.clip(fc, 20.0, SR / 7.0) / SR)
    low = band = 0.0
    out = np.empty_like(x)
    for i in range(len(x)):
        low += f[i] * band
        high = x[i] - low - damp * band
        band += f[i] * high
        out[i] = band if mode == "band" else (low if mode == "low" else high)
    return out


def osc(freq, d, kind="sine", phase0=0.0):
    freq = np.broadcast_to(np.asarray(freq, dtype=float), (n_of(d),))
    ph = phase0 + 2 * np.pi * np.cumsum(freq) / SR
    if kind == "sine":
        return np.sin(ph)
    if kind == "saw":
        return 2.0 * ((ph / (2 * np.pi)) % 1.0) - 1.0
    if kind == "square":
        return np.sign(np.sin(ph))
    if kind == "tri":
        return 2.0 * np.abs(2.0 * ((ph / (2 * np.pi)) % 1.0) - 1.0) - 1.0
    raise ValueError(kind)


def partials(freqs, amps, decays, d, rng, attack=0.002):
    t = times(d)
    out = np.zeros_like(t)
    for f, a, dec in zip(freqs, amps, decays):
        out += a * np.exp(-t / dec) * np.sin(2 * np.pi * f * t + rng.uniform(0, 2 * np.pi))
    att = np.clip(t / attack, 0, 1)
    return out * att


def env_hump(d, peak=0.4, sharp=1.0):
    """Rises to 1 at fraction `peak` of the duration, then falls back to 0."""
    t = times(d) / d
    a = np.where(t < peak, (t / peak), (1 - t) / (1 - peak))
    return np.clip(a, 0, 1) ** sharp


def env_exp(d, rate, attack=0.003):
    t = times(d)
    return np.clip(t / attack, 0, 1) * np.exp(-t * rate)


def pad(x, d):
    out = np.zeros(n_of(d))
    out[: min(len(x), len(out))] = x[: len(out)]
    return out


def place(dst, src, at):
    i = int(at * SR)
    if i >= len(dst):
        return dst
    j = min(len(dst), i + len(src))
    dst[i:j] += src[: j - i]
    return dst


def reverb(x, rt=0.8, mix=0.25, rng=None, tone=5000):
    rng = rng or np.random.default_rng(7)
    ir_t = times(rt * 1.2)
    ir = rng.normal(0, 1, len(ir_t)) * np.exp(-ir_t * 6.9 / rt)
    ir = lowpass(ir, tone)
    ir /= np.sqrt(np.sum(ir ** 2)) + 1e-9
    wet = fftconvolve(x, ir)[: len(x) + len(ir_t)]
    dry = np.concatenate([x, np.zeros(len(wet) - len(x))])
    return dry * (1 - mix) + wet * mix * 1.6


def saturate(x, drive=1.5):
    return np.tanh(x * drive) / np.tanh(drive)


def finish(x, peak=0.89, fade_out=0.02):
    x = x - np.mean(x)
    m = np.max(np.abs(x)) + 1e-9
    x = x / m * peak
    k = min(len(x), n_of(fade_out))
    if k > 0:
        x[-k:] *= np.linspace(1, 0, k)
    k = min(len(x), 32)
    x[:k] *= np.linspace(0, 1, k)
    return x.astype(np.float32)


# ----------------------------------------------------------------------------- sound designs

def whoosh(rng, d=0.32, lo=700, hi=3100, peak=0.4, damp=0.55, zing=0.08, body=0.0):
    t = times(d) / d
    center = lo + (hi - lo) * np.sin(np.pi * np.clip(t / (peak * 2), 0, 1)) ** 1.2
    center = np.where(t > peak, lo * 0.8 + (hi - lo * 0.8) * (1 - (t - peak) / (1 - peak)) ** 1.6, center)
    # A narrow band sweeping up and back down is what makes it read as a blade moving past.
    x = svf(white(d, rng), center, damp * 0.55) * 1.6 + 0.25 * svf(white(d, rng), center * 1.8, 0.9)
    x = lowpass(x, 7000)
    x *= env_hump(d, peak, 1.4)
    if zing:
        x += zing * osc(center * 0.9 + 400, d) * env_hump(d, peak, 2.0)
    if body:
        x += body * lowpass(white(d, rng), 280) * env_hump(d, peak, 1.2) * 3
    return x


def thump(d, f_hi=140, f_lo=48, rate=22):
    t = times(d)
    f = f_lo + (f_hi - f_lo) * np.exp(-t * 25)
    return osc(f, d) * np.exp(-t * rate)


def clang(rng, base, d, ratios=(1.0, 2.41, 3.77, 5.45, 7.13), decays=(0.5, 0.35, 0.25, 0.17, 0.11), bright=1.0):
    amps = [1.0, 0.7, 0.5, 0.35, 0.25]
    x = partials([base * r for r in ratios], amps, decays, d, rng)
    trans = highpass(white(d, rng), 3000) * env_exp(d, 220) * 0.8 * bright
    return x + trans


def bell(rng, f, d, decay=0.5, amp=1.0):
    return amp * partials([f, f * 2.76, f * 5.4, f * 0.5], [1.0, 0.45, 0.2, 0.25], [decay, decay * 0.6, decay * 0.3, decay * 1.2], d, rng)


def crackle(rng, d, grains=30, lo=900, hi=5000, early=2.0):
    out = np.zeros(n_of(d))
    for _ in range(grains):
        at = (rng.random() ** early) * d * 0.9
        gd = rng.uniform(0.004, 0.022)
        c = rng.uniform(lo, hi)
        g = bandpass(white(gd + 0.01, rng), c * 0.7, c * 1.3) * env_exp(gd + 0.01, 1.0 / gd * 3)
        place(out, g * rng.uniform(0.4, 1.0), at)
    return out


def roar(rng, d, f0=110.0, formants=(700, 1150, 2600), drive=3.0):
    t = times(d)
    contour = f0 * (1.0 + 0.25 * np.sin(np.pi * np.clip(t / d, 0, 1)) - 0.15 * (t / d)) * (1 + 0.02 * np.sin(2 * np.pi * 6 * t))
    jitter = 1 + 0.03 * lowpass(rng.normal(0, 1, len(t)), 30) * 10
    src = osc(contour * jitter, d, "saw") + 0.4 * white(d, rng)
    out = np.zeros_like(t)
    for i, f in enumerate(formants):
        out += bandpass(src, f * 0.8, f * 1.2) * (1.0 / (i + 1))
    out *= env_hump(d, 0.25, 0.7)
    return saturate(out * 2, drive)


def magic_sweep(rng, d, lo=400, hi=4200):
    t = times(d) / d
    x = svf(white(d, rng), lo + (hi - lo) * t ** 1.5, 0.35)
    x *= env_hump(d, 0.75, 1.2)
    x += 0.25 * osc(300 + 600 * t, d) * (1 + 0.2 * np.sin(2 * np.pi * 7 * times(d))) * env_hump(d, 0.7, 1.5)
    for _ in range(14):
        at = rng.uniform(0.35, 0.95) * d
        f = rng.uniform(2800, 7000)
        place(x, bell(rng, f, 0.12, 0.05, 0.15), at)
    return x


def boom(rng, d, f_start=85, f_end=28, body=1.0, crack=1.0):
    t = times(d)
    sub = osc(f_end + (f_start - f_end) * np.exp(-t * 2.0), d) * np.exp(-t * 2.2)
    b = lowpass(brown(d, rng), 450) * np.exp(-t * 2.8) * body * 1.4
    mid = bandpass(white(d, rng), 180, 1400) * np.exp(-t * 9) * 0.6
    cr = highpass(white(d, rng), 2500) * env_exp(d, 140) * 1.4 * crack
    return saturate(sub * 1.4 + b + mid + cr, 1.6)


# ----------------------------------------------------------------------------- the library

def build():
    lib = {}

    def add(event, name, data):
        lib.setdefault(event, []).append((name, data))

    for i, (d, lo, hi, seed) in enumerate([(0.30, 650, 3000, 1), (0.34, 600, 2700, 2), (0.27, 800, 3400, 3)]):
        rng = np.random.default_rng(seed)
        add("weapon.swing", f"weapon/swing{i + 1}", whoosh(rng, d, lo, hi, 0.42, 0.55, 0.07))
    for i, (d, seed) in enumerate([(0.50, 11), (0.56, 12)]):
        rng = np.random.default_rng(seed)
        add("weapon.swing_heavy", f"weapon/swing_heavy{i + 1}", whoosh(rng, d, 260, 1150, 0.45, 0.75, 0.03, body=0.5))
    for i, seed in enumerate([21, 22, 23]):
        rng = np.random.default_rng(seed)
        d = 0.28
        x = thump(d, 150 + 20 * i, 50) * 1.2
        x += bandpass(white(d, rng), 500, 2600) * env_exp(d, 55) * 0.9
        x += highpass(white(d, rng), 4000) * env_exp(d, 140) * 0.35
        x += clang(rng, 1700 + 150 * i, d, decays=(0.08, 0.06, 0.05, 0.04, 0.03)) * 0.18
        add("weapon.hit", f"weapon/hit{i + 1}", saturate(x, 1.8))
    for i, seed in enumerate([31, 32]):
        rng = np.random.default_rng(seed)
        d = 0.5
        add("weapon.hit_metal", f"weapon/hit_metal{i + 1}", clang(rng, 1150 + 120 * i, d, decays=(0.28, 0.2, 0.13, 0.09, 0.06)) + thump(d) * 0.5)

    rng = np.random.default_rng(41)
    d = 0.8
    x = clang(rng, 610, d) + bandpass(white(d, rng), 2000, 6500) * env_exp(d, 12) * 0.22 + thump(d, 120, 60) * 0.5
    add("weapon.parry", "weapon/parry", reverb(x, 0.5, 0.18, rng))

    rng = np.random.default_rng(42)
    d = 1.5
    x = clang(rng, 1560, d, ratios=(1.0, 1.5, 2.0, 3.0, 3.79), decays=(1.0, 0.8, 0.6, 0.4, 0.3), bright=1.4) * 1.1
    for k, f in enumerate([2093, 2637, 3136, 4186]):
        place(x, bell(rng, f, 0.7, 0.45, 0.35), 0.04 + 0.05 * k)
    x += thump(d, 180, 70) * 0.6
    add("weapon.perfect_parry", "weapon/perfect_parry", reverb(x, 1.0, 0.3, rng))

    rng = np.random.default_rng(43)
    d = 0.9
    t = times(d)
    x = clang(rng, 180, d, decays=(0.4, 0.3, 0.2, 0.15, 0.1)) + lowpass(white(d, rng), 2000) * env_exp(d, 15) * 0.8
    x += osc(300 * np.exp(-t * 3) + 70, d) * np.exp(-t * 5) * 0.7
    add("weapon.guard_break", "weapon/guard_break", saturate(x, 2.2))

    rng = np.random.default_rng(51)
    d = 0.4
    x = whoosh(rng, d, 350, 1500, 0.3, 0.8, 0.0, body=0.4)
    place(x, thump(0.15, 110, 60, 30) * 0.5, 0.24)
    add("player.dodge", "player/dodge", x)

    rng = np.random.default_rng(61)
    add("ability.cast", "ability/cast", reverb(magic_sweep(rng, 0.75), 0.6, 0.25, rng))
    rng = np.random.default_rng(62)
    add("ability.dash", "ability/dash", whoosh(rng, 0.3, 900, 3800, 0.3, 0.4, 0.12))
    rng = np.random.default_rng(63)
    add("ability.ready", "ability/ready", reverb(bell(rng, 1318, 0.7, 0.3) + bell(rng, 1976, 0.7, 0.25, 0.6), 0.6, 0.2, rng))

    rng = np.random.default_rng(64)
    d = 1.8
    t = times(d)
    x = np.zeros(n_of(d))
    for f in (261.6, 329.6, 392.0, 523.3):
        x += osc(f, d) * np.clip(t / 0.15, 0, 1) * np.exp(-t * 1.6) * 0.3
    x += bell(rng, 1046.5, d, 0.9, 0.6) + magic_sweep(rng, d, 1500, 6000) * 0.25
    add("ultimate.ready", "ability/ultimate_ready", reverb(x, 1.2, 0.3, rng))

    rng = np.random.default_rng(65)
    d = 1.6
    t = times(d)
    swell = svf(white(d, rng), 200 + 6000 * np.clip(t / 0.6, 0, 1) ** 2, 0.5) * np.clip(t / 0.6, 0, 1) ** 2 * (t < 0.62)
    x = swell + pad(boom(rng, 1.0), d) * 0
    place(x, boom(rng, 1.0, 70, 26), 0.6)
    place(x, magic_sweep(rng, 0.8, 2000, 7000) * 0.4, 0.62)
    add("ultimate.cast", "ability/ultimate_cast", reverb(x, 1.2, 0.25, rng))

    rng = np.random.default_rng(71)
    x = boom(rng, 2.2)
    x = place(pad(x, 2.4), crackle(rng, 0.8, 18, 600, 3000) * 0.5, 0.05)
    add("ability.shockwave", "ability/shockwave", reverb(x, 1.6, 0.25, rng, 2500))

    rng = np.random.default_rng(72)
    d = 1.0
    t = times(d)
    x = crackle(rng, d, 34, 900, 5000, 1.6) + lowpass(brown(d, rng), 220) * np.exp(-t * 3) * 0.9
    add("ability.ground_crack", "ability/ground_crack", reverb(x, 0.7, 0.2, rng))

    rng = np.random.default_rng(73)
    d = 1.6
    t = times(d)
    x = lowpass(brown(d, rng), 160) * (0.7 + 0.3 * np.sin(2 * np.pi * 9 * t)) * env_hump(d, 0.15, 0.8) * 2
    x += osc(40, d) * env_hump(d, 0.2, 0.8) * 0.6
    add("ability.rumble", "ability/rumble", saturate(x, 1.3))

    rng = np.random.default_rng(74)
    d = 0.6
    t = times(d)
    gate = np.repeat(rng.random(int(d * 160) + 1) > 0.45, SR // 160 + 1)[: n_of(d)]
    x = highpass(white(d, rng), 1500) * gate * np.exp(-t * 5)
    x += svf(white(d, rng), 3000 - 2000 * t / d, 0.3) * np.exp(-t * 6) * 0.6
    x += osc(120, d, "saw") * gate * np.exp(-t * 8) * 0.25
    add("ability.zap", "ability/zap", saturate(x, 1.8))

    rng = np.random.default_rng(75)
    d = 3.2
    t = times(d)
    roll = lowpass(brown(d, rng), 380) * (0.6 + 0.4 * lowpass(rng.normal(0, 1, len(t)), 4) * 8) * np.exp(-t * 1.1)
    x = roll * 1.6 + pad(boom(rng, 1.5, 70, 25), d) * 0.8
    place(x, highpass(white(0.18, rng), 2000) * env_exp(0.18, 25) * 1.5, 0.0)
    add("ability.thunder", "ability/thunder", reverb(saturate(x, 1.4), 2.0, 0.3, rng, 2000))

    rng = np.random.default_rng(76)
    d = 0.9
    t = times(d)
    x = crackle(rng, d, 26, 3000, 9000, 1.5) * 0.8
    x += partials([2637, 3520, 4186, 5274, 6271], [1, .8, .6, .5, .4], [.35, .28, .22, .18, .14], d, rng) * 0.6
    x += highpass(white(d, rng), 6000) * np.exp(-t * 6) * 0.25
    add("ability.frost", "ability/frost", reverb(x, 0.8, 0.3, rng, 8000))

    rng = np.random.default_rng(77)
    d = 1.3
    t = times(d)
    chord = sum(osc(f, d, "saw") for f in (55.0, 58.3, 82.4, 110.6))
    swell = svf(chord, 200 + 1100 * np.clip(t / 0.9, 0, 1) ** 2, 0.4, "low") * np.clip(t / 0.9, 0, 1) ** 2.2 * (t < 0.92)
    x = swell + pad(thump(0.6, 90, 35, 6), d) * 0
    place(x, thump(0.6, 90, 35, 6) * 1.2, 0.9)
    add("ability.void", "ability/void", reverb(saturate(x, 1.5), 1.2, 0.35, rng, 3000))

    rng = np.random.default_rng(78)
    d = 2.0
    t = times(d)
    chord = osc(55, d, "saw") + osc(55.8, d, "saw") + 0.5 * osc(110.4, d, "saw")
    x = svf(chord, 350 + 250 * np.sin(2 * np.pi * 0.8 * t), 0.5, "low")
    x *= np.clip(t / 0.25, 0, 1) * np.clip((d - t) / 0.25, 0, 1)
    add("ability.void_hum", "ability/void_hum", x)

    rng = np.random.default_rng(79)
    d = 0.4
    t = times(d)
    x = svf(lowpass(white(d, rng), 1200), 520, 0.15) * env_exp(d, 18) * 1.5
    for _ in range(6):
        f = rng.uniform(700, 1500)
        place(x, osc(f * np.exp(-times(0.05) * 20), 0.05) * env_exp(0.05, 60) * 0.3, rng.uniform(0.05, 0.3))
    add("ability.blood", "ability/blood", x)

    rng = np.random.default_rng(80)
    d = 0.7
    t = times(d)
    x = svf(white(d, rng), 2000 * np.exp(-t * 5) + 250, 0.7, "low") * env_exp(d, 6, 0.01) + thump(d, 90, 50, 14) * 0.4
    add("ability.smoke", "ability/smoke", x)

    rng = np.random.default_rng(81)
    d = 0.9
    t = times(d)
    x = whoosh(rng, d, 900, 3000, 0.35, 0.45, 0.0)
    x += osc(220 + 110 * t / d, d) * (1 + 0.3 * np.sin(2 * np.pi * 11 * t)) * env_hump(d, 0.35, 1.2) * 0.35
    add("ability.slash_wave", "ability/slash_wave", reverb(x, 0.6, 0.2, rng))

    rng = np.random.default_rng(82)
    d = 1.3
    t = times(d)
    x = osc(3000 * np.exp(-t * 2.0) + 400, d) * np.clip(t / 0.8, 0, 1) ** 2 * (t < 0.8) * 0.4
    place(x, clang(rng, 1300, 0.5) * 0.7 + thump(0.5) * 0.8, 0.8)
    add("ability.blade_fall", "ability/blade_fall", reverb(x, 0.8, 0.25, rng))

    rng = np.random.default_rng(83)
    d = 0.8
    t = times(d)
    x = bandpass(white(d, rng), 2000, 7500) * np.clip(t / 0.3, 0, 1) * (t < 0.31) * 0.6
    place(x, partials([3135, 4700, 6270], [1, .6, .4], [.4, .25, .15], 0.5, rng), 0.31)
    add("ability.katana_draw", "ability/katana_draw", reverb(x, 0.7, 0.25, rng, 9000))

    rng = np.random.default_rng(84)
    d = 1.3
    x = whoosh(rng, d, 500, 1800, 0.4, 0.9, 0.0) * 0.6
    for _ in range(9):
        f = rng.choice([1568, 1760, 2093, 2349, 2637, 3136])
        place(x, bell(rng, f, 0.4, 0.25, 0.25), rng.uniform(0.0, 1.0))
    add("ability.petals", "ability/petals", reverb(x, 0.9, 0.3, rng))

    rng = np.random.default_rng(85)
    add("ability.war_cry", "ability/war_cry", reverb(roar(rng, 1.2, 130), 0.9, 0.25, rng))

    rng = np.random.default_rng(86)
    d = 1.6
    x = np.zeros(n_of(d))
    for at in (0.0, 0.22, 0.75, 0.97):
        place(x, thump(0.25, 80, 40, 18), at)
    place(x, roar(rng, 1.0, 85, drive=4.0) * 0.8, 0.45)
    add("ability.berserk", "ability/berserk", reverb(x, 0.8, 0.2, rng))

    rng = np.random.default_rng(91)
    add("weapon.throw", "weapon/throw", whoosh(rng, 0.24, 900, 3600, 0.3, 0.45, 0.05))

    rng = np.random.default_rng(92)
    d = 1.0
    t = times(d)
    x = bandpass(white(d, rng), 900, 4200) * (0.5 + 0.5 * np.sin(2 * np.pi * 22 * t)) * 0.8
    x += osc(700 + 200 * np.sin(2 * np.pi * 22 * t), d) * 0.25
    x *= np.clip(t / 0.08, 0, 1) * np.clip((d - t) / 0.2, 0, 1)
    add("weapon.chakram_spin", "weapon/chakram_spin", x)

    rng = np.random.default_rng(93)
    add("weapon.hammer_throw", "weapon/hammer_throw", whoosh(rng, 0.7, 180, 900, 0.4, 0.9, 0.0, body=0.6))
    rng = np.random.default_rng(94)
    d = 0.45
    add("weapon.hammer_catch", "weapon/hammer_catch", thump(d, 130, 55, 16) + partials([400, 870, 1350], [0.5, 0.35, 0.2], [0.2, 0.12, 0.08], d, rng))

    rng = np.random.default_rng(101)
    d = 4.5
    t = times(d)
    fs = [98, 155, 196, 247, 309, 392, 466, 587, 698, 880, 1047]
    x = np.zeros(n_of(d))
    for k, f in enumerate(fs):
        dec = 4.2 - k * 0.32
        bend = f * (1 + 0.02 * (1 - np.exp(-t * 12)))
        x += (0.9 ** k) * (osc(bend, d) + 0.7 * osc(bend * 1.003, d)) * np.exp(-t / dec)
    x += thump(d, 110, 55, 10) * 0.8
    add("block.gong", "block/gong", reverb(x * np.clip(t / 0.004, 0, 1), 2.5, 0.3, rng, 3500))

    rng = np.random.default_rng(102)
    d = 1.0
    x = np.zeros(n_of(d))
    for at in (0.0, 0.06, 0.14):
        place(x, highpass(white(0.02, rng), 2500) * env_exp(0.02, 200), at)
    place(x, thump(0.2, 140, 70, 25) * 0.6, 0.15)
    for k, f in enumerate([1046.5, 1318.5, 1568.0, 2093.0]):
        place(x, bell(rng, f, 0.7, 0.4, 0.35), 0.22 + 0.06 * k)
    add("block.coffer_unlock", "block/coffer_unlock", reverb(x, 0.8, 0.25, rng))

    rng = np.random.default_rng(103)
    d = 1.5
    t = times(d)
    creak = osc(260 + 60 * np.sin(2 * np.pi * (2 + 5 * t / d) * t), d, "saw")
    creak = bandpass(creak * (0.6 + 0.4 * (rng.random(len(t)) > 0.3)), 300, 1800) * env_hump(d, 0.4, 0.8) * 0.5
    x = creak + lowpass(brown(d, rng), 200) * env_hump(d, 0.3, 1.0) * 0.6
    place(x, thump(0.4, 110, 45, 10) + clang(rng, 300, 0.4) * 0.3, 1.05)
    add("block.vault_open", "block/vault_open", reverb(x, 0.9, 0.25, rng))

    rng = np.random.default_rng(104)
    d = 1.7
    t = times(d)
    gate = lowpass((rng.random(len(t)) > 0.5).astype(float), 45)
    x = bandpass(brown(d, rng), 150, 1500) * gate * env_hump(d, 0.5, 0.6) * 2.5
    place(x, thump(0.4, 100, 40, 10) * 1.2, 1.35)
    add("block.sarcophagus_open", "block/sarcophagus_open", reverb(x, 1.0, 0.3, rng, 2500))

    rng = np.random.default_rng(105)
    d = 1.1
    t = times(d)
    x = svf(osc(110, d, "saw") + osc(220.5, d, "saw"), 200 + 3500 * (t / d) ** 2, 0.4, "low") * np.clip(t / d, 0, 1) * 0.6
    place(x, highpass(white(0.25, rng), 2000) * env_exp(0.25, 12) * 0.5, 0.85)
    add("block.altar_charge", "block/altar_charge", reverb(x, 0.7, 0.25, rng))

    rng = np.random.default_rng(106)
    d = 2.8
    t = times(d)
    x = np.zeros(n_of(d))
    for f in (110, 164.8, 220, 329.6):
        x += osc(f, d) * np.clip(t / 0.5, 0, 1) * np.exp(-t * 0.9) * 0.2
    place(x, boom(rng, 1.8, 75, 25), 0.2)
    place(x, crackle(rng, 0.6, 30, 1500, 7000) * 0.6, 0.15)
    add("block.altar_summon", "block/altar_summon", reverb(x, 1.8, 0.3, rng))

    rng = np.random.default_rng(107)
    d = 0.9
    t = times(d)
    strokes = np.clip(np.sin(2 * np.pi * t / 0.45), 0, 1) ** 0.6
    x = bandpass(white(d, rng), 2500, 7500) * strokes * 0.8
    x += partials([2400, 3700], [0.15, 0.1], [0.3, 0.2], d, rng)
    add("block.whetstone", "block/whetstone", x)

    rng = np.random.default_rng(108)
    d = 1.3
    x = partials([1100, 2900, 4300], [1, .5, .3], [.6, .3, .2], d, rng) + thump(d, 160, 70, 18) * 0.5
    place(x, magic_sweep(rng, 0.9, 1200, 6500) * 0.5, 0.15)
    add("block.rune_forge", "block/rune_forge", reverb(x, 0.9, 0.3, rng))

    rng = np.random.default_rng(109)
    d = 1.1
    t = times(d)
    x = svf(white(d, rng), 200 + 2400 * np.clip(t / 0.15, 0, 1), 0.7, "low") * env_exp(d, 4, 0.05) * 1.2
    x += crackle(rng, d, 25, 800, 4000, 0.8) * 0.5
    add("block.brazier_ignite", "block/brazier_ignite", x)

    rng = np.random.default_rng(111)
    add("entity.warlord_roar", "entity/warlord_roar", reverb(roar(rng, 1.8, 68, drive=4.5), 1.4, 0.3, rng, 2500))
    rng = np.random.default_rng(112)
    d = 1.2
    t = times(d)
    x = svf(white(d, rng), 400 + 5000 * (t / d) ** 2, 0.3) * np.clip(t / d, 0, 1) * 0.6
    for f in (220, 277.2, 329.6):
        x += osc(f * (1 + 0.01 * np.sin(2 * np.pi * 5 * t)), d) * env_hump(d, 0.6, 1) * 0.15
    place(x, highpass(white(0.25, rng), 1500) * env_exp(0.25, 14), 0.95)
    add("entity.herald_cast", "entity/herald_cast", reverb(x, 1.0, 0.3, rng))
    rng = np.random.default_rng(113)
    add("entity.captain_taunt", "entity/captain_taunt", reverb(roar(rng, 0.7, 150, (650, 1300, 2700), 2.5), 0.5, 0.2, rng))

    # ------------------------------------------------------------- epic ability layers
    for i, seed in enumerate([201, 202]):
        rng = np.random.default_rng(seed)
        d = 2.8
        t = times(d)
        x = boom(rng, d, 95 - 10 * i, 24, 1.5, 1.3) * 1.3
        x += clang(rng, 130 + 25 * i, d, decays=(0.9, 0.6, 0.4, 0.25, 0.15), bright=0.6) * 0.35
        x += crackle(rng, d, 70, 300, 3500, 1.6) * 0.7
        x += lowpass(brown(d, rng), 160) * np.exp(-t * 1.2) * (1 + 0.5 * np.sin(2 * np.pi * 9 * t)) * 0.8
        add("ability.epic_impact", f"ability/epic_impact{i + 1}", reverb(saturate(x, 1.8), 2.0, 0.35, rng, 3000))

    for i, seed in enumerate([211, 212]):
        rng = np.random.default_rng(seed)
        d = 3.0
        t = times(d)
        x = highpass(white(d, rng), 1800) * env_exp(d, 45) * 1.6
        x += crackle(rng, d, 90, 1200, 9000, 3.0) * 1.2
        rumble = lowpass(brown(d, rng), 220 + 80 * i) * env_hump(d, 0.18, 0.6)
        rumble *= 0.7 + 0.3 * np.abs(lowpass(rng.normal(0, 1, len(t)), 6) * 8).clip(0, 1)
        x += rumble * 1.4 + boom(rng, d, 70, 30, 0.8, 0.0) * 0.8
        add("ability.lightning_strike", f"ability/lightning_strike{i + 1}", reverb(saturate(x, 1.5), 2.2, 0.35, rng, 4000))

    for i, seed in enumerate([221, 222, 223]):
        rng = np.random.default_rng(seed)
        d = 0.75
        t = times(d)
        x = whoosh(rng, d, 1100 + 150 * i, 4800, 0.22, 0.35, 0.18) * 1.1
        x += osc(2000 * np.exp(-t * 1.6) + 700, d) * env_hump(d, 0.15, 1.5) * 0.25
        x += partials([2900 + 200 * i, 4300, 6100], [0.5, 0.3, 0.2], [0.3, 0.2, 0.12], d, rng) * 0.4
        add("ability.blade_throw", f"ability/blade_throw{i + 1}", reverb(x, 0.5, 0.2, rng))

    rng = np.random.default_rng(231)
    d = 2.6
    t = times(d)
    x = np.zeros(n_of(d))
    for f in (196.0, 293.7, 392.0, 587.3):
        v = osc(f * (1 + 0.006 * np.sin(2 * np.pi * 5.2 * t + f)), d, "saw")
        x += lowpass(v, 2400) * 0.14
    x *= env_hump(d, 0.45, 0.8)
    x += magic_sweep(rng, d, 600, 7000) * 0.5
    for k in range(9):
        place(x, clang(rng, rng.uniform(1600, 3200), 0.5, decays=(0.22, 0.15, 0.1, 0.07, 0.05)) * 0.35, 0.9 + k * 0.13)
    place(x, boom(rng, 1.4, 80, 30, 0.8, 0.8) * 0.9, 1.15)
    add("ability.blade_storm", "ability/blade_storm", reverb(x, 1.8, 0.35, rng))

    for i, seed in enumerate([241, 242]):
        rng = np.random.default_rng(seed)
        d = 2.2
        t = times(d)
        swell = svf(white(d, rng), 300 + 3000 * (t / d) ** 3, 0.7, "low") * np.clip(t / (d * 0.55), 0, 1) ** 3
        swell[int(0.55 * len(t)):] *= np.exp(-(t[int(0.55 * len(t)):] - 0.55 * d) * 7)
        x = swell * 1.2 + roar(rng, d, 52 + 6 * i, (420, 800, 1700), 5.0) * 0.6
        x += (osc(41, d) * 0.6 + osc(58, d) * 0.4) * env_hump(d, 0.5, 0.7)
        x += (osc(311, d) + osc(440, d)) * env_hump(d, 0.6, 1.5) * 0.08
        add("ability.dark_power", f"ability/dark_power{i + 1}", reverb(saturate(x, 2.0), 1.8, 0.4, rng, 1600))

    for i, seed in enumerate([251, 252]):
        rng = np.random.default_rng(seed)
        d = 1.2
        t = times(d) / d
        center = 300 + 2600 * np.sin(np.pi * t) ** 2
        x = svf(white(d, rng), center, 0.25) * env_hump(1.2, 0.45, 1.0) * 1.5
        x += svf(white(d, rng), center * 2.2, 0.15) * env_hump(1.2, 0.5, 1.6) * 0.5
        x += whoosh(rng, d, 500, 2600, 0.4, 0.7, 0.04, 0.4) * 0.6
        add("ability.wind_rush", f"ability/wind_rush{i + 1}", reverb(x, 0.9, 0.25, rng))

    rng = np.random.default_rng(261)
    d = 1.8
    t = times(d)
    x = svf(white(d, rng), 250 + 1800 * np.exp(-t * 2.5), 0.5, "low") * env_exp(d, 2.2, 0.04) * 1.6
    x += crackle(rng, d, 80, 600, 4500, 1.2) * 0.8 + boom(rng, d, 75, 35, 0.9, 0.6) * 0.7
    x += roar(rng, d, 85, (500, 950, 2100), 4.0) * 0.35
    add("ability.inferno", "ability/inferno", reverb(saturate(x, 1.8), 1.3, 0.3, rng, 3500))

    for i, seed in enumerate([271, 272]):
        rng = np.random.default_rng(seed)
        d = 3.6
        t = times(d)
        chord = np.zeros(n_of(d))
        for f in ((65.4, 98.0, 130.8, 155.6) if i == 0 else (73.4, 110.0, 146.8, 174.6)):
            chord += osc(f * (1 + 0.004 * np.sin(2 * np.pi * 0.7 * t + f)), d, "saw") * 0.3
        chord = svf(chord, 200 + 2200 * np.exp(-t * 1.4), 0.4, "low") * env_exp(d, 0.9, 0.01)
        x = saturate(chord * 1.6, 2.5) * 0.9
        x += boom(rng, d, 110, 26, 1.6, 1.5) * 1.2
        x += clang(rng, 98, d, decays=(1.4, 0.9, 0.6, 0.4, 0.25), bright=0.8) * 0.3
        x += crackle(rng, d, 60, 400, 6000, 1.8) * 0.5
        add("ultimate.release", f"ability/ultimate_release{i + 1}", reverb(x, 2.6, 0.38, rng, 3500))

    rng = np.random.default_rng(281)
    d = 2.2
    t = times(d)
    rise = (t / d) ** 2
    x = svf(white(d, rng), 200 + 6500 * rise, 0.3) * (0.3 + 0.9 * rise)
    x += osc(110 * 2 ** (3 * t / d), d, "saw") * 0.12 * (0.4 + rise) * (1 + 0.5 * np.sin(2 * np.pi * (4 + 18 * rise) * t))
    x += osc(55, d) * 0.4 * rise
    add("ultimate.charge", "ability/ultimate_charge", lowpass(x, 9000))

    rng = np.random.default_rng(291)
    d = 0.5
    x = whoosh(rng, d, 600, 3200, 0.25, 0.4, 0.1, 0.3) + crackle(rng, d, 18, 2000, 8000, 2.5) * 0.6
    place(x, thump(0.2, 200, 80, 25) * 0.6, 0.0)
    add("armor.double_jump", "armor/double_jump", x)

    rng = np.random.default_rng(292)
    d = 1.1
    t = times(d)
    x = (osc(220, d) + osc(331, d) * 0.6) * (0.5 + 0.5 * np.sin(2 * np.pi * 26 * t)) * env_exp(d, 3.0, 0.01) * 0.6
    x += bell(rng, 1760, d, 0.5, 0.5) + thump(d, 160, 60, 10) * 0.8 + crackle(rng, d, 25, 1500, 7000, 2.0) * 0.5
    add("armor.barrier", "armor/barrier", reverb(x, 0.9, 0.3, rng))
    add("music.boss", "music/boss_battle", boss_theme())
    return lib


def boss_theme():
    """A 27-second loop at 140 bpm in D minor: war drums, a driving low-string ostinato, brass stabs and a choir pad."""
    rng = np.random.default_rng(301)
    bpm = 140.0
    beat = 60.0 / bpm
    bars = 16
    d = bars * 4 * beat
    out = np.zeros(n_of(d))
    t_all = times(d)

    def note(f, dur, kind="saw", cutoff=1800, amp=0.2, attack=0.01, release=0.08):
        n = n_of(dur)
        t = np.arange(n) / SR
        x = osc(f * (1 + 0.003 * np.sin(2 * np.pi * 5 * t)), dur, kind) + 0.5 * osc(f * 1.005, dur, kind)
        env = np.clip(t / attack, 0, 1) * np.clip((dur - t) / release, 0, 1)
        return lowpass(x * env, cutoff) * amp

    # D minor progression, one chord per 2 bars: Dm Bb F C | Dm Bb Gm A
    roots = [73.42, 58.27, 87.31, 65.41, 73.42, 58.27, 49.0, 55.0]
    thirds = [1.189, 1.26, 1.26, 1.26, 1.189, 1.26, 1.189, 1.26]
    for c, root in enumerate(roots):
        start = c * 8 * beat
        # Ostinato: driving eighth notes on root/octave.
        for k in range(16):
            f = root * (2 if k % 4 == 3 else 1)
            place(out, note(f, beat * 0.45, cutoff=900, amp=0.16), start + k * beat / 2)
        # Choir/string pad.
        pad = sum(note(root * 2 * r, 8 * beat, "saw", 1400, 0.05, attack=0.6, release=0.6) for r in (1, thirds[c], 1.498))
        place(out, pad, start)
        # Brass stabs on beats 1 and the "and" of 3 of the first bar of each chord.
        for off in (0, 2.5 * beat, 4 * beat):
            stab = sum(note(root * 4 * r, beat * 0.6, "saw", 2600, 0.07, attack=0.02, release=0.2) for r in (1, thirds[c], 1.498))
            place(out, saturate(stab * 2, 1.5) * 0.5, start + off)
    # War drums: big taiko hits and a snare-ish crack.
    for b in range(bars * 4):
        at = b * beat
        if b % 4 in (0, 2) or (b % 8 == 7):
            place(out, thump(0.6, 120, 45, 7) * 0.9 + lowpass(white(0.6, rng), 400) * env_exp(0.6, 9) * 0.3, at)
        if b % 4 == 3:
            place(out, thump(0.4, 180, 70, 10) * 0.5, at + beat / 2)
        if b % 2 == 1:
            place(out, bandpass(white(0.25, rng), 900, 5000) * env_exp(0.25, 18) * 0.35, at)
        if b % 16 == 15:
            for k in range(4):
                place(out, thump(0.3, 160, 60, 12) * 0.6, at + k * beat / 4)
    out = reverb(out, 1.4, 0.22, rng, 4000)[: n_of(d)]
    # Make the loop seamless: crossfade the tail into the head.
    fade = n_of(0.05)
    out[:fade] *= np.linspace(0, 1, fade)
    out[-fade:] *= np.linspace(1, 0, fade)
    return saturate(out * 1.2, 1.2)



SUBTITLES = {
    "weapon.swing": "Weapon swings",
    "weapon.swing_heavy": "Heavy weapon swings",
    "weapon.hit": "Weapon hits",
    "weapon.hit_metal": "Steel clangs",
    "weapon.parry": "Attack blocked",
    "weapon.perfect_parry": "Perfect parry",
    "weapon.guard_break": "Guard breaks",
    "player.dodge": "Player dodges",
    "ability.cast": "Ability unleashed",
    "ability.dash": "Dash",
    "ability.ready": "Ability ready",
    "ultimate.ready": "Ultimate ready",
    "ultimate.cast": "Ultimate unleashed",
    "ability.shockwave": "Shockwave",
    "ability.ground_crack": "Ground cracks",
    "ability.rumble": "Ground rumbles",
    "ability.zap": "Lightning crackles",
    "ability.thunder": "Thunder",
    "ability.frost": "Ice shatters",
    "ability.void": "Void surges",
    "ability.void_hum": "Void hums",
    "ability.blood": "Blood splatters",
    "ability.smoke": "Smoke bursts",
    "ability.slash_wave": "Slash wave",
    "ability.blade_fall": "Blades fall",
    "ability.katana_draw": "Blade drawn",
    "ability.petals": "Petals swirl",
    "ability.war_cry": "War cry",
    "ability.berserk": "Berserker rage",
    "weapon.throw": "Weapon thrown",
    "weapon.chakram_spin": "Chakram whirs",
    "weapon.hammer_throw": "Hammer thrown",
    "weapon.hammer_catch": "Hammer caught",
    "block.gong": "Gong rings",
    "block.coffer_unlock": "Coffer unlocks",
    "block.vault_open": "Vault opens",
    "block.sarcophagus_open": "Sarcophagus grinds open",
    "block.altar_charge": "Altar charges",
    "block.altar_summon": "Something answers the altar",
    "block.whetstone": "Blade sharpened",
    "block.rune_forge": "Rune forged",
    "block.brazier_ignite": "Brazier ignites",
    "entity.warlord_roar": "Warlord roars",
    "entity.herald_cast": "Storm Herald casts",
    "entity.captain_taunt": "Bandit Captain taunts",
    "ability.epic_impact": "Earth-shattering impact",
    "ability.lightning_strike": "Lightning strikes",
    "ability.blade_throw": "Blade flies",
    "ability.blade_storm": "Blades sing",
    "ability.dark_power": "Dark power surges",
    "ability.wind_rush": "Wind rushes",
    "ability.inferno": "Fire roars",
    "ultimate.release": "Ultimate unleashed",
    "ultimate.charge": "Power gathers",
    "armor.double_jump": "Thunder Step",
    "armor.barrier": "Static Barrier absorbs a hit",
    "music.boss": "Battle music",
}


def main():
    lib = build()
    sounds_json = {}
    for event, files in lib.items():
        entries = []
        for name, data in files:
            path = os.path.join(OUT, name + ".ogg")
            os.makedirs(os.path.dirname(path), exist_ok=True)
            sf.write(path, finish(np.asarray(data, dtype=np.float64)), SR, format="OGG", subtype="VORBIS")
            entries.append("steelstorm:" + name)
        sounds_json[event] = {"sounds": entries, "subtitle": "subtitles.steelstorm." + event}
        if event.startswith("music."):
            sounds_json[event] = {"sounds": [{"name": e, "stream": True} for e in entries]}
    with open(os.path.join(ROOT, "sounds.json"), "w") as f:
        json.dump(sounds_json, f, indent=2)
    missing = set(lib) - set(SUBTITLES)
    if missing:
        raise SystemExit(f"Missing subtitles for {sorted(missing)}")
    print(f"Wrote {sum(len(v) for v in lib.values())} sounds for {len(lib)} events")


if __name__ == "__main__":
    main()
