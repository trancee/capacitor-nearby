package com.getcapacitor.plugin;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;

import android.content.Context;
import android.os.Handler;
import android.util.Base64;

import com.welie.blessed.BluetoothBytesParser;
import com.welie.blessed.BluetoothCentral;
import com.welie.blessed.BluetoothCentralCallback;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.BluetoothPeripheralCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.welie.blessed.BluetoothPeripheral.GATT_SUCCESS;

class Scanner {
    private final Context context;
    private final Callback callback;
    private final Handler handler;

    private static Scanner instance = null;

    private BluetoothCentral central;

    private ScanCallback scanCallback;

    private static long ttlSeconds = 3;

    private class Message {
        Runnable runnable;

        UUID uuid;
        long lastSeen;

        public Message(UUID uuid) {
            this.runnable = () -> {
                // Check if we are still alive.
                if (messages.containsKey(this.uuid)) {
                    callback.onLost(this.uuid);
                }

                this.kill();
            };

            this.uuid = uuid;
            this.alive();
        }

        public void kill() {
            handler.removeCallbacks(this.runnable);

            // Kill yourself.
            messages.remove(this.uuid);
        }

        public void alive() {
            this.lastSeen = System.currentTimeMillis();

            handler.removeCallbacks(this.runnable);

            // Check if we are still alive.
            if (messages.containsKey(this.uuid)) {
                handler.postDelayed(this.runnable, ttlSeconds * 1000);
            }
        }
    }

    private final Map<UUID, Message> messages = new HashMap<>();

    // Callback for peripherals
    private final BluetoothPeripheralCallback peripheralCallback = new BluetoothPeripheralCallback() {
        private int count = 0;

        @Override
        public void onServicesDiscovered(BluetoothPeripheral peripheral) {
//            Log.i(capacitor.getLogTag(),
//                    String.format(
//                            "onServicesDiscovered(peripheral=%s)",
//                            peripheral));

//            peripheral.requestMtu(Constants.GATT_MTU_SIZE);
//
//            // Request a new connection priority
//            peripheral.requestConnectionPriority(CONNECTION_PRIORITY_HIGH);

            count = 0;

//            peripheral.readCharacteristic(Constants.SERVICE_UUID, Constants.CHARACTERISTIC_UUID);

            BluetoothGattCharacteristic characteristic =
                    peripheral.getCharacteristic(Constants.SERVICE_UUID, Constants.CHARACTERISTIC_UUID);

            if (characteristic != null) {
                for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                    UUID uuid = descriptor.getUuid();

                    Message message = messages.get(uuid);

                    if (message != null) {
                        message.alive();
                    } else {
                        count++;
                        peripheral.readDescriptor(descriptor);
                    }
                }
            }

            if (count == 0) {
                central.cancelConnection(peripheral);
            }
        }

//        @Override
//        public void onCharacteristicWrite(BluetoothPeripheral peripheral, byte[] value, BluetoothGattCharacteristic characteristic, int status) {
//            Log.i(capacitor.getLogTag(),
//                    String.format(
//                            "onCharacteristicWrite(peripheral=%s, value=%s, characteristic=%s, status=%s)",
//                            peripheral, value, characteristic, status));
//        }

        @Override
        public void onCharacteristicUpdate(BluetoothPeripheral peripheral, byte[] value, BluetoothGattCharacteristic characteristic, int status) {
//            Log.i(capacitor.getLogTag(),
//                    String.format(
//                            "onCharacteristicUpdate(peripheral=%s, value=%s, characteristic=%s, status=%s)",
//                            peripheral, value, characteristic, status));

            if (status != GATT_SUCCESS) {
                count = 0;
                central.cancelConnection(peripheral);

                return;
            }

            for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                UUID uuid = descriptor.getUuid();

                Message message = messages.get(uuid);

                if (message != null) {
                    message.alive();
                } else {
                    count++;
                    peripheral.readDescriptor(descriptor);
                }
            }
        }

        @Override
        public void onDescriptorRead(final BluetoothPeripheral peripheral, byte[] value, final BluetoothGattDescriptor descriptor, final int status) {
//            Log.i(capacitor.getLogTag(),
//                    String.format(
//                            "onDescriptorRead(peripheral=%s, value=%s, descriptor=%s, status=%s)",
//                            peripheral, value, descriptor, status));

            if (--count <= 0) {
                central.cancelConnection(peripheral);
            }

            if (status != GATT_SUCCESS) {
                return;
            }

            UUID uuid = descriptor.getUuid();

            Message message = messages.get(uuid);

            if (message != null) {
                message.alive();
            } else {
                BluetoothBytesParser parser = new BluetoothBytesParser(value);

                callback.onFound(uuid, Base64.encodeToString(parser.getValue(), Base64.DEFAULT | Base64.NO_WRAP));

                messages.put(uuid, new Message(uuid));
            }
        }

//        @Override
//        public void onMtuChanged(BluetoothPeripheral peripheral, int mtu, int status) {
//            Log.i(capacitor.getLogTag(),
//                    String.format(
//                            "onMtuChanged(peripheral=%s, mtu=%s, status=%s)",
//                            peripheral, mtu, status));
//        }
    };

    // Callback for central
    private final BluetoothCentralCallback bluetoothCentralCallback = new BluetoothCentralCallback() {
//        @Override
//        public void onConnectedPeripheral(BluetoothPeripheral peripheral) {
//            Log.i(capacitor.getLogTag(),
//                    String.format(
//                            "onConnectedPeripheral(peripheral=%s)",
//                            peripheral));
//        }

//        @Override
//        public void onConnectionFailed(BluetoothPeripheral peripheral, final int status) {
//            Log.e(capacitor.getLogTag(),
//                    String.format(
//                            "onConnectionFailed(peripheral=%s, status=%s)",
//                            peripheral, status));
//        }

//        @Override
//        public void onDisconnectedPeripheral(final BluetoothPeripheral peripheral, final int status) {
//            Log.i(capacitor.getLogTag(),
//                    String.format(
//                            "onDisconnectedPeripheral(peripheral=%s, status=%s)",
//                            peripheral, status));
//        }

        @Override
        public void onDiscoveredPeripheral(BluetoothPeripheral peripheral, ScanResult scanResult) {
//            Log.i(capacitor.getLogTag(),
//                    String.format(
//                            "onDiscoveredPeripheral(peripheral=%s, scanResult=%s)",
//                            peripheral, scanResult));

//            ScanRecord scanRecord = scanResult.getScanRecord();
//            byte[] data = scanRecord.getManufacturerSpecificData(Constants.MANUFACTURER_ID);
//            long timestamp = Constants.fromBytes(data);
//
//            Long advertisement = advertisements.get(peripheral.getAddress());
//
//            if (advertisement == null || (advertisement != null && advertisement != timestamp)) {
//                Log.i(capacitor.getLogTag(),
//                        String.format(
//                                "scanRecord=%s, timestamp=%s",
//                                scanRecord, timestamp));

//                    central.stopScan();
            central.connectPeripheral(peripheral, peripheralCallback);

//                advertisements.put(peripheral.getAddress(), timestamp);
//            }
        }

        @Override
        public void onBluetoothAdapterStateChanged(int state) {
//            Log.i(capacitor.getLogTag(),
//                    String.format(
//                            "onBluetoothAdapterStateChanged(state=%s)",
//                            state));

            switch (state) {
                // Indicates the local Bluetooth adapter is on, and ready for use.
                case BluetoothAdapter.STATE_ON:
                    callback.onPermissionChanged(true);

                    break;

                // Indicates the local Bluetooth adapter is off.
                case BluetoothAdapter.STATE_OFF:
                    stop();

                    callback.onPermissionChanged(false);

                    break;
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
//            Log.i(capacitor.getLogTag(),
//                    String.format(
//                            "onScanFailed(state=%s)",
//                            errorCode));

            stop();

            scanCallback.onFailed(errorCode);
        }
    };

    public void start(List<ScanFilter> filters, ScanCallback scanCallback) {
        if (instance != null) {
            if (instance.central != null) {
                this.scanCallback = scanCallback;

                // Scan for peripherals that advertise at least one of the specified service UUIDs.
                instance.central.scanForPeripheralsUsingFilters(filters);
            }
        }
    }

    public void stop() {
        scanCallback = null;

        if (instance != null) {
            if (instance.central != null) {
                // Stop scanning for peripherals.
                instance.central.stopScan();
            }
        }

        // Note: existing messages might time out and emit onLost events.
        messages.clear();
    }

    public static synchronized Scanner getInstance(Context context, Callback callback, Handler handler, Integer scanMode) {
        if (instance == null) {
            instance = new Scanner(context, callback, handler, scanMode);
        }

        return instance;
    }

    private Scanner(Context context, Callback callback, Handler handler, Integer scanMode) {
        this.context = context;
        this.callback = callback;
        this.handler = handler;

        // Create BluetoothCentral
        this.central = new BluetoothCentral(context, bluetoothCentralCallback, handler);

        if (scanMode != null) {
            this.central.setScanMode(scanMode);
        }
    }

    public abstract static class Callback {
        public void onFound(UUID uuid, String message) {
        }

        public void onLost(UUID uuid) {
        }

        public void onPermissionChanged(Boolean permissionGranted) {
        }
    }

    public abstract static class ScanCallback {
        public void onFailed(int errorCode) {
        }
    }
}