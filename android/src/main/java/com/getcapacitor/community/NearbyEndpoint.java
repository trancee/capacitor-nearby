package com.getcapacitor.community;

import static com.getcapacitor.community.Nearby.endpoints;
import static com.getcapacitor.community.NearbyHelper.EndpointID;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import androidx.annotation.Nullable;

public class NearbyEndpoint {

    EndpointID endpointID;

    @Nullable
    byte[] endpointInfo;

    Integer rssi;
    BluetoothDevice device;

    long timestamp;

    final Handler handler = new Handler();
    final Runnable runnable;

    long lastSeen;

    public static long ttlSeconds = 10;

    public NearbyEndpoint(
        EndpointID endpointID,
        @Nullable byte[] endpointInfo,
        Integer rssi,
        BluetoothDevice device,
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
}
