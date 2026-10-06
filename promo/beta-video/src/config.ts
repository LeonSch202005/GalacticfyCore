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

export type Segment = {
  from: number; // Sekunden
  to: number; // Sekunden
  text: string;
  visual: SegmentVisual;
  /** Optionale Schriftgröße der Caption (px) */
  fontSize?: number;
};

export const SEGMENTS: Segment[] = [
  {
    from: 0,
    to: 3,
    text: 'Dieser Minecraft-Server ist noch **nicht fertig** … und genau deshalb brauchen wir **dich**!',
    visual: 'intro',
    fontSize: 78,
  },
  {
    from: 3,
    to: 6,
    text: 'Galacticfy sucht **20 Betatester** – für **Java und Bedrock**!',
    visual: 'testers',
    fontSize: 84,
  },
  {
    from: 6,
    to: 11,
    text: 'Teste unsere Systeme, finde Bugs, check die Menüs und hilf uns, die Wirtschaft perfekt auszubalancieren.',
    visual: 'checklist',
    fontSize: 64,
  },
  {
    from: 11,
    to: 15,
    text: 'Als Dankeschön bekommst du einen **exklusiven Betatester-Prefix** und weitere Belohnungen!',
    visual: 'chat',
    fontSize: 72,
  },
  {
    from: 15,
    to: 19,
    text: 'Aber Achtung: Es gibt nur **20 Plätze** – wer zuerst kommt …',
    visual: 'slots',
    fontSize: 80,
  },
  {
    from: 19,
    to: 22,
    text: 'Komm jetzt auf unseren **Discord** und öffne ein Ticket! Link in der Bio.',
    visual: 'discord',
    fontSize: 80,
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
