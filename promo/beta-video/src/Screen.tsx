import React from 'react';
import {Img, staticFile} from 'remotion';
import {Region, SCREEN_DIR, ScreenImage} from './config';

/**
 * Echter Screenshot-Ausschnitt aus public/screens/.
 *  - region: nur ein Teil des PNGs (Pixel), sonst das ganze Bild
 *  - scale oder width: Darstellungsgröße
 *  - pixelated: harte Minecraft-Pixel beim Hochskalieren (Standard: an, sobald scale > 1)
 *  - outline: Polygon in Screenshot-Pixeln (z. B. SCREEN_OUTLINES.discordBanner) – alles
 *    außerhalb wird weggeschnitten (Glow/drop-shadow am Eltern-Element folgt dem Umriss)
 */
export const Screen: React.FC<{
  img: ScreenImage;
  region?: Region;
  scale?: number;
  width?: number;
  pixelated?: boolean;
  outline?: readonly (readonly [number, number])[];
  style?: React.CSSProperties;
  children?: React.ReactNode;
}> = ({img, region, scale, width, pixelated, outline, style, children}) => {
  const r = region ?? {x: 0, y: 0, w: img.width, h: img.height};
  const s = scale ?? (width ? width / r.w : 1);
  const pix = pixelated ?? s > 1.01;
  const clipPath = outline
    ? `polygon(${outline.map(([x, y]) => `${((x - r.x) * s).toFixed(2)}px ${((y - r.y) * s).toFixed(2)}px`).join(', ')})`
    : undefined;
  return (
    <div
      style={{
        position: 'relative',
        width: r.w * s,
        height: r.h * s,
        overflow: 'hidden',
        flexShrink: 0,
        clipPath,
        ...style,
      }}
    >
      <Img
        src={staticFile(`${SCREEN_DIR}/${img.file}`)}
        style={{
          position: 'absolute',
          left: -r.x * s,
          top: -r.y * s,
          width: img.width * s,
          height: img.height * s,
          maxWidth: 'none',
          imageRendering: pix ? 'pixelated' : 'auto',
        }}
      />
      {children}
    </div>
  );
};

/** Markierungsrahmen (z. B. um "/dc" oder "1/20"), Koordinaten in Screenshot-Pixeln. */
export const Highlight: React.FC<{
  region: Region;
  scale: number;
  color: string;
  /** 0–1: Einblenden */
  progress: number;
  /** pulsierender Glow (z. B. frame) */
  pulse?: number;
  pad?: number;
}> = ({region, scale, color, progress, pulse = 0, pad = 8}) => {
  if (progress <= 0) return null;
  const glow = 16 + Math.sin(pulse / 4) * 8;
  return (
    <div
      style={{
        position: 'absolute',
        left: region.x * scale - pad,
        top: region.y * scale - pad,
        width: region.w * scale + pad * 2,
        height: region.h * scale + pad * 2,
        border: `5px solid ${color}`,
        boxShadow: `0 0 ${glow}px ${color}, inset 0 0 ${glow}px ${color}88`,
        transform: `scale(${1.6 - 0.6 * progress})`,
        opacity: Math.min(1, progress * 1.4),
        pointerEvents: 'none',
      }}
    />
  );
};
