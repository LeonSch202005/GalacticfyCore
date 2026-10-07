import React, {useMemo} from 'react';
import {AbsoluteFill, interpolate, random, useCurrentFrame} from 'remotion';
import {COLORS, HEIGHT, WIDTH} from './config';

const STAR_COUNT = 220;
const BLOCK_COUNT = 26;

/** Sterne + Pixel-Block-Partikel (auch dezent über dem Gameplay genutzt). */
export const SpaceParticles: React.FC<{opacity?: number; starOpacity?: number}> = ({
  opacity = 1,
  starOpacity = 1,
}) => {
  const frame = useCurrentFrame();

  const stars = useMemo(
    () =>
      new Array(STAR_COUNT).fill(0).map((_, i) => ({
        x: random(`sx${i}`) * WIDTH,
        y: random(`sy${i}`) * HEIGHT,
        r: 0.6 + random(`sr${i}`) * 2.4,
        speed: 0.15 + random(`sv${i}`) * 0.9,
        phase: random(`sp${i}`) * Math.PI * 2,
        cyan: random(`sc${i}`) > 0.8,
      })),
    [],
  );

  const blocks = useMemo(
    () =>
      new Array(BLOCK_COUNT).fill(0).map((_, i) => ({
        x: random(`bx${i}`) * WIDTH,
        y: random(`by${i}`) * HEIGHT,
        size: 10 + Math.floor(random(`bs${i}`) * 4) * 6,
        speed: 0.6 + random(`bv${i}`) * 1.6,
        rot: random(`br${i}`) * 90,
        color: [COLORS.purple, COLORS.cyan, '#7c5cff', COLORS.gold][
          Math.floor(random(`bc${i}`) * 4)
        ],
        alpha: 0.25 + random(`ba${i}`) * 0.45,
      })),
    [],
  );

  return (
    <AbsoluteFill style={{opacity, pointerEvents: 'none'}}>
      <svg width={WIDTH} height={HEIGHT} style={{position: 'absolute', opacity: starOpacity}}>
        {stars.map((s, i) => {
          const y = (s.y + frame * s.speed) % HEIGHT;
          const tw = 0.35 + 0.65 * Math.abs(Math.sin(frame / 14 + s.phase));
          return (
            <circle
              key={i}
              cx={s.x}
              cy={y}
              r={s.r}
              fill={s.cyan ? COLORS.cyan : '#ffffff'}
              opacity={tw}
            />
          );
        })}
      </svg>
      {/* Pixel-Block-Partikel (Minecraft-Touch) */}
      {blocks.map((b, i) => {
        const y = (((b.y - frame * b.speed) % HEIGHT) + HEIGHT) % HEIGHT;
        return (
          <div
            key={i}
            style={{
              position: 'absolute',
              left: b.x,
              top: y,
              width: b.size,
              height: b.size,
              background: b.color,
              opacity: b.alpha,
              transform: `rotate(${b.rot + frame * 0.4}deg)`,
              boxShadow: `inset -${b.size / 4}px -${b.size / 4}px 0 rgba(0,0,0,0.35), 0 0 ${b.size}px ${b.color}`,
            }}
          />
        );
      })}
    </AbsoluteFill>
  );
};

export const Background: React.FC = () => {
  const frame = useCurrentFrame();

  // Nebel bewegt sich langsam
  const n1x = 30 + Math.sin(frame / 90) * 12;
  const n1y = 25 + Math.cos(frame / 120) * 8;
  const n2x = 70 + Math.cos(frame / 100) * 14;
  const n2y = 70 + Math.sin(frame / 80) * 10;
  const hue = interpolate(Math.sin(frame / 150), [-1, 1], [0, 25]);

  return (
    <AbsoluteFill style={{backgroundColor: COLORS.bg, overflow: 'hidden'}}>
      <AbsoluteFill
        style={{
          background: [
            `radial-gradient(ellipse 70% 45% at ${n1x}% ${n1y}%, rgba(140,40,255,0.55), transparent 70%)`,
            `radial-gradient(ellipse 65% 40% at ${n2x}% ${n2y}%, rgba(20,200,255,0.35), transparent 70%)`,
            `radial-gradient(ellipse 50% 30% at 50% 50%, rgba(255,60,200,0.18), transparent 75%)`,
            `linear-gradient(180deg, #070320 0%, #0b0530 45%, #04020c 100%)`,
          ].join(','),
          filter: `hue-rotate(${hue}deg)`,
        }}
      />
      <SpaceParticles />
      {/* Vignette */}
      <AbsoluteFill
        style={{
          background:
            'radial-gradient(ellipse 85% 70% at 50% 50%, transparent 55%, rgba(0,0,0,0.65) 100%)',
        }}
      />
    </AbsoluteFill>
  );
};
