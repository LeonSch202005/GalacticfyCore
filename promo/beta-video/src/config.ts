// ============================================================
//  Galacticfy Betatester-Promo – ALLE Texte & Timings hier
// ============================================================
//  - Zeiten in SEKUNDEN (werden mit FPS in Frames umgerechnet)
//  - **Wort** = hervorgehoben (Neon-Farbe)
//  - Ein Voiceover sollte sich an diese Zeitfenster halten.
// ============================================================

export const FPS = 30;
export const WIDTH = 1080;
export const HEIGHT = 1920;

/** Länge der End-Card in Sekunden (nach dem letzten Segment). */
export const END_CARD_SECONDS = 3;

/**
 * Voiceover (public/voiceover.mp3, erzeugt mit scripts/make-voiceover.sh).
 * false = stummes Video. Eigene Aufnahme: einfach die MP3 ersetzen (Zeitfenster s. SEGMENTS).
 */
export const USE_VOICEOVER = true;
export const VOICEOVER_FILE = 'voiceover.mp3';
/** Lautstärke des Voiceovers (0–1). */
export const VOICEOVER_VOLUME = 1;

/** Rechteck in Pixeln innerhalb eines Bildes (für Teil-Ausschnitte / Markierungen). */
export type Region = {x: number; y: number; w: number; h: number};

/**
 * Ein eigenes Grafik-PNG (public/brand/) mit Originalgröße in Pixeln.
 * box = sichtbarer Inhalt im PNG (ohne transparenten Rand / weichen Glow) – danach wird
 * ausgerichtet, damit z. B. die Leiste auf der End-Card genau so breit ist wie die Zeilen.
 */
export type BrandImage = {file: string; width: number; height: number; box: Region};

/**
 * Stichwort für einen Effekt: startet, wenn dieses Wort der Segment-Caption reinploppt
 * (+ offset Sekunden) – so bleibt alles synchron zur Caption bzw. zum Voiceover.
 * Alternativ eine feste Zeit `at` (Sekunden ab Segmentstart).
 */
export type Cue = {word: string; offset?: number} | {at: number};

// ------------------------------------------------------------
//  Marke + EIGENE GRAFIKEN (liegen in public/brand/)
// ------------------------------------------------------------
//  Transparente PNGs, aus den Original-Grafiken freigestellt. KEIN Pixel-Art -> werden
//  immer weich skaliert. Austauschen: neues PNG (gleicher Dateiname oder neuer Name in
//  BRAND_IMAGES) + width/height/box anpassen. Positionen in Bild-Pixeln (1080×1920),
//  Zeiten in Sekunden.
/**
 * Deine eigenen Grafiken (public/brand/): Datei, Originalgröße (px) und box = sichtbarer
 * Inhalt im PNG (ohne transparenten Rand / weichen Schein). Danach wird ausgerichtet und
 * skaliert ("width" in BRAND = sichtbare Breite bzw. Breite des ganzen PNGs, je nach Stelle).
 */
export const BRAND_IMAGES = {
  /** Planet + "20" + "TESTER GESUCHT" (Segment 2) */
  hero: {file: 'hero-20-tester.png', width: 1123, height: 1156, box: {x: 51, y: 99, w: 1014, h: 1045}},
  /** Neon-Pill "★ EXKLUSIV" (Segment 4) */
  exklusiv: {file: 'exklusiv-pill.png', width: 1900, height: 517, box: {x: 11, y: 10, w: 1877, h: 496}},
  /** Rakete, zeigt nach rechts oben (Übergänge, Orbit in Segment 2, End-Card) */
  rocket: {file: 'rocket.png', width: 1045, height: 1131, box: {x: 10, y: 11, w: 1023, h: 1108}},
  /** Leuchtleiste "🚀 20 TESTER GESUCHT" (End-Card, erste Zeile) */
  bar: {file: 'bar-20-tester.png', width: 1574, height: 304, box: {x: 44, y: 44, w: 1485, h: 215}},
  /** Doppel-Banner "[Monitor] JAVA & BEDROCK [Grasblock]" (Segment 2 unter dem Hero + End-Card) */
  javaBedrock: {file: 'java-bedrock.png', width: 1975, height: 436, box: {x: 21, y: 27, w: 1939, h: 384}},
  /** Segment 3, Banner-Folge: Leiste "✦ FEATURES TESTEN 02" */
  featuresTesten: {file: 'features-testen.png', width: 1937, height: 347, box: {x: 11, y: 12, w: 1915, h: 324}},
  /** Segment 3: Leiste "[Käfer] BUGS FINDEN 01" */
  bugsFinden: {file: 'bugs-finden.png', width: 1981, height: 401, box: {x: 11, y: 12, w: 1959, h: 377}},
  /** Segment 3: Klemmbrett + "MENÜS CHECKEN" */
  menuesChecken: {file: 'menues-checken.png', width: 1900, height: 690, box: {x: 26, y: 16, w: 1853, h: 661}},
  /** Segment 3: Geldsack + Münzen + "WIRTSCHAFTS SYSTEM" */
  wirtschaft: {file: 'wirtschaftssystem.png', width: 1998, height: 672, box: {x: 21, y: 12, w: 1958, h: 628}},
} satisfies Record<string, BrandImage>;
export type BrandImageName = keyof typeof BRAND_IMAGES;

/**
 * Echte Screenshots unter den Bannern in Segment 3 (siehe BRAND.checklist):
 *  - 'quest'   Bossbar "Erste Schritte (3/8)"
 *  - 'glitch'  der vorige Screenshot "verbuggt" kurz (RGB-Versatz, verschobene Streifen)
 *  - 'menu'    Vorschau auf das Menü "Prefix wählen"
 *  - 'economy' Scoreboard "Konto › 24.644" / "Nova › 250"
 */
export type ChecklistScreen = 'quest' | 'glitch' | 'menu' | 'economy';

export const BRAND = {
  /** nur Info – das Logo im Video ist der echte Banner-Screenshot SCREENS.logo */
  name: 'GALACTICFY',
  /** Sticker neben dem Logo */
  tag: 'BETA',

  /** Ordner unter public/ */
  dir: 'brand',
  /** alle PNGs (Dateiname, Größe, sichtbarer Inhalt) – siehe BRAND_IMAGES oben */
  images: BRAND_IMAGES,
  /** Flugrichtung der Rakete im PNG (Grad, 0 = nach rechts, −90 = nach oben) */
  rocketHeading: -48,

  /** Segment 2: das "20 TESTER GESUCHT"-Hero groß zwischen Logo und Caption */
  hero: {
    /** Darstellungsbreite des PNGs (px); sichtbarer Inhalt ≈ 90 % davon. Bei einer anderen
     *  Breite auch BRAND.orbit (Mittelpunkt, Radien, Andockpunkt) nachziehen. */
    width: 700,
    /** Oberkante des sichtbaren Inhalts (Planet) im Bild – Logo endet bei ≈ 300 */
    top: 312,
    /** wann es reinploppt (s ab Segmentstart) */
    enterAt: 0.03,
    /** Glow-Puls + Glanz über der "20" (beim Wort "20" der Caption) */
    pulse: {word: '20'} as Cue,
    /** Mitte + Größe der "20" im PNG (Pixel) – dort sitzt der Glow */
    pulseCenter: [565, 465] as [number, number],
    pulseSize: [820, 560] as [number, number],
    /** Leerlauf: Auf/Ab in px */
    float: 8,
  },
  /**
   * Segment 2: dein Banner "JAVA & BEDROCK" (BRAND_IMAGES.javaBedrock) unter dem Hero, über der
   * Caption. Wird per Wisch von links aufgedeckt: beim Wort "Java" bis vor das "&" (linkes Panel),
   * beim Wort "Bedrock" der Rest ("&" + BEDROCK-Panel), danach läuft ein Glanz drüber.
   */
  editions: {
    /** sichtbare Breite (px) – 800 = rechter Rand bei x ≈ 940 (rechte ~140 px bleiben frei für
     *  die TikTok/Reels-Leiste) */
    width: 800,
    /** Oberkante des sichtbaren Inhalts im Bild (px) – Hero endet bei ≈ 963, Caption-Text beginnt
     *  bei ≈ 1185 */
    top: 976,
    /** Aufdecken: bis zu welcher x-Position im PNG (Pixel, 1975 = ganz), beim jeweiligen Stichwort.
     *  862 = kurz vor dem "&" (dessen Rand beginnt bei ≈ 867) – bis zum nächsten Wisch bleibt
     *  dort die leuchtende Wischkante stehen. */
    reveal: [
      {toX: 862, cue: {word: 'Java'} as Cue},
      {toX: 1975, cue: {word: 'Bedrock'} as Cue},
    ],
    /** Dauer eines Wischs (Frames) */
    wipeFrames: 6,
    /** Glanz läuft nach dem letzten Wisch drüber (Frames danach) */
    shineDelay: 6,
  },
  /**
   * Segment 2: Rakete fliegt einmal um den Planeten (hinten kleiner, vorne größer) und
   * dockt dann rechts oben am Hero an (wie in der Original-Grafik mit Rakete).
   */
  orbit: {
    start: 0.75,
    duration: 1.3,
    /** Raketenbreite (px) */
    size: 138,
    /** Mittelpunkt (Bild-px), Radien (px), Neigung (Grad) – folgt dem Ring der Grafik
     *  (passend zu hero.width 700) */
    center: [540, 527] as [number, number],
    radius: [373, 109] as [number, number],
    tilt: -13,
    /** Andock-Position (Mitte der Rakete, Bild-px) + Breite */
    dock: [818, 367] as [number, number],
    dockSize: 118,
    /** Sekunden für das Andocken nach dem Orbit */
    dockDuration: 0.3,
  },

  /**
   * Segment 3: deine Banner als Folge – jeder knallt beim Stichwort groß in die Banner-Fläche
   * (Einschlag mit Blitz, Glanz und kleinem Wackeln), der vorige wird dabei kleiner nach oben
   * weggeschoben. Es steht immer nur EIN großer Banner da. Darunter klein der passende echte
   * Screenshot (screen, siehe ChecklistScreen) und 4 kleine Fortschritts-Kästchen.
   * Reihenfolge = gesprochene Reihenfolge. Die Nummern-Boxen in deinen Grafiken sind "02"
   * (FEATURES TESTEN) und "01" (BUGS FINDEN) – im Video kommt also erst 02, dann 01. Das wird als
   * Countdown inszeniert (02 → 01 → die beiden großen Banner): pulse = Glow-Puls auf der
   * Nummern-Box beim Einschlag (Mitte + Größe in PNG-Pixeln; weglassen = kein Puls).
   * cue = Wort der Caption; offset (s) schiebt den Einschlag auf das gesprochene Wort der
   * TTS-Spur (die Caption zeigt die Wörter schneller, als sie gesprochen werden). Je Banner
   * ≥ 0,5 s ruhig lesbar: BUGS FINDEN steht von 7,4 s bis 8,0 s (Start von MENÜS CHECKEN).
   * width = sichtbare Breite (px) – 860/850 = rechter Rand bei x ≈ 970, Nummern-Boxen ≤ 940.
   */
  checklist: {
    banners: [
      {
        image: 'featuresTesten',
        width: 860,
        cue: {word: 'Systeme', offset: 0.05},
        screen: 'quest',
        pulse: {center: [1725, 175], size: [300, 260]},
      },
      {
        image: 'bugsFinden',
        width: 860,
        cue: {word: 'Bugs', offset: 0.4},
        screen: 'glitch',
        pulse: {center: [1770, 205], size: [300, 270]},
      },
      {image: 'menuesChecken', width: 850, cue: {word: 'Menüs', offset: 0.6}, screen: 'menu'},
      {image: 'wirtschaft', width: 850, cue: {word: 'Wirtschaft', offset: 0.8}, screen: 'economy'},
    ] as {
      image: BrandImageName;
      width: number;
      cue: Cue;
      screen: ChecklistScreen;
      pulse?: {center: [number, number]; size: [number, number]};
    }[],
    /** Mitte der Banner-Fläche im Bild (px von oben) */
    centerY: 520,
    /** Einschlag: Start-Skalierung, Dauer (Frames), Wackeln des Banners (px) */
    fromScale: 1.7,
    slamFrames: 6,
    shake: 9,
    /** so viele Frames braucht der vorige Banner, um weggeschoben zu werden */
    exitFrames: 8,
    /** echte Screenshots unter dem Banner: Abstand zur Unterkante des Banners, zu dem der
     *  Screenshot gehört (px) + Skalierung (1 = Originalgröße der Karte; größer stößt das Menü
     *  an die Fortschritts-Kästchen) */
    screenGap: 40,
    screenScale: 1,
    /** Fortschritts-Kästchen: Oberkante (px von oben), Größe (px); null = keine */
    progressTop: 1052 as number | null,
    progressSize: 40,
  },

  /** Segment 4: EXKLUSIV-Pill knallt beim Wort "exklusiven" über die Chatzeile [Beta Tester] */
  exklusiv: {
    cue: {word: 'exklusiven'} as Cue,
    /** Darstellungsbreite (px) */
    width: 620,
    /** Mitte der Pill über der Chatzeile: 0 = linker Rand, 0.5 = Mitte. 0.37 = bei 620 px Breite
     *  linksbündig mit der Chatzeile (über "[Beta Tester]") */
    anchorX: 0.37,
    /** Abstand der Pill-Unterkante zur Chatzeile (px). Nicht zu klein, sonst verdeckt die
     *  (schräge, links tiefere) Pill den Rahmen bzw. die Oberkanten von "[Beta Tester]". */
    gap: 18,
    /** Abstand zum Menü darüber (px) */
    gapAbove: 24,
    /** leichte Schräglage (Grad) */
    rotate: -2,
    /** Einschlag: Start-Skalierung (wächst nach oben, nie über die Caption), Dauer (Frames),
     *  Wackeln (px) */
    fromScale: 2.8,
    slamFrames: 6,
    shake: 12,
  },

  /**
   * Raketen-Übergänge: die Rakete schießt in ≤ 12 Frames diagonal durchs Bild (mit
   * Leuchtspur), mittig über einem Schnitt; das Bild wackelt kurz.
   * cut = Schnitt VOR Segment Nr. cut+1 (1 = Intro -> Segment 2) oder 'end' = zur End-Card –
   * die Zeit kommt aus SEGMENTS, wandert also beim Umtimen mit.
   * from/to = Mitte der Rakete (Bild-px) am Anfang/Ende (über der Caption), size = Breite (px).
   */
  wipes: [
    {cut: 1, frames: 12, from: [-360, 1060], to: [1420, -440], size: 400},
    {cut: 'end', frames: 12, from: [-380, 1180], to: [1400, -520], size: 430},
  ] as {cut: number | 'end'; frames: number; from: [number, number]; to: [number, number]; size: number}[],
  /** Wackel-Stärke (px) des ganzen Bildes bei den Raketen-Übergängen / beim EXKLUSIV-Einschlag /
   *  bei jedem Banner-Einschlag in Segment 3 */
  wipeShake: 16,
  slamShake: 7,
  bannerShake: 4,

  /** End-Card: Leiste als erste Zeile (sichtbare Breite = Zeilenbreite), darunter das
   *  "JAVA & BEDROCK"-Banner + kleine Rakete am Logo */
  endCard: {
    barWidth: 800,
    /** sichtbare Breite des JAVA-&-BEDROCK-Banners (zweite Zeile); null = kein Banner (dann ggf.
     *  wieder eine Text-Zeile in END_CARD eintragen) */
    editionsWidth: 800 as number | null,
    /** Glanz über das Banner (Frames ab End-Card-Start) */
    editionsShineAt: 32,
    /** Glanz läuft über die Leiste (Frames ab End-Card-Start) */
    barShineAt: 26,
    rocketSize: 118,
    /** Mitte der kleinen Rakete links neben dem Logo (Bild-px); null = keine Rakete */
    rocket: [126, 392] as [number, number] | null,
    rocketAt: 12,
  },
};

export type SegmentVisual =
  | 'intro'
  | 'testers'
  | 'checklist'
  | 'prefix'
  | 'slots'
  | 'discord';

// ------------------------------------------------------------
//  Echte Screenshots vom Server (liegen in public/screens/)
// ------------------------------------------------------------
/** Ordner unter public/, in dem die Screenshot-Ausschnitte liegen. */
export const SCREEN_DIR = 'screens';

/**
 * Ein Screenshot-Ausschnitt: Datei + Originalgröße in Pixeln.
 * smooth = kein Pixel-Art (z. B. Discord-Screenshots): immer weich skalieren statt "pixelated".
 */
export type ScreenImage = {file: string; width: number; height: number; smooth?: boolean};

/**
 * Ausschnitte aus den Ingame-Screenshots (alle PNG, Originalauflösung, ohne Snipping-Tool-Popup).
 * Wer einen Ausschnitt austauscht, muss width/height (und ggf. die Regionen) anpassen.
 */
export const SCREENS = {
  /** GALACTICFY-Banner (Header + End-Card) */
  logo: {file: 'logo.png', width: 458, height: 150},
  /** Menü "Prefix wählen" (komplett, mit leerem Spieler-Inventar – Reserve) */
  prefixMenu: {file: 'prefix-menu.png', width: 704, height: 884},
  /** Menü "Prefix wählen": nur Titel + die sechs Truhen-Reihen + unterer GUI-Rahmen
   *  (aus prefix-menu.png ausgeschnitten, ohne das leere Spieler-Inventar) */
  prefixMenuChest: {file: 'prefix-menu-chest.png', width: 704, height: 528},
  /** Chatzeile "[Beta Tester] Inhaber ✦ Leon185" */
  chatBetaTester: {file: 'chat-betatester.png', width: 714, height: 36},
  /** TAB-Liste mit "[Beta Tester] Inhaber ✦ Leon185" */
  tabBetaTester: {file: 'tab-betatester.png', width: 792, height: 34},
  /** Chat mit Broadcast / Postfach / Belohnungen */
  chatFull: {file: 'chat-full.png', width: 1192, height: 144},
  /** Bossbar "Erste Schritte (3/8) » Sammle Ressourcen in der Farmwelt" */
  bossbar: {file: 'bossbar.png', width: 1468, height: 68},
  /** komplettes Scoreboard */
  scoreboard: {file: 'scoreboard.png', width: 470, height: 604},
  /** Scoreboard-Zeile "Online › 1/20" */
  scoreboardOnline: {file: 'scoreboard-online.png', width: 360, height: 66},
  /** Scoreboard-Zeilen "Konto › 24.644" + "Nova › 250" */
  scoreboardEconomy: {file: 'scoreboard-economy.png', width: 428, height: 140},
  /** Banner "DISCORD /dc | TEAMSPEAK /ts" */
  discordBanner: {file: 'discord-banner.png', width: 464, height: 150},

  // --- Discord (echte Screenshots vom Galacticfy-Discord, KEIN Pixel-Art -> smooth) ---
  // Nur Embed-Abschnitt / Button / Formular / Kanalname – keine Mitgliederliste, keine
  // fremden Namen oder Ticket-Kanäle. Neue Ausschnitte bitte genauso eng schneiden.
  /** Kanal-Kopfzeile "# 🎫 | tickets" (schwarzer Hintergrund) */
  discordChannel: {file: 'discord-channel.png', width: 119, height: 32, smooth: true},
  /** Embed-Abschnitt "Betatester werden" + Text + grüner Button "Jetzt bewerben"
   *  (im Video nur Überschrift + Text: SCREEN_REGIONS.discordApplyText) */
  discordApply: {file: 'discord-apply.png', width: 589, height: 62, smooth: true},
  /** nur der Button "Jetzt bewerben" (abgerundete Ecken transparent) – im Video groß als
   *  eigene Zeile unter dem Text (Glow, Klick, Blitz) */
  discordApplyButton: {file: 'discord-apply-button.png', width: 142, height: 32, smooth: true},
  /** Formular "Betatester" (Modal, Ecken transparent) */
  discordForm: {file: 'discord-form.png', width: 480, height: 786, smooth: true},
} satisfies Record<string, ScreenImage>;

/** Teil-Ausschnitte / Markierungen innerhalb der Screenshots (Pixel im jeweiligen PNG). */
export const SCREEN_REGIONS = {
  /** Bossbar: nur "Erste Schritte (3/8)" */
  questTitle: {x: 0, y: 0, w: 476, h: 46},
  /** Bossbar: nur der Fortschrittsbalken */
  questBar: {x: 368, y: 45, w: 732, h: 20},
  /** Prefix-Menü (prefix-menu-chest.png): Titelleiste – wird mit einem gut lesbaren
   *  "Prefix wählen"-Schild überdeckt (Original: Cyan auf MC-Grau, kaum lesbar) */
  prefixMenuTitle: {x: 4, y: 4, w: 696, h: 64},
  /** Chatzeile: nur "[Beta Tester] Inhaber" (groß im Prefix-Segment) */
  chatRankLine: {x: 0, y: 0, w: 466, h: 36},
  /** Chatzeile: "[Beta Tester]" (bekommt einen Glanz-Effekt) */
  chatPrefix: {x: 0, y: 0, w: 280, h: 36},
  /** Scoreboard-Zeile: nur "/20" = Server-Maximum (wird markiert; "1" = gerade online) */
  onlineMax: {x: 269, y: 12, w: 76, h: 42},
  /** Discord-Banner: "/dc" (wird markiert) */
  discordCommand: {x: 130, y: 96, w: 52, h: 28},
  /** discord-apply.png: nur Überschrift "Betatester werden" + Beschreibung (ohne den Button
   *  rechts daneben, der kommt groß als eigene Zeile: SCREENS.discordApplyButton) */
  discordApplyText: {x: 6, y: 3, w: 404, h: 56},
  /** discord-form.png: Button "Absenden" (wird kurz markiert) */
  discordFormSubmit: {x: 244, y: 721, w: 211, h: 40},
  /** discord-form.png: Text-Cursor im Feld "Minecraft-Name" (blinkt im Video) */
  discordFormCaret: {x: 37, y: 190, w: 3, h: 23},
} satisfies Record<string, Region>;

/**
 * Umrisse (Polygon in Screenshot-Pixeln), auf die ein Screenshot zugeschnitten wird –
 * z. B. damit um das Discord-Banner (abgeschrägte Ecken + Planeten-Kuppel) keine
 * Spielwelt-Pixel aus dem Original-Screenshot stehen bleiben.
 */
export const SCREEN_OUTLINES = {
  discordBanner: [
    [20, 22], [150, 22], [165, 15], [180, 10], [196, 6], [211, 5], [226, 4], [241, 5],
    [256, 6], [272, 10], [287, 15], [302, 22], [447, 22], [461, 35], [461, 128],
    [443, 146], [19, 146], [3, 130], [3, 31],
  ],
} satisfies Record<string, [number, number][]>;

// ------------------------------------------------------------
//  Gameplay-Clips (liegen in public/clips/)
// ------------------------------------------------------------
/** Ordner unter public/, in dem die Clips liegen. */
export const CLIP_DIR = 'clips';

/** Wert, der während des Segments animiert werden kann: Zahl oder [Start, Ende]. */
export type Animated = number | [number, number];

/**
 * Ein Gameplay-Ausschnitt als Vollbild-Hintergrund (16:9 wird mittig auf 9:16 zugeschnitten).
 *  - start/end: Zeitpunkte IM CLIP (Sekunden). Ist end − start länger/kürzer als das
 *    Segment, wird der Clip automatisch schneller/langsamer abgespielt
 *    (gleich lang = Originaltempo).
 *  - zoom: 1 = Clip-Höhe füllt genau die 1920 px. [1, 1.3] = langsamer Zoom rein.
 *  - focusX/focusY: welcher Punkt des Clips (in % von Breite/Höhe, 50 = Mitte) in die
 *    Bildmitte soll. focusX wählt den sichtbaren 9:16-Streifen, focusY wirkt nur bei zoom > 1.
 */
export type ClipShot = {
  file: string;
  start: number;
  end: number;
  zoom?: Animated;
  focusX?: Animated;
  focusY?: Animated;
};

export type Segment = {
  from: number; // Sekunden
  to: number; // Sekunden
  text: string;
  visual: SegmentVisual;
  /** Optionale Schriftgröße der Caption (px) */
  fontSize?: number;
  /** Gameplay-Clip, der hinter diesem Segment läuft */
  clip: ClipShot;
};

export const SEGMENTS: Segment[] = [
  {
    from: 0,
    to: 3,
    text: 'Dieser Minecraft-Server ist noch **nicht fertig** … und genau deshalb brauchen wir **dich**!',
    visual: 'intro',
    fontSize: 78,
    // Drohnenflug runter auf die Spawn-Insel, Zoom auf die Spieler am Steg
    clip: {file: 'spawn-insel.mp4', start: 0, end: 3, zoom: [1, 1.45], focusX: [50, 53], focusY: [50, 78]},
  },
  {
    from: 3,
    to: 6,
    text: 'Galacticfy sucht **20 Betatester** – für **Java und Bedrock**!',
    visual: 'testers',
    fontSize: 84,
    // Anflug über den Plaza auf das lila Portal
    clip: {file: 'portal-plaza.mp4', start: 0, end: 3, zoom: [1.04, 1.16], focusY: [42, 48]},
  },
  {
    from: 6,
    to: 11,
    text: 'Teste unsere Systeme, finde Bugs, check die Menüs und hilf uns, die Wirtschaft perfekt auszubalancieren.',
    visual: 'checklist',
    fontSize: 64,
    // Kreisflug ums Portal mit Stadt & Marktständen (6,5 s Material in 5 s = leicht schneller)
    clip: {file: 'portal-plaza.mp4', start: 6, end: 12.5, zoom: [1.12, 1], focusY: [44, 50]},
  },
  {
    from: 11,
    to: 15,
    text: 'Als Dankeschön bekommst du einen **exklusiven Betatester-Prefix** und weitere Belohnungen!',
    visual: 'prefix',
    fontSize: 72,
    // Haus mit Grasdach -> rein in die Bar (leicht verlangsamt)
    clip: {file: 'see-bruecke.mp4', start: 0, end: 3.7, zoom: [1.12, 1.02], focusY: [50, 46]},
  },
  {
    from: 15,
    to: 19,
    text: 'Aber Achtung: Es gibt nur **20 Plätze** – wer zuerst kommt …',
    visual: 'slots',
    fontSize: 80,
    // See mit Brücke, Flug auf den laufenden Spieler zu
    clip: {file: 'see-bruecke.mp4', start: 4.4, end: 8.4, zoom: [1.04, 1.2], focusY: [52, 64]},
  },
  {
    from: 19,
    to: 22,
    text: 'Komm jetzt auf unseren **Discord** und öffne ein Ticket! Link in der Bio.',
    visual: 'discord',
    fontSize: 80,
    // Zoom ins lila Portal (Übergang zur End-Card)
    clip: {file: 'portal-plaza.mp4', start: 14.2, end: 17.2, zoom: [1.1, 1.6], focusX: [38, 27], focusY: [50, 46]},
  },
];

/** Segment 1: "Server lädt"-Balken */
export const INTRO = {
  label: 'SERVER LÄDT…',
  /** bis zu welchem Prozentwert der Balken läuft */
  percent: 73,
};

/** Segment 4: echtes Menü "Prefix wählen" + echte Chatzeile mit [Beta Tester]. Zeiten ab Segmentstart. */
export const PREFIX = {
  /** Lesbares Schild über der Titelleiste des echten Menüs (Original-Titel ist kaum lesbar) */
  menuTitle: 'Prefix wählen',
  /** Menü ploppt auf */
  menuAt: 0.1,
  /** Chatzeile "[Beta Tester] Inhaber ✦ Leon185" (bei "Betatester-Prefix") */
  chatAt: 1.45,
  /** Geschenk (bei "weitere Belohnungen") */
  rewardAt: 3.0,
};

/**
 * Segment 5: echte Scoreboard-Zeile "Online › 1/20" (markiert wird nur "/20" = Server-Maximum)
 * + 20 freie Slots. Die Slots bleiben "frei" (leere, leuchtende Rahmen) – sie füllen sich
 * NICHT, damit es nicht nach "schon ausgebucht" aussieht. Zeiten ab Segmentstart.
 */
export const SLOTS = {
  total: 20,
  /** Bis wann alle 20 freien Slots aufgeploppt sind (bei "nur 20 Plätze") */
  filledAt: 1.9,
  /** Ab wann es rot blinkt (bei "wer zuerst kommt") */
  alarmAt: 2.55,
  label: 'FREIE PLÄTZE',
  alarmLabel: 'SCHNELL SEIN!',
};

/**
 * Discord-Segment: der ECHTE Bewerbungsweg. Oben das Ingame-Banner "DISCORD /dc", darunter
 * der Kanal "# tickets" mit dem Embed-Abschnitt "Betatester werden" und (groß, eigene Zeile)
 * dem Button "Jetzt bewerben"; der Cursor klickt ihn, dann ploppt das echte Formular
 * "Betatester" auf (mittig, Hintergrund abgedunkelt wie in Discord) und geht kurz vor der
 * End-Card wieder zu. Alles echte Screenshots (SCREENS.discord*), Zeiten ab Segmentstart.
 */
export const DISCORD = {
  /** Wann "/dc" im Banner markiert wird (bei "Discord") */
  highlightAt: 0.9,
  /** Wann der Cursor "Jetzt bewerben" klickt (bei "öffne ein Ticket") */
  clickAt: 1.3,
  /** Wann das Formular "Betatester" aufploppt */
  formAt: 1.5,
  /** Wann "Absenden" im Formular kurz markiert wird */
  submitHintAt: 2.05,
  /** Wann das Popup wieder zugeht (Formular + Abdunklung + Discord-Fenster blenden aus), damit
   *  vor der End-Card noch kurz der Flug ins lila Portal zu sehen ist */
  closeAt: 2.55,
  /** Größe des Formulars (480×786 px Original): 1,18 = ca. 566×928 px, passt zwischen
   *  obere Safe-Zone und Caption */
  formScale: 1.18,
  /** Oberkante des Formulars im Bild (px von oben) */
  formTop: 158,
  /** Abdunklung hinter dem Formular (0–1), wie bei einem Discord-Popup */
  formDim: 0.72,
};

/**
 * End-Card-Zeilen mit Icon. Die ersten beiden Zeilen sind deine Grafiken (Leiste "20 TESTER
 * GESUCHT" + Banner "JAVA & BEDROCK", siehe BRAND.endCard) und stehen deshalb nicht hier.
 */
export const END_CARD: {icon: 'rocket' | 'gamepad' | 'gift' | 'pointer'; text: string}[] = [
  // "Java & Bedrock" ist jetzt dein Banner (BRAND.endCard.editionsWidth). Ohne Banner wieder:
  // {icon: 'gamepad', text: '**Java** & **Bedrock**'},
  {icon: 'gift', text: 'Prefix **[Beta Tester]** + Belohnungen'},
];
/**
 * Letzte, hervorgehobene End-Card-Zeile: der echte Weg zur Bewerbung, als Schritte mit
 * Pfeilen dazwischen. look: 'text' = normaler Text (**…** = Neon), 'channel' = Discord-Kanal,
 * 'button' = grüner Discord-Button (wie der echte "Jetzt bewerben"-Button).
 */
export const END_CARD_STEPS: {text: string; look: 'text' | 'channel' | 'button'}[] = [
  {text: 'Discord **/dc**', look: 'text'},
  {text: '#tickets', look: 'channel'},
  {text: 'Jetzt bewerben', look: 'button'},
];
/** Kleine Info-Kacheln unter den End-Card-Zeilen (Website + Discord-Befehl). */
export const END_CARD_LINKS = ['**galacticfy.de**', 'Discord: **/dc**'];
export const END_CARD_FOOTER = '(Link in Bio)';
/** Stark weichgezeichneter Clip hinter der End-Card (null = nur Weltraum-Hintergrund). */
export const END_CARD_CLIP: ClipShot | null = {
  file: 'see-bruecke.mp4',
  start: 10.9,
  end: 12.3,
  zoom: [1.1, 1.2],
};
/** Weichzeichner (px) und Abdunklung (0–1) für den End-Card-Clip. */
export const END_CARD_CLIP_BLUR = 14;
export const END_CARD_CLIP_DIM = 0.55;

export const COLORS = {
  bg: '#05030f',
  purple: '#b44dff',
  purpleDeep: '#5b1aa8',
  cyan: '#2ef2ff',
  gold: '#ffc83d',
  white: '#f4f1ff',
  muted: '#a99bd6',
};

export const FONT_HEAVY =
  "'Arial Black', 'Helvetica Neue', 'Liberation Sans', 'DejaVu Sans', Arial, sans-serif";
export const FONT_PIXEL =
  "'Minecraftia', 'Press Start 2P', 'DejaVu Sans Mono', 'Liberation Mono', monospace";

// ---- abgeleitete Werte (nicht ändern) ----
export const MAIN_SECONDS = SEGMENTS[SEGMENTS.length - 1].to;
export const TOTAL_FRAMES = Math.round((MAIN_SECONDS + END_CARD_SECONDS) * FPS);
export const sec = (s: number) => Math.round(s * FPS);
/** Zeitpunkt (s) eines Schnitts: vor Segment Nr. cut+1 bzw. 'end' = Beginn der End-Card. */
export const cutSeconds = (cut: number | 'end') =>
  cut === 'end' ? MAIN_SECONDS : SEGMENTS[Math.max(1, Math.min(cut, SEGMENTS.length - 1))].from;
