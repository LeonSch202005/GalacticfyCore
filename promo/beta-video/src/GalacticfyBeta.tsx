import React from 'react';
import {
  AbsoluteFill,
  Audio,
  Sequence,
  interpolate,
  spring,
  staticFile,
  useCurrentFrame,
  useVideoConfig,
} from 'remotion';
import {Background, SpaceParticles} from './Background';
import {CutFlash, FootageGrade, FootageShot} from './Footage';
import {
  BRAND,
  COLORS,
  DISCORD,
  END_CARD,
  END_CARD_CLIP,
  END_CARD_CLIP_BLUR,
  END_CARD_CLIP_DIM,
  END_CARD_FOOTER,
  END_CARD_LINKS,
  END_CARD_STEPS,
  FONT_HEAVY,
  FONT_PIXEL,
  MAIN_SECONDS,
  SCREENS,
  SEGMENTS,
  Segment,
  SegmentVisual,
  TOTAL_FRAMES,
  USE_VOICEOVER,
  VOICEOVER_FILE,
  VOICEOVER_VOLUME,
  sec,
} from './config';
import {KineticCaption, StaticRich} from './RichText';
import {PixelIcon} from './PixelIcon';
import {Screen} from './Screen';
import {
  ChecklistVisual,
  DiscordVisual,
  IntroVisual,
  PrefixVisual,
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

const SegmentScene: React.FC<{seg: Segment}> = ({seg}) => {
  const frame = useCurrentFrame();
  const duration = sec(seg.to - seg.from);
  const Visual = VISUALS[seg.visual];
  const fadeIn = interpolate(frame, [0, 4], [0.3, 1], {extrapolateRight: 'clamp'});
  const fadeOut = interpolate(frame, [duration - 5, duration], [1, 0.1], {
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
        <Visual duration={duration} stage={{top: VISUAL_TOP, height: VISUAL_HEIGHT}} />
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
        <KineticCaption
          text={seg.text}
          fontSize={seg.fontSize ?? 76}
          revealFrames={Math.min(duration * 0.6, duration - 20)}
        />
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

const EndCard: React.FC = () => {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();
  const logo = spring({frame, fps, config: {damping: 10}});
  const stepsIn = spring({frame: frame - 6 - END_CARD.length * 5, fps, config: {damping: 13}});
  const linksIn = spring({frame: frame - 8 - (END_CARD.length + 1) * 5, fps, config: {damping: 12}});
  return (
    <AbsoluteFill
      style={{
        paddingTop: SAFE_TOP + 170,
        alignItems: 'center',
        background: 'radial-gradient(ellipse 80% 50% at 50% 40%, rgba(91,26,168,0.45), transparent 75%)',
      }}
    >
      <div style={{transform: `scale(${logo})`}}>
        <Header scale={1.42} />
      </div>
      <div
        style={{
          marginTop: 80,
          display: 'flex',
          flexDirection: 'column',
          gap: 26,
          width: 800,
        }}
      >
        {END_CARD.map((row, i) => {
          const p = spring({frame: frame - 6 - i * 5, fps, config: {damping: 13}});
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
        {/* Hervorgehoben: der echte Weg zur Bewerbung (Discord /dc -> #tickets -> Jetzt bewerben) */}
        <div
          style={{
            ...endRowStyle,
            padding: '20px 24px',
            border: `5px solid ${COLORS.cyan}`,
            boxShadow: `0 0 ${30 + Math.sin(frame / 5) * 14}px ${COLORS.cyan}`,
            transform: `translateX(${(1 - stepsIn) * 900}px)`,
            opacity: Math.min(1, stepsIn * 1.5),
          }}
        >
          <PixelIcon name="pointer" size={70} glow={COLORS.purple} />
          <div style={{display: 'flex', flexWrap: 'wrap', alignItems: 'center', columnGap: 14, rowGap: 14}}>
            {END_CARD_STEPS.map((step, i) => (
              <div key={i} style={{display: 'flex', alignItems: 'center', gap: 14}}>
                {i > 0 ? (
                  <PixelIcon name="arrowUp" size={34} glow={COLORS.cyan} style={{transform: 'rotate(90deg)'}} />
                ) : null}
                <EndCardStep text={step.text} look={step.look} />
              </div>
            ))}
          </div>
        </div>
      </div>
      {/* Website + Discord-Befehl */}
      <div
        style={{
          marginTop: 36,
          display: 'flex',
          gap: 22,
          width: 800,
          transform: `scale(${0.7 + 0.3 * linksIn})`,
          opacity: Math.min(1, linksIn * 1.5),
        }}
      >
        {END_CARD_LINKS.map((t) => (
          <div
            key={t}
            style={{
              flex: 1,
              textAlign: 'center',
              padding: '20px 12px',
              background: 'rgba(46,242,255,0.12)',
              border: `4px solid ${COLORS.cyan}b3`,
              boxShadow: `0 0 20px rgba(46,242,255,0.35)`,
            }}
          >
            <StaticRich
              text={t}
              style={{
                fontFamily: FONT_PIXEL,
                fontWeight: 700,
                fontSize: 42,
                color: COLORS.white,
                textShadow: '3px 3px 0 rgba(0,0,0,0.6)',
                whiteSpace: 'nowrap',
              }}
            />
          </div>
        ))}
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
          transform: `translateY(${Math.sin(frame / 5) * 8}px)`,
        }}
      >
        <PixelIcon name="arrowUp" size={52} glow={COLORS.cyan} />
        {END_CARD_FOOTER}
        <PixelIcon name="arrowUp" size={52} glow={COLORS.cyan} />
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

export const GalacticfyBeta: React.FC = () => {
  const frame = useCurrentFrame();
  const mainFrames = sec(MAIN_SECONDS);
  const endFrames = TOTAL_FRAMES - mainFrames;
  // Header blendet vor der End-Card aus – und solange das Discord-Formular offen ist
  // (das Popup liegt abgedunkelt über allem, nur die Caption bleibt darüber).
  const discordSeg = SEGMENTS.find((s) => s.visual === 'discord');
  const formFrame = discordSeg ? sec(discordSeg.from + DISCORD.formAt) : TOTAL_FRAMES;
  const headerOpacity = Math.min(
    interpolate(frame, [mainFrames - 8, mainFrames], [1, 0], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'}),
    interpolate(frame, [formFrame, formFrame + 6], [1, 0], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'}),
  );
  // Sterne/Partikel: dezent über dem Gameplay, kräftiger auf der End-Card
  const particleOpacity = interpolate(frame, [mainFrames - 4, mainFrames + 6], [0.38, 0.9], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });
  return (
    <AbsoluteFill style={{backgroundColor: COLORS.bg}}>
      <Background />

      {/* Gameplay-Clips je Segment (Datei + Trim in config.ts) */}
      {SEGMENTS.map((seg, i) => (
        <Sequence
          key={`clip-${i}`}
          from={sec(seg.from)}
          durationInFrames={sec(seg.to - seg.from)}
          premountFor={30}
          name={`Clip ${i + 1}: ${seg.clip.file}`}
        >
          <FootageShot clip={seg.clip} durationInFrames={sec(seg.to - seg.from)} punchIn={i > 0} />
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
          <SegmentScene seg={seg} />
        </Sequence>
      ))}

      <Sequence from={mainFrames} durationInFrames={endFrames} name="End-Card">
        <EndCard />
      </Sequence>

      {/* Blitz-Übergänge an den Schnitten */}
      {SEGMENTS.slice(1).map((seg, i) => (
        <Sequence key={`flash-${i}`} from={sec(seg.from)} durationInFrames={10} name="Flash">
          <CutFlash strength={0.42} length={7} />
        </Sequence>
      ))}
      <Sequence from={mainFrames} durationInFrames={14} name="Flash End-Card">
        <CutFlash color={COLORS.purple} strength={0.9} length={12} />
      </Sequence>

      {/* Voiceover: public/voiceover.mp3 (USE_VOICEOVER in config.ts, false = stumm) */}
      {USE_VOICEOVER ? <Audio src={staticFile(VOICEOVER_FILE)} volume={VOICEOVER_VOLUME} /> : null}
    </AbsoluteFill>
  );
};
