import { WebPlugin } from '@capacitor/core';

import type {
  Message,
  Status,
  InitializeOptions,
  PublishOptions,
  SubscribeOptions,
  NearbyPlugin,
  UUID,
} from './definitions';

export class NearbyWeb extends WebPlugin implements NearbyPlugin {
  serviceUUID: UUID | undefined = undefined;
  uuids: UUID[] = [];

  scan: BluetoothLEScan | undefined = undefined;

  async initialize(options: {
    // A InitializeOptions object for this operation
    options?: InitializeOptions;
  }): Promise<void> {
    console.info('initialize', options);

    if (typeof navigator === 'undefined' || !navigator.bluetooth) {
      throw this.unavailable('Bluetooth not available.');
    }

    const isAvailable = await navigator.bluetooth.getAvailability();
    if (!isAvailable) {
      throw this.unavailable('Bluetooth not available.');
    }

    if (!options.options?.serviceUUID) {
      throw this.unavailable('UUID not found');
    }

    this.serviceUUID = options.options?.serviceUUID;
    if (!this.serviceUUID.startsWith('0x')) {
      if (this.serviceUUID.length === 4) this.serviceUUID = '0000' + this.serviceUUID;
      if (this.serviceUUID.length === 8) this.serviceUUID += '-' + '0000-1000-8000-00805f9b34fb';
    }

    navigator.bluetooth.addEventListener('availabilitychanged', (event) => {
      console.info('bluetooth::availabilitychanged', event);
    });

    this.scan = undefined;
    this.uuids = [];
  }
  async reset(): Promise<void> {
    console.info('reset');

    this.unsubscribe();

    this.scan = undefined;
    this.uuids = [];
  }

  async publish(options: {
    // A Message to publish for nearby devices to see
    message: Message;
    // A PublishOptions object for this operation
    options?: PublishOptions;
  }): Promise<void> {
    console.error('publish', options);
    // throw this.unimplemented('Method not implemented.');
  }
  // Cancels an existing published message.
  async unpublish(): Promise<void> {
    console.error('unpublish');
    // throw this.unimplemented('Method not implemented.');
  }

  async subscribe(options: {
    // A SubscribeOptions object for this operation
    options?: SubscribeOptions;
  }): Promise<void> {
    console.info('subscribe', options);

    if (typeof navigator === 'undefined' || !navigator.bluetooth) {
      throw this.unavailable('Bluetooth not available.');
    }

    this.scan = await navigator.bluetooth.requestLEScan({
      filters: [
        {
          services: [this.serviceUUID!],
        },
      ],
    });

    navigator.bluetooth.addEventListener('advertisementreceived', (event) => {
      console.info('bluetooth::advertisementreceived', event);
    });
  }
  // Cancels an existing subscription.
  async unsubscribe(): Promise<void> {
    console.info('unsubscribe');

    if (this.scan?.active) {
      this.scan?.stop();
    }
    this.scan = undefined;
  }

  async status(): Promise<Status> {
    console.info('status');

    return {
      isPublishing: false,
      isSubscribing: this.scan?.active ?? false,
      uuids: this.uuids,
    };
  }
}
