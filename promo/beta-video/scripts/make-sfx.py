#!/usr/bin/env python3
"""
make-sfx.py – synthetisiert alle Soundeffekte für das Galacticfy-Promo (keine Samples,
keine Downloads, nur numpy). Deterministisch (fester Seed).

    python3 -I scripts/make-sfx.py            # schreibt public/sfx/*.wav

Erzeugt (48 kHz, Stereo, 16 bit, True-Peak <= -3 dBFS, Fade-in/-out, kein DC):
    whoosh.wav        ~0.45 s  Rauschen mit Bandpass-Sweep hoch+runter, Pan L->R (Raketen-Flug)
    impact.wav        ~0.55 s  Sub-Thump (Pitch-Drop) + Crack + kurzer Hall (Banner-Slams)
    impact-small.wav  ~0.32 s  leichtere Variante (Neben-Pops)
    riser.wav         ~1.20 s  Rauschen + Saw-Glide steigend, endet bei ~1.13 s abrupt (Build-up)
    glitch.wav        ~0.30 s  digitales Stottern / Bitcrush (BUGS FINDEN)
    pop.wav           ~0.15 s  UI-Pop / Bubble (Badges, Häkchen)
    ding.wav          ~0.60 s  heller Coin/Bell (Belohnungen, KOSTENLOS)
click.wav wird NICHT angefasst.
"""
import os
import sys
import wave

import numpy as np

SR = 48000
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', 'public', 'sfx')
rng = np.random.default_rng(20261007)


# ---------------------------------------------------------------- DSP helpers
def tax(dur):
    return np.arange(int(round(dur * SR))) / SR


def biquad(x, kind, f, q=0.7071, gain_db=0.0):
    """Causal RBJ biquad (lowpass/highpass/bandpass/peak). x: 1-D."""
    w = 2 * np.pi * f / SR
    cw, sw = np.cos(w), np.sin(w)
    al = sw / (2 * q)
    A = 10 ** (gain_db / 40)
    if kind == 'lp':
        b = [(1 - cw) / 2, 1 - cw, (1 - cw) / 2]; a = [1 + al, -2 * cw, 1 - al]
    elif kind == 'hp':
        b = [(1 + cw) / 2, -(1 + cw), (1 + cw) / 2]; a = [1 + al, -2 * cw, 1 - al]
    elif kind == 'bp':
        b = [al, 0, -al]; a = [1 + al, -2 * cw, 1 - al]
    elif kind == 'peak':
        b = [1 + al * A, -2 * cw, 1 - al * A]; a = [1 + al / A, -2 * cw, 1 - al / A]
    else:
        raise ValueError(kind)
    b0, b1, b2 = (v / a[0] for v in b)
    a1, a2 = a[1] / a[0], a[2] / a[0]
    y = np.empty_like(x)
    x1 = x2 = y1 = y2 = 0.0
    for i, xi in enumerate(x.tolist()):
        yi = b0 * xi + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2, x1, y2, y1 = x1, xi, y1, yi
        y[i] = yi
    return y


def stft_filter(x, gain, n=1024, hop=128):
    """Time-varying zero-phase filter: gain(times[:,None], freqs[None,:]) -> |H|."""
    pad = n
    xp = np.concatenate([np.zeros(pad), x, np.zeros(pad + n)])
    win = 0.5 - 0.5 * np.cos(2 * np.pi * np.arange(n) / n)
    nfr = (len(xp) - n) // hop + 1
    idx = np.arange(n)[None, :] + hop * np.arange(nfr)[:, None]
    X = np.fft.rfft(xp[idx] * win, axis=1)
    times = (hop * np.arange(nfr) + n / 2 - pad) / SR
    freqs = np.fft.rfftfreq(n, 1 / SR)
    Y = np.fft.irfft(X * gain(times[:, None], freqs[None, :]), n=n, axis=1) * win
    y = np.zeros(len(xp)); norm = np.zeros(len(xp))
    for k in range(nfr):
        y[k * hop:k * hop + n] += Y[k]
        norm[k * hop:k * hop + n] += win ** 2
    y /= np.maximum(norm, 1e-9)
    return y[pad:pad + len(x)]


def fft_conv(a, b):
    n = len(a) + len(b) - 1
    m = 1 << (n - 1).bit_length()
    return np.fft.irfft(np.fft.rfft(a, m) * np.fft.rfft(b, m), m)[:n]


def make_ir(dur, t60, hf_from=9000.0, hf_to=1800.0, predelay=0.012):
    """Synthetic stereo room IR (decorrelated noise, HF damps faster)."""
    t = tax(dur)
    out = []
    for _ in range(2):
        nz = rng.standard_normal(len(t)) * np.exp(-6.91 * t / t60)
        nz = stft_filter(nz, lambda tt, ff: 1 / np.sqrt(1 + (ff / (hf_from * (hf_to / hf_from) ** np.clip(tt / dur, 0, 1))) ** 4))
        nz = np.concatenate([np.zeros(int(predelay * SR)), nz])
        out.append(nz / np.sqrt(np.sum(nz ** 2)))
    return np.stack(out)


def reverb(st, ir, wet_db):
    g = 10 ** (wet_db / 20)
    n = st.shape[1]
    wet = np.stack([fft_conv(st[c], ir[c])[:n] for c in range(2)])
    out = st + g * wet
    # tonal sources + a random IR can comb-filter one channel up/down by a few dB:
    # keep the dry L/R balance (these SFX are centred)
    r_in = np.sqrt((st ** 2).mean(axis=1)); r_out = np.sqrt((out ** 2).mean(axis=1))
    return out * (r_in / np.maximum(r_out, 1e-12))[:, None] * np.sqrt((r_out ** 2).mean() / (r_in ** 2).mean())


def pan(mono, p):
    """p in [-1, 1] (scalar or array), constant power."""
    a = (np.asarray(p) + 1) * np.pi / 4
    return np.stack([mono * np.cos(a), mono * np.sin(a)])


def true_peak(st):
    tp = 0.0
    for ch in st:
        x = np.concatenate([np.zeros(64), ch, np.zeros(64)])
        X = np.fft.rfft(x)
        up = np.fft.irfft(X, 4 * len(x)) * 4
        tp = max(tp, np.max(np.abs(up)))
    return tp


def finalize(st, name, peak_db=-3.2, fade_in_ms=0.6, fade_out_ms=30.0, hp=22.0):
    st = np.array([biquad(ch - ch.mean(), 'hp', hp) for ch in st])
    n = st.shape[1]
    fi = max(2, int(fade_in_ms * SR / 1000)); fo = max(2, int(fade_out_ms * SR / 1000))
    env = np.ones(n)
    env[:fi] = 0.5 - 0.5 * np.cos(np.pi * np.arange(fi) / fi)
    env[n - fo:] = 0.5 + 0.5 * np.cos(np.pi * np.arange(1, fo + 1) / fo)
    st = st * env
    st = st - st.mean(axis=1, keepdims=True) * env  # residual DC after fades (tiny)
    st = st * (10 ** (peak_db / 20) / true_peak(st))
    # TPDF dither -> 16 bit
    d = (rng.random(st.shape) - rng.random(st.shape))
    pcm = np.clip(np.round(st * 32767 + d), -32768, 32767).astype('<i2')
    path = os.path.join(OUT, name)
    with wave.open(path, 'wb') as w:
        w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes(pcm.T.copy().tobytes())
    print(f'{name:18s} {n / SR:5.2f}s  tp={20 * np.log10(true_peak(st)):6.2f} dBTP')
    return st


def smoothstep(x):
    x = np.clip(x, 0, 1)
    return x * x * (3 - 2 * x)


# ---------------------------------------------------------------- SFX
def whoosh():
    dur = 0.46
    t = tax(dur); u = t / dur; up = 0.56
    common = rng.standard_normal(len(t))
    nl = 0.65 * common + 0.76 * rng.standard_normal(len(t))
    nr = 0.65 * common + 0.76 * rng.standard_normal(len(t))

    def bump(uu):  # 0 -> 1 at up -> 0.25 at end (log-frequency trajectory)
        r = smoothstep(uu / up)
        f = 1 - 0.75 * smoothstep((uu - up) / (1 - up))
        return np.where(uu < up, r, f)

    def g_air(tt, ff):
        uu = tt / dur
        fc = 420 * (4800 / 420) ** bump(uu)
        bw = 0.55 + 0.2 * bump(uu)
        lf = np.log2(np.maximum(ff, 1) / fc)
        # resonant band (the audible sweep) + a faint broad 'air' floor
        return np.exp(-0.5 * (lf / bw) ** 2) + 0.12 * np.exp(-0.5 * (lf / 1.6) ** 2)

    def g_body(tt, ff):
        uu = tt / dur
        fc = 140 * (520 / 140) ** bump(uu)
        return 1 / np.sqrt(1 + (ff / fc) ** 4) * (ff > 35)

    air = np.stack([stft_filter(nl, g_air), stft_filter(nr, g_air)])
    body = stft_filter(common, g_body)
    # amplitude: swell to peak at up, then fall away; light flutter like a passing rocket
    amp = np.where(u < up, smoothstep(u / up) ** 1.6, np.exp(-((u - up) / 0.2) ** 2 * 2.2))
    flutter = 1 + 0.12 * np.sin(2 * np.pi * (18 + 14 * u) * t)
    p = np.clip(-0.85 + 1.7 * smoothstep(u / 0.95), -0.85, 0.85)
    ang = (p + 1) * np.pi / 4
    st = air * amp * flutter * 0.9
    st[0] *= np.cos(ang) * np.sqrt(2); st[1] *= np.sin(ang) * np.sqrt(2)
    st += pan(body * amp * 0.8, p * 0.6)
    return finalize(st, 'whoosh.wav', fade_in_ms=4, fade_out_ms=40)


def impact(small=False):
    dur = 0.32 if small else 0.56
    t = tax(dur)
    f0, f1, tau_f = (200.0, 85.0, 0.02) if small else (150.0, 54.0, 0.04)
    f = f1 + (f0 - f1) * np.exp(-t / tau_f)
    ph = 2 * np.pi * np.cumsum(f) / SR
    a_env = (1 - np.exp(-t / 0.0012)) * np.exp(-t / (0.06 if small else 0.12))
    thump = np.tanh(1.6 * np.sin(ph) * a_env) / np.tanh(1.6)
    # crack: pre-filtered noise, then enveloped (no filter pre-ring)
    nz = rng.standard_normal(len(t) + 4000)
    crack_src = biquad(biquad(nz, 'hp', 1400), 'lp', 7000 if small else 6000)[4000:]
    crack = crack_src * np.exp(-t / (0.008 if small else 0.013)) * (1 - np.exp(-t / 0.0004))
    mid_src = biquad(biquad(rng.standard_normal(len(t) + 4000), 'lp', 900), 'hp', 120)[4000:]
    mid = mid_src * np.exp(-t / (0.022 if small else 0.045)) * (1 - np.exp(-t / 0.001))
    mono = thump * 1.0 + crack * (0.5 if small else 0.55) + mid * (0.4 if small else 0.55)
    st = np.stack([mono, mono])
    # slight stereo width on the crack only
    st[0] += crack * 0.06; st[1] -= crack * 0.06
    ir = make_ir(0.35 if small else 0.5, 0.3 if small else 0.45)
    st = reverb(st, ir, -17 if small else -14)
    st = st[:, :len(t)]
    return finalize(st, 'impact-small.wav' if small else 'impact.wav', fade_in_ms=0.4, fade_out_ms=60 if not small else 45)


def riser():
    dur = 1.2; peak_t = 1.13
    t = tax(dur); u = np.clip(t / peak_t, 0, 1)
    # noise layer with rising bandpass
    common = rng.standard_normal(len(t))
    nl = 0.5 * common + 0.87 * rng.standard_normal(len(t))
    nr = 0.5 * common + 0.87 * rng.standard_normal(len(t))

    def g(tt, ff):
        uu = np.clip(tt / peak_t, 0, 1)
        fc = 260 * (8500 / 260) ** (uu ** 1.35)
        return np.exp(-0.5 * (np.log2(np.maximum(ff, 1) / fc) / 1.15) ** 2)
    noise = np.stack([stft_filter(nl, g), stft_filter(nr, g)])
    # band-limited saw glide (additive, no aliasing), 3 detuned voices + fifth
    fmax = 9000.0
    saw = np.zeros((2, len(t)))
    for base, det, pp in [(110.0, 0.0, -0.4), (110.0, 0.12, 0.4), (164.81, -0.07, 0.0), (220.0, 0.05, 0.15)]:
        fr = base * 2 ** (det / 12) * 2 ** (2.6 * u ** 1.7)
        ph = 2 * np.pi * np.cumsum(fr) / SR + rng.random() * 2 * np.pi
        v = np.zeros(len(t))
        for k in range(1, 80):
            w = np.clip((fmax - k * fr) / 800, 0, 1)
            if not w.any():
                break
            # soft lowpass that opens with u
            lp = 1 / np.sqrt(1 + (k * fr / (600 + 5000 * u ** 1.5)) ** 4)
            v += (-1) ** (k + 1) * np.sin(k * ph) / k * w * lp
        saw += pan(v, pp)
    trem_rate = 5 + 22 * u ** 1.5
    trem = 1 - 0.28 * (0.5 + 0.5 * np.sin(2 * np.pi * np.cumsum(trem_rate) / SR))
    amp = (u ** 2.3) * trem
    st = (noise * 0.9 + saw * 0.55) * amp
    # abrupt but click-free stop at peak_t, silence afterwards
    cut = np.clip((t - peak_t) / 0.018, 0, 1)
    st = st * (0.5 + 0.5 * np.cos(np.pi * cut))
    return finalize(st, 'riser.wav', fade_in_ms=10, fade_out_ms=20)


def glitch():
    dur = 0.30
    n = int(dur * SR)
    t_src = tax(0.08)

    def sq(f, tt, fmax=9000):  # band-limited square
        v = np.zeros(len(tt))
        for k in range(1, 200, 2):
            if k * f > fmax:
                break
            v += np.sin(2 * np.pi * k * f * tt) / k
        return v * 4 / np.pi

    src_a = 0.6 * sq(440, t_src) + 0.4 * sq(659.25, t_src) + 0.25 * rng.standard_normal(len(t_src))
    src_b = 0.7 * sq(880, t_src) + 0.3 * sq(1318.5, t_src)
    src_c = biquad(rng.standard_normal(len(t_src)), 'bp', 2500, q=0.8) * 2.2
    src_d = 0.8 * sq(220, t_src) + 0.3 * rng.standard_normal(len(t_src))

    def crush(x, bits, hold):
        if hold > 1:
            x = np.repeat(x[::hold], hold)[:len(x)]
        qn = 2 ** (bits - 1)
        return np.round(x * qn) / qn

    out = np.zeros((2, n))
    plan = [  # (start_s, len_s, source, bits, hold, pan, gain)
        (0.000, 0.026, src_a, 6, 2, -0.3, 1.0), (0.026, 0.026, src_a, 6, 2, -0.3, 0.95),
        (0.052, 0.022, src_b, 5, 4, 0.45, 0.8), (0.074, 0.012, src_d, 4, 6, 0.0, 0.9),
        (0.086, 0.012, src_d, 4, 6, 0.0, 0.85), (0.098, 0.012, src_d, 4, 6, 0.0, 0.8),
        (0.110, 0.012, src_d, 4, 6, 0.0, 0.75), (0.122, 0.030, src_c, 3, 12, -0.5, 0.7),
        (0.160, 0.018, src_b, 5, 3, 0.5, 0.75), (0.178, 0.018, src_b, 5, 3, -0.5, 0.65),
        (0.200, 0.040, src_a, 4, 8, 0.2, 0.6), (0.244, 0.008, src_d, 4, 4, 0.0, 0.5),
        (0.256, 0.008, src_d, 4, 4, 0.0, 0.4), (0.268, 0.008, src_d, 4, 4, 0.0, 0.3),
    ]
    for st_s, ln_s, src, bits, hold, pp, gn in plan:
        a = int(st_s * SR); m = int(ln_s * SR)
        off = int(rng.integers(0, len(src) - m))
        seg = crush(src[off:off + m] / 1.3, bits, hold)
        f = int(0.0007 * SR)  # 0.7 ms micro-fades: edges stay tight but don't click
        e = np.ones(m); e[:f] = np.linspace(0, 1, f); e[-f:] = np.linspace(1, 0, f)
        out[:, a:a + m] += pan(seg * e * gn, pp)
    # tame the sample-and-hold fizz above ~11 kHz, keep the digital grit
    out = np.array([biquad(biquad(ch, 'lp', 11000), 'lp', 11000) for ch in out])
    return finalize(out, 'glitch.wav', fade_in_ms=0.6, fade_out_ms=12)


def pop():
    dur = 0.15
    t = tax(dur)
    f = 320 * 2 ** (1.55 * (1 - np.exp(-t / 0.022)))
    ph = 2 * np.pi * np.cumsum(f) / SR
    env = (1 - np.exp(-t / 0.0007)) * np.exp(-t / 0.032)
    tone = (np.sin(ph) + 0.22 * np.sin(2 * ph) + 0.06 * np.sin(3 * ph)) * env
    click_src = biquad(rng.standard_normal(len(t) + 2000), 'hp', 3000)[2000:]
    click = click_src * np.exp(-t / 0.0012) * 0.12
    mono = tone + click
    st = np.stack([mono, mono])
    st = reverb(st, make_ir(0.18, 0.15, 7000, 2500, 0.004), -20)[:, :len(t)]
    return finalize(st, 'pop.wav', fade_in_ms=0.3, fade_out_ms=25)


def ding():
    dur = 0.62
    t = tax(dur)
    ratios = [1.0, 2.0, 3.0, 4.16, 5.43]
    amps = [1.0, 0.32, 0.1, 0.16, 0.05]
    taus = [0.17, 0.10, 0.07, 0.05, 0.035]

    def bell(f0, t0, tau_scale, detune):
        tt = np.clip(t - t0, 0, None)
        on = (t >= t0).astype(float)
        v = np.zeros(len(t))
        for r, a, ta in zip(ratios, amps, taus):
            v += a * np.sin(2 * np.pi * (f0 * r + detune * r) * tt) * np.exp(-tt / (ta * tau_scale))
        return v * on * (1 - np.exp(-tt / 0.0008))

    n2 = 0.072
    st = np.zeros((2, len(t)))
    for c, det in enumerate((-0.9, 0.9)):
        b1 = bell(987.77, 0.0, 0.5, det)
        b1 *= np.clip((n2 + 0.006 - t) / 0.006, 0, 1)  # first note cut by the second (5 ms xfade)
        b2 = bell(1318.51, n2, 1.0, det)
        st[c] = 0.75 * b1 + b2
    tick_src = biquad(rng.standard_normal(len(t) + 2000), 'hp', 5000)[2000:]
    for t0 in (0.0, n2):
        tt = np.clip(t - t0, 0, None)
        st += 0.05 * tick_src * np.exp(-tt / 0.0015) * (t >= t0)
    st = reverb(st, make_ir(0.5, 0.45, 9000, 3000, 0.01), -13)[:, :len(t)]
    return finalize(st, 'ding.wav', fade_in_ms=0.4, fade_out_ms=70)


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    only = set(sys.argv[1:])
    for name, fn in [('whoosh', whoosh), ('impact', impact), ('impact-small', lambda: impact(True)),
                     ('riser', riser), ('glitch', glitch), ('pop', pop), ('ding', ding)]:
        if not only or name in only:
            rng = np.random.default_rng(20261007 + sum(name.encode()))  # per-SFX seed
            fn()
