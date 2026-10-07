# Galacticfy – Betatester-Promo (Remotion)

Vertikales Promo-Video (1080×1920, 30 fps, ~25 s) für TikTok / Reels / Shorts.
Stummes, caption-getriebenes Video – ein Voiceover kann später darüber gelegt werden.
Im Hintergrund läuft echtes Gameplay vom Server (`public/clips/`), darüber
Galacticfy-Look (Lila-Stich, Sterne/Pixel-Partikel, Neon-Captions, Glas-Panels).

Fertiges Video: `out/galacticfy-betatester.mp4`

## Setup

```bash
cd promo/beta-video
npm install
```

## Vorschau / Bearbeiten

```bash
npx remotion studio      # oder: npm run studio
```

Öffnet die Remotion-Studio-Vorschau im Browser (Timeline, Frame-genaues Scrubbing).

## Rendern

```bash
npm run render           # -> out/galacticfy-betatester.mp4 (H.264, CRF 23)
npm run stills           # Kontroll-Standbilder (Frames 45/130/250/400/530/620/700) nach out/
```

`remotion.config.ts` nutzt das vorinstallierte Chromium unter
`/opt/pw-browsers/...`, falls vorhanden – sonst lädt Remotion selbst einen
Headless-Browser. Pfad dort bei Bedarf anpassen.

## Texte & Timings ändern

**Alles steht in `src/config.ts`:**

- `SEGMENTS` – die sechs Caption-Abschnitte mit `from`/`to` in Sekunden.
  `**Wort**` wird in Neon-Cyan hervorgehoben. `fontSize` optional pro Segment.
- `clip` je Segment – welcher Gameplay-Clip dahinter läuft (siehe unten).
- `CHECKLIST`, `CHAT`, `SLOTS`, `DISCORD` – Inhalte der animierten Visuals.
- `END_CARD` / `END_CARD_FOOTER` / `END_CARD_SECONDS` – Abschlusskarte.
- `END_CARD_CLIP` / `END_CARD_CLIP_BLUR` / `END_CARD_CLIP_DIM` – weichgezeichnetes
  Gameplay hinter der End-Card (`null` = nur Weltraum-Hintergrund).
- `COLORS`, Schriften (`FONT_HEAVY`, `FONT_PIXEL`).

Die Gesamtlänge ergibt sich automatisch aus dem letzten Segment + End-Card.

Hinweis: Statt Emojis werden eigene Pixel-Icons (`src/PixelIcon.tsx`) genutzt,
weil der Headless-Browser keine Emoji-Schrift hat.

## Gameplay-Clips austauschen

Die Aufnahmen liegen in `public/clips/` (16:9, z. B. 1920×1080, 30 fps, H.264, ohne Ton).
Sie werden automatisch mittig auf 9:16 zugeschnitten. Aktuelle Belegung:

| Segment | Zeit | Clip | Ausschnitt im Clip |
|---|---|---|---|
| 1 Intro („nicht fertig“) | 0–3 s | `spawn-insel.mp4` | 0,0–3,0 s (Drohnenflug runter zum Steg) |
| 2 20 Betatester | 3–6 s | `portal-plaza.mp4` | 0,0–3,0 s (Anflug aufs Portal) |
| 3 Checkliste | 6–11 s | `portal-plaza.mp4` | 6,0–12,5 s (Kreisflug, 1,3× schneller) |
| 4 Prefix / Chat | 11–15 s | `see-bruecke.mp4` | 0,0–3,7 s (Haus → Bar, leicht verlangsamt) |
| 5 20 Plätze | 15–19 s | `see-bruecke.mp4` | 4,4–8,4 s (Brücke mit Spieler) |
| 6 Discord | 19–22 s | `portal-plaza.mp4` | 14,2–17,2 s (Zoom ins Portal) |
| End-Card | 22–25 s | `see-bruecke.mp4` | 10,9–12,3 s (weichgezeichnet) |

So tauschst du einen Clip:

1. Neue Datei nach `public/clips/` kopieren (z. B. `public/clips/farmwelt.mp4`).
2. In `src/config.ts` beim gewünschten Segment `clip` anpassen:

   ```ts
   clip: {file: 'farmwelt.mp4', start: 2.5, end: 6.5, zoom: [1, 1.2], focusX: 50, focusY: [50, 60]},
   ```

   - `start` / `end` – Sekunden **im Clip**. Ist `end − start` gleich der
     Segment-Länge, läuft der Clip in Originaltempo; länger = schneller,
     kürzer = Zeitlupe. `end` darf nicht hinter dem Clip-Ende liegen.
   - `zoom` – 1 = Clip-Höhe füllt genau das Bild. `[1, 1.3]` = langsamer Zoom rein.
   - `focusX` / `focusY` – welcher Punkt des Clips (in %, 50 = Mitte) in die
     Bildmitte soll. `focusX: 30` zeigt eher den linken Teil des 16:9-Bildes;
     `focusY` wirkt nur bei `zoom > 1` (größer = Inhalt rutscht nach oben,
     z. B. damit Spieler nicht hinter der Caption verschwinden).
   - Statt einer Zahl geht immer auch `[Start, Ende]` für eine Kamerafahrt.
3. Passende Stellen findest du am schnellsten im Studio (`npm run studio`) oder mit
   `ffmpeg -i public/clips/clip.mp4 -vf fps=2,scale=480:-1,tile=6x3 sheet.jpg`.

Look/Lesbarkeit (Lila-Stich, dunkle Verläufe hinter Logo und Captions) steht in
`FootageGrade` in `src/Footage.tsx`.

## Voiceover hinzufügen

1. Aufnahme als `public/voiceover.mp3` speichern (Start bei 0:00, Sätze an den
   Zeitfenstern aus `SEGMENTS` ausrichten: 0–3 s, 3–6 s, 6–11 s, 11–15 s,
   15–19 s, 19–22 s).
2. In `src/config.ts` `USE_VOICEOVER = true` setzen.
3. `npm run render` – das `<Audio>`-Element in `src/GalacticfyBeta.tsx` ist
   bereits eingebaut und wird dann automatisch eingebunden.

## Dateien

```
src/config.ts          Texte, Timings, Farben, Clip-Zuordnung
src/GalacticfyBeta.tsx Haupt-Komposition (Layout, Segmente, End-Card, Audio)
src/Footage.tsx        Gameplay-Clips (Zuschnitt, Zoom), Farblook, Blitz-Übergänge
src/Visuals.tsx        Animierte Visuals je Segment (auf Glas-Panels)
src/RichText.tsx       Kinetische Captions mit Highlight-Wörtern
src/Background.tsx     Sternenhimmel, Nebel, Pixel-Block-Partikel
src/PixelIcon.tsx      Pixel-Art-Icons (Rakete, Controller, Geschenk, ...)
public/clips/          Gameplay-Aufnahmen (werden mit eingecheckt)
```
