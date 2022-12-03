import { WebPlugin } from '@capacitor/core';

import type { NearbyPlugin } from './definitions';

export class NearbyWeb extends WebPlugin implements NearbyPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
