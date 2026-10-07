import React from 'react';
import {interpolate, spring, useCurrentFrame, useVideoConfig} from 'remotion';
import {COLORS, FONT_HEAVY} from './config';

export type Piece = {text: string; highlight: boolean};
/** Ein Wort kann aus mehreren Teilen bestehen (z. B. "**Bedrock**!"). */
export type Word = {pieces: Piece[]; highlight: boolean};

/** Zerlegt "foo **bar baz**! qux" in Wörter mit Highlight-Flag. */
export const parseRich = (text: string): Word[] => {
  const words: Word[] = [];
  let glue = false; // hängt der nächste Teil direkt am vorherigen Wort?
  text.split('**').forEach((part, i) => {
    const highlight = i % 2 === 1;
    const tokens = part.split(/(\s+)/);
    tokens.forEach((tok) => {
      if (tok === '') return;
      if (/^\s+$/.test(tok)) {
        glue = false;
        return;
      }
      const piece = {text: tok, highlight};
      if (glue && words.length) {
        const w = words[words.length - 1];
        w.pieces.push(piece);
        w.highlight = w.highlight || highlight;
      } else {
        words.push({pieces: [piece], highlight});
      }
      glue = true;
    });
  });
  return words;
};

const renderPieces = (w: Word) =>
  w.pieces.map((p, j) => (
    <span key={j} style={p.highlight ? highlightStyle : undefined}>
      {p.text}
    </span>
  ));

export const highlightStyle: React.CSSProperties = {
  color: COLORS.cyan,
  textShadow: `0 0 18px ${COLORS.cyan}, 0 0 42px ${COLORS.purple}, 0 6px 0 ${COLORS.purpleDeep}`,
};

/** Statischer Rich-Text (für End-Card). */
export const StaticRich: React.FC<{text: string; style?: React.CSSProperties}> = ({
  text,
  style,
}) => {
  const words = parseRich(text);
  return (
    <span style={style}>
      {words.map((w, i) => (
        <span key={i}>
          {renderPieces(w)}
          {i < words.length - 1 ? ' ' : ''}
        </span>
      ))}
    </span>
  );
};

/**
 * Kinetische Caption: Wörter ploppen nacheinander rein.
 * revealFrames = in wie vielen Frames alle Wörter sichtbar sind.
 */
export const KineticCaption: React.FC<{
  text: string;
  revealFrames: number;
  fontSize: number;
}> = ({text, revealFrames, fontSize}) => {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();
  const words = parseRich(text);
  const step = revealFrames / Math.max(1, words.length);

  return (
    <div
      style={{
        fontFamily: FONT_HEAVY,
        fontWeight: 900,
        fontSize,
        lineHeight: 1.16,
        color: COLORS.white,
        textAlign: 'center',
        letterSpacing: -1,
        textShadow: '0 0 4px rgba(0,0,0,0.9), 0 4px 0 rgba(0,0,0,0.7), 0 0 26px rgba(0,0,0,0.85)',
        display: 'flex',
        flexWrap: 'wrap',
        justifyContent: 'center',
        columnGap: fontSize * 0.26,
        rowGap: fontSize * 0.08,
      }}
    >
      {words.map((w, i) => {
        const start = i * step;
        const s = spring({
          frame: frame - start,
          fps,
          config: {damping: 11, stiffness: 180, mass: 0.6},
        });
        const opacity = interpolate(frame - start, [0, 4], [0, 1], {
          extrapolateLeft: 'clamp',
          extrapolateRight: 'clamp',
        });
        // Highlight-Wörter bekommen einen extra "Punch"
        const punch = w.highlight
          ? 1 +
            0.18 *
              Math.max(0, Math.sin(Math.min(Math.PI, ((frame - start) / 14) * Math.PI)))
          : 1;
        const y = interpolate(s, [0, 1], [40, 0]);
        return (
          <span
            key={i}
            style={{
              display: 'inline-block',
              opacity,
              transform: `translateY(${y}px) scale(${(0.4 + 0.6 * s) * punch})`,
            }}
          >
            {renderPieces(w)}
          </span>
        );
      })}
    </div>
  );
};
