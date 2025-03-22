package com.getcapacitor.community;

import static com.getcapacitor.community.Nearby.endpoints;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

public class NearbyEndpoint {

    private static final UUID SERVICE_UUID = UUID.fromString("57494e4b-0000-1000-8000-0805f9b34fb");

    @NonNull
    String endpointID;

    @Nullable
    String endpointName;
    @Nullable
    byte[] endpointInfo;

    @Nullable
    Short channel;

    @Nullable
    Integer rssi;

    @NonNull
    private final String address;

    @Nullable
    private BluetoothGatt gatt;

    @Nullable
    private BluetoothSocket socket;

    int state;
    //private boolean isConnected;

    long timestamp;

    final Handler handler = new Handler();
    final Runnable runnable;

    long lastSeen;

    public static long ttlSeconds = 10;

    public NearbyEndpoint(
            @NonNull String endpointID,
            @Nullable String endpointName,
            @Nullable byte[] endpointInfo,
            @Nullable Short channel,
            @Nullable Integer rssi,
            @NonNull String address,
            final Runnable runnable
    ) {
        this.endpointID = endpointID;
        this.endpointName = endpointName;
        this.endpointInfo = endpointInfo;

        this.channel = channel;
        this.rssi = rssi;

        this.address = address;

        this.state = BluetoothProfile.STATE_DISCONNECTED;
        //this.isConnected = false;

        this.runnable = runnable;

        this.timestamp = System.currentTimeMillis();

        lastSeen = System.currentTimeMillis();
        handler.postDelayed(runnable, ttlSeconds * 1000);

        endpoints.put(endpointID, this);
    }

    public void kill() {
        handler.removeCallbacks(runnable);

        // Kill yourself.
        endpoints.remove(endpointID);
    }

    public void alive() {
        handler.removeCallbacks(runnable);

        // Check if we are still alive.
        if (endpoints.containsKey(endpointID)) {
            lastSeen = System.currentTimeMillis();
            handler.postDelayed(runnable, ttlSeconds * 1000);
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void closeGatt() {
        if (this.gatt != null) {
            gatt.close();
        }

        this.gatt = null;
    }

    public void setGatt(@Nullable BluetoothGatt gatt) {
        this.gatt = gatt;
    }

    @NonNull
    public String getAddress() {
        return address;
    }

    @Nullable
    public BluetoothGatt getGatt() {
        return gatt;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public boolean connect(BluetoothDevice device) {
        if (socket == null || !socket.isConnected()) {
            try {
                if (channel != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // L2CAP (>= Android 10)
                    socket = device.createInsecureL2capChannel(channel);
                    // socket = device.createL2capChannel(channel);
                } else {
                    // RFCOMM (< Android 10)
                    socket = device.createInsecureRfcommSocketToServiceRecord(SERVICE_UUID);
                    // socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID);
                }

                socket.connect();
            } catch (IOException e) {
                disconnect();

                return false;
            }
        }

        return true;
    }

    public void disconnect() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }

            socket = null;
        }
    }

    public boolean send(@NonNull String endpointID, @NonNull byte[] payload) {
        if (socket != null && socket.isConnected()) {
            try (
                    OutputStream outputStream = socket.getOutputStream()
            ) {
                // 1. EndpointID
                outputStream.write(endpointID.getBytes());

                // 2. Payload Size
                outputStream.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(payload.length).array());

                // 3. Payload Data
                outputStream.write(payload);

                return true;
            } catch (IOException e) {
                disconnect();
            }
        }

        return false;
    }
}
