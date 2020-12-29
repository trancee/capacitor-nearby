import { WebPlugin } from '@capacitor/core';
import { CapacitorNearbyPlugin } from './definitions';

import {
  Status,
  PublishOptions,
  PublishResult,
  SubscribeOptions,
} from './definitions';

export class CapacitorNearbyWeb extends WebPlugin implements CapacitorNearbyPlugin {
  constructor() {
    super({
      name: 'CapacitorNearby',
      platforms: ['web'],
    });
  }

  async initialize(): Promise<void> {
    console.error('initialize');
    throw new Error("Method not implemented.");
  }
  async reset(): Promise<void> {
    console.error('reset');
    throw new Error("Method not implemented.");
  }

  async publish(options: {
    // A Message to publish for nearby devices to see
    message: string,
    // A PublishOptions object for this operation
    options?: PublishOptions,
  }): Promise<PublishResult> {
    console.error('publish', options);
    throw new Error("Method not implemented.");
  }

  // Cancels an existing published message.
  async unpublish(options: {
    uuid?: string,
  }): Promise<void> {
    console.error('unpublish', options);
    throw new Error("Method not implemented.");
  }

  async subscribe(options: {
    // A SubscribeOptions object for this operation
    options?: SubscribeOptions,
  }): Promise<void> {
    console.error('subscribe', options);
    throw new Error("Method not implemented.");
  }

  // Cancels an existing subscription.
  async unsubscribe(options: {
  }): Promise<void> {
    console.error('unsubscribe', options);
    throw new Error("Method not implemented.");
  }

  async pause(): Promise<void> {
    console.error('pause');
    throw new Error("Method not implemented.");
  }
  async resume(): Promise<void> {
    console.error('resume');
    throw new Error("Method not implemented.");
  }

  async status(): Promise<Status> {
    console.error('status');
    throw new Error("Method not implemented.");
  }
}

const CapacitorNearby = new CapacitorNearbyWeb();

export { CapacitorNearby };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(CapacitorNearby);
