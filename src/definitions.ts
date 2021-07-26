import type { PluginListenerHandle } from '@capacitor/core';

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
};

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
  /**
   * The UUID of the message.
   *
   * @since 1.0.0
   */
  uuid: UUID;

  /**
   * The raw bytes content of the message.
   *
   * @since 1.0.0
   */
  content?: string;
}

export interface InitializeOptions {
  /**
   * Sets the service UUID for the nearby token.
   *
   * @since 1.0.0
   */
  serviceUUID: UUID;

  /**
   * Sets the scan mode.
   *
   * Default:
   * Perform Bluetooth LE scan in balanced power mode.
   *
   * @since 1.0.0
   * @default ScanMode.BALANCED
   */
  scanMode?: ScanMode;

  /**
   * Sets the advertise mode.
   *
   * Default:
   * Perform Bluetooth LE advertising in low latency, high power mode.
   *
   * @since 1.0.0
   * @default AdvertiseMode.LOW_LATENCY
   */
  advertiseMode?: AdvertiseMode;
  /**
   * Sets the TX power level for advertising.
   *
   * Default:
   * Advertise using high TX power level.
   *
   * @since 1.0.0
   * @default TxPowerLevel.HIGH
   */
  txPowerLevel?: TxPowerLevel;
}

export interface PublishOptions {
  /**
   * Sets the time to live in seconds for the publish operation.
   *
   * @since 1.0.0
   */
  ttlSeconds?: TTLSeconds;
}

export interface SubscribeOptions {
  /**
   * Sets the time to live in seconds for the subscribe operation.
   *
   * @since 1.0.0
   */
  ttlSeconds?: TTLSeconds;
}

export enum BluetoothState {
  // The manager’s state is unknown.
  UNKNOWN = 'unknown',
  // A state that indicates the connection with the system service was momentarily lost.
  RESETTING = 'resetting',
  // A state that indicates this device doesn’t support the Bluetooth low energy central or client role.
  UNSUPPORTED = 'unsupported',
  // A state that indicates the application isn’t authorized to use the Bluetooth low energy role.
  UNAUTHORIZED = 'unauthorized',
  // A state that indicates Bluetooth is currently powered off.
  POWERED_OFF = 'poweredOff',
  // A state that indicates Bluetooth is currently powered on and available to use.
  POWERED_ON = 'poweredOn',
}

export interface NearbyPlugin {
  /**
   * Initializes Bluetooth LE for advertising and scanning of nearby tokens.
   *
   * @since 1.0.0
   */
  initialize(options: {
    // A InitializeOptions object for this operation
    options?: InitializeOptions;
  }): Promise<void>;
  /**
   * Stops and resets advertising and scanning of nearby tokens.
   *
   * @since 1.0.0
   */
  reset(): Promise<void>;

  /**
   * Start publishing nearby token.
   *
   * @since 1.0.0
   */
  publish(options: {
    // A Message to publish for nearby devices to see
    message: Message;
    // A PublishOptions object for this operation
    options?: PublishOptions;
  }): Promise<void>;
  /**
   * Stop publishing nearby token.
   *
   * @since 1.0.0
   */
  unpublish(): Promise<void>;

  /**
   * Start listening to nearby tokens.
   *
   * @since 1.0.0
   */
  subscribe(options: {
    // A SubscribeOptions object for this operation
    options?: SubscribeOptions;
  }): Promise<void>;
  /**
   * Stop listening to nearby tokens.
   *
   * @since 1.0.0
   */
  unsubscribe(): Promise<void>;

  /**
   * Returns status of operations and found tokens.
   *
   * @since 1.0.0
   */
  status(): Promise<Status>;

  /**
   * Called when permission is granted or revoked for this app to use Nearby.
   *
   * @since 1.0.0
   */
  addListener(
    eventName: 'onPermissionChanged',
    listenerFunc: (permissionGranted: boolean) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;
  /**
   * Called when state of Bluetooth has changed.
   *
   * @since 1.0.0
   */
  addListener(
    eventName: 'onBluetoothStateChanged',
    listenerFunc: (state: BluetoothState) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  /**
   * Called when messages are found.
   *
   * @since 1.0.0
   */
  addListener(
    eventName: 'onFound',
    listenerFunc: (uuid: UUID, content?: string) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;
  /**
   * Called when a message is no longer detectable nearby.
   *
   * @since 1.0.0
   */
  addListener(
    eventName: 'onLost',
    listenerFunc: (uuid: UUID, content?: string) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  /**
   * The published token has expired.
   *
   * @since 1.0.0
   */
  addListener(
    eventName: 'onPublishExpired',
    listenerFunc: () => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;
  /**
   * The subscription has expired.
   *
   * @since 1.0.0
   */
  addListener(
    eventName: 'onSubscribeExpired',
    listenerFunc: () => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;
}
