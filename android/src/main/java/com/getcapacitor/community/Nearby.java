package com.getcapacitor.community;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
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
import android.os.Build;
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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static android.bluetooth.le.ScanCallback.SCAN_FAILED_ALREADY_STARTED;

interface Constants {
    // v5 (Name-based | SHA1 hash) UUID (winkee.app)
//    UUID SERVICE_UUID = UUID.fromString("1c2cceae-66cd-55cd-8769-d961a7412368");
    UUID SERVICE_UUID = UUID.fromString("1c2cceae-0000-1000-8000-00805f9b34fb");

    String BLUETOOTH_NOT_SUPPORTED = "Bluetooth not supported";
    String BLE_NOT_SUPPORTED = "Bluetooth Low Energy not supported";
    String NOT_INITIALIZED = "not initialized";
    String PERMISSION_DENIED = "permission denied";

    String UUID_NOT_FOUND = "UUID not found";
}

@NativePlugin(
        permissions = {
                // Allows an app to access approximate location.
                Manifest.permission.ACCESS_COARSE_LOCATION,
                // Allows an app to access precise location.
                Manifest.permission.ACCESS_FINE_LOCATION,

                // Allows applications to connect to paired bluetooth devices.
                Manifest.permission.BLUETOOTH,
                // Allows applications to discover and pair bluetooth devices.
                Manifest.permission.BLUETOOTH_ADMIN,
        }
)
public class Nearby extends Plugin {
    protected static final int REQUEST_PERMISSIONS = 10000;

    protected static final int REQUEST_LOCATION_SERVICE = 10001;
    protected static final int REQUEST_BLUETOOTH_SERVICE = 10002;

    private BluetoothAdapter mAdapter;

    private Scanner mScanner;
    private Advertiser mAdvertiser;

    Integer advertiseMode = AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY;
    Integer txPowerLevel = AdvertiseSettings.ADVERTISE_TX_POWER_HIGH;

    private UUID uuid;
    private byte[] data;

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

        stop();
    }

    @Override
    protected void handleOnResume() {
        super.handleOnResume();

        // Avoid asking for Bluetooth permission when starting the app.
//        if (!isBluetoothEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(null, enableBtIntent, REQUEST_BLUETOOTH_SERVICE);
//        } else {
//            checkPermissions();
//        }
    }

    @PluginMethod
    public void initialize(PluginCall call) {
        try {
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

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
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
                } else {
                    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        saveCall(call);

                        if (isGooglePlayServicesAvailable()) {
                            showEnableLocationSetting();
                        } else {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(call, intent, REQUEST_LOCATION_SERVICE);
                        }

                        return;
                    }
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

                mAdapter =
                        BluetoothAdapter.getDefaultAdapter();

                mScanner = new Scanner(
                        this.mAdapter,

                        new Scanner.BeaconCallback() {
                            @Override
                            public void onFound(UUID uuid, byte[] data) {
                                try {
                                    if (mScanner.isScanning()) {
                                        JSObject jsData =
                                                new JSObject()
                                                        .put("uuid", uuid.toString());

                                        if (data != null && data.length > 0) {
                                            jsData.put("content", Base64.encodeToString(data, Base64.DEFAULT | Base64.NO_WRAP));
                                        }

                                        notifyListeners("onFound", jsData);
                                    }
                                } catch (Exception e) {
                                    Log.e(getLogTag(), "onFound", e);
                                }
                            }

                            @Override
                            public void onLost(UUID uuid, byte[] data) {
                                try {
                                    if (mScanner.isScanning()) {
                                        JSObject jsData =
                                                new JSObject()
                                                        .put("uuid", uuid.toString());

                                        if (data != null && data.length > 0) {
                                            jsData.put("content", Base64.encodeToString(data, Base64.DEFAULT | Base64.NO_WRAP));
                                        }

                                        notifyListeners("onLost", jsData);
                                    }
                                } catch (Exception e) {
                                    Log.e(getLogTag(), "onLost", e);
                                }
                            }
                        }
                );

                mAdvertiser = new Advertiser(
                        this.mAdapter
                );

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

    private void start(PluginCall call) {
        if (mAdvertiser != null && uuid != null) {
            Advertiser.Beacon beacon = new Advertiser.Beacon(
                    uuid,
                    data
            );

            mAdvertiser.start(
                    beacon,

                    new Advertiser.Callback() {
                        @Override
                        public void onSuccess(AdvertiseSettings settings) {
                            call.success(
                                    new JSObject()
                                            .put("uuid", uuid)
                            );
                        }

                        @Override
                        public void onFailure(int errorCode, String errorMessage) {
                            call.error(errorMessage, String.valueOf(errorCode), null);
                        }
                    }
            );
        }

        if (mScanner != null) {
            mScanner.start(
                    new Scanner.Callback() {
                        @Override
                        public void onFailure(int errorCode, String errorMessage) {
                            call.error(errorMessage, String.valueOf(errorCode), null);
                        }
                    }
            );
        }
    }

    private void stop() {
        doUnsubscribe();

        if (mAdvertiser != null) {
            mAdvertiser.stop();
        }
    }

    @PluginMethod()
    public void reset(PluginCall call) {
        try {
            stop();

//            initialize(call);
            
            uuid = null;
            data = null;

            call.success();
        } catch (Exception e) {
            call.error(e.getLocalizedMessage(), e);
        }
    }

    @PluginMethod()
    public void pause(PluginCall call) {
        if (mAdapter == null) {
            call.reject(Constants.NOT_INITIALIZED);
            return;
        }

        try {
            stop();

            call.success();
        } catch (Exception e) {
            call.error(e.getLocalizedMessage(), e);
        }
    }

    @PluginMethod()
    public void resume(PluginCall call) {
        if (mAdapter == null) {
            call.reject(Constants.NOT_INITIALIZED);
            return;
        }

        try {
            start(call);

            call.success();
        } catch (Exception e) {
            call.error(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Publish
     */

    @PluginMethod
    public void publish(PluginCall call) {
        if (mAdapter == null) {
            call.reject(Constants.NOT_INITIALIZED);
            return;
        }

        try {
            JSObject messageObject = call.getObject("message", null);
            if (messageObject != null) {
                String messageUUID = messageObject.getString("uuid", null);
                if (messageUUID != null && messageUUID.length() > 0) {
                    uuid = UUID.fromString(messageUUID);
                } else {
                    call.reject(Constants.UUID_NOT_FOUND);
                    return;
                }

                String content = messageObject.getString("content", null);
                if (content != null && content.length() > 0) {
                    data = Base64.decode(content, Base64.DEFAULT);
                }
            }

            Integer ttlSeconds = null;

            JSObject optionsObject = call.getObject("options", null);
            if (optionsObject != null) {
                ttlSeconds = optionsObject.getInteger("ttlSeconds");
            }

            if (!mAdvertiser.isAdvertising()) {
                Advertiser.Beacon beacon = new Advertiser.Beacon(uuid, data);

                mAdvertiser.start(
                        beacon,

                        new Advertiser.Callback() {
                            @Override
                            public void onSuccess(AdvertiseSettings settings) {
                                call.success(
//                                        new JSObject()
//                                                .put("uuid", uuid)
                                );
                            }

                            @Override
                            public void onFailure(int errorCode, String errorMessage) {
                                call.error(errorMessage, String.valueOf(errorCode), null);
                            }
                        }
                );
            } else {
                call.success(
//                        new JSObject()
//                                .put("uuid", uuid)
                );
            }

            if (ttlSeconds != null) {
                // Sets the time to live in seconds for the publish or subscribe.
                // Stops scanning after a pre-defined scan period.
                handler.postDelayed(() -> onPublishExpired(/*uuid*/), ttlSeconds * 1000);
            }
        } catch (Exception e) {
            Log.e(getLogTag(), "publish", e);

            call.error(e.getLocalizedMessage(), e);
        }
    }

    private void onPublishExpired(/*UUID uuid*/) {
        if (mAdvertiser.isAdvertising()) {
            notifyListeners("onPublishExpired",
                    null
//                    new JSObject()
//                            .put("uuid", uuid)
            );
        }

        mAdvertiser.stop();
    }

    @PluginMethod
    public void unpublish(PluginCall call) {
        if (mAdapter == null) {
            call.reject(Constants.NOT_INITIALIZED);
            return;
        }

        try {
//            String messageUUID = call.getString("uuid", null);

            mAdvertiser.stop();

            uuid = null;
            data = null;

            call.success();
        } catch (Exception e) {
            Log.e(getLogTag(), "stopAdvertising", e);

            call.error(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Subscribe
     */

    @PluginMethod
    public void subscribe(final PluginCall call) {
        if (mAdapter == null) {
            call.reject(Constants.NOT_INITIALIZED);
            return;
        }

        if (!mScanner.isScanning()) {
            try {
                Integer ttlSeconds = null;

                JSObject optionsObject = call.getObject("options", null);
                if (optionsObject != null) {
                    ttlSeconds = optionsObject.getInteger("ttlSeconds");
                }

                mScanner.start(
                        new Scanner.Callback() {
                            @Override
                            public void onFailure(int errorCode, String errorMessage) {
                                call.error(errorMessage, String.valueOf(errorCode), null);
                            }
                        }
                );

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

    private void onSubscribeExpired() {
        if (mScanner.isScanning()) {
            notifyListeners("onSubscribeExpired", null);
        }

        doUnsubscribe();
    }

    @PluginMethod()
    public void unsubscribe(PluginCall call) {
        if (mAdapter == null) {
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

    private void doUnsubscribe() {
        if (mScanner != null) {
            mScanner.stop();
        }
    }

    @PluginMethod()
    public void status(PluginCall call) {
        try {
            boolean isPublishing = mAdvertiser.isAdvertising();
            boolean isSubscribing = mScanner.isScanning();

            Set<UUID> uuids = null;// = this.beacons.keySet();

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

    private boolean isBluetoothEnabled() {
        BluetoothAdapter bluetoothAdapter =
                BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            return false;
        }

        return bluetoothAdapter.isEnabled();
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
