package com.getcapacitor.community;

import static com.getcapacitor.community.Nearby.MISSING_ENDPOINT_ID;
import static com.getcapacitor.community.Nearby.MISSING_PAYLOAD_LENGTH;
import static com.getcapacitor.community.NearbyHelper.ENDPOINT_ID_LENGTH;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.Build;
import android.os.ParcelUuid;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class NearbyAdvertiser {

    private static NearbyAdvertiser instance = null;

    @NonNull
    private final BluetoothAdapter adapter;

    @Nullable
    private BluetoothServerSocket socket;

    @NonNull
    private final UUID serviceUUID;

    @Nullable
    private final String endpointName;

    @NonNull
    private final UUID endpointUUID;

    Integer advertiseMode = AdvertiseSettings.ADVERTISE_MODE_BALANCED;
    Integer txPowerLevel = AdvertiseSettings.ADVERTISE_TX_POWER_HIGH;

    private BluetoothLeAdvertiser advertiser;
    private AdvertiseCallback advertiseCallback;

    private boolean isAdvertising;

    public static final int MAXIMUM_DATA_SIZE = 14;

    @Nullable
    private Thread thread;

    public static synchronized NearbyAdvertiser getInstance(
        @NonNull BluetoothAdapter adapter,
        @NonNull UUID serviceUUID,
        @Nullable String endpointName,
        @NonNull UUID endpointUUID
    ) {
        if (instance == null) {
            instance = new NearbyAdvertiser(adapter, serviceUUID, endpointName, endpointUUID);
        }

        return instance;
    }

    NearbyAdvertiser(
        @NonNull BluetoothAdapter adapter,
        @NonNull UUID serviceUUID,
        @Nullable String endpointName,
        @NonNull UUID endpointUUID
    ) {
        this.adapter = adapter;

        this.serviceUUID = serviceUUID;

        this.endpointName = endpointName;
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

    public void start(@Nullable byte[] endpointInfo) {
        start(endpointInfo, null);
    }

    @SuppressLint("MissingPermission")
    public void start(@Nullable byte[] endpointInfo, Callback callback) {
        if (isAdvertising) {
            stop();
        }

        @NonNull
        final BluetoothServerSocket serverSocket;

        @Nullable
        Short channel = null;

        try {
            // Create a new listening server socket
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // L2CAP (>= Android 10)
                serverSocket = adapter.listenUsingInsecureL2capChannel();
                // socket = adapter.listenUsingL2capChannel();

                channel = (short) serverSocket.getPsm();
            } else {
                // RFCOMM (< Android 10)
                serverSocket = adapter.listenUsingInsecureRfcommWithServiceRecord(endpointName, endpointUUID);
                // socket = adapter.listenUsingRfcommWithServiceRecord(endpointName, endpointUUID);
            }
        } catch (Exception exception) {
            callback.onFailure(exception);

            return;
        }

        if (thread != null) {
            thread.interrupt();
            thread = null;
        }

        thread = new Thread(() -> {
            BluetoothSocket socket;

            try {
                while ((socket = serverSocket.accept()) != null) {
                    BluetoothSocket finalSocket = socket;

                    new Thread(() -> {
                        @Nullable
                        String endpointID = null;

                        ScheduledFuture<?> schedule;

                        try (
                            InputStream inputStream = finalSocket.getInputStream();
                            OutputStream outputStream = finalSocket.getOutputStream()
                        ) {
                            int bufferSize = finalSocket.getMaxReceivePacketSize();
                            byte[] buffer = new byte[bufferSize];

                            schedule = Executors.newSingleThreadScheduledExecutor()
                                .schedule(
                                    () -> {
                                        try {
                                            inputStream.close();
                                            outputStream.close();
                                        } catch (IOException ignored) {}
                                    },
                                    1000,
                                    TimeUnit.MILLISECONDS
                                );

                            try {
                                // 1. Identifier
                                if (inputStream.read(buffer) == ENDPOINT_ID_LENGTH) {
                                    endpointID = new String(buffer, 0, ENDPOINT_ID_LENGTH);

                                    callback.onConnected(endpointID);
                                } else {
                                    throw new IOException(MISSING_ENDPOINT_ID);
                                }
                            } finally {
                                if (schedule != null) {
                                    schedule.cancel(true);
                                }
                            }

                            while (true) {
                                // 2. Payload Length
                                if (inputStream.read(buffer) == 3) {
                                    int length =
                                        ((int) buffer[0] & 0xff) | (((int) buffer[1] & 0xff) << 8) | (((int) buffer[2] & 0xff) << 16);

                                    ByteBuffer payload = ByteBuffer.allocate(length);

                                    int read;
                                    // 3. Payload
                                    while ((read = inputStream.read(buffer)) > 0) {
                                        payload.put(buffer, 0, read);

                                        if (length == payload.position()) {
                                            break;
                                        }
                                    }

                                    // 4. Checksum
                                    if (inputStream.read(buffer) == 4) {
                                        long checksum =
                                            ((long) buffer[0] & 0xff) |
                                            (((long) buffer[1] & 0xff) << 8) |
                                            (((long) buffer[2] & 0xff) << 16) |
                                            (((long) buffer[3] & 0xff) << 24);

                                        Checksum crc32 = new CRC32();
                                        crc32.update(payload.array(), 0, payload.array().length);
                                        boolean ok = checksum == crc32.getValue();

                                        // 5. (N)ACK
                                        outputStream.write(ok ? 1 : 0);
                                    }

                                    callback.onReceived(endpointID, payload.array());
                                } else {
                                    throw new IOException(MISSING_PAYLOAD_LENGTH);
                                }
                            }
                        } catch (IOException exception) {
                            try {
                                finalSocket.close();
                            } catch (IOException ignored) {}

                            if (endpointID != null) {
                                callback.onDisconnected(endpointID);
                            }
                        }
                    }).start();
                }
            } catch (IOException ignored) {}
        });
        thread.start();

        advertiser = adapter.getBluetoothLeAdvertiser();

        if (advertiser == null || !isBluetoothAvailable()) {
            int errorCode = AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED;

            Exception exception = new Exception(advertiseFailed(errorCode));
            callback.onFailure(exception);

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
            .addServiceUuid(new ParcelUuid(endpointUUID))
            // Whether the transmission power level should be included in the advertise packet.
            .setIncludeTxPowerLevel(false)
            // Set whether the device name should be included in advertise packet.
            .setIncludeDeviceName(false);

        {
            byte size = (byte) ((endpointInfo != null) ? endpointInfo.length : 0);

            if (size > MAXIMUM_DATA_SIZE) {
                Exception exception = new Exception("data too large");
                callback.onFailure(exception);

                return;
            }

            ByteBuffer buffer = ByteBuffer.allocate(1 + size + ((channel != null) ? 1 : 0));

            if (channel != null) {
                size |= (byte) 0x80;
            }

            buffer.put(size);

            if (endpointInfo != null) buffer.put(endpointInfo);
            if (channel != null) buffer.put(channel.byteValue());

            UUID dataUUID = NearbyHelper.makeUUID(buffer.array());

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

                    callback.onSuccess(settingsInEffect);
                }

                @Override
                // Callback when advertising could not be started.
                public void onStartFailure(int errorCode) {
                    // Log.e("AdvertiseCallback", String.format("onStartFailure(errorCode=%d)", errorCode));

                    super.onStartFailure(errorCode);

                    stop();

                    Exception exception = new Exception(advertiseFailed(errorCode));
                    callback.onFailure(exception);
                }
            };
        }

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

        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (Exception ignored) {}

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

        public void onSuccess(AdvertiseSettings settings) {}

        public void onFailure(Exception exception) {}

        public void onConnected(@NonNull String endpointID) {}

        public void onDisconnected(@NonNull String endpointID) {}

        public void onReceived(@NonNull String endpointID, byte[] payload) {}
    }
}
