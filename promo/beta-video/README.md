# Galacticfy – Betatester-Promo (Remotion)

Vertikales Promo-Video (1080×1920, 30 fps, **37,6 s** = 34,6 s + 3 s End-Card) für TikTok / Reels /
Shorts. Gesprochen von **dir selbst** (Aufnahme Take 2, `public/voiceover.mp3`), **ohne Untertitel**
(`SHOW_CAPTIONS = false`), mit eigenem **Musik-Bett** (duckt unter der Stimme) und
**Sound-Effekten** auf jedem Einschlag. Im Hintergrund läuft echtes Gameplay vom Server
(`public/clips/`), darüber Galacticfy-Look (Lila-Stich, Sterne/Pixel-Partikel, Glas-Panels),
**echte Screenshots** (`public/screens/`: Logo, Menü „Prefix wählen“, Chat mit `[Beta Tester]`,
Scoreboard, Bossbar-Quest, das Discord-Banner mit `/dc`, der echte Bewerbungsweg auf dem Discord)
und vor allem **deine eigenen Grafiken** (`public/brand/`), die genau auf den gesprochenen Wörtern
reinknallen – siehe [Ablauf](#ablauf-sekunde-für-sekunde).

Fertige Videos:

- `out/galacticfy-betatester.mp4` – Stimme + Musik + Sound-Effekte (zum Hochladen)
- `out/galacticfy-betatester-ohne-musik.mp4` – gleiches Bild, nur Stimme + Sound-Effekte
  (z. B. wenn du in TikTok/Instagram einen Trend-Sound drunterlegen willst)

Beide: H.264 CRF 23, AAC 192 kbit/s, ≈ −14 LUFS, True Peak ≤ −1 dBTP.

## Setup

```bash
cd promo/beta-video
npm install
```

## Vorschau / Bearbeiten

```bash
npx remotion studio      # oder: npm run studio
```

Öffnet die Remotion-Studio-Vorschau im Browser (Timeline, Frame-genaues Scrubbing). Dort gibt es
zwei Kompositionen: `GalacticfyBeta` (mit Musik) und `GalacticfyBeta-OhneMusik`.

## Rendern

```bash
npm run render           # = bash scripts/render.sh -> beide Videos (mit + ohne Musik), Ton gemastert
npm run render:nur-musik # nur out/galacticfy-betatester.mp4 (geht schneller)
npm run render:ohne-ton  # out/galacticfy-betatester-ohne-ton.mp4 (gleiches Bild, ganz ohne Ton)
npm run stills           # Kontroll-Standbilder (alle Abschnitte + End-Card) nach out/
```

`scripts/render.sh` braucht `bash`, `npx` und `ffmpeg` (Windows: in **Git Bash** ausführen,
`ffmpeg` mit `winget install Gyan.FFmpeg`). Warum ein Skript statt nur `remotion render`?
Deine Stimme ist schon auf −14 LUFS / −1,9 dBTP gemastert – die Einschläge landen absichtlich
genau auf den betonten Silben, also genau auf den lautesten Stellen der Stimme. Ohne Mastering
würde die Summe dort kurz über 0 dBFS gehen (Clipping). Das Skript

1. lässt Remotion mit 3 dB Reserve rendern (`masterGain` 0,708, verlustfreier PCM-Ton),
2. holt die 3 dB zurück und fängt die paar Spitzen mit einem 4×-überabgetasteten Limiter ab
   (−1,5 dBFS, 1,5 ms Vorschau, Laufzeit kompensiert -> lippensynchron),
3. kopiert das Bild (kein zweites Encodieren) und schreibt den Ton als AAC 192 kbit/s,
4. rendert für die Fassung ohne Musik nur den Ton neu und legt ihn unter dasselbe Bild,
5. zeigt am Ende Lautheit + True Peak beider Dateien an.

Die Stimme selbst wird dabei nicht leiser – der Limiter greift nur in den wenigen Millisekunden,
in denen Stimme + Effekt zusammen zu laut wären (max. ≈ 1,5 dB).

Kontroll-Standbilder in `out/` werden nicht eingecheckt (`.gitignore`), nur die beiden MP4s.

`remotion.config.ts` nutzt das vorinstallierte Chromium unter
`/opt/pw-browsers/...`, falls vorhanden – sonst lädt Remotion selbst einen
Headless-Browser. Pfad dort bei Bedarf anpassen.

## Ablauf (Sekunde für Sekunde)

Zeiten im fertigen Video; „Wort“ = wann du es in der Aufnahme sagst. Banner brauchen 6 Frames
(0,2 s) für ihren Einschlag – sie starten deshalb ≈ 0,17 s vor dem Wort, damit der **Einschlag auf
der betonten Silbe** landet. Die Zeiten in der Tabelle sind die Einschläge.

| Zeit | Wort (Aufnahme) | Bild | Ton |
|---|---|---|---|
| 0,00 | – | **Hook:** großes Panel „SERVER LÄDT…“ **mitten im Bild** (Riesen-Prozentzahl, dicker Balken), schon in Frame 0 da – mit **Start-Glitch** („ERR“-Flackern, RGB-Streifen übers ganze Bild), Zoom-Punch und Wackeln; Drohne über der (aufgehellten) Spawn-Insel, Balken rast hoch | Start-Glitch, Musik blendet in 0,5 s ein |
| 2,03 | „nicht“ 2,05 | Balken bleibt bei 73 % hängen: Glitch auf Panel + ganzem Bild, Bild wackelt | Glitch |
| 2,37 | „fertig“ 2,37 | zweiter Glitch + **Zoom-Punch**, letzter Block blinkt danach rot | Impact (klein) |
| 3,37 | „und genau…“ 3,39 | Schnitt (auf dem Musik-Beat): **Sturzflug** über den See auf die Brücke; das Panel rückt nach oben | – |
| 4,13 | „deshalb“ 4,13 | Panel kippt in den Fehler-Zustand: rot, **„TESTER FEHLEN!“** | Impact (klein) |
| 5,38 | „dich“ 5,38 | Sticker **„GESUCHT: DU!“** knallt aufs Panel + Zoom-Punch – die Drohne landet auf dem Spieler „[Beta Tester] Inhaber“ | Impact (klein) |
| 5,73 | „Galacticfy“ 5,92 | **Raketen-Übergang** -> Hero „20 TESTER GESUCHT“ ploppt rein, **Musik-Drop** | Whoosh + Impact (+ Crash der Musik) |
| 7,10 | „zwanzig“ 7,09 | Glow-Puls + Druckwelle auf der „20“ | Impact (klein) |
| 7,33 | „Betatester“ 7,68 | Rakete fliegt um den Planeten, dockt an | – |
| 8,80 | „Java“ 8,85 | JAVA-Panel wird aufgewischt | Impact (klein) |
| 9,40 | „Bedrock“ 9,50 | „& BEDROCK“ wird aufgewischt, Glanz (steht bis zum Schnitt ≥ 0,5 s komplett da) | Impact |
| 10,10 | „Teste“ 10,32 | Schnitt (Blitz) – **FEATURES TESTEN** knallt direkt auf „Teste“ rein (10,33) + Bossbar „Erste Schritte (3/8)“, Haken 1 | Whoosh, Impact + Pop |
| 12,43 | „Bugs“ 12,39 | **BUGS FINDEN** + RGB-Glitch, Bossbar „verbuggt“ zweimal, Haken 2 | Impact (klein) + Glitch (+ 2. Glitch 0,5 s später) + Pop |
| 13,57 | „Menüs“ 13,54 | **MENÜS CHECKEN** + echtes Menü „Prefix wählen“, Haken 3 | Impact + Pop |
| 14,57 | „hilf“ 14,55 | Banner + Menü pulsieren/glänzen kurz (kein Stillstand bis zum nächsten Banner) | – |
| 15,67 | „Wirtschaft“ 15,65 | **WIRTSCHAFTS SYSTEM** + Konto/Nova, Haken 4 | Impact + Pop |
| 16,27 | „perfekt“ 16,29 | Glanz + Puls auf dem Banner | – |
| 16,93 | „auszubalancieren“ 16,96 | Banner wippt wie eine Waage | – |
| 18,63 | „Als Dankeschön“ 18,82 | **Raketen-Übergang** (diesmal von rechts unten) | Whoosh |
| 19,10 | „Dankeschön“ 19,08 | **DEINE BELOHNUNG** (Geschenk) knallt rein, **Funken-Explosion** aus dem Geschenk | Impact |
| 20,07 | „bekommst du“ 20,06 | Geschenk pulsiert + glänzt, zweite Funken-Explosion | – |
| 20,70 | „ex-**klu**-siven“ 20,68 | **EXKLUSIVER PREFIX** knallt rein (Geschenk wird nach oben weggeschoben) | Impact |
| 21,80 | „Betatester-Prefix“ 21,89 | Chat-Leiste „BETATESTER Deinname » GG!“ + echte Chatzeile „[Beta Tester] Inhaber“ | 2× Pop |
| 24,20 | „Be-**loh**-nungen“ 24,17 | **BATTLEPASS** schiebt die Chat-Zeilen raus, knallt in die Mitte | Impact |
| 24,67 | – | **KOSTENLOS!**-Stempel (bleibt mit BATTLEPASS + EXKLUSIVER PREFIX bis 25,4 s stehen) | Ding + Impact (klein) |
| 25,40 | „Achtung“ 25,48 | **roter Alarm-Schnitt** + Zoom-Punch, 20 Slots ploppen auf, Zähler zählt hoch („PLÄTZE“) | Impact |
| 26,77 | „zwanzig Plätze“ 26,75 | Zähler springt auf **20**, Leuchtring | Pop |
| 27,60 | „wer zuerst kommt“ 27,65 | Alarm: rot blinken, „SCHNELL SEIN!“, Zähler wird zu **„NOCH FREI“** | Impact |
| 27,90–28,60 | „kommt mahlt zuerst“ | drei Plätze werden vergeben (Spielerkopf, rot): **20 -> 19 -> 18 -> 17 NOCH FREI** | – |
| 29,53 | „Komm jetzt“ 29,74 | Schnitt: Ingame-Banner „/dc“ + Discord-Kanal „#tickets“, Cursor fliegt rein | Whoosh |
| 30,97 | „Discord“ 30,95 | **DISCORD BEITRETEN** (unten), danach „/dc“ markiert | Impact + Pop |
| 31,80 | „öffne“ 31,82 | Cursor **klickt „Jetzt bewerben“** -> echtes Formular „Betatester“ ploppt auf, Kanal + „/dc“-Banner gehen dabei weg (sauberer, leicht abgedunkelter Hintergrund) | Mausklick (31,83, erster eingedrückter Frame) + Pop |
| 32,43 | „Ticket“ 32,42 | **#TICKETS ÖFFNEN** (unten) | Impact |
| 32,83 | – | „Absenden“ wird markiert | Pop |
| 33,23 | „Link in der Bio“ 33,20 | **JETZT BEWERBEN KLICKEN** (deine Grafik) | Impact |
| 33,80 | „Bio“ 33,81 | Banner pulsiert + glänzt, darunter **„↑ (Link in Bio) ↑“**, Formular geht zu -> Flug ins lila Portal | Riser (leise) startet 33,43 |
| 34,57 | – | **Raketen-Übergang** -> End-Card (Logo, 20 TESTER GESUCHT, JAVA & BEDROCK, Prefix + Belohnungen, DISCORD discord.gg/…, „(Link in Bio)“) | Whoosh + Impact (+ Crash der Musik), Musik lauter |
| 35,9–37,57 | – | End-Card: DISCORD-Leiste pulsiert + glänzt alle 0,8 s, Pfeile hüpfen nach oben | Musik blendet in der letzten 1 s aus |

## Texte & Timings ändern

**Alles steht in `src/config.ts`:**

- `CUE` – **alle Effekt-Zeitpunkte** (Sekunden ab Segmentstart), je mit Wort und Zeit aus
  deiner Aufnahme im Kommentar. Die Grafiken (`BRAND`, `INTRO`, `SLOTS`, `DISCORD` …) **und** die
  Sounds (`SFX`) lesen beide von hier – wer einen Wert ändert, verschiebt Bild + Ton zusammen.
- `SEGMENTS` – die sechs Abschnitte mit `from`/`to` in Sekunden (= Sätze der Aufnahme),
  `clip` (Gameplay dahinter), `cuts` (weitere Schnitte im Segment), `flash` (Blitzfarbe beim
  Schnitt). `text` = der Satz (nur für die abgeschalteten Untertitel / als Notiz).
- `BRAND_IMAGES` / `BRAND` – deine Grafiken (Dateien, Größen, Positionen), siehe
  [Eigene Grafiken](#eigene-grafiken-publicbrand).
- `INTRO` (Ladebalken-Hook), `PUNCHES` (Zoom-Punches), `PREFIX`, `SLOTS`, `DISCORD` – Inhalte der
  animierten Visuals (Zeiten kommen aus `CUE`).
- `USE_MUSIC` / `MUSIC` / `VOICE_SPEECH` – Musik-Bett + Ducking, `USE_SFX` / `SFX` –
  Sound-Effekte, siehe [Ton](#ton-stimme-musik-sound-effekte).
- `SCREENS` / `SCREEN_REGIONS` / `SCREEN_OUTLINES` – die Screenshot-Ausschnitte (siehe unten).
- `END_CARD` / `END_CARD_FOOTER` („(Link in Bio)“) / `END_CARD_SECONDS` / `END_CARD_CLIP` –
  Abschlusskarte.
- `SHOW_CAPTIONS` – Untertitel (bewusst `false`), `COLORS`, Schriften.

Die Gesamtlänge ergibt sich automatisch aus dem letzten Segment + End-Card und muss zur
Tonspur passen (34,566667 + 3 s = 1127 Frames).

Hinweis: Statt Emojis werden eigene Pixel-Icons (`src/PixelIcon.tsx`) genutzt, weil der
Headless-Browser keine Emoji-Schrift hat.

## Ton: Stimme, Musik, Sound-Effekte

### Stimme (`public/voiceover.mp3`)

Deine **zweite Aufnahme** (ein ganzer Take, Rohdatei unverändert in
`public/voiceover-original/leon-aufnahme-2.mp3`), geschnitten und gemastert mit

```bash
bash scripts/make-voice-take2.sh        # -> public/voiceover.mp3 (37,57 s, 48 kHz, −14 LUFS, −1,9 dBTP)
```

Das Skript kürzt die langen Pausen zwischen den Sätzen (nur in Stille, 15-ms-Crossfades, Atmer in
den Pausen fallen mit raus), repariert das abgeschnittene „dich“, schaltet einen Mundklick zwischen
„exklusiven“ und „Betatester-Prefix“ stumm (`CLICKS`), filtert Brummen, mindert Rauschen sanft,
entschärft S-Laute, komprimiert leicht und bringt alles auf −14 LUFS. Die
Wort-Zeiten im Kommentar von `CUE` stammen aus dieser Datei. `USE_VOICEOVER = false` = ohne
Stimme, `VOICEOVER_VOLUME` = Lautstärke.

### Musik (`public/music/bed.mp3`)

Selbst erzeugtes, lizenzfreies EDM-Bett (C-Moll, ≈ 127 BPM, keine Samples), mit dem **Drop genau
auf dem Schnitt zu „Galacticfy sucht…“** (5,733 s) und dem **Crash genau auf der End-Card**
(34,567 s); dazwischen läuft es auf dem Beat-Raster, 2,4 s vor der End-Card kommt ein Breakdown
mit eigenem Riser. Neu erzeugen (≈ 25 s), z. B. nach dem Umtimen:

```bash
python3 -I scripts/make-music.py --drop <SEGMENTS[1].from> --end <letztes SEGMENTS.to>
python3 -I scripts/make-music.py --drop 5.733333 --end 34.566667   # aktueller Stand
```

Lautstärke (`MUSIC` in `src/config.ts`): 0,36 in Sprechpausen, **0,2 während du sprichst**
(≈ 15 dB unter der Stimme – auch am Handy-Lautsprecher noch hörbar; Ducking mit 7-Frame-Rampen; wo
gesprochen wird, steht in `VOICE_SPEECH` – mit ffmpeg `silencedetect` aus der Aufnahme gemessen),
0,75 auf der End-Card (damit der Schluss nicht deutlich leiser ist als der Rest), 0,5 s Einblenden am
Anfang, 1 s Ausblenden am Ende.

**Musik ausschalten:**

- in `src/config.ts` `USE_MUSIC = false` setzen und neu rendern, **oder**
- einfach die zweite Datei nehmen: `out/galacticfy-betatester-ohne-musik.mp4` (wird bei
  `npm run render` immer mit erzeugt; im Studio: Komposition `GalacticfyBeta-OhneMusik`).

Lauter/leiser: `MUSIC.volume` / `MUSIC.ducked` / `MUSIC.endCard` (0–1).

### Sound-Effekte (`public/sfx/`)

Ebenfalls selbst synthetisiert (`python3 -I scripts/make-sfx.py`, ohne Samples):

| Datei | Klang | Wo |
|---|---|---|
| `whoosh.wav` | Rauschen mit Sweep, links -> rechts (Peak nach 0,26 s) | Raketen-Übergänge + Schnitte 2->3, 5->6 (Peak genau auf dem Schnitt) |
| `impact.wav` | Sub-Thump + Crack | große Banner-Einschläge (u. a. DEINE BELOHNUNG, BATTLEPASS), Hero, „Achtung“-Schnitt, Alarm, End-Card |
| `impact-small.wav` | leichter Thump | „fertig“, TESTER FEHLEN!, Sticker „GESUCHT: DU!“, Puls auf der „20“, JAVA, BUGS FINDEN, KOSTENLOS |
| `glitch.wav` | digitales Stottern | Start-Glitch (Frame 0), „nicht“, BUGS FINDEN, Bossbar-Glitch |
| `pop.wav` | UI-Pop | Häkchen, Chat-Zeilen, Zähler 20, „/dc“, Formular, „Absenden“ |
| `ding.wav` | Münz-/Glocken-Ding | KOSTENLOS! |
| `riser.wav` | Build-up (endet bei 1,13 s) | leise (0,22) in die End-Card – die Musik hat dort schon einen eigenen Riser |
| `click.wav` | Mausklick | Klick auf „Jetzt bewerben“ (`DISCORD.clickSound`, `clickVolume`) |

Alle Einsätze stehen als Liste `SFX` in `src/config.ts`: `{label, file, seg, at, volume}` –
`seg` = Segment (`'intro'`, `'testers'`, … oder `'endCard'`), `at` = Sekunden ab dessen Start
(negativ = vor dem Schnitt). Einschläge stehen als `CUE.… + SLAM` drin (= genau der
Einschlag-Frame des Banners) – wer ein `CUE` verschiebt, verschiebt den Sound mit. Lautstärken
0,28–0,65, damit die Stimme immer vorne bleibt. Einen Sound entfernen = Zeile löschen; alle aus:
`USE_SFX = false`.

## Gameplay-Clips austauschen

Die Aufnahmen liegen in `public/clips/` (16:9, 1920×1080, 30 fps, H.264, ohne Ton). Sie werden
automatisch mittig auf 9:16 zugeschnitten. Jeder Moment der Clips kommt **nur einmal** vor.
Aktuelle Belegung (Tempo = Clip-Sekunden / Video-Sekunden; < 0,8× nur bei Drohnen-/Kamerafahrten):

| Abschnitt | Zeit | Clip | Ausschnitt im Clip | Tempo |
|---|---|---|---|---|
| 1 Intro | 0–3,37 s | `spawn-insel.mp4` | 0,0–3,15 s (Drohnenflug runter zum Steg mit Spielern, aufgehellt) | 0,94× |
| 1 Intro ab „und genau deshalb“ | 3,37–5,73 s | `see-bruecke-dive.mp4` | 0,03–1,67 s = `see-bruecke.mp4` 12,37 -> 10,70 s **rückwärts**: Sturzflug über den See auf die Brücke, landet bei „dich“ auf dem Spieler „[Beta Tester] Inhaber“ | 0,69× |
| 2 20 Betatester | 5,73–10,1 s | `portal-plaza.mp4` | 0,0–4,4 s (Anflug aufs Portal) | 1,01× |
| 3 Checkliste | 10,1–18,63 s | `portal-plaza.mp4` | 4,4–13,0 s (raus aus dem Portal, Kreisflug) | 1,01× |
| 4 Belohnungen | 18,63–25,4 s | `see-bruecke.mp4` | 0,0–5,3 s (Haus -> Bar -> raus zum See, aufgehellt) | 0,78× |
| 5 20 Plätze | 25,4–29,53 s | `see-bruecke.mp4` | 5,3–8,55 s (Drohne über die Brücke zum Spieler) | 0,79× |
| 6 Discord | 29,53–34,57 s | `portal-plaza.mp4` | 13,0–17,2 s (Zoom ins lila Portal) | 0,83× |
| End-Card | 34,57–37,57 s | `see-bruecke.mp4` | 8,6–10,45 s (Drohne übers Wasser, weichgezeichnet) | 0,62× |

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
     `focusY` wirkt nur bei `zoom > 1`.
   - Statt einer Zahl geht immer auch `[Start, Ende]` für eine Kamerafahrt.
   - Ein weiterer Schnitt innerhalb eines Segments: `cuts: [{at: 2.0, clip: {…}}]` (`at` =
     Sekunden ab Segmentstart; harter Schnitt mit Zoom-Punch + kleinem Blitz).
3. Passende Stellen findest du am schnellsten im Studio (`npm run studio`) oder mit
   `ffmpeg -i public/clips/clip.mp4 -vf fps=2,scale=480:-1,tile=6x3 sheet.jpg`.

`see-bruecke-dive.mp4` ist kein neues Material, sondern ein rückwärts laufender Ausschnitt aus
`see-bruecke.mp4` (im Original zieht die Drohne vom Spieler weg). Neu erzeugen:
`ffmpeg -ss 10.7 -i public/clips/see-bruecke.mp4 -t 1.7 -an -vf "reverse,setpts=PTS-STARTPTS" -r 30 -c:v libx264 -preset slow -crf 18 -g 30 -pix_fmt yuv420p -movflags +faststart public/clips/see-bruecke-dive.mp4`.

Dunkle Aufnahmen lassen sich je Clip aufhellen: `brightness: 1.2` im `clip`-Eintrag.
Look/Lesbarkeit (Lila-Stich, dunkle Verläufe hinter Logo und unten) steht in
`FootageGrade` in `src/Footage.tsx` – ohne Untertitel wird die untere Bildhälfte nur leicht
abgedunkelt (kräftiger erst ganz unten, wo TikTok/Reels die Beschreibung zeigt).

Layout: Logo oben ab y = 140, die Visuals darunter (y ≈ 312–1085; das Lade-Panel im Hook steht
anfangs mittig bei y ≈ 730), im Discord-Teil die großen Banner unten (Mitte y = 1300, wo früher die
Untertitel standen) und darunter ab „Bio“ „(Link in Bio)“ (y ≈ 1462). Die untersten ~380 px und die
rechten ~140 px bleiben frei für die TikTok/Reels-Oberfläche (Beschreibung, Like-Leiste). Einzige
Ausnahme: Das Discord-Formular in Segment 6 ist ein Popup – es liegt (wie in Discord) mittig über
dem abgedunkelten Bild inkl. Logo, reicht von y ≈ 158 bis ≈ 1085 und geht bei „Bio“ wieder zu,
damit der Flug ins lila Portal vor der End-Card noch zu sehen ist.

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
| `prefix-menu-chest.png` | Menü „Prefix wählen“: Titel + die sechs Truhen-Reihen + unterer GUI-Rahmen (aus `prefix-menu.png` geschnitten, ohne das leere Spieler-Inventar). Der kaum lesbare Original-Titel (Cyan auf Grau) wird im Video von einem dunklen „Prefix wählen“-Schild überdeckt (`PREFIX.menuTitle`) | Segment 3 klein unter dem Banner MENÜS CHECKEN (in Segment 4 nicht mehr – wäre eine Wiederholung; wieder einschalten: `BRAND.rewards.menu = {scale: 0.62, centerY: 520}`) |
| `scoreboard-economy.png` | „Konto › 24.644“ / „Nova › 250“ | Segment 3 klein unter dem Banner WIRTSCHAFTS SYSTEM |
| `chat-betatester.png` | Chat „[Beta Tester] Inhaber ✦ Leon185“ | Segment 4 (Belohnungen) – unter der Chat-Leiste (1,4×), nur „[Beta Tester] Inhaber“ (`SCREEN_REGIONS.chatRankLine`) |
| `scoreboard-online.png` | „Online › 1/20“ | Reserve: aus (`SLOTS.showOnline = false`), weil „1/20“ neben dem Platz-Zähler wie „nur noch 19 frei“ wirkte |
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

Deine Marken-Grafiken liegen als **transparente PNGs** in `public/brand/` (Originalauflösung).
Sie sind kein Pixel-Art und werden immer **weich skaliert** (nie `pixelated`), nie beschnitten oder
übermalt. Eingebunden über `src/Brand.tsx` (Bild, Glow-Puls, Glanz, Einschlag/„Slam“,
Bild-Wackeln) und `src/Visuals.tsx`; Dateien + Größen stehen in `BRAND_IMAGES`, Größen,
Positionen und Zeiten im Block `BRAND` in `src/config.ts` (die Zeiten selbst kommen aus `CUE`).

| Datei | Inhalt | Wo im Video (Zeiten im fertigen Video) |
|---|---|---|
| `rocket.png` | Cartoon-Rakete | **Übergänge** Intro -> 2 (5,73 s), Checkliste -> Belohnungen (18,63 s, von rechts unten) und -> End-Card (34,57 s): schießt in 12 Frames diagonal durchs Bild, mit Leuchtspur, das Bild wackelt. In Segment 2 fliegt sie um den Planeten (7,33–8,63 s) und dockt am Hero an. Auf der End-Card klein neben dem Logo |
| `hero-20-tester.png` | Planet + „20“ + „TESTER GESUCHT“ | Segment 2: ploppt auf dem Musik-Drop rein (5,73 s), **Glow-Puls + Druckwelle auf der „20“ genau auf „zwanzig“** (7,10 s), schwebt danach |
| `java-bedrock.png` | „JAVA & BEDROCK“ | Segment 2 unter dem Hero: JAVA-Panel per Wisch **auf „Java“** (8,80 s), „& BEDROCK“ **auf „Bedrock“** (9,43 s), dann Glanz. End-Card: zweite Zeile |
| `features-testen.png` | „✦ FEATURES TESTEN 02“ | Segment 3, Einschlag **auf „Teste“** (10,33 s, direkt nach dem Schnitt), darunter die Bossbar |
| `bugs-finden.png` | „BUGS FINDEN 01“ | Einschlag **auf „Bugs“** (12,43 s) mit RGB-Glitch |
| `menues-checken.png` | „MENÜS CHECKEN“ | Einschlag **auf „Menüs“** (13,57 s), darunter das echte Menü; kleiner Puls + Glanz auf „hilf“ (14,57 s) |
| `wirtschaftssystem.png` | „WIRTSCHAFTS SYSTEM“ | Einschlag **auf „Wirtschaft“** (15,67 s), Glanz auf „perfekt“ (16,27 s), wippt wie eine Waage auf „auszubalancieren“ (16,93 s) |
| `exklusiver-prefix.png` | „EXKLUSIVER PREFIX“ | Segment 4, Einschlag **auf „ex-KLU-siven“** (20,70 s) |
| `betatester-chat.png` | Chat-Leiste „BETATESTER Deinname » GG!“ | **auf „Betatester-Prefix“** (21,80 s), darunter die echte Chatzeile |
| `deine-belohnung.png` | „DEINE BELOHNUNG“ | Segment 4, Einschlag **auf „Dankeschön“** (19,10 s) mit Funken-Explosion aus dem Geschenk, pulsiert nochmal auf „bekommst du“ (20,07 s), wird von EXKLUSIVER PREFIX weggeschoben |
| `battlepass.png` | „BATTLEPASS · PREMIUM“ | Einschlag **auf „Be-LOH-nungen“** (24,20 s) in der Mitte (schiebt die Chat-Zeilen raus), bleibt bis zum Schnitt (1,2 s) |
| `kostenlos.png` | Stempel „KOSTENLOS!“ | Einschlag 24,67 s auf dem Battlepass (+ Ding), ≈ 0,7 s lesbar |
| `discord-beitreten.png` | „DISCORD BEITRETEN »“ | Segment 6 unten, Einschlag **auf „Discord“** (30,97 s) |
| `tickets-oeffnen.png` | „#TICKETS ÖFFNEN »“ | Einschlag **auf „Ticket“** (32,43 s) |
| `jetzt-bewerben-klicken.png` | Klemmbrett + „JETZT BEWERBEN KLICKEN »“ | Einschlag **auf „Link in der Bio“** (33,23 s), pulsiert + glänzt nochmal auf „Bio“ (33,80 s) – darunter erscheint dann „↑ (Link in Bio) ↑“ –, bleibt bis zur End-Card (ersetzt die alte Leiste `jetzt-bewerben.png`) |
| `bar-20-tester.png` | Leiste „🚀 20 TESTER GESUCHT“ | End-Card, erste Zeile |
| `discord-link.png` | „DISCORD discord.gg/ZGAxYfG5yX“ | End-Card, über „(Link in Bio)“; pulsiert + glänzt alle 0,8 s |
| `jetzt-bewerben-discord.png` | großes Discord-Logo + „JETZT BEWERBEN“ | **Reserve** (in `BRAND_IMAGES` eingetragen): auf der End-Card wäre es zu voll (sie würde in die TikTok-Leiste unten rutschen), und im Discord-Teil stünde „JETZT BEWERBEN“ sonst doppelt. Austauschen: in `BRAND.cta.banners` `'jetztBewerbenKlicken'` durch `'jetztBewerbenDiscord'` ersetzen (Breite ≈ 700) |
| `jetzt-bewerben.png`, `exklusiv-pill.png` | ältere Leisten | Reserve (nicht im Video) |

**Segment 3 (Banner-Folge):** Es steht immer nur **ein** großer Banner in der Banner-Fläche. Jeder
neue knallt von 1,7× auf 1× rein, der vorige schrumpft nach oben weg. Darunter der passende echte
Screenshot und vier kleine Kästchen, die sich beim jeweiligen Einschlag abhaken. Jeder Banner
steht ≥ 0,9 s ruhig da. Die Nummern-Boxen in deinen Grafiken lauten „02“ (FEATURES TESTEN) und
„01“ (BUGS FINDEN) – im Video wirkt das wie ein Countdown 02 -> 01.

**Segment 4 (Belohnungen):** erst DEINE BELOHNUNG (das „Dankeschön“, mit Funken aus dem Geschenk),
dann EXKLUSIVER PREFIX oben; in der Mitte die Chat-Leiste + echte Chatzeile, die bei BATTLEPASS
(„weitere Belohnungen“) rausfliegen; KOSTENLOS! stempelt auf den Battlepass. Das echte Menü
„Prefix wählen“ steht hier nicht mehr (kommt schon in Segment 3 vor). Damit am Ende alles ≥ 0,7 s
lesbar steht, liegt der Schnitt zu Segment 5 nicht vor „Aber“, sondern genau vor „**Achtung**“
(25,40 s) – der rote Alarm-Schnitt knallt dadurch direkt auf das Wort.

**Segment 5 (20 Plätze):** Der Zähler landet genau auf „zwanzig Plätze“ bei 20; ab „wer zuerst
kommt“ heißt er „NOCH FREI“ und zählt auf 17 runter, drei Slots zeigen einen Spielerkopf (vergeben).
Zeiten: `CUE.slots.taken`, welche Slots: `SLOTS.takenSlots`.

**Hook (Segment 1):** Texte und Zeiten in `INTRO` (`label`, `errorLabel` „TESTER FEHLEN!“,
`sticker` „GESUCHT: DU!“, `centerY`, `dock`) bzw. `CUE.intro`.

**Grafik austauschen:**

1. Neues PNG **mit Transparenz** nach `public/brand/` legen – am einfachsten unter dem alten
   Dateinamen.
2. In `src/config.ts` unter `BRAND_IMAGES` `width`/`height` (Pixel) anpassen und `box` = der
   sichtbare Inhalt im PNG (im Zweifel `{x: 0, y: 0, w: width, h: height}`).
3. Bei einer neuen Rakete `BRAND.rocketHeading` setzen (Flugrichtung im Bild in Grad).
4. Bei einem neuen Hero `BRAND.hero.pulseCenter`/`pulseSize` und ggf. `BRAND.orbit.dock` prüfen,
   bei einem neuen JAVA-&-BEDROCK-Banner `BRAND.editions.reveal[0].toX`, bei neuen
   Checklisten-Leisten `BRAND.checklist.banners[].pulse`.

**Größe/Position ändern** – alles in `BRAND`: `hero`, `editions`, `orbit`, `checklist`
(`centerY`, `fromScale`, `slamFrames`, `exitFrames`, `screenGap`, `progressTop`, `bumpAt`,
`balance`), `rewards` (`banners`, `battlepass`, `kostenlos`, `chatBar`, `realChat`, `menu`),
`cta` (Banner unten im Discord-Teil, `bumpAt`), `wipes` (Raketen-Übergänge: `cut` = an welchem
Schnitt, `from`/`to`, `size`), `wipeShake`/`slamShake`/`bannerShake`, `endCard`.

**Zeiten (`cue`):** stehen bei allen Effekten als feste Zeit `{at: CUE.…}` (Sekunden ab
Segmentstart) und kommen aus dem Block `CUE`. Alternativ geht weiterhin ein Wort der
(abgeschalteten) Caption: `{word: 'exklusiven', offset: 0.2}`.

## Neue Aufnahme?

1. Rohdatei nach `public/voiceover-original/` legen und wie Take 2 aufbereiten: entweder
   `scripts/make-voice-take2.sh` anpassen (Schnittliste `PIECES` oben im Skript) oder mit
   `bash scripts/import-voiceover.sh aufnahme.m4a --auto-start` importieren (Mono, Hochpass,
   Rauschminderung, Kompressor, −14 LUFS; `SEG_FROM`/`SEG_TO` oben im Skript = Spiegel von
   `SEGMENTS`).
2. Wort-Zeiten messen (z. B. im Studio scrubben oder mit Audacity) und eintragen:
   `SEGMENTS[].from/to` (Schnitt ≈ 0,2 s vor dem ersten Wort eines Satzes), alle Werte in `CUE`
   (Einschläge ≈ 0,17 s vor dem Wort), `VOICE_SPEECH` neu messen
   (`ffmpeg -i public/voiceover.mp3 -af silencedetect=noise=-38dB:d=0.18 -f null -`). Das letzte
   `to` + 3 s muss der Länge der MP3 entsprechen.
3. Musik neu erzeugen (`python3 -I scripts/make-music.py --drop … --end …`, siehe oben), dann
   `npm run render`.

Oder einfach die Aufnahme an Claude schicken – Schnitt, Timing, Effekte und Rendern werden dann
für dich erledigt.

Tipps zum Aufnehmen: ruhiger Raum (Teppich/Vorhänge), 15–20 cm Abstand zum Mikro, leicht
seitlich sprechen (gegen „Plopp“), keine Musik im Hintergrund, mit Energie und lieber etwas
schneller sprechen. Jedes gängige Format geht (m4a, mp3, wav, ogg, …).

### TTS-Voiceover (Reserve)

`public/voiceover-tts.mp3` ist die alte deutsche TTS-Spur (Stimme „Thorsten“, Piper,
CC0; passt nicht mehr zum aktuellen Timing). Neu erzeugen mit
`bash scripts/make-voiceover.sh --out public/voiceover-tts.mp3` (ohne `--out` würde es deine
Aufnahme in `public/voiceover.mp3` überschreiben!).

## Dateien

```
src/config.ts          Timings (CUE, SEGMENTS), Grafiken, Musik, Sound-Effekte, Farben, Clips, Screenshots
src/GalacticfyBeta.tsx Haupt-Komposition (Layout, Segmente, End-Card, Wackeln/Punches, Ton-Mix)
src/Root.tsx           Kompositionen GalacticfyBeta (mit Musik) + GalacticfyBeta-OhneMusik
src/Footage.tsx        Gameplay-Clips (Zuschnitt, Zoom, Punch), Farblook, Blitz-Übergänge
src/Visuals.tsx        Animierte Visuals je Segment (Ladebalken-Hook, Banner-Folgen, Discord-Weg)
src/Brand.tsx          Eigene Grafiken: Bild, Glow/Glanz, Rakete, Raketen-Übergang, Bild-Wackeln
src/Screen.tsx         Screenshot-Ausschnitt (<Img>, pixelated bzw. smooth) + Markierungsrahmen
src/RichText.tsx       (abgeschaltete) Kinetic Captions + Cue-Berechnung
src/Background.tsx     Sternenhimmel, Nebel, Pixel-Block-Partikel
src/PixelIcon.tsx      Pixel-Art-Icons (Rakete, Controller, Geschenk, ...)
public/clips/          Gameplay-Aufnahmen
public/screens/        Ausschnitte aus echten Ingame- und Discord-Screenshots (PNG)
public/brand/          Eigene Grafiken (transparente PNGs)
public/voiceover.mp3   deine Aufnahme (Take 2, gemastert); Rohdateien in public/voiceover-original/
public/music/bed.mp3   Musik-Bett (scripts/make-music.py)
public/sfx/            Sound-Effekte (scripts/make-sfx.py; click.wav = Mausklick)
scripts/render.sh      Rendern + Ton-Mastering (beide Fassungen)
scripts/make-voice-take2.sh  Take 2 schneiden + mastern
scripts/make-music.py / make-sfx.py   Musik / Sound-Effekte erzeugen
scripts/import-voiceover.sh  andere Aufnahmen importieren
scripts/make-voiceover.sh / .py      TTS-Generator (Reserve)
```
