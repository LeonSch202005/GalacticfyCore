#!/usr/bin/env bash
# Leons 2. Aufnahme (ein ganzer Take) -> fertige Sprachspur public/voiceover.mp3
#
#   bash scripts/make-voice-take2.sh                 # Standard: Quelle/Ziel wie unten
#   bash scripts/make-voice-take2.sh --in X.mp3 --out probe.mp3
#   TMPDIR=/irgendwo bash scripts/make-voice-take2.sh   # Arbeitsordner für Zwischendateien
#   VOICE_WORK=ordner bash scripts/make-voice-take2.sh   # Zwischendateien dort behalten
#
# Was passiert:
#   1. Schnitt: Die langen Pausen zwischen den Sätzen werden gekürzt (nur innerhalb von Stille –
#      die Aufnahme hat dort digitale Stille / Raumrauschen), Übergänge mit 15 ms Crossfade.
#      Atmer/Schmatzer in den Pausen fallen dabei mit raus. Vorne bleiben 0,15 s bis zum ersten
#      Wort, hinter "Link in der Bio" wird sanft ausgeblendet. Wortenden, die in der Aufnahme hart
#      an Stille stoßen, werden kurz ausgeblendet; beim abgeschnittenen "dich" wird das "ch" aus
#      "nicht" angesetzt.
#         Satzpausen ~0,40 s, vor "Aber Achtung" und "Komm jetzt" ~0,50 s (kurzer Spannungs-Beat).
#      Das Einatmen in "nicht fertig … und genau" wird um 8 dB leiser gemacht (bleibt drin), ein
#      Mundklick zwischen "exklusiven" und "Betatester-Prefix" wird stummgeschaltet (CLICKS).
#   2. Klang: Hochpass 80 Hz -> Vorverstärkung -> sanfte Rauschminderung (afftdn, nr 8 dB, fester Boden) ->
#      leichte Absenkung 300 Hz (-1,5 dB) + Präsenz +2,5 dB um 4 kHz -> leichter De-Esser ->
#      Kompressor 2,8:1 (Schwelle -18 dB, ~2 dB im Mittel) -> Limiter (Spitzen) -> loudnorm zweistufig (messen, dann linear
#      anwenden) auf -14 LUFS / -1,5 dBTP -> 48 kHz Stereo -> mit Stille auf Videolänge
#      aufgefüllt -> MP3 192 kbit/s.
#
# Die Schnittliste (PIECES) bezieht sich auf Sekunden in der ROH-Aufnahme
# (public/voiceover-original/leon-aufnahme-2.mp3, wie ffmpeg sie dekodiert).
# Benötigt: bash, ffmpeg (mit libmp3lame), awk, grep, sed.
set -euo pipefail
export LC_ALL=C

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/.." && pwd)"
IN="$ROOT/public/voiceover-original/leon-aufnahme-2.mp3"
OUT="$ROOT/public/voiceover.mp3"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --in) IN="$2"; shift 2 ;;
    --out) OUT="$2"; shift 2 ;;
    -h|--help) sed -n '2,26p' "$0"; exit 0 ;;
    *) echo "Unbekannte Option: $1" >&2; exit 1 ;;
  esac
done
[[ -f "$IN" ]] || { echo "Quelle fehlt: $IN" >&2; exit 1; }

# ------------------------------------------------------------------------------
#  Schnittliste: behaltene Stücke "von bis [Ausblenden]" (Sekunden in der Roh-Aufnahme).
#  Jeder Schnittpunkt liegt in Stille (digitale Null oder Raumrauschen < -55 dBFS).
# ------------------------------------------------------------------------------
PIECES=(
  "0.465 5.866"        # Dieser Minecraft-Server ist noch nicht fertig … und genau deshalb brauchen wir di-
  "2.512 2.615 0.040"  # [ç] aus "ni(ch)t": In der Aufnahme ist "dich" direkt nach dem i hart abgeschnitten
                       # (Stille ab 5,877) -> das "ch" (gleicher Laut, gleicher Kontext "i_") wird angesetzt
  "5.950 6.223"        # Pause (digitale Stille aus der Aufnahme)
  "6.880 11.299"       # Galacticfy sucht 20 Betatester – für Java und Bedrock
  "12.120 20.679"      # Teste unsere Systeme, finde Bugs, check die Menüs … perfekt auszubalancieren
  "21.140 27.516"      # Als Dankeschön … Betatester-Prefix und weitere Belohnungen
  "27.920 32.450"      # Aber Achtung: Es gibt nur 20 Plätze – wer zuerst kommt, mahlt zuerst
  "33.125 36.370"      # Komm jetzt auf unseren Discord und öffne ein Ticket!
  "36.590 37.950 0.10" # Link in der Bio.  (Ausblenden nach "Bio", Rauschen ~ -60 dBFS)
)
# In der Aufnahme enden einige Wörter hart an digitaler Stille (Schnitt beim Aufnehmen) ->
# kurz ausblenden, damit das Wortende nicht "abgehackt" klingt: "Ende Dauer" in Roh-Sekunden.
FADES=(
  "10.942 0.030"   # Bedrock
  "20.292 0.040"   # auszubalancieren
  "27.074 0.040"   # Belohnungen
  "32.034 0.030"   # zuerst
)
# Atmer innerhalb eines Satzes nur leiser machen (nicht schneiden): "von bis dB" in Roh-Sekunden,
# mit 15 ms Rampen. 3,34–3,73: hörbares Einatmen in der Pause "nicht fertig … und genau".
BREATHS=(
  "3.340 3.730 -8"
)
# Mundklicks (Schmatzer) in Sprechpausen stummschalten – gleiche Rechnung wie BREATHS ("von bis dB",
# 15-ms-Rampen, ganz leise zwischen von+0,015 und bis-0,015). 23,821: 2-ms-Knack zwischen
# "exklusiven" und "Betatester-Prefix" (im Video bei 21,476 s), ~25 dB über dem Raumrauschen.
CLICKS=(
  "23.805 23.840 -40"
)
RAMP=0.015
LEAD=0.115          # Stille vor dem 1. Stück (Stück 1 beginnt 0,035 s vor "Dieser" -> Wort bei 0,15 s)
XF=0.015            # Crossfade an jedem Schnitt (s)
FADE_IN=0.010       # Einblenden am Anfang
TOTAL=37.566667     # Länge der fertigen Spur = Videolänge = 1127 Frames @30 fps
                    # ("Bio" endet bei ~34,16 s + 0,4 s -> Discord-Segment bis 34,567 s (Frame 1037) + 3,0 s End-Card)

# ---- Klang ----
PREGAIN=16          # dB, bringt die leise Aufnahme (~ -36 LUFS) vor dem Kompressor auf ~ -20 LUFS
# afftdn verzögert um 1200 Samples (25 ms bei 48 kHz) -> direkt wieder vorne abschneiden, damit das Timing stimmt
CLEAN="highpass=f=80,volume=${PREGAIN}dB,afftdn=nr=8:nf=-50,atrim=start_sample=1200,asetpts=PTS-STARTPTS"
EQ="equalizer=f=300:t=q:w=1.0:g=-1.5,equalizer=f=4000:t=q:w=0.9:g=2.5"
DEESS="deesser=i=0.4:m=0.5:f=0.5:s=o"   # leicht: im Schnitt ~1 dB auf S-Lauten, max ~6 dB
COMP="acompressor=threshold=-18dB:ratio=2.8:attack=5:release=120:knee=4:detection=rms"
PRE_LUFS=-20        # Pegel (Mono) vor dem Limiter ...
LIMIT_DB=-6.0       # ... Limiter-Decke (Sample-Peak). Stereo misst +3 LU -> -17 LUFS, Peaks <= ~-5,5 dBTP,
                    # loudnorm braucht dann ~+3 dB und bleibt LINEAR (TP bleibt unter -1,5 dBTP)
TARGET_LUFS=-14
TARGET_TP=-1.5
TARGET_LRA=20       # nur Obergrenze, damit loudnorm nicht in den dynamischen Modus fällt

if [[ -n "${VOICE_WORK:-}" ]]; then   # Zwischendateien behalten (zum Nachprüfen)
  WORK="$VOICE_WORK"; mkdir -p "$WORK"
else
  WORK="$(mktemp -d "${TMPDIR:-/tmp}/voice-take2.XXXXXX")"
  trap 'rm -rf "$WORK"' EXIT
fi
FF=(ffmpeg -hide_banner -nostdin -y)

# ---------------------------------------------------------------- 1. Schnitt
n=${#PIECES[@]}
vol="1"
for br in "${BREATHS[@]}" "${CLICKS[@]}"; do
  read -r a b g <<< "$br"
  vol+=$(awk -v a="$a" -v b="$b" -v g="$g" -v r="$RAMP" \
    'BEGIN{printf "*(1-%.4f*clip(min((t-%s)/%s,(%s-t)/%s),0,1))", 1 - 10 ^ (g / 20), a, r, b, r}')
done
for fd in "${FADES[@]}"; do
  read -r e d <<< "$fd"
  vol+=$(awk -v e="$e" -v d="$d" 'BEGIN{printf "*if(between(t,%.4f,%.4f),clip((%s-t)/%s,0,1),1)", e - d, e + 0.3, e, d}')
done
# Jedes Stück einzeln rendern (Stück 2 stammt aus einer früheren Stelle der Aufnahme – in einem
# gemeinsamen asplit-Graphen würde acrossfade dessen Ende zu früh sehen und Stück 1 kappen).
PRE="aformat=sample_fmts=fltp:sample_rates=48000:channel_layouts=mono,asetnsamples=n=48:p=0,volume=volume='$vol':eval=frame"
echo "Schnitt (Roh-Sekunden -> Sekunden in voiceover.mp3):"
pos=$LEAD
inputs=()
for ((i = 1; i <= n; i++)); do
  read -r a b fo <<< "${PIECES[$((i - 1))]}"
  f="$PRE,atrim=start=$a:end=$b,asetpts=PTS-STARTPTS"
  if ((i == 1)); then f+=",afade=t=in:st=0:d=$FADE_IN"; fi
  if [[ -n "${fo:-}" ]]; then
    st=$(awk -v a="$a" -v b="$b" -v d="$fo" 'BEGIN{printf "%.4f", b - a - d}')
    f+=",afade=t=out:st=$st:d=$fo"
  fi
  "${FF[@]}" -loglevel error -i "$IN" -af "$f" -c:a pcm_f32le "$WORK/piece$i.wav"
  inputs+=(-i "$WORK/piece$i.wav")
  if ((i > 1)); then pos=$(awk -v p="$pos" -v x="$XF" 'BEGIN{printf "%.3f", p - x}'); fi
  awk -v i="$i" -v a="$a" -v b="$b" -v p="$pos" 'BEGIN{
    printf "  Stück %d: roh %7.3f–%7.3f  ->  %7.3f–%7.3f   (Verschiebung %+.3f s)\n", i, a, b, p, p + b - a, p - a}'
  pos=$(awk -v p="$pos" -v a="$a" -v b="$b" 'BEGIN{printf "%.3f", p + b - a}')
done
echo "  geschnittene Länge: $pos s"
fc=""
prev="0:a"
for ((i = 2; i <= n; i++)); do
  fc+="[$prev][$((i - 1)):a]acrossfade=d=$XF:c1=tri:c2=tri[x$i];"
  prev="x$i"
done
lead_ms=$(awk -v l="$LEAD" 'BEGIN{printf "%d", l * 1000 + 0.5}')
fc+="[$prev]adelay=delays=$lead_ms:all=1,apad=pad_dur=0.1[out]"   # +0,1 s Stille: afftdn gibt sein Ende sonst nicht ganz aus
"${FF[@]}" -loglevel error "${inputs[@]}" -filter_complex "$fc" -map "[out]" -c:a pcm_f32le "$WORK/edit.wav"

# ---------------------------------------------------------------- 2. Klang
loud_i() { # integrierte Lautheit (LUFS) einer Datei
  "${FF[@]}" -loglevel info -i "$1" -af ebur128 -f null - 2>&1 |
    awk '/Integrated loudness:/{f=1} f && /I:/{print $2; exit}'
}
"${FF[@]}" -loglevel error -i "$WORK/edit.wav" -af "$CLEAN,$EQ,$DEESS,$COMP" -c:a pcm_f32le "$WORK/comp.wav"
I_COMP=$(loud_i "$WORK/comp.wav")
GAIN=$(awk -v i="$I_COMP" -v t="$PRE_LUFS" 'BEGIN{printf "%.2f", t - i}')
LIM=$(awk -v d="$LIMIT_DB" 'BEGIN{printf "%.4f", 10 ^ (d / 20)}')
echo "Nach Kompressor: $I_COMP LUFS -> ${GAIN} dB bis $PRE_LUFS LUFS, Limiter-Decke $LIMIT_DB dBFS"
"${FF[@]}" -loglevel error -i "$WORK/comp.wav" \
  -af "volume=${GAIN}dB,alimiter=limit=$LIM:attack=4:release=60:level=disabled:asc=1:latency=1,pan=stereo|c0=c0|c1=c0" \
  -c:a pcm_f32le "$WORK/lim.wav"

# (Stereo schon VOR loudnorm: Dual-Mono misst +3 LU lauter als Mono – gemessen wird, was rauskommt)
# loudnorm Durchgang 1: messen
LN="loudnorm=I=$TARGET_LUFS:TP=$TARGET_TP:LRA=$TARGET_LRA"
json=$(ffmpeg -hide_banner -nostdin -i "$WORK/lim.wav" -af "$LN:print_format=json" -f null - 2>&1 |
  sed -n '/^{/,/^}/p')
get() { echo "$json" | grep "\"$1\"" | sed -E 's/.*: *"([^"]*)".*/\1/'; }
MI=$(get input_i); MTP=$(get input_tp); MLRA=$(get input_lra); MTH=$(get input_thresh); OFF=$(get target_offset)
echo "loudnorm Messung: I=$MI LUFS, TP=$MTP dBTP, LRA=$MLRA LU, Schwelle=$MTH, Offset=$OFF"

# loudnorm Durchgang 2: linear anwenden, auf Videolänge auffüllen, MP3.
# libmp3lame landet ~0,25 dB leiser als die WAV-Vorlage -> MP3 messen und einmal mit Korrektur neu kodieren.
mkdir -p "$(dirname "$OUT")"
encode() { # $1 = Korrektur in dB (nach loudnorm)
  "${FF[@]}" -loglevel info -i "$WORK/lim.wav" -af \
    "$LN:measured_I=$MI:measured_TP=$MTP:measured_LRA=$MLRA:measured_thresh=$MTH:offset=$OFF:linear=true:print_format=summary,aresample=48000,volume=${1}dB,apad=whole_dur=$TOTAL,atrim=end=$TOTAL" \
    -ar 48000 -ac 2 -c:a libmp3lame -b:a 192k "$OUT" 2>&1
}
log=$(encode 0)
echo "$log" | grep -E "Normalization Type|Output Integrated|Output True Peak" | sed 's/^/  /'
if echo "$log" | grep -q "Normalization Type: *Dynamic"; then
  echo "WARNUNG: loudnorm ist in den dynamischen Modus gefallen (LIMIT_DB senken)." >&2
fi
I_MP3=$(loud_i "$OUT")
CORR=$(awk -v i="$I_MP3" -v t="$TARGET_LUFS" 'BEGIN{printf "%.2f", t - i}')
if awk -v c="$CORR" 'BEGIN{exit !(c > 0.05 || c < -0.05)}'; then
  echo "MP3 misst $I_MP3 LUFS -> Korrektur ${CORR} dB, neu kodieren"
  encode "$CORR" > /dev/null
fi

# ---------------------------------------------------------------- Kontrolle
echo "Fertig: $OUT"
ffprobe -v error -show_entries format=duration:stream=sample_rate,channels -of default=nw=1 "$OUT" | sed 's/^/  /'
ffmpeg -hide_banner -nostdin -i "$OUT" -af ebur128=peak=true -f null - 2>&1 |
  awk '/Summary:/{f=1} f' | grep -E "I:|LRA:|Peak:" | sed 's/^ */  /'
