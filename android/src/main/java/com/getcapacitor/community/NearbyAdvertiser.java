package com.getcapacitor.community;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.ParcelUuid;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.UUID;

public class NearbyAdvertiser {

    private static NearbyAdvertiser instance = null;

    @NonNull
    private final BluetoothAdapter adapter;

    @NonNull
    private final UUID serviceUUID;

    @Nullable
    private final UUID endpointUUID;

    Integer advertiseMode = AdvertiseSettings.ADVERTISE_MODE_BALANCED;
    Integer txPowerLevel = AdvertiseSettings.ADVERTISE_TX_POWER_HIGH;

    private BluetoothLeAdvertiser advertiser;
    private AdvertiseCallback advertiseCallback;

    private boolean isAdvertising;

    public static synchronized NearbyAdvertiser getInstance(
        @NonNull BluetoothAdapter adapter,
        @NonNull UUID serviceUUID,
        @Nullable  UUID endpointUUID
    ) {
        if (instance == null) {
            instance = new NearbyAdvertiser(adapter, serviceUUID, endpointUUID);
        }

        return instance;
    }

    NearbyAdvertiser(
            @NonNull BluetoothAdapter adapter,
            @NonNull UUID serviceUUID,
            @Nullable UUID endpointUUID
    ) {
        this.adapter = adapter;

        this.serviceUUID = serviceUUID;
        this.endpointUUID = endpointUUID;
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

    public void start() {
        start(null, null);
    }

    public void start(@Nullable byte[] data) {
        start(data, null);
    }

    @SuppressLint("MissingPermission")
    public void start(@Nullable byte[] data, Callback callback) {
        if (isAdvertising) {
            stop();
        }

        advertiser = adapter.getBluetoothLeAdvertiser();

        if (advertiser == null || !isBluetoothAvailable()) {
            int errorCode = AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED;

            if (callback != null) {
                Exception exception = new Exception(advertiseFailed(errorCode));

                callback.onFailure(exception);
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
            .setConnectable(true)
            .build();

        // Advertise data packet container for Bluetooth LE advertising.
        // This represents the data to be advertised as well as the scan response data for active scans.
        AdvertiseData.Builder builder = new AdvertiseData.Builder()
            // Add a service UUID to advertise data.
            .addServiceUuid(new ParcelUuid(serviceUUID))
            // Whether the transmission power level should be included in the advertise packet.
            .setIncludeTxPowerLevel(false)
            // Set whether the device name should be included in advertise packet.
            .setIncludeDeviceName(false);

        if (endpointUUID != null) {
            builder.addServiceUuid(new ParcelUuid(endpointUUID));
        }

        if (data != null && data.length > 0) {
            UUID dataUUID = NearbyHelper.makeUUID(data);

            builder.addServiceUuid(new ParcelUuid(dataUUID));
        }

        AdvertiseData advertiseData = builder.build();

        if (advertiseCallback == null) {
            // Bluetooth LE advertising callbacks, used to deliver advertising operation status.
            // https://developer.android.com/reference/android/bluetooth/le/AdvertiseCallback
            advertiseCallback = new AdvertiseCallback() {
                @Override
                // Callback triggered in response to BluetoothLeAdvertiser#startAdvertising indicating that the advertising has been started successfully.
                public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                    super.onStartSuccess(settingsInEffect);

                    isAdvertising = true;

                    if (callback != null) {
                        callback.onSuccess(settingsInEffect);
                    }
                }

                @Override
                // Callback when advertising could not be started.
                public void onStartFailure(int errorCode) {
                    // Log.e("AdvertiseCallback", String.format("onStartFailure(errorCode=%d)", errorCode));

                    super.onStartFailure(errorCode);

                    stop();

                    if (callback != null) {
                        Exception exception = new Exception(advertiseFailed(errorCode));

                        callback.onFailure(exception);
                    }
                }
            };
        }

        // AdvertiseData scanResponse = new AdvertiseData.Builder()
        //         .setIncludeDeviceName(true)
        //         .build();

        //        totalBytes(advertiseData, true);

        // java.lang.IllegalArgumentException: Legacy advertising data too big
        // java.lang.IllegalArgumentException: Advertising data too big
        // advertiser.startAdvertising(advertiseSettings, advertiseData, scanResponse, advertiseCallback);
        advertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback);
    }

    @SuppressLint("MissingPermission")
    public void stop() {
        if (advertiser != null && advertiseCallback != null) {
            if (isBluetoothAvailable()) {
                advertiser.stopAdvertising(advertiseCallback);
            }

            advertiseCallback = null;
        }

        isAdvertising = false;
    }

    public boolean isAdvertising() {
        return isAdvertising;
    }

    public boolean isBluetoothAvailable() {
        return adapter.isEnabled() && adapter.getState() == BluetoothAdapter.STATE_ON;
    }

    private String advertiseFailed(int errorCode) {
        return switch (errorCode) {
            case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "Failed to start advertising as the advertise data to be broadcasted is larger than 31 bytes.";
            case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Failed to start advertising because no advertising instance is available.";
            case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "Failed to start advertising as the advertising is already started.";
            case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "Operation failed due to an internal error.";
            case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "This feature is not supported on this platform.";
            default -> "Unknown error.";
        };
    }

    /**
     * Callback
     */

    public abstract static class Callback {

        public void onSuccess(AdvertiseSettings settings) {
        }

        public void onFailure(Exception exception) {
        }
    }
}
