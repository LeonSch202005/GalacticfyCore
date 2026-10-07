import React from 'react';
import {AbsoluteFill, Easing, Img, interpolate, random, staticFile, useCurrentFrame} from 'remotion';
import {BRAND, BrandImage, COLORS} from './config';

// Eigene Grafiken (public/brand/, siehe BRAND in config.ts). Kein Pixel-Art -> immer weich
// skaliert (image-rendering: auto), nie "pixelated".

export const brandSrc = (img: BrandImage) => staticFile(`${BRAND.dir}/${img.file}`);

/** Darstellungshöhe eines Brand-PNGs bei gegebener Breite. */
export const brandHeight = (img: BrandImage, width: number) => (width * img.height) / img.width;

/** Eigene Grafik, width = Darstellungsbreite des ganzen PNGs. */
export const BrandImg: React.FC<{img: BrandImage; width: number; style?: React.CSSProperties}> = ({
  img,
  width,
  style,
}) => (
  <Img
    src={brandSrc(img)}
    style={{
      display: 'block',
      width,
      height: brandHeight(img, width),
      maxWidth: 'none',
      imageRendering: 'auto',
      ...style,
    }}
  />
);

/**
 * Aufgehellte Kopie der Grafik genau über dem Original, per CSS-Verlaufsmaske nur teilweise
 * sichtbar: Glow-Puls (radial-gradient) oder Glanz-Streifen (linear-gradient). Folgt der Form
 * (Alpha) der Grafik – es leuchtet also nur die Grafik selbst, kein Rechteck.
 */
export const BrightCopy: React.FC<{
  img: BrandImage;
  width: number;
  mask: string;
  opacity: number;
  brightness?: number;
}> = ({img, width, mask, opacity, brightness = 1.8}) => {
  if (opacity <= 0.001) return null;
  return (
    <BrandImg
      img={img}
      width={width}
      style={{
        position: 'absolute',
        left: 0,
        top: 0,
        opacity: Math.min(1, opacity),
        filter: `brightness(${brightness}) saturate(1.25)`,
        WebkitMaskImage: mask,
        maskImage: mask,
      }}
    />
  );
};

/** Maske für einen diagonalen Glanz-Streifen an Position p (in %, −20 … 120). */
export const shineMask = (p: number, w = 11, angle = 105) =>
  `linear-gradient(${angle}deg, transparent ${p - w}%, #000 ${p}%, transparent ${p + w}%)`;

/**
 * Rakete (rocket.png), Mitte bei (x, y), Breite size, fliegt in Richtung heading
 * (Grad, 0 = nach rechts, −90 = nach oben). Absolut positioniert.
 */
export const RocketSprite: React.FC<{
  x: number;
  y: number;
  size: number;
  heading: number;
  opacity?: number;
  filter?: string;
}> = ({x, y, size, heading, opacity = 1, filter}) => {
  const img = BRAND.images.rocket;
  const h = brandHeight(img, size);
  return (
    <div
      style={{
        position: 'absolute',
        left: x - size / 2,
        top: y - h / 2,
        width: size,
        height: h,
        transform: `rotate(${heading - BRAND.rocketHeading}deg)`,
        opacity,
        filter,
      }}
    >
      <BrandImg img={img} width={size} />
    </div>
  );
};

/** Winkel in Grad, auf −180 … 180 normiert (für Interpolation von Flugrichtungen). */
export const wrapDeg = (d: number) => ((((d + 180) % 360) + 360) % 360) - 180;

export type Wipe = (typeof BRAND.wipes)[number];

/**
 * Raketen-Übergang: Rakete schießt in `frames` Frames von `from` nach `to` (Bild-px) – mit
 * Leuchtspur (Verlauf weiß → cyan → lila) und nachgezogenen, weichgezeichneten Kopien.
 * Läuft in einer <Sequence>, die frames/2 vor dem Schnitt beginnt.
 */
export const RocketWipe: React.FC<{wipe: Wipe}> = ({wipe}) => {
  const frame = useCurrentFrame();
  const {frames, from, to, size} = wipe;
  const pos = (f: number) => {
    const t = f / frames;
    // leicht beschleunigend
    const e = 0.7 * t + 0.3 * t * t;
    return [from[0] + (to[0] - from[0]) * e, from[1] + (to[1] - from[1]) * e] as const;
  };
  const dx = to[0] - from[0];
  const dy = to[1] - from[1];
  const heading = (Math.atan2(dy, dx) * 180) / Math.PI;
  const dist = Math.hypot(dx, dy);
  const ux = dx / dist;
  const uy = dy / dist;
  const [x, y] = pos(frame);
  // Spur: endet am Triebwerk, wird mit der Strecke länger, blendet nach dem Durchflug aus
  const fade = frame <= frames ? 1 : Math.max(0, 1 - (frame - frames) / 5);
  const travelled = Math.hypot(x - from[0], y - from[1]);
  const trailLen = Math.min(1250, travelled + size * 0.4);
  const trailH = size * 0.42;
  const tailX = x - ux * size * 0.42;
  const tailY = y - uy * size * 0.42;
  const ghosts = [1, 2, 3, 4, 5];
  return (
    <AbsoluteFill style={{pointerEvents: 'none', overflow: 'hidden'}}>
      {fade > 0 ? (
        <div
          style={{
            position: 'absolute',
            left: tailX - trailLen,
            top: tailY - trailH / 2,
            width: trailLen,
            height: trailH,
            transformOrigin: '100% 50%',
            transform: `rotate(${heading}deg)`,
            borderRadius: trailH,
            background: `linear-gradient(90deg, rgba(91,26,168,0) 0%, ${COLORS.purple}aa 45%, ${COLORS.cyan}dd 80%, #ffffff 100%)`,
            filter: 'blur(10px)',
            opacity: 0.9 * fade,
            mixBlendMode: 'screen',
          }}
        />
      ) : null}
      {frame <= frames + 1
        ? ghosts.map((k) => {
            const [gx, gy] = pos(frame - k * 0.55);
            return (
              <RocketSprite
                key={k}
                x={gx}
                y={gy}
                size={size}
                heading={heading}
                opacity={0.42 * (1 - k / (ghosts.length + 1))}
                filter={`blur(${3 + k * 2.5}px)`}
              />
            );
          })
        : null}
      {frame <= frames + 1 ? (
        <RocketSprite
          x={x}
          y={y}
          size={size}
          heading={heading}
          filter={`drop-shadow(0 0 22px ${COLORS.purple}) drop-shadow(0 0 40px ${COLORS.cyan}88)`}
        />
      ) : null}
    </AbsoluteFill>
  );
};

/**
 * "Slam": eine Grafik knallt von fromScale auf 1 (beschleunigt, slamFrames Frames lang) und
 * wackelt nach dem Einschlag kurz. f = Frames seit Start, frame = aktueller Frame (für die
 * Zufalls-Wackler), seed = Name für die Zufallswerte. Liefert u. a. scale (× (1 ± squash) für
 * kurzes Stauchen), hit = Frames seit dem Einschlag, flash = Blitz 0–1, Wackeln sx/sy (px), sr (Grad).
 */
export const slamAt = (
  f: number,
  frame: number,
  o: {fromScale: number; slamFrames: number; shake: number; seed: string},
) => {
  const slam = interpolate(f, [0, o.slamFrames], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
    easing: Easing.in(Easing.cubic),
  });
  const hit = f - o.slamFrames;
  const shakeK = hit >= 0 ? Math.max(0, 1 - hit / 11) : 0;
  return {
    slam,
    hit,
    scale: o.fromScale + (1 - o.fromScale) * slam,
    squash: hit >= 0 ? 0.09 * Math.exp(-hit / 2.5) * Math.cos(hit * 1.1) : 0,
    sx: (random(`${o.seed}-x-${frame}`) - 0.5) * 2 * o.shake * shakeK,
    sy: (random(`${o.seed}-y-${frame}`) - 0.5) * 2 * o.shake * 0.6 * shakeK,
    sr: (random(`${o.seed}-r-${frame}`) - 0.5) * 4 * shakeK,
    flash: interpolate(hit, [0, 1, 9], [0, 1, 0], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'}),
    opacity: interpolate(f, [0, 2], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'}),
  };
};

/** Glanz-Streifen (BrightCopy + shineMask), der in `frames` Frames einmal über die Grafik läuft.
 *  f = Frames seit Start des Glanzes. */
export const Shine: React.FC<{
  img: BrandImage;
  width: number;
  f: number;
  frames?: number;
  opacity?: number;
  band?: number;
  angle?: number;
  brightness?: number;
}> = ({img, width, f, frames = 16, opacity = 0.8, band = 8, angle = 110, brightness = 1.9}) => (
  <BrightCopy
    img={img}
    width={width}
    opacity={f >= 0 && f <= frames ? opacity : 0}
    mask={shineMask(
      interpolate(f, [0, frames], [-15, 118], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'}),
      band,
      angle,
    )}
    brightness={brightness}
  />
);

/** Ein "Einschlag", der das ganze Bild kurz wackeln lässt (Frame im Video, Stärke px, Länge). */
export type Impact = {frame: number; amp: number; len: number};

/** Bild-Wackeln (Verschiebung + minimaler Zoom, damit keine Ränder sichtbar werden). */
export const shakeAt = (frame: number, impacts: Impact[]) => {
  let e = 0;
  for (const im of impacts) {
    const f = frame - im.frame;
    if (f < -2 || f > im.len) continue;
    const k = f < 0 ? (f + 3) / 3 : 1 - f / im.len;
    e += im.amp * k * k;
  }
  if (e < 0.05) return {x: 0, y: 0, scale: 1};
  return {
    x: (random(`shake-x-${frame}`) - 0.5) * 2 * e,
    y: (random(`shake-y-${frame}`) - 0.5) * 2 * e,
    scale: 1 + (2 * e + 2) / 1080,
  };
};
