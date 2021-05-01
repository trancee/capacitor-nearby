package com.getcapacitor.community;

import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;

import android.os.Handler;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.util.Log;

import java.nio.ByteBuffer;
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
//    private LeScanCallback leScanCallback;

    private boolean mScanning;

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

    public void start(Callback callback) {
        // Log.i("Scanner",
        //         String.format(
        //                 "start()"));

        if (mScanning) {
            stop();
        }

        scanner =
                adapter.getBluetoothLeScanner();

        if (scanner == null) {
            int errorCode = android.bluetooth.le.ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED;

            callback.onFailure(errorCode, scanFailed(errorCode));
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

                            UUID uuid = null;
                            byte[] data = null;

                            Map<ParcelUuid, byte[]> map = record.getServiceData();
                            for (ParcelUuid key : map.keySet()) {
                                uuid = key.getUuid();
                                data = map.get(key);
                            }

                            if (uuid != null) {
                                // Log.i("ScanCallback",
                                //         String.format(
                                //                 "UUID=%s",
                                //                 uuid.toString()));

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

                            callback.onFailure(errorCode, scanFailed(errorCode));
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

//        if (leScanCallback == null) {
//            leScanCallback =
//                    new LeScanCallback() {
//                        @Override
//                        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
//                            Log.i("startLeScan",
//                                    String.format(
//                                            "onLeScan(device=%s, device=%d, scanRecord=%s)",
//                                            device, rssi, scanRecord));
//                        }
//                    };
//        }
//
//        // Starts a scan for Bluetooth LE devices, looking for devices that advertise given services.
//        // https://developer.android.com/reference/android/bluetooth/BluetoothAdapter
//        boolean result = adapter.startLeScan(
//                new UUID[]{
//                        Constants.SERVICE_UUID,
//                },
//
//                leScanCallback
//        );

        mScanning = true;
    }

    public void stop() {
        if (scanner != null && scanCallback != null) {
            scanner.stopScan(scanCallback);

            scanCallback = null;
        }

//        if (adapter != null && leScanCallback != null) {
//            adapter.stopLeScan(leScanCallback);
//
//            leScanCallback = null;
//        }

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
    }

    /**
     * Beacon
     */

    private Handler handler = new Handler();

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
