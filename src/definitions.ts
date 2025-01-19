/// <reference types="@capacitor/cli" />

import type { PluginListenerHandle } from '@capacitor/core';

declare module '@capacitor/cli' {
  export interface PluginsConfig {
    /**
     * These configuration values are available:
     */
    Nearby?: {
      /**
       * An identifier to advertise your app to other endpoints.
       *
       * Only available for Android and iOS.
       */
      serviceId?: string;

      /**
       * Sets the `Strategy` to be used when discovering or advertising to Nearby devices.
       *
       * Only available for Android and iOS.
       */
      strategy?: Strategy;

      /**
       * Sets whether low power should be used.
       *
       * Only available for Android and iOS.
       *
       * @default false
       * @example false
       */
      lowPower?: boolean;
    };
  }
}

export type UUID = string;

export interface Beacon {
  uuid: UUID;
}

export type BeaconCallback = (_: Beacon) => void;

export type Status = {
  isPublishing: boolean;
  isSubscribing: boolean;
  uuids: UUID[];
};

/**
 * The `Strategy` to be used when discovering or advertising to Nearby devices.
 *
 * The `Strategy` defines
 *  1. the connectivity requirements for the device, and
 *  2. the topology constraints of the connection.
 *
 * @since 4.0.0
 */
export enum Strategy {
  // Peer-to-peer strategy that supports an M-to-N, or cluster-shaped, connection topology.
  CLUSTER = 'cluster',
  // Peer-to-peer strategy that supports a 1-to-N, or star-shaped, connection topology.
  STAR = 'star',
  // Peer-to-peer strategy that supports a 1-to-1 connection topology.
  POINT_TO_POINT = 'p2p',
}

export interface InitializeOptions {
  /**
   * A human readable name for this endpoint, to appear on the remote device.
   *
   * @since 4.0.0
   */
  name?: string;

  /**
   * An identifier to advertise your app to other endpoints.
   *
   * @since 4.0.0
   */
  serviceId: string;

  /**
   * Sets the `Strategy` to be used when discovering or advertising to Nearby devices.
   *
   * @since 4.0.0
   */
  strategy: Strategy;

  /**
   * Sets whether low power should be used.
   *
   * @since 4.0.0
   * @default false
   */
  lowPower?: boolean;
}

export interface PublishOptions {
  /**
   * Sets the beacon UUID for the publish operation.
   *
   * @since 1.1.0
   */
  uuid: UUID;
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
   * Initializes Nearby Connections for advertising and discovering of endpoints.
   *
   * @since 1.0.0
   */
  initialize(options: InitializeOptions): Promise<void>;
  /**
   * Stops and resets advertising and discovering of endpoints.
   *
   * @since 1.0.0
   */
  reset(): Promise<void>;

  /**
   * Start publishing nearby token.
   *
   * @since 1.0.0
   */
  publish(options: PublishOptions): Promise<void>;
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
  subscribe(): Promise<void>;
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
    listenerFunc: (granted: boolean) => void,
  ): Promise<PluginListenerHandle>;
  /**
   * Called when state of Bluetooth has changed.
   *
   * @since 1.0.0
   */
  addListener(
    eventName: 'onBluetoothStateChanged',
    listenerFunc: (state: BluetoothState) => void,
  ): Promise<PluginListenerHandle>;

  /**
   * Called when beacons are found.
   *
   * @since 1.1.0
   */
  addListener(eventName: 'onFound', listenerFunc: BeaconCallback): Promise<PluginListenerHandle>;
  /**
   * Called when a beacon is no longer detectable nearby.
   *
   * @since 1.1.0
   */
  addListener(eventName: 'onLost', listenerFunc: BeaconCallback): Promise<PluginListenerHandle>;
}
