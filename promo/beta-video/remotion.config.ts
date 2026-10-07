import {Config} from '@remotion/cli/config';
import fs from 'fs';

// Vorinstalliertes Chromium verwenden (keinen Browser herunterladen).
// Auf einem anderen Rechner diese Zeile einfach entfernen/anpassen.
const LOCAL_CHROMIUM =
  '/opt/pw-browsers/chromium_headless_shell-1194/chrome-linux/headless_shell';

if (fs.existsSync(LOCAL_CHROMIUM)) {
  Config.setBrowserExecutable(LOCAL_CHROMIUM);
}

Config.setVideoImageFormat('jpeg');
Config.setJpegQuality(90);
Config.setCodec('h264');
// CRF 23 wird in den Render-Befehlen übergeben (package.json, scripts/render.sh) – nicht hier,
// sonst scheitert das reine Ton-Rendern (--codec=wav) für die Fassung ohne Musik.
Config.setPixelFormat('yuv420p');
Config.setOverwriteOutput(true);
