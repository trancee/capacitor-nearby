package com.getcapacitor.community;

import static com.getcapacitor.community.Nearby.endpoints;

import android.Manifest;
import android.bluetooth.BluetoothGatt;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

public class NearbyEndpoint {

    @NonNull
    String endpointID;

    @Nullable
    byte[] endpointInfo;

    @Nullable
    Integer rssi;

    @NonNull
    private final String address;

    @Nullable
    private BluetoothGatt gatt;

    private boolean isConnected;

    long timestamp;

    final Handler handler = new Handler();
    final Runnable runnable;

    long lastSeen;

    public static long ttlSeconds = 10;

    public NearbyEndpoint(
        @NonNull String endpointID,
        @Nullable byte[] endpointInfo,
        @Nullable Integer rssi,
        @NonNull String address,
        final Runnable runnable
    ) {
        this.endpointID = endpointID;
        this.endpointInfo = endpointInfo;

        this.rssi = rssi;
        this.address = address;

        this.isConnected = false;

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

    public void isConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }
    public boolean isConnected() { return isConnected; }
}
