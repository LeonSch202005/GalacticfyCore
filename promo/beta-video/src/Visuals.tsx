import React from 'react';
import {interpolate, spring, useCurrentFrame, useVideoConfig, Easing} from 'remotion';
import {
  CHAT,
  CHECKLIST,
  COLORS,
  DISCORD,
  FONT_HEAVY,
  FONT_PIXEL,
  SLOTS,
} from './config';
import {PixelIcon} from './PixelIcon';

type VProps = {duration: number};

const clamp = {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'} as const;

const usePop = (delay = 0, damping = 12) => {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();
  return spring({frame: frame - delay, fps, config: {damping, stiffness: 160, mass: 0.7}});
};

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
      style={{filter: `drop-shadow(0 0 40px ${COLORS.purple})`, overflow: 'visible'}}
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

/* ---------- 1: Intro ---------- */
export const IntroVisual: React.FC<VProps> = ({duration}) => {
  const frame = useCurrentFrame();
  const pop = usePop(0, 10);
  const progress = interpolate(frame, [8, duration * 0.6], [0, 73], {
    ...clamp,
    easing: Easing.out(Easing.cubic),
  });
  const blink = Math.floor(frame / 8) % 2 === 0;
  const segs = 14;
  const filled = Math.round((progress / 100) * segs);
  return (
    <div style={{display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 50}}>
      <div style={{transform: `scale(${pop}) rotate(${(1 - pop) * -20}deg)`}}>
        <PixelPlanet size={460} />
      </div>
      <div style={{display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 16}}>
        <div
          style={{
            display: 'flex',
            gap: 6,
            padding: 8,
            border: `6px solid ${COLORS.white}`,
            background: 'rgba(0,0,0,0.5)',
            boxShadow: `0 0 30px ${COLORS.purple}`,
          }}
        >
          {new Array(segs).fill(0).map((_, i) => (
            <div
              key={i}
              style={{
                width: 44,
                height: 44,
                background: i < filled ? (i % 2 ? COLORS.purple : COLORS.cyan) : 'rgba(255,255,255,0.08)',
                boxShadow: i < filled ? 'inset -6px -6px 0 rgba(0,0,0,0.3)' : undefined,
              }}
            />
          ))}
        </div>
        <div
          style={{
            fontFamily: FONT_PIXEL,
            fontWeight: 700,
            fontSize: 38,
            color: COLORS.gold,
            letterSpacing: 2,
            textShadow: '3px 3px 0 #3a2400',
          }}
        >
          SERVER LÄDT… {Math.round(progress)}%{blink ? '_' : ' '}
        </div>
      </div>
    </div>
  );
};

/* ---------- 2: 20 Betatester – Java & Bedrock ---------- */
const EditionBadge: React.FC<{label: string; color: string; from: number; delay: number}> = ({
  label,
  color,
  from,
  delay,
}) => {
  const p = usePop(delay, 13);
  return (
    <div
      style={{
        transform: `translateX(${(1 - p) * from}px)`,
        opacity: Math.min(1, p * 1.5),
        fontFamily: FONT_HEAVY,
        fontWeight: 900,
        fontSize: 52,
        color: '#fff',
        padding: '22px 34px',
        background: `linear-gradient(180deg, ${color}, rgba(0,0,0,0.5))`,
        border: `5px solid ${color}`,
        boxShadow: `0 0 34px ${color}, inset -8px -8px 0 rgba(0,0,0,0.3)`,
        letterSpacing: 1,
        textShadow: '0 4px 0 rgba(0,0,0,0.5)',
      }}
    >
      {label}
    </div>
  );
};

export const TestersVisual: React.FC<VProps> = () => {
  const frame = useCurrentFrame();
  const p = usePop(0, 9);
  const glow = 30 + Math.sin(frame / 5) * 12;
  return (
    <div style={{display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 20}}>
      <div
        style={{
          fontFamily: FONT_HEAVY,
          fontWeight: 900,
          fontSize: 400,
          lineHeight: 1,
          color: COLORS.white,
          transform: `scale(${p})`,
          textShadow: `0 0 ${glow}px ${COLORS.cyan}, 0 0 ${glow * 2}px ${COLORS.purple}, 12px 12px 0 ${COLORS.purpleDeep}`,
          WebkitTextStroke: `6px ${COLORS.cyan}`,
        }}
      >
        20
      </div>
      <div
        style={{
          fontFamily: FONT_PIXEL,
          fontWeight: 700,
          fontSize: 64,
          color: COLORS.gold,
          letterSpacing: 8,
          textShadow: '5px 5px 0 #3a2400',
          opacity: interpolate(frame, [6, 14], [0, 1], clamp),
        }}
      >
        BETATESTER
      </div>
      <div style={{display: 'flex', gap: 30, marginTop: 30}}>
        <EditionBadge label="JAVA" color="#e8762b" from={-700} delay={30} />
        <EditionBadge label="BEDROCK" color="#3aa655" from={700} delay={42} />
      </div>
    </div>
  );
};

/* ---------- 3: Checkliste ---------- */
export const ChecklistVisual: React.FC<VProps> = ({duration}) => {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();
  // Haken passend zum gesprochenen Satz verteilen
  const slot = (duration - 20) / CHECKLIST.length;
  return (
    <div style={{display: 'flex', flexDirection: 'column', gap: 28, width: 880}}>
      {CHECKLIST.map((item, i) => {
        const appear = spring({frame: frame - i * 5, fps, config: {damping: 14}});
        const checkAt = 10 + i * slot;
        const check = spring({frame: frame - checkAt, fps, config: {damping: 9, stiffness: 200}});
        const done = frame >= checkAt;
        return (
          <div
            key={item}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 34,
              padding: '26px 34px',
              transform: `translateX(${(1 - appear) * (i % 2 ? 600 : -600)}px)`,
              background: done ? 'rgba(46,242,255,0.14)' : 'rgba(10,5,30,0.7)',
              border: `5px solid ${done ? COLORS.cyan : 'rgba(180,77,255,0.6)'}`,
              boxShadow: done ? `0 0 30px rgba(46,242,255,0.5)` : 'none',
            }}
          >
            <div
              style={{
                width: 84,
                height: 84,
                background: '#1a1030',
                border: '5px solid #fff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxShadow: 'inset -6px -6px 0 rgba(0,0,0,0.5)',
                flexShrink: 0,
              }}
            >
              {done ? (
                <PixelIcon name="check" size={70 * check} glow="#3dff7a" />
              ) : null}
            </div>
            <div
              style={{
                fontFamily: FONT_HEAVY,
                fontWeight: 900,
                fontSize: 60,
                color: done ? COLORS.white : COLORS.muted,
              }}
            >
              {item}
            </div>
          </div>
        );
      })}
    </div>
  );
};

/* ---------- 4: Minecraft-Chat ---------- */
export const ChatVisual: React.FC<VProps> = () => {
  const frame = useCurrentFrame();
  const giftPop = usePop(0, 8);
  const box = usePop(6, 14);
  const line1 = frame >= 14;
  const line2 = frame >= 34;
  const typed = Math.max(0, Math.floor((frame - 50) / 1.6));
  const msg = CHAT.message.slice(0, typed);
  const caret = Math.floor(frame / 8) % 2 === 0 && typed < CHAT.message.length + 6;
  const shine = interpolate(frame % 45, [0, 45], [-120, 220]);
  const mcShadow = (c: string) => `4px 4px 0 ${c}`;
  return (
    <div style={{display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 46}}>
      <div style={{transform: `scale(${giftPop}) translateY(${Math.sin(frame / 7) * 10}px)`}}>
        <PixelIcon name="gift" size={230} glow={COLORS.gold} />
      </div>
      <div
        style={{
          width: 960,
          padding: '30px 34px',
          background: 'rgba(0,0,0,0.62)',
          transform: `scaleY(${box})`,
          fontFamily: FONT_PIXEL,
          fontWeight: 700,
          fontSize: 36,
          lineHeight: 1.45,
          color: '#fff',
          textShadow: mcShadow('#3f3f3f'),
          display: 'flex',
          flexDirection: 'column',
          gap: 10,
        }}
      >
        <div style={{opacity: line1 ? 1 : 0, color: '#55ff55', textShadow: mcShadow('#153f15')}}>
          ✔ {CHAT.systemLine}
        </div>
        <div style={{opacity: line2 ? 1 : 0}}>
          <span
            style={{
              position: 'relative',
              display: 'inline-block',
              verticalAlign: 'top',
              color: COLORS.gold,
              textShadow: `${mcShadow('#3f2a00')}, 0 0 18px ${COLORS.gold}`,
              overflow: 'hidden',
            }}
          >
            <span style={{color: COLORS.purple, textShadow: mcShadow('#2a0d4a')}}>[</span>
            {CHAT.prefix}
            <span style={{color: COLORS.purple, textShadow: mcShadow('#2a0d4a')}}>]</span>
            <span
              style={{
                position: 'absolute',
                top: 0,
                bottom: 0,
                left: `${shine}%`,
                width: '30%',
                background: 'linear-gradient(90deg, transparent, rgba(255,255,255,0.55), transparent)',
                transform: 'skewX(-20deg)',
              }}
            />
          </span>{' '}
          <span style={{color: '#fff'}}>{CHAT.player}</span>
          <span style={{color: '#aaa'}}>: </span>
          <span style={{color: '#fff'}}>
            {msg}
            {caret ? '_' : ''}
          </span>
        </div>
      </div>
    </div>
  );
};

/* ---------- 5: 20 Plätze ---------- */
export const SlotsVisual: React.FC<VProps> = ({duration}) => {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();
  const taken = Math.floor(
    interpolate(frame, [15, duration - 15], [0, SLOTS.total - SLOTS.countDownTo + 0.999], {
      ...clamp,
      easing: Easing.in(Easing.quad),
    }),
  );
  const free = SLOTS.total - taken;
  const low = free <= 6;
  const tick = spring({frame: frame - Math.floor(frame / 6) * 6, fps, config: {damping: 10}});
  const shake = low ? Math.sin(frame * 2.3) * 6 : 0;
  // Reihenfolge der befüllten Slots (deterministisch gemischt)
  const order = [7, 2, 13, 18, 0, 11, 5, 16, 9, 3, 14, 19, 6, 1, 12, 17, 8, 4, 15, 10];
  const takenSet = new Set(order.slice(0, taken));
  return (
    <div style={{display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 30}}>
      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          transform: `translateX(${shake}px)`,
        }}
      >
        <div
          style={{
            fontFamily: FONT_PIXEL,
            fontWeight: 700,
            fontSize: 48,
            letterSpacing: 6,
            color: low ? '#ff5577' : COLORS.cyan,
            textShadow: '4px 4px 0 rgba(0,0,0,0.6)',
          }}
        >
          {SLOTS.label}
        </div>
        <div
          style={{
            fontFamily: FONT_HEAVY,
            fontWeight: 900,
            fontSize: 200,
            lineHeight: 1,
            color: low ? '#ff3355' : COLORS.white,
            transform: `scale(${0.9 + 0.1 * tick})`,
            textShadow: `0 0 40px ${low ? '#ff3355' : COLORS.cyan}, 10px 10px 0 ${COLORS.purpleDeep}`,
            fontVariantNumeric: 'tabular-nums',
          }}
        >
          {free}
          <span style={{fontSize: 90, color: COLORS.muted, textShadow: 'none'}}>/{SLOTS.total}</span>
        </div>
      </div>
      {/* Inventar-Raster */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(5, 140px)',
          gap: 12,
          padding: 18,
          background: '#c6c6c6',
          border: '6px solid #fff',
          boxShadow: `inset -8px -8px 0 #555, 0 0 40px ${COLORS.purple}`,
        }}
      >
        {new Array(SLOTS.total).fill(0).map((_, i) => {
          const isTaken = takenSet.has(i);
          return (
            <div
              key={i}
              style={{
                width: 140,
                height: 96,
                background: '#8b8b8b',
                boxShadow: 'inset 6px 6px 0 #373737, inset -6px -6px 0 #fff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              {isTaken ? <PixelIcon name="head" size={66} /> : null}
            </div>
          );
        })}
      </div>
    </div>
  );
};

/* ---------- 6: Discord ---------- */
export const DiscordVisual: React.FC<VProps> = ({duration}) => {
  const frame = useCurrentFrame();
  const p = usePop(0, 9);
  const btn = usePop(14, 12);
  const clickAt = Math.round(duration * 0.55);
  const pressed = frame >= clickAt && frame < clickAt + 6;
  const clicked = frame >= clickAt;
  const cursorX = interpolate(frame, [20, clickAt], [420, 250], {...clamp, easing: Easing.inOut(Easing.cubic)});
  const cursorY = interpolate(frame, [20, clickAt], [300, 85], {...clamp, easing: Easing.inOut(Easing.cubic)});
  const rings = [0, 1, 2].map((i) => ((frame + i * 15) % 45) / 45);
  const blurple = '#5865F2';
  return (
    <div style={{display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 60}}>
      <div style={{position: 'relative', width: 380, height: 380, transform: `scale(${p})`}}>
        {rings.map((r, i) => (
          <div
            key={i}
            style={{
              position: 'absolute',
              inset: 0,
              border: `8px solid ${COLORS.cyan}`,
              transform: `scale(${0.8 + r * 0.7})`,
              opacity: 1 - r,
            }}
          />
        ))}
        {/* Chat-Bubble-Icon */}
        <svg viewBox="0 0 16 16" width={380} height={380} shapeRendering="crispEdges" style={{position: 'absolute', inset: 0, filter: `drop-shadow(0 0 40px ${blurple})`}}>
          <rect x={1} y={2} width={14} height={10} fill={blurple} />
          <rect x={2} y={1} width={12} height={1} fill={blurple} />
          <rect x={2} y={12} width={12} height={1} fill={blurple} />
          <rect x={3} y={13} width={3} height={2} fill={blurple} />
          <rect x={1} y={11} width={14} height={1} fill="rgba(0,0,0,0.25)" />
          <rect x={4} y={6} width={2} height={3} fill="#fff" />
          <rect x={10} y={6} width={2} height={3} fill="#fff" />
          <rect x={5} y={10} width={6} height={1} fill="#fff" />
        </svg>
      </div>
      <div style={{position: 'relative'}}>
        <div
          style={{
            transform: `scale(${btn * (pressed ? 0.92 : 1)})`,
            fontFamily: FONT_HEAVY,
            fontWeight: 900,
            fontSize: 64,
            color: '#fff',
            padding: '30px 60px',
            background: clicked ? '#3dbb6a' : blurple,
            border: '6px solid #fff',
            boxShadow: `inset -10px -10px 0 rgba(0,0,0,0.3), 0 0 40px ${clicked ? '#3dff7a' : blurple}`,
            display: 'flex',
            alignItems: 'center',
            gap: 22,
          }}
        >
          {clicked ? <PixelIcon name="check" size={64} /> : null}
          {DISCORD.button}
        </div>
        <div style={{position: 'absolute', left: '50%', top: 0, transform: `translate(${cursorX}px, ${cursorY}px)`}}>
          <PixelIcon name="cursor" size={90} />
        </div>
      </div>
    </div>
  );
};
