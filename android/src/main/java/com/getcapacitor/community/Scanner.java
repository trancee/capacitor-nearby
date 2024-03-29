package com.getcapacitor.community;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Scanner {

    private static Scanner instance = null;

    private final BluetoothAdapter adapter;
    private final BeaconCallback beaconCallback;

    private final UUID serviceUUID;

    private Integer scanMode = ScanSettings.SCAN_MODE_BALANCED;

    private BluetoothLeScanner scanner;
    private ScanCallback scanCallback;

    private boolean mScanning;

    private final Handler handler = new Handler();
    private Runnable runnable;

    private static final long ttlSeconds = 10;

    public static synchronized Scanner getInstance(BluetoothAdapter adapter, UUID serviceUUID, BeaconCallback beaconCallback) {
        if (instance == null) {
            instance = new Scanner(adapter, serviceUUID, beaconCallback);
        }

        return instance;
    }

    Scanner(BluetoothAdapter adapter, UUID serviceUUID, BeaconCallback beaconCallback) {
        this.adapter = adapter;

        this.serviceUUID = serviceUUID;

        this.beaconCallback = beaconCallback;
    }

    public Integer getScanMode() {
        return scanMode;
    }

    public void setScanMode(Integer scanMode) {
        this.scanMode = scanMode;
    }

    public void start() {
        start(null, null);
    }

    public void start(Callback callback) {
        start(null, callback);
    }

    public void start(Integer ttlSeconds, Callback callback) {
        stopTimer();

        if (mScanning) {
            stop();
        }

        scanner = adapter.getBluetoothLeScanner();

        if (scanner == null || !isBluetoothAvailable()) {
            int errorCode = android.bluetooth.le.ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED;

            if (callback != null) {
                callback.onFailure(errorCode, scanFailed(errorCode));
            }

            return;
        }

        List<ScanFilter> filters = new ArrayList<>();
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(serviceUUID)).build();
        filters.add(filter);

        // https://developer.android.com/reference/android/bluetooth/le/ScanSettings.Builder
        ScanSettings settings = new ScanSettings.Builder()
            // Set scan mode for Bluetooth LE scan.
            .setScanMode(scanMode)
            .build();

        if (scanCallback == null) {
            // Bluetooth LE scan callbacks. Scan results are reported using these callbacks.
            // https://developer.android.com/reference/android/bluetooth/le/ScanCallback
            scanCallback =
                new ScanCallback() {
                    @Override
                    // Callback when a BLE advertisement has been found.
                    public void onScanResult(int callbackType, ScanResult result) {
                        super.onScanResult(callbackType, result);

                        // Represents a scan record from Bluetooth LE scan.
                        ScanRecord record = result.getScanRecord();

                        Map<ParcelUuid, byte[]> map = record.getServiceData();
                        for (ParcelUuid key : map.keySet()) {
                            UUID uuid = key.getUuid();
                            byte[] data = map.get(key);

                            if (uuid.compareTo(serviceUUID) == 0) {
                                continue;
                            }

                            synchronized (beacons) {
                                Beacon beacon = beacons.get(uuid);
                                if (beacon != null) {
                                    beacon.alive();
                                } else {
                                    beacons.put(uuid, new Beacon(uuid));

                                    if (beaconCallback != null) {
                                        beaconCallback.onFound(uuid, data);
                                    }
                                }
                            }
                        }

                        List<ParcelUuid> serviceUuids = record.getServiceUuids();
                        for (ParcelUuid serviceUuid : serviceUuids) {
                            UUID uuid = serviceUuid.getUuid();

                            if (uuid.compareTo(serviceUUID) == 0) {
                                continue;
                            }

                            synchronized (beacons) {
                                Beacon beacon = beacons.get(uuid);
                                if (beacon != null) {
                                    beacon.alive();
                                } else {
                                    beacons.put(uuid, new Beacon(uuid));

                                    if (beaconCallback != null) {
                                        beaconCallback.onFound(uuid, null);
                                    }
                                }
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
                        Log.e("ScanCallback", String.format("onScanFailed(errorCode=%d)", errorCode));

                        super.onScanFailed(errorCode);

                        stop();

                        if (callback != null) {
                            callback.onFailure(errorCode, scanFailed(errorCode));
                        }
                    }
                };
        }

        // Start Bluetooth LE scan.
        // https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner
        scanner.startScan(filters, settings, scanCallback);

        if (ttlSeconds != null) {
            startTimer(ttlSeconds, callback);
        }

        mScanning = true;
    }

    public void stop() {
        stopTimer();

        if (scanner != null && scanCallback != null) {
            if (isBluetoothAvailable()) {
                scanner.stopScan(scanCallback);
            }

            scanCallback = null;
        }

        mScanning = false;
    }

    public boolean isScanning() {
        return mScanning;
    }

    public boolean isBluetoothAvailable() {
        return (adapter != null && adapter.isEnabled() && adapter.getState() == BluetoothAdapter.STATE_ON);
    }

    private String scanFailed(int errorCode) {
        switch (errorCode) {
            case android.bluetooth.le.ScanCallback.SCAN_FAILED_ALREADY_STARTED:
                return "Failed to start scan as BLE scan with the same settings is already started by the app.";
            case android.bluetooth.le.ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                return "Failed to start scan as app cannot be registered.";
            case android.bluetooth.le.ScanCallback.SCAN_FAILED_INTERNAL_ERROR:
                return "Failed to start scan due an internal error.";
            case android.bluetooth.le.ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED:
                return "Failed to start power optimized scan as this feature is not supported.";
            default:
                return "Unknown error.";
        }
    }

    /**
     * Timer
     */

    public void startTimer(Integer ttlSeconds, Scanner.Callback callback) {
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

    public abstract static class BeaconCallback {

        public void onFound(UUID uuid, byte[] data) {}

        public void onLost(UUID uuid, byte[] data) {}
    }

    public abstract static class Callback {

        public void onFailure(int errorCode, String errorMessage) {}

        public void onExpired() {}
    }

    /**
     * Beacon
     */

    private final Map<UUID, Beacon> beacons = new HashMap<>();

    public Set<UUID> getBeacons() {
        return beacons.keySet();
    }

    private class Beacon {

        UUID uuid;
        byte[] data;

        long timestamp;

        Runnable runnable;

        long lastSeen;

        public Beacon(UUID uuid, byte[] data) {
            this.uuid = uuid;
            this.data = data;

            this.timestamp = System.currentTimeMillis();

            this.runnable =
                () -> {
                    // Check if we are still alive.
                    synchronized (beacons) {
                        Beacon beacon = beacons.get(this.uuid);
                        if (beacon != null) {
                            if (mScanning) {
                                if (beaconCallback != null) {
                                    beaconCallback.onLost(this.uuid, this.data);
                                }
                            }
                        }
                    }

                    this.kill();
                };

            synchronized (beacons) {
                this.lastSeen = System.currentTimeMillis();

                handler.postDelayed(this.runnable, ttlSeconds * 1000);

                beacons.put(this.uuid, this);
            }
        }

        public Beacon(UUID uuid) {
            this(uuid, null);
        }

        public UUID uuid() {
            return this.uuid;
        }

        public byte[] data() {
            return this.data;
        }

        public void kill() {
            handler.removeCallbacks(this.runnable);

            // Kill yourself.
            synchronized (beacons) {
                beacons.remove(this.uuid);
            }
        }

        public void alive() {
            this.lastSeen = System.currentTimeMillis();

            handler.removeCallbacks(this.runnable);

            // Check if we are still alive.
            synchronized (beacons) {
                if (beacons.containsKey(this.uuid)) {
                    handler.postDelayed(this.runnable, ttlSeconds * 1000);
                }
            }
        }
    }
}
