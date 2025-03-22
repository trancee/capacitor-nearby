package com.getcapacitor.community;

import static com.getcapacitor.community.NearbyHelper.ENDPOINT_ID_LENGTH;
import static java.lang.Thread.sleep;

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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

public class NearbyAdvertiser {

    private static NearbyAdvertiser instance = null;

    @NonNull
    private final BluetoothAdapter adapter;

    @Nullable
    private BluetoothServerSocket socket;

    @NonNull
    private final UUID serviceUUID;

    @NonNull
    private final String endpointName;

    @Nullable
    private final UUID endpointUUID;

    Integer advertiseMode = AdvertiseSettings.ADVERTISE_MODE_BALANCED;
    Integer txPowerLevel = AdvertiseSettings.ADVERTISE_TX_POWER_HIGH;

    private BluetoothLeAdvertiser advertiser;
    private AdvertiseCallback advertiseCallback;

    private boolean isAdvertising;

    public static final int MAXIMUM_DATA_SIZE = 14;
    public static final int THREAD_SLEEP_TIME = 100; // millis

    public static synchronized NearbyAdvertiser getInstance(
        @NonNull BluetoothAdapter adapter,
        @NonNull UUID serviceUUID,
        @NonNull String endpointName,
        @Nullable UUID endpointUUID
    ) {
        if (instance == null) {
            instance = new NearbyAdvertiser(adapter, serviceUUID, endpointName, endpointUUID);
        }

        return instance;
    }

    NearbyAdvertiser(
        @NonNull BluetoothAdapter adapter,
        @NonNull UUID serviceUUID,
        @NonNull String endpointName,
        @Nullable UUID endpointUUID
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

    public void start(@Nullable byte[] data) {
        start(data, null);
    }

    @SuppressLint("MissingPermission")
    public void start(@Nullable byte[] data, Callback callback) {
        if (isAdvertising) {
            stop();
        }

        @Nullable
        Short channel = null;

        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // L2CAP (>= Android 10)
                socket = adapter.listenUsingInsecureL2capChannel();
                // socket = adapter.listenUsingL2capChannel();
                channel = (short) socket.getPsm();
            } else {
                // RFCOMM (< Android 10)
                socket = adapter.listenUsingInsecureRfcommWithServiceRecord(endpointName, endpointUUID);
                // socket = adapter.listenUsingRfcommWithServiceRecord(endpointName, endpointUUID);
            }

            Thread acceptThread = new Thread(() -> {
                if (socket != null) {
                    BluetoothSocket client;

                    try {
                        while ((client = socket.accept()) != null) {
                            int packetSize = client.getMaxReceivePacketSize();

                            InputStream inputStream;

                            if ((inputStream = client.getInputStream()) != null) {
                                Thread connectThread = new Thread(() -> {
                                    byte[] input = new byte[packetSize];

                                    try {
                                        int read;

                                        main: while ((read = inputStream.available()) != -1) {
                                            if (read == 0) {
                                                sleep(THREAD_SLEEP_TIME);
                                                continue;
                                            }

                                            // wait for data to be available
                                            while ((read = inputStream.available()) <= 0) {
                                                if (read == -1) break main;
                                                sleep(THREAD_SLEEP_TIME);
                                            }

                                            // 1. EndpointID
                                            if ((inputStream.read(input)) != ENDPOINT_ID_LENGTH) {
                                                continue;
                                            }

                                            @Nullable
                                            String endpointID = new String(input, 0, ENDPOINT_ID_LENGTH);

                                            // wait for data to be available
                                            while ((read = inputStream.available()) <= 0) {
                                                if (read == -1) break main;
                                                sleep(THREAD_SLEEP_TIME);
                                            }

                                            // 2. Payload Size
                                            if ((inputStream.read(input)) != 4) {
                                                continue;
                                            }

                                            int payloadSize = ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN).getInt();
                                            ByteBuffer buffer = ByteBuffer.allocate(payloadSize);

                                            // 3. Payload Data
                                            while (inputStream.available() > 0) {
                                                buffer.put(input, 0, inputStream.read(input));
                                            }

                                            if (callback != null) {
                                                byte[] payload = buffer.array();

                                                callback.onPayload(endpointID, payload);
                                            }
                                        }
                                    } catch (Exception exception) {
                                        if (callback != null) callback.onFailure(exception);
                                    }
                                });

                                if (callback != null) {
                                    String endpointID = client.getRemoteDevice().getAddress();

                                    callback.onConnected(endpointID);
                                }

                                connectThread.start();
                            }
                        }
                    } catch (IOException exception) {
                        if (callback != null) {
                            callback.onDisconnected(exception.getMessage());
                        }
                    } catch (Exception exception) {
                        if (callback != null) callback.onFailure(exception);
                    }
                }
            });

            acceptThread.start();
        } catch (Exception exception) {
            callback.onFailure(exception);
            return;
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

        {
            byte size = (byte) ((data != null) ? data.length : 0);

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

            if (data != null) buffer.put(data);
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

        public void onConnected(String endpointID) {}
        public void onDisconnected(String endpointID) {}

        public void onPayload(String endpointID, byte[] payload) {}
    }
}
