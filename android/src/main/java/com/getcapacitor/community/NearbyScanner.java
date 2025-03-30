package com.getcapacitor.community;

import static com.getcapacitor.community.NearbyHelper.BLUETOOTH_BASE_UUID_LSB;
import static com.getcapacitor.community.NearbyHelper.makeBuffer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.ParcelUuid;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NearbyScanner {

    private static NearbyScanner instance = null;

    @NonNull
    private final BluetoothAdapter adapter;

    @NonNull
    private final UUID serviceUUID;

    @NonNull
    private final UUID serviceMask;

    private Integer scanMode = ScanSettings.SCAN_MODE_BALANCED;
    private Integer reportDelay = 0;

    private BluetoothLeScanner scanner;
    private ScanCallback scanCallback;

    private boolean isScanning;

    public static synchronized NearbyScanner getInstance(
        @NonNull BluetoothAdapter adapter,
        @NonNull UUID serviceUUID,
        @NonNull UUID serviceMask
    ) {
        if (instance == null) {
            instance = new NearbyScanner(adapter, serviceUUID, serviceMask);
        }

        return instance;
    }

    NearbyScanner(@NonNull BluetoothAdapter adapter, @NonNull UUID serviceUUID, @NonNull UUID serviceMask) {
        this.adapter = adapter;

        this.serviceUUID = serviceUUID;
        this.serviceMask = serviceMask;
    }

    public void setScanMode(Integer scanMode) {
        this.scanMode = scanMode;
    }

    public Integer getScanMode() {
        return scanMode;
    }

    public void setReportDelay(Integer reportDelay) {
        this.reportDelay = reportDelay;
    }

    public Integer getReportDelay() {
        return reportDelay;
    }

    public void start() {
        start(null);
    }

    @SuppressLint("MissingPermission")
    public void start(Callback callback) {
        if (isScanning) {
            stop();
        }

        scanner = adapter.getBluetoothLeScanner();

        if (scanner == null || !isBluetoothAvailable()) {
            int errorCode = ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED;

            Exception exception = new Exception(scanFailed(errorCode));
            callback.onFailure(exception);

            return;
        }

        List<ScanFilter> filters = new ArrayList<>();
        // https://developer.android.com/reference/android/bluetooth/le/ScanFilter.Builder
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(serviceUUID), new ParcelUuid(serviceMask)).build();
        filters.add(filter);

        // https://developer.android.com/reference/android/bluetooth/le/ScanSettings.Builder
        ScanSettings settings = new ScanSettings.Builder()
            // Set scan mode for Bluetooth LE scan.
            .setScanMode(scanMode)
            .setReportDelay(reportDelay)
            .build();

        if (scanCallback == null) {
            // Bluetooth LE scan callbacks. Scan results are reported using these callbacks.
            // https://developer.android.com/reference/android/bluetooth/le/ScanCallback
            scanCallback = new ScanCallback() {
                @Override
                // Callback when a BLE advertisement has been found.
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);

                    parseScanResult(result, callback);
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    super.onBatchScanResults(results);

                    for (ScanResult result : results) {
                        parseScanResult(result, callback);
                    }
                }

                @Override
                // Callback when scan could not be started.
                public void onScanFailed(int errorCode) {
                    // Log.e("ScanCallback", String.format("onScanFailed(errorCode=%d)", errorCode));

                    super.onScanFailed(errorCode);

                    stop();

                    Exception exception = new Exception(scanFailed(errorCode));
                    callback.onFailure(exception);
                }
            };
        }

        // Start Bluetooth LE scan.
        // https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner
        scanner.startScan(filters, settings, scanCallback);

        isScanning = true;

        Executors.newSingleThreadScheduledExecutor()
            .schedule(
                () -> {
                    if (isScanning) {
                        callback.onSuccess();
                    }
                },
                100,
                TimeUnit.MILLISECONDS
            );
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void parseScanResult(ScanResult result, Callback callback) {
        // Represents a scan record from Bluetooth LE scan.
        ScanRecord record = result.getScanRecord();
        if (record == null) return;

        int rssi = result.getRssi();

        BluetoothDevice device = result.getDevice();
        if (device == null) return;

        List<ParcelUuid> serviceUuids = record.getServiceUuids();
        if (serviceUuids != null) {
            @Nullable
            UUID id = null;
            @Nullable
            String name = device.getName();
            @Nullable
            byte[] info = null;
            @Nullable
            Short channel = null;

            for (ParcelUuid serviceUuid : serviceUuids) {
                UUID uuid = serviceUuid.getUuid();

                if (uuid.compareTo(serviceUUID) == 0) {
                    continue;
                }

                if (id == null && uuid.getLeastSignificantBits() == BLUETOOTH_BASE_UUID_LSB) {
                    id = uuid;
                    continue;
                }

                ByteBuffer buffer = makeBuffer(uuid);
                buffer.rewind();

                byte size = buffer.get();

                info = new byte[size & 0x7f];
                buffer.get(info);

                if ((size & 0x80) != 0) {
                    channel = (short) (buffer.get() & 0xff);
                }

                break;
            }

            callback.onFound(id, name, info, channel, rssi, device);
        }
    }

    @SuppressLint("MissingPermission")
    public void stop() {
        if (scanner != null && scanCallback != null) {
            if (isBluetoothAvailable()) {
                scanner.stopScan(scanCallback);
            }

            scanCallback = null;
        }

        isScanning = false;
    }

    public boolean isScanning() {
        return isScanning;
    }

    public boolean isBluetoothAvailable() {
        return adapter.isEnabled() && adapter.getState() == BluetoothAdapter.STATE_ON;
    }

    private String scanFailed(int errorCode) {
        return switch (errorCode) {
            case ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "Failed to start scan as BLE scan with the same settings is already started by the app.";
            case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Failed to start scan as app cannot be registered.";
            case ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "Failed to start scan due an internal error.";
            case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "Failed to start power optimized scan as this feature is not supported.";
            case ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> "Failed to start scan as it is out of hardware resources.";
            case ScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY -> "Failed to start scan as application tries to scan too frequently.";
            default -> "Unknown error.";
        };
    }

    /**
     * Callback
     */

    public abstract static class Callback {

        public void onFound(
            @Nullable UUID id,
            @Nullable String name,
            @Nullable byte[] info,
            @Nullable Short channel,
            Integer rssi,
            BluetoothDevice device
        ) {}

        public void onSuccess() {}

        public void onFailure(Exception exception) {}
    }
}
