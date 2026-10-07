#!/usr/bin/env python3
"""
Galacticfy Beta-Promo - deutsches TTS-Voiceover erzeugen -> public/voiceover.mp3

Normalerweise ueber den Wrapper starten (legt venv an, laedt + prueft die Stimme):

    bash scripts/make-voiceover.sh

Stimme
------
Piper / VITS "de_DE-thorsten-high" (Thorsten Mueller, Datensatz CC0,
https://github.com/thorstenMueller/Thorsten-Voice). Bezogen als GitHub-Release-
Asset des sherpa-onnx-Projekts (identisches Piper-ONNX-Modell + .onnx.json,
nur um Metadaten ergaenzt), weil huggingface.co hier nicht erreichbar ist:
https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-de_DE-thorsten-high.tar.bz2
Laufzeit: pip "piper-tts==1.8.0" (bringt espeak-ng-Phonemizer mit).

Ablauf
------
1. Liest SEGMENTS (from/to/text) aus src/config.ts und prueft, dass die
   Caption-Texte (ohne **) noch exakt den Texten unten entsprechen.
   Aendert sich eine Caption, bricht das Skript ab -> LINES unten anpassen.
2. Jede Zeile einzeln synthetisieren. Sprechbeginn = Segmentstart + LEAD,
   Ende spaetestens END_MARGIN vor dem naechsten Segment. Tempo zuerst ueber
   Piper length_scale (LS_LADDER, mehrere Takes je Stufe, laengster passender
   Take gewinnt), danach notfalls ffmpeg atempo (max. MAX_ATEMPO). Passt es dann
   immer noch nicht, darf eine Zeile bis MAX_OVERRUN s ins naechste Fenster
   laufen (wird gemeldet; die naechste Zeile rutscht dann ggf. nach hinten).
3. 25,0 s Spur zusammensetzen, Hochpass + leichte Kompression, Lautheit auf
   TARGET_LUFS, Limiter, 48 kHz Stereo MP3.
4. Kontrollmessung mit ffmpeg (silencedetect je Zeile, ebur128, volumedetect).

Aussprache-Korrekturen (nur TTS-Eingabe, Captions bleiben unveraendert)
-----------------------------------------------------------------------
[[...]] = rohe espeak-ng-Phoneme fuer Piper (umgeht die Rechtschreib-Regeln).
  Minecraft-Server  -> [[mˈaɪnkraftsˌœɾvɜ]]   espeak: "Mienecraft-Sörvə"
  Galacticfy        -> [[ɡalˈaktɪkfˌaɪ]]     espeak: "Galaktik-fii", jetzt "Ga-LAK-tik-fai"
  Betatester        -> [[bˈeːtatˌɛstɜ]]      espeak: "be-TAA-tester" (be- als Vorsilbe)
  Java              -> Dschaawa              espeak: "Jawa"
  Bedrock           -> Bettrock              espeak: "be-DROCK"
  Bugs              -> Baggs                 espeak: "Buuks"
  check             -> tscheck               espeak: "chek" mit ich-Laut
  Menues            -> [[meːnˈyːs]]          Betonung auf -nues
  Als Dankeschoen   -> [[als dˈaŋkəʃˌøːn]]   espeak: "Dankä-schoen" (+ "Als" bleibt unbetont)
  Betatester-Prefix -> [[bˈeːtatˌɛstɜprˌiːfɪks]]  "Priefix" (englisch), nicht "Praefix"
  Bio               -> [[bˈiːoː]]            espeak: "Bjo"
  Discord           -> unveraendert (espeak: "Diskort", passt)
  "…" (Zeile 1)     -> Komma (espeak macht aus "…" keine Pause); Zeile 5: "…" -> "."
  "–" (Zeile 2)     -> Komma im Phonem-Block (Satzzeichen direkt nach [[...]] verwirft espeak)
  Satzzeichen nach einem [[...]]-Block gehoeren IN den Block, z. B. [[bˈiːoː.]]

Kontrolle (2026-10-07): Rueck-Transkription des Ergebnisses mit Whisper large-v3-turbo
(sherpa-onnx-whisper-turbo) ergab alle sechs Saetze wortgetreu; "Galacticfy" wurde
als "Galactik Fire" verstanden (= gesprochen "Ga-LAK-tik-fai"), "Bedrock" als "Betrok".
"""

from __future__ import annotations

import argparse
import json
import re
import shutil
import subprocess
import sys
import tempfile
from dataclasses import dataclass
from pathlib import Path

import numpy as np
import onnxruntime
from piper import PiperVoice, SynthesisConfig

# ------------------------------------------------------------------ Texte
# (caption = exakter Caption-Text aus config.ts ohne **, tts = Eingabe fuer Piper)
LINES = [
    dict(
        caption="Dieser Minecraft-Server ist noch nicht fertig … und genau deshalb brauchen wir dich!",
        tts="Dieser [[mˈaɪnkraftsˌœɾvɜ]] ist noch nicht fertig, und genau deshalb brauchen wir dich!",
    ),
    dict(
        caption="Galacticfy sucht 20 Betatester – für Java und Bedrock!",
        tts="[[ɡalˈaktɪkfˌaɪ]] sucht zwanzig [[bˈeːtatˌɛstɜ,]] für Dschaawa und Bettrock!",
    ),
    dict(
        caption="Teste unsere Systeme, finde Bugs, check die Menüs und hilf uns, die Wirtschaft perfekt auszubalancieren.",
        tts="Teste unsere Systeme, finde Baggs, tscheck die [[meːnˈyːs]] und hilf uns, "
        "die Wirtschaft perfekt auszubalancieren.",
    ),
    dict(
        caption="Als Dankeschön bekommst du einen exklusiven Betatester-Prefix und weitere Belohnungen!",
        tts="[[als dˈaŋkəʃˌøːn]] bekommst du einen exklusiven [[bˈeːtatˌɛstɜprˌiːfɪks]] "
        "und weitere Belohnungen!",
    ),
    dict(
        caption="Aber Achtung: Es gibt nur 20 Plätze – wer zuerst kommt …",
        tts="Aber Achtung: Es gibt nur zwanzig Plätze – wer zuerst kommt.",
    ),
    dict(
        caption="Komm jetzt auf unseren Discord und öffne ein Ticket! Link in der Bio.",
        tts="Komm jetzt auf unseren Discord und öffne ein Ticket! Link in der [[bˈiːoː.]]",
    ),
]

# ------------------------------------------------------------------ Parameter
TOTAL_SECONDS = 25.0
LEAD = 0.10  # Sprechbeginn nach Segmentstart
END_MARGIN = 0.08  # Mindestabstand Zeilenende -> naechstes Segment
MIN_GAP = 0.15  # Mindestpause zwischen zwei Zeilen (falls eine ueberlaeuft)
MAX_OVERRUN = 0.40  # max. Ueberhang ins naechste Fenster
SENT_GAP = 0.14  # Pause zwischen zwei Saetzen innerhalb einer Zeile
LS_LADDER = [0.90, 0.87, 0.84, 0.81, 0.78, 0.75, 0.72]  # Piper length_scale (<1 = schneller)
TAKES = 4  # Takes je length_scale-Stufe
MAX_ATEMPO = 1.30
NOISE_SCALE = 0.72  # etwas mehr Prosodie-Variation als Default 0.667
NOISE_W = 0.90  # etwas lebendigere Phonemdauern als Default 0.8
SEED = 20261007
THREADS = 4
LINE_RMS_DBFS = -20.0  # Pegelangleich der Zeilen untereinander (vor dem Mastering)
TARGET_LUFS = -14.0
LIMIT_LINEAR = 0.79  # Limiter-Decke (~ -2 dBFS Sample-Peak, < -1 dBTP nach MP3)
TRIM_REL_DB = -45.0  # Stille-Schwelle relativ zum lautesten 10-ms-Frame

PRE_CHAIN = (
    "aresample=48000:resampler=soxr:precision=28,"
    "highpass=f=80,"
    "acompressor=threshold=0.1:ratio=3:attack=5:release=120:knee=4"
)


# ------------------------------------------------------------------ Hilfen
@dataclass
class Segment:
    start: float
    end: float
    caption: str


def read_segments(config_ts: Path) -> list[Segment]:
    src = config_ts.read_text(encoding="utf-8")
    block = src[src.index("export const SEGMENTS") :]
    block = block[: block.index("];")]
    pat = re.compile(r"from:\s*([\d.]+),\s*to:\s*([\d.]+),\s*text:\s*'((?:[^'\\]|\\.)*)'", re.S)
    segs = [
        Segment(float(a), float(b), c.replace("\\'", "'").replace("**", ""))
        for a, b, c in pat.findall(block)
    ]
    if not segs:
        sys.exit("Keine SEGMENTS in config.ts gefunden")
    return segs


def ffmpeg_bin() -> str:
    exe = shutil.which("ffmpeg")
    if not exe:
        sys.exit("ffmpeg nicht gefunden")
    return exe


def run_ffmpeg(args: list[str], stdin: bytes | None = None) -> subprocess.CompletedProcess:
    cmd = [ffmpeg_bin(), "-hide_banner", "-nostdin", "-y", *args]
    return subprocess.run(cmd, input=stdin, capture_output=True, check=True)


def frames_db(a: np.ndarray, sr: int, win: float = 0.01) -> tuple[np.ndarray, int]:
    f = max(1, int(sr * win))
    n = len(a) // f
    rms = np.sqrt(np.mean(a[: n * f].reshape(n, f) ** 2, axis=1) + 1e-12)
    return 20 * np.log10(rms), f


def trim(a: np.ndarray, sr: int) -> np.ndarray:
    """Stille vorn/hinten entfernen (Schwelle relativ zum Maximum), kurze Fades."""
    db, f = frames_db(a, sr)
    active = np.where(db > db.max() + TRIM_REL_DB)[0]
    if len(active) == 0:
        return a[:0]
    s = max(0, active[0] * f - int(0.005 * sr))
    e = min(len(a), (active[-1] + 1) * f + int(0.02 * sr))
    out = a[s:e].astype(np.float32).copy()
    fi, fo = int(0.004 * sr), int(0.015 * sr)
    out[:fi] *= np.linspace(0, 1, fi, dtype=np.float32)
    out[-fo:] *= np.linspace(1, 0, fo, dtype=np.float32)
    return out


def active_rms_db(a: np.ndarray, sr: int) -> float:
    db, _ = frames_db(a, sr)
    loud = db[db > db.max() - 30]
    return float(10 * np.log10(np.mean(10 ** (loud / 10))))


def atempo(a: np.ndarray, sr: int, factor: float) -> np.ndarray:
    res = run_ffmpeg(
        ["-f", "f32le", "-ar", str(sr), "-ac", "1", "-i", "pipe:0",
         "-af", f"atempo={factor:.4f}", "-f", "f32le", "-ar", str(sr), "-ac", "1", "pipe:1"],
        stdin=a.astype(np.float32).tobytes(),
    )
    return trim(np.frombuffer(res.stdout, dtype=np.float32), sr)


class Synth:
    def __init__(self, model: Path, seed: int):
        # Seed VOR dem Laden setzen: die Zufalls-Ops des VITS-Graphen ziehen ihren
        # Seed beim Erzeugen der Session -> reproduzierbare Takes.
        onnxruntime.set_seed(seed)
        self.voice = PiperVoice.load(model)
        so = onnxruntime.SessionOptions()
        so.intra_op_num_threads = THREADS  # feste Thread-Zahl -> bit-identische Ergebnisse
        self.voice.session = onnxruntime.InferenceSession(
            str(model), sess_options=so, providers=["CPUExecutionProvider"]
        )
        self.sr = self.voice.config.sample_rate

    def take(self, text: str, ls: float) -> np.ndarray:
        cfg = SynthesisConfig(
            length_scale=ls, noise_scale=NOISE_SCALE, noise_w_scale=NOISE_W, normalize_audio=False
        )
        parts = [trim(c.audio_float_array, self.sr) for c in self.voice.synthesize(text, cfg)]
        gap = np.zeros(int(SENT_GAP * self.sr), dtype=np.float32)
        out: list[np.ndarray] = []
        for i, p in enumerate(parts):
            if i:
                out.append(gap)
            out.append(p)
        return np.concatenate(out)


def fit_line(model: Path, idx: int, text: str, avail: float, hard_max: float) -> dict:
    syn = Synth(model, SEED + idx)
    sr = syn.sr
    log = []
    best = None  # (dur, audio, ls)
    shortest = None
    for ls in LS_LADDER:
        takes = [syn.take(text, ls) for _ in range(TAKES)]
        durs = [len(t) / sr for t in takes]
        log.append(f"ls={ls:.2f}: " + " ".join(f"{d:.2f}" for d in durs))
        fitting = [(d, t) for d, t in zip(durs, takes) if d <= avail]
        if fitting:
            d, t = max(fitting, key=lambda x: x[0])  # laengster passender Take
            best = (d, t, ls)
            break
        d, t = min(zip(durs, takes), key=lambda x: x[0])
        if shortest is None or d < shortest[0]:
            shortest = (d, t, ls)
    tempo = 1.0
    if best is None:
        d, t, ls = shortest
        tempo = min(MAX_ATEMPO, d / avail * 1.01)
        t = atempo(t, sr, tempo)
        best = (len(t) / sr, t, ls)
    d, t, ls = best
    if d > hard_max:
        print(f"WARNUNG Zeile {idx + 1}: {d:.2f}s passt auch mit Ueberhang nicht ({hard_max:.2f}s)")
    gain = 10 ** ((LINE_RMS_DBFS - active_rms_db(t, sr)) / 20)
    return dict(audio=t * gain, sr=sr, dur=d, ls=ls, tempo=tempo, log=log)


def measure_loudness(path: Path, extra_filter: str = "") -> dict:
    flt = (extra_filter + "," if extra_filter else "") + "loudnorm=I=-14:TP=-1:LRA=11:print_format=json"
    res = run_ffmpeg(["-i", str(path), "-af", flt, "-f", "null", "-"])
    err = res.stderr.decode("utf-8", "replace")
    return json.loads(err[err.rindex("{") : err.rindex("}") + 1])


def verify(mp3: Path, placed: list[dict]) -> list[dict]:
    err = run_ffmpeg(
        ["-i", str(mp3), "-af",
         "silencedetect=noise=-40dB:d=0.06,ebur128=peak=true,volumedetect", "-f", "null", "-"]
    ).stderr.decode("utf-8", "replace")
    probe = subprocess.run(
        [shutil.which("ffprobe") or "ffprobe", "-v", "error", "-show_entries",
         "format=duration:stream=sample_rate,channels,bit_rate", "-of", "json", str(mp3)],
        capture_output=True, check=True,
    )
    info = json.loads(probe.stdout)
    total = float(info["format"]["duration"])
    # Stille-Intervalle -> Sprach-Intervalle
    starts = [float(x) for x in re.findall(r"silence_start: (-?[\d.]+)", err)]
    ends = [float(x) for x in re.findall(r"silence_end: ([\d.]+)", err)]
    sil = list(zip(starts, ends + [total] * (len(starts) - len(ends))))
    speech, cur = [], 0.0
    for s, e in sil:
        if s > cur + 1e-3:
            speech.append((cur, s))
        cur = max(cur, e)
    if cur < total - 0.05:
        speech.append((cur, total))
    speech = [(s, e) for s, e in speech if e - s >= 0.03]
    rows = []
    for i, p in enumerate(placed):
        mine = [
            (s, e) for s, e in speech
            if max(range(len(placed)), key=lambda j: min(e, placed[j]["end"]) - max(s, placed[j]["start"])) == i
        ]
        rows.append(dict(start=min(s for s, _ in mine), end=max(e for _, e in mine)) if mine else {})
    summary = err[err.rindex("Summary:") :] if "Summary:" in err else ""
    stats = dict(
        I=float(re.search(r"I:\s+(-?[\d.]+) LUFS", summary).group(1)),
        LRA=float(re.search(r"LRA:\s+(-?[\d.]+) LU", summary).group(1)),
        TP=float(re.search(r"Peak:\s+(-?[\d.]+) dBFS", summary).group(1)),
        max_volume=float(re.search(r"max_volume: (-?[\d.]+) dB", err).group(1)),
        mean_volume=float(re.search(r"mean_volume: (-?[\d.]+) dB", err).group(1)),
        duration=total,
        stream=info["streams"][0],
    )
    return rows, stats


def main() -> None:
    root = Path(__file__).resolve().parent.parent
    ap = argparse.ArgumentParser()
    ap.add_argument("--model", required=True, type=Path, help="de_DE-thorsten-high.onnx (+ .onnx.json daneben)")
    ap.add_argument("--config", type=Path, default=root / "src" / "config.ts")
    ap.add_argument("--out", type=Path, default=root / "public" / "voiceover.mp3")
    args = ap.parse_args()

    segs = read_segments(args.config)
    if len(segs) != len(LINES):
        sys.exit(f"config.ts hat {len(segs)} Segmente, Skript kennt {len(LINES)}")
    for i, (seg, line) in enumerate(zip(segs, LINES)):
        if seg.caption != line["caption"]:
            sys.exit(f"Caption {i + 1} geaendert - LINES anpassen:\n  config: {seg.caption}\n  skript: {line['caption']}")

    placed = []
    prev_end = 0.0
    sr = None
    for i, (seg, line) in enumerate(zip(segs, LINES)):
        start = max(seg.start + LEAD, prev_end + MIN_GAP if i else 0.0)
        next_start = segs[i + 1].start if i + 1 < len(segs) else seg.end
        avail = next_start - END_MARGIN - start
        hard_max = next_start + MAX_OVERRUN - start
        r = fit_line(args.model, i, line["tts"], avail, hard_max)
        sr = r["sr"]
        end = start + r["dur"]
        prev_end = end
        flag = ""
        if end > next_start - END_MARGIN + 1e-3:
            flag = f"  <- UEBERHANG {end - next_start:+.2f}s ins naechste Fenster"
        if start > seg.start + LEAD + 1e-3:
            flag += f"  <- Start verschoben (+{start - seg.start - LEAD:.2f}s)"
        print(f"Zeile {i + 1} [{seg.start:g}-{seg.end:g}s]: {start:.2f}-{end:.2f}s  dauer={r['dur']:.2f}s "
              f"(frei {avail:.2f}s)  length_scale={r['ls']:.2f}  atempo={r['tempo']:.3f}{flag}")
        for l in r["log"]:
            print("     takes", l)
        placed.append(dict(start=start, end=end, audio=r["audio"], ls=r["ls"], tempo=r["tempo"], seg=seg))

    # ---- Spur zusammensetzen (Modell-Rate, mono, float)
    buf = np.zeros(int(TOTAL_SECONDS * sr), dtype=np.float32)
    for p in placed:
        s = int(round(p["start"] * sr))
        a = p["audio"][: len(buf) - s]
        buf[s : s + len(a)] += a

    with tempfile.TemporaryDirectory(prefix="voiceover-") as td:
        td = Path(td)
        raw = td / "track.wav"
        run_ffmpeg(["-f", "f32le", "-ar", str(sr), "-ac", "1", "-i", "pipe:0", "-c:a", "pcm_f32le", str(raw)],
                   stdin=buf.tobytes())
        # Lautheit nach Vorverarbeitung messen -> statische Verstaerkung + Limiter
        gain_db = TARGET_LUFS - float(measure_loudness(raw, PRE_CHAIN)["input_i"])
        for attempt in range(3):
            chain = (
                f"{PRE_CHAIN},volume={gain_db:.2f}dB,"
                f"alimiter=limit={LIMIT_LINEAR}:attack=3:release=60:level=disabled,"
                f"aformat=sample_rates=48000:channel_layouts=stereo,"
                f"apad=whole_dur={TOTAL_SECONDS},atrim=0:{TOTAL_SECONDS}"
            )
            mp3 = td / "voiceover.mp3"
            run_ffmpeg(["-i", str(raw), "-af", chain, "-ar", "48000", "-ac", "2",
                        "-c:a", "libmp3lame", "-b:a", "192k", str(mp3)])
            got = float(measure_loudness(mp3)["input_i"])
            print(f"Mastering-Durchgang {attempt + 1}: Gain {gain_db:+.2f} dB -> {got:.2f} LUFS")
            if abs(got - TARGET_LUFS) <= 0.3:
                break
            gain_db += TARGET_LUFS - got
        args.out.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(mp3, args.out)

    rows, stats = verify(args.out, placed)
    print(f"\n{args.out}: {stats['duration']:.3f}s, {stats['stream']}")
    print(f"Integrated {stats['I']:.1f} LUFS, LRA {stats['LRA']:.1f} LU, True Peak {stats['TP']:.1f} dBTP, "
          f"max_volume {stats['max_volume']:.1f} dB, mean_volume {stats['mean_volume']:.1f} dB")
    print("\nGemessen (silencedetect -40 dB):")
    for i, (p, r) in enumerate(zip(placed, rows)):
        seg = p["seg"]
        nxt = placed[i + 1]["seg"].start if i + 1 < len(placed) else seg.end
        if not r:
            print(f"  Zeile {i + 1}: keine Sprache gefunden!")
            continue
        status = "OK" if r["end"] <= nxt else f"UEBERHANG {r['end'] - nxt:+.2f}s"
        print(f"  Zeile {i + 1} Fenster {seg.start:g}-{seg.end:g}s: Sprache {r['start']:.2f}-{r['end']:.2f}s "
              f"(Start +{r['start'] - seg.start:.2f}s, Ende {r['end'] - nxt:+.2f}s vs. naechstes Segment) {status}")


if __name__ == "__main__":
    main()
