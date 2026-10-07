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
 * Voiceover: deine eigene Aufnahme, Take 2 (public/voiceover.mp3, geschnitten + gemastert mit
 * scripts/make-voice-take2.sh; Rohaufnahme in public/voiceover-original/leon-aufnahme-2.mp3).
 * false = ohne Stimme.
 */
export const USE_VOICEOVER = true;
/** Untertitel (Kinetic Captions) anzeigen? Bewusst AUS (keine Untertitel). Die Effekt-Zeiten
 *  hängen NICHT davon ab. */
export const SHOW_CAPTIONS = false;
export const VOICEOVER_FILE = 'voiceover.mp3';
/** Lautstärke des Voiceovers (0–1). */
export const VOICEOVER_VOLUME = 1;

/**
 * Musik-Bett unter allem (public/music/bed.mp3, selbst erzeugt mit scripts/make-music.py –
 * Drop genau auf SEGMENTS[1].from, Crash genau auf der End-Card). false = keine Musik.
 * Die Komposition "GalacticfyBeta-OhneMusik" (npm run render:ohne-musik) schaltet sie
 * unabhängig davon immer ab.
 */
export const USE_MUSIC = true;
export const MUSIC = {
  file: 'music/bed.mp3',
  /** Lautstärke (0–1), wenn gerade niemand spricht */
  volume: 0.36,
  /** Lautstärke, während die Stimme spricht ("Ducking") – ≈ 15 dB unter der Stimme */
  ducked: 0.2,
  /** Lautstärke auf der End-Card (dort spricht niemand mehr) */
  endCard: 0.75,
  /** Rampe beim Ab-/Aufblenden fürs Ducking (Frames) */
  rampFrames: 7,
  /** Einblenden am Anfang / Ausblenden am Ende (Sekunden) */
  fadeIn: 0.5,
  fadeOut: 1,
};
/**
 * Wo die Stimme spricht (Sekunden im Video) – daran duckt die Musik. Gemessen mit
 * ffmpeg silencedetect (−38 dB, Pausen ≥ 0,18 s) auf public/voiceover.mp3. Bei einer neuen
 * Aufnahme neu messen:
 *   ffmpeg -i public/voiceover.mp3 -af silencedetect=noise=-38dB:d=0.18 -f null -
 */
export const VOICE_SPEECH: [number, number][] = [
  [0.12, 5.6],
  [5.87, 9.9],
  [10.32, 18.42],
  [18.81, 24.72],
  [25.17, 29.25],
  [29.74, 34.19],
];

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

/** Dauer eines "Einschlags" (Banner knallt von groß auf 1) in Frames bzw. Sekunden. */
export const SLAM_FRAMES = 6;
export const SLAM = SLAM_FRAMES / FPS;

/**
 * ALLE Effekt-Zeitpunkte – Sekunden ab Start des jeweiligen Segments (SEGMENTS[].from),
 * gemessen an der Wortliste deiner Aufnahme (Take 2). In Klammern: das Wort und wann es im
 * Video gesprochen wird. Banner, die "einschlagen", starten SLAM (0,2 s) früher, damit der
 * Einschlag genau AUF der betonten Silbe landet (Start ≈ Wort − 0,17 s). Die Sounds (SFX unten)
 * und die Grafiken (BRAND) lesen beide von hier – wer einen Wert ändert, verschiebt Bild + Ton.
 */
export const CUE = {
  intro: {
    /** Frame 0: Start-Glitch ("ERR"-Flackern) + Zoom-Punch + Wackeln – Hook, bevor das erste Wort kommt */
    boot: 0,
    /** Glitch auf dem Ladebalken ("nicht" 2,05 s) */
    nicht: 2.05,
    /** Zoom-Punch + zweiter Glitch ("fertig" 2,37 s) */
    fertig: 2.37,
    /** Schnitt: Sturzflug auf den Spieler auf der Brücke ("und genau…" 3,39 s; liegt auf dem
     *  Musik-Beat); das Lade-Panel rückt dabei nach oben */
    cut: 3.367,
    /** Lade-Panel kippt in den Fehler-Zustand "TESTER FEHLEN!" ("deshalb" 4,13 s) */
    deshalb: 4.133,
    /** Sticker "GESUCHT: DU!" knallt aufs Panel + Zoom-Punch ("dich" 5,38 s) */
    dich: 5.38,
  },
  testers: {
    /** Glow-Puls auf der "20" (Spitze 0,1 s später auf "zwanzig" 7,09 s) */
    pulse20: 1.267,
    /** Rakete fliegt um den Planeten ("Betatester" 7,68 s) */
    orbit: 1.6,
    /** Wisch JAVA ("Java" 8,85 s) */
    java: 3.067,
    /** Wisch & BEDROCK ("Bedrock" 9,50 s; 1 Frame früher, damit JAVA & BEDROCK vor dem Schnitt
     *  ≥ 0,5 s komplett steht) */
    bedrock: 3.667,
  },
  checklist: {
    /** FEATURES TESTEN (Einschlag auf "Teste" 10,32 s – direkt nach dem Schnitt, kein leerer Einstieg) */
    teste: 0.033,
    /** BUGS FINDEN (Einschlag auf "Bugs" 12,39 s) */
    bugs: 2.133,
    /** MENÜS CHECKEN (Einschlag auf "Menüs" 13,54 s) */
    menues: 3.267,
    /** MENÜS CHECKEN + Menü bekommen einen kleinen Puls + Glanz ("hilf" 14,55 s) */
    hilf: 4.45,
    /** WIRTSCHAFTS SYSTEM (Einschlag auf "Wirtschaft" 15,65 s) */
    wirtschaft: 5.367,
    /** Glanz + Puls auf dem Banner ("perfekt" 16,29 s) */
    perfekt: 6.167,
    /** Banner wippt wie eine Waage ("auszubalancieren" 16,96 s) */
    balance: 6.833,
  },
  prefix: {
    /** DEINE BELOHNUNG (Geschenk) – Einschlag auf "Dankeschön" 19,08 s */
    danke: 0.267,
    /** DEINE BELOHNUNG pulsiert + glänzt nochmal ("bekommst du" 20,06 s) */
    du: 1.433,
    /** EXKLUSIVER PREFIX (Einschlag auf "ex-KLU-siven" 20,68 s) */
    exklusiv: 1.867,
    /** Chat-Leiste "BETATESTER …" + echte Chatzeile ("Betatester-Prefix" 21,89 s) */
    chat: 3.167,
    /** BATTLEPASS-Ticket schiebt die Chat-Zeilen raus (Einschlag auf "Be-LOH-nungen" 24,17 s) */
    battlepass: 5.367,
    /** KOSTENLOS!-Stempel auf dem Battlepass (Einschlag 24,67 s) */
    kostenlos: 5.867,
  },
  slots: {
    /** alle 20 Slots sind da, Zähler springt auf 20 ("zwanzig Plätze" 26,75 s) */
    filled: 1.35,
    /** Alarm "SCHNELL SEIN!" ("wer zuerst kommt" 27,65 s) */
    alarm: 2.2,
    /** danach werden 3 Plätze "vergeben", Zähler "NOCH FREI" 20 -> 17 ("kommt" 27,9 / 28,25 / "mahlt zuerst" 28,6 s) */
    taken: [2.5, 2.85, 3.2],
  },
  discord: {
    /** DISCORD BEITRETEN (Einschlag auf "Discord" 30,95 s) */
    discord: 1.233,
    /** "/dc" im Ingame-Banner wird markiert (kurz nach "Discord") */
    highlight: 1.6,
    /** Cursor klickt "Jetzt bewerben" ("öffne" 31,82 s) + Klick-Sound */
    click: 2.28,
    /** echtes Formular "Betatester" ploppt auf */
    form: 2.45,
    /** #TICKETS ÖFFNEN (Einschlag auf "Ticket" 32,42 s) */
    ticket: 2.7,
    /** "Absenden" im Formular wird markiert */
    submit: 3.3,
    /** JETZT BEWERBEN KLICKEN (Einschlag auf "Link" 33,20 s) */
    link: 3.5,
    /** Popup geht zu, der Banner wippt nochmal ("Bio" 33,81 s) */
    bio: 4.267,
  },
};

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
  /** Segment 4: Stern + Krone + "EXKLUSIVER PREFIX" */
  exklusiverPrefix: {file: 'exklusiver-prefix.png', width: 1988, height: 674, box: {x: 25, y: 18, w: 1940, h: 633}},
  /** Segment 4: Chat-Leiste "★ BETATESTER Deinname » GG!" */
  betatesterChat: {file: 'betatester-chat.png', width: 2016, height: 475, box: {x: 36, y: 42, w: 1952, h: 393}},
  /** Segment 4: Geschenk + "DEINE BELOHNUNG" */
  deineBelohnung: {file: 'deine-belohnung.png', width: 1975, height: 704, box: {x: 11, y: 14, w: 1943, h: 674}},
  /** Segment 4: Ticket "BATTLEPASS · PREMIUM" */
  battlepass: {file: 'battlepass.png', width: 1973, height: 639, box: {x: 12, y: 16, w: 1949, h: 604}},
  /** Segment 4: schräger Stempel "KOSTENLOS!" (auf dem Battlepass) */
  kostenlos: {file: 'kostenlos.png', width: 1464, height: 825, box: {x: 22, y: 24, w: 1419, h: 772}},
  /** Segment 6: "DISCORD BEITRETEN »" */
  discordBeitreten: {file: 'discord-beitreten.png', width: 1970, height: 597, box: {x: 21, y: 26, w: 1926, h: 544}},
  /** Segment 6: Ticket + "#TICKETS ÖFFNEN »" */
  ticketsOeffnen: {file: 'tickets-oeffnen.png', width: 2012, height: 556, box: {x: 11, y: 12, w: 1993, h: 532}},
  /** (Reserve) Rakete + "JETZT BEWERBEN »" – ersetzt durch jetztBewerbenKlicken */
  jetztBewerben: {file: 'jetzt-bewerben.png', width: 1991, height: 492, box: {x: 15, y: 14, w: 1963, h: 464}},
  /** Segment 6, letzter Banner ("Link in der Bio"): Klemmbrett + "JETZT BEWERBEN KLICKEN »" */
  jetztBewerbenKlicken: {file: 'jetzt-bewerben-klicken.png', width: 2014, height: 534, box: {x: 8, y: 13, w: 1995, h: 508}},
  /** (Reserve) großes Discord-Logo + "JETZT BEWERBEN" + "DISCORD" – z. B. statt jetztBewerbenKlicken
   *  in BRAND.cta einsetzbar; auf der End-Card wäre es zu voll */
  jetztBewerbenDiscord: {file: 'jetzt-bewerben-discord.png', width: 1677, height: 680, box: {x: 31, y: 23, w: 1624, h: 633}},
  /** End-Card: "DISCORD" + Einladungslink discord.gg/ZGAxYfG5yX */
  discordLink: {file: 'discord-link.png', width: 2016, height: 589, box: {x: 13, y: 25, w: 1988, h: 534}},
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
    /** Glow-Puls + Glanz über der "20" (Spitze 3 Frames später, auf "zwanzig") */
    pulse: {at: CUE.testers.pulse20} as Cue,
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
      {toX: 862, cue: {at: CUE.testers.java} as Cue},
      {toX: 1975, cue: {at: CUE.testers.bedrock} as Cue},
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
    start: CUE.testers.orbit,
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
        cue: {at: CUE.checklist.teste},
        screen: 'quest',
        pulse: {center: [1725, 175], size: [300, 260]},
      },
      {
        image: 'bugsFinden',
        width: 860,
        cue: {at: CUE.checklist.bugs},
        screen: 'glitch',
        pulse: {center: [1770, 205], size: [300, 270]},
      },
      {image: 'menuesChecken', width: 850, cue: {at: CUE.checklist.menues}, screen: 'menu'},
      {image: 'wirtschaft', width: 850, cue: {at: CUE.checklist.wirtschaft}, screen: 'economy'},
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
    slamFrames: SLAM_FRAMES,
    shake: 9,
    /** so viele Frames braucht der vorige Banner, um weggeschoben zu werden */
    exitFrames: 8,
    /** letzter Banner (WIRTSCHAFTS SYSTEM): Glanz + kleiner Puls bei "perfekt", danach wippt er
     *  wie eine Waage bei "auszubalancieren" (Grad, Dauer in s) */
    bumpAt: CUE.checklist.perfekt,
    balance: {at: CUE.checklist.balance, degrees: 4, duration: 1.4},
    /** weitere kleine Pulse + Glanz auf dem gerade stehenden Banner + seinem Screenshot (s ab
     *  Segmentstart), damit es zwischen zwei Einschlägen nicht stillsteht ("hilf") */
    bumps: [CUE.checklist.hilf] as number[],
    /** echte Screenshots unter dem Banner: Abstand zur Unterkante des Banners, zu dem der
     *  Screenshot gehört (px) + Skalierung (1 = Originalgröße der Karte; größer stößt das Menü
     *  an die Fortschritts-Kästchen) */
    screenGap: 40,
    screenScale: 1,
    /** Fortschritts-Kästchen: Oberkante (px von oben), Größe (px); null = keine */
    progressTop: 1048 as number | null,
    progressSize: 54,
  },

  /**
   * Segment 4 (Belohnungen): oben knallen nacheinander Banner rein (wie Segment 3, immer nur einer
   * groß): "DEINE BELOHNUNG" (Geschenk) bei "Dankeschön" (pulsiert nochmal bei "bekommst du"),
   * "EXKLUSIVER PREFIX" bei "exklusiven". Darunter die Chat-Leiste "BETATESTER Deinname » GG!" (bei
   * "Betatester-Prefix") und klein die echte Chatzeile "[Beta Tester] Inhaber" als Beweis. Bei
   * "weitere Belohnungen" schiebt das Battlepass-Ticket die beiden Chat-Zeilen raus und knallt in die
   * Mitte, dann der "KOSTENLOS!"-Stempel – alles bleibt bis zum Schnitt ≥ 0,7 s lesbar.
   * Positionen = px im Bild (1080×1920).
   */
  rewards: {
    banners: [
      // Geschenk etwas tiefer (Bildmitte ist bis zur Chat-Leiste frei), Funken-Explosion beim
      // Einschlag und bei "bekommst du" (burst)
      {image: 'deineBelohnung', width: 860, cue: {at: CUE.prefix.danke}, bumpAt: CUE.prefix.du, centerY: 600, burst: true},
      {image: 'exklusiverPrefix', width: 860, cue: {at: CUE.prefix.exklusiv}},
    ] as {image: BrandImageName; width: number; cue: Cue; bumpAt?: number; centerY?: number; burst?: boolean}[],
    /** Mitte der Banner-Fläche (px von oben) */
    centerY: 480,
    fromScale: 1.7,
    slamFrames: SLAM_FRAMES,
    shake: 9,
    exitFrames: 8,
    /** Battlepass-Ticket: sichtbare Breite, Mitte (px von oben), Start. 800 px = rechter Rand bei
     *  x ≈ 940 (rechte TikTok-Leiste bleibt frei) */
    battlepass: {image: 'battlepass' as BrandImageName, width: 800, centerY: 870, cue: {at: CUE.prefix.battlepass} as Cue},
    /** "KOSTENLOS!"-Stempel: sichtbare Breite, Mitte (px), Drehung, Start (s ab Segmentstart);
     *  Einschlag nach 5 Frames. Mitte x 740 -> rechter Rand ≈ 915 */
    kostenlos: {width: 350, center: [740, 1058] as [number, number], rotate: -8, at: CUE.prefix.kostenlos},
    /** Chat-Leiste: sichtbare Breite, Mitte (px von oben), Stichwort */
    chatBar: {width: 900, centerY: 835, cue: {at: CUE.prefix.chat} as Cue},
    /** echte Chatzeile darunter: Skalierung, Oberkante (px von oben), Frames nach der Chat-Leiste */
    realChat: {scale: 1.4, top: 978, delay: 6},
    /** echtes Menü "Prefix wählen" vor dem ersten Banner: Skalierung, Mitte (px von oben);
     *  null = aus (es kommt schon in Segment 3 unter MENÜS CHECKEN vor) */
    menu: null as {scale: number; centerY: number} | null,
  },

  /**
   * Segment 6 (Discord): unten (wo früher die Untertitel standen) knallen nacheinander deine
   * Banner rein: "DISCORD BEITRETEN" bei "Discord", "#TICKETS ÖFFNEN" bei "Ticket",
   * "JETZT BEWERBEN KLICKEN" bei "Link in der Bio" (wippt bei "Bio" nochmal). Zeiten = s ab
   * Segmentstart.
   */
  cta: {
    banners: [
      {image: 'discordBeitreten', width: 860, cue: {at: CUE.discord.discord}},
      {image: 'ticketsOeffnen', width: 860, cue: {at: CUE.discord.ticket}},
      {image: 'jetztBewerbenKlicken', width: 860, cue: {at: CUE.discord.link}, bumpAt: CUE.discord.bio},
    ] as {image: BrandImageName; width: number; cue: Cue; bumpAt?: number}[],
    centerY: 1300,
    fromScale: 1.6,
    slamFrames: SLAM_FRAMES,
    shake: 9,
    exitFrames: 8,
  },

  /** (nicht mehr benutzt) EXKLUSIV-Pill – ersetzt durch BRAND.rewards */
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
    // Checkliste -> Belohnungen: diesmal von rechts unten nach links oben
    {cut: 3, frames: 12, from: [1440, 1120], to: [-380, -420], size: 380},
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
 *  - brightness: Helligkeit (1 = normal) für dunkle Aufnahmen.
 */
export type ClipShot = {
  file: string;
  start: number;
  end: number;
  zoom?: Animated;
  focusX?: Animated;
  focusY?: Animated;
  /** Helligkeit (1 = normal), z. B. 1,2 für dunkle Aufnahmen */
  brightness?: number;
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
  /** weitere Clips innerhalb des Segments: ab Sekunde `at` (ab Segmentstart) läuft `clip`
   *  (harter Schnitt mit kurzem Zoom-Punch + kleinem Blitz) */
  cuts?: {at: number; clip: ClipShot}[];
  /** Farbe des Blitzes beim Schnitt IN dieses Segment (Standard: Cyan) */
  flash?: string;
};

// Zeitfenster = Sätze deiner Aufnahme (Take 2). Jeder Schnitt liegt ≈ 0,2 s vor dem ersten
// Wort des Satzes – Ausnahme Segment 5: der Schnitt liegt zwischen "Aber" und "Achtung"
// (25,40 s), damit BATTLEPASS + KOSTENLOS vorher noch ≥ 0,5 s lesbar stehen und der rote
// Alarm-Schnitt genau auf "Achtung" knallt. Das letzte `to` (34,566667) + End-Card (3 s) =
// Länge der Tonspur (1127 Frames).
// Clip-Tempo (Clip-Sekunden / Segment-Sekunden): jeder Moment der Clips kommt nur EINMAL vor.
export const SEGMENTS: Segment[] = [
  {
    from: 0,
    to: 5.733333,
    text: 'Dieser Minecraft-Server ist noch **nicht fertig** … und genau deshalb brauchen wir **dich**!',
    visual: 'intro',
    fontSize: 78,
    // Drohnenflug runter auf die Spawn-Insel (0,94×, etwas aufgehellt) – startet mit Zoom-Punch
    // mitten in der Bewegung …
    clip: {file: 'spawn-insel.mp4', start: 0, end: 3.15, zoom: [1.18, 1.45], focusX: [50, 53], focusY: [58, 76], brightness: 1.18},
    // … Schnitt auf "und genau deshalb": STURZFLUG über den See auf die Brücke, landet bei "dich" auf
    // dem Spieler "[Beta Tester] Inhaber" (0,69×). see-bruecke-dive.mp4 = see-bruecke.mp4 10,70–12,40 s
    // RÜCKWÄRTS (ffmpeg -vf reverse) – im Original zieht die Drohne vom Spieler weg.
    cuts: [
      {
        at: CUE.intro.cut,
        clip: {file: 'see-bruecke-dive.mp4', start: 0.033, end: 1.667, zoom: [1.35, 1.45], focusX: 50, focusY: [38, 72], brightness: 1.12},
      },
    ],
  },
  {
    from: 5.733333,
    to: 10.1,
    text: 'Galacticfy sucht **20 Betatester** – für **Java und Bedrock**!',
    visual: 'testers',
    fontSize: 84,
    // Anflug über den Plaza auf das lila Portal (1,01×)
    clip: {file: 'portal-plaza.mp4', start: 0, end: 4.4, zoom: [1.04, 1.16], focusY: [42, 48]},
  },
  {
    from: 10.1,
    to: 18.633333,
    text: 'Teste unsere Systeme, finde Bugs, check die Menüs und hilf uns, die Wirtschaft perfekt auszubalancieren.',
    visual: 'checklist',
    fontSize: 64,
    // raus aus dem Portal + Kreisflug ums Portal mit Stadt & Marktständen (1,01×)
    clip: {file: 'portal-plaza.mp4', start: 4.4, end: 13.0, zoom: [1.12, 1], focusY: [44, 50]},
  },
  {
    from: 18.633333,
    to: 25.4,
    text: 'Als Dankeschön bekommst du einen **exklusiven Betatester-Prefix** und weitere Belohnungen!',
    visual: 'prefix',
    fontSize: 72,
    // Haus mit Grasdach -> durch die Bar -> raus zum See (0,78×, Kamerafahrt; aufgehellt, der
    // Anfang ist dunkel)
    clip: {file: 'see-bruecke.mp4', start: 0, end: 5.3, zoom: [1.12, 1.02], focusY: [50, 46], brightness: 1.25},
  },
  {
    from: 25.4,
    to: 29.533333,
    text: 'Aber Achtung: Es gibt nur **20 Plätze** – wer zuerst kommt …',
    visual: 'slots',
    fontSize: 80,
    // Drohne über die Brücke auf den laufenden Spieler zu (0,79×, Drohne)
    clip: {file: 'see-bruecke.mp4', start: 5.3, end: 8.55, zoom: [1.04, 1.2], focusY: [52, 64]},
    flash: '#ff3355',
  },
  {
    from: 29.533333,
    to: 34.566667,
    text: 'Komm jetzt auf unseren **Discord** und öffne ein Ticket! Link in der Bio.',
    visual: 'discord',
    fontSize: 80,
    // zurück ins lila Portal, Zoom rein (Übergang zur End-Card) (0,83×)
    clip: {file: 'portal-plaza.mp4', start: 13.0, end: 17.2, zoom: [1.1, 1.6], focusX: [38, 27], focusY: [50, 46]},
  },
];

/**
 * Segment 1 (Hook): großes "Server lädt"-Panel MITTEN im Bild. Steht schon in Frame 0 da (kein
 * Einblenden aus Schwarz) – mit Start-Glitch ("ERR"-Flackern), Zoom-Punch und Wackeln – und läuft
 * schnell hoch, bleibt bei "nicht" hängen (Glitch + Glitch-Sound), bei "fertig" knallt ein
 * Zoom-Punch übers ganze Bild. Beim Schnitt ("und genau…") rückt das Panel nach oben (Bildmitte
 * frei für den Sturzflug auf den Spieler), kippt bei "deshalb" in den Fehler-Zustand und bei "dich"
 * knallt der Sticker drauf. Zeiten ab Segmentstart (siehe CUE.intro).
 */
export const INTRO = {
  label: 'SERVER LÄDT…',
  /** Text im Fehler-Zustand (ab "deshalb") */
  errorLabel: 'TESTER FEHLEN!',
  errorAt: CUE.intro.deshalb,
  /** Sticker auf dem Panel (Einschlag genau auf "dich") */
  sticker: 'GESUCHT: DU!',
  stickerAt: CUE.intro.dich,
  /** Prozent in Frame 0 (läuft sofort, damit das erste Bild schon Bewegung hat) */
  startPercent: 12,
  /** bei diesem Wert bleibt der Balken hängen ("nicht") */
  percent: 73,
  /** Start-Glitch in Frame 0 (Frames, die er dauert) */
  bootGlitch: {at: CUE.intro.boot, frames: 6},
  /** Glitch-Stöße auf Balken + Bild (Sekunden ab Segmentstart) */
  glitches: [CUE.intro.nicht, CUE.intro.fertig],
  /** Mitte des Panels (Bild-px von oben) am Anfang, Größe 1 = ≈ 795 px breit */
  centerY: 730,
  /** nach dem Schnitt: Panel rückt nach oben (Mitte, Skalierung) */
  dock: {at: CUE.intro.cut, centerY: 462, scale: 0.8},
};

/**
 * Zoom-Punches übers ganze Bild (Kamera "springt" kurz rein und federt zurück): Segment +
 * Sekunden ab Segmentstart + Stärke (0,06 = 6 % größer).
 */
export const PUNCHES: {seg: SegmentVisual; at: number; amount: number}[] = [
  {seg: 'intro', at: CUE.intro.boot, amount: 0.06},
  {seg: 'intro', at: CUE.intro.fertig, amount: 0.07},
  {seg: 'intro', at: CUE.intro.dich, amount: 0.06},
  {seg: 'slots', at: 0, amount: 0.05},
];

/** Echtes Menü "Prefix wählen" (Segment 3 unter MENÜS CHECKEN; in Segment 4 nur, wenn
 *  BRAND.rewards.menu gesetzt ist). Zeiten ab Segmentstart. */
export const PREFIX = {
  /** Lesbares Schild über der Titelleiste des echten Menüs (Original-Titel ist kaum lesbar) */
  menuTitle: 'Prefix wählen',
  /** Menü ploppt in Segment 4 auf (nur mit BRAND.rewards.menu) */
  menuAt: 0.1,
};

/**
 * Segment 5: 20 Plätze. Die Slots ploppen auf, der Zähler landet genau bei "zwanzig Plätze" auf
 * 20 ("PLÄTZE"). Bei "wer zuerst kommt" Alarm: der Zähler wird zu "NOCH FREI" und zählt runter
 * (20 -> 17), drei Slots werden "vergeben" (Spielerkopf, rot) – die meisten bleiben frei, also
 * nicht "ausgebucht", aber: schnell sein. Zeiten ab Segmentstart.
 */
export const SLOTS = {
  total: 20,
  /** Bis wann alle 20 Slots aufgeploppt sind (Zähler = 20 genau bei "zwanzig Plätze") */
  filledAt: CUE.slots.filled,
  /** Ab wann es rot blinkt (bei "wer zuerst kommt") */
  alarmAt: CUE.slots.alarm,
  /** Wann jeweils ein Platz vergeben wird (Zähler −1) und welcher Slot (0–19) */
  takenAt: CUE.slots.taken,
  takenSlots: [3, 16, 9],
  label: 'PLÄTZE',
  /** Zähler-Text ab dem Alarm */
  alarmCountLabel: 'NOCH FREI',
  alarmLabel: 'SCHNELL SEIN!',
  /** echte Scoreboard-Zeile "Online › 1/20" zeigen? Aus: "1/20" las sich neben dem Zähler wie
   *  "nur noch 19 frei" (gemischte Botschaft) */
  showOnline: false,
};

/**
 * Discord-Segment: der ECHTE Bewerbungsweg. Oben das Ingame-Banner "DISCORD /dc", darunter
 * der Kanal "# tickets" mit dem Embed-Abschnitt "Betatester werden" und (groß, eigene Zeile)
 * dem Button "Jetzt bewerben"; der Cursor klickt ihn, dann ploppt das echte Formular
 * "Betatester" auf (mittig, Hintergrund abgedunkelt wie in Discord) und geht kurz vor der
 * End-Card wieder zu. Alles echte Screenshots (SCREENS.discord*), Zeiten ab Segmentstart.
 */
export const DISCORD = {
  /** Wann "/dc" im Banner markiert wird (kurz nach "Discord") */
  highlightAt: CUE.discord.highlight,
  /** Wann der Cursor "Jetzt bewerben" klickt (bei "öffne ein Ticket") */
  clickAt: CUE.discord.click,
  /** Mausklick-Sound beim Klick (public/sfx/click.wav), Lautstärke 0–1; null = aus */
  clickSound: 'sfx/click.wav' as string | null,
  clickVolume: 0.9,
  /** Wann das Formular "Betatester" aufploppt */
  formAt: CUE.discord.form,
  /** Wann "Absenden" im Formular kurz markiert wird */
  submitHintAt: CUE.discord.submit,
  /** Wann das Popup wieder zugeht (Formular + Abdunklung blenden aus), damit vor der End-Card
   *  noch der Flug ins lila Portal zu sehen ist ("Bio"). Das Discord-Fenster + "/dc"-Banner
   *  blenden schon beim Aufploppen des Formulars aus (keine abgeschnittenen Reste neben dem Popup). */
  closeAt: CUE.discord.bio,
  /** Größe des Formulars (480×786 px Original): 1,18 = ca. 566×928 px, passt zwischen
   *  obere Safe-Zone und die Banner unten */
  formScale: 1.18,
  /** Oberkante des Formulars im Bild (px von oben) */
  formTop: 158,
  /** Abdunklung hinter dem Formular (0–1), wie bei einem Discord-Popup */
  formDim: 0.6,
  /** "(Link in Bio)" mit Pfeilen unter dem letzten Banner, ab "Bio" (passt zum gesprochenen Text) */
  linkInBio: {at: CUE.discord.bio, centerY: 1462},
};

// ------------------------------------------------------------
//  SOUND-EFFEKTE (public/sfx/, selbst erzeugt mit scripts/make-sfx.py)
// ------------------------------------------------------------
/** Peak des Whooshs liegt 0,26 s nach Dateibeginn -> so viel früher starten (Peak = Schnitt). */
export const WHOOSH_PEAK = 0.26;
/** Riser ist bei 1,13 s am lautesten und dann sofort still -> so viel vor dem Ziel starten. */
export const RISER_PEAK = 1.13;

/**
 * Ein Sound-Effekt: Datei unter public/sfx/, Segment (`visual` aus SEGMENTS oder 'endCard')
 * + Sekunden ab dessen Start (negativ = vor dem Schnitt), Lautstärke 0–1.
 * Einschläge: `at: CUE.… + SLAM` = genau der Einschlag-Frame des Banners.
 * Die Stimme bleibt vorne: Effekte meist 0,3–0,65.
 */
export type Sfx = {label: string; file: string; seg: SegmentVisual | 'endCard'; at: number; volume: number};

/** Alle Sounds an/aus (der Mausklick im Discord-Teil hängt an DISCORD.clickSound). */
export const USE_SFX = true;
export const SFX: Sfx[] = [
  // --- 1 Intro: Start-Glitch, "nicht fertig", "deshalb", "dich" ---
  {label: 'Start-Glitch (Frame 0)', file: 'glitch.wav', seg: 'intro', at: CUE.intro.boot, volume: 0.32},
  {label: 'Glitch "nicht"', file: 'glitch.wav', seg: 'intro', at: CUE.intro.nicht - 0.03, volume: 0.45},
  {label: 'Punch "fertig"', file: 'impact-small.wav', seg: 'intro', at: CUE.intro.fertig, volume: 0.42},
  {label: 'TESTER FEHLEN! "deshalb"', file: 'impact-small.wav', seg: 'intro', at: CUE.intro.deshalb, volume: 0.38},
  {label: 'Sticker GESUCHT: DU! "dich"', file: 'impact-small.wav', seg: 'intro', at: CUE.intro.dich, volume: 0.5},

  // --- 2 Testers: Raketen-Übergang + Drop der Musik ---
  {label: 'Whoosh Rakete -> 2', file: 'whoosh.wav', seg: 'testers', at: -WHOOSH_PEAK, volume: 0.6},
  {label: 'Hero "20 TESTER GESUCHT"', file: 'impact.wav', seg: 'testers', at: 0, volume: 0.55},
  {label: 'Puls auf der "20"', file: 'impact-small.wav', seg: 'testers', at: CUE.testers.pulse20 + 3 / FPS, volume: 0.5},
  {label: 'Wisch JAVA', file: 'impact-small.wav', seg: 'testers', at: CUE.testers.java + 1 / FPS, volume: 0.45},
  {label: 'Wisch & BEDROCK', file: 'impact.wav', seg: 'testers', at: CUE.testers.bedrock + 1 / FPS, volume: 0.5},

  // --- 3 Checkliste ---
  {label: 'Whoosh Schnitt -> 3', file: 'whoosh.wav', seg: 'checklist', at: -WHOOSH_PEAK, volume: 0.42},
  {label: 'FEATURES TESTEN', file: 'impact.wav', seg: 'checklist', at: CUE.checklist.teste + SLAM, volume: 0.6},
  {label: 'Haken 1', file: 'pop.wav', seg: 'checklist', at: CUE.checklist.teste + SLAM + 4 / FPS, volume: 0.28},
  {label: 'BUGS FINDEN', file: 'impact-small.wav', seg: 'checklist', at: CUE.checklist.bugs + SLAM, volume: 0.5},
  {label: 'BUGS FINDEN Glitch', file: 'glitch.wav', seg: 'checklist', at: CUE.checklist.bugs + SLAM, volume: 0.45},
  {label: 'Bossbar Glitch', file: 'glitch.wav', seg: 'checklist', at: CUE.checklist.bugs + SLAM + 15 / FPS, volume: 0.3},
  {label: 'Haken 2', file: 'pop.wav', seg: 'checklist', at: CUE.checklist.bugs + SLAM + 4 / FPS, volume: 0.28},
  {label: 'MENÜS CHECKEN', file: 'impact.wav', seg: 'checklist', at: CUE.checklist.menues + SLAM, volume: 0.55},
  {label: 'Haken 3', file: 'pop.wav', seg: 'checklist', at: CUE.checklist.menues + SLAM + 4 / FPS, volume: 0.28},
  {label: 'WIRTSCHAFTS SYSTEM', file: 'impact.wav', seg: 'checklist', at: CUE.checklist.wirtschaft + SLAM, volume: 0.6},
  {label: 'Haken 4', file: 'pop.wav', seg: 'checklist', at: CUE.checklist.wirtschaft + SLAM + 4 / FPS, volume: 0.28},

  // --- 4 Belohnungen ---
  {label: 'Whoosh Rakete -> 4', file: 'whoosh.wav', seg: 'prefix', at: -WHOOSH_PEAK, volume: 0.6},
  {label: 'DEINE BELOHNUNG "Dankeschön"', file: 'impact.wav', seg: 'prefix', at: CUE.prefix.danke + SLAM, volume: 0.6},
  {label: 'EXKLUSIVER PREFIX', file: 'impact.wav', seg: 'prefix', at: CUE.prefix.exklusiv + SLAM, volume: 0.65},
  {label: 'Chat-Leiste', file: 'pop.wav', seg: 'prefix', at: CUE.prefix.chat + 4 / FPS, volume: 0.45},
  {label: 'echte Chatzeile', file: 'pop.wav', seg: 'prefix', at: CUE.prefix.chat + 10 / FPS, volume: 0.3},
  {label: 'BATTLEPASS "Belohnungen"', file: 'impact.wav', seg: 'prefix', at: CUE.prefix.battlepass + SLAM, volume: 0.6},
  {label: 'KOSTENLOS! (Ding)', file: 'ding.wav', seg: 'prefix', at: CUE.prefix.kostenlos + 5 / FPS, volume: 0.5},
  {label: 'KOSTENLOS! (Stempel)', file: 'impact-small.wav', seg: 'prefix', at: CUE.prefix.kostenlos + 5 / FPS, volume: 0.32},

  // --- 5 Plätze: roter Alarm-Schnitt auf "Achtung" ---
  {label: '"Achtung" Schnitt', file: 'impact.wav', seg: 'slots', at: 0, volume: 0.6},
  {label: 'Zähler = 20', file: 'pop.wav', seg: 'slots', at: CUE.slots.filled, volume: 0.3},
  {label: 'Alarm SCHNELL SEIN!', file: 'impact.wav', seg: 'slots', at: CUE.slots.alarm, volume: 0.55},

  // --- 6 Discord ---
  {label: 'Whoosh Schnitt -> 6', file: 'whoosh.wav', seg: 'discord', at: -WHOOSH_PEAK, volume: 0.42},
  {label: 'DISCORD BEITRETEN', file: 'impact.wav', seg: 'discord', at: CUE.discord.discord + SLAM, volume: 0.6},
  {label: '"/dc" markiert', file: 'pop.wav', seg: 'discord', at: CUE.discord.highlight, volume: 0.35},
  {label: 'Formular ploppt auf', file: 'pop.wav', seg: 'discord', at: CUE.discord.form + 2 / FPS, volume: 0.28},
  {label: '#TICKETS ÖFFNEN', file: 'impact.wav', seg: 'discord', at: CUE.discord.ticket + SLAM, volume: 0.55},
  {label: '"Absenden" markiert', file: 'pop.wav', seg: 'discord', at: CUE.discord.submit, volume: 0.3},
  {label: 'JETZT BEWERBEN KLICKEN', file: 'impact.wav', seg: 'discord', at: CUE.discord.link + SLAM, volume: 0.62},

  // --- End-Card (die Musik hat dort selbst Riser + Crash -> Riser hier nur leise) ---
  {label: 'Riser -> End-Card', file: 'riser.wav', seg: 'endCard', at: -RISER_PEAK, volume: 0.22},
  {label: 'Whoosh Rakete -> End-Card', file: 'whoosh.wav', seg: 'endCard', at: -WHOOSH_PEAK, volume: 0.6},
  {label: 'End-Card Einschlag', file: 'impact.wav', seg: 'endCard', at: 0, volume: 0.55},
];

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
  // Drohne übers Wasser (stark weichgezeichnet; 0,62×) – kommt sonst nirgends vor
  file: 'see-bruecke.mp4',
  start: 8.6,
  end: 10.45,
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
