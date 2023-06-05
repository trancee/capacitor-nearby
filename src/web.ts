import { WebPlugin } from '@capacitor/core';

import type {
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

  isPublishing = false;
  publishTimeout: NodeJS.Timeout | undefined = undefined;

  isSubscribing = false;
  subscribeTimeout: NodeJS.Timeout | undefined = undefined;

  async initialize(options: InitializeOptions): Promise<void> {
    console.info('initialize', options);

    if (typeof navigator === 'undefined' || !navigator.bluetooth) {
      throw this.unavailable('Bluetooth not available.');
    }

    const isAvailable = await navigator.bluetooth.getAvailability();
    if (!isAvailable) {
      throw this.unavailable('Bluetooth not available.');
    }

    if (!options.serviceUUID) {
      throw this.unavailable('UUID not found');
    }

    this.serviceUUID = options.serviceUUID;
    if (!this.serviceUUID.startsWith('0x')) {
      if (this.serviceUUID.length === 4)
        this.serviceUUID = '0000' + this.serviceUUID;
      if (this.serviceUUID.length === 8)
        this.serviceUUID += '-' + '0000-1000-8000-00805f9b34fb';
    }

    navigator.bluetooth.onavailabilitychanged = event => {
      console.info('bluetooth::availabilitychanged', event);

      this.reset();
    };
  }
  async reset(): Promise<void> {
    console.info('reset');

    this.unpublish();
    this.unsubscribe();

    this.uuids = [];

    delete this.scan;
  }

  async publish(options: PublishOptions): Promise<void> {
    console.info('publish', options);
    // throw this.unimplemented('Method not implemented.');

    if (this.isPublishing) throw this.unavailable('is already publishing.');
    this.isPublishing = true;

    if (options.ttlSeconds && options.ttlSeconds > 0) {
      this.publishTimeout = setTimeout(() => {
        this.unpublish();
        this.notifyListeners('onPublishExpired', {});
      }, options.ttlSeconds * 1000);
    }
  }
  // Cancels an existing published beacon.
  async unpublish(): Promise<void> {
    console.info('unpublish');
    // throw this.unimplemented('Method not implemented.');

    clearTimeout(this.publishTimeout);
    delete this.publishTimeout;

    this.isPublishing = false;
  }

  async subscribe(options: SubscribeOptions): Promise<void> {
    console.info('subscribe', options);

    if (typeof navigator === 'undefined' || !navigator.bluetooth) {
      throw this.unavailable('Bluetooth not available.');
    }

    if (this.isSubscribing) throw this.unavailable('is already subscribing.');

    if (this.serviceUUID) {
      this.isSubscribing = true;

      if (options.ttlSeconds && options.ttlSeconds > 0) {
        this.subscribeTimeout = setTimeout(() => {
          this.unsubscribe();
          this.notifyListeners('onSubscribeExpired', {});
        }, options.ttlSeconds * 1000);
      }

      this.scan = await navigator.bluetooth.requestLEScan({
        filters: [{ services: [this.serviceUUID] }],
      });

      navigator.bluetooth.onadvertisementreceived = event => {
        console.info('bluetooth::advertisementreceived', event);

        const rssi = event.rssi;

        const uuid = event.uuids.slice(-1);
        uuid &&
          !this.uuids.includes(uuid.toString()) &&
          this.uuids.push(uuid.toString());

        this.notifyListeners('onFound', { uuid, rssi });
      };
    }
  }
  // Cancels an existing subscription.
  async unsubscribe(): Promise<void> {
    console.info('unsubscribe');

    clearTimeout(this.subscribeTimeout);
    delete this.subscribeTimeout;

    if (this.scan?.active) {
      this.scan?.stop();
    }

    delete this.scan;

    this.isSubscribing = false;
  }

  async status(): Promise<Status> {
    console.info('status');

    return {
      isPublishing: this.isPublishing,
      isSubscribing: this.scan?.active ?? false,
      uuids: this.uuids,
    };
  }
}
