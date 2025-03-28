/// <reference types="@capacitor/cli" />

import type { PermissionState, PluginListenerHandle } from '@capacitor/core';

declare module '@capacitor/cli' {
  export interface PluginsConfig {
    /**
     * These configuration values are available:
     */
    Nearby?: {
      /**
       * A human readable name for this endpoint, to appear on the remote device.
       *
       * @since 4.1.0
       * @example "My App"
       */
      endpointName?: string;
      /**
       * Identifing information about this endpoint, to appear on the remote device.
       *
       * ___Note: maximum length is 14 bytes.___
       *
       * @since 4.1.0
       * @type Base64 encoded string
       * @example "TXkgQXBw"
       */
      endpointInfo?: string;

      /**
       * An identifier to advertise your app to other endpoints.
       *
       * The `serviceID` value must uniquely identify your app.
       * As a best practice, use the package name of your app (for example, `com.example.myapp`).
       *
       * @since 4.1.0
       * @example "com.example.myapp"
       */
      serviceID?: ServiceID;
    };
  }
}

export interface NearbyPlugin {
  /**
   * Initializes Nearby for advertising and discovering of endpoints.
   *
   * @since 4.1.0
   */
  initialize(options?: InitializeOptions): Promise<InitializeResult>;
  /**
   * Stops and resets advertising and discovering of endpoints.
   *
   * @since 4.1.0
   */
  reset(): Promise<void>;

  /**
   * Starts advertising the local endpoint.
   *
   * @since 4.1.0
   */
  startAdvertising(options?: StartAdvertisingOptions): Promise<void>;
  /**
   * Stops advertising the local endpoint.
   *
   * @since 4.1.0
   */
  stopAdvertising(): Promise<void>;

  /**
   * Starts discovering remote endpoints.
   *
   * @since 4.1.0
   */
  startDiscovering(): Promise<void>;
  /**
   * Stops discovering remote endpoints.
   *
   * @since 4.1.0
   */
  stopDiscovering(): Promise<void>;

  connect(options: ConnectOptions): Promise<void>;
  /**
   * Disconnects from a remote endpoint.
   * `Payload`s can no longer be sent to or received from the endpoint after this method is called.
   *
   * @since 4.1.0
   */
  disconnect(options: DisconnectOptions): Promise<void>;

  /**
   * Sends a `Payload` to a remote endpoint.
   *
   * @since 4.1.0
   */
  sendPayload(options: SendPayloadOptions): Promise<void>;

  /**
   * Returns advertising and discovering status, and discovered endpoints.
   *
   * @since 4.1.0
   */
  status(): Promise<StatusResult>;

  /**
   * Check for the appropriate permissions to use Nearby.
   *
   * @since 4.1.0
   */
  checkPermissions(): Promise<PermissionStatus>;
  /**
   * Request the appropriate permissions to use Nearby.
   *
   * @since 4.1.0
   */
  requestPermissions(permissions?: NearbyPermissions): Promise<PermissionStatus>;

  /**
   * Called when permission is granted or revoked for this app to use Nearby.
   *
   * @since 4.1.0
   */
  addListener(
    eventName: 'onPermissionChanged',
    listenerFunc: (granted: boolean) => void,
  ): Promise<PluginListenerHandle>;
  /**
   * Called when state of Bluetooth has changed.
   *
   * @since 4.1.0
   */
  addListener(
    eventName: 'onBluetoothStateChanged',
    listenerFunc: (state: BluetoothState) => void,
  ): Promise<PluginListenerHandle>;

  /**
   * Called when a remote endpoint is discovered.
   *
   * @since 4.1.0
   */
  addListener(eventName: 'onEndpointFound', listenerFunc: EndpointFoundCallback): Promise<PluginListenerHandle>;
  /**
   * Called when a remote endpoint is no longer discoverable.
   *
   * @since 4.1.0
   */
  addListener(eventName: 'onEndpointLost', listenerFunc: EndpointLostCallback): Promise<PluginListenerHandle>;

  /**
   * Called when a remote endpoint is connected.
   *
   * @since 4.1.0
   */
  addListener(eventName: 'onEndpointConnected', listenerFunc: EndpointConnectedCallback): Promise<PluginListenerHandle>;
  /**
   * Called when a remote endpoint is disconnected or has become unreachable.
   *
   * @since 4.1.0
   */
  addListener(
    eventName: 'onEndpointDisconnected',
    listenerFunc: EndpointDisconnectedCallback,
  ): Promise<PluginListenerHandle>;

  /**
   * Called when a `Payload` is received from a remote endpoint.
   *
   * @since 4.1.0
   */
  addListener(eventName: 'onPayloadReceived', listenerFunc: PayloadReceivedCallback): Promise<PluginListenerHandle>;
}

/**
 * Used to represent a service identifier.
 *
 * @since 4.1.0
 */
export type ServiceID = string;

/**
 * Used to represent an endpoint.
 *
 * @since 4.1.0
 */
export type EndpointID = string;

export interface Endpoint {
  /**
   * The ID of the remote endpoint that was discovered.
   *
   * @since 4.1.0
   */
  readonly endpointID: EndpointID;

  /**
   * A human readable name for this endpoint.
   *
   * @since 4.1.0
   */
  readonly endpointName?: string;
  /**
   * Identifing information about this endpoint.
   *
   * ___Note: maximum length is 14 bytes.___
   *
   * @since 4.1.0
   */
  readonly endpointInfo?: string;
}

// Endpoint Discovery

/**
 * Called when a remote endpoint is discovered.
 *
 * @since 4.1.0
 */
export type EndpointFoundCallback = (_: Endpoint) => void;
/**
 * Called when a remote endpoint is no longer discoverable.
 *
 * @since 4.1.0
 */
export type EndpointLostCallback = (_: Endpoint) => void;

// Connection Lifecycle

/**
 * Called when a remote endpoint is connected.
 *
 * @since 4.1.0
 */
export type EndpointConnectedCallback = (_: Endpoint) => void;
/**
 * Called when a remote endpoint is disconnected or has become unreachable.
 *
 * @since 4.1.0
 */
export type EndpointDisconnectedCallback = (_: Endpoint) => void;

// Payload

/**
 * Called when a payload is received from a remote endpoint. Depending on the type of the payload,
 * all of the data may or may not have been received at the time of this call.
 *
 * @since 4.1.0
 */
export type PayloadReceivedCallback = (_: Endpoint & Payload) => void;

/**
 * A Payload sent between devices.
 *
 * @since 4.1.0
 */
export interface Payload {
  /**
   * Payload data.
   *
   * @since 4.1.0
   * @example "Hello, World!"
   */
  readonly payload: string;
}

// Options

export interface InitializeOptions {
  /**
   * A human readable name for this endpoint, to appear on the remote device.
   *
   * @since 4.1.0
   * @example "My App"
   */
  endpointName?: string;
  /**
   * Identifing information about this endpoint, to appear on the remote device.
   *
   * ___Note: maximum length is 14 bytes.___
   *
   * @since 4.1.0
   * @example "My App"
   */
  endpointInfo?: string;

  /**
   * An identifier to advertise your app to other endpoints.
   *
   * The `serviceID` value must uniquely identify your app.
   * As a best practice, use the package name of your app (for example, `com.example.myapp`).
   *
   * @since 4.1.0
   * @example "com.example.myapp"
   */
  serviceID?: ServiceID;
}

export interface InitializeResult {
  /**
   * A unique identifier for this endpoint.
   *
   * @since 4.1.0
   */
  endpointID: EndpointID;
}

export interface StartAdvertisingOptions {
  /**
   * Identifing information about this endpoint.
   *
   * ___Note: maximum length is 14 bytes.___
   *
   * @since 4.1.0
   */
  endpointInfo?: string;
}

export interface ConnectOptions {
  /**
   * The identifier for the remote endpoint to connect to.
   *
   * @since 4.1.0
   */
  endpointID: EndpointID;
}

export interface DisconnectOptions {
  /**
   * The identifier for the remote endpoint to disconnect from.
   *
   * @since 4.1.0
   */
  endpointID: EndpointID;
}

export interface SendPayloadOptions {
  /**
   * The identifier for the remote endpoint to which the payload should be sent.
   *
   * @since 4.1.0
   */
  endpointID?: EndpointID;
  /**
   * The identifiers for the remote endpoints to which the payload should be sent.
   *
   * @since 4.1.0
   */
  endpointIDs?: EndpointID[];

  /**
   * The `Payload` to be sent.
   *
   * @since 4.1.0
   * @type Base64 encoded string
   */
  payload: string;
}

export interface StatusResult {
  isAdvertising: boolean;
  isDiscovering: boolean;
}

export enum BluetoothState {
  /**
   * The manager’s state is unknown.
   *
   * @since 1.0.0
   */
  UNKNOWN = 'unknown',
  /**
   * A state that indicates the connection with the system service was momentarily lost.
   *
   * @since 1.0.0
   */
  RESETTING = 'resetting',
  /**
   * A state that indicates this device doesn’t support the Bluetooth low energy central or client role.
   *
   * @since 1.0.0
   */
  UNSUPPORTED = 'unsupported',
  /**
   * A state that indicates the application isn’t authorized to use the Bluetooth low energy role.
   *
   * @since 1.0.0
   */
  UNAUTHORIZED = 'unauthorized',
  /**
   * A state that indicates Bluetooth is currently powered off.
   *
   * @since 1.0.0
   */
  POWERED_OFF = 'poweredOff',
  /**
   * A state that indicates Bluetooth is currently powered on and available to use.
   *
   * @since 1.0.0
   */
  POWERED_ON = 'poweredOn',
}

export interface PermissionStatus {
  /**
   * `BLUETOOTH_ADVERTISE` Required to be able to advertise to nearby Bluetooth devices.
   * `BLUETOOTH_CONNECT` Required to be able to connect to paired Bluetooth devices.
   * `BLUETOOTH_SCAN` Required to be able to discover and pair nearby Bluetooth devices.
   *
   * `BLUETOOTH` Allows applications to connect to paired bluetooth devices.
   * `BLUETOOTH_ADMIN` Allows applications to discover and pair bluetooth devices.
   *
   * @since 4.1.0
   */
  bluetooth: PermissionState;
  /**
   * `ACCESS_FINE_LOCATION` Allows an app to access precise location.
   *
   * `ACCESS_COARSE_LOCATION` Allows an app to access approximate location.
   *
   * @since 4.1.0
   */
  location: PermissionState;
}

export type NearbyPermissionType = 'bluetooth' | 'location';

export interface NearbyPermissions {
  permissions: NearbyPermissionType[];
}
