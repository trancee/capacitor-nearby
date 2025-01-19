import { WebPlugin } from '@capacitor/core';

import type { Status, InitializeOptions, PublishOptions, NearbyPlugin } from './definitions';

export class NearbyWeb extends WebPlugin implements NearbyPlugin {
  async initialize(options: InitializeOptions): Promise<void> {
    console.info('initialize', options);
    throw this.unimplemented('Method not implemented.');
  }
  async reset(): Promise<void> {
    console.info('reset');
    throw this.unimplemented('Method not implemented.');
  }

  async publish(options: PublishOptions): Promise<void> {
    console.info('publish', options);
    throw this.unimplemented('Method not implemented.');
  }
  async unpublish(): Promise<void> {
    console.info('unpublish');
    throw this.unimplemented('Method not implemented.');
  }

  async subscribe(): Promise<void> {
    console.info('subscribe');
    throw this.unimplemented('Method not implemented.');
  }
  async unsubscribe(): Promise<void> {
    console.info('unsubscribe');
    throw this.unimplemented('Method not implemented.');
  }

  async status(): Promise<Status> {
    console.info('status');
    throw this.unimplemented('Method not implemented.');
  }
}
