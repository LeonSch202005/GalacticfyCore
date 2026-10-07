#!/usr/bin/env bash
# Erzeugt public/voiceover.mp3 - deutsches TTS-Voiceover (25,0 s, 48 kHz Stereo, ~-14 LUFS).
#
#   bash scripts/make-voiceover.sh            # -> public/voiceover.mp3
#   bash scripts/make-voiceover.sh --out x.mp3
#
# Danach in src/config.ts USE_VOICEOVER = true setzen und neu rendern.
#
# Stimme: Piper/VITS "de_DE-thorsten-high" (Thorsten-Voice, CC0), als Release-Asset
# des sherpa-onnx-Projekts auf GitHub (huggingface.co ist hier gesperrt).
# Texte, Aussprache-Korrekturen und Timing-Logik: scripts/make-voiceover.py
#
# Benoetigt: python3 (venv), ffmpeg + ffprobe (mit libmp3lame), curl, tar + bzip2.
# Cache fuer venv + Stimme (~500 MB): $VOICEOVER_CACHE, Standard ~/.cache/galacticfy-voiceover
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CACHE="${VOICEOVER_CACHE:-$HOME/.cache/galacticfy-voiceover}"
VOICE_URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-de_DE-thorsten-high.tar.bz2"
VOICE_SHA256="dd4ed1b0d42c30a1a4862fc2b243e8044d52b8889c9ff3d1e99e92028888bc4a"
VOICE_DIR="$CACHE/voice-thorsten-high"
MODEL="$VOICE_DIR/vits-piper-de_DE-thorsten-high/de_DE-thorsten-high.onnx"

mkdir -p "$CACHE"

if [ ! -f "$CACHE/venv/.installed" ]; then
  python3 -m venv "$CACHE/venv"
  "$CACHE/venv/bin/pip" install --quiet piper-tts==1.8.0 onnxruntime==1.30.0 numpy==2.5.3
  touch "$CACHE/venv/.installed"
fi

if [ ! -f "$VOICE_DIR/.ok" ]; then
  rm -rf "$VOICE_DIR"
  mkdir -p "$VOICE_DIR"
  curl -fL --retry 3 -o "$VOICE_DIR/voice.tar.bz2" "$VOICE_URL"
  echo "$VOICE_SHA256  $VOICE_DIR/voice.tar.bz2" | sha256sum -c -
  # nur Modell + Konfiguration entpacken (espeak-ng-Daten bringt piper-tts selbst mit)
  tar -xjf "$VOICE_DIR/voice.tar.bz2" -C "$VOICE_DIR" --no-same-owner \
    vits-piper-de_DE-thorsten-high/de_DE-thorsten-high.onnx \
    vits-piper-de_DE-thorsten-high/de_DE-thorsten-high.onnx.json \
    vits-piper-de_DE-thorsten-high/MODEL_CARD
  rm "$VOICE_DIR/voice.tar.bz2"
  touch "$VOICE_DIR/.ok"
fi

# -I: isolierter Modus (weder CWD noch PYTHONPATH landen im Importpfad)
exec "$CACHE/venv/bin/python" -I "$HERE/make-voiceover.py" --model "$MODEL" "$@"
