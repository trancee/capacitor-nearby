import { PluginListenerHandle } from '@capacitor/core';

declare module '@capacitor/core' {
  interface PluginRegistry {
    CapacitorNearby: CapacitorNearbyPlugin;
  }
}

export type Status = {
  isPublishing: boolean;
  isSubscribing: boolean;
  uuids: string[];
}

export interface PublishOptions {
  interval?: number;
}

export type PublishResult = {
  uuid: string;
  timestamp: number;
}

export interface SubscribeOptions {
  interval?: number;
}

export interface CapacitorNearbyPlugin {
  initialize(): Promise<void>;
  reset(): Promise<void>;

  publish(options: {
    // A Message to publish for nearby devices to see
    message: string,
    // A PublishOptions object for this operation
    options?: PublishOptions,
  }): Promise<PublishResult>;

  // Cancels an existing published message.
  unpublish(options: {
    uuid?: string,
  }): Promise<void>;

  subscribe(options: {
    // A SubscribeOptions object for this operation
    options?: SubscribeOptions,
  }): Promise<void>;

  // Cancels an existing subscription.
  unsubscribe(options: {
  }): Promise<void>;

  pause(): Promise<void>;
  resume(): Promise<void>;

  status(): Promise<Status>;

  // Called when permission is granted or revoked for this app to use Nearby.
  addListener(eventName: 'onPermissionChanged', listenerFunc: (permissionGranted: boolean) => void): PluginListenerHandle;

  // Called when messages are found.
  addListener(eventName: 'onFound', listenerFunc: (uuid: string, message: string) => void): PluginListenerHandle;
  // Called when a message is no longer detectable nearby.
  addListener(eventName: 'onLost', listenerFunc: (uuid: string) => void): PluginListenerHandle;

  // The published message is expired.
  addListener(eventName: 'onPublishExpired', listenerFunc: (uuid: string) => void): PluginListenerHandle;
  // The subscription is expired.
  addListener(eventName: 'onSubscribeExpired', listenerFunc: (uuid: string) => void): PluginListenerHandle;
}
