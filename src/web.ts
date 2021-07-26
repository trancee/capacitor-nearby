import { WebPlugin } from '@capacitor/core';

import type {
  Message,
  Status,
  UUID,
  InitializeOptions,
  PublishOptions,
  SubscribeOptions,
  NearbyPlugin,
} from './definitions';

export class NearbyWeb extends WebPlugin implements NearbyPlugin {
  async initialize(options: {
    // A InitializeOptions object for this operation
    options?: InitializeOptions;
  }): Promise<void> {
    console.error('initialize', options);
    throw this.unimplemented('Method not implemented.');
  }
  async reset(): Promise<void> {
    console.error('reset');
    throw this.unimplemented('Method not implemented.');
  }

  async publish(options: {
    // A Message to publish for nearby devices to see
    message: Message;
    // A PublishOptions object for this operation
    options?: PublishOptions;
  }): Promise<void> {
    console.error('publish', options);
    throw this.unimplemented('Method not implemented.');
  }
  // Cancels an existing published message.
  async unpublish(options: { uuid?: UUID }): Promise<void> {
    console.error('unpublish', options);
    throw this.unimplemented('Method not implemented.');
  }

  async subscribe(options: {
    // A SubscribeOptions object for this operation
    options?: SubscribeOptions;
  }): Promise<void> {
    console.error('subscribe', options);
    throw this.unimplemented('Method not implemented.');
  }
  // Cancels an existing subscription.
  async unsubscribe(options: unknown): Promise<void> {
    console.error('unsubscribe', options);
    throw this.unimplemented('Method not implemented.');
  }

  async status(): Promise<Status> {
    console.error('status');
    throw this.unimplemented('Method not implemented.');
  }
}

const Nearby = new NearbyWeb();

export { Nearby };
