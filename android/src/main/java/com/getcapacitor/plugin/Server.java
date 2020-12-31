package com.getcapacitor.plugin;

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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class Server {
    private BluetoothManager manager;

    private static Context context = null;
    private static Server instance = null;

    private BluetoothGattServer server;
    private BluetoothGattServerCallback serverCallback;

    class Packet {
        int mtu;
        int size;

        int offset;
        int rt;

        boolean available;

        public Packet(int mtu, int size) {
            this.mtu = mtu;
            this.size = size;

            this.offset = 0;
            this.rt = (size + mtu) / mtu;

            this.available = this.offset < this.size;
        }

        public void next() {
            this.offset += this.mtu;

            this.available = this.offset < this.size;
        }
    }

    private final Map<BluetoothDevice, Integer> mtus = new HashMap<>();
    private final Map<BluetoothDevice, Packet> packets = new HashMap<>();

    private final Map<UUID, byte[]> descriptors = new HashMap<>();

    //    private final Map<String, BluetoothDevice> connections = new HashMap<>();

    public void start() {
        // Check if server is already running.
        if (server == null) {
            if (serverCallback == null) {
                serverCallback =
                        new BluetoothGattServerCallback() {
                            /**
                             * Callback indicating when a remote device has been connected or disconnected.
                             *
                             * @param device Remote device that has been connected or disconnected.
                             * @param status Status of the connect or disconnect operation.
                             * @param newState Returns the new connection state. Can be one of {@link
                             * BluetoothProfile#STATE_DISCONNECTED} or {@link BluetoothProfile#STATE_CONNECTED}
                             */
                            @Override
                            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                                synchronized (mtus) {
                                    switch (newState) {
                                        case BluetoothProfile.STATE_CONNECTED:
                                            mtus.put(device, Constants.GATT_MTU_SIZE_DEFAULT);

                                            break;

                                        case BluetoothProfile.STATE_DISCONNECTED:
                                            mtus.remove(device);

                                            break;
                                    }
                                }
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
//                                super.onDescriptorReadRequest(device, requestId, offset, descriptor);

                                Log.i("Server",
                                        String.format(
                                                "onDescriptorReadRequest(device=%s, requestId=%s, offset=%s, descriptor=%s)",
                                                device, requestId, offset, descriptor));

                                int status = BluetoothGatt.GATT_FAILURE;
                                byte[] value = new byte[]{};

                                UUID uuid = descriptor.getUuid();
                                byte[] message = descriptors.get(uuid);

                                if (message != null) {
                                    Packet packet = packets.get(device);

                                    if (packet == null) {
                                        Integer mtu = mtus.get(device);

                                        packet = new Packet(mtu - 3, message.length);
                                        packets.put(device, packet);
                                    }

                                    if (packet.available) {
                                        // https://stackoverflow.com/questions/46317971/bluetoothgattservercallback-oncharacteristicreadrequest-called-multiple-time
                                        value = Arrays.copyOfRange(message, packet.offset, Math.min(packet.offset + packet.mtu, message.length));

                                        packet.next();

                                        status = BluetoothGatt.GATT_SUCCESS;
                                    } else {
                                        status = BluetoothGatt.GATT_INVALID_OFFSET;
                                    }
                                } else {
                                    // Descriptor does not exist.
                                }

                                boolean success = server.sendResponse(device, requestId, status, offset, value);
                            }

                            /**
                             * Callback indicating the MTU for a given device connection has changed.
                             *
                             * <p>This callback will be invoked if a remote client has requested to change
                             * the MTU for a given connection.
                             *
                             * @param device The remote device that requested the MTU change
                             * @param mtu The new MTU size
                             */
                            @Override
                            public void onMtuChanged(BluetoothDevice device, int mtu) {
                                synchronized (mtus) {
                                    mtus.put(device, mtu);
                                }
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

    public boolean addMessage(UUID uuid, byte[] message) {
        synchronized (descriptors) {
            if (descriptors.containsKey(uuid)) {
                return false;
            }

            descriptors.put(uuid, message);

//            restart();

            return true;
        }
    }

    public boolean removeMessage(UUID uuid) {
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
