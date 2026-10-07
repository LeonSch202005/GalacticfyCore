import React from 'react';
import {Composition} from 'remotion';
import {GalacticfyBeta} from './GalacticfyBeta';
import {FPS, HEIGHT, TOTAL_FRAMES, WIDTH} from './config';

export const RemotionRoot: React.FC = () => {
  return (
    <>
      {/* Hauptvideo: Stimme + Musik + Sound-Effekte */}
      <Composition
        id="GalacticfyBeta"
        component={GalacticfyBeta}
        durationInFrames={TOTAL_FRAMES}
        fps={FPS}
        width={WIDTH}
        height={HEIGHT}
        defaultProps={{music: true, masterGain: 1}}
      />
      {/* gleiches Video ohne Musik (Stimme + Sound-Effekte) */}
      <Composition
        id="GalacticfyBeta-OhneMusik"
        component={GalacticfyBeta}
        durationInFrames={TOTAL_FRAMES}
        fps={FPS}
        width={WIDTH}
        height={HEIGHT}
        defaultProps={{music: false, masterGain: 1}}
      />
    </>
  );
};
