import React, {useMemo} from 'react';
import {
  AbsoluteFill,
  Audio,
  Sequence,
  interpolate,
  random,
  spring,
  staticFile,
  useCurrentFrame,
  useVideoConfig,
} from 'remotion';
import {Background, SpaceParticles} from './Background';
import {BrandImg, BrightCopy, Impact, RocketSprite, RocketWipe, shakeAt, shineMask} from './Brand';
import {CutFlash, FootageGrade, FootageShot} from './Footage';
import {
  BRAND,
  BrandImage,
  COLORS,
  DISCORD,
  SHOW_CAPTIONS,
  END_CARD,
  END_CARD_CLIP,
  END_CARD_CLIP_BLUR,
  END_CARD_CLIP_DIM,
  END_CARD_FOOTER,
  END_CARD_LINKS,
  END_CARD_STEPS,
  FONT_HEAVY,
  FONT_PIXEL,
  FPS,
  MAIN_SECONDS,
  MUSIC,
  PUNCHES,
  SCREENS,
  SEGMENTS,
  SFX,
  Segment,
  SegmentVisual,
  TOTAL_FRAMES,
  USE_MUSIC,
  USE_SFX,
  USE_VOICEOVER,
  VOICE_SPEECH,
  cutSeconds,
  INTRO,
  SLOTS,
  VOICEOVER_FILE,
  VOICEOVER_VOLUME,
  sec,
} from './config';
import {KineticCaption, StaticRich, captionRevealFrames, cueFrame} from './RichText';
import {PixelIcon} from './PixelIcon';
import {Screen} from './Screen';
import {
  ChecklistVisual,
  DiscordVisual,
  IntroVisual,
  introGlitchAt,
  PrefixVisual,
  rewardHits,
  SlotsVisual,
  TestersVisual,
} from './Visuals';

// Layout (1080x1920): Header (echtes Logo) oben ab 140px, Visual-Bereich 300–1085,
// Caption 1110–1540. Unterste ~380px und rechte ~140px bleiben frei (TikTok/Reels-UI:
// Beschreibung unten, Like/Kommentar-Leiste rechts). Dahinter läuft Vollbild-Gameplay.
const SAFE_TOP = 140;
const VISUAL_TOP = 312;
const VISUAL_HEIGHT = 773;
const CAPTION_TOP = 1110;
const CAPTION_HEIGHT = 430;
const CAPTION_LEFT = 80;
const CAPTION_RIGHT = 140;

/** Kompakte Visuals sitzen oben (unter dem Logo) oder unten (direkt über der Caption),
 *  damit das Gameplay in der Bildmitte frei bleibt. */
const VISUAL_ALIGN: Record<SegmentVisual, 'top' | 'bottom'> = {
  intro: 'top',
  testers: 'bottom',
  checklist: 'top',
  prefix: 'bottom',
  slots: 'top',
  discord: 'bottom',
};

const VISUALS = {
  intro: IntroVisual,
  testers: TestersVisual,
  checklist: ChecklistVisual,
  prefix: PrefixVisual,
  slots: SlotsVisual,
  discord: DiscordVisual,
};

/** Echtes GALACTICFY-Banner aus dem Scoreboard + "BETA"-Sticker. */
const Header: React.FC<{scale?: number}> = ({scale = 1}) => {
  const frame = useCurrentFrame();
  const glow = 14 + Math.sin(frame / 8) * 6;
  return (
    <div style={{position: 'relative'}}>
      <div
        style={{
          filter: `drop-shadow(0 0 ${glow * scale}px ${COLORS.purple}) drop-shadow(0 8px 18px rgba(0,0,0,0.6))`,
        }}
      >
        <Screen img={SCREENS.logo} scale={scale} />
      </div>
      <div
        style={{
          position: 'absolute',
          right: -30 * scale,
          bottom: -12 * scale,
          fontFamily: FONT_PIXEL,
          fontWeight: 700,
          fontSize: 30 * scale,
          color: COLORS.bg,
          background: COLORS.cyan,
          padding: `${5 * scale}px ${11 * scale}px`,
          boxShadow: `0 0 ${20 * scale}px ${COLORS.cyan}, inset -${5 * scale}px -${5 * scale}px 0 rgba(0,0,0,0.3)`,
          transform: 'rotate(-6deg)',
        }}
      >
        {BRAND.tag}
      </div>
    </div>
  );
};

const SegmentScene: React.FC<{seg: Segment; first?: boolean}> = ({seg, first}) => {
  const frame = useCurrentFrame();
  const duration = sec(seg.to - seg.from);
  const Visual = VISUALS[seg.visual];
  // erstes Segment: schon in Frame 0 voll da (Hook – kein Einblenden)
  const fadeIn = first ? 1 : interpolate(frame, [0, 4], [0.3, 1], {extrapolateRight: 'clamp'});
  // kurzes Ausblenden (3 Frames) – der Blitz am Schnitt übernimmt den Rest; so stehen die letzten
  // Grafiken eines Segments (z. B. JAVA & BEDROCK, KOSTENLOS!) länger voll da
  const fadeOut = interpolate(frame, [duration - 3, duration], [1, 0.1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });
  const out = 1 - fadeOut;
  return (
    <AbsoluteFill style={{opacity: Math.min(fadeIn, fadeOut)}}>
      <div
        style={{
          position: 'absolute',
          top: VISUAL_TOP,
          left: 0,
          right: 0,
          height: VISUAL_HEIGHT,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: VISUAL_ALIGN[seg.visual] === 'top' ? 'flex-start' : 'flex-end',
          transform: `scale(${1 - out * 0.12}) translateY(${-out * 40}px)`,
        }}
      >
        <Visual duration={duration} stage={{top: VISUAL_TOP, height: VISUAL_HEIGHT}} caption={seg.text} />
      </div>
      <div
        style={{
          position: 'absolute',
          top: CAPTION_TOP,
          left: CAPTION_LEFT,
          right: CAPTION_RIGHT,
          height: CAPTION_HEIGHT,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          transform: `translateY(${out * 30}px)`,
        }}
      >
        {SHOW_CAPTIONS ? (
          <KineticCaption
            text={seg.text}
            fontSize={seg.fontSize ?? 76}
            revealFrames={captionRevealFrames(duration)}
          />
        ) : null}
      </div>
    </AbsoluteFill>
  );
};

const endRowStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: 24,
  padding: '22px 24px',
  background: 'rgba(10,5,30,0.74)',
};
const endRowText: React.CSSProperties = {
  fontFamily: FONT_HEAVY,
  fontWeight: 900,
  fontSize: 46,
  lineHeight: 1.15,
  color: COLORS.white,
  textShadow: '0 4px 0 rgba(0,0,0,0.6)',
};

/** Ein Schritt der Bewerbungs-Zeile: Text, Discord-Kanal ("#tickets") oder grüner Discord-Button. */
const EndCardStep: React.FC<{text: string; look: 'text' | 'channel' | 'button'}> = ({text, look}) => {
  if (look === 'text') return <StaticRich text={text} style={{...endRowText, whiteSpace: 'nowrap'}} />;
  const chip: React.CSSProperties = {
    fontFamily: FONT_HEAVY,
    fontWeight: 900,
    fontSize: 40,
    lineHeight: 1.1,
    padding: '7px 18px 9px',
    borderRadius: 10,
    whiteSpace: 'nowrap',
    color: '#fff',
  };
  if (look === 'channel') {
    const hash = text.startsWith('#');
    return (
      <div
        style={{
          ...chip,
          background: '#111214',
          border: '3px solid rgba(255,255,255,0.22)',
          color: '#e6e7ea',
          boxShadow: '0 6px 14px rgba(0,0,0,0.5)',
        }}
      >
        {hash ? <span style={{color: '#8a8e96', marginRight: 4}}>#</span> : null}
        {hash ? text.slice(1) : text}
      </div>
    );
  }
  // Farbe wie der echte "Jetzt bewerben"-Button (SCREENS.discordApplyButton)
  return (
    <div
      style={{
        ...chip,
        background: '#006c37',
        border: '3px solid #23a55a',
        boxShadow: '0 0 20px rgba(61,255,122,0.45), 0 6px 14px rgba(0,0,0,0.5)',
        textShadow: '0 2px 0 rgba(0,0,0,0.35)',
      }}
    >
      {text}
    </div>
  );
};

/**
 * End-Card-Zeile aus einer eigenen Grafik (sichtbare Breite = visibleWidth, z. B. Zeilenbreite):
 * Leiste "20 TESTER GESUCHT" bzw. Banner "JAVA & BEDROCK". Gleitet rein, Glanz bei shineAt.
 */
const EndCardImageRow: React.FC<{
  img: BrandImage;
  visibleWidth: number;
  shineAt: number;
  progress: number;
  pulse?: boolean;
  /** Glanz + Puls wiederholen sich alle N Frames (ab shineAt) – hält die End-Card lebendig */
  loopEvery?: number;
}> = ({img, visibleWidth, shineAt, progress, pulse, loopEvery}) => {
  const frame = useCurrentFrame();
  const s = visibleWidth / img.box.w;
  const w = img.width * s;
  const shineF = loopEvery && frame >= shineAt ? (frame - shineAt) % loopEvery : frame - shineAt;
  // Herzschlag-Puls im Takt des Glanzes (nur mit loopEvery), sonst leichtes Atmen
  const beat = loopEvery && frame >= shineAt ? interpolate(shineF, [0, 3, 12], [0, 1, 0], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'}) : 0;
  const breathe = pulse ? 1 + 0.012 * Math.sin(frame / 5) : 1;
  return (
    <div
      style={{
        position: 'relative',
        height: img.box.h * s,
        transform: `translateX(${(1 - progress) * 900}px) scale(${breathe * (1 + 0.045 * beat)})`,
        opacity: Math.min(1, progress * 1.5),
        filter: beat > 0.01 ? `brightness(${1 + 0.25 * beat})` : undefined,
      }}
    >
      <div style={{position: 'absolute', left: -img.box.x * s, top: -img.box.y * s}}>
        <BrandImg img={img} width={w} />
        <BrightCopy
          img={img}
          width={w}
          opacity={shineF >= 0 && shineF <= 16 ? 0.75 : 0}
          mask={shineMask(interpolate(shineF, [0, 16], [-10, 115], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'}), 7)}
          brightness={1.7}
        />
      </div>
    </div>
  );
};

/** Kleine Rakete links neben dem Logo auf der End-Card (BRAND.endCard.rocket). */
const EndCardRocket: React.FC = () => {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();
  const pos = BRAND.endCard.rocket;
  if (!pos) return null;
  const p = spring({frame: frame - BRAND.endCard.rocketAt, fps, config: {damping: 11, stiffness: 150}});
  if (p <= 0.001) return null;
  return (
    <RocketSprite
      x={pos[0] + (1 - p) * -160}
      y={pos[1] + (1 - p) * 160 + Math.sin(frame / 7) * 7}
      size={BRAND.endCard.rocketSize * (0.6 + 0.4 * p)}
      heading={BRAND.rocketHeading + Math.sin(frame / 9) * 4}
      opacity={Math.min(1, p * 2)}
      filter={`drop-shadow(0 0 16px ${COLORS.purple}) drop-shadow(0 8px 12px rgba(0,0,0,0.5))`}
    />
  );
};

const EndCard: React.FC = () => {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();
  const logo = spring({frame, fps, config: {damping: 10}});
  // Zeile 0 = eigene Leiste (BRAND.images.bar), ggf. Zeile 1 = Banner "JAVA & BEDROCK",
  // danach END_CARD, dann die Bewerbungs-Zeile
  const editionsWidth = BRAND.endCard.editionsWidth;
  const imageRows = editionsWidth ? 2 : 1;
  const rows = END_CARD.length + imageRows;
  const barIn = spring({frame: frame - 6, fps, config: {damping: 13}});
  const editionsIn = spring({frame: frame - 11, fps, config: {damping: 13}});
  const stepsIn = spring({frame: frame - 6 - rows * 5, fps, config: {damping: 13}});
  const linksIn = spring({frame: frame - 8 - (rows + 1) * 5, fps, config: {damping: 12}});
  return (
    <AbsoluteFill
      style={{
        paddingTop: SAFE_TOP + 170,
        alignItems: 'center',
        background: 'radial-gradient(ellipse 80% 50% at 50% 40%, rgba(91,26,168,0.45), transparent 75%)',
      }}
    >
      <EndCardRocket />
      <div style={{transform: `scale(${logo})`}}>
        <Header scale={1.42} />
      </div>
      {/* Abstände etwas enger als früher (80 / 26), weil das JAVA-&-BEDROCK-Banner höher ist als
          die alte Text-Zeile – so bleibt "(Link in Bio)" über der unteren Safe-Zone (1540 px) */}
      <div
        style={{
          marginTop: 58,
          display: 'flex',
          flexDirection: 'column',
          gap: 22,
          width: 800,
        }}
      >
        <EndCardImageRow
          img={BRAND.images.bar}
          visibleWidth={BRAND.endCard.barWidth}
          shineAt={BRAND.endCard.barShineAt}
          progress={barIn}
          pulse
        />
        {editionsWidth ? (
          <EndCardImageRow
            img={BRAND.images.javaBedrock}
            visibleWidth={editionsWidth}
            shineAt={BRAND.endCard.editionsShineAt}
            progress={editionsIn}
          />
        ) : null}
        {END_CARD.map((row, i) => {
          const p = spring({frame: frame - 6 - (i + imageRows) * 5, fps, config: {damping: 13}});
          return (
            <div
              key={i}
              style={{
                ...endRowStyle,
                border: '5px solid rgba(180,77,255,0.75)',
                boxShadow: '0 0 22px rgba(180,77,255,0.4)',
                transform: `translateX(${(1 - p) * 900}px)`,
                opacity: Math.min(1, p * 1.5),
              }}
            >
              <PixelIcon name={row.icon} size={70} glow={COLORS.purple} />
              <StaticRich text={row.text} style={endRowText} />
            </div>
          );
        })}
        {/* Deine Leiste "DISCORD discord.gg/…" (ersetzt die Zeile Discord /dc -> #tickets -> Jetzt bewerben) */}
        <EndCardImageRow img={BRAND.images.discordLink} visibleWidth={800} shineAt={40} progress={stepsIn} pulse loopEvery={24} />
      </div>
      <div
        style={{
          marginTop: 48,
          display: 'flex',
          alignItems: 'center',
          gap: 20,
          fontFamily: FONT_PIXEL,
          fontWeight: 700,
          fontSize: 50,
          color: COLORS.gold,
          textShadow: '4px 4px 0 #3a2400',
          opacity: interpolate(frame, [30, 40], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'}),
          transform: `translateY(${Math.sin(frame / 5) * 6}px)`,
        }}
      >
        {/* Pfeile hüpfen im Takt nach oben (zeigen auf den Link im Profil) */}
        <PixelIcon name="arrowUp" size={52} glow={COLORS.cyan} style={{transform: `translateY(${-14 * Math.abs(Math.sin((frame * Math.PI) / 24))}px)`}} />
        {END_CARD_FOOTER}
        <PixelIcon name="arrowUp" size={52} glow={COLORS.cyan} style={{transform: `translateY(${-14 * Math.abs(Math.sin((frame * Math.PI) / 24))}px)`}} />
      </div>
    </AbsoluteFill>
  );
};

/** Weichgezeichnetes Gameplay hinter der End-Card. */
const EndCardBackdrop: React.FC<{durationInFrames: number}> = ({durationInFrames}) => {
  if (!END_CARD_CLIP) return null;
  return (
    <AbsoluteFill>
      <FootageShot
        clip={END_CARD_CLIP}
        durationInFrames={durationInFrames}
        punchIn={false}
        blur={END_CARD_CLIP_BLUR}
      />
      <AbsoluteFill style={{background: COLORS.purpleDeep, opacity: 0.35, mixBlendMode: 'color'}} />
      <AbsoluteFill style={{background: `rgba(5,3,15,${END_CARD_CLIP_DIM})`}} />
    </AbsoluteFill>
  );
};

/**
 * Glitch übers ganze Bild bei "nicht fertig" (INTRO.glitches): farbige, verschobene Streifen
 * (RGB-Versatz-Look) für ein paar Frames. Läuft in der Sequence des Intro-Segments.
 */
const IntroGlitchOverlay: React.FC = () => {
  const frame = useCurrentFrame();
  const g = introGlitchAt(frame);
  if (g <= 0.02) return null;
  const bands = [0, 1, 2, 3, 4, 5].map((b) => ({
    top: random(`ig-t-${frame}-${b}`) * 100,
    h: 1.5 + random(`ig-h-${frame}-${b}`) * 7,
    dx: (random(`ig-x-${frame}-${b}`) - 0.5) * 160 * g,
    cyan: random(`ig-c-${frame}-${b}`) > 0.5,
  }));
  return (
    <AbsoluteFill style={{pointerEvents: 'none', mixBlendMode: 'screen'}}>
      {bands.map((b, i) => (
        <div
          key={i}
          style={{
            position: 'absolute',
            left: -100,
            right: -100,
            top: `${b.top}%`,
            height: `${b.h}%`,
            transform: `translateX(${b.dx}px)`,
            background: b.cyan
              ? `linear-gradient(90deg, transparent, ${COLORS.cyan}66 20%, ${COLORS.cyan}aa 50%, transparent)`
              : 'linear-gradient(90deg, transparent, rgba(255,40,110,0.45) 30%, rgba(255,40,110,0.7) 60%, transparent)',
            opacity: g,
          }}
        />
      ))}
      {/* kurzer Farbstich auf dem ganzen Bild */}
      <AbsoluteFill style={{background: `rgba(255,30,90,${0.12 * g})`}} />
    </AbsoluteFill>
  );
};

const smooth01 = (x: number) => {
  const t = Math.min(1, Math.max(0, x));
  return t * t * (3 - 2 * t);
};

/**
 * Lautstärke der Musik in Frame f (ab Videostart): MUSIC.volume in Sprechpausen, MUSIC.ducked
 * während die Stimme spricht (VOICE_SPEECH, weiche Rampen von MUSIC.rampFrames), MUSIC.endCard auf
 * der End-Card; Ein-/Ausblenden am Anfang/Ende.
 */
export const musicVolumeAt = (f: number, voice: boolean) => {
  const t = f / FPS;
  const ramp = MUSIC.rampFrames / FPS;
  let duck = 0;
  if (voice) {
    for (const [a, b] of VOICE_SPEECH) {
      let d = 0;
      if (t >= a && t <= b) d = 1;
      else if (t < a) d = smooth01((t - (a - ramp)) / ramp);
      else d = smooth01(1 - (t - b) / ramp);
      duck = Math.max(duck, d);
    }
  }
  const base = interpolate(t, [MAIN_SECONDS - 0.1, MAIN_SECONDS + 0.15], [MUSIC.volume, MUSIC.endCard], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });
  const total = TOTAL_FRAMES / FPS;
  const fade = Math.min(smooth01(t / MUSIC.fadeIn), smooth01((total - t) / MUSIC.fadeOut));
  return (base + (MUSIC.ducked - base) * duck) * fade;
};

/** Start-Frame eines Sound-Effekts im Video (Segmentstart bzw. End-Card + at). */
const sfxFrame = (seg: SegmentVisual | 'endCard', at: number) => {
  if (seg === 'endCard') return sec(MAIN_SECONDS) + sec(at);
  const s = SEGMENTS.find((x) => x.visual === seg);
  if (!s) throw new Error(`SFX: Segment "${seg}" gibt es nicht (src/config.ts, SFX).`);
  return sec(s.from) + sec(at);
};

/** Zoom-Punch-Hüllkurve: schnell rein (2 Frames), dann ausfedern. */
const punchEnv = (f: number) => (f < 0 ? 0 : f < 2 ? f / 2 : Math.exp(-(f - 2) / 5));

export type GalacticfyProps = {
  /** Musik-Bett an/aus (die zweite Komposition "GalacticfyBeta-OhneMusik" setzt false) */
  music?: boolean;
  /** Gesamtlautstärke aller Töne (1 = normal). scripts/render.sh rendert mit 0,708 (−3 dB
   *  Headroom, damit Remotions 16-bit-Mix nie clippt) und holt die 3 dB beim Mastern mit einem
   *  True-Peak-Limiter zurück. */
  masterGain?: number;
};

export const GalacticfyBeta: React.FC<GalacticfyProps> = ({music = true, masterGain = 1}) => {
  const frame = useCurrentFrame();
  const mainFrames = sec(MAIN_SECONDS);
  const endFrames = TOTAL_FRAMES - mainFrames;
  // Header blendet vor der End-Card aus – und solange das Discord-Formular offen ist
  // (das Popup liegt abgedunkelt über allem, nur die Banner unten bleiben darüber).
  const discordSeg = SEGMENTS.find((s) => s.visual === 'discord');
  const formFrame = discordSeg ? sec(discordSeg.from) + sec(DISCORD.formAt) : TOTAL_FRAMES;
  const headerOpacity = Math.min(
    interpolate(frame, [mainFrames - 8, mainFrames], [1, 0], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'}),
    interpolate(frame, [formFrame, formFrame + 6], [1, 0], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'}),
  );
  // Sterne/Partikel: dezent über dem Gameplay, kräftiger auf der End-Card
  const particleOpacity = interpolate(frame, [mainFrames - 4, mainFrames + 6], [0.38, 0.9], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });
  // Bild-Wackeln: Raketen-Übergänge, Glitch im Intro, alle Banner-Einschläge, Alarm in Segment 5
  const impacts = useMemo<Impact[]>(() => {
    const list: Impact[] = BRAND.wipes.map((w) => ({frame: sec(cutSeconds(w.cut)) - 1, amp: BRAND.wipeShake, len: 9}));
    const segOf = (v: SegmentVisual) => SEGMENTS.find((s) => s.visual === v);
    const dur = (s: Segment) => sec(s.to - s.from);
    const intro = segOf('intro');
    if (intro) {
      // Start-Glitch in Frame 0, Glitches bei "nicht fertig", Fehler-Zustand ("deshalb"), Sticker ("dich")
      list.push({frame: sec(intro.from) + sec(INTRO.bootGlitch.at), amp: 12, len: 7});
      INTRO.glitches.forEach((t) => list.push({frame: sec(intro.from) + sec(t), amp: 10, len: 6}));
      list.push({frame: sec(intro.from) + sec(INTRO.errorAt), amp: 6, len: 6});
      list.push({frame: sec(intro.from) + sec(INTRO.stickerAt), amp: 8, len: 8});
    }
    const checklistSeg = segOf('checklist');
    if (checklistSeg && BRAND.bannerShake > 0) {
      BRAND.checklist.banners.forEach((b) =>
        list.push({
          frame: sec(checklistSeg.from) + cueFrame(b.cue, checklistSeg.text, dur(checklistSeg)) + BRAND.checklist.slamFrames,
          amp: BRAND.bannerShake,
          len: 7,
        }),
      );
    }
    const prefixSeg = segOf('prefix');
    if (prefixSeg) {
      rewardHits(dur(prefixSeg), prefixSeg.text).forEach((h) =>
        list.push({frame: sec(prefixSeg.from) + h, amp: BRAND.slamShake, len: 8}),
      );
    }
    const slotsSeg = segOf('slots');
    if (slotsSeg) {
      list.push({frame: sec(slotsSeg.from), amp: 9, len: 9});
      list.push({frame: sec(slotsSeg.from) + sec(SLOTS.alarmAt), amp: 7, len: 9});
    }
    if (discordSeg) {
      BRAND.cta.banners.forEach((b) =>
        list.push({
          frame: sec(discordSeg.from) + cueFrame(b.cue, discordSeg.text, dur(discordSeg)) + BRAND.cta.slamFrames,
          amp: BRAND.bannerShake + 1,
          len: 7,
        }),
      );
    }
    return list;
  }, [discordSeg]);
  const shake = shakeAt(frame, impacts);
  // Zoom-Punches (PUNCHES): das ganze Bild springt kurz rein
  const punch = PUNCHES.reduce((p, pu) => {
    const seg = SEGMENTS.find((s) => s.visual === pu.seg);
    if (!seg) return p;
    return p + pu.amount * punchEnv(frame - (sec(seg.from) + sec(pu.at)));
  }, 0);
  const zoom = shake.scale * (1 + punch);
  // Clips je Segment, inkl. weiterer Schnitte innerhalb eines Segments (Segment.cuts)
  const shots = SEGMENTS.flatMap((seg, i) => {
    const from = sec(seg.from);
    const to = from + sec(seg.to - seg.from);
    const parts = [{at: 0, clip: seg.clip}, ...(seg.cuts ?? [])];
    return parts.map((p, k) => {
      const start = from + sec(p.at);
      const end = k < parts.length - 1 ? from + sec(parts[k + 1].at) : to;
      // allererster Shot: Zoom-Punch ab Frame 0 (Bewegung sofort), aber ohne Weichzeichner
      return {key: `${i}-${k}`, start, len: end - start, clip: p.clip, punchBlur: i > 0 || k > 0, inner: k > 0};
    });
  });
  const withMusic = USE_MUSIC && music;
  return (
    <AbsoluteFill style={{backgroundColor: COLORS.bg}}>
      {/* Alles Sichtbare wackelt gemeinsam (shakeAt in Brand.tsx); minimaler Zoom verdeckt die Ränder */}
      <AbsoluteFill
        style={{transform: zoom === 1 ? undefined : `translate(${shake.x}px, ${shake.y}px) scale(${zoom})`}}
      >
        <Background />

        {/* Gameplay-Clips je Segment (Datei + Trim in config.ts) */}
        {shots.map((sh) => (
          <Sequence
            key={`clip-${sh.key}`}
            from={sh.start}
            durationInFrames={sh.len}
            premountFor={30}
            name={`Clip ${sh.key}: ${sh.clip.file}`}
          >
            <FootageShot clip={sh.clip} durationInFrames={sh.len} punchIn punchBlur={sh.punchBlur} />
          </Sequence>
        ))}
        <Sequence durationInFrames={mainFrames} name="Farblook">
          <FootageGrade />
        </Sequence>
        <Sequence from={mainFrames} durationInFrames={endFrames} premountFor={30} name="End-Card-Clip">
          <EndCardBackdrop durationInFrames={endFrames} />
        </Sequence>

        <SpaceParticles opacity={particleOpacity} starOpacity={0.7} />

        {/* Header (Logo) während der Hauptsegmente */}
        <AbsoluteFill
          style={{
            top: SAFE_TOP,
            height: 120,
            alignItems: 'center',
            justifyContent: 'flex-start',
            opacity: headerOpacity,
          }}
        >
          <Header />
        </AbsoluteFill>

        {SEGMENTS.map((seg, i) => (
          <Sequence
            key={i}
            from={sec(seg.from)}
            durationInFrames={sec(seg.to - seg.from)}
            name={`${i + 1}: ${seg.visual}`}
          >
            <SegmentScene seg={seg} first={i === 0} />
          </Sequence>
        ))}

        <Sequence from={mainFrames} durationInFrames={endFrames} name="End-Card">
          <EndCard />
        </Sequence>

        {/* Glitch übers ganze Bild bei "nicht fertig" */}
        <Sequence durationInFrames={sec(SEGMENTS[0].to - SEGMENTS[0].from)} name="Intro-Glitch">
          <IntroGlitchOverlay />
        </Sequence>

        {/* Blitz-Übergänge an den Schnitten (Farbe je Segment: Segment.flash) */}
        {SEGMENTS.slice(1).map((seg, i) => (
          <Sequence key={`flash-${i}`} from={sec(seg.from)} durationInFrames={10} name="Flash">
            <CutFlash color={seg.flash ?? COLORS.cyan} strength={seg.flash ? 0.6 : 0.42} length={seg.flash ? 9 : 7} />
          </Sequence>
        ))}
        {shots
          .filter((sh) => sh.inner)
          .map((sh) => (
            <Sequence key={`flash-in-${sh.key}`} from={sh.start} durationInFrames={8} name="Flash (Schnitt im Segment)">
              <CutFlash strength={0.28} length={6} />
            </Sequence>
          ))}
        <Sequence from={mainFrames} durationInFrames={14} name="Flash End-Card">
          <CutFlash color={COLORS.purple} strength={0.9} length={12} />
        </Sequence>

        {/* Raketen-Übergänge (eigene Grafik rocket.png), mittig über dem Schnitt */}
        {BRAND.wipes.map((w, i) => (
          <Sequence
            key={`wipe-${i}`}
            from={sec(cutSeconds(w.cut)) - Math.round(w.frames / 2)}
            durationInFrames={w.frames + 6}
            name={`Raketen-Übergang ${i + 1}`}
          >
            <RocketWipe wipe={w} />
          </Sequence>
        ))}
      </AbsoluteFill>

      {/* Voiceover: public/voiceover.mp3 (USE_VOICEOVER in config.ts, false = ohne Stimme) */}
      {USE_VOICEOVER ? (
        <Audio src={staticFile(VOICEOVER_FILE)} volume={VOICEOVER_VOLUME * masterGain} name="Voiceover" />
      ) : null}
      {/* Musik-Bett mit Ducking unter der Stimme (USE_MUSIC / MUSIC in config.ts) */}
      {withMusic ? (
        <Audio src={staticFile(MUSIC.file)} volume={(f) => musicVolumeAt(f, USE_VOICEOVER) * masterGain} name="Musik" />
      ) : null}
      {/* Sound-Effekte (SFX in config.ts) */}
      {USE_SFX
        ? SFX.map((fx, i) => (
            <Sequence key={`sfx-${i}`} from={sfxFrame(fx.seg, fx.at)} durationInFrames={45} name={`SFX: ${fx.label}`}>
              <Audio src={staticFile(`sfx/${fx.file}`)} volume={fx.volume * masterGain} />
            </Sequence>
          ))
        : null}
      {/* Mausklick-Sound beim Klick auf "Jetzt bewerben" (Discord-Segment) */}
      {DISCORD.clickSound && discordSeg ? (
        // +1: das Knacken liegt auf dem ersten Frame, in dem der Button sichtbar eingedrückt ist
        <Sequence from={sec(discordSeg.from) + sec(DISCORD.clickAt) + 1} durationInFrames={sec(0.5)} name="SFX: Mausklick">
          <Audio src={staticFile(DISCORD.clickSound)} volume={DISCORD.clickVolume * masterGain} />
        </Sequence>
      ) : null}
    </AbsoluteFill>
  );
};
