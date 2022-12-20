package com.getcapacitor.community;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import java.nio.ByteBuffer;
import java.util.UUID;

public class Advertiser {

    private static Advertiser instance = null;

    private final BluetoothAdapter adapter;

    private final UUID serviceUUID;

    Integer advertiseMode = AdvertiseSettings.ADVERTISE_MODE_BALANCED;
    Integer txPowerLevel = AdvertiseSettings.ADVERTISE_TX_POWER_HIGH;

    private BluetoothLeAdvertiser advertiser;
    private AdvertiseCallback advertiseCallback;

    private boolean mAdvertising;

    private final Handler handler = new Handler();
    private Runnable runnable;

    public static synchronized Advertiser getInstance(BluetoothAdapter adapter, UUID serviceUUID) {
        if (instance == null) {
            instance = new Advertiser(adapter, serviceUUID);
        }

        return instance;
    }

    Advertiser(BluetoothAdapter adapter, UUID serviceUUID) {
        this.adapter = adapter;

        this.serviceUUID = serviceUUID;
    }

    public Integer getAdvertiseMode() {
        return advertiseMode;
    }

    public void setAdvertiseMode(Integer advertiseMode) {
        this.advertiseMode = advertiseMode;
    }

    public Integer getTxPowerLevel() {
        return txPowerLevel;
    }

    public void setTxPowerLevel(Integer txPowerLevel) {
        this.txPowerLevel = txPowerLevel;
    }

    public void start(Beacon beacon) {
        start(beacon, null, null);
    }

    public void start(Beacon beacon, Callback callback) {
        start(beacon, null, callback);
    }

    @SuppressLint("MissingPermission")
    public void start(Beacon beacon, Integer ttlSeconds, Callback callback) {
        stopTimer();

        if (mAdvertising) {
            stop();
        }

        advertiser = adapter.getBluetoothLeAdvertiser();

        if (advertiser == null || !isBluetoothAvailable()) {
            int errorCode = AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED;

            if (callback != null) {
                callback.onFailure(errorCode, advertiseFailed(errorCode));
            }

            return;
        }

        // The AdvertiseSettings provide a way to adjust advertising preferences for each Bluetooth LE advertisement instance.
        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
            // Set advertise mode to control the advertising power and latency.
            .setAdvertiseMode(advertiseMode)
            // Set advertise TX power level to control the transmission power level for the advertising.
            .setTxPowerLevel(txPowerLevel)
            // Limit advertising to a given amount of time.
            // .setTimeout(30 * 1000)  // May not exceed 180000 milliseconds. A value of 0 will disable the time limit.
            // Set whether the advertisement type should be connectable or non-connectable.
            .setConnectable(false)
            .build();

        // Advertise data packet container for Bluetooth LE advertising.
        // This represents the data to be advertised as well as the scan response data for active scans.
        AdvertiseData advertiseData = new AdvertiseData.Builder()
            // Add a service UUID to advertise data.
            .addServiceUuid(new ParcelUuid(serviceUUID))
            .addServiceUuid(new ParcelUuid(beacon.uuid()))
            // Whether the transmission power level should be included in the advertise packet.
            .setIncludeTxPowerLevel(false)
            // Set whether the device name should be included in advertise packet.
            .setIncludeDeviceName(false)
            .build();

        if (advertiseCallback == null) {
            // Bluetooth LE advertising callbacks, used to deliver advertising operation status.
            // https://developer.android.com/reference/android/bluetooth/le/AdvertiseCallback
            advertiseCallback =
                new AdvertiseCallback() {
                    @Override
                    // Callback triggered in response to BluetoothLeAdvertiser#startAdvertising indicating that the advertising has been started successfully.
                    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                        super.onStartSuccess(settingsInEffect);

                        mAdvertising = true;

                        if (ttlSeconds != null) {
                            startTimer(ttlSeconds, callback);
                        }

                        if (callback != null) {
                            callback.onSuccess(settingsInEffect);
                        }
                    }

                    @Override
                    // Callback when advertising could not be started.
                    public void onStartFailure(int errorCode) {
                        Log.e("AdvertiseCallback", String.format("onStartFailure(errorCode=%d)", errorCode));

                        super.onStartFailure(errorCode);

                        stop();

                        if (callback != null) {
                            callback.onFailure(errorCode, advertiseFailed(errorCode));
                        }
                    }
                };
        }

        // java.lang.IllegalArgumentException: Legacy advertising data too big
        // java.lang.IllegalArgumentException: Advertising data too big
        advertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback);
    }

    @SuppressLint("MissingPermission")
    public void stop() {
        stopTimer();

        if (advertiser != null && advertiseCallback != null) {
            if (isBluetoothAvailable()) {
                advertiser.stopAdvertising(advertiseCallback);
            }

            advertiseCallback = null;
        }

        mAdvertising = false;
    }

    public boolean isAdvertising() {
        return mAdvertising;
    }

    public boolean isBluetoothAvailable() {
        return (adapter != null && adapter.isEnabled() && adapter.getState() == BluetoothAdapter.STATE_ON);
    }

    private String advertiseFailed(int errorCode) {
        switch (errorCode) {
            //            case AdvertiseCallback.ADVERTISE_SUCCESS:
            //                return "The requested operation was successful.";
            case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                return "Failed to start advertising as the advertise data to be broadcasted is larger than 31 bytes.";
            case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                return "Failed to start advertising because no advertising instance is available.";
            case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                return "Failed to start advertising as the advertising is already started.";
            case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                return "Operation failed due to an internal error.";
            case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                return "This feature is not supported on this platform.";
            default:
                return "Unknown error.";
        }
    }

    /**
     * Timer
     */

    public void startTimer(Integer ttlSeconds, Callback callback) {
        if (ttlSeconds != null && callback != null) {
            runnable = callback::onExpired;

            // Sets the time to live in seconds for the publish or subscribe.
            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(runnable, ttlSeconds * 1000);
        }
    }

    public void stopTimer() {
        if (runnable != null) {
            handler.removeCallbacks(runnable);

            runnable = null;
        }
    }

    /**
     * Callback
     */

    public abstract static class Callback {

        public void onSuccess(AdvertiseSettings settings) {}

        public void onFailure(int errorCode, String errorMessage) {}

        public void onExpired() {}
    }

    /**
     * Beacon
     */

    public static class Beacon {

        UUID uuid;
        byte[] data;

        long timestamp;

        public Beacon(UUID uuid, byte[] data) {
            this.uuid = uuid;
            this.data = data;

            this.timestamp = System.currentTimeMillis();
        }

        public Beacon(UUID uuid) {
            this(uuid, null);
        }

        public UUID uuid() {
            return this.uuid;
        }

        public byte[] data() { // 22 - 4
            int length = 0; // 7

            if (this.data != null) {
                length += this.data.length;
            }

            ByteBuffer data = ByteBuffer.wrap(new byte[length]);

            if (this.data != null) {
                data.put(this.data);
            }

            return data.array();
        }
    }
}
