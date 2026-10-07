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

export const BRAND = {
  /** nur Info – das Logo im Video ist der echte Banner-Screenshot SCREENS.logo */
  name: 'GALACTICFY',
  /** Sticker neben dem Logo */
  tag: 'BETA',
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

/** Rechteck in Pixeln innerhalb eines Screenshots (für Teil-Ausschnitte / Markierungen). */
export type Region = {x: number; y: number; w: number; h: number};
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

/** Segment 2: "20 BETATESTER" + Editionen */
export const TESTERS = {
  count: '20',
  label: 'BETATESTER',
  editions: [
    {label: 'JAVA', color: '#e8762b'},
    {label: 'BEDROCK', color: '#3aa655'},
  ],
};

/**
 * Checkliste in Segment 3. at = Sekunden ab Segmentstart, wann der Haken kommt
 * (passend zum gesprochenen Wort). screen = echter Screenshot, der unter der Liste erscheint:
 *  - 'quest'   Bossbar "Erste Schritte (3/8)"
 *  - 'glitch'  der vorige Screenshot "verbuggt" kurz + BUG-Sticker
 *  - 'menu'    Vorschau auf das Menü "Prefix wählen"
 *  - 'economy' Scoreboard "Konto › 24.644" / "Nova › 250"
 */
export type ChecklistScreen = 'quest' | 'glitch' | 'menu' | 'economy';
export const CHECKLIST: {text: string; at: number; screen: ChecklistScreen}[] = [
  {text: 'Systeme testen', at: 0.35, screen: 'quest'},
  {text: 'Bugs finden', at: 1.25, screen: 'glitch'},
  {text: 'Menüs checken', at: 2.1, screen: 'menu'},
  {text: 'Wirtschaft balancen', at: 3.3, screen: 'economy'},
];
/** Sticker, der bei "Bugs finden" aufploppt. */
export const BUG_STICKER = 'BUG!';

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

/** End-Card */
export const END_CARD: {icon: 'rocket' | 'gamepad' | 'gift' | 'pointer'; text: string}[] = [
  {icon: 'rocket', text: '**20 BETATESTER** GESUCHT'},
  {icon: 'gamepad', text: '**Java** & **Bedrock**'},
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
