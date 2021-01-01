package com.getcapacitor.plugin;

import android.Manifest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.ScanFilter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Handler;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.protobuf.ByteString;

import com.welie.blessed.BluetoothCentral;

import at.favre.lib.bytes.Bytes;

interface Constants {
    // v5 (Name-based | SHA1 hash) UUID (winkee.app)
    UUID SERVICE_UUID = UUID.fromString("1c2cceae-66cd-55cd-8769-d961a7412368");
    // v5 (Name-based | SHA1 hash) UUID (profile.winkee.app)
    UUID CHARACTERISTIC_UUID = UUID.fromString("35274eec-ae41-5975-a27a-608b334ce36e");

    String BLUETOOTH_NOT_SUPPORTED = "Bluetooth not supported";
    String BLE_NOT_SUPPORTED = "Bluetooth Low Energy not supported";
    String NOT_INITIALIZED = "API not initialized";
    String ALREADY_INITIALIZED = "API already initialized";
    String PERMISSION_DENIED = "permissions not granted";
    String PUBLISH_MESSAGE_CONTENT = "must provide message with content";
    String PUBLISH_MESSAGE_TYPE = "must provide message with type";
    String PUBLISH_MESSAGE = "must provide message";
    String MESSAGE_UUID_NOT_FOUND = "message UUID not found";

    int GATT_MTU_SIZE_DEFAULT = 23;
    int GATT_MTU_SIZE = 185;    // iOS always asks for 185
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

    private Scanner scanner;
    private Server server;

    Integer advertiseMode = AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY;
    Integer txPowerLevel = AdvertiseSettings.ADVERTISE_TX_POWER_HIGH;

    private BluetoothLeAdvertiser advertiser;
    private AdvertiseCallback advertiseCallback;

    private boolean mScanning;
    private boolean mAdvertising;

    private final Map<UUID, Proto.Message> messages = new HashMap<>();

    private Handler handler = new Handler();

    @Override
    protected void handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.handleRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean permissionGranted = false;

        PluginCall call = getSavedCall();
        if (call != null) {
            freeSavedCall();

            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_DENIED) {
                    call.reject(Constants.PERMISSION_DENIED);
                    break;
                }
            }

            if (permissionGranted) {
                initialize(call);
            }
        }

        notifyListeners("onPermissionChanged",
                new JSObject()
                        .put("permissionGranted", permissionGranted)
        );
    }

    @Override
    protected void handleOnActivityResult(int requestCode, int resultCode, Intent intentData) {
        super.handleOnActivityResult(requestCode, resultCode, intentData);

        {
            boolean permissionGranted = resultCode == Activity.RESULT_OK;

            notifyListeners("onPermissionChanged",
                    new JSObject()
                            .put("permissionGranted", permissionGranted)
            );
        }

        PluginCall call = getSavedCall();
        if (call != null) {
            freeSavedCall();

            if (resultCode == Activity.RESULT_OK) {
                initialize(call);
            } else {
                call.reject(Constants.PERMISSION_DENIED);
            }
        }
    }

    @Override
    protected void handleOnDestroy() {
        super.handleOnDestroy();

        close();
    }

    @Override
    protected void handleOnResume() {
        super.handleOnResume();

        if (!isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(null, enableBtIntent, REQUEST_BLUETOOTH_SERVICE);
        } else {
//            checkPermissions();
        }
    }

    private boolean isBluetoothEnabled() {
        BluetoothAdapter bluetoothAdapter =
                BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            return false;
        }

        return bluetoothAdapter.isEnabled();
    }

    @SuppressLint("NewApi")
    @PluginMethod
    public void initialize(PluginCall call) {
        try {
//            if (manager != null || adapter != null || scanner != null || advertiser != null) {
//                call.reject(Constants.ALREADY_INITIALIZED);
//                return;
//            }

            if (!getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                Log.i(getLogTag(),
                        String.format(
                                Constants.BLUETOOTH_NOT_SUPPORTED));

                call.error(Constants.BLUETOOTH_NOT_SUPPORTED);
                return;
            }

            if (!getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Log.i(getLogTag(),
                        String.format(
                                Constants.BLE_NOT_SUPPORTED));

                call.error(Constants.BLE_NOT_SUPPORTED);
                return;
            }

            if (!isBluetoothEnabled()) {
                saveCall(call);

                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(call, intent, REQUEST_BLUETOOTH_SERVICE);

                return;
            }

            if (hasRequiredPermissions()) {
                final LocationManager locationManager =
                        (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);

                if (!locationManager.isLocationEnabled()) {
                    saveCall(call);

                    if (isGooglePlayServicesAvailable()) {
                        showEnableLocationSetting();
                    } else {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(call, intent, REQUEST_LOCATION_SERVICE);
                    }

                    return;
                }

                Integer scanMode = null;

                advertiseMode = AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY;
                txPowerLevel = AdvertiseSettings.ADVERTISE_TX_POWER_HIGH;

                JSObject optionsObject = call.getObject("options", null);
                if (optionsObject != null) {
                    scanMode = optionsObject.getInteger("scanMode");

                    advertiseMode = optionsObject.getInteger("advertiseMode");
                    txPowerLevel = optionsObject.getInteger("txPowerLevel");
                }

                BluetoothAdapter bluetoothAdapter =
                        BluetoothAdapter.getDefaultAdapter();

                advertiser =
                        bluetoothAdapter.getBluetoothLeAdvertiser();

                scanner =
                        Scanner.getInstance(getContext(), scannerCallback, handler, scanMode);

                server =
                        Server.getInstance(getContext());

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

    private final Scanner.Callback scannerCallback = new Scanner.Callback() {
        public void onFound(UUID uuid, byte[] data) {
            try {
                Proto.Message message = Proto.Message.parseFrom(data);

                if (mScanning) {
                    String content = Base64.encodeToString(message.getContent().toByteArray(), Base64.DEFAULT | Base64.NO_WRAP);

                    notifyListeners("onFound",
                            new JSObject()
                                    .put("uuid", uuid.toString())
                                    .put("timestamp", message.getTimestamp())

                                    .put("content", content)
                                    .put("type", message.getType())
                    );
                }
            } catch (Exception e) {
                Log.e(getLogTag(), "onFound", e);
            }
        }

        public void onLost(UUID uuid, byte[] data) {
            try {
                Proto.Message message = Proto.Message.parseFrom(data);

                if (mScanning) {
                    String content = Base64.encodeToString(message.getContent().toByteArray(), Base64.DEFAULT | Base64.NO_WRAP);

                    notifyListeners("onLost",
                            new JSObject()
                                    .put("uuid", uuid.toString())
                                    .put("timestamp", message.getTimestamp())

                                    .put("content", content)
                                    .put("type", message.getType())
                    );
                }
            } catch (Exception e) {
                Log.e(getLogTag(), "onLost", e);
            }
        }

        public void onPermissionChanged(Boolean permissionGranted) {
            if (!permissionGranted) {
                close();
            }

            notifyListeners("onPermissionChanged",
                    new JSObject()
                            .put("permissionGranted", permissionGranted)
            );
        }
    };

    private void close() {
        for (UUID uuid : this.messages.keySet()) {
            doUnpublish(uuid);
        }

        stopAdvertising();

        advertiser = null;
        advertiseCallback = null;

        server = null;
    }

    @PluginMethod()
    public void reset(PluginCall call) {
        try {
            close();

//            initialize(call);
            call.success();
        } catch (Exception e) {
            call.error(e.getLocalizedMessage(), e);
        }
    }

    @PluginMethod()
    public void pause(PluginCall call) {
        try {
            call.success();
        } catch (Exception e) {
            call.error(e.getLocalizedMessage(), e);
        }
    }

    @PluginMethod()
    public void resume(PluginCall call) {
        try {
            call.success();
        } catch (Exception e) {
            call.error(e.getLocalizedMessage(), e);
        }
    }

    @PluginMethod
    public void publish(PluginCall call) {
        if (advertiser == null) {
            call.reject(Constants.NOT_INITIALIZED);
            return;
        }

        try {
            Proto.Message message;

            JSObject messageObject = call.getObject("message", null);
            if (messageObject != null) {
                String content = messageObject.getString("content", null);
                if (content == null || content.length() == 0) {
                    call.reject(Constants.PUBLISH_MESSAGE_CONTENT);
                    return;
                }

                String type = messageObject.getString("type", null);
                if (type == null || type.length() == 0) {
                    call.reject(Constants.PUBLISH_MESSAGE_TYPE);
                    return;
                }

                // A message that will be shared with nearby devices.
                message = Proto.Message.newBuilder()
                        .setUuid(
                                ByteString.copyFrom(
                                        Bytes.from(
                                                UUID.randomUUID()
                                        ).array()
                                )
                        )
                        .setTimestamp(System.currentTimeMillis())

                        // An arbitrary array holding the content of the message. The maximum content size is MAX_CONTENT_SIZE_BYTES.
                        .setContent(ByteString.copyFrom(Base64.decode(content, Base64.DEFAULT)))
                        // A string that describe what the bytes of the content represent. The maximum type length is MAX_TYPE_LENGTH.
                        .setType(type)
                        .build();
            } else {
                call.reject(Constants.PUBLISH_MESSAGE);
                return;
            }

            Integer ttlSeconds = null;

            JSObject optionsObject = call.getObject("options", null);
            if (optionsObject != null) {
                ttlSeconds = optionsObject.getInteger("ttlSeconds");
            }

            if (!mAdvertising) {
                if (advertiseCallback == null) {
                    // Bluetooth LE advertising callbacks, used to deliver advertising operation status.
                    // https://developer.android.com/reference/android/bluetooth/le/AdvertiseCallback
                    advertiseCallback =
                            new AdvertiseCallback() {
                                @Override
                                // Callback triggered in response to BluetoothLeAdvertiser#startAdvertising indicating that the advertising has been started successfully.
                                public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                                    startAdvertising(message);

                                    call.success(
                                            new JSObject()
                                                    .put("uuid", Bytes.wrap(message.getUuid().toByteArray()).toUUID())
                                                    .put("timestamp", message.getTimestamp())
                                    );
                                }

                                @Override
                                // Callback when advertising could not be started.
                                public void onStartFailure(int errorCode) {
                                    stopAdvertising();

                                    call.error(advertiseFailed(errorCode));
                                }
                            };
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
                                .setConnectable(true)
                                .build();

                // Advertise data packet container for Bluetooth LE advertising.
                // This represents the data to be advertised as well as the scan response data for active scans.
                AdvertiseData advertiseData =
                        new AdvertiseData.Builder()
                                // Add a service UUID to advertise data.
                                .addServiceUuid(new ParcelUuid(Constants.SERVICE_UUID))
                                // Whether the transmission power level should be included in the advertise packet.
                                .setIncludeTxPowerLevel(false)
                                // Set whether the device name should be included in advertise packet.
                                .setIncludeDeviceName(false)
                                .build();

                // java.lang.IllegalArgumentException: Legacy advertising data too big
                // java.lang.IllegalArgumentException: Advertising data too big
                advertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback);
                mAdvertising = true;
            } else {
                startAdvertising(message);

                call.success(
                        new JSObject()
                                .put("uuid", Bytes.wrap(message.getUuid().toByteArray()).toUUID())
                                .put("timestamp", message.getTimestamp())
                );
            }

            if (ttlSeconds != null) {
                // Sets the time to live in seconds for the publish or subscribe.
                // Stops scanning after a pre-defined scan period.
                handler.postDelayed(() -> onPublishExpired(Bytes.wrap(message.getUuid().toByteArray()).toUUID()), ttlSeconds * 1000);
            }
        } catch (Exception e) {
            Log.e(getLogTag(), "publish", e);

            call.error(e.getLocalizedMessage(), e);
        }
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

    private void onPublishExpired(UUID uuid) {
        if (this.messages.containsKey(uuid)) {
            notifyListeners("onPublishExpired",
                    new JSObject()
                            .put("uuid", uuid)
            );
        }

        doUnpublish(uuid);

        stopAdvertising();
    }

    private void startAdvertising(Proto.Message message) {
        UUID uuid = Bytes.wrap(message.getUuid().toByteArray()).toUUID();

        this.messages.put(uuid, message);

        if (server != null) {
            server.addMessage(uuid, message.toByteArray());

            server.restart();

            mAdvertising = true;
        }
    }

    private void stopAdvertising() {
        // Only stop server if there are no more messages to publish.
        if (this.messages.isEmpty()) {
            if (server != null) {
                server.stop();

                // We need to keep the reference.
//                server = null;
            }

            if (advertiser != null && advertiseCallback != null) {
                advertiser.stopAdvertising(advertiseCallback);

                // We need to keep the reference.
//                advertiser = null;
                advertiseCallback = null;
            }

            mAdvertising = false;
        } else {
            if (server != null) {
                server.restart();
            }
        }
    }

    @PluginMethod
    public void unpublish(PluginCall call) {
        if (advertiser == null) {
            call.reject(Constants.NOT_INITIALIZED);
            return;
        }

        try {
            String messageUUID = call.getString("uuid", null);

            if (messageUUID == null || messageUUID.length() == 0) {
                // Unpublish all messages.
                for (UUID uuid : this.messages.keySet()) {
                    doUnpublish(uuid);
                }
            } else {
                // Unpublish message.
                UUID uuid = UUID.fromString(messageUUID);

                if (this.messages.containsKey(uuid)) {
                    doUnpublish(uuid);
                } else {
                    call.reject(Constants.MESSAGE_UUID_NOT_FOUND);
                    return;
                }
            }

            stopAdvertising();

            call.success();
        } catch (Exception e) {
            Log.e(getLogTag(), "stopAdvertising", e);

            call.error(e.getLocalizedMessage(), e);
        }
    }

    private void doUnpublish(UUID uuid) {
        if (server != null) {
            server.removeMessage(uuid);
        }

        this.messages.remove(uuid);
    }

    @PluginMethod
    public void subscribe(final PluginCall call) {
        if (scanner == null) {
            call.reject(Constants.NOT_INITIALIZED);
            return;
        }

        if (!mScanning) {
            try {
                Integer ttlSeconds = null;

                JSObject optionsObject = call.getObject("options", null);
                if (optionsObject != null) {
                    ttlSeconds = optionsObject.getInteger("ttlSeconds");
                }

                List<ScanFilter> filters = new ArrayList<>();
                ScanFilter filter = new ScanFilter.Builder()
                        .setServiceUuid(new ParcelUuid(Constants.SERVICE_UUID))
                        // Set partial filter on service data.
                        // For any bit in the mask, set it to 1 if it needs to match the one in service data,
                        // otherwise set it to 0 to ignore that bit.
//                        .setServiceData(
//                                new ParcelUuid(Constants.SERVICE_UUID),
//                                new byte[]{(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF},
//                                new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00}
//                        )
                        .build();
                filters.add(filter);

                scanner.start(filters, new Scanner.ScanCallback() {
                    @Override
                    public void onFailed(int errorCode) {
                        call.error(scanFailed(errorCode));
                    }
                });

                // Start Bluetooth LE scan.
//                scanner.startScan(filters, settings, scanCallback);
                mScanning = true;

                if (ttlSeconds != null) {
                    // Sets the time to live in seconds for the publish or subscribe.
                    // Stops scanning after a pre-defined scan period.
                    handler.postDelayed(() -> onSubscribeExpired(), ttlSeconds * 1000);
                }
            } catch (Exception e) {
                Log.e(getLogTag(), "scan", e);

                call.error(e.getLocalizedMessage(), e);
                return;
            }
        } else {
            doUnsubscribe();
        }

        call.success();
    }

    private String scanFailed(int errorCode) {
        switch (errorCode) {
            case BluetoothCentral.SCAN_FAILED_ALREADY_STARTED:
                return "Fails to start scan as BLE scan with the same settings is already started by the app.";
            case BluetoothCentral.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                return "Fails to start scan as app cannot be registered.";
            case BluetoothCentral.SCAN_FAILED_INTERNAL_ERROR:
                return "Fails to start scan due an internal error.";
            case BluetoothCentral.SCAN_FAILED_FEATURE_UNSUPPORTED:
                return "Fails to start power optimized scan as this feature is not supported.";
            case BluetoothCentral.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES:
                return "Failed to start scan as it is out of hardware resources.";
            case BluetoothCentral.SCAN_FAILED_SCANNING_TOO_FREQUENTLY:
                return "Failed to start scan as application tries to scan too frequently.";
            default:
                return "Unknown error.";
        }
    }

    private void onSubscribeExpired() {
        if (mScanning) {
            notifyListeners("onSubscribeExpired", null);
        }

        doUnsubscribe();
    }

    private void doUnsubscribe() {
        mScanning = false;

        if (scanner != null) {
            scanner.stop();
        }
    }

    @PluginMethod()
    public void unsubscribe(PluginCall call) {
        if (scanner == null) {
            call.reject(Constants.NOT_INITIALIZED);
            return;
        }

        try {
            doUnsubscribe();

            call.success();
        } catch (Exception e) {
            call.error(e.getLocalizedMessage(), e);
        }
    }

    @PluginMethod()
    public void status(PluginCall call) {
        try {
            boolean isPublishing = mAdvertising;
            boolean isSubscribing = mScanning;

            Set<UUID> uuids = this.messages.keySet();

            Log.i(getLogTag(),
                    String.format(
                            "status(isPublishing=%s, isSubscribing=%s, uuids=%s)",
                            isPublishing, isSubscribing, uuids));

            call.success(
                    new JSObject()
                            .put("isPublishing", isPublishing)
                            .put("isSubscribing", isSubscribing)
                            .put("uuids", new JSArray(uuids))
            );
        } catch (Exception e) {
            call.error(e.getLocalizedMessage(), e);
        }
    }

    private boolean isGooglePlayServicesAvailable() {
        final Activity activity = getBridge().getActivity();

        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();

        int status = googleApiAvailability.isGooglePlayServicesAvailable(activity);

        return (status == ConnectionResult.SUCCESS);
    }

    // https://developer.android.com/training/location/change-location-settings
    private void showEnableLocationSetting() {
        final Activity activity = getBridge().getActivity();

        LocationRequest locationRequest = LocationRequest.create();
        // This method sets the rate in milliseconds at which your app prefers to receive location updates.
        locationRequest.setInterval(10000);
        // This method sets the fastest rate in milliseconds at which your app can handle location updates.
        locationRequest.setFastestInterval(5000);
        // This method sets the priority of the request, which gives the Google Play services location services a strong hint about which location sources to use.
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                // Whether or not location is required by the calling app in order to continue.
                .setAlwaysShow(true)
                // Sets whether the client wants BLE scan to be enabled.
                .setNeedBle(true);

        Task<LocationSettingsResponse> task =
                LocationServices.getSettingsClient(activity)
                        .checkLocationSettings(builder.build());

        task.addOnFailureListener(activity, e -> {
            if (e instanceof ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(
                            activity,
                            REQUEST_LOCATION_SERVICE
                    );
                } catch (IntentSender.SendIntentException sendEx) {
                    // Ignore the error.
                }
            }
        });
    }
}
