package com.getcapacitor.community;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;

public class NearbyConnection {
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
