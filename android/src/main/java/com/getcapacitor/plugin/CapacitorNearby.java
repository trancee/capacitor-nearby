package com.getcapacitor.plugin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Handler;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.util.Log;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

interface Constants {
    // v5 (Name-based | SHA1 hash) UUID (winkee.app)
    String SERVICE_UUID = "1c2cceae-66cd-55cd-8769-d961a7412368";
    // v5 (Name-based | SHA1 hash) UUID (profile.winkee.app)
    String CHARACTERISTIC_UUID = "35274eec-ae41-5975-a27a-608b334ce36e";
    String DESCRIPTOR_UUID = "35274eec-ae41-5975-a27a-608b334ce36e";

    String BLE_NOT_SUPPORTED = "Bluetooth Low Energy not supported";

    int GATT_MTU_SIZE_DEFAULT = 23;
    int GATT_MAX_MTU_SIZE = 517;
}

@NativePlugin(
        requestCodes = {
                CapacitorNearby.REQUEST_PERMISSIONS,

                CapacitorNearby.REQUEST_LOCATION_SERVICE,
                CapacitorNearby.REQUEST_BLUETOOTH_SERVICE,
        },

        permissions = {
                // Allows an app to access precise location.
                Manifest.permission.ACCESS_FINE_LOCATION,

                // Allows applications to connect to paired bluetooth devices.
                Manifest.permission.BLUETOOTH,
                // Allows applications to discover and pair bluetooth devices.
                Manifest.permission.BLUETOOTH_ADMIN,
        }
)
public class CapacitorNearby extends Plugin {
    protected static final int REQUEST_PERMISSIONS = 10000;

    protected static final int REQUEST_LOCATION_SERVICE = 10001;
    protected static final int REQUEST_BLUETOOTH_SERVICE = 10002;

    private BluetoothManager manager;
    private BluetoothAdapter adapter;
    private BluetoothLeScanner scanner;
    private BluetoothLeAdvertiser advertiser;

    private boolean mScanning;
    private boolean mAdvertising;

    private ScanCallback scanCallback;
    private AdvertisingSetCallback advertisingCallback;

    private AdvertisingSet mAdvertisingSet;

    private BluetoothGattServer gattServer;

    private Handler handler = new Handler();

    @SuppressLint("NewApi")
    @PluginMethod
    public void initialize(PluginCall call) {
        try {
            // Use this check to determine whether BLE is supported on the device. Then
            // you can selectively disable BLE-related features.
            if (!getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Log.i(getLogTag(),
                        String.format(
                                Constants.BLE_NOT_SUPPORTED));

                call.error(Constants.BLE_NOT_SUPPORTED);
                return;
            }

            if (hasRequiredPermissions()) {
                final LocationManager locationManager =
                        (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);

                if (!locationManager.isLocationEnabled()) {
//                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//                    startActivityForResult(call, intent, REQUEST_LOCATION_SERVICE);
                    showEnableLocationSetting();
                    return;
                }

                // Initializes Bluetooth adapter.
                manager =
                        (BluetoothManager) getContext().getSystemService(Context.BLUETOOTH_SERVICE);

                adapter =
                        manager.getAdapter();

                // Ensures Bluetooth is available on the device and it is enabled. If not,
                // displays a dialog requesting user permission to enable Bluetooth.
                if (adapter == null || !adapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(call, enableBtIntent, REQUEST_BLUETOOTH_SERVICE);
                    return;
                }

                advertiser =
                        adapter.getBluetoothLeAdvertiser();

                scanner =
                        adapter.getBluetoothLeScanner();

                Log.i(getLogTag(), "LeMaximumAdvertisingDataLength: " + adapter.getLeMaximumAdvertisingDataLength());

                call.success();
            } else {
                saveCall(call);

                pluginRequestPermission(
                        // Allows an app to access precise location.
                        Manifest.permission.ACCESS_FINE_LOCATION,

                        REQUEST_PERMISSIONS
                );
            }
        } catch (Exception e) {
            call.error(e.getLocalizedMessage(), e);
        }
    }

    @Override
    protected void handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.handleRequestPermissionsResult(requestCode, permissions, grantResults);

        PluginCall call = getSavedCall();
        if (call != null) {
            freeSavedCall();

            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_DENIED) {
                    call.unavailable();
                    return;
                }
            }

            initialize(call);
        }
    }

    @Override
    protected void handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        super.handleOnActivityResult(requestCode, resultCode, data);

        PluginCall call = getSavedCall();
        if (call != null) {
            freeSavedCall();

            if (resultCode == Activity.RESULT_OK) {
                initialize(call);
            } else {
                call.unavailable();
            }
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected void handleOnDestroy() {
        super.handleOnDestroy();

        if (gattServer != null) {
            gattServer.close();

            gattServer = null;
        }

        if (advertiser != null) {
            advertiser.stopAdvertisingSet(advertisingCallback);
            mAdvertising = false;

            advertiser = null;
        }

        if (scanner != null) {
            // Stops an ongoing Bluetooth LE scan.
            scanner.stopScan(scanCallback);
            mScanning = false;

            scanner = null;
        }
    }

    private BluetoothGattServer startGattServer() {
        BluetoothGattServerCallback gattCallback = new BluetoothGattServerCallback() {
            // Callback indicating when a remote device has been connected or disconnected.
            @Override
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                Log.i(getLogTag(),
                        String.format(
                                "onConnectionStateChange(device=%s, status=%s, newState=%s)",
                                device, status, newState));

                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED:
                        if (devices.containsKey(device.getAddress())) {
                            gattServer.cancelConnection(device);
                        } else {
                            // Allow connection to proceed. Mark device connected
                            devices.put(device.getAddress(), device);
                        }

                        break;
                    case BluetoothProfile.STATE_DISCONNECTED:
                        // We've disconnected
                        devices.remove(device.getAddress());

                        break;
                }

                JSObject bluetoothDeviceObject = new JSObject();
                bluetoothDeviceObject.put("address", device.getAddress());
                bluetoothDeviceObject.put("name", device.getName());
                bluetoothDeviceObject.put("type", device.getType());

                if (device.getUuids() != null) {
                    bluetoothDeviceObject.put("uuids", JSArray.from(device.getUuids()));
                }

                JSObject data = new JSObject();
                data.put("device", bluetoothDeviceObject);
                data.put("status", status);
                data.put("newState", newState);

                notifyListeners("onConnectionStateChange", data);

                super.onConnectionStateChange(device, status, newState);
            }

            // Indicates whether a local service has been added successfully.
            @Override
            public void onServiceAdded(int status, BluetoothGattService service) {
                Log.i(getLogTag(),
                        String.format(
                                "onServiceAdded(status=%s, service=%s)",
                                status, service));

                JSObject bluetoothGattServiceObject = new JSObject();
                bluetoothGattServiceObject.put("uuid", service.getUuid().toString());
                bluetoothGattServiceObject.put("type", service.getType());

                JSObject data = new JSObject();
                data.put("status", status);
                data.put("service", bluetoothGattServiceObject);

                notifyListeners("onServiceAdded", data);

                super.onServiceAdded(status, service);
            }

            // A remote client has requested to read a local characteristic.
            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                Log.i(getLogTag(),
                        String.format(
                                "onCharacteristicReadRequest(device=%s, requestId=%s, offset=%s, characteristic=%s)",
                                device, requestId, offset, characteristic));

                super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            }

            // A remote client has requested to write to a local characteristic.
            @Override
            public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                Log.i(getLogTag(),
                        String.format(
                                "onCharacteristicWriteRequest(device=%s, requestId=%s, offset=%s, characteristic=%s, preparedWrite=%s, responseNeeded=%s, offset=%s, value=%s)",
                                device, requestId, offset, characteristic, preparedWrite, responseNeeded, offset, value));

                BluetoothGattCharacteristic localCharacteristic = gattServer.getService(UUID.fromString(Constants.SERVICE_UUID)).getCharacteristic(characteristic.getUuid());
                if (localCharacteristic != null) {
                    // Must send response before notifying callback (which might trigger data send before remote central received ack)
                    if (responseNeeded) {
                        boolean success = gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                    }
                } else {
                    // Request for unrecognized characteristic. Send GATT_FAILURE
                    boolean success = gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
                }

                super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            }

            // A remote client has requested to read a local descriptor.
            @Override
            public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
                Log.i(getLogTag(),
                        String.format(
                                "onDescriptorReadRequest(device=%s, requestId=%s, offset=%s, descriptor=%s)",
                                device, requestId, offset, descriptor));

                super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            }

            // A remote client has requested to write to a local descriptor.
            @Override
            public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                Log.i(getLogTag(),
                        String.format(
                                "onDescriptorWriteRequest(device=%s, requestId=%s, descriptor=%s, preparedWrite=%s, responseNeeded=%s, offset=%s, value=%s)",
                                device, requestId, descriptor, preparedWrite, responseNeeded, offset, value));

                if (Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE) && responseNeeded) {
                    boolean success = gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                }

                super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            }

            // Execute all pending write operations for this device.
            @Override
            public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
                Log.i(getLogTag(),
                        String.format(
                                "onExecuteWrite(device=%s, requestId=%s, execute=%s)",
                                device, requestId, execute));

                super.onExecuteWrite(device, requestId, execute);
            }

            // Callback invoked when a notification or indication has been sent to a remote device.
            @Override
            public void onNotificationSent(BluetoothDevice device, int status) {
                Log.i(getLogTag(),
                        String.format(
                                "onNotificationSent(device=%s, status=%s)",
                                device, status));

                super.onNotificationSent(device, status);
            }
        };

        return manager.openGattServer(getContext(), gattCallback);
    }

    private boolean setupGattServer() {
        BluetoothGattService service =
                new BluetoothGattService(
                        UUID.fromString(Constants.SERVICE_UUID),

                        BluetoothGattService.SERVICE_TYPE_PRIMARY
                );

        BluetoothGattCharacteristic dataCharacteristic =
                new BluetoothGattCharacteristic(
                        UUID.fromString(Constants.CHARACTERISTIC_UUID),

                        BluetoothGattCharacteristic.PROPERTY_READ |
                                BluetoothGattCharacteristic.PROPERTY_WRITE |
                                BluetoothGattCharacteristic.PROPERTY_INDICATE,

                        BluetoothGattCharacteristic.PERMISSION_READ |
                                BluetoothGattCharacteristic.PERMISSION_WRITE
                );

        dataCharacteristic.addDescriptor(
                new BluetoothGattDescriptor(
                        UUID.fromString(Constants.DESCRIPTOR_UUID),

                        BluetoothGattDescriptor.PERMISSION_WRITE |
                                BluetoothGattDescriptor.PERMISSION_READ));

        service.addCharacteristic(dataCharacteristic);

        return gattServer.addService(service);
    }

    @SuppressLint("NewApi")
    @PluginMethod
    public void advertise(PluginCall call) {
        if (advertiser == null) {
            call.unavailable();
            return;
        }

        if (!mAdvertising) {
            advertisingCallback =
                    new AdvertisingSetCallback() {
                        @Override
                        public void onAdvertisingSetStarted(AdvertisingSet advertisingSet, int txPower, int status) {
                            Log.i(getLogTag(),
                                    String.format(
                                            "onAdvertisingSetStarted(txPower=%s, status=%s)",
                                            txPower, status));

                            mAdvertisingSet = advertisingSet;

                            JSObject data = new JSObject();
                            data.put("txPower", txPower);
                            data.put("status", status);

                            notifyListeners("onAdvertisingSetStarted", data);
                        }

                        @Override
                        public void onAdvertisingDataSet(AdvertisingSet advertisingSet, int status) {
                            Log.i(getLogTag(),
                                    String.format(
                                            "onAdvertisingDataSet(status=%s)",
                                            status));

                            JSObject data = new JSObject();
                            data.put("status", status);

                            notifyListeners("onAdvertisingDataSet", data);
                        }

                        @Override
                        public void onScanResponseDataSet(AdvertisingSet advertisingSet, int status) {
                            Log.i(getLogTag(),
                                    String.format(
                                            "onScanResponseDataSet(status=%s)",
                                            status));

                            JSObject data = new JSObject();
                            data.put("status", status);

                            notifyListeners("onScanResponseDataSet", data);
                        }

                        @Override
                        public void onAdvertisingSetStopped(AdvertisingSet advertisingSet) {
                            Log.i(getLogTag(),
                                    String.format(
                                            "onAdvertisingSetStopped()"));

                            notifyListeners("onAdvertisingSetStopped", null);
                        }
                    };

            try {
                AdvertisingSetParameters parameters = (new AdvertisingSetParameters.Builder())
                        .setLegacyMode(true)
                        .setConnectable(true)
                        .setScannable(true)
                        .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
                        .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH)
                        .build();

                AdvertiseData data = (new AdvertiseData.Builder())
                        .setIncludeTxPowerLevel(false)
                        .setIncludeDeviceName(false)
//                        .addServiceData(ParcelUuid.fromString(Constants.SERVICE_UUID), "UEHV6nWB2yk8pyoJadR*.7kCMdnjS#M|%1%2".getBytes())
                        .addServiceUuid(ParcelUuid.fromString(Constants.SERVICE_UUID))
                        .build();

                AdvertiseData scanResponse = (new AdvertiseData.Builder())
                        .setIncludeDeviceName(false)
                        .build();

//            // Stops scanning after a pre-defined scan period.
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    mScanning = false;
//                    advertiser.stopAdvertisingSet(advertisingCallback);
//                }
//            }, SCAN_PERIOD);

                gattServer = startGattServer();
                setupGattServer();

                // java.lang.IllegalArgumentException: Legacy advertising data too big
                // java.lang.IllegalArgumentException: Advertising data too big
                advertiser.startAdvertisingSet(parameters, data, scanResponse, null, null, advertisingCallback);
                mAdvertising = true;
            } catch (Exception e) {
                Log.e(getLogTag(), "startAdvertisingSet", e);

                call.error(e.getLocalizedMessage(), e);
                return;
            }
        } else {
            try {
                synchronized (devices) {
                    for (BluetoothDevice device : devices.values()) {
                        gattServer.cancelConnection(device);
                    }

                    devices.clear();
                }

                gattServer.close();
                gattServer = null;

                advertiser.stopAdvertisingSet(advertisingCallback);
                mAdvertising = false;
            } catch (Exception e) {
                Log.e(getLogTag(), "stopAdvertisingSet", e);

                call.error(e.getLocalizedMessage(), e);
                return;
            }
        }

        JSObject data = new JSObject();
        data.put("isAdvertising", mAdvertising);

        call.success(data);
    }

    private final Map<String, BluetoothDevice> devices = new HashMap<>();

    private void connect(final BluetoothDevice device) {
        if (devices.containsKey(device.getAddress())) {
            return;
        }

        BluetoothGattCallback gattCallback =
                new BluetoothGattCallback() {
                    // Callback indicating when GATT client has connected/disconnected to/from a remote GATT server.
                    @Override
                    public void onConnectionStateChange(final BluetoothGatt gatt, int status,
                                                        int newState) {
                        Log.i(getLogTag(),
                                String.format(
                                        "onConnectionStateChange(gatt=%s, status=%s, newState=%s)",
                                        gatt, status, newState));

                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            synchronized (devices) {
                                switch (newState) {
                                    case BluetoothProfile.STATE_DISCONNECTING:
                                        break;

                                    case BluetoothProfile.STATE_DISCONNECTED:
                                        devices.remove(device.getAddress());

                                        gatt.close();
                                        break;

                                    case BluetoothProfile.STATE_CONNECTED:
//                                        // FIXME: Might need to use UI Thread to prevent rare threading issue causing a deadlock.
//                                        handler.post(new Runnable() {
//                                            @Override
//                                            public void run() {
//                                                // Discover services
//                                                gatt.discoverServices();
//                                            }
//                                        });

                                        // Discover services
                                        gatt.discoverServices();

//                                        connections.put(device.getAddress(), gatt);

                                        gatt.requestMtu(Constants.GATT_MAX_MTU_SIZE);

                                        break;
                                }
                            }
                        } else {
                            gatt.close();
                        }

                        JSObject data = new JSObject();
                        data.put("status", status);
                        data.put("newState", newState);

                        notifyListeners("onConnectionStateChange", data);

                        super.onConnectionStateChange(gatt, status, newState);
                    }

                    // Callback indicating the MTU for a given device connection has changed.
                    @Override
                    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                        Log.i(getLogTag(),
                                String.format(
                                        "onMtuChanged(gatt=%s, mtu=%s, status=%s)",
                                        gatt, mtu, status));

                        JSObject data = new JSObject();
                        data.put("mtu", mtu);
                        data.put("status", status);

                        notifyListeners("onMtuChanged", data);

                        super.onMtuChanged(gatt, mtu, status);
                    }

                    // Callback invoked when the list of remote services, characteristics and descriptors for the remote device have been updated, ie new services have been discovered.
                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        Log.i(getLogTag(),
                                String.format(
                                        "onServicesDiscovered(gatt=%s, status=%s)",
                                        gatt, status));

                        JSArray servicesArray = new JSArray();

                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            for (BluetoothGattService service : gatt.getServices()) {
                                JSObject serviceObject = new JSObject();

                                serviceObject.put("uuid", service.getUuid());
                                serviceObject.put("type", service.getType());

                                JSArray characteristicsArray = new JSArray();

                                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                                    JSObject characteristicObject = new JSObject();

                                    characteristicObject.put("uuid", characteristic.getUuid());
                                    characteristicObject.put("value", characteristic.getValue());

                                    characteristicsArray.put(characteristicObject);

                                    characteristic.setValue("Hello world.");
                                    gatt.writeCharacteristic(characteristic);
                                }

                                serviceObject.put("characteristics", characteristicsArray);

                                servicesArray.put(serviceObject);
                            }
                        }

                        JSObject data = new JSObject();
                        data.put("status", status);
                        data.put("services", servicesArray);

                        notifyListeners("onServicesDiscovered", data);

                        super.onServicesDiscovered(gatt, status);
                    }

                    // Callback indicating the result of a descriptor write operation.
                    @Override
                    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                                  int status) {
                        Log.i(getLogTag(),
                                String.format(
                                        "onDescriptorWrite(gatt=%s, descriptor=%s, status=%s)",
                                        gatt, descriptor, status));
                    }

                    // Callback triggered as a result of a remote characteristic notification.
                    @Override
                    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                        Log.i(getLogTag(),
                                String.format(
                                        "onCharacteristicChanged(gatt=%s, characteristic=%s)",
                                        gatt, characteristic));

                        super.onCharacteristicChanged(gatt, characteristic);
                    }

                    // Callback indicating the result of a characteristic write operation.
                    @Override
                    public void onCharacteristicWrite(BluetoothGatt gatt,
                                                      BluetoothGattCharacteristic characteristic, int status) {
                        Log.i(getLogTag(),
                                String.format(
                                        "onCharacteristicWrite(gatt=%s, characteristic=%s, status=%s)",
                                        gatt, characteristic, status));
                    }

                    // Callback reporting the result of a characteristic read operation.
                    @Override
                    public void onCharacteristicRead(BluetoothGatt gatt,
                                                     BluetoothGattCharacteristic characteristic,
                                                     int status) {
                        Log.i(getLogTag(),
                                String.format(
                                        "onCharacteristicRead(gatt=%s, characteristic=%s, status=%s)",
                                        gatt, characteristic, status));
                    }

                    // Callback reporting the RSSI for a remote device connection.
                    @Override
                    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                        Log.i(getLogTag(),
                                String.format(
                                        "onReadRemoteRssi(gatt=%s, rssi=%s, status=%s)",
                                        gatt, rssi, status));

                        super.onReadRemoteRssi(gatt, rssi, status);
                    }
                };

        // Connect to GATT Server hosted by this device.
        device.connectGatt(getContext(), false, gattCallback);
    }

    @SuppressLint("NewApi")
    @PluginMethod
    public void scan(final PluginCall call) {
        if (scanner == null) {
            call.unavailable();
            return;
        }

        if (!mScanning) {
            scanCallback =
                    new ScanCallback() {
                        // Callback when a BLE advertisement has been found.
                        @Override
                        public void onScanResult(int callbackType, ScanResult result) {
                            super.onScanResult(callbackType, result);

                            Log.i(getLogTag(),
                                    String.format(
                                            "onScanResult(callbackType=%s, result=%s)",
                                            callbackType, result));

                            BluetoothDevice device = result.getDevice();
                            connect(device);

                            devices.replace(device.getAddress(), device);
                            if (devices.containsKey(device.getAddress())) return;
                            devices.put(device.getAddress(), device);

                            try {
                                BluetoothClass bluetoothClass = device.getBluetoothClass();

                                JSObject bluetoothClassObject = new JSObject();
                                bluetoothClassObject.put("deviceClass", bluetoothClass.getDeviceClass());
                                bluetoothClassObject.put("majorDeviceClass", bluetoothClass.getMajorDeviceClass());

                                JSObject bluetoothDeviceObject = new JSObject();
                                bluetoothDeviceObject.put("address", device.getAddress());
                                bluetoothDeviceObject.put("name", device.getName());
                                bluetoothDeviceObject.put("type", device.getType());

                                if (device.getUuids() != null) {
                                    bluetoothDeviceObject.put("uuids", JSArray.from(device.getUuids()));
                                }

                                ScanRecord scanRecord = result.getScanRecord();

                                JSObject scanRecordObject = new JSObject();
                                scanRecordObject.put("advertiseFlags", scanRecord.getAdvertiseFlags());
                                scanRecordObject.put("bytes", scanRecord.getBytes());
                                scanRecordObject.put("deviceName", scanRecord.getDeviceName());
                                scanRecordObject.put("txPowerLevel", scanRecord.getTxPowerLevel());

                                JSObject scanResultObject = new JSObject();
                                scanResultObject.put("device", bluetoothDeviceObject);
                                scanResultObject.put("scanRecord", scanRecordObject);
                                scanResultObject.put("timestampNanos", result.getTimestampNanos());
                                scanResultObject.put("rssi", result.getRssi());
                                scanResultObject.put("txPower", result.getTxPower());// O
                                scanResultObject.put("primaryPhy", result.getPrimaryPhy());// O
                                scanResultObject.put("secondaryPhy", result.getSecondaryPhy());// O
                                scanResultObject.put("advertisingSid", result.getAdvertisingSid());// O
                                scanResultObject.put("periodicAdvertisingInterval", result.getPeriodicAdvertisingInterval());// O
                                scanResultObject.put("dataStatus", result.getDataStatus());// O
                                scanResultObject.put("isConnectable", result.isConnectable());// O
                                scanResultObject.put("isLegacy", result.isLegacy());// O

                                JSObject data = new JSObject();
                                data.put("callbackType", callbackType);
                                data.put("result", scanResultObject);

                                notifyListeners("onScanResult", data);
                            } catch (Exception e) {
                                Log.e(getLogTag(), "onScanResult", e);

                                call.error(e.getLocalizedMessage(), e);
                                return;
                            }
                        }

                        // Callback when scan could not be started.
                        @Override
                        public void onScanFailed(int errorCode) {
                            super.onScanFailed(errorCode);

                            Log.i(getLogTag(),
                                    String.format(
                                            "onScanFailed(errorCode=%s)",
                                            errorCode));

                            JSObject data = new JSObject();
                            data.put("errorCode", errorCode);

                            notifyListeners("onScanFailed", data);
                        }

                        // Callback when batch results are delivered.
                        @Override
                        public void onBatchScanResults(List<ScanResult> results) {
                            super.onBatchScanResults(results);

                            Log.i(getLogTag(),
                                    String.format(
                                            "onBatchScanResults(results=%s)",
                                            results));

                            for (ScanResult result : results) {
                                BluetoothDevice device = result.getDevice();

                                if (devices.containsKey(device.getAddress())) return;
                                devices.put(device.getAddress(), device);

                                connect(device);
                            }
                        }
                    };

//            // Stops scanning after a pre-defined scan period.
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    mScanning = false;
//                    scanner.stopScan(scanCallback);
//                }
//            }, SCAN_PERIOD);

            try {
                ArrayList<ScanFilter> filters = new ArrayList<>();

                {
                    ScanFilter.Builder builder = new ScanFilter.Builder();
                    // Set filter on service uuid.
                    builder.setServiceUuid(ParcelUuid.fromString(Constants.SERVICE_UUID));
                    filters.add(builder.build());
                }

                ScanSettings.Builder builder = new ScanSettings.Builder();
                // Set scan mode for Bluetooth LE scan.
                builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
                // Set callback type for Bluetooth LE scan.
                builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
                // Set match mode for Bluetooth LE scan filters hardware match.
                // MATCH_MODE_STICKY: For sticky mode, higher threshold of signal strength and sightings is required before reporting by hw
                // MATCH_MODE_AGGRESSIVE: In Aggressive mode, hw will determine a match sooner even with feeble signal strength and few number of sightings/match in a duration.
                builder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE);
                ScanSettings settings = builder.build();

                // Start Bluetooth LE scan.
                scanner.startScan(filters, settings, scanCallback);
                mScanning = true;
            } catch (Exception e) {
                Log.e(getLogTag(), "startScan", e);

                call.error(e.getLocalizedMessage(), e);
                return;
            }
        } else {
            try {
                // Stops an ongoing Bluetooth LE scan.
                scanner.stopScan(scanCallback);
                mScanning = false;
            } catch (Exception e) {
                Log.e(getLogTag(), "stopScan", e);

                call.error(e.getLocalizedMessage(), e);
                return;
            }
        }

        JSObject data = new JSObject();
        data.put("isScanning", mScanning);

        call.success(data);
    }

    private void showEnableLocationSetting() {
        final Activity activity = getBridge().getActivity();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .setNeedBle(true);

        LocationServices.getSettingsClient(activity)
                .checkLocationSettings(builder.build())
                .addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
                    @Override
                    public void onComplete(Task<LocationSettingsResponse> task) {
                        try {
                            LocationSettingsResponse response = task.getResult(ApiException.class);
                            // All location settings are satisfied. The client can initialize location
                            // requests here.
                        } catch (ApiException exception) {
                            switch (exception.getStatusCode()) {
                                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                    // Location settings are not satisfied. But could be fixed by showing the
                                    // user a dialog.
                                    try {
                                        // Cast to a resolvable exception.
                                        ResolvableApiException resolvable = (ResolvableApiException) exception;
                                        // Show the dialog by calling startResolutionForResult(),
                                        // and check the result in onActivityResult().
                                        resolvable.startResolutionForResult(
                                                activity,
                                                REQUEST_LOCATION_SERVICE
                                        );
                                    } catch (IntentSender.SendIntentException e) {
                                        // Ignore the error.
                                    } catch (ClassCastException e) {
                                        // Ignore, should be an impossible error.
                                    }
                                    break;
                                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                    // Location settings are not satisfied. However, we have no way to fix the
                                    // settings so we won't show the dialog.
                                    break;
                            }
                        }
                    }
                });
    }
}
