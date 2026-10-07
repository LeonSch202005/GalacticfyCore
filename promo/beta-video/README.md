# Galacticfy – Betatester-Promo (Remotion)

Vertikales Promo-Video (1080×1920, 30 fps, 25 s) für TikTok / Reels / Shorts.
Caption-getriebenes Video mit deutschem Voiceover (`public/voiceover.mp3`).
Im Hintergrund läuft echtes Gameplay vom Server (`public/clips/`), darüber
Galacticfy-Look (Lila-Stich, Sterne/Pixel-Partikel, Neon-Captions, Glas-Panels) und
**echte Ingame-Screenshots** (`public/screens/`): Logo, Menü „Prefix wählen“, Chat mit
`[Beta Tester]`, Scoreboard, Bossbar-Quest und das Discord-Banner mit `/dc`.

Fertige Videos:

- `out/galacticfy-betatester.mp4` – mit Voiceover (AAC-Ton)
- `out/galacticfy-betatester-ohne-ton.mp4` – gleiches Video ohne Tonspur

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
npm run render           # -> out/galacticfy-betatester.mp4 (H.264, CRF 23, mit Voiceover)
npm run render:ohne-ton  # -> out/galacticfy-betatester-ohne-ton.mp4 (gleiches Video, --muted)
npm run stills           # Kontroll-Standbilder (ein paar Frames je Segment + End-Card) nach out/
```

Kontroll-Standbilder in `out/` werden nicht eingecheckt (`.gitignore`), nur die beiden MP4s.

`remotion.config.ts` nutzt das vorinstallierte Chromium unter
`/opt/pw-browsers/...`, falls vorhanden – sonst lädt Remotion selbst einen
Headless-Browser. Pfad dort bei Bedarf anpassen.

## Texte & Timings ändern

**Alles steht in `src/config.ts`:**

- `SEGMENTS` – die sechs Caption-Abschnitte mit `from`/`to` in Sekunden.
  `**Wort**` wird in Neon-Cyan hervorgehoben. `fontSize` optional pro Segment.
- `clip` je Segment – welcher Gameplay-Clip dahinter läuft (siehe unten).
- `INTRO`, `TESTERS`, `CHECKLIST`, `PREFIX`, `SLOTS`, `DISCORD` – Inhalte und
  Einsatzzeitpunkte (`at`, `chatAt`, `clickAt` … in Sekunden ab Segmentstart, passend
  zum gesprochenen Wort) der animierten Visuals.
- `SCREENS` / `SCREEN_REGIONS` / `SCREEN_OUTLINES` – die Screenshot-Ausschnitte (siehe unten).
- `END_CARD` / `END_CARD_LINKS` (galacticfy.de, Discord: /dc) / `END_CARD_FOOTER` /
  `END_CARD_SECONDS` – Abschlusskarte.
- `USE_VOICEOVER` / `VOICEOVER_FILE` / `VOICEOVER_VOLUME` – Voiceover (siehe unten).
- `END_CARD_CLIP` / `END_CARD_CLIP_BLUR` / `END_CARD_CLIP_DIM` – weichgezeichnetes
  Gameplay hinter der End-Card (`null` = nur Weltraum-Hintergrund).
- `COLORS`, Schriften (`FONT_HEAVY`, `FONT_PIXEL`).

Die Gesamtlänge ergibt sich automatisch aus dem letzten Segment + End-Card.

Achtung Voiceover: Wer eine Caption ändert, muss auch das Voiceover neu erzeugen
(`LINES` in `scripts/make-voiceover.py` anpassen, dann `npm run voiceover`) – oder es mit
`USE_VOICEOVER = false` abschalten.

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

Layout: Captions sitzen unten-mittig (y ≈ 1110–1540), die untersten ~380 px und die
rechten ~140 px bleiben frei für die TikTok/Reels-Oberfläche (Beschreibung, Like-Leiste).
Die Werte stehen oben in `src/GalacticfyBeta.tsx` (`CAPTION_*`, `VISUAL_*`).

## Echte Screenshots (`public/screens/`)

Statt erfundener Mock-ups zeigt das Video Ausschnitte aus echten Ingame-Screenshots
(PNG in Originalauflösung, verlustfrei, ohne Snipping-Tool-Popup und ohne Partikel-Kleckse).
Sie werden mit `<Img>` eingebunden (`src/Screen.tsx`), beim Hochskalieren mit
`image-rendering: pixelated`, und liegen auf Glas-Panels mit leichtem Neon-Glow.

| Datei | Inhalt | Wo im Video |
|---|---|---|
| `logo.png` | GALACTICFY-Banner aus dem Scoreboard | Header (ganzes Video) + End-Card |
| `bossbar.png` | Bossbar „Erste Schritte (3/8) » …“ + Fortschrittsbalken | Checkliste „Systeme testen“ (+ Glitch bei „Bugs finden“) |
| `prefix-menu-chest.png` | Menü „Prefix wählen“: Titel + die sechs Truhen-Reihen + unterer GUI-Rahmen (aus `prefix-menu.png` geschnitten, ohne das leere Spieler-Inventar). Der kaum lesbare Original-Titel (Cyan auf Grau) wird im Video von einem dunklen „Prefix wählen“-Schild überdeckt (`PREFIX.menuTitle`) | Checkliste „Menüs checken“ + Segment 4 (klein, als Kontext) |
| `scoreboard-economy.png` | „Konto › 24.644“ / „Nova › 250“ | Checkliste „Wirtschaft balancen“ |
| `chat-betatester.png` | Chat „[Beta Tester] Inhaber ✦ Leon185“ | Segment 4 (Prefix) – groß (1,75×), nur „[Beta Tester] Inhaber“ (`SCREEN_REGIONS.chatRankLine`) |
| `scoreboard-online.png` | „Online › 1/20“ | Segment 5 (20 Plätze) – markiert wird nur „/20“ (Server-Maximum), die Slots darunter bleiben „frei“ |
| `discord-banner.png` | „DISCORD /dc \| TEAMSPEAK /ts“ | Segment 6 (Discord) – auf den Banner-Umriss zugeschnitten (`SCREEN_OUTLINES.discordBanner`), damit keine Spielwelt-Ecken stehen bleiben |
| `prefix-menu.png`, `tab-betatester.png`, `chat-full.png`, `scoreboard.png` | ganzes Menü inkl. Spieler-Inventar, TAB-Zeile, Chat mit Broadcast/Postfach/Belohnung, ganzes Scoreboard | Reserve (aktuell nicht im Video) |

Screenshot austauschen: neues PNG nach `public/screens/` legen und in `src/config.ts`
bei `SCREENS` Dateiname + `width`/`height` (Pixel) anpassen. Teil-Ausschnitte und
Markierungsrahmen (z. B. „Erste Schritte (3/8)“, der Rahmen um „/20“ oder „/dc“)
stehen als Pixel-Rechtecke in `SCREEN_REGIONS`, Umriss-Masken (Discord-Banner) als Polygon
in `SCREEN_OUTLINES` – beides muss bei einem neuen Bild ggf. nachgezogen werden. Wichtig: im Ausschnitt dürfen keine Windows-Benachrichtigungen o. Ä. sein.

## Voiceover

`public/voiceover.mp3` ist ein deutsches TTS-Voiceover (Stimme „Thorsten“, Piper/VITS
`de_DE-thorsten-high`, CC0), 25,0 s, 48 kHz Stereo, ca. −14 LUFS. Jeder Satz liegt im
Zeitfenster seines Segments (0–3 s, 3–6 s, 6–11 s, 11–15 s, 15–19 s, 19–22 s), die
End-Card (22–25 s) ist ohne Sprache. Eingebunden über `<Audio>` in `src/GalacticfyBeta.tsx`.

**Neu erzeugen** (z. B. nach Textänderungen):

```bash
npm run voiceover        # = bash scripts/make-voiceover.sh  -> public/voiceover.mp3
```

Das Skript legt ein Python-venv (piper-tts) und die Stimme in `~/.cache/galacticfy-voiceover`
an (ca. 500 MB, Laufzeit ~3 min). Texte, Aussprache-Korrekturen (z. B. „Galacticfy“,
„Bedrock“, „Prefix“) und das Timing stehen in `scripts/make-voiceover.py`. Ändert sich eine
Caption in `src/config.ts`, bricht das Skript ab – dann dort `LINES` mit anpassen.

**Ausschalten:** in `src/config.ts` `USE_VOICEOVER = false` setzen und neu rendern – oder
einfach `npm run render:ohne-ton` (gleiches Video ohne Tonspur).

**Eigene Aufnahme statt TTS:**

1. Aufnahme als `public/voiceover.mp3` speichern (Start bei 0:00, 25 s lang). Jeder Satz
   sollte in seinem Zeitfenster aus `SEGMENTS` liegen (siehe oben).
2. Weicht das Timing ab: `from`/`to` der `SEGMENTS` an die Sätze anpassen (Reihenfolge
   beibehalten, End-Card ≥ 2,5 s) – die Clip-Ausschnitte (`clip.start`/`clip.end`) mitziehen,
   sonst laufen sie schneller/langsamer. Die Einsatzzeitpunkte (`CHECKLIST[].at`,
   `PREFIX.chatAt`, `SLOTS.alarmAt`, `DISCORD.clickAt` …) ggf. auf die neuen Wörter legen.
3. Lautstärke bei Bedarf über `VOICEOVER_VOLUME` (0–1).
4. `npm run render` (und `npm run render:ohne-ton`).

## Dateien

```
src/config.ts          Texte, Timings, Farben, Clip- und Screenshot-Zuordnung
src/GalacticfyBeta.tsx Haupt-Komposition (Layout, Segmente, End-Card, Audio)
src/Footage.tsx        Gameplay-Clips (Zuschnitt, Zoom), Farblook, Blitz-Übergänge
src/Visuals.tsx        Animierte Visuals je Segment (auf Glas-Panels, mit Screenshots)
src/Screen.tsx         Screenshot-Ausschnitt (<Img>, pixelated) + Markierungsrahmen
src/RichText.tsx       Kinetische Captions mit Highlight-Wörtern
src/Background.tsx     Sternenhimmel, Nebel, Pixel-Block-Partikel
src/PixelIcon.tsx      Pixel-Art-Icons (Rakete, Controller, Geschenk, ...)
public/clips/          Gameplay-Aufnahmen (werden mit eingecheckt)
public/screens/        Ausschnitte aus echten Ingame-Screenshots (PNG)
public/voiceover.mp3   Voiceover (TTS)
scripts/               Voiceover-Generator (make-voiceover.sh / .py)
```
