# Galacticfy – Betatester-Promo (Remotion)

Vertikales Promo-Video (1080×1920, 30 fps, ~25 s) für TikTok / Reels / Shorts.
Stummes, caption-getriebenes Video – ein Voiceover kann später darüber gelegt werden.

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
npm run stills           # Kontroll-Standbilder (Frames 45/250/400/700) nach out/
```

`remotion.config.ts` nutzt das vorinstallierte Chromium unter
`/opt/pw-browsers/...`, falls vorhanden – sonst lädt Remotion selbst einen
Headless-Browser. Pfad dort bei Bedarf anpassen.

## Texte & Timings ändern

**Alles steht in `src/config.ts`:**

- `SEGMENTS` – die sechs Caption-Abschnitte mit `from`/`to` in Sekunden.
  `**Wort**` wird in Neon-Cyan hervorgehoben. `fontSize` optional pro Segment.
- `CHECKLIST`, `CHAT`, `SLOTS`, `DISCORD` – Inhalte der animierten Visuals.
- `END_CARD` / `END_CARD_FOOTER` / `END_CARD_SECONDS` – Abschlusskarte.
- `COLORS`, Schriften (`FONT_HEAVY`, `FONT_PIXEL`).

Die Gesamtlänge ergibt sich automatisch aus dem letzten Segment + End-Card.

Hinweis: Statt Emojis werden eigene Pixel-Icons (`src/PixelIcon.tsx`) genutzt,
weil der Headless-Browser keine Emoji-Schrift hat.

## Voiceover hinzufügen

1. Aufnahme als `public/voiceover.mp3` speichern (Start bei 0:00, Sätze an den
   Zeitfenstern aus `SEGMENTS` ausrichten: 0–3 s, 3–6 s, 6–11 s, 11–15 s,
   15–19 s, 19–22 s).
2. In `src/config.ts` `USE_VOICEOVER = true` setzen.
3. `npm run render` – das `<Audio>`-Element in `src/GalacticfyBeta.tsx` ist
   bereits eingebaut und wird dann automatisch eingebunden.

## Dateien

```
src/config.ts          Texte, Timings, Farben
src/GalacticfyBeta.tsx Haupt-Komposition (Layout, Segmente, End-Card, Audio)
src/Visuals.tsx        Animierte Visuals je Segment
src/RichText.tsx       Kinetische Captions mit Highlight-Wörtern
src/Background.tsx     Sternenhimmel, Nebel, Pixel-Block-Partikel
src/PixelIcon.tsx      Pixel-Art-Icons (Rakete, Controller, Geschenk, ...)
```
