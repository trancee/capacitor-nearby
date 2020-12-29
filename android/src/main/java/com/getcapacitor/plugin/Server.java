package com.getcapacitor.plugin;

import com.getcapacitor.Plugin;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class Server extends Plugin {
    private BluetoothManager manager;

    private static Context context = null;
    private static Server instance = null;

    private BluetoothGattServer server;
    private BluetoothGattServerCallback serverCallback;

    private final Map<UUID, byte[]> descriptors = new HashMap<>();

//    private final Map<String, BluetoothDevice> connections = new HashMap<>();

    public void start() {
        // Check if server is already running.
        if (server == null) {
            if (serverCallback == null) {
                serverCallback =
                        new BluetoothGattServerCallback() {
//                            /**
//                             * Callback indicating when a remote device has been connected or disconnected.
//                             *
//                             * @param device Remote device that has been connected or disconnected.
//                             * @param status Status of the connect or disconnect operation.
//                             * @param newState Returns the new connection state. Can be one of {@link
//                             * BluetoothProfile#STATE_DISCONNECTED} or {@link BluetoothProfile#STATE_CONNECTED}
//                             */
//                            @Override
//                            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
//                                super.onConnectionStateChange(device, status, newState);
//
//                                Log.i(getLogTag(),
//                                        String.format(
//                                                "onConnectionStateChange(device=%s, status=%s, newState=%s)",
//                                                device, status, newState));
//
//                                synchronized (connections) {
//                                    switch (newState) {
//                                        case BluetoothProfile.STATE_CONNECTED:
//                                            // Allow connection to proceed. Mark device connected
//                                            connections.put(device.getAddress(), device);
//
//                                            break;
//
//                                        case BluetoothProfile.STATE_DISCONNECTED:
//                                            // We've disconnected
//                                            connections.remove(device.getAddress());
//
//                                            break;
//                                    }
//                                }
//                            }

                            /**
                             * A remote client has requested to read a local characteristic.
                             *
                             * <p>An application must call {@link BluetoothGattServer#sendResponse}
                             * to complete the request.
                             *
                             * @param device The remote device that has requested the read operation
                             * @param requestId The Id of the request
                             * @param offset Offset into the value of the characteristic
                             * @param characteristic Characteristic to be read
                             */
                            @Override
                            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                                super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

                                Log.i(getLogTag(),
                                        String.format(
                                                "onCharacteristicReadRequest(device=%s, requestId=%s, offset=%s, characteristic=%s)",
                                                device, requestId, offset, characteristic));

                                int status = BluetoothGatt.GATT_FAILURE;
                                byte[] value = null;

                                UUID uuid = characteristic.getUuid();

                                BluetoothGattCharacteristic localCharacteristic =
                                        server.getService(Constants.SERVICE_UUID)
                                                .getCharacteristic(uuid);

                                if (localCharacteristic != null) {
                                    // https://stackoverflow.com/questions/46317971/bluetoothgattservercallback-oncharacteristicreadrequest-called-multiple-time
//                            value = Arrays.copyOfRange(message, offset, message.length);

                                    status = BluetoothGatt.GATT_SUCCESS;
                                } else {
                                    // Request for unrecognized characteristic. Send GATT_FAILURE
                                }

                                server.sendResponse(device, requestId, status, offset, value);
                            }

                            /**
                             * A remote client has requested to read a local descriptor.
                             *
                             * <p>An application must call {@link BluetoothGattServer#sendResponse}
                             * to complete the request.
                             *
                             * @param device The remote device that has requested the read operation
                             * @param requestId The Id of the request
                             * @param offset Offset into the value of the characteristic
                             * @param descriptor Descriptor to be read
                             */
                            @Override
                            public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
                                super.onDescriptorReadRequest(device, requestId, offset, descriptor);

                                Log.i(getLogTag(),
                                        String.format(
                                                "onDescriptorReadRequest(device=%s, requestId=%s, offset=%s, descriptor=%s)",
                                                device, requestId, offset, descriptor));

                                int status = BluetoothGatt.GATT_FAILURE;
                                byte[] value = null;

                                UUID uuid = descriptor.getUuid();

                                if (descriptors.containsKey(uuid)) {
                                    byte[] message = descriptors.get(uuid);

                                    // https://stackoverflow.com/questions/46317971/bluetoothgattservercallback-oncharacteristicreadrequest-called-multiple-time
                                    value = Arrays.copyOfRange(message, offset, message.length);

                                    status = BluetoothGatt.GATT_SUCCESS;
                                } else {
                                    // Descriptor does not exist.
                                }

                                server.sendResponse(device, requestId, status, offset, value);
                            }
                        };
            }

            server =
                    manager.openGattServer(context, serverCallback);
        }

        addService();
    }

    public void restart() {
        reset();

        start();
    }

    public void stop() {
        reset();

        if (server != null) {
            server.close();

            server = null;
//            serverCallback = null;
        }
    }

    public void reset() {
//        synchronized (connections) {
//            if (server != null) {
//                for (BluetoothDevice device : connections.values()) {
//                    server.cancelConnection(device);
//                }
//            }
//
//            connections.clear();
//        }

//        synchronized (descriptors) {
//            descriptors.clear();
//        }

//        if (server != null) {
//            server.clearServices();
//        }
    }

    private boolean addService() {
        if (server == null) {
            return false;
        }

        server.clearServices();

        BluetoothGattService service =
                new BluetoothGattService(
                        Constants.SERVICE_UUID,

                        BluetoothGattService.SERVICE_TYPE_PRIMARY
                );

        BluetoothGattCharacteristic characteristic =
                new BluetoothGattCharacteristic(
                        Constants.CHARACTERISTIC_UUID,

                        BluetoothGattCharacteristic.PROPERTY_READ,
                        BluetoothGattCharacteristic.PERMISSION_READ
                );

        for (UUID uuid : descriptors.keySet()) {
            BluetoothGattDescriptor descriptor =
                    new BluetoothGattDescriptor(
                            uuid,

                            BluetoothGattCharacteristic.PERMISSION_READ
                    );

            characteristic.addDescriptor(descriptor);
        }

        service.addCharacteristic(characteristic);

        return server.addService(service);
    }

    public boolean addMessage(@NotNull UUID uuid, @NotNull byte[] message) {
        synchronized (descriptors) {
            if (descriptors.containsKey(uuid)) {
                return false;
            }

            descriptors.put(uuid, message);

//            restart();

            return true;
        }
    }

    public boolean removeMessage(@NotNull UUID uuid) {
        synchronized (descriptors) {
            if (descriptors.containsKey(uuid)) {
                descriptors.remove(uuid);

//                restart();

                return true;
            }

            return false;
        }
    }

    public static synchronized Server getInstance(Context context) {
        if (instance == null) {
            instance = new Server(context.getApplicationContext());
        }

        return instance;
    }

    private Server(Context context) {
        this.context = context;

        // Initializes Bluetooth adapter.
        manager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
    }
}