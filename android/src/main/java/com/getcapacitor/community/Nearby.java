package com.getcapacitor.community;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import androidx.activity.result.ActivityResult;
import androidx.annotation.StringDef;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

interface Constants {
    String BLUETOOTH_LE_NOT_SUPPORTED = "Bluetooth Low Energy not supported";
    String BLUETOOTH_NOT_AVAILABLE = "Bluetooth not available";
    String NOT_INITIALIZED = "not initialized";
    String PERMISSION_DENIED = "permission denied";

    String UUID_NOT_FOUND = "UUID not found";

    String BLUETOOTH_BASE_UUID = "0000-1000-8000-00805f9b34fb";

    @StringDef(
        {
            BluetoothState.UNKNOWN,
            BluetoothState.RESETTING,
            BluetoothState.UNSUPPORTED,
            BluetoothState.UNAUTHORIZED,
            BluetoothState.POWERED_OFF,
            BluetoothState.POWERED_ON
        }
    )
    @Retention(RetentionPolicy.SOURCE)
    @interface BluetoothState {
        // The manager’s state is unknown.
        String UNKNOWN = "unknown";

        // A state that indicates the connection with the system service was momentarily
        // lost.
        String RESETTING = "resetting";

        // A state that indicates this device doesn’t support the Bluetooth low energy
        // central or client role.
        String UNSUPPORTED = "unsupported";

        // A state that indicates the application isn’t authorized to use the Bluetooth
        // low energy role.
        String UNAUTHORIZED = "unauthorized";

        // A state that indicates Bluetooth is currently powered off.
        String POWERED_OFF = "poweredOff";

        // A state that indicates Bluetooth is currently powered on and available to
        // use.
        String POWERED_ON = "poweredOn";
    }
}

@SuppressLint("InlinedApi")
@CapacitorPlugin(
    name = "Nearby",
    permissions = {
        @Permission(
            strings = {
                // Allows an app to access approximate location.
                Manifest.permission.ACCESS_COARSE_LOCATION,
                // Allows an app to access precise location.
                Manifest.permission.ACCESS_FINE_LOCATION
            },
            alias = "location"
        ),
        @Permission(
            strings = {
                // Allows applications to connect to paired bluetooth devices.
                Manifest.permission.BLUETOOTH,
                // Allows applications to discover and pair bluetooth devices.
                Manifest.permission.BLUETOOTH_ADMIN
            },
            alias = "bluetoothLegacy"
        ),
        @Permission(
            strings = {
                // Required to be able to connect to paired Bluetooth devices.
                Manifest.permission.BLUETOOTH_CONNECT,
                // Required to be able to advertise to nearby Bluetooth devices.
                Manifest.permission.BLUETOOTH_ADVERTISE,
                // Required to be able to discover and pair nearby Bluetooth devices.
                Manifest.permission.BLUETOOTH_SCAN
            },
            alias = "bluetooth"
        )
    }
)
public class Nearby extends Plugin {

    private final ArrayList<String> aliases = new ArrayList<>();

    private BroadcastReceiver mReceiver;
    private BluetoothAdapter mAdapter;

    private Scanner mScanner;
    private Advertiser mAdvertiser;

    protected UUID serviceUUID;
    protected UUID serviceMask;

    private UUID uuid;
    private byte[] data;

    /**
     * Clean up callback to prevent leaks.
     */
    @Override
    protected void handleOnDestroy() {
        super.handleOnDestroy();

        stop();

        if (mReceiver != null) {
            try {
                getContext().unregisterReceiver(mReceiver);
            } catch (final IllegalArgumentException e) {
                // The receiver was not registered.
            }

            mReceiver = null;
        }
    }

    /**
     * Initialize
     */

    @PluginMethod
    public void initialize(PluginCall call) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            aliases.add("bluetooth");
        } else {
            aliases.add("bluetoothLegacy");
        }
        aliases.add("location");

        requestPermissionForAliases(aliases.toArray(new String[0]), call, "initializeCallback");
    }

    @PermissionCallback
    private void initializeCallback(PluginCall call) {
        for (String alias : aliases) {
            if (getPermissionState(alias) != PermissionState.GRANTED) {
                call.reject(Constants.PERMISSION_DENIED);
                return;
            }
        }

        initializeBluetooth(call);
    }

    private void initializeBluetooth(PluginCall call) {
        if (!getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.i(getLogTag(), Constants.BLUETOOTH_LE_NOT_SUPPORTED);

            call.reject(Constants.BLUETOOTH_LE_NOT_SUPPORTED);
            return;
        }

        if (!isBluetoothEnabled()) {
            final Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(call, intent, "initializeBluetoothCallback");

            return;
        }

        String serviceUUID = call.getString("serviceUUID", null);
        if (serviceUUID != null && serviceUUID.length() > 0) {
            serviceUUID = serviceUUID.replace("0x", "");
            String serviceMask = "00000000-0000-0000-0000-000000000000";

            switch (serviceUUID.length()) {
                case 4 -> {
                    serviceUUID = "0000" + serviceUUID + "-" + Constants.BLUETOOTH_BASE_UUID;
                    serviceMask = "0000ffff-0000-0000-0000-000000000000";
                }
                case 8 -> {
                    serviceUUID = serviceUUID + "-" + Constants.BLUETOOTH_BASE_UUID;
                    serviceMask = "ffffffff-0000-0000-0000-000000000000";
                }
            }

            this.serviceUUID = UUID.fromString(serviceUUID);
            this.serviceMask = UUID.fromString(serviceMask);
        } else {
            call.reject(Constants.UUID_NOT_FOUND);
            return;
        }

        Integer scanMode = call.getInt("scanMode", ScanSettings.SCAN_MODE_BALANCED);

        Integer advertiseMode = call.getInt("advertiseMode", AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
        Integer txPowerLevel = call.getInt("txPowerLevel", AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);

        mAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mAdapter == null || !mAdapter.isEnabled()) {
            Log.i(getLogTag(), Constants.BLUETOOTH_NOT_AVAILABLE);

            call.reject(Constants.BLUETOOTH_NOT_AVAILABLE);
            return;
        }

        mScanner =
            Scanner.getInstance(
                this.mAdapter,
                this.serviceUUID,
                this.serviceMask,
                new Scanner.BeaconCallback() {
                    @Override
                    public void onFound(UUID uuid, byte[] data) {
                        try {
                            if (mScanner.isScanning()) {
                                JSObject jsData = new JSObject().put("uuid", uuid.toString());

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
                                JSObject jsData = new JSObject().put("uuid", uuid.toString());

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

        if (scanMode != null) {
            mScanner.setScanMode(scanMode);
        }

        mAdvertiser = Advertiser.getInstance(this.mAdapter, this.serviceUUID);

        if (advertiseMode != null) {
            mAdvertiser.setAdvertiseMode(advertiseMode);
        }
        if (txPowerLevel != null) {
            mAdvertiser.setTxPowerLevel(txPowerLevel);
        }

        registerReceiver();

        call.resolve();
    }

    @ActivityCallback
    private void initializeBluetoothCallback(PluginCall call, ActivityResult result) {
        boolean granted = result.getResultCode() == Activity.RESULT_OK;

        notifyListeners("onPermissionChanged", new JSObject().put("granted", granted));

        if (granted) {
            initialize(call);
        } else {
            call.reject(Constants.PERMISSION_DENIED);
        }
    }

    private void registerReceiver() {
        if (mReceiver == null) {
            mReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        final String action = intent.getAction();

                        if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                            notifyListeners("onBluetoothStateChanged", new JSObject().put("state", fromBluetoothState(state)));
                        }
                    }

                    private String fromBluetoothState(int bluetoothState) {
                        return switch (bluetoothState) {
                            case BluetoothAdapter.STATE_ON -> Constants.BluetoothState.POWERED_ON;
                            case BluetoothAdapter.STATE_OFF -> Constants.BluetoothState.POWERED_OFF;
                            case BluetoothAdapter.STATE_TURNING_ON,
                                BluetoothAdapter.STATE_TURNING_OFF -> Constants.BluetoothState.RESETTING;
                            default -> Constants.BluetoothState.UNKNOWN;
                        };
                    }
                };

            IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            getContext().registerReceiver(mReceiver, filter);
        }
    }

    /**
     * Reset
     */

    @PluginMethod
    public void reset(PluginCall call) {
        try {
            stop();

            uuid = null;
            data = null;

            call.resolve();
        } catch (Exception e) {
            call.reject(e.getLocalizedMessage(), e);
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

            if (!mAdvertiser.isAdvertising()) {
                Advertiser.Beacon beacon = new Advertiser.Beacon(uuid, data);

                mAdvertiser.start(
                    beacon,
                    call.getInt("ttlSeconds", null),
                    new Advertiser.Callback() {
                        @Override
                        public void onSuccess(AdvertiseSettings settings) {
                            call.resolve();
                        }

                        @Override
                        public void onFailure(int errorCode, String errorMessage) {
                            call.reject(errorMessage, String.valueOf(errorCode));
                        }

                        @Override
                        public void onExpired() {
                            onPublishExpired();
                        }
                    }
                );
            } else {
                call.resolve();
            }
        } catch (Exception e) {
            Log.e(getLogTag(), "publish", e);

            call.reject(e.getLocalizedMessage(), e);
        }
    }

    private void onPublishExpired() {
        if (mAdvertiser.isAdvertising()) {
            notifyListeners("onPublishExpired", null);
        }

        doUnpublish();
    }

    @PluginMethod
    public void unpublish(PluginCall call) {
        if (mAdapter == null) {
            call.reject(Constants.NOT_INITIALIZED);
            return;
        }

        try {
            doUnpublish();

            uuid = null;
            data = null;

            call.resolve();
        } catch (Exception e) {
            Log.e(getLogTag(), "stopAdvertising", e);

            call.reject(e.getLocalizedMessage(), e);
        }
    }

    private void doUnpublish() {
        if (mAdvertiser != null) {
            mAdvertiser.stop();
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
                mScanner.start(
                    call.getInt("ttlSeconds", null),
                    new Scanner.Callback() {
                        @Override
                        public void onFailure(int errorCode, String errorMessage) {
                            call.reject(errorMessage, String.valueOf(errorCode));
                        }

                        @Override
                        public void onExpired() {
                            onSubscribeExpired();
                        }
                    }
                );
            } catch (Exception e) {
                Log.e(getLogTag(), "scan", e);

                call.reject(e.getLocalizedMessage(), e);
                return;
            }
        } else {
            doUnsubscribe();
        }

        call.resolve();
    }

    private void onSubscribeExpired() {
        if (mScanner.isScanning()) {
            notifyListeners("onSubscribeExpired", null);
        }

        doUnsubscribe();
    }

    @PluginMethod
    public void unsubscribe(PluginCall call) {
        if (mAdapter == null) {
            call.reject(Constants.NOT_INITIALIZED);
            return;
        }

        try {
            doUnsubscribe();

            call.resolve();
        } catch (Exception e) {
            call.reject(e.getLocalizedMessage(), e);
        }
    }

    private void doUnsubscribe() {
        if (mScanner != null) {
            mScanner.stop();
        }
    }

    /**
     * Status
     */

    @PluginMethod
    public void status(PluginCall call) {
        try {
            boolean isPublishing = mAdvertiser.isAdvertising();
            boolean isSubscribing = mScanner.isScanning();

            Set<UUID> uuids = mScanner.getBeacons();

            call.resolve(
                new JSObject().put("isPublishing", isPublishing).put("isSubscribing", isSubscribing).put("uuids", new JSArray(uuids))
            );
        } catch (Exception e) {
            call.reject(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Helper
     */

    private boolean isBluetoothEnabled() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            return false;
        }

        return bluetoothAdapter.isEnabled();
    }

    private void stop() {
        doUnsubscribe();
        doUnpublish();
    }
}
