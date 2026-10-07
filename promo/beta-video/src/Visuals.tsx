import React from 'react';
import {Easing, interpolate, random, spring, useCurrentFrame, useVideoConfig} from 'remotion';
import {
  BRAND,
  ChecklistScreen,
  COLORS,
  DISCORD,
  FONT_HEAVY,
  FONT_PIXEL,
  INTRO,
  PREFIX,
  SCREEN_OUTLINES,
  SCREEN_REGIONS,
  SCREENS,
  SLOTS,
  sec,
} from './config';
import {BrandImg, BrightCopy, RocketSprite, Shine, brandHeight, shineMask, slamAt, wrapDeg} from './Brand';
import {PixelIcon} from './PixelIcon';
import {cueFrame} from './RichText';
import {Highlight, Screen} from './Screen';

/**
 * duration = Segment-Länge in Frames, stage = Visual-Bereich im Bild (px): top/height,
 * caption = Text der Segment-Caption (für Effekte, die auf ein gesprochenes Wort warten).
 */
type VProps = {duration: number; stage?: {top: number; height: number}; caption?: string};

const clamp = {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'} as const;

const usePop = (delay = 0, damping = 12) => {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();
  return spring({frame: frame - delay, fps, config: {damping, stiffness: 160, mass: 0.7}});
};

/** Neon-Glow für echte Screenshots (drop-shadow folgt auch transparenten Rändern). */
const neon = (color: string, strength = 1) =>
  `drop-shadow(0 0 ${14 * strength}px ${color}) drop-shadow(0 10px 22px rgba(0,0,0,0.55))`;

/* ---------- Glas-Panel: halbtransparent, damit das Gameplay durchscheint ---------- */
export const Glass: React.FC<{
  children: React.ReactNode;
  accent?: string;
  style?: React.CSSProperties;
}> = ({children, accent = COLORS.purple, style}) => (
  <div
    style={{
      background: 'linear-gradient(180deg, rgba(22,10,52,0.66), rgba(8,4,24,0.74))',
      border: `4px solid ${accent}b3`,
      boxShadow: `0 0 26px ${accent}66, inset 0 0 28px ${accent}26, 0 14px 40px rgba(0,0,0,0.45)`,
      backdropFilter: 'blur(8px)',
      ...style,
    }}
  >
    {children}
  </div>
);

/* ---------- Pixel-Planet ---------- */
const PixelPlanet: React.FC<{size: number}> = ({size}) => {
  const frame = useCurrentFrame();
  const N = 20;
  const cells: React.ReactNode[] = [];
  const shift = frame / 6;
  for (let y = 0; y < N; y++) {
    for (let x = 0; x < N; x++) {
      const dx = x - N / 2 + 0.5;
      const dy = y - N / 2 + 0.5;
      const d = Math.sqrt(dx * dx + dy * dy);
      if (d > N / 2) continue;
      // Bänder + Schattierung
      const band = Math.floor((y + Math.sin((x + shift) / 3) * 1.5) / 3) % 3;
      const light = dx * -0.6 + dy * -0.6;
      const colors = ['#7a2cff', '#b44dff', '#4b18a0'];
      let c = colors[(band + 3) % 3];
      if (light < -4) c = '#2a0d5c';
      if (d > N / 2 - 1.2 && light > 2) c = '#e3b6ff';
      cells.push(<rect key={`${x}-${y}`} x={x} y={y} width={1.02} height={1.02} fill={c} />);
    }
  }
  return (
    <svg
      width={size}
      height={size}
      viewBox={`-6 -6 ${N + 12} ${N + 12}`}
      shapeRendering="crispEdges"
      style={{filter: `drop-shadow(0 0 ${size / 12}px ${COLORS.purple})`, overflow: 'visible'}}
    >
      {/* Ring hinten */}
      <ellipse cx={N / 2} cy={N / 2} rx={N * 0.82} ry={N * 0.18} fill="none" stroke={COLORS.cyan} strokeWidth={1.1} opacity={0.5} transform={`rotate(-18 ${N / 2} ${N / 2})`} />
      {cells}
      {/* Ring vorne (untere Hälfte) */}
      <path
        d={`M ${N / 2 - N * 0.82} ${N / 2} A ${N * 0.82} ${N * 0.18} 0 0 0 ${N / 2 + N * 0.82} ${N / 2}`}
        fill="none"
        stroke={COLORS.cyan}
        strokeWidth={1.1}
        transform={`rotate(-18 ${N / 2} ${N / 2})`}
      />
    </svg>
  );
};

/* ---------- 1: Intro – kleiner "Server lädt"-Balken ---------- */
export const IntroVisual: React.FC<VProps> = ({duration}) => {
  const frame = useCurrentFrame();
  const pop = usePop(2, 11);
  const progress = interpolate(frame, [8, duration * 0.6], [0, INTRO.percent], {
    ...clamp,
    easing: Easing.out(Easing.cubic),
  });
  const blink = Math.floor(frame / 8) % 2 === 0;
  const segs = 14;
  const filled = Math.round((progress / 100) * segs);
  return (
    <Glass
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 26,
        padding: '18px 30px 18px 18px',
        transform: `translateY(${(1 - pop) * -60}px) scale(${0.8 + 0.2 * pop})`,
        opacity: Math.min(1, pop * 1.4),
      }}
    >
      <div style={{transform: `rotate(${frame * 0.6}deg)`}}>
        <PixelPlanet size={120} />
      </div>
      <div style={{display: 'flex', flexDirection: 'column', gap: 12}}>
        <div
          style={{
            fontFamily: FONT_PIXEL,
            fontWeight: 700,
            fontSize: 36,
            color: COLORS.gold,
            letterSpacing: 2,
            textShadow: '3px 3px 0 #3a2400',
          }}
        >
          {INTRO.label} {Math.round(progress)}%{blink ? '_' : ' '}
        </div>
        <div
          style={{
            display: 'flex',
            gap: 4,
            padding: 5,
            border: `4px solid ${COLORS.white}`,
            background: 'rgba(0,0,0,0.5)',
            boxShadow: `0 0 18px ${COLORS.purple}`,
          }}
        >
          {new Array(segs).fill(0).map((_, i) => (
            <div
              key={i}
              style={{
                width: 32,
                height: 30,
                background: i < filled ? (i % 2 ? COLORS.purple : COLORS.cyan) : 'rgba(255,255,255,0.1)',
                boxShadow: i < filled ? 'inset -5px -5px 0 rgba(0,0,0,0.3)' : undefined,
              }}
            />
          ))}
        </div>
      </div>
    </Glass>
  );
};

/* ---------- 2: 20 Betatester – Java & Bedrock ---------- */

/** Rakete fliegt einmal um den Planeten des Heros und dockt rechts oben an (BRAND.orbit). */
const useOrbitRocket = () => {
  const frame = useCurrentFrame();
  const O = BRAND.orbit;
  const of = frame - sec(O.start);
  if (of < 0) return null;
  const oDur = sec(O.duration);
  const t = Easing.inOut(Easing.sin)(Math.min(1, of / oDur));
  // oben (leicht rechts, hinter dem Planeten) -> rechts -> vorne unten -> links -> hinten -> rechts oben
  const phi0 = -Math.PI / 2 + 0.35;
  const phi1 = 2 * Math.PI - Math.PI / 4;
  const phi = phi0 + (phi1 - phi0) * t;
  const tilt = (O.tilt * Math.PI) / 180;
  const rot = (x: number, y: number) => [x * Math.cos(tilt) - y * Math.sin(tilt), x * Math.sin(tilt) + y * Math.cos(tilt)];
  const [ex, ey] = rot(O.radius[0] * Math.cos(phi), O.radius[1] * Math.sin(phi));
  const [vx, vy] = rot(-O.radius[0] * Math.sin(phi), O.radius[1] * Math.cos(phi));
  const depth = Math.sin(phi); // −1 = hinten, 1 = vorne
  const orbitX = O.center[0] + ex;
  const orbitY = O.center[1] + ey;
  const orbitHeading = (Math.atan2(vy, vx) * 180) / Math.PI;
  const orbitSize = O.size * (1 + 0.22 * depth);
  // Andocken
  const d = Easing.inOut(Easing.cubic)(interpolate(of, [oDur, oDur + sec(O.dockDuration)], [0, 1], clamp));
  const docked = of >= oDur + sec(O.dockDuration);
  const idle = docked ? Math.sin((of - oDur) / 9) : 0;
  return {
    x: orbitX + (O.dock[0] - orbitX) * d,
    y: orbitY + (O.dock[1] - orbitY) * d + idle * 5,
    size: orbitSize + (O.dockSize - orbitSize) * d,
    heading: orbitHeading + wrapDeg(BRAND.rocketHeading - orbitHeading) * d + idle * 2.5,
    behind: depth < 0 && d < 0.35,
    opacity: interpolate(of, [0, 5], [0, 1], clamp),
  };
};

/**
 * Segment 2: die eigene Grafik "20 TESTER GESUCHT" (BRAND.images.hero) groß zwischen Logo und
 * Caption – ploppt mit Federung + leichter Drehung rein, Glow-Puls + Glanz auf der "20" beim
 * Wort "20", schwebt danach sanft. Darunter dein Banner "JAVA & BEDROCK" (BRAND.editions):
 * linkes Panel beim Wort "Java", "&" + BEDROCK beim Wort "Bedrock" (Wisch von links).
 * Alle Positionen in Bild-Pixeln (1080×1920).
 */
export const TestersVisual: React.FC<VProps> = ({duration, stage, caption}) => {
  const frame = useCurrentFrame();
  const {fps, width} = useVideoConfig();
  const stageTop = stage?.top ?? 0;
  const stageH = stage?.height ?? 773;
  const Y = (abs: number) => abs - stageTop;

  const H = BRAND.hero;
  const img = BRAND.images.hero;
  const s = H.width / img.width;
  const heroH = brandHeight(img, H.width);
  const box = img.box;
  // sichtbaren Inhalt (nicht das PNG mit Rand) mittig ausrichten
  const left = width / 2 - (box.x + box.w / 2) * s;
  const top = H.top - box.y * s;
  // Drehpunkt = Mitte der "20"
  const ox = H.pulseCenter[0] * s;
  const oy = H.pulseCenter[1] * s;

  const enter = spring({frame: frame - sec(H.enterAt), fps, config: {damping: 10.5, stiffness: 140, mass: 0.75}});
  const floatIn = interpolate(frame, [14, 30], [0, 1], clamp);
  const floatY = Math.sin((frame - 14) / 13) * H.float * floatIn;
  const idleRot = Math.sin(frame / 21) * 0.8 * floatIn;

  const pulseAt = caption ? cueFrame(H.pulse, caption, duration) : 12;
  const pf = frame - pulseAt;
  const pulse = interpolate(pf, [0, 3, 20], [0, 1, 0], clamp);
  const shine = interpolate(pf, [2, 18], [-15, 118], clamp);
  const glow = 14 + 5 * Math.sin(frame / 6) + 34 * pulse;
  const ring = interpolate(pf, [0, 16], [0, 1], clamp);

  const cx = (H.pulseCenter[0] / img.width) * 100;
  const cy = (H.pulseCenter[1] / img.height) * 100;
  const rx = (H.pulseSize[0] / 2 / img.width) * 100;
  const ry = (H.pulseSize[1] / 2 / img.height) * 100;

  const rocket = useOrbitRocket();
  const rocketEl = rocket ? (
    <RocketSprite
      x={rocket.x}
      y={Y(rocket.y)}
      size={rocket.size}
      heading={rocket.heading}
      opacity={rocket.opacity}
      filter={`drop-shadow(0 0 14px ${COLORS.purple}) drop-shadow(0 8px 12px rgba(0,0,0,0.5))`}
    />
  ) : null;

  return (
    <div style={{position: 'relative', width, height: stageH, flexShrink: 0}}>
      {/* Druckwelle hinter der "20" beim Wort "20" */}
      {ring > 0 && ring < 1 ? (
        <div
          style={{
            position: 'absolute',
            left: left + ox - 520 * ring,
            top: Y(top) + oy + floatY - 520 * ring,
            width: 1040 * ring,
            height: 1040 * ring,
            borderRadius: '50%',
            border: `${10 * (1 - ring) + 2}px solid ${COLORS.cyan}`,
            boxShadow: `0 0 40px ${COLORS.cyan}, inset 0 0 30px ${COLORS.purple}`,
            opacity: 0.85 * (1 - ring),
          }}
        />
      ) : null}
      {rocket?.behind ? rocketEl : null}
      <div
        style={{
          position: 'absolute',
          left,
          top: Y(top),
          width: H.width,
          height: heroH,
          transformOrigin: `${ox}px ${oy}px`,
          transform: `translateY(${floatY}px) scale(${0.3 + 0.7 * enter}) rotate(${(1 - enter) * -14 + idleRot}deg)`,
          opacity: Math.min(1, enter * 3),
          filter: `drop-shadow(0 0 ${glow}px ${COLORS.cyan}aa) drop-shadow(0 16px 26px rgba(0,0,0,0.45))`,
        }}
      >
        <BrandImg img={img} width={H.width} />
        {/* Glow-Puls auf der "20" + Glanz-Streifen über die ganze Grafik */}
        <BrightCopy
          img={img}
          width={H.width}
          opacity={pulse * 0.9}
          mask={`radial-gradient(ellipse ${rx}% ${ry}% at ${cx}% ${cy}%, #000 0%, #000 45%, transparent 100%)`}
        />
        <BrightCopy img={img} width={H.width} opacity={pf >= 2 && pf <= 18 ? 0.85 : 0} mask={shineMask(shine, 9)} brightness={2.1} />
      </div>
      {rocket && !rocket.behind ? rocketEl : null}

      {/* Banner "JAVA & BEDROCK" unter dem Hero, Teil für Teil beim gesprochenen Wort */}
      <EditionsBanner duration={duration} caption={caption} top={Y(BRAND.editions.top)} />
    </div>
  );
};

/**
 * Segment 2: dein Banner "JAVA & BEDROCK" (BRAND_IMAGES.javaBedrock). Wird per Wisch von links
 * aufgedeckt (clip-path): je Stichwort bis zur nächsten x-Position (BRAND.editions.reveal), mit
 * leuchtender Wischkante; danach Glanz. top = Oberkante des sichtbaren Inhalts (Stage-px).
 */
const EditionsBanner: React.FC<{duration: number; caption?: string; top: number}> = ({duration, caption, top}) => {
  const frame = useCurrentFrame();
  const {fps, width} = useVideoConfig();
  const E = BRAND.editions;
  const img = BRAND.images.javaBedrock;
  const s = E.width / img.box.w;
  const w = img.width * s;
  const h = img.height * s;
  const left = width / 2 - (img.box.x + img.box.w / 2) * s;
  const cues = E.reveal.map((r, i) => (caption ? cueFrame(r.cue, caption, duration) : 30 + i * 12));
  if (frame < cues[0]) return null;
  // aufgedeckt bis revealX (PNG-px); jeder Wisch läuft von der vorigen Kante zur nächsten
  let revealX = 0;
  let edge = 0; // Sichtbarkeit der leuchtenden Wischkante
  let run = 0; // 1 = Kante mitten im Wisch (breit), 0 = steht still (schmale Lichtlinie)
  E.reveal.forEach((r, i) => {
    // Wische, deren Stichwort noch nicht kam, zählen nicht (sonst stünde der erste Teil sofort da)
    if (frame < cues[i]) return;
    const prevX = i ? E.reveal[i - 1].toX : 0;
    const t = interpolate(frame - cues[i], [0, E.wipeFrames], [0, 1], {...clamp, easing: Easing.out(Easing.cubic)});
    revealX = Math.max(revealX, prevX + (r.toX - prevX) * t);
    // bis zum nächsten Wisch bleibt die Kante als schmale Lichtlinie stehen (statt eines harten Schnitts)
    const waiting = i < E.reveal.length - 1 && frame <= cues[i + 1];
    const m = t > 0 && t < 1 ? Math.sin(t * Math.PI) : 0;
    run = Math.max(run, m);
    edge = Math.max(edge, waiting && t >= 0.5 ? 0.8 + 0.2 * m : m);
  });
  const full = revealX >= img.width - 0.5;
  const enter = spring({frame: frame - cues[0], fps, config: {damping: 12, stiffness: 170, mass: 0.6}});
  const last = cues[cues.length - 1];
  // kleiner "Stups" bei jedem weiteren Stichwort
  const bump = cues
    .slice(1)
    .reduce((m, c) => Math.max(m, interpolate(frame - c, [0, 3, 10], [0, 1, 0], clamp)), 0);
  const glow = 12 + 4 * Math.sin(frame / 6) + 14 * bump;
  const edgeX = revealX * s;
  return (
    <div
      style={{
        position: 'absolute',
        left,
        top: top - img.box.y * s,
        width: w,
        height: h,
        transformOrigin: `${(img.box.x + img.box.w / 2) * s}px ${(img.box.y + img.box.h / 2) * s}px`,
        transform: `translateY(${(1 - enter) * 26}px) scale(${(0.86 + 0.14 * enter) * (1 + 0.035 * bump)})`,
        opacity: Math.min(1, enter * 2.5),
      }}
    >
      {/* Glow am Eltern-Element -> folgt dem schon aufgedeckten Teil */}
      <div style={{filter: `drop-shadow(0 0 ${glow}px ${COLORS.purple}aa) drop-shadow(0 10px 16px rgba(0,0,0,0.5))`}}>
        <div style={{clipPath: full ? undefined : `inset(0 ${(w - edgeX).toFixed(2)}px 0 0)`}}>
          <BrandImg img={img} width={w} />
          <Shine img={img} width={w} f={frame - (last + E.wipeFrames + E.shineDelay)} frames={16} opacity={0.75} />
        </div>
      </div>
      {edge > 0.01 ? (
        <div
          style={{
            position: 'absolute',
            left: edgeX - (4 + 6 * run) / 2,
            top: img.box.y * s - 6,
            width: 4 + 6 * run,
            height: img.box.h * s + 12,
            borderRadius: 5,
            background: run > 0.3 ? '#fff' : '#e6fdff',
            boxShadow: `0 0 ${10 + 8 * run}px #fff, 0 0 ${24 + 10 * run}px ${COLORS.cyan}, 0 0 60px ${COLORS.purple}`,
            opacity: edge,
          }}
        />
      ) : null}
    </div>
  );
};

/* ---------- 3: Checkliste + echter Screenshot zum aktuellen Punkt ---------- */

/** Bossbar-Quest, zweizeilig: "Erste Schritte (3/8)" + Fortschrittsbalken. */
const QuestCard: React.FC = () => (
  <Glass
    accent={COLORS.cyan}
    style={{display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8, padding: 14}}
  >
    <Screen img={SCREENS.bossbar} region={SCREEN_REGIONS.questTitle} scale={1.45} />
    <Screen img={SCREENS.bossbar} region={SCREEN_REGIONS.questBar} scale={0.94} />
  </Glass>
);

/**
 * Echtes Menü "Prefix wählen" – nur Titel + Truhen-Reihen (ohne leeres Spieler-Inventar).
 * Der Original-Titel (Cyan auf MC-Grau, ~1,4:1 Kontrast) wird von einem dunklen Schild
 * mit demselben Text in der Video-Schrift überdeckt, damit man ihn auf dem Handy lesen kann.
 */
const PrefixMenu: React.FC<{scale: number}> = ({scale}) => {
  const t = SCREEN_REGIONS.prefixMenuTitle;
  return (
    <div style={{position: 'relative'}}>
      <Screen img={SCREENS.prefixMenuChest} scale={scale} />
      <div
        style={{
          position: 'absolute',
          left: t.x * scale,
          top: t.y * scale,
          width: t.w * scale,
          height: t.h * scale,
          boxSizing: 'border-box',
          display: 'flex',
          alignItems: 'center',
          paddingLeft: 22 * scale,
          background: 'linear-gradient(180deg, rgba(24,12,54,0.97), rgba(10,5,30,0.97))',
          borderBottom: `${Math.max(3, Math.round(5 * scale))}px solid ${COLORS.cyan}`,
          fontFamily: FONT_PIXEL,
          fontWeight: 700,
          fontSize: Math.round(t.h * scale * 0.74),
          lineHeight: 1,
          color: COLORS.cyan,
          letterSpacing: 1,
          textShadow: `0 0 10px rgba(46,242,255,0.55), 2px 2px 0 #000`,
          whiteSpace: 'nowrap',
        }}
      >
        {PREFIX.menuTitle}
      </div>
    </div>
  );
};

const MenuCard: React.FC = () => (
  <div style={{filter: neon(COLORS.purple, 1.3)}}>
    <PrefixMenu scale={0.56} />
  </div>
);

const EconomyCard: React.FC = () => (
  <Glass accent={COLORS.gold} style={{padding: 12}}>
    <Screen img={SCREENS.scoreboardEconomy} scale={1.6} />
  </Glass>
);

const CARD: Record<Exclude<ChecklistScreen, 'glitch'>, React.FC> = {
  quest: QuestCard,
  menu: MenuCard,
  economy: EconomyCard,
};

/** Kurzer "Bug"-Glitch: RGB-Versatz + verschobene Streifen. */
const Glitch: React.FC<{amount: number; children: React.ReactNode}> = ({amount, children}) => {
  const frame = useCurrentFrame();
  if (amount <= 0.01) return <>{children}</>;
  const jx = (random(`gx${frame}`) - 0.5) * 34 * amount;
  const bands = [0, 1, 2].map((b) => ({
    top: random(`gt${frame}-${b}`) * 80,
    h: 6 + random(`gh${frame}-${b}`) * 16,
    dx: (random(`gd${frame}-${b}`) - 0.5) * 90 * amount,
  }));
  return (
    <div style={{position: 'relative', transform: `translateX(${jx}px)`}}>
      <div
        style={{
          filter: `drop-shadow(${7 * amount}px 0 0 rgba(255,40,90,0.85)) drop-shadow(${-7 * amount}px 0 0 rgba(40,240,255,0.85))`,
        }}
      >
        {children}
      </div>
      {bands.map((b, i) => (
        <div
          key={i}
          style={{
            position: 'absolute',
            inset: 0,
            clipPath: `inset(${b.top}% 0 ${Math.max(0, 100 - b.top - b.h)}% 0)`,
            transform: `translateX(${b.dx}px)`,
          }}
        >
          {children}
        </div>
      ))}
    </div>
  );
};

/** Frames (ab Segmentstart), in denen die Banner in Segment 3 starten bzw. einschlagen. */
const useChecklistTimes = (duration: number, caption?: string) => {
  const C = BRAND.checklist;
  const starts = C.banners.map((b, i) => (caption ? cueFrame(b.cue, caption, duration) : 12 + i * 30));
  const hits = starts.map((f) => f + C.slamFrames);
  return {starts, hits};
};

/**
 * Ein Banner aus BRAND.checklist: knallt bei `start` in die Banner-Fläche (slamAt: von groß auf
 * 1, Blitz, Stauchen, Wackeln, Glanz) und wird bei `exitAt` (= Start des nächsten Banners) kleiner
 * nach oben weggeschoben. Mitte des sichtbaren Inhalts bei (cx, cy) in Stage-px.
 */
const ChecklistBanner: React.FC<{i: number; start: number; exitAt: number | null; cx: number; cy: number}> = ({
  i,
  start,
  exitAt,
  cx,
  cy,
}) => {
  const frame = useCurrentFrame();
  const C = BRAND.checklist;
  const b = C.banners[i];
  const img = BRAND.images[b.image];
  const f = frame - start;
  const g = exitAt === null ? -1 : frame - exitAt;
  if (f < 0 || g > C.exitFrames) return null;
  const s = b.width / img.box.w;
  const w = img.width * s;
  const h = img.height * s;
  const ox = (img.box.x + img.box.w / 2) * s;
  const oy = (img.box.y + img.box.h / 2) * s;
  const sl = slamAt(f, frame, {fromScale: C.fromScale, slamFrames: C.slamFrames, shake: C.shake, seed: `banner-${i}`});
  const tilt = (i % 2 ? 1 : -1) * 7;
  // weggeschoben: kleiner, nach oben, leicht gekippt, ausblenden
  const e = g < 0 ? 0 : interpolate(g, [0, C.exitFrames], [0, 1], {...clamp, easing: Easing.out(Easing.cubic)});
  // kurz halten, damit der Banner danach noch ≥ 0,4 s sauber lesbar steht
  const glitch = b.screen === 'glitch' ? interpolate(sl.hit, [0, 1, 5], [0, 0.6, 0], clamp) : 0;
  const glow = 14 + 6 * Math.sin(frame / 6) + 26 * sl.flash;
  // Glow-Puls auf der Nummern-Box (02 / 01) kurz nach dem Einschlag
  const pulse = b.pulse ? interpolate(sl.hit, [1, 5, 22], [0, 1, 0], clamp) : 0;
  const banner = (
    <div style={{position: 'relative', width: w, height: h}}>
      <BrandImg img={img} width={w} />
      {b.pulse ? (
        <BrightCopy
          img={img}
          width={w}
          opacity={pulse * 0.95}
          brightness={1.9}
          mask={`radial-gradient(ellipse ${(b.pulse.size[0] / 2 / img.width) * 100}% ${(b.pulse.size[1] / 2 / img.height) * 100}% at ${(b.pulse.center[0] / img.width) * 100}% ${(b.pulse.center[1] / img.height) * 100}%, #000 0%, #000 45%, transparent 100%)`}
        />
      ) : null}
      <Shine img={img} width={w} f={sl.hit - 2} frames={16} opacity={0.8} />
    </div>
  );
  return (
    <div
      style={{
        position: 'absolute',
        left: cx - ox,
        top: cy - oy,
        width: w,
        height: h,
        transformOrigin: `${ox}px ${oy}px`,
        transform: [
          `translate(${sl.sx}px, ${sl.sy - e * 170}px)`,
          `rotate(${(1 - sl.slam) * tilt + sl.sr + e * -tilt * 0.6}deg)`,
          `scale(${sl.scale * (1 + sl.squash) * (1 - 0.6 * e)}, ${sl.scale * (1 - sl.squash) * (1 - 0.6 * e)})`,
        ].join(' '),
        opacity: sl.opacity * (1 - e),
        filter: `brightness(${1 + 1.1 * sl.flash}) drop-shadow(0 0 ${glow}px ${COLORS.purple}99) drop-shadow(0 12px 18px rgba(0,0,0,0.55))`,
        zIndex: 2,
      }}
    >
      {glitch > 0.01 ? <Glitch amount={glitch}>{banner}</Glitch> : banner}
    </div>
  );
};

/** 4 kleine Fortschritts-Kästchen: leuchten auf, sobald ihr Banner einschlägt (aktuelles pulsiert);
 *  das nächste, noch leere Kästchen glimmt schon vorher (kein toter Moment vor dem ersten Banner). */
const ChecklistProgress: React.FC<{hits: number[]}> = ({hits}) => {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();
  const size = BRAND.checklist.progressSize;
  const gap = Math.round(size * 0.55);
  const appear = spring({frame: frame - 2, fps, config: {damping: 14, stiffness: 160}});
  let active = -1;
  hits.forEach((h, i) => {
    if (frame >= h) active = i;
  });
  return (
    <div
      style={{
        position: 'relative',
        display: 'flex',
        alignItems: 'center',
        gap,
        transform: `scale(${0.6 + 0.4 * appear})`,
        opacity: Math.min(1, appear * 1.5),
      }}
    >
      {/* Verbindungslinie, füllt sich mit */}
      <div
        style={{
          position: 'absolute',
          left: size / 2,
          right: size / 2,
          top: size / 2 - 3,
          height: 6,
          background: 'rgba(255,255,255,0.18)',
        }}
      />
      <div
        style={{
          position: 'absolute',
          left: size / 2,
          top: size / 2 - 3,
          height: 6,
          width:
            (size + gap) *
            Math.max(
              0,
              hits.reduce((n, h) => n + interpolate(frame - h, [0, 6], [0, 1], clamp), 0) - 1,
            ),
          background: `linear-gradient(90deg, ${COLORS.cyan}, ${COLORS.purple})`,
          boxShadow: `0 0 12px ${COLORS.cyan}`,
        }}
      />
      {hits.map((h, i) => {
        const done = frame >= h;
        const p = spring({frame: frame - h, fps, config: {damping: 9, stiffness: 200}});
        const isActive = i === active;
        const pulse = isActive ? 1 + 0.08 * Math.sin((frame - h) / 4) : 1;
        // nächstes offenes Kästchen: glimmt cyan und atmet leicht
        const wait = !done && i === active + 1 ? 0.5 - 0.5 * Math.cos(frame / 4) : -1;
        return (
          <div
            key={i}
            style={{
              position: 'relative',
              width: size,
              height: size,
              boxSizing: 'border-box',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              background: done
                ? `linear-gradient(180deg, rgba(46,242,255,0.45), rgba(91,26,168,0.85))`
                : 'rgba(10,5,30,0.7)',
              border: `4px solid ${done ? COLORS.cyan : wait >= 0 ? `rgba(46,242,255,${(0.55 + 0.45 * wait).toFixed(3)})` : 'rgba(255,255,255,0.5)'}`,
              boxShadow: done
                ? `0 0 ${isActive ? 22 : 10}px ${COLORS.cyan}, inset -4px -4px 0 rgba(0,0,0,0.3)`
                : wait >= 0
                  ? `0 0 ${(4 + 14 * wait).toFixed(2)}px ${COLORS.cyan}, inset -4px -4px 0 rgba(0,0,0,0.4)`
                  : 'inset -4px -4px 0 rgba(0,0,0,0.4)',
              transform: `scale(${(done ? 0.7 + 0.3 * p + (isActive ? 0.12 : 0) : wait >= 0 ? 1 + 0.07 * wait : 1) * pulse})`,
            }}
          >
            {done ? <PixelIcon name="check" size={size * 0.72 * Math.min(1, p)} glow="#3dff7a" /> : null}
          </div>
        );
      })}
    </div>
  );
};

/**
 * Segment 3: deine Banner (BRAND.checklist) als Folge – immer nur einer groß, jeweils beim
 * Stichwort; darunter klein der echte Screenshot zum aktuellen Punkt (Bossbar-Quest, Glitch bei
 * "Bugs", Prefix-Menü, Konto/Nova) und 4 Fortschritts-Kästchen. Positionen in Bild-px.
 */
export const ChecklistVisual: React.FC<VProps> = ({duration, stage, caption}) => {
  const frame = useCurrentFrame();
  const {fps, width} = useVideoConfig();
  const stageTop = stage?.top ?? 0;
  const stageH = stage?.height ?? 773;
  const Y = (abs: number) => abs - stageTop;
  const C = BRAND.checklist;
  const {starts, hits} = useChecklistTimes(duration, caption);

  // Jede Karte (außer 'glitch') kommt beim Einschlag ihres Banners, blendet aus, während der Banner
  // der nächsten Karte reinknallt (weg, bevor die neue Karte kommt), und sitzt screenGap unter der
  // Unterkante ihres Banners.
  const bannerBottom = (i: number) => {
    const b = C.banners[i];
    const img = BRAND.images[b.image];
    return C.centerY + (img.box.h * b.width) / img.box.w / 2;
  };
  const cards = C.banners
    .map((b, i) => ({screen: b.screen, start: starts[i], from: hits[i], top: bannerBottom(i) + C.screenGap}))
    .filter(
      (c): c is {screen: Exclude<ChecklistScreen, 'glitch'>; start: number; from: number; top: number} =>
        c.screen !== 'glitch',
    );
  const cardFade = Math.max(2, C.slamFrames);
  const glitches = C.banners.map((b, i) => (b.screen === 'glitch' ? hits[i] : null)).filter(
    (c): c is number => c !== null,
  );

  return (
    <div style={{position: 'relative', width, height: stageH, flexShrink: 0}}>
      {/* Echte Server-Screenshots zum aktuellen Punkt, klein unter dem Banner */}
      <div style={{position: 'absolute', left: 0, right: 0, top: 0}}>
        {cards.map((c, k) => {
          const to = k < cards.length - 1 ? cards[k + 1].start : duration + 30;
          if (frame < c.from || frame > to + cardFade) return null;
          const inP = spring({frame: frame - c.from, fps, config: {damping: 12, stiffness: 170, mass: 0.7}});
          const outP = interpolate(frame, [to, to + cardFade], [1, 0], clamp);
          const g = glitches.find((gf) => gf >= c.from && gf < to);
          // zwei kurze Glitch-Stöße, solange "BUGS FINDEN" steht
          const glitch =
            g === undefined
              ? 0
              : Math.max(
                  interpolate(frame - g, [0, 2, 12], [0, 1, 0], clamp),
                  interpolate(frame - g, [15, 17, 22], [0, 0.6, 0], clamp),
                );
          const Card = CARD[c.screen];
          return (
            <div
              key={k}
              style={{
                position: 'absolute',
                left: 0,
                right: 0,
                top: Y(c.top),
                display: 'flex',
                justifyContent: 'center',
                opacity: Math.min(1, inP * 1.6) * outP,
                transform: `translateY(${(1 - inP) * 50}px) scale(${C.screenScale * (0.82 + 0.18 * inP) * (0.92 + 0.08 * outP)})`,
                transformOrigin: 'top center',
              }}
            >
              <Glitch amount={glitch}>
                <Card />
              </Glitch>
            </div>
          );
        })}
      </div>

      {/* Fortschritt: 4 kleine Kästchen */}
      {C.progressTop !== null ? (
        <div style={{position: 'absolute', left: 0, right: 0, top: Y(C.progressTop), display: 'flex', justifyContent: 'center'}}>
          <ChecklistProgress hits={hits} />
        </div>
      ) : null}

      {/* Die Banner: immer nur einer groß, der vorige wird weggeschoben */}
      {C.banners.map((b, i) => (
        <ChecklistBanner
          key={b.image}
          i={i}
          start={starts[i]}
          exitAt={i < starts.length - 1 ? starts[i + 1] : null}
          cx={width / 2}
          cy={Y(C.centerY)}
        />
      ))}
    </div>
  );
};

/**
 * EXKLUSIV-Pill (eigene Grafik): knallt von groß auf 1 rein, Blitz + Wackeln + Druckwelle
 * beim Einschlag, danach leicht pulsierender Glow. f = Frames seit dem Stichwort.
 */
const ExklusivPill: React.FC<{f: number}> = ({f}) => {
  const frame = useCurrentFrame();
  const P = BRAND.exklusiv;
  const img = BRAND.images.exklusiv;
  if (f < 0) return null;
  const w = P.width;
  const h = brandHeight(img, w);
  const {slam, hit, scale, squash, sx, sy, sr, flash} = slamAt(f, frame, {
    fromScale: P.fromScale,
    slamFrames: P.slamFrames,
    shake: P.shake,
    seed: 'pill',
  });
  const wave = interpolate(hit, [0, 13], [0, 1], clamp);
  const glow = 16 + 7 * Math.sin(frame / 6);
  return (
    <div
      style={{
        position: 'absolute',
        left: 0,
        top: 0,
        width: w,
        height: h,
        // Drehpunkt unten mittig: die große Pill wächst nach oben (übers Menü), nie über die Caption
        transformOrigin: '50% 100%',
        transform: `translate(${sx}px, ${sy}px) rotate(${P.rotate + (1 - slam) * -9 + sr}deg) scale(${scale * (1 + squash)}, ${scale * (1 - squash)})`,
        opacity: interpolate(f, [0, 2], [0, 1], clamp),
      }}
    >
      {/* Druckwelle (Pill-Umriss wächst und verblasst) */}
      {wave > 0 && wave < 1 ? (
        <div
          style={{
            position: 'absolute',
            inset: 0,
            borderRadius: h,
            border: `${8 * (1 - wave) + 2}px solid ${COLORS.cyan}`,
            boxShadow: `0 0 34px ${COLORS.cyan}, inset 0 0 22px ${COLORS.purple}`,
            transform: `scale(${1 + 0.45 * wave}, ${1 + 1.1 * wave})`,
            opacity: 1 - wave,
          }}
        />
      ) : null}
      <div
        style={{
          position: 'relative',
          filter: `brightness(${1 + 1.3 * flash}) drop-shadow(0 0 ${glow}px ${COLORS.cyan}cc) drop-shadow(0 0 ${glow * 2}px ${COLORS.purple}88) drop-shadow(0 12px 18px rgba(0,0,0,0.6))`,
        }}
      >
        <BrandImg img={img} width={w} />
        <BrightCopy
          img={img}
          width={w}
          opacity={hit >= 3 && hit <= 20 ? 0.8 : 0}
          mask={shineMask(interpolate(hit, [3, 20], [-15, 118], clamp), 8, 110)}
          brightness={1.9}
        />
      </div>
    </div>
  );
};

/* ---------- 4: Prefix – echtes Menü "Prefix wählen" (Kontext) + echte Chatzeile (Hauptsache) ---------- */
export const PrefixVisual: React.FC<VProps> = ({duration, caption}) => {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();
  const menuP = spring({frame: frame - sec(PREFIX.menuAt), fps, config: {damping: 12, stiffness: 150, mass: 0.8}});
  const chatP = spring({frame: frame - sec(PREFIX.chatAt), fps, config: {damping: 14, stiffness: 140}});
  // Einschieben der Chatzeile OHNE Überschwingen: sonst ragt sie kurz in die rechte
  // TikTok-Leiste und rutscht unter der (stehenden) EXKLUSIV-Pill weg
  const chatX = spring({
    frame: frame - sec(PREFIX.chatAt),
    fps,
    config: {damping: 18, stiffness: 140, overshootClamping: true},
  });
  const giftP = spring({frame: frame - sec(PREFIX.rewardAt), fps, config: {damping: 8, stiffness: 170}});
  const menuScale = 0.62;
  // Chat-Schrift im Screenshot: 1 MC-Pixel = 4 px -> bei 1,75x genau 7 px (gleichmäßige Pixel)
  const chatScale = 1.75;
  const line = SCREEN_REGIONS.chatRankLine;
  const r = SCREEN_REGIONS.chatPrefix;
  const shineT = (frame - sec(PREFIX.chatAt) - 8) % 40;
  const shine = interpolate(shineT, [0, 22], [-40, 140], clamp);
  const bump = 0.05 * Math.max(0, Math.sin(Math.min(Math.PI, ((frame - sec(PREFIX.chatAt) - 6) / 12) * Math.PI)));
  // EXKLUSIV-Pill (eigene Grafik) beim Wort "exklusiven", direkt über der Chatzeile
  const P = BRAND.exklusiv;
  const pillAt = caption ? cueFrame(P.cue, caption, duration) : sec(1.2);
  const pillW = P.width;
  const pillH = brandHeight(BRAND.images.exklusiv, pillW);
  const chatW = line.w * chatScale;
  return (
    <div style={{position: 'relative', display: 'flex', flexDirection: 'column', alignItems: 'center', marginBottom: 22}}>
      {/* Menü als Kontext: kleiner, tritt zurück, sobald die Chatzeile kommt */}
      <div
        style={{
          marginBottom: P.gapAbove,
          transform: `translateY(${(1 - menuP) * 80}px) scale(${(0.55 + 0.45 * menuP) * (1 - 0.06 * chatP)}) rotate(${(1 - menuP) * -5}deg)`,
          transformOrigin: 'bottom center',
          opacity: Math.min(1, menuP * 1.6),
          filter: `${neon(COLORS.purple, 1.2)} brightness(${1 - 0.32 * chatP}) saturate(${1 - 0.3 * chatP})`,
        }}
      >
        <PrefixMenu scale={menuScale} />
      </div>

      {/* Platz für die EXKLUSIV-Pill, direkt über der Chatzeile (Abstand P.gap) */}
      <div style={{position: 'relative', width: chatW, height: pillH + P.gap, transform: 'translateX(-20px)', zIndex: 2}}>
        <div style={{position: 'absolute', left: chatW * P.anchorX - pillW / 2, top: 0}}>
          <ExklusivPill f={frame - pillAt} />
        </div>
      </div>

      {/* Geschenk für "weitere Belohnungen" */}
      <div
        style={{
          position: 'absolute',
          right: 40,
          top: 10,
          transform: `scale(${giftP}) translateY(${Math.sin(frame / 7) * 8}px) rotate(${Math.sin(frame / 11) * 7}deg)`,
        }}
      >
        <PixelIcon name="gift" size={150} glow={COLORS.gold} />
      </div>

      {/* Echte Chatzeile, groß: "[Beta Tester] Inhaber" (leicht nach links, weg von der TikTok-Leiste).
          Liegt ÜBER der Pill, damit deren Schein/Schatten den cyanfarbenen Rahmen nicht anknabbert. */}
      <div
        style={{
          position: 'relative',
          zIndex: 3,
          transform: `translateX(${(1 - chatX) * -900 - 20}px) scale(${1 + bump})`,
          // Pop beim Ankommen wächst nach links – die rechte Kante bleibt aus der TikTok-Leiste
          transformOrigin: '100% 50%',
          opacity: Math.min(1, chatP * 2),
          boxShadow: `0 0 34px ${COLORS.cyan}aa, 0 0 0 5px ${COLORS.cyan}cc, 0 14px 30px rgba(0,0,0,0.55)`,
        }}
      >
        <Screen img={SCREENS.chatBetaTester} region={line} scale={chatScale}>
          <div
            style={{
              position: 'absolute',
              top: 0,
              bottom: 0,
              left: (r.x - line.x) * chatScale,
              width: r.w * chatScale,
              overflow: 'hidden',
              mixBlendMode: 'screen',
            }}
          >
            <div
              style={{
                position: 'absolute',
                top: 0,
                bottom: 0,
                left: `${shine}%`,
                width: '28%',
                background: 'linear-gradient(90deg, transparent, rgba(255,255,255,0.6), transparent)',
                transform: 'skewX(-20deg)',
              }}
            />
          </div>
        </Screen>
      </div>
    </div>
  );
};

/* ---------- 5: 20 Plätze – echte Scoreboard-Zeile "Online › 1/20" + 20 freie Slots ----------
 * Die Slots bleiben "frei" (leere, leuchtende Rahmen mit "+") und werden beim Alarm nicht rot –
 * sonst sähe es nach "schon voll / ausgebucht" aus. Rot werden nur Rahmen + Sticker. */
export const SlotsVisual: React.FC<VProps> = () => {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();
  const panel = usePop(0, 13);
  const fillFrom = 8;
  const fillTo = sec(SLOTS.filledAt);
  const shown = Math.min(
    SLOTS.total,
    Math.floor(interpolate(frame, [fillFrom, fillTo], [0, SLOTS.total + 0.999], clamp)),
  );
  const full = frame >= fillTo;
  const alarmAt = sec(SLOTS.alarmAt);
  const alarm = frame >= alarmAt;
  const blink = alarm && Math.floor((frame - alarmAt) / 5) % 2 === 0;
  const accent = alarm ? '#ff3355' : COLORS.cyan;
  const shake = alarm ? Math.sin(frame * 2.3) * 6 * interpolate(frame - alarmAt, [0, 20], [1, 0.35], clamp) : 0;
  const countPop = spring({frame: frame - fillTo, fps, config: {damping: 8, stiffness: 200}});
  const ring = spring({frame: frame - fillTo + 2, fps, config: {damping: 12}});
  const tick = shown > 0 ? spring({frame: frame - (fillFrom + ((shown - 1) / SLOTS.total) * (fillTo - fillFrom)), fps, config: {damping: 10}}) : 0;
  const alarmPop = alarm ? spring({frame: frame - alarmAt, fps, config: {damping: 9, stiffness: 200}}) : 0;
  const onlineScale = 2;
  return (
    <Glass
      accent={accent}
      style={{
        position: 'relative',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 16,
        padding: '22px 26px 26px',
        transform: `translateY(${(1 - panel) * -50}px) translateX(${shake}px)`,
        opacity: Math.min(1, panel * 1.4),
      }}
    >
      {/* echte Scoreboard-Zeile */}
      <div style={{position: 'relative', boxShadow: `0 0 0 4px rgba(255,255,255,0.12)`}}>
        <Screen img={SCREENS.scoreboardOnline} scale={onlineScale} />
        <Highlight
          region={SCREEN_REGIONS.onlineMax}
          scale={onlineScale}
          color={COLORS.cyan}
          progress={ring}
          pulse={frame}
          pad={0}
        />
      </div>

      <div style={{display: 'flex', alignItems: 'center', gap: 28}}>
        <div
          style={{
            fontFamily: FONT_HEAVY,
            fontWeight: 900,
            fontSize: 128,
            lineHeight: 1,
            color: COLORS.white,
            transform: `scale(${full ? 1 + 0.18 * (1 - countPop) + (blink ? 0.06 : 0) : 0.92 + 0.08 * tick})`,
            textShadow: `0 0 34px ${accent}, 8px 8px 0 ${COLORS.purpleDeep}`,
            fontVariantNumeric: 'tabular-nums',
            minWidth: 170,
            textAlign: 'right',
          }}
        >
          {shown}
        </div>
        <div
          style={{
            fontFamily: FONT_PIXEL,
            fontWeight: 700,
            fontSize: 48,
            letterSpacing: 4,
            color: COLORS.cyan,
            textShadow: '4px 4px 0 rgba(0,0,0,0.6)',
            whiteSpace: 'nowrap',
          }}
        >
          {SLOTS.label}
        </div>
      </div>

      {/* "Schnell sein!"-Sticker bei "wer zuerst kommt" */}
      <div
        style={{
          position: 'absolute',
          right: 24,
          top: '100%',
          marginTop: -26,
          padding: '8px 18px',
          background: 'rgba(30,4,16,0.92)',
          border: '5px solid #ff3355',
          boxShadow: '0 0 28px #ff3355',
          fontFamily: FONT_HEAVY,
          fontWeight: 900,
          fontSize: 40,
          color: '#ff4466',
          textShadow: '0 0 14px #ff3355, 0 3px 0 rgba(0,0,0,0.6)',
          whiteSpace: 'nowrap',
          transform: `scale(${alarmPop}) rotate(${7 + Math.sin(frame / 4) * 2}deg)`,
        }}
      >
        {SLOTS.alarmLabel}
      </div>

      {/* 20 Slots wie zwei Hotbars */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(10, 66px)',
          gap: 6,
          padding: 10,
          background: 'rgba(198,198,198,0.88)',
          border: '5px solid #fff',
          boxShadow: `inset -6px -6px 0 #555, 0 0 26px ${COLORS.purple}`,
        }}
      >
        {new Array(SLOTS.total).fill(0).map((_, i) => {
          const on = i < shown;
          const p = on
            ? spring({frame: frame - (fillFrom + (i / SLOTS.total) * (fillTo - fillFrom)), fps, config: {damping: 10, stiffness: 220}})
            : 0;
          // "Wer zuerst kommt": eine Leuchtwelle läuft über die (weiterhin freien) Slots
          const wave = alarm ? Math.max(0, Math.sin((frame - alarmAt) / 2.2 - (i % 10) * 0.55 - Math.floor(i / 10) * 0.8)) : 0;
          const c = COLORS.cyan;
          return (
            <div
              key={i}
              style={{
                width: 66,
                height: 60,
                background: '#8b8b8b',
                boxShadow: 'inset 5px 5px 0 #373737, inset -5px -5px 0 #fff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              {/* freier Platz: dunkler Rahmen mit leuchtendem Rand und "+" */}
              <div
                style={{
                  position: 'relative',
                  width: 42,
                  height: 38,
                  boxSizing: 'border-box',
                  background: 'rgba(6,16,34,0.9)',
                  border: `4px solid ${c}`,
                  opacity: on ? 1 : 0,
                  transform: `scale(${p * (1 + 0.1 * wave)})`,
                  boxShadow: `0 0 ${10 + 14 * wave}px ${c}, inset 0 0 8px ${c}66`,
                }}
              >
                <div style={{position: 'absolute', left: '50%', top: '50%', width: 18, height: 5, marginLeft: -9, marginTop: -2.5, background: c}} />
                <div style={{position: 'absolute', left: '50%', top: '50%', width: 5, height: 18, marginLeft: -2.5, marginTop: -9, background: c}} />
              </div>
            </div>
          );
        })}
      </div>
    </Glass>
  );
};

/* ---------- 6: Discord – der echte Bewerbungsweg ----------
 * Ingame-Banner "/dc" -> Kanal "# tickets" mit dem Embed-Abschnitt "Betatester werden" ->
 * Cursor klickt "Jetzt bewerben" -> das echte Formular "Betatester" ploppt auf (Hintergrund
 * abgedunkelt wie bei einem Discord-Popup, Caption bleibt darüber).
 * Discord-Screenshots sind kein Pixel-Art: sie werden weich skaliert (SCREENS.discord*.smooth).
 * Alle Positionen in Bild-Pixeln (1080×1920); stage.top = Oberkante des Visual-Bereichs. */
const BLURPLE = '#5865F2';
/** Embed-Farbleiste links (aus dem Screenshot) */
const EMBED_BAR = '#9c59ff';
/** Hintergründe aus den Screenshots: Embed (10,10,12), Kanal-Kopfzeile (0,0,0) */
const DC_EMBED_BG = '#0a0a0c';
const DC_HEADER_BG = '#000000';
/** Formular-Feld-Hintergrund (für den blinkenden Text-Cursor) */
const DC_FIELD_BG = 'rgb(9,9,11)';

// Discord-"Fenster" (Kanal-Kopfzeile + Embed-Abschnitt), etwas links der Mitte,
// damit alles sicher außerhalb der rechten TikTok-Leiste (x > 940) liegt.
// Zwei Zeilen statt Original-Layout (Button rechts neben dem Text wäre am Handy zu klein):
//   Zeile 1: Überschrift + Beschreibung (SCREEN_REGIONS.discordApplyText)
//   Zeile 2: der echte Button "Jetzt bewerben", groß – wird geklickt
const DC_PANEL_LEFT = 30;
const DC_PANEL_BOTTOM = 1079; // Unterkante (Caption beginnt bei ~1110)
const DC_BORDER = 4;
const DC_CHANNEL_SCALE = 2;
const DC_TEXT_SCALE = 2.08; // Zeile 1: 404 px -> 840 px breit
const DC_BUTTON_SCALE = 2.6; // Zeile 2: 142×32 -> 369×83 px
const DC_ROW_GAP = 16;
const DC_HEADER_PAD = {x: 22, y: 14};
const DC_BODY_PAD = {top: 22, right: 16, bottom: 24, left: 20};
const DC_BAR_W = 6;
const DC_BAR_GAP = 14;
const DC_BANNER_SCALE = 1.45;
const DC_CURSOR = 100; // Größe des Maus-Cursors (px)

export const DiscordVisual: React.FC<VProps> = ({stage}) => {
  const frame = useCurrentFrame();
  const {fps, width, height} = useVideoConfig();
  const stageTop = stage?.top ?? 0;
  const stageH = stage?.height ?? height;
  /** Bild-y -> y im Visual-Bereich */
  const Y = (abs: number) => abs - stageTop;

  // ---- Layout (abgeleitet) ----
  const ch = SCREENS.discordChannel;
  const tx = SCREEN_REGIONS.discordApplyText;
  const bt = SCREENS.discordApplyButton;
  const headerH = ch.height * DC_CHANNEL_SCALE + DC_HEADER_PAD.y * 2;
  const textW = tx.w * DC_TEXT_SCALE;
  const textH = tx.h * DC_TEXT_SCALE;
  const btnW = bt.width * DC_BUTTON_SCALE;
  const btnH = bt.height * DC_BUTTON_SCALE;
  // Button bündig mit dem Text darüber (Text beginnt im PNG bei x = 12)
  const btnIndent = (12 - tx.x) * DC_TEXT_SCALE;
  const bodyH = DC_BODY_PAD.top + textH + DC_ROW_GAP + btnH + DC_BODY_PAD.bottom;
  const panelW = DC_BORDER * 2 + DC_BODY_PAD.left + DC_BAR_W + DC_BAR_GAP + textW + DC_BODY_PAD.right;
  const panelH = DC_BORDER * 2 + headerH + 2 + bodyH;
  const panelTop = DC_PANEL_BOTTOM - panelH;
  const panelCenterX = DC_PANEL_LEFT + panelW / 2;
  // Button "Jetzt bewerben" in Bild-Koordinaten
  const contentLeft = DC_PANEL_LEFT + DC_BORDER + DC_BODY_PAD.left + DC_BAR_W + DC_BAR_GAP;
  const contentTop = panelTop + DC_BORDER + headerH + 2 + DC_BODY_PAD.top;
  const btnX = contentLeft + btnIndent;
  const btnY = contentTop + textH + DC_ROW_GAP;
  // Banner "/dc" über dem Fenster
  const bn = SCREENS.discordBanner;
  const bannerW = bn.width * DC_BANNER_SCALE;
  const bannerH = bn.height * DC_BANNER_SCALE;
  const bannerTop = panelTop - 34 - bannerH;
  // Formular: mittig im Bild (wie Logo, Caption und End-Card)
  const fm = SCREENS.discordForm;
  const formW = fm.width * DISCORD.formScale;
  const formLeft = width / 2 - formW / 2;

  // ---- Timing ----
  const bannerP = usePop(0, 11);
  const panelP = spring({frame: frame - 4, fps, config: {damping: 14, stiffness: 150, mass: 0.8}});
  const hl = spring({frame: frame - sec(DISCORD.highlightAt), fps, config: {damping: 11}});
  const clickAt = sec(DISCORD.clickAt);
  const formAt = sec(DISCORD.formAt);
  const press = interpolate(frame - clickAt, [0, 2, 7], [0, 1, 0], clamp);
  const flash = interpolate(frame - clickAt, [0, 2, 14], [0, 1, 0], clamp);
  const ripple = frame >= clickAt ? interpolate(frame - clickAt, [0, 16], [0, 1], clamp) : 0;
  // Button lockt vor dem Klick mit einem pulsierenden Glow
  const lure = interpolate(frame, [12, 22], [0, 1], clamp) * (frame < clickAt ? 0.6 + 0.4 * Math.sin(frame / 3) : 0);
  const formP = spring({frame: frame - formAt, fps, config: {damping: 14, stiffness: 190, mass: 0.75}});
  const dim = interpolate(frame - formAt, [0, 6], [0, 1], clamp);
  const submit = spring({frame: frame - sec(DISCORD.submitHintAt), fps, config: {damping: 12}});
  const caretOff = frame >= formAt && Math.floor((frame - formAt) / 14) % 2 === 1;
  // Popup geht zu: Formular schrumpft + blendet aus, Abdunklung und Discord-Fenster blenden aus
  // -> die letzten Frames vor dem End-Card-Blitz zeigen den Flug ins lila Portal.
  const close = interpolate(frame - sec(DISCORD.closeAt), [0, 7], [0, 1], {...clamp, easing: Easing.in(Easing.quad)});
  const open = 1 - close;

  // Cursor (Spitze = linke obere Ecke des Icons) kommt von rechts durch die leere Button-Zeile
  // (nicht über Text oder Caption), klickt unten rechts auf den Button (Beschriftung bleibt
  // lesbar) und verschwindet mit dem Popup.
  const tipX = btnX + btnW * 0.92;
  const tipY = btnY + btnH * 0.72;
  const cursorX = interpolate(frame, [8, clickAt], [width + 40, tipX], {...clamp, easing: Easing.out(Easing.cubic)});
  const cursorY = interpolate(frame, [8, clickAt], [tipY - 24, tipY], {...clamp, easing: Easing.inOut(Easing.quad)});
  const cursorScale = 1 - 0.12 * press;
  const rippleR = 110;

  const glow = 1.15 + Math.sin(frame / 5) * 0.3;

  return (
    <div style={{position: 'relative', width, height: stageH, flexShrink: 0}}>
      {/* Ingame-Banner "DISCORD /dc" */}
      <div
        style={{
          position: 'absolute',
          left: panelCenterX - bannerW / 2,
          top: Y(bannerTop),
          transform: `scale(${0.5 + 0.5 * bannerP}) translateY(${(1 - bannerP) * 60}px)`,
          opacity: Math.min(1, bannerP * 1.5) * open,
          filter: neon(BLURPLE, glow),
        }}
      >
        <Screen img={bn} scale={DC_BANNER_SCALE} outline={SCREEN_OUTLINES.discordBanner} />
        <Highlight
          region={SCREEN_REGIONS.discordCommand}
          scale={DC_BANNER_SCALE}
          color={COLORS.cyan}
          progress={hl}
          pulse={frame}
          pad={9}
        />
      </div>

      {/* Discord-"Fenster": Kanal "# tickets" + Embed-Abschnitt "Betatester werden" + Button */}
      <div
        style={{
          position: 'absolute',
          left: DC_PANEL_LEFT,
          top: Y(panelTop),
          width: panelW,
          height: panelH,
          transform: `translateY(${(1 - panelP) * 80}px) scale(${0.9 + 0.1 * panelP})`,
          opacity: Math.min(1, panelP * 1.6) * open,
        }}
      >
        <div
          style={{
            width: panelW,
            height: panelH,
            boxSizing: 'border-box',
            border: `${DC_BORDER}px solid ${BLURPLE}b3`,
            borderRadius: 14,
            overflow: 'hidden',
            background: DC_EMBED_BG,
            boxShadow: `0 0 26px ${BLURPLE}88, 0 0 60px ${COLORS.purple}44, 0 14px 40px rgba(0,0,0,0.55)`,
          }}
        >
          <div
            style={{
              height: headerH,
              boxSizing: 'border-box',
              padding: `${DC_HEADER_PAD.y}px ${DC_HEADER_PAD.x}px`,
              background: DC_HEADER_BG,
              borderBottom: '2px solid rgba(255,255,255,0.08)',
            }}
          >
            <Screen img={ch} scale={DC_CHANNEL_SCALE} />
          </div>
          <div
            style={{
              display: 'flex',
              alignItems: 'stretch',
              gap: DC_BAR_GAP,
              padding: `${DC_BODY_PAD.top}px ${DC_BODY_PAD.right}px ${DC_BODY_PAD.bottom}px ${DC_BODY_PAD.left}px`,
            }}
          >
            <div style={{width: DC_BAR_W, borderRadius: 3, background: EMBED_BAR, flexShrink: 0}} />
            <Screen img={SCREENS.discordApply} region={tx} scale={DC_TEXT_SCALE} />
          </div>
        </div>
        {/* Button "Jetzt bewerben" (echter Ausschnitt), groß als eigene Zeile: Glow, Klick, Blitz */}
        <div
          style={{
            position: 'absolute',
            left: btnX - DC_PANEL_LEFT,
            top: btnY - panelTop,
            transform: `scale(${1 - 0.08 * press})`,
            filter: `brightness(${1 + 0.7 * flash}) drop-shadow(0 0 ${8 + 26 * lure + 36 * flash}px rgba(61,255,122,${0.3 + 0.5 * lure + 0.6 * flash}))`,
          }}
        >
          <Screen img={bt} scale={DC_BUTTON_SCALE} />
        </div>
      </div>

      {/* Klick-Welle */}
      {ripple > 0 && ripple < 1 ? (
        <div
          style={{
            position: 'absolute',
            left: tipX - rippleR * ripple,
            top: Y(tipY) - rippleR * ripple,
            width: 2 * rippleR * ripple,
            height: 2 * rippleR * ripple,
            borderRadius: '50%',
            border: `7px solid rgba(255,255,255,${0.9 * (1 - ripple)})`,
            boxShadow: `0 0 24px rgba(61,255,122,${0.9 * (1 - ripple)}), inset 0 0 18px rgba(61,255,122,${0.6 * (1 - ripple)})`,
          }}
        />
      ) : null}
      {/* Maus-Cursor */}
      <div
        style={{
          position: 'absolute',
          left: cursorX,
          top: Y(cursorY),
          transform: `scale(${cursorScale})`,
          transformOrigin: '0 0',
          opacity: 1 - dim,
          filter: 'drop-shadow(0 6px 10px rgba(0,0,0,0.6))',
        }}
      >
        <PixelIcon name="cursor" size={DC_CURSOR} />
      </div>

      {/* Popup: Abdunklung (ganzes Bild, auch Logo – nur die Caption bleibt darüber) */}
      {dim * open > 0 ? (
        <div
          style={{
            position: 'absolute',
            left: -400,
            top: Y(0) - 400,
            width: width + 800,
            height: height + 800,
            background: `rgba(0,0,0,${DISCORD.formDim * dim * open})`,
          }}
        />
      ) : null}
      {/* Echtes Formular "Betatester" */}
      {frame >= formAt && open > 0 ? (
        <div
          style={{
            position: 'absolute',
            left: formLeft,
            top: Y(DISCORD.formTop),
            transform: `translateY(${(1 - formP) * 40}px) scale(${(0.84 + 0.16 * formP) * (1 - 0.1 * close)})`,
            transformOrigin: '50% 40%',
            opacity: Math.min(1, formP * 2.2) * open,
            filter: `drop-shadow(0 0 22px ${BLURPLE}99) drop-shadow(0 0 50px ${COLORS.purple}55) drop-shadow(0 18px 40px rgba(0,0,0,0.7))`,
          }}
        >
          <Screen img={fm} scale={DISCORD.formScale}>
            {/* blinkender Text-Cursor im Feld "Minecraft-Name" */}
            {caretOff ? (
              <div
                style={{
                  position: 'absolute',
                  left: SCREEN_REGIONS.discordFormCaret.x * DISCORD.formScale,
                  top: SCREEN_REGIONS.discordFormCaret.y * DISCORD.formScale,
                  width: SCREEN_REGIONS.discordFormCaret.w * DISCORD.formScale,
                  height: SCREEN_REGIONS.discordFormCaret.h * DISCORD.formScale,
                  background: DC_FIELD_BG,
                }}
              />
            ) : null}
          </Screen>
          <Highlight
            region={SCREEN_REGIONS.discordFormSubmit}
            scale={DISCORD.formScale}
            color={COLORS.cyan}
            progress={submit}
            pulse={frame}
            pad={6}
          />
        </div>
      ) : null}
    </div>
  );
};
