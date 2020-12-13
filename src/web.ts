import { WebPlugin } from '@capacitor/core';
import { CapacitorNearbyPlugin } from './definitions';

export class CapacitorNearbyWeb extends WebPlugin implements CapacitorNearbyPlugin {
  constructor() {
    super({
      name: 'CapacitorNearby',
      platforms: ['web'],
    });
  }

  async initialize(): Promise<void> {
    console.error('initialize');
    return;
  }

  async advertise(): Promise<void> {
    console.error('advertise');
    return;
  }
  async scan(): Promise<void> {
    console.error('scan');
    return;
  }
}

const CapacitorNearby = new CapacitorNearbyWeb();

export { CapacitorNearby };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(CapacitorNearby);
