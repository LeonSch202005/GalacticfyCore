import React from 'react';
import {interpolate, spring, useCurrentFrame, useVideoConfig} from 'remotion';
import {COLORS, Cue, FONT_HEAVY, sec} from './config';

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

/** In wie vielen Frames alle Wörter einer Caption sichtbar sind (duration = Segment-Frames). */
export const captionRevealFrames = (duration: number) => Math.min(duration * 0.6, duration - 20);

const normWord = (s: string) => s.toLowerCase().replace(/[^\p{L}\p{N}-]/gu, '');

/**
 * Frame (ab Segmentstart), in dem das Wort `word` der Caption reinploppt – gleiche Rechnung
 * wie in KineticCaption. Satzzeichen und Groß-/Kleinschreibung zählen nicht ("bedrock" findet
 * "**Bedrock**!"). Steht das Wort nicht (mehr) in der Caption, gibt es einen klaren Fehler.
 */
export const captionWordFrame = (text: string, duration: number, word: string): number => {
  const words = parseRich(text);
  const step = captionRevealFrames(duration) / Math.max(1, words.length);
  const target = normWord(word);
  const i = words.findIndex((w) => normWord(w.pieces.map((p) => p.text).join('')) === target);
  if (i < 0) {
    throw new Error(
      `Stichwort "${word}" steht nicht in der Caption "${text}" – in src/config.ts (BRAND) anpassen.`,
    );
  }
  return i * step;
};

/** Frame (ab Segmentstart) eines Cue: Wort der Caption (+ offset) oder feste Zeit. */
export const cueFrame = (cue: Cue, text: string, duration: number): number =>
  'at' in cue ? sec(cue.at) : Math.round(captionWordFrame(text, duration, cue.word) + sec(cue.offset ?? 0));

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
  const columnGap = fontSize * 0.26;

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
        columnGap,
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
        // Wörter dürfen beim Reinploppen nur so weit über 100 % wachsen, dass sie pro Seite
        // höchstens ~22 % des Wortabstands einnehmen (Breite grob geschätzt) – sonst liest
        // man kurz "AlsDankeschön", "nichtfertig" oder "unserenDiscordund".
        const chars = w.pieces.reduce((n, p) => n + p.text.length, 0);
        const estWidth = Math.max(1, chars) * fontSize * 0.6;
        const maxGrow = Math.min(0.1, (2 * columnGap * 0.22) / estWidth);
        // Highlight-Wörter: kein Überschwingen, dafür ein "Punch" (kurz größer + leicht nach oben)
        const base = Math.min(0.4 + 0.6 * s, w.highlight ? 1 : 1 + maxGrow);
        const punchT = w.highlight
          ? Math.max(0, Math.sin(Math.min(Math.PI, ((frame - start) / 14) * Math.PI)))
          : 0;
        const punch = 1 + maxGrow * punchT;
        const lift = -fontSize * 0.06 * punchT;
        const y = interpolate(s, [0, 1], [40, 0]) + lift;
        return (
          <span
            key={i}
            style={{
              display: 'inline-block',
              opacity,
              transform: `translateY(${y}px) scale(${base * punch})`,
            }}
          >
            {renderPieces(w)}
          </span>
        );
      })}
    </div>
  );
};
