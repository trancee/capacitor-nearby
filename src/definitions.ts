import { PluginListenerHandle } from '@capacitor/core';

declare module '@capacitor/core' {
  interface PluginRegistry {
    Nearby: NearbyPlugin;
  }
}

export type UUID = string;

export enum TTLSeconds {
  // The default time to live in seconds.
  TTL_SECONDS_DEFAULT = 300,
  // The maximum time to live in seconds, if not TTL_SECONDS_INFINITE.
  TTL_SECONDS_MAX = 86400,
  // An infinite time to live in seconds.
  // Note: This is currently only supported for subscriptions.
  TTL_SECONDS_INFINITE = 2147483647,
}

export type Status = {
  isPublishing: boolean;
  isSubscribing: boolean;
  uuids: UUID[];
}

export enum ScanMode {
  // Perform Bluetooth LE scan in low power mode.
  LOW_POWER = 0,
  // Perform Bluetooth LE scan in balanced power mode.
  BALANCED = 1,
  // Scan using highest duty cycle.
  LOW_LATENCY = 2,

  // A special Bluetooth LE scan mode.
  OPPORTUNISTIC = -1,
}

export enum AdvertiseMode {
  // Perform Bluetooth LE advertising in low power mode.
  LOW_POWER = 0,
  // Perform Bluetooth LE advertising in balanced power mode.
  BALANCED = 1,
  // Perform Bluetooth LE advertising in low latency, high power mode.
  LOW_LATENCY = 2,
}

export enum TxPowerLevel {
  // Advertise using the lowest transmission (TX) power level.
  ULTRA_LOW = 0,
  // Advertise using low TX power level.
  LOW = 1,
  // Advertise using medium TX power level.
  MEDIUM = 2,
  // Advertise using high TX power level.
  HIGH = 3,
}

// A message that will be shared with nearby devices.
export interface Message {
  // The UUID of the message.
  readonly uuid: UUID;

  // The raw bytes content of the message.
  data?: string;
}

export interface InitializeOptions {
  scanMode?: ScanMode;

  advertiseMode?: AdvertiseMode;
  txPowerLevel?: TxPowerLevel;
}

export interface PublishOptions {
  // Sets the time to live in seconds for the publish.
  ttlSeconds?: TTLSeconds;
}

export interface SubscribeOptions {
  // Sets the time to live in seconds for the subscribe.
  ttlSeconds?: TTLSeconds;
}

export type PublishResult = {
  // Returns the UUID of the message.
  uuid: UUID;
}

export interface NearbyPlugin {
  initialize(options: {
    // A InitializeOptions object for this operation
    options?: InitializeOptions,
  }): Promise<void>;
  reset(): Promise<void>;

  publish(options: {
    // A Message to publish for nearby devices to see
    message: Message,
    // A PublishOptions object for this operation
    options?: PublishOptions,
  }): Promise<PublishResult>;
  // Cancels an existing published message.
  unpublish(options: {
    uuid?: UUID,
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
  addListener(eventName: 'onFound', listenerFunc: (uuid: UUID, content: string) => void): PluginListenerHandle;
  // Called when a message is no longer detectable nearby.
  addListener(eventName: 'onLost', listenerFunc: (uuid: UUID) => void): PluginListenerHandle;

  // The published message is expired.
  addListener(eventName: 'onPublishExpired', listenerFunc: (uuid: UUID) => void): PluginListenerHandle;
  // The subscription is expired.
  addListener(eventName: 'onSubscribeExpired', listenerFunc: (uuid: UUID) => void): PluginListenerHandle;
}
