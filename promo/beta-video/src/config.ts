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

/** Voiceover: Datei nach public/voiceover.mp3 legen und auf true setzen. */
export const USE_VOICEOVER = false;
export const VOICEOVER_FILE = 'voiceover.mp3';

export const BRAND = {
  name: 'GALACTICFY',
  tag: 'BETA',
};

export type SegmentVisual =
  | 'intro'
  | 'testers'
  | 'checklist'
  | 'chat'
  | 'slots'
  | 'discord';

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
    visual: 'chat',
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

/** Checkliste in Segment 3 */
export const CHECKLIST = [
  'Systeme testen',
  'Bugs finden',
  'Menüs checken',
  'Wirtschaft balancen',
];

/** Minecraft-Chat-Mockup in Segment 4 */
export const CHAT = {
  prefix: 'BETATESTER',
  player: 'Steve',
  message: 'Danke für die Belohnungen! <3',
  systemLine: 'Du hast den Rang BETATESTER erhalten!',
};

/** Slot-Zähler in Segment 5 */
export const SLOTS = {
  total: 20,
  /** Auf wie viele freie Plätze der Zähler herunterzählt */
  countDownTo: 3,
  label: 'FREIE PLÄTZE',
};

/** Discord-Segment */
export const DISCORD = {
  button: 'Ticket öffnen',
  hint: 'Link in der Bio',
};

/** End-Card */
export const END_CARD: {icon: 'rocket' | 'gamepad' | 'gift' | 'pointer'; text: string}[] = [
  {icon: 'rocket', text: '**20 BETATESTER** GESUCHT'},
  {icon: 'gamepad', text: '**Java** & **Bedrock**'},
  {icon: 'gift', text: 'Exklusiver **Prefix** + Belohnungen'},
  {icon: 'pointer', text: 'Jetzt bewerben – **Discord**'},
];
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
