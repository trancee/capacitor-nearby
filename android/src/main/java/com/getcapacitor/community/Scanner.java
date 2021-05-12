package com.getcapacitor.community;

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
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Scanner {
    private static Scanner instance = null;

    private BluetoothAdapter adapter;
    private BeaconCallback beaconCallback;

    Integer scanMode = ScanSettings.SCAN_MODE_BALANCED;

    private BluetoothLeScanner scanner;
    private ScanCallback scanCallback;

    private boolean mScanning;

    private Handler handler = new Handler();
    private Runnable runnable;

    private static long ttlSeconds = 10;

    public static synchronized Scanner getInstance(BluetoothAdapter adapter, BeaconCallback beaconCallback) {
        if (instance == null) {
            instance = new Scanner(adapter, beaconCallback);
        }

        return instance;
    }

    Scanner(BluetoothAdapter adapter, BeaconCallback beaconCallback) {
        this.adapter = adapter;

        this.beaconCallback = beaconCallback;
    }

    public Integer getScanMode() {
        return scanMode;
    }

    public void setScanMode(Integer scanMode) {
        this.scanMode = scanMode;
    }

//    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
//
//    private static String bytesToHex(byte[] bytes) {
//        char[] hexChars = new char[bytes.length * 2];
//        for (int j = 0; j < bytes.length; j++) {
//            int v = bytes[j] & 0xFF;
//            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
//            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
//        }
//        return new String(hexChars);
//    }

    public void start() {
        start(null, null);
    }

    public void start(Callback callback) {
        start(null, callback);
    }

    public void start(Integer ttlSeconds, Callback callback) {
        // Log.i("Scanner",
        //         String.format(
        //                 "start()"));

        stopTimer();

        if (mScanning) {
            stop();
        }

        scanner =
                adapter.getBluetoothLeScanner();

        if (scanner == null) {
            int errorCode = android.bluetooth.le.ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED;

            if (callback != null) {
                callback.onFailure(errorCode, scanFailed(errorCode));
            }

            return;
        }

        List<ScanFilter> filters = new ArrayList<>();
        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(Constants.SERVICE_UUID))
                .build();
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
                            // Log.i("ScanCallback",
                            //         String.format(
                            //                 "onScanResult(callbackType=%d, result=%s)",
                            //                 callbackType, result));

                            super.onScanResult(callbackType, result);

                            BluetoothDevice device = result.getDevice();

                            // Represents a scan record from Bluetooth LE scan.
                            ScanRecord record = result.getScanRecord();

                            // Log.i("ScanCallback",
                            //         String.format(
                            //                 "onScanResult(device=%s, record=%s)",
                            //                 device, record));

                            // Log.i("ScanCallback",
                            //         String.format(
                            //                 "ScanRecord(rawBytes=%s)",
                            //                 bytesToHex(record.getBytes())));

                            Map<ParcelUuid, byte[]> map = record.getServiceData();
                            for (ParcelUuid key : map.keySet()) {
                                UUID uuid = key.getUuid();
                                byte[] data = map.get(key);

                                // Log.i("ScanCallback",
                                //         String.format(
                                //                 "UUID=%s",
                                //                 uuid.toString()));

                                if (uuid.compareTo(Constants.SERVICE_UUID) == 0) {
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

                                // Log.i("ScanCallback",
                                //         String.format(
                                //                 "UUID=%s",
                                //                 uuid.toString()));

                                if (uuid.compareTo(Constants.SERVICE_UUID) == 0) {
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
                            // Log.i("ScanCallback",
                            //         String.format(
                            //                 "onBatchScanResults(results=%s)",
                            //                 results));

                            super.onBatchScanResults(results);
                        }

                        @Override
                        // Callback when scan could not be started.
                        public void onScanFailed(int errorCode) {
                            Log.e("ScanCallback",
                                    String.format(
                                            "onScanFailed(errorCode=%d)",
                                            errorCode));

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
        scanner.startScan(
                filters,
                settings,

                scanCallback
        );

        if (ttlSeconds != null) {
            startTimer(ttlSeconds, callback);
        }

        mScanning = true;
    }

    public void stop() {
        stopTimer();

        if (scanner != null && scanCallback != null) {
            scanner.stopScan(scanCallback);

            scanCallback = null;
        }

        mScanning = false;
    }

    public boolean isScanning() {
        return mScanning;
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
            runnable = () -> callback.onExpired();

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
        public void onFound(UUID uuid, byte[] data) {
        }

        public void onLost(UUID uuid, byte[] data) {
        }
    }

    public abstract static class Callback {
        public void onFailure(int errorCode, String errorMessage) {
        }

        public void onExpired() {
        }
    }

    /**
     * Beacon
     */

    private final Map<UUID, Beacon> beacons = new HashMap<>();

    private class Beacon {
        UUID uuid;
        byte[] data;

        long timestamp;

        Runnable runnable;

        BluetoothDevice device;

        long lastSeen;

        public Beacon(UUID uuid, byte[] data) {
            this.uuid = uuid;
            this.data = data;

            this.timestamp = System.currentTimeMillis();

            this.runnable = () -> {
                // Log.i("Scanner::Beacon",
                //         String.format(
                //                 "Runnable(uuid=%s)",
                //                 this.uuid));

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
            // Log.i("Scanner::Beacon::kill",
            //         String.format(
            //                 "Runnable(uuid=%s)",
            //                 this.uuid));

            handler.removeCallbacks(this.runnable);

            // Kill yourself.
            synchronized (beacons) {
                beacons.remove(this.uuid);
            }
        }

        public void alive() {
            this.lastSeen = System.currentTimeMillis();

            // Log.i("Scanner::Beacon::alive",
            //         String.format(
            //                 "Runnable(uuid=%s)",
            //                 this.uuid));

            handler.removeCallbacks(this.runnable);

            // Check if we are still alive.
            synchronized (beacons) {
                if (beacons.containsKey(this.uuid)) {
                    // Log.i("Scanner::Beacon::post",
                    //         String.format(
                    //                 "Runnable(uuid=%s, ttlSeconds=%d)",
                    //                 this.uuid, ttlSeconds));

                    handler.postDelayed(this.runnable, ttlSeconds * 1000);
                }
            }
        }
    }
}
