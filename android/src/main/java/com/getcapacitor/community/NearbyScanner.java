package com.getcapacitor.community;

import static com.getcapacitor.community.NearbyHelper.EndpointID;
import static com.getcapacitor.community.NearbyHelper.makeBytes;
import static com.getcapacitor.community.NearbyHelper.makeString;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;
import android.os.ParcelUuid;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NearbyScanner {

    private static NearbyScanner instance = null;

    private final BluetoothAdapter adapter;

    private final UUID serviceUUID;
    private final UUID serviceMask;

    private Integer scanMode = ScanSettings.SCAN_MODE_BALANCED;

    private BluetoothLeScanner scanner;
    private ScanCallback scanCallback;

    private boolean isScanning;

    public static synchronized NearbyScanner getInstance(BluetoothAdapter adapter, UUID serviceUUID, UUID serviceMask) {
        if (instance == null) {
            instance = new NearbyScanner(adapter, serviceUUID, serviceMask);
        }

        return instance;
    }

    NearbyScanner(BluetoothAdapter adapter, UUID serviceUUID, UUID serviceMask) {
        this.adapter = adapter;

        this.serviceUUID = serviceUUID;
        this.serviceMask = serviceMask;
    }

    public Integer getScanMode() {
        return scanMode;
    }

    public void setScanMode(Integer scanMode) {
        this.scanMode = scanMode;
    }

    public void start() {
        start(null);
    }

    @SuppressLint("MissingPermission")
    public void start(@Nullable Callback callback) {
        if (isScanning) {
            stop();
        }

        scanner = adapter.getBluetoothLeScanner();

        if (scanner == null || !isBluetoothAvailable()) {
            int errorCode = ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED;

            if (callback != null) {
                Exception exception = new Exception(scanFailed(errorCode));

                callback.onFailure(exception);
            }

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
            .build();

        if (scanCallback == null) {
            // Bluetooth LE scan callbacks. Scan results are reported using these callbacks.
            // https://developer.android.com/reference/android/bluetooth/le/ScanCallback
            scanCallback = new ScanCallback() {
                @Override
                // Callback when a BLE advertisement has been found.
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);

                    // Represents a scan record from Bluetooth LE scan.
                    ScanRecord record = result.getScanRecord();
                    if (record == null) return;

                    int rssi = result.getRssi();

                    BluetoothDevice device = result.getDevice();
                    if (device == null) return;

                    /*
                            Map<ParcelUuid, byte[]> map = record.getServiceData();
                            if (map != null) {
                                for (ParcelUuid key : map.keySet()) {
                                    UUID uuid = key.getUuid();

                                    if (uuid.compareTo(serviceUUID) == 0) {
                                        continue;
                                    }

                                    synchronized (beacons) {
                                        Beacon beacon = beacons.get(uuid);
                                        if (beacon != null) {
                                            beacon.alive();
                                        } else {
                                            beacons.put(uuid, new Beacon(uuid, rssi));

                                            if (beaconCallback != null) {
                                                beaconCallback.onFound(uuid, rssi);
                                            }
                                        }
                                    }
                                }
                            }
                            */

                    List<ParcelUuid> serviceUuids = record.getServiceUuids();
                    if (serviceUuids != null) {
                        EndpointID endpointID = null;
                        byte[] endpointInfo = null;

                        for (ParcelUuid serviceUuid : serviceUuids) {
                            UUID uuid = serviceUuid.getUuid();

                            if (uuid.compareTo(serviceUUID) == 0) {
                                continue;
                            }

                            if (endpointID == null) {
                                endpointID = new EndpointID(makeString(uuid));
                                continue;
                            }

                            endpointInfo = makeBytes(uuid);
                            break;
                        }

                        if (callback != null) {
                            callback.onFound(endpointID, endpointInfo, rssi, device);
                        }
                    }
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    super.onBatchScanResults(results);
                }

                @Override
                // Callback when scan could not be started.
                public void onScanFailed(int errorCode) {
                    // Log.e("ScanCallback", String.format("onScanFailed(errorCode=%d)", errorCode));

                    super.onScanFailed(errorCode);

                    stop();

                    if (callback != null) {
                        Exception exception = new Exception(scanFailed(errorCode));

                        callback.onFailure(exception);
                    }
                }
            };
        }

        // Start Bluetooth LE scan.
        // https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner
        scanner.startScan(filters, settings, scanCallback);

        isScanning = true;

        Handler handler = new Handler();
        handler.postDelayed(
            () -> {
                if (isScanning) {
                    if (callback != null) {
                        callback.onSuccess();
                    }
                }
            },
            100
        );
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
        return (adapter != null && adapter.isEnabled() && adapter.getState() == BluetoothAdapter.STATE_ON);
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

        public void onFound(EndpointID endpointID, @Nullable byte[] endpointInfo, Integer rssi, BluetoothDevice device) {}

        public void onLost(EndpointID endpointID) {}

        public void onSuccess() {}

        public void onFailure(Exception exception) {}
    }
}
