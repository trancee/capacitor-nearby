package com.getcapacitor.community;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class NearbyEndpoint {

    private static final UUID SERVICE_UUID = UUID.fromString("57494e4b-0000-1000-8000-0805f9b34fb");
    public static final int MAXIMUM_PAYLOAD_SIZE = 0x1000000;

    @NonNull
    final NearbyConfig config;

    @NonNull
    final String endpointID;

    @Nullable
    final String endpointName;

    @Nullable
    final byte[] endpointInfo;

    @Nullable
    final Short channel;

    @Nullable
    final Integer rssi;

    @NonNull
    private final BluetoothDevice device;

    @Nullable
    private BluetoothGatt gatt;

    @Nullable
    private BluetoothSocket socket;

    long timestamp;

    @Nullable
    private ScheduledFuture<?> schedule;

    private Runnable command;

    long lastSeen;

    public static long ttlSeconds = 10;

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public NearbyEndpoint(
        @NonNull NearbyConfig config,
        @NonNull String endpointID,
        @Nullable String endpointName,
        @Nullable byte[] endpointInfo,
        @Nullable Short channel,
        @Nullable Integer rssi,
        @NonNull BluetoothDevice device
    ) {
        this.config = config;

        this.endpointID = endpointID;
        this.endpointName = endpointName;
        this.endpointInfo = endpointInfo;

        this.channel = channel;
        this.rssi = rssi;

        this.device = device;

        this.timestamp = System.currentTimeMillis();

        alive();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    protected void finalize() {
        kill();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void kill() {
        try {
            disconnect();
        } catch (IOException ignored) {}

        if (schedule != null) {
            schedule.cancel(true);
            schedule = null;
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void alive() {
        lastSeen = System.currentTimeMillis();

        if (schedule != null) {
            schedule.cancel(true);
            schedule = null;
        }

        schedule = Executors.newSingleThreadScheduledExecutor()
            .scheduleWithFixedDelay(
                () -> {
                    kill();

                    if (command != null) {
                        command.run();
                    }
                },
                ttlSeconds,
                ttlSeconds,
                TimeUnit.SECONDS
            );
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void connect() throws IOException {
        /*
        BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);

                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED:
                        if (callback != null) {
                            callback.onConnected();
                        }
                        break;
                    case BluetoothProfile.STATE_CONNECTING:
                        // TODO
                        break;
                    case BluetoothProfile.STATE_DISCONNECTING:
                        // TODO
                        break;
                    case BluetoothProfile.STATE_DISCONNECTED:
                        if (callback != null) {
                            callback.onDisconnected();
                        }
                        break;
                }
            }
        };

        gatt = device.connectGatt(config.getContext(), false, gattCallback);
        */

        // Get a BluetoothSocket for a connection with the
        // given BluetoothDevice
        if (channel != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // L2CAP (>= Android 10)
            socket = device.createInsecureL2capChannel(channel);
            // socket = device.createL2capChannel(channel);
        } else {
            // RFCOMM (< Android 10)
            socket = device.createInsecureRfcommSocketToServiceRecord(SERVICE_UUID);
            // socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID);
        }

        @Nullable
        IOException exception = null;

        for (int retries = 10; retries > 0; retries--) {
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                socket.connect();

                exception = null;
                break;
            } catch (IOException e) {
                exception = e;
            }
        }

        if (exception != null) {
            throw exception;
        }

        OutputStream outputStream;
        if ((outputStream = socket.getOutputStream()) != null) {
            outputStream.write(config.endpointID.getBytes());
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void disconnect() throws IOException {
        if (socket != null) {
            socket.close();
        }

        socket = null;
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void sendPayload(@NonNull byte[] payload) throws IOException {
        if (socket == null && channel != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ScheduledFuture<?> schedule = null;

                try (
                    BluetoothSocket socket = device.createInsecureL2capChannel(channel);
                    InputStream inputStream = socket.getInputStream();
                    OutputStream outputStream = socket.getOutputStream();
                ) {
                    schedule = Executors.newSingleThreadScheduledExecutor()
                        .schedule(
                            () -> {
                                try {
                                    inputStream.close();
                                    outputStream.close();
                                    socket.close();
                                } catch (IOException ignored) {}
                            },
                            5000,
                            TimeUnit.MILLISECONDS
                        );

                    IOException exception = null;
                    for (int retries = 10; retries > 0; retries--) {
                        try {
                            socket.connect();

                            exception = null;
                            break;
                        } catch (IOException e) {
                            exception = e;
                        }
                    }

                    if (exception != null) {
                        throw exception;
                    }

                    int length = payload.length;

                    if (length >= MAXIMUM_PAYLOAD_SIZE) {
                        throw new IOException("payload too large");
                    }

                    // https://raw.githubusercontent.com/krzyzanowskim/CryptoSwift/416a57ee940e0bdf29ef1dae042722d1b147b790/Sources/CryptoSwift/Checksum.swift
                    Checksum crc32 = new CRC32();
                    crc32.update(payload, 0, length);
                    long checksum = crc32.getValue();

                    // 1. Identify
                    outputStream.write(config.endpointID.getBytes());
                    // 2. Payload Length
                    outputStream.write(
                        new byte[] { (byte) ((length) & 0xff), (byte) ((length >> 8) & 0xff), (byte) ((length >> 16) & 0xff) }
                    );
                    // 3. Payload
                    outputStream.write(payload);
                    // 4. Checksum
                    outputStream.write(
                        new byte[] {
                            (byte) ((checksum) & 0xff),
                            (byte) ((checksum >> 8) & 0xff),
                            (byte) ((checksum >> 16) & 0xff),
                            (byte) ((checksum >> 24) & 0xff)
                        }
                    );

                    // 5. (N)ACK
                    if (!(inputStream.read() > 0)) {
                        throw new IOException("not acknowledged");
                    }
                } finally {
                    if (schedule != null) {
                        schedule.cancel(true);
                    }
                }
            }
        } else if (socket != null) {
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();

            int length = payload.length;

            if (length >= MAXIMUM_PAYLOAD_SIZE) {
                throw new IOException("payload too large");
            }

            // https://raw.githubusercontent.com/krzyzanowskim/CryptoSwift/416a57ee940e0bdf29ef1dae042722d1b147b790/Sources/CryptoSwift/Checksum.swift
            Checksum crc32 = new CRC32();
            crc32.update(payload, 0, length);
            long checksum = crc32.getValue();

            // 1. Identify
            // outputStream.write(config.endpointID.getBytes());
            // 2. Payload Length
            outputStream.write(new byte[] { (byte) ((length) & 0xff), (byte) ((length >> 8) & 0xff), (byte) ((length >> 16) & 0xff) });
            // 3. Payload
            outputStream.write(payload);
            // 4. Checksum
            outputStream.write(
                new byte[] {
                    (byte) ((checksum) & 0xff),
                    (byte) ((checksum >> 8) & 0xff),
                    (byte) ((checksum >> 16) & 0xff),
                    (byte) ((checksum >> 24) & 0xff)
                }
            );

            // 5. (N)ACK
            if (!(inputStream.read() > 0)) {
                throw new IOException("not acknowledged");
            }
            /*
                int length = payload.length;

                if (length >= MAXIMUM_PAYLOAD_SIZE) {
                    return false;
                }

                outputStream.write(new byte[]{(byte) (length), (byte) (length >> 8), (byte) (length >> 16)});
                outputStream.write(payload);

                return true;
                */
        }
    }

    public NearbyEndpoint onLost(final Runnable command) {
        this.command = command;

        return this;
    }
}
