#!/usr/bin/env bash
# Eigene Voiceover-Aufnahme importieren -> public/voiceover.mp3 (25,0 s, 48 kHz Stereo, -14 LUFS)
#
#   bash scripts/import-voiceover.sh zeile1.m4a zeile2.m4a zeile3.m4a zeile4.m4a zeile5.m4a zeile6.m4a
#       6 Dateien = eine pro Caption-Zeile (Reihenfolge wie im Video). Jede Zeile wird
#       gesäubert, Stille vorne/hinten abgeschnitten und ~0,1 s nach Beginn ihres Segments
#       platziert. Ist eine Zeile zu lang für ihr Zeitfenster, gibt es eine WARNUNG.
#
#   bash scripts/import-voiceover.sh aufnahme.m4a
#       1 Datei = ganzer Take (0:00 der Aufnahme = 0:00 im Video). Wird gesäubert und auf
#       25,0 s gekürzt/aufgefüllt; danach werden die erkannten Sprechpassagen neben die
#       Caption-Zeitfenster geschrieben.
#       --offset SEK   Aufnahme verschieben: positiv = SEK am Anfang abschneiden
#                      (Aufnahme lief schon vor dem Video), negativ = SEK Stille davor
#       --auto-start   alles vor dem ersten Wort abschneiden, Sprache beginnt bei 0,1 s
#
#   --out DATEI        anderes Ziel als public/voiceover.mp3 (z. B. zum Probehören)
#   npm run voiceover:import -- <dateien>   (gleiches Skript)
#
# Formate: alles, was ffmpeg lesen kann (m4a, mp3, wav, ogg/opus, webm, mp4/mov mit Tonspur …).
# Klang: Mono -> Hochpass 80 Hz -> sanfte Rauschminderung (afftdn) -> [Pausen vorne/hinten
#        abschneiden; kurze Klicks beim Starten/Stoppen der Aufnahme werden ignoriert] ->
#        leichte Kompression -> Stereo 48 kHz -> loudnorm auf -14 LUFS / -1,5 dBTP -> MP3 192 kbit/s.
# Beim ersten Import wird das bisherige public/voiceover.mp3 (TTS) als
# public/voiceover-tts.mp3 gesichert und danach nie überschrieben.
#
# Benötigt nur ffmpeg (mit libmp3lame) + Standard-Shell-Werkzeuge (awk, sed, grep).
set -euo pipefail
export LC_ALL=C

# ------------------------------------------------------------------------------
#  Zeitfenster der 6 Caption-Zeilen in Sekunden – Spiegel von SEGMENTS in src/config.ts.
#  Werden dort from/to geändert, hier mitziehen (das Skript warnt, wenn sie abweichen).
# ------------------------------------------------------------------------------
SEG_FROM=(0 3 6 11 15 19)
SEG_TO=(3 6 11 15 19 22)
END_CARD_SECONDS=3   # = END_CARD_SECONDS in config.ts (End-Card ohne Sprache)
LEAD=0.10            # Zeile beginnt so viele Sekunden nach Segmentstart
END_MARGIN=0.05      # ... und sollte so viele Sekunden vor Segmentende fertig sein
MIN_GAP=0.12         # Mindestpause, falls eine Zeile in die nächste hineinläuft

# ---- Klang ----
TARGET_LUFS=-14
TARGET_TP=-1.5
PRE_LUFS=-18         # Vor-Normalisierung je Datei, damit alle Zeilen gleich laut sind
SILENCE_DB=-42       # Schwelle fürs Finden von Sprach-Anfang/-Ende (nach Vor-Normalisierung,
                     # gemessen auf einer bandbegrenzten Kopie: 150 Hz – 5 kHz)
DETECT_DB=-38        # Schwelle für die Sprech-Erkennung im 1-Datei-Modus (nach loudnorm)
MIN_SILENCE=0.15     # kürzere Pausen zählen noch zur Sprache
MIN_RUN=0.15         # kürzere Geräusche am Anfang/Ende (Klick/Tippen beim Starten/Stoppen der
ISOLATION=0.25       # Aufnahme), die >= ISOLATION s Abstand zur Sprache haben, zählen nicht als Sprache
PAD_IN=0.05          # so viel bleibt vor dem ersten Wort stehen ...
PAD_OUT=0.08         # ... und nach dem letzten (leise Wortenden, Nachhall)
BAND="highpass=f=150,lowpass=f=5000"
CLEAN="highpass=f=80,afftdn=nr=10:nf=-50:tn=1"
COMP="acompressor=threshold=-22dB:ratio=2.5:attack=8:release=160:knee=4"

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/.." && pwd)"
DEFAULT_OUT="$ROOT/public/voiceover.mp3"
TTS_BACKUP="$ROOT/public/voiceover-tts.mp3"
N=${#SEG_FROM[@]}
TOTAL=$(awk -v t="${SEG_TO[$((N - 1))]}" -v e="$END_CARD_SECONDS" 'BEGIN{printf "%.3f", t + e}')

die() { echo "FEHLER: $*" >&2; exit 1; }
warn() { echo "WARNUNG: $*" >&2; }
calc() { awk "BEGIN{printf \"%.3f\", $*}"; }
fmt() { awk -v x="$1" 'BEGIN{printf "%.2f", x}'; }

usage() {
  # Kopfkommentar (Zeile 2 bis zur ersten Nicht-Kommentar-Zeile) ohne "# " ausgeben
  awk 'NR == 1 { next } /^#/ { sub(/^# ?/, ""); print; next } { exit }' "${BASH_SOURCE[0]}"
  exit "${1:-0}"
}

# ---- Argumente ----
OUT="$DEFAULT_OUT"
OFFSET=0
AUTO_START=0
FILES=()
while [ $# -gt 0 ]; do
  case "$1" in
    -h|--help) usage 0 ;;
    --out) [ $# -ge 2 ] || die "--out braucht einen Dateinamen"; OUT="$2"; shift 2 ;;
    --offset) [ $# -ge 2 ] || die "--offset braucht Sekunden"; OFFSET="$2"; shift 2 ;;
    --auto-start) AUTO_START=1; shift ;;
    --) shift; while [ $# -gt 0 ]; do FILES+=("$1"); shift; done ;;
    -*) die "unbekannte Option: $1 (Hilfe: --help)" ;;
    *) FILES+=("$1"); shift ;;
  esac
done
[[ "$OFFSET" =~ ^-?[0-9]+([.][0-9]+)?$ ]] || die "--offset erwartet eine Zahl in Sekunden (z. B. 1.25 oder -0.5)"

if [ ${#FILES[@]} -eq 1 ]; then
  MODE=single
elif [ ${#FILES[@]} -eq "$N" ]; then
  MODE=lines
  { [ "$OFFSET" = 0 ] && [ $AUTO_START = 0 ]; } || die "--offset/--auto-start gibt es nur im 1-Datei-Modus"
else
  echo "Bitte entweder 1 Datei (ganzer Take) oder $N Dateien (eine pro Caption-Zeile, in Reihenfolge) angeben." >&2
  echo >&2
  usage 1
fi

command -v ffmpeg >/dev/null || die "ffmpeg nicht gefunden (z. B. 'sudo apt install ffmpeg' / 'brew install ffmpeg' / Windows: 'winget install Gyan.FFmpeg', danach Git Bash neu öffnen)"
encoders=$(ffmpeg -hide_banner -encoders 2>/dev/null || true)
[[ "$encoders" == *libmp3lame* ]] || die "ffmpeg ohne MP3-Encoder (libmp3lame)"
for f in "${FILES[@]}"; do [ -f "$f" ] || die "Datei nicht gefunden: $f"; done

# ---- Zeitfenster mit src/config.ts vergleichen (nur Hinweis) ----
if [ -f "$ROOT/src/config.ts" ]; then
  cfg=$(tr -s ' \n' '  ' <"$ROOT/src/config.ts" | grep -oE 'from: *[0-9.]+, *to: *[0-9.]+' \
    | sed -E 's/from: *([0-9.]+), *to: *([0-9.]+)/\1-\2/' || true)
  mine=""
  for ((i = 0; i < N; i++)); do mine+="$(fmt "${SEG_FROM[$i]}")-$(fmt "${SEG_TO[$i]}") "; done
  cfgn=""
  for pair in $cfg; do cfgn+="$(fmt "${pair%-*}")-$(fmt "${pair#*-}") "; done
  if [ "$cfgn" != "$mine" ]; then
    warn "Zeitfenster weichen von src/config.ts ab – SEG_FROM/SEG_TO oben im Skript anpassen!"
    echo "         Skript: $mine" >&2
    echo "         config: $cfgn" >&2
  fi
fi

TMP="$(mktemp -d "${TMPDIR:-/tmp}/voiceover-import.XXXXXX")"
trap 'rm -rf "$TMP"' EXIT

ff() { ffmpeg -nostdin -hide_banner -loglevel error -y "$@"; }

# ffmpeg-Ausgabe (stderr) eines Analyse-Laufs; Fehler hier nie fatal (Auswertung prüft selbst)
analyze() { ffmpeg -nostdin -hide_banner -nostats "$@" -f null - 2>&1 || true; }
# Integrierte Lautheit (LUFS) / True Peak (dBTP) / Dauer (s) messen
measure_i() {
  analyze -i "$1" -af ebur128=framelog=quiet | awk '/Integrated loudness:/{f=1} f && $1=="I:" && !d {print $2; d=1}'
}
measure_tp() {
  analyze -i "$1" -af ebur128=framelog=quiet:peak=true | awk '/True peak:/{f=1} f && $1=="Peak:" && !d {print $2; d=1}'
}
duration() {
  analyze -i "$1" | awk '/Duration:/ && !d {split($2, t, ":"); sub(/,/, "", t[3]); printf "%.3f", t[1] * 3600 + t[2] * 60 + t[3]; d=1}'
}

# Datei -> Mono 48 kHz, Hochpass, Rauschminderung, auf PRE_LUFS gebracht
#   $1 Eingabe, $2 Ausgabe (WAV), $3 Bezeichnung für Meldungen
prepare() {
  local in="$1" out="$2" label="$3" raw="$TMP/raw-$RANDOM.wav" i gain
  ff -i "$in" -vn -map 0:a:0 -ac 1 -ar 48000 -af "$CLEAN" -c:a pcm_f32le "$raw" \
    || die "$label: konnte nicht gelesen werden (keine Tonspur?)"
  i=$(measure_i "$raw")
  if [ -z "$i" ] || awk -v i="$i" 'BEGIN{exit !(i <= -60)}'; then
    die "$label: (fast) stumm – Mikro aus oder falsche Datei? (gemessen: ${i:-?} LUFS)"
  fi
  gain=$(calc "$PRE_LUFS - ($i)")
  ff -i "$raw" -af "volume=${gain}dB" -c:a pcm_f32le "$out"
  echo "$i"
}

# Sprache in einer (vorbereiteten) Aufnahme finden -> "Start Ende" in Sekunden (-1 -1 = nichts).
# Erkennung auf einer bandbegrenzten Kopie (Trittschall/Rumpeln und Zischen zählen weniger);
# Pausen < MIN_SILENCE gehören zur Sprache. Kurze Geräusche (< MIN_RUN s) ganz vorne/hinten mit
# >= ISOLATION s Abstand zur Sprache – typisch: Klick/Tippen beim Starten oder Stoppen der
# Aufnahme – werden ignoriert.
speech_bounds() {
  local in="$1" len
  len=$(duration "$in")
  { analyze -i "$in" -af "$BAND,silencedetect=noise=${SILENCE_DB}dB:d=$MIN_SILENCE" \
    | grep -oE 'silence_(start|end): -?[0-9.e+-]+' || true; } \
    | awk -v len="$len" -v minrun="$MIN_RUN" -v iso="$ISOLATION" '
      BEGIN { pos = 0; insil = 0; n = 0 }
      /silence_start/ { t = $2 + 0; if (t < 0) t = 0; if (!insil && t > pos) { n++; S[n] = pos; E[n] = t }; insil = 1 }
      /silence_end/   { pos = $2 + 0; insil = 0 }
      END {
        if (!insil && len > pos) { n++; S[n] = pos; E[n] = len }
        if (n == 0) { print "-1 -1"; exit }
        a = 1; b = n
        while (a < b && E[a] - S[a] < minrun && S[a + 1] - E[a] >= iso) a++
        while (b > a && E[b] - S[b] < minrun && S[b] - E[b - 1] >= iso) b--
        printf "%.3f %.3f\n", S[a], E[b]
      }'
}
# 15-ms-Blenden an beiden Enden (gegen Knackser an den Schnittkanten)
EDGE_FADES="afade=t=in:d=0.015,areverse,afade=t=in:d=0.015,areverse"

# Mono-Spur (WAV) -> Stereo 48 kHz MP3 mit -14 LUFS / -1,5 dBTP (loudnorm, 2 Durchgänge, linear)
finalize() {
  local in="$1" out="$2" st="$TMP/stereo.wav" pre="$TMP/limited.wav" i gain json mi mtp mlra mth moff
  ff -i "$in" -af "pan=stereo|c0=c0|c1=c0" -c:a pcm_f32le "$st"
  # grob auf Ziel-Lautheit + Limiter, damit loudnorm danach linear (ohne Pumpen) arbeiten kann
  i=$(measure_i "$st")
  gain=$(calc "$TARGET_LUFS - ($i)")
  ff -i "$st" -af "volume=${gain}dB,alimiter=limit=-2.5dB:attack=4:release=60:level=disabled" -c:a pcm_f32le "$pre"
  json=$(ffmpeg -nostdin -hide_banner -nostats -i "$pre" \
    -af "loudnorm=I=$TARGET_LUFS:TP=$TARGET_TP:LRA=20:print_format=json" -f null - 2>&1 | sed -n '/^{/,/^}/p')
  jget() { echo "$json" | sed -n "s/.*\"$1\" *: *\"\([^\"]*\)\".*/\1/p"; }
  mi=$(jget input_i); mtp=$(jget input_tp); mlra=$(jget input_lra); mth=$(jget input_thresh); moff=$(jget target_offset)
  [ -n "$mi" ] || die "loudnorm-Messung fehlgeschlagen"
  ff -i "$pre" -af "loudnorm=I=$TARGET_LUFS:TP=$TARGET_TP:LRA=20:measured_I=$mi:measured_TP=$mtp:measured_LRA=$mlra:measured_thresh=$mth:offset=$moff:linear=true,aresample=48000,atrim=end=$TOTAL,apad=whole_dur=$TOTAL" \
    -c:a pcm_f32le "$TMP/norm.wav"
  ff -i "$TMP/norm.wav" -ar 48000 -ac 2 -c:a libmp3lame -b:a 192k "$out"
  # MP3-Kodierung verschiebt die Lautheit oft um ein paar Zehntel -> einmal nachkorrigieren
  local li tp d
  li=$(measure_i "$out"); tp=$(measure_tp "$out")
  d=$(awk -v t="$TARGET_LUFS" -v i="$li" -v p="$tp" -v m="$TARGET_TP" \
    'BEGIN{d = t - i; if (p + d > m - 0.2) d = m - 0.2 - p; printf "%.2f", d}')
  if awk -v d="$d" 'BEGIN{exit !(d > 0.1 || d < -0.1)}'; then
    ff -i "$TMP/norm.wav" -af "volume=${d}dB" -ar 48000 -ac 2 -c:a libmp3lame -b:a 192k "$out"
  fi
}

echo "== Galacticfy Voiceover-Import ($([ $MODE = lines ] && echo "$N Zeilen-Dateien" || echo "1 Datei, ganzer Take")) =="
echo

if [ $MODE = lines ]; then
  # ---------------------------------------------------------------- 6 Zeilen-Dateien
  declare -a DUR START CUT
  for ((i = 0; i < N; i++)); do
    f="${FILES[$i]}"
    label="Zeile $((i + 1)) ($(basename "$f"))"
    prepare "$f" "$TMP/pre$i.wav" "$label" >/dev/null
    len=$(duration "$TMP/pre$i.wav")
    read -r s0 e0 <<<"$(speech_bounds "$TMP/pre$i.wav")"
    awk -v s="$s0" -v e="$e0" 'BEGIN{exit !(s < 0 || e - s < 0.2)}' \
      && die "$label: keine Sprache gefunden (zu leise oder nur Geräusche?)"
    a=$(awk -v s="$s0" -v p="$PAD_IN" 'BEGIN{x = s - p; if (x < 0) x = 0; printf "%.3f", x}')
    b=$(awk -v e="$e0" -v p="$PAD_OUT" -v l="$len" 'BEGIN{x = e + p; if (x > l) x = l; printf "%.3f", x}')
    # vorne/hinten weggeschnitten (zur Kontrolle in der Tabelle)
    CUT[$i]="$(fmt "$a")/$(fmt "$(calc "$len - $b")")"
    ff -i "$TMP/pre$i.wav" -af "atrim=start=$a:end=$b,asetpts=PTS-STARTPTS,$EDGE_FADES,$COMP" -c:a pcm_f32le "$TMP/line$i.wav"
    DUR[$i]=$(duration "$TMP/line$i.wav")
  done

  echo "Zeile  Fenster         Platz    Länge    weg v/h s    liegt bei       Status"
  prev_end=0
  overruns=0
  srcs=""   # Stille-Quellen (anullsrc)
  chain=""  # Reihenfolge für concat
  inputs=()
  idx=0
  for ((i = 0; i < N; i++)); do
    nominal=$(calc "${SEG_FROM[$i]} + $LEAD")
    avail=$(calc "${SEG_TO[$i]} - $END_MARGIN - $nominal")
    start=$(awk -v n="$nominal" -v p="$prev_end" -v g="$MIN_GAP" -v i="$i" 'BEGIN{s = n; if (i > 0 && p + g > s) s = p + g; printf "%.3f", s}')
    end=$(calc "$start + ${DUR[$i]}")
    START[$i]=$start
    over=$(calc "${DUR[$i]} - $avail")
    if awk -v o="$over" 'BEGIN{exit !(o > 0.005)}'; then
      status="ZU LANG: +$(fmt "$over") s"
      overruns=$((overruns + 1))
    elif awk -v s="$start" -v n="$nominal" 'BEGIN{exit !(s > n + 0.005)}'; then
      status="verschoben: startet $(fmt "$(calc "$start - $nominal")") s zu spät (Vorzeile zu lang)"
      overruns=$((overruns + 1))
    else
      status="ok (Luft $(fmt "$(calc "-($over)")") s)"
    fi
    printf '%-6s %-14s %-8s %-8s %-12s %-15s %s\n' "$((i + 1))" \
      "$(fmt "${SEG_FROM[$i]}")-$(fmt "${SEG_TO[$i]}") s" "$(fmt "$avail") s" "$(fmt "${DUR[$i]}") s" \
      "${CUT[$i]}" "$(fmt "$start")-$(fmt "$end") s" "$status"
    gap=$(calc "$start - $prev_end")
    if awk -v g="$gap" 'BEGIN{exit !(g > 0.0005)}'; then
      srcs+="anullsrc=r=48000:cl=mono:d=$gap[g$i];"
      chain+="[g$i]"
      idx=$((idx + 1))
    fi
    inputs+=(-i "$TMP/line$i.wav")
    chain+="[$i:a]"
    idx=$((idx + 1))
    prev_end="$end"
  done
  echo "(weg v/h = vorne/hinten weggeschnittene Pause. Wurde bei einer Zeile kaum etwas"
  echo " weggeschnitten, obwohl du Pausen gemacht hast, ist dort ein Geräusch (Atmer, Rascheln)"
  echo " mit drin – die Zeile ist dann länger als nötig: anhören oder neu aufnehmen.)"
  echo
  if awk -v e="$prev_end" -v t="${SEG_TO[$((N - 1))]}" 'BEGIN{exit !(e > t)}'; then
    warn "Die letzte Zeile endet erst bei $(fmt "$prev_end") s – die End-Card beginnt bei $(fmt "${SEG_TO[$((N - 1))]}") s."
  fi
  if awk -v e="$prev_end" -v t="$TOTAL" 'BEGIN{exit !(e > t)}'; then
    warn "Gesamtlänge $(fmt "$prev_end") s > $(fmt "$TOTAL") s – das Ende wird abgeschnitten!"
  fi
  if [ $overruns -gt 0 ]; then
    warn "$overruns Zeile(n) passen nicht in ihr Zeitfenster – Ton und Captions laufen dort auseinander."
    echo "         Entweder diese Zeilen etwas schneller/kürzer neu aufnehmen, oder die Captions" >&2
    echo "         umtimen: in src/config.ts SEGMENTS from/to z. B. so setzen (clip.start/end mitziehen)" >&2
    echo "         und SEG_FROM/SEG_TO oben in diesem Skript genauso, dann das Skript nochmal starten:" >&2
    sug=""
    for ((i = 0; i < N; i++)); do
      if [ "$i" -eq 0 ]; then f0=${SEG_FROM[0]}; else f0=$(awk -v a="${SEG_FROM[$i]}" -v s="${START[$i]}" -v l="$LEAD" 'BEGIN{x = s - l; if (a > x) x = a; printf "%.2f", x}'); fi
      if [ "$i" -lt $((N - 1)) ]; then
        t0=$(awk -v a="${SEG_FROM[$((i + 1))]}" -v s="${START[$((i + 1))]}" -v l="$LEAD" 'BEGIN{x = s - l; if (a > x) x = a; printf "%.2f", x}')
      else
        t0=$(awk -v t="${SEG_TO[$i]}" -v e="$prev_end" -v m="$END_MARGIN" 'BEGIN{x = e + m; if (t > x) x = t; printf "%.2f", x}')
      fi
      sug+="$((i + 1)): $(fmt "$f0")-$(fmt "$t0")   "
    done
    echo "         $sug" >&2
    echo "         (Video wird dann $(fmt "$(calc "$t0 + $END_CARD_SECONDS")") s lang.)" >&2
    echo >&2
  fi
  # Spur zusammensetzen: Stille, Zeile, Stille, Zeile, ... (überlappt nie)
  filter="$srcs${chain}concat=n=$idx:v=0:a=1,atrim=end=$TOTAL,afade=t=out:st=$(calc "$TOTAL - 0.08"):d=0.08[out]"
  ff "${inputs[@]}" -filter_complex "$filter" -map "[out]" -c:a pcm_f32le "$TMP/track.wav"
else
  # ---------------------------------------------------------------- 1 Datei (ganzer Take)
  f="${FILES[0]}"
  prepare "$f" "$TMP/pre.wav" "$(basename "$f")" >/dev/null
  shift_s="$OFFSET"
  if [ $AUTO_START = 1 ]; then
    # Anfang der ersten Sprechpassage (kurze Klicks davor, z. B. beim Start der Aufnahme, zählen nicht)
    read -r first _ <<<"$(speech_bounds "$TMP/pre.wav")"
    awk -v s="$first" 'BEGIN{exit !(s < 0)}' && die "$(basename "$f"): keine Sprache gefunden (zu leise?)"
    shift_s=$(calc "$first - $LEAD + ($OFFSET)")
    echo "Auto-Start: erstes Wort bei $(fmt "$first") s in der Aufnahme -> wird auf $(fmt "$(calc "$LEAD - ($OFFSET)")") s gelegt."
  fi
  if awk -v s="$shift_s" 'BEGIN{exit !(s > 0)}'; then
    pos="atrim=start=$shift_s,asetpts=PTS-STARTPTS,"
  elif awk -v s="$shift_s" 'BEGIN{exit !(s < 0)}'; then
    pos="adelay=delays=$(awk -v s="$shift_s" 'BEGIN{printf "%d", -s * 1000}'):all=1,"
  else
    pos=""
  fi
  rec_len=$(duration "$TMP/pre.wav")
  ff -i "$TMP/pre.wav" -af "${pos}$COMP,atrim=end=$TOTAL,afade=t=out:st=$(calc "$TOTAL - 0.08"):d=0.08,apad=whole_dur=$TOTAL" -c:a pcm_f32le "$TMP/track.wav"
  usable=$(calc "$rec_len - ($shift_s)")
  if awk -v u="$usable" -v t="$TOTAL" 'BEGIN{exit !(u > t + 0.05)}'; then
    echo "Hinweis: Aufnahme ist $(fmt "$usable") s lang -> auf $(fmt "$TOTAL") s gekürzt (sanft ausgeblendet)."
  else
    echo "Aufnahme: $(fmt "$usable") s -> mit Stille auf $(fmt "$TOTAL") s aufgefüllt."
  fi
  echo
fi

# ---------------------------------------------------------------- Ausgabe
mkdir -p "$(dirname "$OUT")"
finalize "$TMP/track.wav" "$TMP/out.mp3"

if [ "$(cd "$(dirname "$OUT")" && pwd)/$(basename "$OUT")" = "$DEFAULT_OUT" ] && [ -f "$DEFAULT_OUT" ] && [ ! -f "$TTS_BACKUP" ]; then
  cp "$DEFAULT_OUT" "$TTS_BACKUP"
  echo "Bisheriges Voiceover (TTS) gesichert: public/voiceover-tts.mp3"
fi
mv "$TMP/out.mp3" "$OUT"

if [ $MODE = single ]; then
  # Sprechpassagen erkennen (Pausen >= 0,15 s) und neben die Caption-Fenster stellen
  echo "Erkannte Sprechpassagen (zum Vergleich mit den Caption-Fenstern):"
  { analyze -i "$OUT" -af "silencedetect=noise=${DETECT_DB}dB:d=0.15" \
    | grep -oE 'silence_(start|end): -?[0-9.e+-]+' || true; } \
    | awk -v total="$TOTAL" -v minrun="$MIN_RUN" -v from="${SEG_FROM[*]}" -v to="${SEG_TO[*]}" '
      BEGIN { n = split(from, F, " "); split(to, T, " "); pos = 0; insil = 0; cnt = 0 }
      function report(a, b,   k, seg, note) {
        if (b - a < minrun) return
        seg = 0
        for (k = 1; k <= n; k++) if (a >= F[k] && a < T[k]) seg = k
        if (seg == 0) note = (a >= T[n]) ? "in der End-Card (dort ist keine Caption)" : "?"
        else if (b > T[seg] + 0.05) note = sprintf("Zeile %d (%.2f–%.2f s) – läuft %.2f s ins nächste Fenster!", seg, F[seg], T[seg], b - T[seg])
        else note = sprintf("Zeile %d (%.2f–%.2f s) ok", seg, F[seg], T[seg])
        printf "  %6.2f – %6.2f s  (%.2f s)   %s\n", a, b, b - a, note
        cnt++
      }
      /silence_start/ { t = $2 + 0; if (t < 0) t = 0; if (!insil) report(pos, t); insil = 1 }
      /silence_end/   { pos = $2 + 0; insil = 0 }
      END { if (!insil) report(pos, total); if (cnt == 0) print "  (keine Sprache erkannt?)" }'
  echo
  echo "Captions: $(for ((i = 0; i < N; i++)); do printf '%s–%s s  ' "$(fmt "${SEG_FROM[$i]}")" "$(fmt "${SEG_TO[$i]}")"; done)"
  echo "Passen die Passagen nicht: in src/config.ts SEGMENTS (from/to) daran anpassen – oder mit"
  echo "--offset SEK / --auto-start verschieben – oder die 6 Sätze einzeln aufnehmen (empfohlen)."
  echo
fi

li=$(measure_i "$OUT")
tp=$(measure_tp "$OUT")
echo "Fertig: ${OUT#"$ROOT"/}  ($(fmt "$(duration "$OUT")") s, 48 kHz Stereo, ${li} LUFS, True Peak ${tp} dBTP)"
if [ "$OUT" = "$DEFAULT_OUT" ]; then
  echo "Jetzt rendern:  npm run render   (stumme Fassung: npm run render:ohne-ton)"
  [ -f "$TTS_BACKUP" ] && echo "Zurück zur TTS-Stimme:  cp public/voiceover-tts.mp3 public/voiceover.mp3"
fi
