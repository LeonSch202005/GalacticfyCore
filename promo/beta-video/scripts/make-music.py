#!/usr/bin/env python3
"""
make-music.py – prozeduraler, lizenzfreier Musik-Bed für das Galacticfy-Promo.

Alles ist hier selbst synthetisiert (numpy, additive/band-limitierte Oszillatoren,
keine Samples, keine Downloads) -> 100 % eigene Musik, frei verwendbar.

    python3 -I scripts/make-music.py                       # Default-Timing (siehe unten)
    python3 -I scripts/make-music.py --drop 6.8 --end 38.3 # Drop + End-Card-Hit neu setzen

Aufbau (C-Moll, i-VI-III-VII = Cm-Ab-Eb-Bb, ~126 BPM, four-on-the-floor):
  0 s .. drop        Intro: Supersaw-Pad + Pluck-Arp hinter einem sich öffnenden Lowpass,
                     gedämpfte Kick ab 2 Takten vor dem Drop, Riser + Snare-Roll, 1/16 Pause
  drop .. end-bd     Drop A (8 Takte) / Drop B: Kick, Clap 2&4, Hats, Offbeat-/Rolling-Bass,
                     gesidechaintes Supersaw-Pad, Pluck-Arpeggio mit Ping-Pong-Delay
  end-bd .. end      Breakdown (4-7 Schläge): Drums raus, Highpass-Sweep, Riser, Snare-Roll
  end ..             End-Card-Hit (Crash) + Groove weiter, dann ausdünnen, Schlussakkord,
                     Fade-out (Datei ist bewusst länger als das Video).

--drop = Sekunde, auf der der Drop landet (= SEGMENTS[1].from im Video),
--end  = Sekunde der End-Card (= MAIN_SECONDS). Das Tempo wird im Bereich
--bpm-min..--bpm-max so gewählt, dass zwischen drop und end eine ganze Zahl Schläge liegt
(beide Punkte liegen also exakt auf dem Beat-Raster).

Mix: Ziel -17 LUFS integriert, True Peak < -1 dBTP (vor MP3 -1.6 dBTP), kein DC.
Ausgabe: public/music/bed.mp3 (192 kbit/s, 48 kHz Stereo). Deterministisch (fester Seed).
"""
import argparse
import json
import math
import os
import subprocess
import tempfile
import wave

import numpy as np

SR = 48000
HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT = os.path.normpath(os.path.join(HERE, '..'))


# ======================================================================== helpers
def midi_hz(m):
    return 440.0 * 2 ** ((m - 69) / 12)


def smoothstep(x):
    x = np.clip(x, 0.0, 1.0)
    return x * x * (3 - 2 * x)


def pan_gains(p):
    a = (p + 1) * np.pi / 4
    return math.cos(a) * math.sqrt(2), math.sin(a) * math.sqrt(2)  # 0 dB in the centre


def fade_edges(x, fi, fo):
    """Raised-cosine fade in/out (samples) on the last axis."""
    n = x.shape[-1]
    fi = min(fi, n); fo = min(fo, n)
    if fi > 1:
        x[..., :fi] *= 0.5 - 0.5 * np.cos(np.pi * np.arange(fi) / fi)
    if fo > 1:
        x[..., n - fo:] *= 0.5 + 0.5 * np.cos(np.pi * np.arange(1, fo + 1) / fo)
    return x


def static_filter(x, gain_fn):
    """Zero-phase static filter in the frequency domain (x: (..., n))."""
    n = x.shape[-1]
    m = 1 << (n + 4096 - 1).bit_length()
    X = np.fft.rfft(x, m, axis=-1)
    f = np.fft.rfftfreq(m, 1 / SR)
    return np.fft.irfft(X * gain_fn(f), m, axis=-1)[..., :n]


def lp_mag(f, fc, order=2):
    return 1 / np.sqrt(1 + (np.asarray(f) / fc) ** (2 * order))


def hp_mag(f, fc, order=2):
    f = np.maximum(np.asarray(f, dtype=float), 1e-6)
    return 1 / np.sqrt(1 + (fc / f) ** (2 * order))


def stft_filter(x, gain, n=2048, hop=256):
    """Time-varying zero-phase filter. x: (ch, len). gain(times[:,None], freqs[None,:])."""
    out = np.zeros_like(x, dtype=float)
    win = 0.5 - 0.5 * np.cos(2 * np.pi * np.arange(n) / n)
    pad = n
    L = x.shape[-1]
    for c in range(x.shape[0]):
        xp = np.concatenate([np.zeros(pad), x[c], np.zeros(pad + n)])
        nfr = (len(xp) - n) // hop + 1
        times = (hop * np.arange(nfr) + n / 2 - pad) / SR
        freqs = np.fft.rfftfreq(n, 1 / SR)
        G = gain(times[:, None], freqs[None, :])
        y = np.zeros(len(xp)); norm = np.zeros(len(xp))
        step = 512  # frames per chunk (memory)
        for s in range(0, nfr, step):
            e = min(nfr, s + step)
            idx = np.arange(n)[None, :] + hop * np.arange(s, e)[:, None]
            Y = np.fft.irfft(np.fft.rfft(xp[idx] * win, axis=1) * G[s:e], n=n, axis=1) * win
            for k in range(e - s):
                o = (s + k) * hop
                y[o:o + n] += Y[k]
                norm[o:o + n] += win ** 2
        out[c] = (y / np.maximum(norm, 1e-9))[pad:pad + L]
    return out


def fft_conv(a, b):
    n = len(a) + len(b) - 1
    m = 1 << (n - 1).bit_length()
    return np.fft.irfft(np.fft.rfft(a, m) * np.fft.rfft(b, m), m)[:n]


def running_min(x, w):
    """Sliding minimum, centred window of length w (odd), van Herk/Gil-Werman, O(n)."""
    r = w // 2
    xp = np.concatenate([np.full(r, np.inf), x, np.full(r + w, np.inf)])
    nb = len(xp) // w
    xp = xp[:nb * w].reshape(nb, w)
    pre = np.minimum.accumulate(xp, axis=1).ravel()
    suf = np.minimum.accumulate(xp[:, ::-1], axis=1)[:, ::-1].ravel()
    n = len(x)
    i = np.arange(n)
    return np.minimum(suf[i], pre[i + w - 1])


def moving_avg(x, w):
    c = np.concatenate([[0.0], np.cumsum(x)])
    r = w // 2
    i = np.arange(len(x))
    lo = np.clip(i - r, 0, len(x)); hi = np.clip(i + r + 1, 0, len(x))
    return (c[hi] - c[lo]) / (hi - lo)


def true_peak_db(st):
    tp = 0.0
    for ch in st:
        n = len(ch)
        m = 1 << (n + 256).bit_length()
        X = np.fft.rfft(ch, m)
        up = np.fft.irfft(X, 4 * m) * 4
        tp = max(tp, float(np.max(np.abs(up))))
    return 20 * math.log10(tp + 1e-12)


def lufs(st):
    """ITU-R BS.1770-4 integrated loudness (K-weighting applied in the frequency domain)."""
    b1 = [1.53512485958697, -2.69169618940638, 1.19839281085285]; a1 = [1, -1.69065929318241, 0.73248077421585]
    b2 = [1.0, -2.0, 1.0]; a2 = [1, -1.99004745483398, 0.99007225036621]

    def H(b, a, f):
        z = np.exp(-2j * np.pi * f / SR)
        return (b[0] + b[1] * z + b[2] * z * z) / (a[0] + a[1] * z + a[2] * z * z)
    kw = static_filter(st.astype(float), lambda f: H(b1, a1, f) * H(b2, a2, f))
    blk, hop = int(0.4 * SR), int(0.1 * SR)
    nb = (kw.shape[1] - blk) // hop + 1
    c = np.concatenate([np.zeros((2, 1)), np.cumsum(kw ** 2, axis=1)], axis=1)
    starts = np.arange(nb) * hop
    z = (c[:, starts + blk] - c[:, starts]) / blk
    zs = z.sum(axis=0)
    l = -0.691 + 10 * np.log10(zs + 1e-20)
    g1 = zs[l > -70]
    lr = -0.691 + 10 * np.log10(g1.mean()) - 10
    g2 = zs[(l > -70) & (l > lr)]
    return -0.691 + 10 * np.log10(g2.mean())


# ======================================================================== instruments
class Synth:
    def __init__(self, seed):
        self.rng = np.random.default_rng(seed)
        self.cache = {}

    def noise(self, n, gain_fn=None):
        x = self.rng.standard_normal(n)
        return static_filter(x, gain_fn) if gain_fn else x

    # ---------------- drums
    def kick(self):
        t = np.arange(int(0.42 * SR)) / SR
        f = 48 + 92 * np.exp(-t / 0.032) + 420 * np.exp(-t / 0.0028)
        ph = 2 * np.pi * np.cumsum(f) / SR
        amp = (1 - np.exp(-t / 0.0004)) * np.where(t < 0.04, 1.0, np.exp(-(t - 0.04) / 0.11))
        x = np.tanh(1.9 * np.sin(ph) * amp) / np.tanh(1.9)
        click = self.noise(len(t), lambda f_: hp_mag(f_, 2500) * lp_mag(f_, 9000)) * np.exp(-t / 0.0018) * 0.12
        return fade_edges(x + click, 4, int(0.03 * SR))

    def clap(self):
        t = np.arange(int(0.42 * SR)) / SR
        band = lambda f_: hp_mag(f_, 900, 2) * lp_mag(f_, 5200, 2) * (1 + 0.8 * np.exp(-0.5 * (np.log2(np.maximum(f_, 1) / 1500) / 0.5) ** 2))
        n1 = self.noise(len(t), band); n2 = self.noise(len(t), band); n3 = self.noise(len(t), band)
        env = np.zeros(len(t))
        for d in (0.0, 0.0095, 0.019):
            tt = np.clip(t - d, 0, None)
            env += (t >= d) * np.exp(-tt / 0.0042) * (1 - np.exp(-tt / 0.0003))
        tt = np.clip(t - 0.027, 0, None)
        env += (t >= 0.027) * 1.1 * np.exp(-tt / 0.06) * (1 - np.exp(-tt / 0.0005))
        room = (t >= 0.027) * 0.22 * np.exp(-tt / 0.16)
        body_f = 185 * (1 + 0.15 * np.exp(-t / 0.01))
        body = np.sin(2 * np.pi * np.cumsum(body_f) / SR) * np.exp(-t / 0.045) * (1 - np.exp(-t / 0.0006)) * 0.38
        wires = self.noise(len(t), lambda f_: hp_mag(f_, 4500) * lp_mag(f_, 12000)) * np.exp(-t / 0.075) * 0.18
        mono = n1 * env + body + wires
        st = np.stack([mono + n2 * room, mono + n3 * room])
        return fade_edges(st, 4, int(0.05 * SR))

    def snare(self):
        t = np.arange(int(0.25 * SR)) / SR
        nz = self.noise(len(t), lambda f_: hp_mag(f_, 700) * lp_mag(f_, 9000))
        body = np.sin(2 * np.pi * 200 * t) * np.exp(-t / 0.035) * 0.5
        x = (nz * np.exp(-t / 0.055) + body) * (1 - np.exp(-t / 0.0005))
        return fade_edges(x, 4, int(0.04 * SR))

    def hat(self, open_=False):
        dur = 0.38 if open_ else 0.09
        t = np.arange(int(dur * SR)) / SR
        nz = self.noise(len(t), lambda f_: hp_mag(f_, 7200, 3) * lp_mag(f_, 15500, 2))
        metal = np.zeros(len(t))
        for fr in (5230.0, 6810.0, 8270.0, 9690.0, 11370.0):
            metal += np.sin(2 * np.pi * fr * t + self.rng.random() * 6.28)
        metal = static_filter(metal, lambda f_: hp_mag(f_, 6000))
        tau = 0.15 if open_ else 0.022
        env = (1 - np.exp(-t / 0.0004)) * np.exp(-t / tau)
        x = (nz + 0.07 * metal) * env
        return fade_edges(x, 4, int(0.03 * SR))

    def crash(self):
        t = np.arange(int(2.6 * SR)) / SR
        g = lambda f_: hp_mag(f_, 3200, 2) * lp_mag(f_, 12500, 2)
        env = (1 - np.exp(-t / 0.002)) * (0.55 * np.exp(-t / 0.12) + 0.45 * np.exp(-t / 0.95))
        st = np.stack([self.noise(len(t), g), self.noise(len(t), g)]) * env
        return fade_edges(st, 4, int(0.3 * SR))

    def riser(self, dur, f_lo=300.0, f_hi=9000.0, gap=0.0):
        n = int(dur * SR)
        t = np.arange(n) / SR
        u = np.clip(t / max(dur - gap, 1e-3), 0, 1)

        def g(tt, ff):
            uu = np.clip(tt / max(dur - gap, 1e-3), 0, 1)
            fc = f_lo * (f_hi / f_lo) ** (uu ** 1.4)
            return np.exp(-0.5 * (np.log2(np.maximum(ff, 1) / fc) / 1.0) ** 2)
        common = self.rng.standard_normal(n)
        st = np.stack([0.55 * common + 0.83 * self.rng.standard_normal(n),
                       0.55 * common + 0.83 * self.rng.standard_normal(n)])
        st = stft_filter(st, g, n=1024, hop=128)
        trem = 1 - 0.22 * (0.5 + 0.5 * np.sin(2 * np.pi * np.cumsum(4 + 20 * u ** 1.6) / SR))
        st *= (u ** 2.2) * trem
        stop = np.clip((t - (dur - gap)) / 0.012, 0, 1)
        st *= 0.5 + 0.5 * np.cos(np.pi * stop)
        return fade_edges(st, int(0.02 * SR), 8)

    # ---------------- tonal (all additive => no aliasing)
    def supersaw_chord(self, notes, n, attack, release, fc, sustain_to=0.82):
        key = ('pad', tuple(notes), n, attack, release, fc, sustain_to)
        if key in self.cache:
            return self.cache[key]
        nt = n + int(release * SR)
        t = np.arange(nt) / SR
        st = np.zeros((2, nt))
        det = [-18, -11, -5, 0, 5, 11, 17]
        gains = [0.55, 0.72, 0.9, 1.0, 0.9, 0.72, 0.55]
        pans = [-0.85, 0.6, -0.3, 0.0, 0.3, -0.6, 0.85]
        for m in notes:
            for d, gv, p in zip(det, gains, pans):
                f = midi_hz(m) * 2 ** (d / 1200)
                z = np.exp(1j * (2 * np.pi * f * t + self.rng.random() * 2 * np.pi))
                zk = z.copy()
                acc = np.zeros(nt)
                K = int(min(9000.0, 20000.0) // f)
                for k in range(1, K + 1):
                    a = (1 / k) * lp_mag(k * f, fc) * hp_mag(k * f, 160) * (1 if k % 2 else -1)
                    acc += a * zk.imag
                    zk *= z
                gl, gr = pan_gains(p)
                st[0] += acc * gv * gl; st[1] += acc * gv * gr
        st /= (len(notes) * 4.0)
        env = np.ones(nt)
        na = max(2, int(attack * SR))
        env[:na] = 0.5 - 0.5 * np.cos(np.pi * np.arange(na) / na)
        env[:n] *= 1 - (1 - sustain_to) * smoothstep(np.arange(n) / n)
        env[n:] = sustain_to * (0.5 + 0.5 * np.cos(np.pi * np.arange(nt - n) / max(1, nt - n)))
        st *= env
        self.cache[key] = st
        return st

    def pluck(self, m):
        key = ('pluck', m)
        if key in self.cache:
            return self.cache[key]
        dur = 0.34
        t = np.arange(int(dur * SR)) / SR
        st = np.zeros((2, len(t)))
        for d, p in ((-7, -0.35), (7, 0.35)):
            f = midi_hz(m) * 2 ** (d / 1200)
            z = np.exp(1j * (2 * np.pi * f * t + self.rng.random() * 6.28))
            zk = z.copy(); acc = np.zeros(len(t))
            for k in range(1, int(10000 // f) + 1):
                rate = 3.5 + 9.0 * (k * f / 1000.0)
                acc += (1 if k % 2 else -1) / k * np.exp(-t * rate) * zk.imag
                zk *= z
            gl, gr = pan_gains(p)
            st[0] += acc * gl; st[1] += acc * gr
        st *= (1 - np.exp(-t / 0.0012)) * 0.5
        st = fade_edges(st, 2, int(0.03 * SR))
        self.cache[key] = st
        return st

    def bass(self, m, dur):
        key = ('bass', m, round(dur, 4))
        if key in self.cache:
            return self.cache[key]
        rel = 0.03
        t = np.arange(int((dur + rel) * SR)) / SR
        f = midi_hz(m)
        fc = 330 + 1300 * np.exp(-t / 0.06)
        z = np.exp(1j * 2 * np.pi * f * t)
        zk = z.copy(); acc = np.zeros(len(t))
        for k in range(1, int(3200 // f) + 1):
            acc += (1 if k % 2 else -1) / k * lp_mag(k * f, fc, 2) * zk.imag
            zk *= z
        acc += 0.25 * np.sin(2 * np.pi * f * t)  # sub reinforcement
        env = np.ones(len(t))
        env *= 1 - np.exp(-t / 0.0015)
        n_on = int(dur * SR)
        env[n_on:] *= 0.5 + 0.5 * np.cos(np.pi * np.arange(len(t) - n_on) / max(1, len(t) - n_on))
        x = np.tanh(1.4 * acc * env) / np.tanh(1.4)
        x = fade_edges(x, 2, 16)
        self.cache[key] = x
        return x


def make_ir(rng, dur, t60, predelay=0.022):
    t = np.arange(int(dur * SR)) / SR
    out = []
    for _ in range(2):
        nz = rng.standard_normal(len(t)) * np.exp(-6.91 * t / t60)
        nz = stft_filter(nz[None], lambda tt, ff: lp_mag(ff, 9000 * (1800 / 9000) ** np.clip(tt / dur, 0, 1), 2) * hp_mag(ff, 220),
                         n=1024, hop=128)[0]
        nz = fade_edges(nz, 16, int(0.1 * SR))
        nz = np.concatenate([np.zeros(int(predelay * SR)), nz])
        out.append(nz / np.sqrt(np.sum(nz ** 2)))
    return out


# ======================================================================== arrangement
CHORDS = [  # C minor: i - VI - III - VII
    dict(name='Cm', pad=[60, 63, 67, 72], bass=36, arp=[72, 75, 79, 84]),
    dict(name='Ab', pad=[60, 63, 68, 72], bass=32, arp=[68, 72, 75, 80]),
    dict(name='Eb', pad=[58, 63, 67, 70], bass=39, arp=[67, 70, 75, 79]),
    dict(name='Bb', pad=[58, 62, 65, 70], bass=34, arp=[65, 70, 74, 77]),
]


class Bus:
    """Stereo buffer covering [t0, t1) of the song (only what is needed, to save memory)."""

    def __init__(self, t0, t1):
        self.t0 = t0
        self.n0 = int(round(t0 * SR))
        self.x = np.zeros((2, int(round(t1 * SR)) - self.n0))

    def add(self, sig, t, gain=1.0, pan=None):
        s = int(round(t * SR)) - self.n0
        sig = np.atleast_2d(sig)
        if sig.shape[0] == 1:
            if pan is None:
                sig = np.vstack([sig, sig])
            else:
                gl, gr = pan_gains(pan)
                sig = np.vstack([sig * gl, sig * gr])
        elif pan is not None:
            gl, gr = pan_gains(pan)
            sig = sig * np.array([[gl], [gr]])
        a, b = max(0, s), min(self.x.shape[1], s + sig.shape[1])
        if b > a:
            self.x[:, a:b] += gain * sig[:, a - s:b - s]

    def times(self):
        return (self.n0 + np.arange(self.x.shape[1])) / SR


def choose_bpm(drop, end, lo, hi, target=126.0):
    span = end - drop
    best = None
    for k in range(int(math.ceil(span * lo / 60)), int(math.floor(span * hi / 60)) + 1):
        bpm = 60 * k / span
        if best is None or abs(bpm - target) < abs(best[0] - target):
            best = (bpm, k)
    if best is None:
        return target, None
    return best


def render(args):
    D, E = args.drop, args.end
    if args.bpm:
        bpm, k = args.bpm, round((E - D) * args.bpm / 60)
    else:
        bpm, k = choose_bpm(D, E, args.bpm_min, args.bpm_max)
    beat = 60.0 / bpm; bar = 4 * beat; s16 = beat / 4
    if k is None:
        k = int((E - D) / beat)
    bd_beats = 4 + (k % 4) if k >= 8 else k
    groove_bars = (k - bd_beats) // 4
    BD0 = E - bd_beats * beat
    tail_bars = args.tail_bars
    ring = 3.6
    total = E + tail_bars * bar + ring
    rng = np.random.default_rng(args.seed)
    syn = Synth(args.seed)

    kick = syn.kick(); clap = syn.clap(); snare = syn.snare()
    hat_c = syn.hat(False); hat_o = syn.hat(True); crash = syn.crash()

    intro = {k_: Bus(0, D + 1.5) for k_ in ('pad', 'arp', 'kick', 'hat')}
    main = {k_: Bus(0, total) for k_ in ('pad', 'arp', 'kick', 'clap', 'hat', 'crash', 'bass')}
    bdb = {k_: Bus(BD0 - 0.05, E + 1.5) for k_ in ('pad', 'arp')}
    fx = Bus(0, total)
    kick_times = []

    def vel(base, spread=0.08):
        return base * (1 + spread * (rng.random() * 2 - 1))

    def groove_bar(t0, ch, style, bars_left_in_phrase, beats=4, thin=False):
        c = CHORDS[ch]
        # drums
        for b in range(beats):
            tb = t0 + b * beat
            fill = (bars_left_in_phrase == 1 and b == beats - 1 and not thin)
            if not fill:
                main['kick'].add(kick, tb, 1.0); kick_times.append(tb)
            if b % 2 == 1 and not thin:
                main['clap'].add(clap, tb, vel(0.62, 0.04))
            if not thin:
                for s in range(4):
                    v = [0.42, 0.24, 0.0, 0.26][s]
                    if s == 2:
                        main['hat'].add(hat_o, tb + 2 * s16, vel(0.5 if style == 'A' else 0.68), pan=0.1)
                    else:
                        main['hat'].add(hat_c, tb + s * s16, vel(v, 0.15), pan=(-0.25 if s % 2 else 0.2))
            if fill:  # 16th snare/clap roll into the next phrase
                for s in range(4):
                    main['clap'].add(snare, tb + s * s16, 0.32 + 0.12 * s)
        # bass
        for b in range(beats):
            tb = t0 + b * beat
            if thin:
                continue
            if style == 'A':
                main['bass'].add(syn.bass(c['bass'], 1.6 * s16), tb + 2 * s16, 0.95)
            else:
                for s, oc in ((1, 0), (2, 0), (3, 12 if b == 3 else 0)):
                    main['bass'].add(syn.bass(c['bass'] + oc, 0.85 * s16), tb + s * s16, 0.82 if s != 2 else 0.95)
        # pad (sidechained later) – bright stab-sustain
        n = int(round(beats * beat * SR))
        main['pad'].add(syn.supersaw_chord(c['pad'], n, 0.006, 0.07, 4500 if style == 'A' else 6000), t0, 1.0)
        # arp
        pat = [0, 1, 2, 3] * 4 if style == 'A' else [0, 1, 2, 3, 2, 1, 2, 3, 0, 1, 2, 3, 2, 3, 2, 1]
        acc = [1.0, 0.62, 0.8, 0.62]
        for i in range(beats * 4):
            note = c['arp'][pat[i % 16]] + (12 if style == 'B' and i % 8 == 7 else 0)
            main['arp'].add(syn.pluck(note), t0 + i * s16, vel(0.55 * acc[i % 4], 0.05))

    # ------------------------------------------------------------- intro (0 .. D)
    n_intro = int(math.ceil(D / bar))
    for j in range(n_intro, 0, -1):
        t0 = D - j * bar; ch = (-j) % 4; c = CHORDS[ch]
        n = int(round(bar * SR))
        intro['pad'].add(syn.supersaw_chord(c['pad'], n, 0.12, 0.12, 5000, 0.9), t0, 1.0)
        if j <= 2:
            for i in range(16):
                intro['arp'].add(syn.pluck(c['arp'][i % 4]), t0 + i * s16, vel(0.55 * [1, .62, .8, .62][i % 4], 0.05))
            for b in range(4):
                if j == 1 and b >= 2:
                    continue
                intro['kick'].add(kick, t0 + b * beat, 1.0); kick_times.append(t0 + b * beat)
        if j == 1:
            for b in range(4):
                intro['hat'].add(hat_o, t0 + b * beat + 2 * s16, vel(0.42))
            # snare roll on beats 3-4 (16ths, last 16th left empty = mini gap before the drop)
            for s in range(7):
                fx.add(snare, t0 + 2 * beat + s * s16, 0.16 + 0.07 * s)
            r = syn.riser(bar, 350, 9000, gap=s16)
            fx.add(r, t0, 0.42)
            rc = crash[:, :int(beat * SR)][:, ::-1].copy()
            fx.add(fade_edges(rc, int(0.25 * beat * SR), int(0.004 * SR)), D - s16 - beat, 0.3)

    # ------------------------------------------------------------- drop A/B
    for b in range(groove_bars):
        t0 = D + b * bar
        style = 'A' if (b // 8) % 2 == 0 else 'B'
        if b % 8 == 0:
            main['crash'].add(crash, t0, 0.55 if b == 0 else 0.42)
        left = 8 - (b % 8)
        if b == groove_bars - 1:
            left = 0  # no fill right before the breakdown
        groove_bar(t0, b % 4, style, left)

    # ------------------------------------------------------------- breakdown (BD0 .. E)
    bdc = CHORDS[3]
    n = int(round(bd_beats * beat * SR))
    bdb['pad'].add(syn.supersaw_chord(bdc['pad'], n, 0.02, 0.05, 6000, 1.0), BD0, 1.0)
    for i in range(bd_beats * 4):
        bdb['arp'].add(syn.pluck(bdc['arp'][i % 4]), BD0 + i * s16, vel(0.5 * [1, .62, .8, .62][i % 4], 0.05))
    # snare roll: quarters -> 8ths -> 16ths, crescendo, last 16th empty
    hits = []
    for bb in range(bd_beats):
        rem = bd_beats - bb
        sub = 1 if rem > 3 else (2 if rem > 2 else 4)
        for s in range(sub):
            hits.append(BD0 + bb * beat + s * beat / sub)
    hits = [h for h in hits if h < E - s16 * 0.99]
    for i, h in enumerate(hits):
        fx.add(snare, h, 0.12 + 0.36 * (i / max(1, len(hits) - 1)) ** 1.3)
    fx.add(syn.riser(E - BD0, 300, 10000, gap=s16), BD0, 0.5)
    rc = crash[:, :int(1.5 * beat * SR)][:, ::-1].copy()
    fx.add(fade_edges(rc, int(0.4 * beat * SR), int(0.004 * SR)), E - s16 - 1.5 * beat, 0.32)

    # ------------------------------------------------------------- end card + outro
    main['crash'].add(crash, E, 0.6)
    for b in range(tail_bars):
        t0 = E + b * bar
        thin = b >= tail_bars - 2
        groove_bar(t0, b % 4, 'B', 8 - (b % 8) if b < tail_bars - 3 else 0, thin=thin)
    tend = E + tail_bars * bar
    main['kick'].add(kick, tend, 1.0); kick_times.append(tend)
    main['crash'].add(crash, tend, 0.45)
    main['pad'].add(syn.supersaw_chord(CHORDS[0]['pad'], int((ring - 0.6) * SR), 0.006, 0.6, 4200, 0.35), tend, 1.0)
    main['bass'].add(syn.bass(CHORDS[0]['bass'], 0.9), tend, 0.9)

    # ------------------------------------------------------------- sidechain
    tt = main['pad'].times()
    duck = np.zeros(len(tt))
    rel = 0.3
    for tk in kick_times:
        a = int(round(tk * SR)); b = min(len(tt), a + int(rel * SR))
        if b <= max(a, 0):
            continue
        tau = (np.arange(max(a, 0), b) - a) / SR
        d = (1 - smoothstep(tau / rel)) * np.clip(tau / 0.004, 0, 1)
        duck[max(a, 0):b] = np.maximum(duck[max(a, 0):b], d)

    def sc(bus, depth):
        d = duck[bus.n0:bus.n0 + bus.x.shape[1]]
        if len(d) < bus.x.shape[1]:
            d = np.concatenate([d, np.zeros(bus.x.shape[1] - len(d))])
        bus.x *= 1 - depth * d

    sc(main['pad'], 0.72); sc(main['bass'], 0.5); sc(main['arp'], 0.3)
    sc(intro['pad'], 0.35); sc(intro['arp'], 0.2)

    # ------------------------------------------------------------- arp delay (ping-pong, dotted 8th)
    def delay(bus, fb=0.33, taps=4):
        d = int(round(0.75 * beat * SR))
        x = bus.x; wet = np.zeros_like(x)
        mono = x.mean(axis=0)
        for i in range(1, taps + 1):
            g = fb ** i
            ch = (i + 1) % 2
            wet[ch, i * d:] += g * mono[:len(mono) - i * d]
        wet = static_filter(wet, lambda f: lp_mag(f, 3200, 2) * hp_mag(f, 300, 1))
        bus.x = x + 0.8 * wet

    for bus in (main['arp'], intro['arp'], bdb['arp']):
        delay(bus)

    # ------------------------------------------------------------- mix per route
    levels = dict(kick=0.85, clap=1.8, hat=0.62, crash=0.38, bass=0.5, pad=1.6, arp=1.1)
    sends = dict(clap=0.22, pad=0.16, arp=0.22, hat=0.05, crash=0.0, kick=0.0, bass=0.0)
    total_n = int(round(total * SR))
    dry = np.zeros((2, total_n)); send = np.zeros((2, total_n))

    def route(buses, filt=None):
        n0 = next(iter(buses.values())).n0
        ln = next(iter(buses.values())).x.shape[1]
        d = np.zeros((2, ln)); s = np.zeros((2, ln))
        for name, bus in buses.items():
            d += levels[name] * bus.x
            s += levels[name] * sends[name] * bus.x
        if filt is not None:
            d = stft_filter(d, filt); s = stft_filter(s, filt)
        a = max(0, n0); b = min(total_n, n0 + ln)
        dry[:, a:b] += d[:, a - n0:b - n0]; send[:, a:b] += s[:, a - n0:b - n0]

    def intro_lp(t, f):  # opens from ~280 Hz at 0 s to ~5 kHz at the drop, small resonance
        u = np.clip(t / D, 0, 1)
        fc = 280 * (5200 / 280) ** (u ** 1.35)
        res = 1 + 0.6 * np.exp(-0.5 * (np.log2(np.maximum(f, 1) / fc) / 0.25) ** 2)
        return lp_mag(f, fc, 2) * res

    def bd_hp(t, f):  # highpass sweep up during the breakdown (thins out), lowpass stays open
        u = np.clip((t - BD0) / (E - BD0), 0, 1)
        fc = 150 * (1400 / 150) ** (u ** 1.15)
        return hp_mag(f, fc, 3)

    route(main); route(intro, intro_lp); route(bdb, bd_hp)
    ir = make_ir(rng, 2.2, 1.7)
    wet = np.stack([fft_conv(send[c], ir[c])[:total_n] for c in range(2)])
    mix = dry + 0.55 * wet + 1.0 * fx.x[:, :total_n]

    # ------------------------------------------------------------- master
    mix = static_filter(mix, lambda f: hp_mag(f, 26, 2) * lp_mag(f, 17000, 2))
    mix -= mix.mean(axis=1, keepdims=True)
    fade_edges(mix, int(0.12 * SR), int(3.0 * SR))
    stems_rms = {}
    for name in ('kick', 'clap', 'hat', 'bass', 'pad', 'arp'):
        x = levels[name] * main[name].x[:, int(D * SR):int(BD0 * SR)]
        stems_rms[name] = round(20 * math.log10(np.sqrt(np.mean(x ** 2)) + 1e-12), 1)
    L = lufs(mix)
    mix *= 10 ** ((args.lufs - L) / 20)
    # transparent lookahead peak limiter (only touches the few peaks above the ceiling)
    ceil = 10 ** (args.ceiling / 20)
    for _ in range(4):
        tp = true_peak_db(mix)
        if tp <= args.ceiling:
            break
        a = np.max(np.abs(mix), axis=0)
        req = np.minimum(1.0, ceil * 0.97 / np.maximum(a, 1e-9))
        w = int(0.006 * SR) | 1
        g = moving_avg(running_min(req, 2 * w + 1), w)
        g = np.minimum(g, moving_avg(running_min(req, int(0.04 * SR) | 1), int(0.03 * SR)))
        mix *= g
    info = dict(bpm=round(bpm, 3), beat_s=round(beat, 5), bar_s=round(bar, 5), drop_s=D, breakdown_s=round(BD0, 3),
                end_card_hit_s=E, groove_bars=groove_bars, breakdown_beats=bd_beats, length_s=round(total, 2),
                lufs_pre_norm=round(L, 2), lufs=round(lufs(mix), 2), true_peak_dbtp=round(true_peak_db(mix), 2),
                stem_rms_drop_db=stems_rms)
    return mix, info


def write_wav24(path, st):
    x = np.clip(st, -1, 1 - 2 ** -23)
    pcm = np.ascontiguousarray(np.round(x.T * 8388607).astype("<i4"))
    b = pcm.view(np.uint8).reshape(-1, 4)[:, :3].tobytes()
    with wave.open(path, 'wb') as w:
        w.setnchannels(2); w.setsampwidth(3); w.setframerate(SR)
        w.writeframes(b)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--drop', type=float, default=6.8, help='Sekunde des Drops (= SEGMENTS[1].from)')
    ap.add_argument('--end', type=float, default=38.3, help='Sekunde der End-Card (= MAIN_SECONDS)')
    ap.add_argument('--bpm', type=float, default=None, help='Tempo erzwingen (sonst automatisch)')
    ap.add_argument('--bpm-min', type=float, default=124.0)
    ap.add_argument('--bpm-max', type=float, default=128.0)
    ap.add_argument('--tail-bars', type=int, default=8)
    ap.add_argument('--lufs', type=float, default=-17.0)
    ap.add_argument('--ceiling', type=float, default=-1.6, help='True-Peak-Grenze vor dem MP3-Encode (dBTP)')
    ap.add_argument('--seed', type=int, default=4207)
    ap.add_argument('--out', default=os.path.join(PROJECT, 'public', 'music', 'bed.mp3'))
    ap.add_argument('--wav', default=None, help='optional: zusätzlich 24-bit-WAV hierhin schreiben')
    args = ap.parse_args()

    mix, info = render(args)
    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
    with tempfile.TemporaryDirectory() as td:
        wav = args.wav or os.path.join(td, 'bed.wav')
        write_wav24(wav, mix)
        subprocess.run(['ffmpeg', '-hide_banner', '-loglevel', 'error', '-y', '-i', wav,
                        '-c:a', 'libmp3lame', '-b:a', '192k', '-ar', str(SR), args.out], check=True)
    print(json.dumps(info, indent=2))


if __name__ == '__main__':
    main()
