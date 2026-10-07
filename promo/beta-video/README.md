# Galacticfy – Betatester-Promo (Remotion)

Vertikales Promo-Video (1080×1920, 30 fps, 25 s) für TikTok / Reels / Shorts.
Caption-getriebenes Video mit deutschem Voiceover (`public/voiceover.mp3` – aktuell TTS,
eigene Aufnahme: siehe [So nimmst du den Voiceover auf](#so-nimmst-du-den-voiceover-auf)).
Im Hintergrund läuft echtes Gameplay vom Server (`public/clips/`), darüber
Galacticfy-Look (Lila-Stich, Sterne/Pixel-Partikel, Neon-Captions, Glas-Panels) und
**echte Screenshots** (`public/screens/`): Logo, Menü „Prefix wählen“, Chat mit
`[Beta Tester]`, Scoreboard, Bossbar-Quest, das Discord-Banner mit `/dc` – und der echte
Bewerbungsweg auf dem Discord (Kanal `#tickets` → „Jetzt bewerben“ → Formular „Betatester“).
Die großen Hingucker sind **deine eigenen Grafiken** (`public/brand/`): das „20 TESTER
GESUCHT“-Hero, das „JAVA & BEDROCK“-Banner, die vier Banner der Checkliste (FEATURES TESTEN,
BUGS FINDEN, MENÜS CHECKEN, WIRTSCHAFTS SYSTEM), die EXKLUSIV-Pill, die Rakete und die
Leuchtleiste – siehe [Eigene Grafiken](#eigene-grafiken-publicbrand).

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
- `BRAND_IMAGES` / `BRAND` – Logo-Sticker („BETA“) und alles zu den **eigenen Grafiken**
  (Dateien, Größen, Positionen, Zeiten, Stichwörter) – auch die Banner-Folge in Segment 3
  (`BRAND.checklist`) und das JAVA-&-BEDROCK-Banner (`BRAND.editions`), siehe
  [Eigene Grafiken](#eigene-grafiken-publicbrand).
- `INTRO`, `PREFIX`, `SLOTS`, `DISCORD` – Inhalte und
  Einsatzzeitpunkte (`chatAt`, `clickAt`, `formAt` … in Sekunden ab Segmentstart,
  passend zum gesprochenen Wort) der animierten Visuals. `DISCORD` enthält außerdem Größe/
  Position des Formulars (`formScale`, `formTop`), die Abdunklung dahinter (`formDim`) und
  wann das Popup wieder zugeht (`closeAt`).
- `SCREENS` / `SCREEN_REGIONS` / `SCREEN_OUTLINES` – die Screenshot-Ausschnitte (siehe unten).
- `END_CARD` (Zeilen mit Icon; die ersten beiden Zeilen – „20 TESTER GESUCHT“ und „JAVA &
  BEDROCK“ – sind deine Grafiken aus `BRAND`) / `END_CARD_STEPS` (hervorgehobene Zeile „Discord /dc →
  #tickets → Jetzt bewerben“) / `END_CARD_LINKS` (galacticfy.de, Discord: /dc) /
  `END_CARD_FOOTER` („(Link in Bio)“) / `END_CARD_SECONDS` – Abschlusskarte.
- `USE_VOICEOVER` / `VOICEOVER_FILE` / `VOICEOVER_VOLUME` – Voiceover (siehe unten).
- `END_CARD_CLIP` / `END_CARD_CLIP_BLUR` / `END_CARD_CLIP_DIM` – weichgezeichnetes
  Gameplay hinter der End-Card (`null` = nur Weltraum-Hintergrund).
- `COLORS`, Schriften (`FONT_HEAVY`, `FONT_PIXEL`).

Die Gesamtlänge ergibt sich automatisch aus dem letzten Segment + End-Card.

Achtung Voiceover: Wer eine Caption ändert, muss auch das Voiceover neu aufnehmen bzw.
erzeugen (siehe [Voiceover](#voiceover)) – oder es mit `USE_VOICEOVER = false` abschalten.
Wer die Zeitfenster (`from`/`to`) ändert, zieht sie auch oben in
`scripts/import-voiceover.sh` (`SEG_FROM`/`SEG_TO`) nach.

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
Die Werte stehen oben in `src/GalacticfyBeta.tsx` (`CAPTION_*`, `VISUAL_*`). Einzige
Ausnahme: Das Discord-Formular in Segment 6 ist ein Popup – es liegt (wie in Discord) mittig
über dem abgedunkelten Bild inkl. Logo, reicht von y ≈ 158 bis ≈ 1085 (x ≈ 257–823) und lässt
nur die Caption frei. Es geht kurz vor der End-Card wieder zu (`DISCORD.closeAt`), damit der
Flug ins lila Portal vor dem Blitz noch zu sehen ist.

## Echte Screenshots (`public/screens/`)

Statt erfundener Mock-ups zeigt das Video Ausschnitte aus echten Ingame- und
Discord-Screenshots (PNG in Originalauflösung, verlustfrei, ohne Snipping-Tool-Popup und
ohne Partikel-Kleckse). Sie werden mit `<Img>` eingebunden (`src/Screen.tsx`) und liegen auf
Glas-Panels mit leichtem Neon-Glow. Minecraft-Screenshots werden beim Hochskalieren mit
`image-rendering: pixelated` gezeichnet (harte Pixel), die Discord-Screenshots sind kein
Pixel-Art und werden weich skaliert (`smooth: true` in `SCREENS`).

| Datei | Inhalt | Wo im Video |
|---|---|---|
| `logo.png` | GALACTICFY-Banner aus dem Scoreboard | Header (ganzes Video) + End-Card |
| `bossbar.png` | Bossbar „Erste Schritte (3/8) » …“ + Fortschrittsbalken | Segment 3 klein unter dem Banner FEATURES TESTEN (+ Glitch, solange BUGS FINDEN steht) |
| `prefix-menu-chest.png` | Menü „Prefix wählen“: Titel + die sechs Truhen-Reihen + unterer GUI-Rahmen (aus `prefix-menu.png` geschnitten, ohne das leere Spieler-Inventar). Der kaum lesbare Original-Titel (Cyan auf Grau) wird im Video von einem dunklen „Prefix wählen“-Schild überdeckt (`PREFIX.menuTitle`) | Segment 3 klein unter dem Banner MENÜS CHECKEN + Segment 4 (klein, als Kontext) |
| `scoreboard-economy.png` | „Konto › 24.644“ / „Nova › 250“ | Segment 3 klein unter dem Banner WIRTSCHAFTS SYSTEM |
| `chat-betatester.png` | Chat „[Beta Tester] Inhaber ✦ Leon185“ | Segment 4 (Prefix) – groß (1,75×), nur „[Beta Tester] Inhaber“ (`SCREEN_REGIONS.chatRankLine`) |
| `scoreboard-online.png` | „Online › 1/20“ | Segment 5 (20 Plätze) – markiert wird nur „/20“ (Server-Maximum), die Slots darunter bleiben „frei“ |
| `discord-banner.png` | „DISCORD /dc \| TEAMSPEAK /ts“ | Segment 6 (Discord) – über dem Discord-Fenster, „/dc“ wird markiert; auf den Banner-Umriss zugeschnitten (`SCREEN_OUTLINES.discordBanner`), damit keine Spielwelt-Ecken stehen bleiben |
| `discord-channel.png` | Discord-Kanal-Kopfzeile „# 🎫 \| tickets“ | Segment 6 – Kopfzeile des Discord-Fensters |
| `discord-apply.png` | Embed-Abschnitt „Betatester werden“ + Text + grüner Button „Jetzt bewerben“ | Segment 6 – im Discord-Fenster, nur Überschrift + Text (`SCREEN_REGIONS.discordApplyText`, 2,08×) |
| `discord-apply-button.png` | nur der Button „Jetzt bewerben“ (Ecken transparent) | Segment 6 – groß (2,6×) als eigene Zeile unter dem Text (am Handy lesbar); Glow, Klick und Blitz |
| `discord-form.png` | Formular „Betatester“ (Minecraft-Name, Alter, Test-Erfahrung, Zeit pro Woche, Motivation, Abbrechen/Absenden; Ecken transparent) | Segment 6 – ploppt nach dem Klick mittig auf (1,18×), „Absenden“ wird kurz markiert, Text-Cursor blinkt; geht bei `DISCORD.closeAt` wieder zu |
| `prefix-menu.png`, `tab-betatester.png`, `chat-full.png`, `scoreboard.png` | ganzes Menü inkl. Spieler-Inventar, TAB-Zeile, Chat mit Broadcast/Postfach/Belohnung, ganzes Scoreboard | Reserve (aktuell nicht im Video) |

Datenschutz: Die Discord-Ausschnitte sind bewusst eng geschnitten – keine Mitgliederliste,
keine fremden Namen/Avatare/Aktivitäten, keine Ticket-Kanäle anderer Nutzer. Neue
Discord-Screenshots bitte genauso zuschneiden.

Screenshot austauschen: neues PNG nach `public/screens/` legen und in `src/config.ts`
bei `SCREENS` Dateiname + `width`/`height` (Pixel) anpassen. Teil-Ausschnitte und
Markierungsrahmen (z. B. „Erste Schritte (3/8)“, der Rahmen um „/20“ oder „/dc“)
stehen als Pixel-Rechtecke in `SCREEN_REGIONS`, Umriss-Masken (Discord-Banner) als Polygon
in `SCREEN_OUTLINES` – beides muss bei einem neuen Bild ggf. nachgezogen werden. Wichtig: im Ausschnitt dürfen keine Windows-Benachrichtigungen o. Ä. sein.

## Eigene Grafiken (`public/brand/`)

Deine Marken-Grafiken liegen als **transparente PNGs** in `public/brand/` (Originalauflösung;
Grafiken mit schwarzem Hintergrund wurden freigestellt, die mit Transparenz nur auf den
sichtbaren Inhalt zugeschnitten). Sie sind kein Pixel-Art und werden immer **weich skaliert**
(nie `pixelated`). Eingebunden über `src/Brand.tsx` (Bild, Glow-Puls, Glanz, Einschlag/„Slam“,
Bild-Wackeln) und `src/Visuals.tsx`; Dateien + Größen stehen in `BRAND_IMAGES`, Größen,
Positionen, Zeiten und Stichwörter im Block `BRAND` in `src/config.ts`. Die Grafiken selbst
werden nie beschnitten oder übermalt.

| Datei | Inhalt | Wo im Video (Zeiten im fertigen Video) |
|---|---|---|
| `hero-20-tester.png` | Planet + „20“ + „TESTER GESUCHT“ | Segment 2 (3–6 s), groß zwischen Logo und Caption (700 px breit): ploppt bei 3,0 s mit Federung + leichter Drehung rein, **Glow-Puls + Glanz auf der „20“ beim Wort „20“** (3,4 s), schwebt danach sanft |
| `java-bedrock.png` | Doppel-Banner „[Monitor] JAVA & BEDROCK [Grasblock]“ | Segment 2 direkt unter dem Hero, über der Caption (800 px sichtbar breit, rechter Rand bei x ≈ 940, Oberkante 976 px): per Wisch von links mit leuchtender Kante aufgedeckt – **beim Wort „Java“** (4,2–4,4 s) das linke JAVA-Panel (die Kante bleibt dort als schmale Lichtlinie stehen), **beim Wort „Bedrock“** (4,6–4,8 s) „&“ + BEDROCK-Panel; Glanz bei 5,0 s. End-Card: zweite Zeile (800 px = Zeilenbreite, statt der alten Text-Zeile „Java & Bedrock“), gleitet bei 22,37 s rein, Glanz bei ≈ 23,1 s |
| `features-testen.png` | Leiste „✦ FEATURES TESTEN 02“ | Segment 3 (6–11 s), Banner 1: knallt **beim Wort „Systeme“** groß rein (Start 6,47 s, Einschlag 6,67 s mit Blitz, Stauchen, Wackeln, Glanz + Glow-Puls auf der „02“), 860 px sichtbar breit, Mitte bei y = 520. Darunter die echte Bossbar „Erste Schritte (3/8)“ |
| `bugs-finden.png` | Leiste „[Käfer] BUGS FINDEN 01“ | Segment 3, Banner 2: **beim Wort „Bugs“** (Start 7,2 s, Einschlag 7,4 s, kurzer RGB-Glitch auf dem Banner, Puls auf der „01“; steht bis 8,0 s), 860 px sichtbar breit; FEATURES TESTEN wird dabei kleiner nach oben weggeschoben. Die Bossbar darunter „verbuggt“ zweimal kurz |
| `menues-checken.png` | Klemmbrett + „MENÜS CHECKEN“ | Segment 3, Banner 3: **beim Wort „Menüs“** (Start 8,0 s, Einschlag 8,2 s), 850 px sichtbar breit; darunter das echte Menü „Prefix wählen“ |
| `wirtschaftssystem.png` | Geldsack + Münzen + „WIRTSCHAFTS SYSTEM“ | Segment 3, Banner 4: **beim Wort „Wirtschaft“** (Start 9,2 s, Einschlag 9,4 s), 850 px sichtbar breit, bleibt bis zum Segmentende; darunter die echten Scoreboard-Zeilen „Konto › 24.644“ / „Nova › 250“ |
| `rocket.png` | Cartoon-Rakete | **Übergänge** 1→2 (2,8–3,2 s) und 6→End-Card (21,8–22,2 s): schießt in 12 Frames diagonal durchs Bild (über der Caption), mit Leuchtspur + Nachzieh-Kopien, das Bild wackelt kurz. In Segment 2 fliegt sie einmal um den Planeten (3,75–5,05 s, hinten kleiner/vorne größer) und dockt rechts oben am Hero an. Auf der End-Card klein links neben dem Logo |
| `exklusiv-pill.png` | Neon-Pill „★ EXKLUSIV“ | Segment 4 (11–15 s): knallt **beim Wort „exklusiven“** (12,2 s) von groß auf normal direkt über die echte Chatzeile „[Beta Tester] Inhaber“ (linksbündig mit der Zeile, über „[Beta Tester]“), Einschlag bei 12,4 s mit Blitz, Wackeln, Druckwelle und Glanz; die Chatzeile schiebt sich bei „Betatester-Prefix“ (12,45 s) darunter |
| `bar-20-tester.png` | Leuchtleiste „🚀 20 TESTER GESUCHT“ | End-Card (22–25 s): erste Zeile (statt der alten Text-Zeile), so breit wie die anderen Zeilen, gleitet bei 22,2 s rein, Glanz bei ≈ 22,9 s |

**Segment 3 (Banner-Folge):** Es steht immer nur **ein** großer Banner in der Banner-Fläche
(zwischen Logo und Caption). Jeder neue knallt von 1,7× auf 1× rein, der vorige schrumpft dabei
nach oben weg (≈ 0,27 s). Unter dem Banner sitzt der passende echte Screenshot (`screenScale` 1);
er blendet aus, während der nächste Banner reinknallt, und der neue kommt erst mit dessen
Einschlag (keine Überblendung zweier Screenshots). Ganz unten über der Caption vier kleine
Kästchen, die sich beim Einschlag des jeweiligen Banners abhaken (Fortschritt 1–4, ohne Text);
das nächste offene Kästchen glimmt schon vorher. Bei 860/850 px Breite ragen die Banner nur mit
ihrer Pixel-Deko bis x ≈ 970 in den rechten Streifen; Texte und Nummern-Boxen bleiben bei x ≤ 940.
Die Nummern-Boxen in deinen Grafiken lauten „02“ (FEATURES TESTEN) und „01“ (BUGS FINDEN) –
gesprochen wird aber erst „Systeme“, dann „Bugs“. Die Grafiken bleiben unverändert; im Video wirkt das wie ein **Countdown 02 → 01** (je ein Glow-Puls auf der
Nummer) vor den beiden großen Bannern. Wer lieber 01 → 02 hätte, exportiert die beiden Leisten mit
getauschten Nummern neu (gleiche Dateinamen) – sonst ist nichts zu ändern.

**Grafik austauschen:**

1. Neues PNG **mit Transparenz** (kein schwarzer Hintergrund) nach `public/brand/` legen –
   am einfachsten unter dem alten Dateinamen.
2. In `src/config.ts` unter `BRAND_IMAGES` `width`/`height` (Pixel) anpassen und `box` = der
   sichtbare Inhalt im PNG (ohne transparenten Rand/weichen Schein; im Zweifel
   `{x: 0, y: 0, w: width, h: height}`). Danach wird ausgerichtet – z. B. ist die Leiste auf
   der End-Card genau so breit wie die Zeilen darunter.
3. Bei einer neuen Rakete `BRAND.rocketHeading` setzen: Flugrichtung im Bild in Grad
   (0 = nach rechts, −90 = nach oben; die jetzige zeigt mit −48° nach rechts oben).
4. Bei einem neuen Hero `BRAND.hero.pulseCenter`/`pulseSize` (wo die „20“ im PNG sitzt) und
   ggf. `BRAND.orbit.dock` (Andockpunkt der Rakete) prüfen.
5. Bei einem neuen JAVA-&-BEDROCK-Banner in `BRAND.editions.reveal` die x-Position (PNG-Pixel)
   prüfen, bis zu der beim Wort „Java“ aufgedeckt wird (jetzt 862 = kurz vor dem „&“), bei
   neuen Checklisten-Leisten in `BRAND.checklist.banners[].pulse` die Mitte der Nummern-Box.

**Größe/Position/Timing ändern** – alles in `BRAND`:

- `hero` – Breite, Oberkante, Einsatz, `pulse` (Stichwort für den Glow-Puls), `float`. Bei
  einer anderen Breite `orbit` (Mittelpunkt, Radien, Andockpunkt) mitskalieren.
- `editions` – das JAVA-&-BEDROCK-Banner in Segment 2: sichtbare Breite, Oberkante, `reveal`
  (je Teil: bis wohin aufgedeckt wird + Stichwort), `wipeFrames`, `shineDelay`.
- `checklist` – die Banner-Folge in Segment 3: je Banner `image` (Name aus `BRAND_IMAGES`),
  sichtbare Breite, `cue` (Stichwort + `offset`), `screen` (echter Screenshot darunter:
  `'quest'`, `'glitch'`, `'menu'`, `'economy'`), `pulse` (Nummern-Box); außerdem `centerY`
  (Mitte der Banner-Fläche), Einschlag (`fromScale`, `slamFrames`, `shake`), `exitFrames`,
  `screenGap`/`screenScale` (Screenshot unter dem Banner) und `progressTop`/`progressSize`
  (Fortschritts-Kästchen, `null` = keine). `bannerShake` = Bild-Wackeln je Einschlag.
- `orbit` – Start, Dauer, Größe, Ellipse (`center`/`radius`/`tilt`) und Andockpunkt.
- `exklusiv` – `cue` (Stichwort), Breite, `anchorX` (0,37 = linksbündig mit der Chatzeile, über
  „[Beta Tester]“; 0,5 = mittig über der Chatzeile), `gap`, Schräglage, Einschlag (`fromScale`, `slamFrames`, `shake`).
- `wipes` – die Raketen-Übergänge: `cut` = an welchem Schnitt (`1` = Intro → Segment 2,
  `2` = Segment 2 → 3, … `'end'` = zur End-Card), `frames` (≤ 12), `from`/`to` (Bild-px),
  `size`; `wipeShake`/`slamShake` = Wackel-Stärke. Einen Übergang löschen = Zeile entfernen,
  einen weiteren Schnitt = Zeile kopieren und `cut` ändern.
- `endCard` – Breite der Leiste, Glanz-Zeitpunkt, `editionsWidth` (JAVA-&-BEDROCK-Banner als
  zweite Zeile; `null` = keins, dann ggf. die Text-Zeile in `END_CARD` wieder einkommentieren),
  `editionsShineAt`, kleine Rakete (`rocket: null` = keine).

**Stichwörter (`cue`):** Effekte wie die EXKLUSIV-Pill, der „20“-Puls, das JAVA-&-BEDROCK-Banner
oder die Banner in Segment 3 warten auf ein Wort der Caption (`{word: 'exklusiven'}`) und kommen
genau dann, wenn dieses Wort in der Caption reinploppt (+ `offset`). Ändern sich Caption oder
Timing, wandern sie automatisch mit.
Achtung: Die Caption zeigt jeden Satz in den ersten ~60 % seines Zeitfensters – wer den Satz
übers ganze Fenster spricht, sagt die Wörter etwas später. In der aktuellen TTS-Spur kommen
„20“, „Java“ und „Bedrock“ (Segment 2) ≈ 0,6–1 s nach ihrem Effekt, „exklusiven“ (Segment 4)
≈ 0,2 s danach. Die vier Banner in Segment 3 haben deshalb schon einen `offset` (0,05 / 0,4 /
0,6 / 0,8 s), damit ihr Einschlag auf dem gesprochenen Wort der TTS-Spur liegt (≈ 6,7 / 7,4 /
8,2 / 9,4 s; gesprochen ≈ 6,8 / 7,5 / 8,1 / 9,4 s) – ohne offset kämen sie alle in den ersten
2,4 s und stünden nur je ~0,4 s. Folge: In der **stummen Fassung** kommen diese vier Banner
≈ 0,3–1 s nach ihrem Wort in der Caption (zum Mitlesen/Teleprompter zählt nur die Caption).
Nach deiner eigenen Aufnahme lässt sich jeder Effekt einzeln nachschieben, ohne die Caption zu
ändern: `{word: 'Java', offset: 0.8}` (Sekunden, positiv = später). Steht das Wort nicht mehr in der Caption,
bricht das Rendern mit einer klaren Meldung ab (dann das Stichwort in `config.ts` anpassen,
oder eine feste Zeit `{at: 1.2}` in Sekunden ab Segmentstart eintragen).

### Segment 4 (11–15 s): Belohnungen

Oben steht zuerst das echte Menü „Prefix wählen“, dann knallen nacheinander deine Grafiken rein
(immer nur eine groß, die vorige wird nach oben weggeschoben):

| Datei (`public/brand/`) | Wann |
|---|---|
| `exklusiver-prefix.png` | beim Wort „exklusiven“ |
| `deine-belohnung.png` | beim Wort „Belohnungen“ |
| `battlepass.png` | 0,55 s nach „Belohnungen“ |
| `kostenlos.png` | Stempel auf dem Battlepass, 0,35 s danach |
| `betatester-chat.png` | Chat-Leiste „BETATESTER Deinname » GG!“ bei „Betatester-Prefix“ |

Darunter klein die echte Chatzeile `[Beta Tester] Inhaber` als Beweis. Alles einstellbar in
`BRAND.rewards` (`src/config.ts`). Die alte EXKLUSIV-Pill (`exklusiv-pill.png`, `BRAND.exklusiv`)
wird nicht mehr benutzt.

## Ton

Aktuell: **keine Stimme** (`USE_VOICEOVER = false`), nur ein **Mausklick** beim Klick auf
„Jetzt bewerben“ im Discord-Teil (`public/sfx/click.wav`, mit ffmpeg erzeugt; Zeitpunkt =
`DISCORD.clickAt`, Lautstärke `DISCORD.clickVolume`, `clickSound: null` schaltet ihn ab).
Den Sprecher-Ton legst du selbst drüber (z. B. in TikTok/CapCut) – oder mit
`scripts/import-voiceover.sh` importieren und `USE_VOICEOVER = true` setzen.

## Voiceover

Eingebunden über `<Audio>` in `src/GalacticfyBeta.tsx`, Datei `public/voiceover.mp3`
(25,0 s, 48 kHz Stereo, −14 LUFS). Jeder Satz liegt im Zeitfenster seines Segments, die
End-Card (22–25 s) ist ohne Sprache.

**Ausschalten:** in `src/config.ts` `USE_VOICEOVER = false` setzen und neu rendern – oder
einfach `npm run render:ohne-ton` (gleiches Video ohne Tonspur).

### So nimmst du den Voiceover auf

Du brauchst nur dein Handy oder ein Headset – und ~10 Minuten.

**Die 6 Sätze** (genau wie die Captions; „Platz“ = so viel Zeit hast du pro Satz):

| # | Fenster | Platz | Text |
|---|---|---|---|
| 1 | 0–3 s | ≈ 2,8 s | Dieser Minecraft-Server ist noch nicht fertig … und genau deshalb brauchen wir dich! |
| 2 | 3–6 s | ≈ 2,8 s | Galacticfy sucht 20 Betatester – für Java und Bedrock! |
| 3 | 6–11 s | ≈ 4,8 s | Teste unsere Systeme, finde Bugs, check die Menüs und hilf uns, die Wirtschaft perfekt auszubalancieren. |
| 4 | 11–15 s | ≈ 3,8 s | Als Dankeschön bekommst du einen exklusiven Betatester-Prefix und weitere Belohnungen! |
| 5 | 15–19 s | ≈ 3,8 s | Aber Achtung: Es gibt nur 20 Plätze – wer zuerst kommt … |
| 6 | 19–22 s | ≈ 2,8 s | Komm jetzt auf unseren Discord und öffne ein Ticket! Link in der Bio. |

Das ist zügiges TikTok-Tempo – vor allem Satz 1 und 3 sind knapp. Lieber mit Energie und
etwas schneller sprechen als zu langsam (lächeln hilft hörbar).

**Variante A – 6 einzelne Dateien (empfohlen):**

1. Pro Satz eine Aufnahme: Aufnahme starten, kurz (~1 s) Pause, Satz sprechen, kurz Pause,
   stoppen. Die Pausen schneidet das Skript automatisch weg – auch ein Klick/Tippen beim
   Starten oder Stoppen der Aufnahme stört dabei nicht.
2. Versprecher? Einfach diesen einen Satz nochmal aufnehmen und die beste Version nehmen.
3. Dateien durchnummerieren, z. B. `satz1.m4a` … `satz6.m4a`.

Tipp: Vorher einmal das stumme Video (`out/galacticfy-betatester-ohne-ton.mp4`) ansehen und
mitlesen – dann hast du das Tempo im Ohr.

**Variante B – ein Take mit dem Video als Teleprompter:**

1. Das stumme Video `out/galacticfy-betatester-ohne-ton.mp4` auf dem PC (oder einem zweiten
   Gerät) öffnen.
2. Aufnahme starten, dann das Video starten und mitlesen – die Captions erscheinen Wort für
   Wort genau dann, wenn du sie sprechen sollst.
3. Beim Import mit `--auto-start` wird alles vor deinem ersten Wort abgeschnitten (ein kurzer
   Klick beim Starten der Aufnahme oder des Videos wird dabei ignoriert).

**Aufnehmen mit …** Handy-Sprachmemo (iPhone „Sprachmemos“, Android „Rekorder“), am PC mit
OBS (nur Mikrofon-Spur, z. B. als `.mkv`/`.mp4`) oder Audacity (Export als WAV). Jedes
gängige Format geht (m4a, mp3, wav, ogg, webm, mp4 …).

- ruhiger Raum, Fenster zu, Lüfter/Benachrichtigungen aus; Zimmer mit Teppich, Vorhängen
  oder Sofa klingt besser als ein kahler Raum (weniger Hall)
- 15–20 cm Abstand zum Mikro, leicht seitlich sprechen (gegen „Plopp“ bei P/B)
- keine Musik im Hintergrund (Musik kannst du später in TikTok drunterlegen)

**Importieren & rendern:**

```bash
# Variante A: 6 Dateien in der Reihenfolge der Sätze
bash scripts/import-voiceover.sh satz1.m4a satz2.m4a satz3.m4a satz4.m4a satz5.m4a satz6.m4a
# Variante B: ein ganzer Take
bash scripts/import-voiceover.sh aufnahme.m4a --auto-start

npm run render            # mit deiner Stimme
npm run render:ohne-ton   # stumme Fassung (unverändert)
```

(Auch als `npm run voiceover:import -- <dateien>`.) Das Skript braucht nur `bash` und `ffmpeg`.
Es macht jede Aufnahme zu Mono, filtert Brummen/Trittschall (Hochpass 80 Hz), mindert Rauschen
sanft, schneidet die Pausen vorne/hinten weg (Variante A; kurze Klicks beim Starten/Stoppen
der Aufnahme zählen nicht als Sprache), komprimiert leicht und bringt alles auf
−14 LUFS / −1,5 dBTP (48 kHz Stereo).

**Windows:** Die Befehle in **Git Bash** ausführen (kommt mit „Git for Windows“; in der
normalen Eingabeaufforderung/PowerShell gibt es kein `bash`, dann scheitert auch
`npm run voiceover:import`). `ffmpeg` installieren mit `winget install Gyan.FFmpeg` und Git Bash
danach neu öffnen. Unter WSL gehen Windows-Pfade als `/mnt/c/Users/…/satz1.m4a`. Oder einfach
die Aufnahmen an Claude schicken (siehe unten).

Danach zeigt es:

- **Variante A:** pro Satz Länge vs. Platz, wie viel Pause vorne/hinten weggeschnitten wurde
  (`weg v/h`) und wo er im Video liegt. Ist ein Satz zu lang,
  kommt eine **WARNUNG** – dann den Satz etwas schneller neu aufnehmen, oder die Captions
  umtimen (das Skript schlägt passende `from`/`to`-Werte für `SEGMENTS` vor; dann auch
  `SEG_FROM`/`SEG_TO` oben im Skript anpassen und es nochmal laufen lassen).
- **Variante B:** die erkannten Sprechpassagen neben den Caption-Fenstern. Liegt alles um
  einen festen Betrag daneben: `--offset SEK` (positiv = Anfang abschneiden, negativ = Stille
  davor).

Beim ersten Import wird die bisherige TTS-Spur als `public/voiceover-tts.mp3` gesichert.
Zurück zur TTS-Stimme: `cp public/voiceover-tts.mp3 public/voiceover.mp3`. Mit
`--out probe.mp3` kannst du erst mal in eine andere Datei importieren und probehören.

**Oder ganz einfach:** Schick die Aufnahmen an Claude – Import, Timing-Check, ggf. Umtimen
der Captions und Rendern werden dann für dich erledigt.

### TTS-Voiceover (aktuelle Spur)

`public/voiceover.mp3` ist derzeit ein deutsches TTS-Voiceover (Stimme „Thorsten“,
Piper/VITS `de_DE-thorsten-high`, CC0). **Neu erzeugen** (z. B. nach Textänderungen):

```bash
npm run voiceover        # = bash scripts/make-voiceover.sh  -> public/voiceover.mp3
```

Achtung: Das überschreibt `public/voiceover.mp3` – nach einer eigenen Aufnahme also nur mit
`bash scripts/make-voiceover.sh --out public/voiceover-tts.mp3` benutzen.

Das Skript legt ein Python-venv (piper-tts) und die Stimme in `~/.cache/galacticfy-voiceover`
an (ca. 500 MB, Laufzeit ~3 min). Texte, Aussprache-Korrekturen (z. B. „Galacticfy“,
„Bedrock“, „Prefix“) und das Timing stehen in `scripts/make-voiceover.py`. Ändert sich eine
Caption in `src/config.ts`, bricht das Skript ab – dann dort `LINES` mit anpassen.

### Timing an eine Aufnahme anpassen (von Hand)

Weicht das Timing ab: `from`/`to` der `SEGMENTS` an die Sätze anpassen (Reihenfolge
beibehalten, End-Card ≥ 2,5 s) – die Clip-Ausschnitte (`clip.start`/`clip.end`) mitziehen,
sonst laufen sie schneller/langsamer. Die Einsatzzeitpunkte (`PREFIX.chatAt`, `SLOTS.alarmAt`, `DISCORD.clickAt`/`formAt`/`closeAt` …) ggf. auf die neuen Wörter
legen – die Effekte der eigenen Grafiken (Stichwörter `cue`, Raketen-Übergänge an den
Schnitten) wandern von selbst mit. Lautstärke bei Bedarf über `VOICEOVER_VOLUME` (0–1).

## Dateien

```
src/config.ts          Texte, Timings, Farben, Clip- und Screenshot-Zuordnung
src/GalacticfyBeta.tsx Haupt-Komposition (Layout, Segmente, End-Card, Audio)
src/Footage.tsx        Gameplay-Clips (Zuschnitt, Zoom), Farblook, Blitz-Übergänge
src/Visuals.tsx        Animierte Visuals je Segment (auf Glas-Panels, mit Screenshots + eigenen Grafiken)
src/Brand.tsx          Eigene Grafiken: Bild, Glow/Glanz, Rakete, Raketen-Übergang, Bild-Wackeln
src/Screen.tsx         Screenshot-Ausschnitt (<Img>, pixelated bzw. smooth) + Markierungsrahmen
src/RichText.tsx       Kinetische Captions mit Highlight-Wörtern
src/Background.tsx     Sternenhimmel, Nebel, Pixel-Block-Partikel
src/PixelIcon.tsx      Pixel-Art-Icons (Rakete, Controller, Geschenk, ...)
public/clips/          Gameplay-Aufnahmen (werden mit eingecheckt)
public/screens/        Ausschnitte aus echten Ingame- und Discord-Screenshots (PNG)
public/brand/          Eigene Grafiken (transparente PNGs: Hero, JAVA & BEDROCK, 4 Checklisten-Banner,
                       EXKLUSIV-Pill, Rakete, Leiste)
public/voiceover.mp3   Voiceover (aktuell TTS; eigene Aufnahme -> import-voiceover.sh)
scripts/               import-voiceover.sh (eigene Aufnahme importieren),
                       make-voiceover.sh / .py (TTS-Generator)
```
