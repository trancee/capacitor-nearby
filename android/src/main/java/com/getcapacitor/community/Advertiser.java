package com.getcapacitor.community;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;

import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

public class Advertiser {
    private static Advertiser instance = null;

    private BluetoothAdapter adapter;

    Integer advertiseMode = AdvertiseSettings.ADVERTISE_MODE_BALANCED;
    Integer txPowerLevel = AdvertiseSettings.ADVERTISE_TX_POWER_HIGH;

    private BluetoothLeAdvertiser advertiser;
    private AdvertiseCallback advertiseCallback;

    private boolean mAdvertising;

    private Handler handler = new Handler();
    private Runnable runnable;

    public static synchronized Advertiser getInstance(BluetoothAdapter adapter) {
        if (instance == null) {
            instance = new Advertiser(adapter);
        }

        return instance;
    }

    Advertiser(BluetoothAdapter adapter) {
        this.adapter = adapter;
    }

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public void start(Beacon beacon, Callback callback) {
        start(beacon, null, callback);
    }

    public void start(Beacon beacon, Integer ttlSeconds, Callback callback) {
        // Log.i("Advertiser",
        //         String.format(
        //                 "start(beacon={uuid=%s, data=%s})",
        //                 beacon.uuid(), bytesToHex(beacon.data())));

        stopTimer();

        if (mAdvertising) {
            stop();
        }

        advertiser =
                adapter.getBluetoothLeAdvertiser();

        if (advertiser == null) {
            int errorCode = AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED;

            callback.onFailure(errorCode, advertiseFailed(errorCode));
            return;
        }

        // The AdvertiseSettings provide a way to adjust advertising preferences for each Bluetooth LE advertisement instance.
        AdvertiseSettings advertiseSettings =
                new AdvertiseSettings.Builder()
                        // Set advertise mode to control the advertising power and latency.
                        .setAdvertiseMode(advertiseMode)
                        // Set advertise TX power level to control the transmission power level for the advertising.
                        .setTxPowerLevel(txPowerLevel)
                        // Limit advertising to a given amount of time.
//                            .setTimeout(30 * 1000)  // May not exceed 180000 milliseconds. A value of 0 will disable the time limit.
                        // Set whether the advertisement type should be connectable or non-connectable.
                        .setConnectable(false)
                        .build();

        // Advertise data packet container for Bluetooth LE advertising.
        // This represents the data to be advertised as well as the scan response data for active scans.
        AdvertiseData advertiseData =
                new AdvertiseData.Builder()
                        // Add a service UUID to advertise data.
                        .addServiceUuid(new ParcelUuid(Constants.SERVICE_UUID))
//                        .addServiceData(new ParcelUuid(Constants.SERVICE_UUID), new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21})
                        .addServiceData(new ParcelUuid(beacon.uuid()), beacon.data())
//                        .addServiceData(new ParcelUuid(serviceUUID), bb.array())
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
                            // Log.i("AdvertiseCallback",
                            //         String.format(
                            //                 "onStartSuccess(settingsInEffect=%s)",
                            //                 settingsInEffect));

                            super.onStartSuccess(settingsInEffect);

                            mAdvertising = true;

                            if (ttlSeconds != null) {
                                startTimer(ttlSeconds);
                            }

                            callback.onSuccess(settingsInEffect);
                        }

                        @Override
                        // Callback when advertising could not be started.
                        public void onStartFailure(int errorCode) {
                            Log.e("AdvertiseCallback",
                                    String.format(
                                            "onStartFailure(errorCode=%d)",
                                            errorCode));

                            super.onStartFailure(errorCode);

                            stop();

                            callback.onFailure(errorCode, advertiseFailed(errorCode));
                        }
                    };
        }

        // java.lang.IllegalArgumentException: Legacy advertising data too big
        // java.lang.IllegalArgumentException: Advertising data too big
        advertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback);

//        mAdvertising = true;
    }

    public void stop() {
        stopTimer();

        if (advertiser != null && advertiseCallback != null) {
            advertiser.stopAdvertising(advertiseCallback);

            advertiseCallback = null;
        }

        mAdvertising = false;
    }

    public boolean isAdvertising() {
        return mAdvertising;
    }

    private String advertiseFailed(int errorCode) {
        switch (errorCode) {
            case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                return "Failed to start advertising as the advertising is already started.";
            case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                return "Failed to start advertising as the advertise data to be broadcasted is larger than 31 bytes.";
            case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                return "This feature is not supported on this platform.";
            case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                return "Operation failed due to an internal error.";
            case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                return "Failed to start advertising because no advertising instance is available.";
            default:
                return "Unknown error.";
        }
    }

    /**
     * Timer
     */
    public void startTimer(Integer ttlSeconds) {
        if (ttlSeconds != null) {
            runnable = new Runnable() {
                public void run() {
                    callback.onExpired();
                }
            };

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
        public void onSuccess(AdvertiseSettings settings) {
        }

        public void onFailure(int errorCode, String errorMessage) {
        }

        public void onExpired() {
        }
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

        public byte[] data() {  // 22 - 4
            int length = 0; // 7

            if (this.data != null) {
                length += this.data.length;
            }

            ByteBuffer data = ByteBuffer.wrap(new byte[length]);

//            data.putLong(this.timestamp);

//            byteBuffer.putLong(this.uuid.getMostSignificantBits());
//            byteBuffer.putLong(this.uuid.getLeastSignificantBits());

            if (this.data != null) {
                data.put(this.data);
            }

            return data.array();
        }
    }
}
