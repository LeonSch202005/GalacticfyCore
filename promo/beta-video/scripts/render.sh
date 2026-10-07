#!/usr/bin/env bash
# ------------------------------------------------------------------------------------------
#  render.sh – fertige Videos rendern + Ton mastern
#
#    bash scripts/render.sh                 # beide Fassungen (mit + ohne Musik)
#    bash scripts/render.sh --nur-musik     # nur out/galacticfy-betatester.mp4
#
#  Ergebnis:
#    out/galacticfy-betatester.mp4             Stimme + Musik + Sound-Effekte
#    out/galacticfy-betatester-ohne-musik.mp4  gleiches Bild, Stimme + Sound-Effekte
#
#  Warum ein Skript? Deine Stimme ist auf −14 LUFS / −1,9 dBTP gemastert – Einschläge, die
#  genau auf den betonten Silben landen, würden die Summe sonst über 0 dBFS treiben
#  (Remotion mischt in 16 Bit -> Clipping). Deshalb:
#    1. Remotion rendert mit masterGain 0,708 (−3 dB Headroom) und verlustfreiem PCM-Ton (.mkv)
#    2. ffmpeg holt die 3 dB zurück und fängt die wenigen Spitzen mit einem 4×-überabgetasteten
#       Limiter (−1,5 dBFS) ab -> True Peak ≤ −1 dBTP, Lautheit bleibt ≈ −14 LUFS
#    3. Bild wird nur kopiert (kein zweites Encodieren), Ton als AAC 192 kbit/s
#  Braucht: bash, npx (Node), ffmpeg.
# ------------------------------------------------------------------------------------------
set -euo pipefail
cd "$(dirname "$0")/.."

BOTH=1
[[ "${1:-}" == "--nur-musik" ]] && BOTH=0

GAIN_DB=3
MASTER_GAIN=0.7079   # = 10^(−3/20)
PROPS="{\"masterGain\":${MASTER_GAIN}}"
# Limiter: 4× Abtastrate (fängt auch Spitzen ZWISCHEN den Samples = True Peak), 1,5 ms Vorschau,
# Laufzeit kompensiert (Ton bleibt lippensynchron), keine automatische Pegel-Anhebung.
AF="volume=${GAIN_DB}dB,aresample=192000,alimiter=limit=0.84:attack=1.5:release=80:level=0:latency=1,aresample=48000"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p out

echo "==> Video + Ton (mit Musik) rendern …"
npx remotion render GalacticfyBeta "$TMP/video.mkv" --codec=h264 --crf=23 --audio-codec=pcm-16 --props="$PROPS"

master() { # $1 = Ton-Quelle, $2 = Ziel
  ffmpeg -hide_banner -loglevel error -y -i "$TMP/video.mkv" -i "$1" -map 0:v:0 -map 1:a:0 \
    -c:v copy -af "$AF" -c:a aac -b:a 192k -ar 48000 -movflags +faststart -shortest "$2"
  echo "--- $2"
  ffmpeg -hide_banner -nostats -i "$2" -map 0:a -af ebur128=peak=true -f null - 2>&1 |
    awk '/Integrated loudness/{f=1} f&&/I:/{print "    Lautheit:  "$2" LUFS"} f&&/Peak:/{print "    True Peak: "$2" dBTP"}'
}

master "$TMP/video.mkv" out/galacticfy-betatester.mp4

if [[ $BOTH == 1 ]]; then
  echo "==> Ton ohne Musik rendern (nur Audio, gleiches Bild) …"
  npx remotion render GalacticfyBeta-OhneMusik "$TMP/ohne-musik.wav" --codec=wav --props="$PROPS"
  master "$TMP/ohne-musik.wav" out/galacticfy-betatester-ohne-musik.mp4
fi
echo "Fertig."
