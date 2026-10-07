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
  const progress = interpolate(frame, [8, duration * 0.6], [0, 73], {
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
          SERVER LÄDT… {Math.round(progress)}%{blink ? '_' : ' '}
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
        fontSize: 40,
        color: '#fff',
        padding: '14px 24px',
        background: `linear-gradient(180deg, ${color}, rgba(0,0,0,0.5))`,
        border: `4px solid ${color}`,
        boxShadow: `0 0 24px ${color}, inset -6px -6px 0 rgba(0,0,0,0.3)`,
        letterSpacing: 1,
        textShadow: '0 3px 0 rgba(0,0,0,0.5)',
      }}
    >
      {label}
    </div>
  );
};

export const TestersVisual: React.FC<VProps> = () => {
  const frame = useCurrentFrame();
  const panel = usePop(0, 13);
  const p = usePop(3, 9);
  const glow = 22 + Math.sin(frame / 5) * 9;
  return (
    <Glass
      accent={COLORS.cyan}
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 34,
        padding: '18px 38px 22px 30px',
        transform: `scale(${0.85 + 0.15 * panel})`,
        opacity: Math.min(1, panel * 1.4),
      }}
    >
      <div
        style={{
          fontFamily: FONT_HEAVY,
          fontWeight: 900,
          fontSize: 230,
          lineHeight: 1,
          color: COLORS.white,
          transform: `scale(${p})`,
          textShadow: `0 0 ${glow}px ${COLORS.cyan}, 0 0 ${glow * 2}px ${COLORS.purple}, 9px 9px 0 ${COLORS.purpleDeep}`,
          WebkitTextStroke: `5px ${COLORS.cyan}`,
        }}
      >
        20
      </div>
      <div style={{display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 22}}>
        <div
          style={{
            fontFamily: FONT_PIXEL,
            fontWeight: 700,
            fontSize: 54,
            color: COLORS.gold,
            letterSpacing: 4,
            textShadow: '4px 4px 0 #3a2400',
            opacity: interpolate(frame, [6, 14], [0, 1], clamp),
          }}
        >
          BETATESTER
        </div>
        <div style={{display: 'flex', gap: 20}}>
          <EditionBadge label="JAVA" color="#e8762b" from={-500} delay={26} />
          <EditionBadge label="BEDROCK" color="#3aa655" from={500} delay={36} />
        </div>
      </div>
    </Glass>
  );
};

/* ---------- 3: Checkliste ---------- */
export const ChecklistVisual: React.FC<VProps> = ({duration}) => {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();
  const panel = usePop(0, 14);
  // Haken passend zum gesprochenen Satz verteilen
  const slot = (duration - 20) / CHECKLIST.length;
  return (
    <Glass
      style={{
        display: 'flex',
        flexDirection: 'column',
        gap: 14,
        width: 820,
        padding: '22px 26px',
        transform: `scaleY(${0.6 + 0.4 * panel})`,
        transformOrigin: 'top center',
        opacity: Math.min(1, panel * 1.5),
      }}
    >
      {CHECKLIST.map((item, i) => {
        const appear = spring({frame: frame - 4 - i * 5, fps, config: {damping: 14}});
        const checkAt = 10 + i * slot;
        const check = spring({frame: frame - checkAt, fps, config: {damping: 9, stiffness: 200}});
        const done = frame >= checkAt;
        return (
          <div
            key={item}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 28,
              padding: '12px 20px',
              transform: `translateX(${(1 - appear) * (i % 2 ? 500 : -500)}px)`,
              opacity: Math.min(1, appear * 1.5),
              background: done ? 'rgba(46,242,255,0.16)' : 'rgba(255,255,255,0.04)',
              border: `4px solid ${done ? COLORS.cyan : 'rgba(180,77,255,0.45)'}`,
              boxShadow: done ? `0 0 22px rgba(46,242,255,0.45)` : 'none',
            }}
          >
            <div
              style={{
                width: 64,
                height: 64,
                background: '#1a1030',
                border: '4px solid #fff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxShadow: 'inset -5px -5px 0 rgba(0,0,0,0.5)',
                flexShrink: 0,
              }}
            >
              {done ? <PixelIcon name="check" size={54 * check} glow="#3dff7a" /> : null}
            </div>
            <div
              style={{
                fontFamily: FONT_HEAVY,
                fontWeight: 900,
                fontSize: 48,
                color: done ? COLORS.white : COLORS.muted,
                textShadow: '0 3px 0 rgba(0,0,0,0.6)',
              }}
            >
              {item}
            </div>
          </div>
        );
      })}
    </Glass>
  );
};

/* ---------- 4: Minecraft-Chat ---------- */
export const ChatVisual: React.FC<VProps> = () => {
  const frame = useCurrentFrame();
  const giftPop = usePop(4, 8);
  const box = usePop(0, 14);
  const line1 = frame >= 10;
  const line2 = frame >= 28;
  const typed = Math.max(0, Math.floor((frame - 42) / 1.6));
  const msg = CHAT.message.slice(0, typed);
  const caret = Math.floor(frame / 8) % 2 === 0 && typed < CHAT.message.length + 6;
  const shine = interpolate(frame % 45, [0, 45], [-120, 220]);
  const mcShadow = (c: string) => `4px 4px 0 ${c}`;
  return (
    <div style={{position: 'relative', width: 980}}>
      <div
        style={{
          position: 'absolute',
          right: 10,
          top: -150,
          transform: `scale(${giftPop}) translateY(${Math.sin(frame / 7) * 8}px) rotate(${Math.sin(frame / 11) * 6}deg)`,
        }}
      >
        <PixelIcon name="gift" size={150} glow={COLORS.gold} />
      </div>
      <div
        style={{
          padding: '24px 30px',
          background: 'rgba(0,0,0,0.6)',
          borderLeft: `6px solid ${COLORS.purple}`,
          boxShadow: `0 0 26px rgba(180,77,255,0.35)`,
          backdropFilter: 'blur(6px)',
          transform: `scaleY(${box})`,
          transformOrigin: 'bottom center',
          fontFamily: FONT_PIXEL,
          fontWeight: 700,
          fontSize: 35,
          lineHeight: 1.45,
          color: '#fff',
          textShadow: mcShadow('#3f3f3f'),
          display: 'flex',
          flexDirection: 'column',
          gap: 8,
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
  const panel = usePop(0, 13);
  const taken = Math.floor(
    interpolate(frame, [15, duration - 15], [0, SLOTS.total - SLOTS.countDownTo + 0.999], {
      ...clamp,
      easing: Easing.in(Easing.quad),
    }),
  );
  const free = SLOTS.total - taken;
  const low = free <= 6;
  const tick = spring({frame: frame - Math.floor(frame / 6) * 6, fps, config: {damping: 10}});
  const shake = low ? Math.sin(frame * 2.3) * 5 : 0;
  // Reihenfolge der befüllten Slots (deterministisch gemischt)
  const order = [7, 2, 13, 18, 0, 11, 5, 16, 9, 3, 14, 19, 6, 1, 12, 17, 8, 4, 15, 10];
  const takenSet = new Set(order.slice(0, taken));
  const accent = low ? '#ff3355' : COLORS.cyan;
  return (
    <Glass
      accent={accent}
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 14,
        padding: '16px 24px 24px',
        transform: `translateY(${(1 - panel) * -50}px) translateX(${shake}px)`,
        opacity: Math.min(1, panel * 1.4),
      }}
    >
      <div style={{display: 'flex', alignItems: 'center', gap: 30}}>
        <div
          style={{
            fontFamily: FONT_HEAVY,
            fontWeight: 900,
            fontSize: 150,
            lineHeight: 1,
            color: low ? '#ff3355' : COLORS.white,
            transform: `scale(${0.9 + 0.1 * tick})`,
            textShadow: `0 0 34px ${accent}, 8px 8px 0 ${COLORS.purpleDeep}`,
            fontVariantNumeric: 'tabular-nums',
          }}
        >
          {free}
          <span style={{fontSize: 70, color: COLORS.muted, textShadow: '0 4px 0 rgba(0,0,0,0.6)'}}>
            /{SLOTS.total}
          </span>
        </div>
        <div
          style={{
            fontFamily: FONT_PIXEL,
            fontWeight: 700,
            fontSize: 44,
            lineHeight: 1.25,
            letterSpacing: 4,
            color: low ? '#ff5577' : COLORS.cyan,
            textShadow: '4px 4px 0 rgba(0,0,0,0.6)',
            maxWidth: 360,
          }}
        >
          {SLOTS.label}
        </div>
      </div>
      {/* Inventar-Raster (2 x 10 wie zwei Hotbars) */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(10, 76px)',
          gap: 6,
          padding: 10,
          background: 'rgba(198,198,198,0.88)',
          border: '5px solid #fff',
          boxShadow: `inset -6px -6px 0 #555, 0 0 26px ${COLORS.purple}`,
        }}
      >
        {new Array(SLOTS.total).fill(0).map((_, i) => {
          const isTaken = takenSet.has(i);
          return (
            <div
              key={i}
              style={{
                width: 76,
                height: 68,
                background: '#8b8b8b',
                boxShadow: 'inset 5px 5px 0 #373737, inset -5px -5px 0 #fff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              {isTaken ? <PixelIcon name="head" size={48} /> : null}
            </div>
          );
        })}
      </div>
    </Glass>
  );
};

/* ---------- 6: Discord ---------- */
export const DiscordVisual: React.FC<VProps> = ({duration}) => {
  const frame = useCurrentFrame();
  const p = usePop(0, 9);
  const btn = usePop(10, 12);
  const clickAt = Math.round(duration * 0.5);
  const pressed = frame >= clickAt && frame < clickAt + 6;
  const clicked = frame >= clickAt;
  const cursorX = interpolate(frame, [16, clickAt], [330, 150], {...clamp, easing: Easing.inOut(Easing.cubic)});
  const cursorY = interpolate(frame, [16, clickAt], [230, 70], {...clamp, easing: Easing.inOut(Easing.cubic)});
  const rings = [0, 1, 2].map((i) => ((frame + i * 15) % 45) / 45);
  const blurple = '#5865F2';
  const size = 210;
  return (
    <div style={{display: 'flex', alignItems: 'center', gap: 46}}>
      <div style={{position: 'relative', width: size, height: size, transform: `scale(${p})`}}>
        {rings.map((r, i) => (
          <div
            key={i}
            style={{
              position: 'absolute',
              inset: 0,
              border: `6px solid ${COLORS.cyan}`,
              transform: `scale(${0.8 + r * 0.7})`,
              opacity: 1 - r,
            }}
          />
        ))}
        {/* Chat-Bubble-Icon */}
        <svg viewBox="0 0 16 16" width={size} height={size} shapeRendering="crispEdges" style={{position: 'absolute', inset: 0, filter: `drop-shadow(0 0 24px ${blurple})`}}>
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
            fontSize: 56,
            color: '#fff',
            padding: '26px 44px',
            background: clicked ? '#3dbb6a' : blurple,
            border: '6px solid #fff',
            boxShadow: `inset -9px -9px 0 rgba(0,0,0,0.3), 0 0 40px ${clicked ? '#3dff7a' : blurple}, 0 12px 30px rgba(0,0,0,0.5)`,
            display: 'flex',
            alignItems: 'center',
            gap: 18,
            whiteSpace: 'nowrap',
          }}
        >
          {clicked ? <PixelIcon name="check" size={56} /> : null}
          {DISCORD.button}
        </div>
        <div style={{position: 'absolute', left: '50%', top: 0, transform: `translate(${cursorX}px, ${cursorY}px)`}}>
          <PixelIcon name="cursor" size={80} />
        </div>
      </div>
    </div>
  );
};
