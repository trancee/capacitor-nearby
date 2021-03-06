package com.getcapacitor.plugin;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.welie.blessed.BluetoothBytesParser;
import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothCentralManagerCallback;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.BluetoothPeripheralCallback;
import com.welie.blessed.GattStatus;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_HIGH;

class Scanner {
    private final Context context;
    private final Callback callback;
    private final Handler handler;

    private static Scanner instance = null;

    private BluetoothCentralManager central;

    private ScanCallback scanCallback;

    private static long ttlSeconds = 3;

    private class Message {
        Runnable runnable;

        BluetoothPeripheral peripheral;

        UUID uuid;
        byte[] content;

        long lastSeen;

        public Message(BluetoothPeripheral peripheral, UUID uuid, byte[] content) {
            this.runnable = () -> {
                Log.i("Scanner::Message",
                        String.format(
                                "Runnable(uuid=%s)",
                                this.uuid));

                // Check if we are still alive.
                synchronized (messages) {
                    Message message = messages.get(this.uuid);
                    if (message != null) {
                        callback.onLost(this.uuid, message.content);
                    }
                }

                this.kill();
            };

            synchronized (peripherals) {
                this.peripheral = peripheral;

                this.uuid = uuid;
                this.content = content;

                this.lastSeen = System.currentTimeMillis();

                handler.postDelayed(this.runnable, ttlSeconds * 1000);

                peripherals.put(this.peripheral, this);
            }
        }

        public void kill() {
            Log.i("Scanner::Message::kill",
                    String.format(
                            "Runnable(uuid=%s)",
                            this.uuid));

            handler.removeCallbacks(this.runnable);

            // Kill yourself.
            synchronized (peripherals) {
                peripherals.remove(this.peripheral);
            }

            synchronized (messages) {
                messages.remove(this.uuid);
            }
        }

        public void alive() {
            this.lastSeen = System.currentTimeMillis();

            Log.i("Scanner::Message::alive",
                    String.format(
                            "Runnable(uuid=%s)",
                            this.uuid));

            handler.removeCallbacks(this.runnable);

            // Check if we are still alive.
            synchronized (messages) {
                if (messages.containsKey(this.uuid)) {
                    Log.i("Scanner::Message::post",
                            String.format(
                                    "Runnable(uuid=%s, ttlSeconds=%d)",
                                    this.uuid, ttlSeconds));

                    handler.postDelayed(this.runnable, ttlSeconds * 1000);
                }
            }
        }
    }

    class Packet {
        byte[] data = new byte[]{};

        public void append(byte[] data) {
            this.data = append(this.data, data);
        }

        byte[] append(byte[] a, byte[] b) {
            byte[] result = new byte[a.length + b.length];

            System.arraycopy(a, 0, result, 0, a.length);
            System.arraycopy(b, 0, result, a.length, b.length);

            return result;
        }
    }

    private final Map<UUID, Message> messages = new HashMap<>();
    private final Map<UUID, Packet> packets = new HashMap<>();
    private final Map<BluetoothPeripheral, Message> peripherals = new HashMap<>();

    // Callback for peripherals
    private final BluetoothPeripheralCallback peripheralCallback = new BluetoothPeripheralCallback() {
        @Override
        public void onServicesDiscovered(BluetoothPeripheral peripheral) {
            Log.i("Scanner",
                    String.format(
                            "onServicesDiscovered(peripheral=%s, packets=%s)",
                            peripheral, packets));

            peripheral.requestMtu(Constants.GATT_MTU_SIZE);

            // Request a new connection priority
            peripheral.requestConnectionPriority(CONNECTION_PRIORITY_HIGH);

//            peripheral.readCharacteristic(Constants.SERVICE_UUID, Constants.CHARACTERISTIC_UUID);

            BluetoothGattCharacteristic characteristic =
                    peripheral.getCharacteristic(Constants.SERVICE_UUID, Constants.CHARACTERISTIC_UUID);

            if (characteristic != null) {
                for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                    UUID uuid = descriptor.getUuid();

                    synchronized (messages) {
                        Message message = messages.get(uuid);

                        if (message != null) {
                            message.alive();
                        } else {
                            packets.put(uuid, new Packet());

                            peripheral.readDescriptor(descriptor);
                        }
                    }
                }
            }

            if (packets.isEmpty()) {
                try {
                    // BluetoothGatt gatt
                    final Method refresh = peripheral.getClass().getMethod("refresh");
                    if (refresh != null) {
                        refresh.invoke(peripheral);
                    }
                } catch (Exception e) {
                    // Log it
                }

                central.cancelConnection(peripheral);
            }
        }

        @Override
        public void onDescriptorRead(final BluetoothPeripheral peripheral, byte[] value, final BluetoothGattDescriptor descriptor, final GattStatus status) {
            Log.i("Scanner",
                    String.format(
                            "onDescriptorRead(peripheral=%s, value=%s, descriptor=%s, status=%s)",
                            peripheral, value.length, descriptor, status));

            if (packets.isEmpty()) {
                try {
                    // BluetoothGatt gatt
                    final Method refresh = peripheral.getClass().getMethod("refresh");
                    if (refresh != null) {
                        refresh.invoke(peripheral);
                    }
                } catch (Exception e) {
                    // Log it
                }

                central.cancelConnection(peripheral);
                return;
            }

            UUID uuid = descriptor.getUuid();

            Packet packet = packets.get(uuid);

            if (packet != null) {
                if (status == GattStatus.SUCCESS) {
                    BluetoothBytesParser parser = new BluetoothBytesParser(value);

                    byte[] data = parser.getValue();
                    packet.append(data);

                    peripheral.readDescriptor(descriptor);
                } else if (status == GattStatus.INVALID_OFFSET) {
                    synchronized (messages) {
                        callback.onFound(uuid, packet.data);

                        packets.remove(uuid);

                        Message message = new Message(peripheral, uuid, packet.data);
                        messages.put(uuid, message);
                    }
                }
            }
        }

//        @Override
//        public void onMtuChanged(@NotNull final BluetoothPeripheral peripheral, int mtu, @NotNull GattStatus status) {
//            Log.i("Scanner",
//                    String.format(
//                            "onMtuChanged(peripheral=%s, mtu=%s, status=%s)",
//                            peripheral, mtu, status));
//        }
    };

    // Callback for central
    private final BluetoothCentralManagerCallback bluetoothCentralManagerCallback = new BluetoothCentralManagerCallback() {
        @Override
        public void onDiscoveredPeripheral(BluetoothPeripheral peripheral, ScanResult scanResult) {
            Log.i("Scanner",
                    String.format(
                            "onDiscoveredPeripheral(peripheral=%s, scanResult=%s)",
                            peripheral, scanResult));

            synchronized (peripherals) {
                Message message = peripherals.get(peripheral);
                if (message != null) {
                    message.alive();
                }
            }

            //                    central.stopScan();
            central.connectPeripheral(peripheral, peripheralCallback);
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
            Log.i("Scanner",
                    String.format(
                            "onScanFailed(state=%s)",
                            errorCode));

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

        synchronized (peripherals) {
            peripherals.clear();
        }
        synchronized (messages) {
            // Note: existing messages might time out and emit onLost events.
            messages.clear();
        }
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
        this.central = new BluetoothCentralManager(context, bluetoothCentralManagerCallback, handler);

        if (scanMode != null) {
            this.central.setScanMode(scanMode);
        }
    }

    public abstract static class Callback {
        public void onFound(UUID uuid, byte[] data) {
        }

        public void onLost(UUID uuid, byte[] data) {
        }

        public void onPermissionChanged(Boolean permissionGranted) {
        }
    }

    public abstract static class ScanCallback {
        public void onFailed(int errorCode) {
        }
    }
}
