package com.getcapacitor.community;

import static com.getcapacitor.community.NearbyHelper.EndpointID;

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

    @NonNull
    private final EndpointID endpointID;

    Integer advertiseMode = AdvertiseSettings.ADVERTISE_MODE_BALANCED;
    Integer txPowerLevel = AdvertiseSettings.ADVERTISE_TX_POWER_HIGH;

    private BluetoothLeAdvertiser advertiser;
    private AdvertiseCallback advertiseCallback;

    private boolean isAdvertising;

    public static synchronized NearbyAdvertiser getInstance(
        @NonNull BluetoothAdapter adapter,
        @NonNull UUID serviceUUID,
        @NonNull EndpointID endpointID
    ) {
        if (instance == null) {
            instance = new NearbyAdvertiser(adapter, serviceUUID, endpointID);
        }

        return instance;
    }

    NearbyAdvertiser(@NonNull BluetoothAdapter adapter, @NonNull UUID serviceUUID, @NonNull EndpointID endpointID) {
        this.adapter = adapter;

        this.serviceUUID = serviceUUID;
        this.endpointID = endpointID;
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
            .setTimeout(0)
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

        builder.addServiceUuid(new ParcelUuid(endpointID.uuid()));

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

        public void onSuccess(AdvertiseSettings settings) {}

        public void onFailure(Exception exception) {}
    }
    /*
    private static final int MAX_LEGACY_ADVERTISING_DATA_BYTES = 31;
    // Each fields need one byte for field length and another byte for field type.
    private static final int OVERHEAD_BYTES_PER_FIELD = 2;
    // Flags field will be set by system.
    private static final int FLAGS_FIELD_BYTES = 3;
    private static final int MANUFACTURER_SPECIFIC_DATA_LENGTH = 2;

    // Compute the size of advertisement data or scan resp
    @SuppressLint("NewApi")
    private int totalBytes(AdvertiseData data, boolean isFlagsIncluded) {
        if (data == null) return 0;
        // Flags field is omitted if the advertising is not connectable.
        int size = (isFlagsIncluded) ? FLAGS_FIELD_BYTES : 0;
        if (data.getServiceUuids() != null) {
            int num16BitUuids = 0;
            int num32BitUuids = 0;
            int num128BitUuids = 0;
            for (ParcelUuid uuid : data.getServiceUuids()) {
                if (BluetoothUuid.is16BitUuid(uuid)) {
                    ++num16BitUuids;
                } else if (BluetoothUuid.is32BitUuid(uuid)) {
                    ++num32BitUuids;
                } else {
                    ++num128BitUuids;
                }
            }
            // 16 bit service uuids are grouped into one field when doing advertising.
            if (num16BitUuids != 0) {
                size += OVERHEAD_BYTES_PER_FIELD + num16BitUuids * BluetoothUuid.UUID_BYTES_16_BIT;
            }
            // 32 bit service uuids are grouped into one field when doing advertising.
            if (num32BitUuids != 0) {
                size += OVERHEAD_BYTES_PER_FIELD + num32BitUuids * BluetoothUuid.UUID_BYTES_32_BIT;
            }
            // 128 bit service uuids are grouped into one field when doing advertising.
            if (num128BitUuids != 0) {
                size +=
                        OVERHEAD_BYTES_PER_FIELD
                                + num128BitUuids * BluetoothUuid.UUID_BYTES_128_BIT;
            }
        }
        if (data.getServiceSolicitationUuids() != null) {
            int num16BitUuids = 0;
            int num32BitUuids = 0;
            int num128BitUuids = 0;
            for (ParcelUuid uuid : data.getServiceSolicitationUuids()) {
                if (BluetoothUuid.is16BitUuid(uuid)) {
                    ++num16BitUuids;
                } else if (BluetoothUuid.is32BitUuid(uuid)) {
                    ++num32BitUuids;
                } else {
                    ++num128BitUuids;
                }
            }
            // 16 bit service uuids are grouped into one field when doing advertising.
            if (num16BitUuids != 0) {
                size += OVERHEAD_BYTES_PER_FIELD + num16BitUuids * BluetoothUuid.UUID_BYTES_16_BIT;
            }
            // 32 bit service uuids are grouped into one field when doing advertising.
            if (num32BitUuids != 0) {
                size += OVERHEAD_BYTES_PER_FIELD + num32BitUuids * BluetoothUuid.UUID_BYTES_32_BIT;
            }
            // 128 bit service uuids are grouped into one field when doing advertising.
            if (num128BitUuids != 0) {
                size +=
                        OVERHEAD_BYTES_PER_FIELD
                                + num128BitUuids * BluetoothUuid.UUID_BYTES_128_BIT;
            }
        }
        for (TransportDiscoveryData transportDiscoveryData : data.getTransportDiscoveryData()) {
            size += OVERHEAD_BYTES_PER_FIELD + transportDiscoveryData.totalBytes();
        }
        for (ParcelUuid uuid : data.getServiceData().keySet()) {
            int uuidLen = BluetoothUuid.uuidToBytes(uuid).length;
            size +=
                    OVERHEAD_BYTES_PER_FIELD
                            + uuidLen
                            + byteLength(data.getServiceData().get(uuid));
        }
        for (int i = 0; i < data.getManufacturerSpecificData().size(); ++i) {
            size +=
                    OVERHEAD_BYTES_PER_FIELD
                            + MANUFACTURER_SPECIFIC_DATA_LENGTH
                            + byteLength(data.getManufacturerSpecificData().valueAt(i));
        }
        if (data.getIncludeTxPowerLevel()) {
            size += OVERHEAD_BYTES_PER_FIELD + 1; // tx power level value is one byte.
        }
        if (data.getIncludeDeviceName()) {
//            final int length = mBluetoothAdapter.getNameLengthForAdvertise();
//            if (length >= 0) {
//                size += OVERHEAD_BYTES_PER_FIELD + length;
//            }
        }
        return size;
    }

    private int byteLength(byte[] array) {
        return array == null ? 0 : array.length;
    }
*/
}
/*
class BluetoothUuid {
    public static final ParcelUuid BASE_UUID =
            ParcelUuid.fromString("00000000-0000-1000-8000-00805F9B34FB");
    public static final int UUID_BYTES_16_BIT = 2;
    public static final int UUID_BYTES_32_BIT = 4;
    public static final int UUID_BYTES_128_BIT = 16;

    public static boolean is16BitUuid(ParcelUuid parcelUuid) {
        UUID uuid = parcelUuid.getUuid();
        if (uuid.getLeastSignificantBits() != BASE_UUID.getUuid().getLeastSignificantBits()) {
            return false;
        }
        return ((uuid.getMostSignificantBits() & 0xFFFF0000FFFFFFFFL) == 0x1000L);
    }

    public static boolean is32BitUuid(ParcelUuid parcelUuid) {
        UUID uuid = parcelUuid.getUuid();
        if (uuid.getLeastSignificantBits() != BASE_UUID.getUuid().getLeastSignificantBits()) {
            return false;
        }
        if (is16BitUuid(parcelUuid)) {
            return false;
        }
        return ((uuid.getMostSignificantBits() & 0xFFFFFFFFL) == 0x1000L);
    }

    public static byte[] uuidToBytes(ParcelUuid uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("uuid cannot be null");
        }

        if (is16BitUuid(uuid)) {
            byte[] uuidBytes = new byte[UUID_BYTES_16_BIT];
            int uuidVal = getServiceIdentifierFromParcelUuid(uuid);
            uuidBytes[0] = (byte) (uuidVal & 0xFF);
            uuidBytes[1] = (byte) ((uuidVal & 0xFF00) >> 8);
            return uuidBytes;
        }

        if (is32BitUuid(uuid)) {
            byte[] uuidBytes = new byte[UUID_BYTES_32_BIT];
            int uuidVal = getServiceIdentifierFromParcelUuid(uuid);
            uuidBytes[0] = (byte) (uuidVal & 0xFF);
            uuidBytes[1] = (byte) ((uuidVal & 0xFF00) >> 8);
            uuidBytes[2] = (byte) ((uuidVal & 0xFF0000) >> 16);
            uuidBytes[3] = (byte) ((uuidVal & 0xFF000000) >> 24);
            return uuidBytes;
        }

        // Construct a 128 bit UUID.
        long msb = uuid.getUuid().getMostSignificantBits();
        long lsb = uuid.getUuid().getLeastSignificantBits();

        byte[] uuidBytes = new byte[UUID_BYTES_128_BIT];
        ByteBuffer buf = ByteBuffer.wrap(uuidBytes).order(ByteOrder.LITTLE_ENDIAN);
        buf.putLong(8, msb);
        buf.putLong(0, lsb);
        return uuidBytes;
    }

    private static int getServiceIdentifierFromParcelUuid(ParcelUuid parcelUuid) {
        UUID uuid = parcelUuid.getUuid();
        long value = (uuid.getMostSignificantBits() & 0xFFFFFFFF00000000L) >>> 32;
        return (int) value;
    }
}
*/
