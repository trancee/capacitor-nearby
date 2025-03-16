package com.getcapacitor.community;

import static com.getcapacitor.community.Nearby.endpoints;
import static com.getcapacitor.community.NearbyHelper.EndpointID;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

public class NearbyEndpoint {

    EndpointID endpointID;

    @Nullable
    byte[] endpointInfo;

    Integer rssi;

    @NonNull
    private final BluetoothDevice device;

    @Nullable
    private BluetoothGatt gatt;

    long timestamp;

    final Handler handler = new Handler();
    final Runnable runnable;

    long lastSeen;

    public static long ttlSeconds = 10;

    public NearbyEndpoint(
        EndpointID endpointID,
        @Nullable byte[] endpointInfo,
        Integer rssi,
        @NonNull final BluetoothDevice device,
        final Runnable runnable
    ) {
        this.endpointID = endpointID;
        this.endpointInfo = endpointInfo;

        this.rssi = rssi;
        this.device = device;

        this.runnable = runnable;

        this.timestamp = System.currentTimeMillis();

        lastSeen = System.currentTimeMillis();
        handler.postDelayed(runnable, ttlSeconds * 1000);

        endpoints.put(endpointID.toString(), this);
    }

    public void kill() {
        handler.removeCallbacks(runnable);

        // Kill yourself.
        endpoints.remove(endpointID.toString());
    }

    public void alive() {
        handler.removeCallbacks(runnable);

        // Check if we are still alive.
        if (endpoints.containsKey(endpointID.toString())) {
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
    public BluetoothDevice getDevice() {
        return device;
    }

    @Nullable
    public BluetoothGatt getGatt() {
        return gatt;
    }
}
