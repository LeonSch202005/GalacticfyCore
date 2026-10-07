import React from 'react';
import {
  AbsoluteFill,
  Easing,
  OffthreadVideo,
  interpolate,
  staticFile,
  useCurrentFrame,
  useVideoConfig,
} from 'remotion';
import {Animated, CLIP_DIR, ClipShot, COLORS, HEIGHT, WIDTH} from './config';

// Die Aufnahmen sind 16:9 (1920x1080) und werden auf 9:16 zugeschnitten.
const SRC_ASPECT = 16 / 9;
const clampOpts = {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'} as const;

const valueAt = (v: Animated | undefined, fallback: number, t: number) => {
  if (v === undefined) return fallback;
  if (typeof v === 'number') return v;
  return v[0] + (v[1] - v[0]) * t;
};

const clampNum = (v: number, min: number, max: number) => Math.min(max, Math.max(min, v));

/**
 * Gameplay-Clip als Vollbild-Hintergrund.
 * Zeit läuft relativ zur umgebenden <Sequence>; Tempo = (end − start) / Shot-Länge.
 */
export const FootageShot: React.FC<{
  clip: ClipShot;
  durationInFrames: number;
  /** Zoom-/Blur-"Punch" beim Reinschneiden */
  punchIn?: boolean;
  /** Dauerhafter Weichzeichner (px), z. B. für die End-Card */
  blur?: number;
}> = ({clip, durationInFrames, punchIn = true, blur = 0}) => {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();

  // Langsame Kamerafahrt über die ganze Shot-Länge
  const t = Easing.inOut(Easing.sin)(clampNum(frame / Math.max(1, durationInFrames - 1), 0, 1));
  const punch = punchIn
    ? interpolate(frame, [0, 10], [0.16, 0], {...clampOpts, easing: Easing.out(Easing.cubic)})
    : 0;
  const zoom = Math.max(1, valueAt(clip.zoom, 1, t)) * (1 + punch);

  const h = HEIGHT * zoom;
  const w = h * SRC_ASPECT;
  const fx = valueAt(clip.focusX, 50, t) / 100;
  const fy = valueAt(clip.focusY, 50, t) / 100;
  // Fokuspunkt möglichst in die Bildmitte, ohne dass Ränder sichtbar werden
  const left = clampNum(WIDTH / 2 - fx * w, WIDTH - w, 0);
  const top = clampNum(HEIGHT / 2 - fy * h, HEIGHT - h, 0);

  const playbackRate = (clip.end - clip.start) / (durationInFrames / fps);
  const cutBlur = punchIn ? interpolate(frame, [0, 6], [10, 0], clampOpts) : 0;

  return (
    <AbsoluteFill style={{overflow: 'hidden'}}>
      <OffthreadVideo
        src={staticFile(`${CLIP_DIR}/${clip.file}`)}
        trimBefore={Math.round(clip.start * fps)}
        playbackRate={playbackRate}
        muted
        style={{
          position: 'absolute',
          left,
          top,
          width: w,
          height: h,
          maxWidth: 'none',
          objectFit: 'cover',
          filter: `blur(${blur + cutBlur}px) saturate(1.2) contrast(1.08) brightness(1.06)`,
        }}
      />
    </AbsoluteFill>
  );
};

/**
 * Farblook über dem Gameplay: leichter Lila-Stich, dunkle Verläufe oben (Logo)
 * und im Caption-Bereich (y ≈ 1100–1540), Vignette.
 */
export const FootageGrade: React.FC = () => {
  return (
    <AbsoluteFill style={{pointerEvents: 'none'}}>
      {/* Lila Farbstich */}
      <AbsoluteFill style={{background: COLORS.purpleDeep, opacity: 0.16, mixBlendMode: 'color'}} />
      <AbsoluteFill style={{background: 'rgba(60,20,140,0.07)'}} />
      {/* Oben: Logo-Bereich */}
      <AbsoluteFill
        style={{
          background:
            'linear-gradient(180deg, rgba(5,3,15,0.78) 0px, rgba(5,3,15,0.45) 250px, rgba(5,3,15,0) 440px)',
        }}
      />
      {/* Unten: Captions + App-UI */}
      <AbsoluteFill
        style={{
          background:
            'linear-gradient(180deg, rgba(5,3,15,0) 880px, rgba(5,3,15,0.55) 1080px, rgba(5,3,15,0.68) 1250px, rgba(5,3,15,0.68) 1560px, rgba(5,3,15,0.5) 1920px)',
        }}
      />
      {/* Vignette */}
      <AbsoluteFill
        style={{
          background:
            'radial-gradient(ellipse 95% 80% at 50% 45%, transparent 60%, rgba(5,0,20,0.5) 100%)',
        }}
      />
    </AbsoluteFill>
  );
};

/** Kurzer Licht-Blitz beim Schnitt. */
export const CutFlash: React.FC<{color?: string; strength?: number; length?: number}> = ({
  color = COLORS.cyan,
  strength = 0.7,
  length = 8,
}) => {
  const frame = useCurrentFrame();
  const o = interpolate(frame, [0, length], [strength, 0], {
    ...clampOpts,
    easing: Easing.out(Easing.quad),
  });
  if (o <= 0) return null;
  return (
    <AbsoluteFill
      style={{
        opacity: o,
        background: `radial-gradient(ellipse 80% 60% at 50% 45%, rgba(255,255,255,0.85), ${color} 55%, ${COLORS.purpleDeep} 100%)`,
        mixBlendMode: 'screen',
        pointerEvents: 'none',
      }}
    />
  );
};
