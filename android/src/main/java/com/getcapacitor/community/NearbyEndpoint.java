package com.getcapacitor.community;

import static android.bluetooth.BluetoothProfile.*;
import static com.getcapacitor.community.Nearby.endpoints;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import java.io.IOException;
import java.util.UUID;

public class NearbyEndpoint {

    private static final UUID SERVICE_UUID = UUID.fromString("57494e4b-0000-1000-8000-0805f9b34fb");

    @NonNull
    String endpointID;

    @Nullable
    byte[] endpointInfo;

    @Nullable
    Short channel;

    @Nullable
    Integer rssi;

    @NonNull
    private final BluetoothDevice device;

    @Nullable
    private BluetoothGatt gatt;

    @Nullable
    private BluetoothSocket socket;

    @NonNull
    private final String address;

    int state;
    //private boolean isConnected;

    long timestamp;

    final Handler handler = new Handler();
    final Runnable runnable;

    long lastSeen;

    public static long ttlSeconds = 10;

    public NearbyEndpoint(
        @NonNull String endpointID,
        @Nullable byte[] endpointInfo,
        @Nullable Short channel,
        @Nullable Integer rssi,
        @NonNull BluetoothDevice device,
        final Runnable runnable
    ) {
        this.endpointID = endpointID;
        this.endpointInfo = endpointInfo;

        this.channel = channel;
        this.rssi = rssi;

        this.device = device;
        this.address = device.getAddress();

        this.state = STATE_DISCONNECTED;
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

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Nullable
    public BluetoothSocket getSocket() {
        if (socket == null) {
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
            } catch (IOException e) {
                socket = null;

                throw new RuntimeException(e);
            }
        }
        return socket;
    }
    /*
    public void isConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }
    public boolean isConnected() { return isConnected; }
     */
}
