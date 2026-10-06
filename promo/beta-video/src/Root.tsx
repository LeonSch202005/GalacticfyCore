import React from 'react';
import {Composition} from 'remotion';
import {GalacticfyBeta} from './GalacticfyBeta';
import {FPS, HEIGHT, TOTAL_FRAMES, WIDTH} from './config';

export const RemotionRoot: React.FC = () => {
  return (
    <Composition
      id="GalacticfyBeta"
      component={GalacticfyBeta}
      durationInFrames={TOTAL_FRAMES}
      fps={FPS}
      width={WIDTH}
      height={HEIGHT}
    />
  );
};
