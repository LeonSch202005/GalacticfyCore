import React from 'react';
import {COLORS} from './config';

// Pixel-Art-Icons (statt Emoji – headless Chromium hat keine Emoji-Schrift).
// Jedes Zeichen = 1 Pixel. "." = transparent.

const PALETTE: Record<string, string> = {
  W: '#ffffff',
  C: COLORS.cyan,
  P: COLORS.purple,
  D: COLORS.purpleDeep,
  Y: COLORS.gold,
  O: '#ff7a1a',
  R: '#ff3355',
  G: '#3dff7a',
  K: '#1a1030',
  S: '#c68c5a', // Haut
  B: '#4a2d1a', // Haare
  T: '#2fb5c9', // Shirt
  E: '#3b2a8a', // Augen
};

export const ICONS = {
  rocket: [
    '.....WW.....',
    '....WCCW....',
    '....WCCW....',
    '...WWWWWW...',
    '...WPPPPW...',
    '...WPCCPW...',
    '...WPCCPW...',
    '...WPPPPW...',
    '..PWWWWWWP..',
    '.PP.WWWW.PP.',
    '.P..OYYO..P.',
    '.....OO.....',
  ],
  gamepad: [
    '............',
    '............',
    '.PPPPPPPPPP.',
    'PPPPPPPPPPPP',
    'PPWPPPPPPCPP',
    'PWWWPPPPCPCP',
    'PPWPPPPPPCPP',
    'PPPPPPPPPPPP',
    'PPPP....PPPP',
    '.PP......PP.',
    '............',
    '............',
  ],
  gift: [
    '...Y....Y...',
    '....Y..Y....',
    '.....YY.....',
    'PPPPPYYPPPPP',
    'PPPPPYYPPPPP',
    'YYYYYYYYYYYY',
    '.DPPPYYPPPD.',
    '.DPPPYYPPPD.',
    '.DPPPYYPPPD.',
    '.DPPPYYPPPD.',
    '.DDDDYYDDDD.',
    '............',
  ],
  pointer: [
    '............',
    '......C.....',
    '......CC....',
    '......CCC...',
    'CCCCCCCCCC..',
    'CCCCCCCCCCC.',
    'CCCCCCCCCCC.',
    'CCCCCCCCCC..',
    '......CCC...',
    '......CC....',
    '......C.....',
    '............',
  ],
  check: [
    '............',
    '..........G.',
    '.........GG.',
    '........GG..',
    '.......GG...',
    '.G....GG....',
    '.GG..GG.....',
    '..GGGG......',
    '...GG.......',
    '............',
  ],
  head: [
    'BBBBBBBB',
    'BBBBBBBB',
    'BSSSSSSB',
    'SSSSSSSS',
    'SWESSEWS',
    'SSSBBSSS',
    'SSBSSBSS',
    'SSBBBBSS',
  ],
  arrowUp: [
    '.....CC.....',
    '....CCCC....',
    '...CCCCCC...',
    '..CCCCCCCC..',
    '.CCC.CC.CCC.',
    '.....CC.....',
    '.....CC.....',
    '.....CC.....',
    '.....CC.....',
    '.....CC.....',
  ],
  cursor: [
    'K.........',
    'KK........',
    'KWK.......',
    'KWWK......',
    'KWWWK.....',
    'KWWWWK....',
    'KWWWWWK...',
    'KWWWWWWK..',
    'KWWWWKKKK.',
    'KWKWWK....',
    'KK.KWWK...',
    '....KWK...',
    '....KK....',
  ],
  bug: [
    '...W....W...',
    '....W..W....',
    '...WKKKKW...',
    '..KKYKKYKK..',
    'W.KKKKKKKK.W',
    '.WRRRWWRRRW.',
    '..RRRWWRRR..',
    'W.RKRWWRKR.W',
    '.WRRRWWRRRW.',
    '..RRKWWKRR..',
    'W..RRWWRR..W',
    '....RRRR....',
  ],
} as const;

export type IconName = keyof typeof ICONS;

export const PixelIcon: React.FC<{
  name: IconName;
  size: number;
  glow?: string;
  style?: React.CSSProperties;
}> = ({name, size, glow, style}) => {
  const grid = ICONS[name];
  const rows = grid.length;
  const cols = grid[0].length;
  const px = size / Math.max(rows, cols);
  return (
    <svg
      width={cols * px}
      height={rows * px}
      viewBox={`0 0 ${cols} ${rows}`}
      shapeRendering="crispEdges"
      style={{
        filter: glow ? `drop-shadow(0 0 ${size / 8}px ${glow})` : undefined,
        flexShrink: 0,
        ...style,
      }}
    >
      {grid.flatMap((row, y) =>
        row.split('').map((ch, x) =>
          ch === '.' ? null : (
            <rect key={`${x}-${y}`} x={x} y={y} width={1.02} height={1.02} fill={PALETTE[ch]} />
          ),
        ),
      )}
    </svg>
  );
};
