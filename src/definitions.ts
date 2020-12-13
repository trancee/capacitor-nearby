import { PluginListenerHandle } from '@capacitor/core';

declare module '@capacitor/core' {
  interface PluginRegistry {
    CapacitorNearby: CapacitorNearbyPlugin;
  }
}

export enum Status {
  // A GATT operation completed successfully
  SUCCESS = 0x00000000,
  // GATT read operation is not permitted
  READ_NOT_PERMITTED = 0x00000002,
  // GATT write operation is not permitted
  WRITE_NOT_PERMITTED = 0x00000003,
  // Insufficient authentication for a given operation
  INSUFFICIENT_AUTHENTICATION = 0x00000005,
  // The given request is not supported
  REQUEST_NOT_SUPPORTED = 0x00000006,
  // A read or write operation was requested with an invalid offset
  INVALID_OFFSET = 0x00000007,
  // A write operation exceeds the maximum length of the attribute
  INVALID_ATTRIBUTE_LENGTH = 0x0000000d,
  // Insufficient encryption for a given operation
  INSUFFICIENT_ENCRYPTION = 0x0000000f,
  // A remote device connection is congested.
  CONNECTION_CONGESTED = 0x0000008f,
  // A GATT operation failed, errors other than the above
  FAILURE = 0x00000101,
}

export enum State {
  // The profile is in disconnected state
  DISCONNECTED = 0x00000000,
  // The profile is in connecting state
  CONNECTING = 0x00000001,
  // The profile is in connected state
  CONNECTED = 0x00000002,
  // The profile is in disconnecting state
  DISCONNECTING = 0x00000003,
}

// Represents a Bluetooth class, which describes general characteristics and capabilities of a device.
export type BluetoothClass = {
  // Return the (major and minor) device class component of this BluetoothClass.
  deviceClass: number;
  // Return the major device class component of this BluetoothClass.
  majorDeviceClass: number;
}

// Represents a remote Bluetooth device.
export type BluetoothDevice = {
  // Returns the hardware address of this BluetoothDevice.
  address: string;
  // Get the friendly Bluetooth name of the remote device.
  name: string;
  // Get the Bluetooth device type of the remote device.
  type: number;

  // // Get the Bluetooth class of the remote device.
  // bluetoothClass: BluetoothClass;
  // // Get the bond state of the remote device.
  // bondState: number;

  // Returns the supported features (UUIDs) of the remote device.
  uuids: string[];
}

// Represents a Bluetooth GATT Descriptor
export type BluetoothGattDescriptor = {
  // Returns the UUID of this descriptor.
  uuid: string;

  // Returns the stored value for this descriptor
  value: string;
}

// Represents a Bluetooth GATT Characteristic
export type BluetoothGattCharacteristic = {
  // Returns the UUID of this characteristic
  uuid: string;

  // Get the stored value for this characteristic.
  value: string;

  // Returns a list of descriptors for this characteristic.
  descriptors: BluetoothGattDescriptor[];
}

export enum ServiceType {
  // Primary service
  PRIMARY = 0x00000000,
  // Secondary service (included by primary services)
  SECONDARY = 0x00000001,
}

// Represents a Bluetooth GATT Service
export type BluetoothGattService = {
  // Returns the UUID of this service
  uuid: string;

  // Get the type of this service (primary/secondary)
  type: ServiceType;

  // Returns a list of characteristics included in this service.
  characteristics: BluetoothGattCharacteristic[];
}

// This class provides a way to control single Bluetooth LE advertising instance.
export type AdvertisingSet = {
}

// Represents a scan record from Bluetooth LE scan.
export type ScanRecord = {
  // Returns the advertising flags indicating the discoverable mode and capability of the device.
  advertiseFlags: number;
  // Returns raw bytes of scan record.
  bytes: string;
  // Returns the local name of the BLE device.
  deviceName: string;
  // Returns the transmission power level of the packet in dBm.
  txPowerLevel: number;
}

// ScanResult for Bluetooth LE scan.
export type ScanResult = {
  // Returns the remote Bluetooth device identified by the Bluetooth device address.
  device: BluetoothDevice;

  // Returns the scan record, which is a combination of advertisement and scan response.
  scanRecord: ScanRecord;

  // Returns timestamp since boot when the scan record was observed.
  timestampNanos: number;

  // Returns the received signal strength in dBm.
  rssi: number;
  // Returns the transmit power in dBm.
  txPower: number;
  // Returns the primary Physical Layer on which this advertisment was received.
  primaryPhy: number;
  // Returns the secondary Physical Layer on which this advertisment was received.
  secondaryPhy: number;
  // Returns the advertising set id.
  advertisingSid: number;
  // Returns the periodic advertising interval in units of 1.25ms.
  periodicAdvertisingInterval: number;
  // Returns the data status.
  dataStatus: number;

  // Returns true if this object represents connectable scan result.
  isConnectable: boolean;
  // Returns true if this object represents legacy scan result.
  isLegacy: boolean;
}

export interface CapacitorNearbyPlugin {
  initialize(): Promise<void>;

  advertise(): Promise<void>;
  scan(): Promise<void>;

  addListener(eventName: 'onAdvertisingSetStarted', listenerFunc: (advertisingSet: AdvertisingSet, txPower: number, status: number) => void): PluginListenerHandle;
  addListener(eventName: 'onAdvertisingDataSet', listenerFunc: (advertisingSet: AdvertisingSet, status: number) => void): PluginListenerHandle;
  addListener(eventName: 'onScanResponseDataSet', listenerFunc: (advertisingSet: AdvertisingSet, status: number) => void): PluginListenerHandle;
  addListener(eventName: 'onAdvertisingSetStopped', listenerFunc: (advertisingSet: AdvertisingSet) => void): PluginListenerHandle;

  addListener(eventName: 'onScanResult', listenerFunc: (callbackType: number, result: ScanResult) => void): PluginListenerHandle;
  addListener(eventName: 'onScanFailed', listenerFunc: (errorCode: number) => void): PluginListenerHandle;

  // Peripheral (Server)
  addListener(eventName: 'onConnectionStateChange', listenerFunc: (device: BluetoothDevice, status: Status, newState: State) => void): PluginListenerHandle;
  addListener(eventName: 'onServiceAdded', listenerFunc: (status: Status, service: BluetoothGattService) => void): PluginListenerHandle;

  // Central (Client)
  addListener(eventName: 'onConnectionStateChange', listenerFunc: (status: Status, newState: State) => void): PluginListenerHandle;
  addListener(eventName: 'onMtuChanged', listenerFunc: (mtu: number, status: Status) => void): PluginListenerHandle;
  addListener(eventName: 'onServicesDiscovered', listenerFunc: (status: Status, services: BluetoothGattService[]) => void): PluginListenerHandle;
}
