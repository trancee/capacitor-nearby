import { registerPlugin } from '@capacitor/core';

import type { NearbyPlugin } from './definitions';

const Nearby = registerPlugin<NearbyPlugin>('Nearby', {
  web: () => import('./web').then(m => new m.NearbyWeb()),
});

export * from './definitions';
export { Nearby };
