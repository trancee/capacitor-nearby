package com.getcapacitor.community;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

public class NearbyConnection {
    /*
    // Assuming you have a characteristic UUID for reading/writing
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("your-characteristic-uuid");
    public static final UUID SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    public static final UUID CHARACTERISTIC_UUID_TX = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");

    private BluetoothGatt gatt;

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void connect(@NonNull BluetoothDevice device) {
        gatt = device.connectGatt(this, false, gattCallback);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // Discover services after connection is established
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // Handle disconnection
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Services discovered, proceed with encryption
                enableEncryption(gatt);
            } else {
                // Service discovery failed
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        private void enableEncryption(BluetoothGatt gatt) {
            gatt.requestMtu(512); // Request a higher MTU size if needed
            // Encryption is automatically handled by the stack once connected and services are discovered
        }

        @Override
        public void onMtusChanged(BluetoothGatt gatt, int mtu, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //                Log.d("BLE", "MTU changed to: " + mtu);
                // Proceed with data exchange
            } else {
                //                Log.e("BLE", "Failed to change MTU");
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Handle read data
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Data written successfully
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // Handle received data
        }
    };

    private void readCharacteristic(BluetoothGatt gatt) {
        BluetoothGattService service = gatt.getService(SERVICE_UUID);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
            if (characteristic != null && (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                gatt.readCharacteristic(characteristic);
            }
        }
    }

    private void writeCharacteristic(BluetoothGatt gatt, byte[] data) {
        BluetoothGattService service = gatt.getService(SERVICE_UUID);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
            if (characteristic != null && (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
                characteristic.setValue(data);
                gatt.writeCharacteristic(characteristic);
            }
        }
    }
 */
    /*
    public void connect() {
        final BluetoothDevice remoteDevice = bluetoothAdapter.getRemoteDevice(address);
        if (remoteDevice == null) {
            Exception exception = new Exception(MISSING_ENDPOINT);
            callback.error(exception);
            return;
        }

        @SuppressLint("MissingPermission")
        BluetoothGatt gatt = remoteDevice.connectGatt(
            this,
            false,
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        //Log.d(TAG, "Connected to GATT server.");
                        gatt.discoverServices();
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        //Log.w(TAG, "Disconnected from GATT server.");
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        transferCharac = gatt.getService("your-service-uuid").getCharacteristic("your-characteristic-uuid");
                        transferCharac.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                        //Log.d(TAG, "Service discovered.");
                    } else {
                        //Log.w(TAG, "onServicesDiscovered received: " + status);
                    }
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        currentOffset += characteristic.getValue().length;
                        sendData();
                    } else {
                        //Log.w(TAG, "Failed to write data: " + status);
                        retrySendData(characteristic);
                    }
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        // Handle received data here.
                    } else {
                        //Log.w(TAG, "Failed to read data: " + status);
                    }
                }
            }
        );
    }
 */
}
