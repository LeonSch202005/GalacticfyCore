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
  END_CARD,
  END_CARD_CLIP,
  END_CARD_CLIP_BLUR,
  END_CARD_CLIP_DIM,
  END_CARD_FOOTER,
  FONT_HEAVY,
  FONT_PIXEL,
  MAIN_SECONDS,
  SEGMENTS,
  Segment,
  SegmentVisual,
  TOTAL_FRAMES,
  USE_VOICEOVER,
  VOICEOVER_FILE,
  sec,
} from './config';
import {KineticCaption, StaticRich} from './RichText';
import {PixelIcon} from './PixelIcon';
import {
  ChatVisual,
  ChecklistVisual,
  DiscordVisual,
  IntroVisual,
  SlotsVisual,
  TestersVisual,
} from './Visuals';

// Layout (1080x1920): Header oben ab 150px, Visual-Bereich 290–1085, Caption 1110–1540,
// unterste ~380px bleiben frei (TikTok/Reels-UI). Dahinter läuft Vollbild-Gameplay.
const SAFE_TOP = 150;
const VISUAL_TOP = 290;
const VISUAL_HEIGHT = 795;
const CAPTION_TOP = 1110;
const CAPTION_HEIGHT = 430;

/** Kompakte Visuals sitzen oben (unter dem Logo) oder unten (direkt über der Caption),
 *  damit das Gameplay in der Bildmitte frei bleibt. */
const VISUAL_ALIGN: Record<SegmentVisual, 'top' | 'bottom'> = {
  intro: 'top',
  testers: 'bottom',
  checklist: 'top',
  chat: 'bottom',
  slots: 'top',
  discord: 'bottom',
};

const VISUALS = {
  intro: IntroVisual,
  testers: TestersVisual,
  checklist: ChecklistVisual,
  chat: ChatVisual,
  slots: SlotsVisual,
  discord: DiscordVisual,
};

const Header: React.FC<{scale?: number}> = ({scale = 1}) => {
  const frame = useCurrentFrame();
  const glow = 18 + Math.sin(frame / 8) * 6;
  return (
    <div style={{display: 'flex', alignItems: 'center', gap: 18 * scale}}>
      <div
        style={{
          fontFamily: FONT_HEAVY,
          fontWeight: 900,
          fontSize: 76 * scale,
          letterSpacing: 4 * scale,
          color: COLORS.white,
          textShadow: `0 0 ${glow}px ${COLORS.purple}, 0 0 ${glow * 2}px ${COLORS.purple}, ${5 * scale}px ${5 * scale}px 0 ${COLORS.purpleDeep}`,
        }}
      >
        {BRAND.name}
      </div>
      <div
        style={{
          fontFamily: FONT_PIXEL,
          fontWeight: 700,
          fontSize: 34 * scale,
          color: COLORS.bg,
          background: COLORS.cyan,
          padding: `${6 * scale}px ${12 * scale}px`,
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
        <Visual duration={duration} />
      </div>
      <div
        style={{
          position: 'absolute',
          top: CAPTION_TOP,
          left: 70,
          right: 70,
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

const EndCard: React.FC = () => {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();
  const logo = spring({frame, fps, config: {damping: 10}});
  return (
    <AbsoluteFill
      style={{
        paddingTop: SAFE_TOP + 110,
        alignItems: 'center',
        background: 'radial-gradient(ellipse 80% 50% at 50% 40%, rgba(91,26,168,0.45), transparent 75%)',
      }}
    >
      <div style={{transform: `scale(${logo})`}}>
        <Header scale={1.25} />
      </div>
      <div
        style={{
          marginTop: 110,
          display: 'flex',
          flexDirection: 'column',
          gap: 40,
          width: 940,
        }}
      >
        {END_CARD.map((row, i) => {
          const p = spring({frame: frame - 8 - i * 6, fps, config: {damping: 13}});
          return (
            <div
              key={i}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 34,
                padding: '28px 34px',
                background: 'rgba(10,5,30,0.72)',
                border: `5px solid ${i === END_CARD.length - 1 ? COLORS.cyan : 'rgba(180,77,255,0.75)'}`,
                boxShadow:
                  i === END_CARD.length - 1
                    ? `0 0 ${30 + Math.sin(frame / 5) * 14}px ${COLORS.cyan}`
                    : `0 0 22px rgba(180,77,255,0.4)`,
                transform: `translateX(${(1 - p) * 900}px)`,
                opacity: Math.min(1, p * 1.5),
              }}
            >
              <PixelIcon name={row.icon} size={90} glow={COLORS.purple} />
              <StaticRich
                text={row.text}
                style={{
                  fontFamily: FONT_HEAVY,
                  fontWeight: 900,
                  fontSize: 54,
                  lineHeight: 1.15,
                  color: COLORS.white,
                  textShadow: '0 4px 0 rgba(0,0,0,0.6)',
                }}
              />
            </div>
          );
        })}
      </div>
      <div
        style={{
          marginTop: 40,
          display: 'flex',
          alignItems: 'center',
          gap: 20,
          fontFamily: FONT_PIXEL,
          fontWeight: 700,
          fontSize: 52,
          color: COLORS.gold,
          textShadow: '4px 4px 0 #3a2400',
          opacity: interpolate(frame, [30, 40], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'}),
          transform: `translateY(${Math.sin(frame / 5) * 8}px)`,
        }}
      >
        <PixelIcon name="arrowUp" size={56} glow={COLORS.cyan} />
        {END_CARD_FOOTER}
        <PixelIcon name="arrowUp" size={56} glow={COLORS.cyan} />
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
  const headerOpacity = interpolate(frame, [mainFrames - 8, mainFrames], [1, 0], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });
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

      {/* Voiceover: public/voiceover.mp3 ablegen und USE_VOICEOVER in config.ts auf true setzen */}
      {USE_VOICEOVER ? <Audio src={staticFile(VOICEOVER_FILE)} /> : null}
    </AbsoluteFill>
  );
};
