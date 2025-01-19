package com.getcapacitor.community;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
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
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

interface Constants {
    String BLUETOOTH_LE_NOT_SUPPORTED = "Bluetooth Low Energy not supported";
    String BLUETOOTH_NOT_AVAILABLE = "Bluetooth not available";
    String NOT_INITIALIZED = "not initialized";
    String PERMISSION_DENIED = "permission denied";

    String MISSING_STRATEGY = "missing strategy";
    String UNKNOWN_STRATEGY = "unknown strategy";

    String UUID_NOT_FOUND = "UUID not found";

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

@CapacitorPlugin(
    name = "Nearby",
    permissions = {
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
        ),
        @Permission(
            strings = {
                // Required to be able to advertise and connect to nearby devices via Wi-Fi.
                Manifest.permission.NEARBY_WIFI_DEVICES
            },
            alias = "wifiNearby"
        ),
        @Permission(
            strings = {
                // Allows applications to access information about Wi-Fi networks.
                Manifest.permission.ACCESS_WIFI_STATE,
                // Allows applications to change Wi-Fi connectivity state.
                Manifest.permission.CHANGE_WIFI_STATE
            },
            alias = "wifiState"
        ),
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
                // Allows an app to access approximate location.
                Manifest.permission.ACCESS_COARSE_LOCATION
            },
            alias = "locationCoarse"
        )
    }
)
public abstract class NearbyPlugin extends Plugin {

    private final List<String> aliases = new ArrayList<String>();

    private UUID uuid;
    private Strategy strategy;
    private String name;
    private String serviceId;

    /**
     * Our handler to Nearby Connections.
     */
    private ConnectionsClient mConnectionsClient;

    /**
     * The devices we've discovered near us.
     */
    private final Map<String, Endpoint> mDiscoveredEndpoints = new HashMap<>();

    /**
     * The devices we have pending connections to. They will stay pending until we call {@link
     * #acceptConnection(Endpoint)} or {@link #rejectConnection(Endpoint)}.
     */
    private final Map<String, Endpoint> mPendingConnections = new HashMap<>();

    /**
     * The devices we are currently connected to. For advertisers, this may be large. For discoverers,
     * there will only be one entry in this map.
     */
    private final Map<String, Endpoint> mEstablishedConnections = new HashMap<>();

    /**
     * True if we are asking a discovered device to connect to us. While we ask, we cannot ask another
     * device.
     */
    private boolean mIsConnecting = false;

    /**
     * True if we are discovering.
     */
    private boolean mIsDiscovering = false;

    /**
     * True if we are advertising.
     */
    private boolean mIsAdvertising = false;

    @Override
    public void load() {
        mConnectionsClient = com.google.android.gms.nearby.Nearby.getConnectionsClient(getActivity());
    }

    /**
     * Clean up callback to prevent leaks.
     */
    @Override
    protected void handleOnDestroy() {
        super.handleOnDestroy();

        stop();
    }

    /**
     * Initialize
     */

    @PluginMethod
    public void initialize(PluginCall call) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            aliases.add("bluetooth");
            aliases.add("wifiState");
            aliases.add("wifiNearby");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            aliases.add("bluetooth");
            aliases.add("wifiState");
            aliases.add("location");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            aliases.add("bluetoothLegacy");
            aliases.add("wifiState");
            aliases.add("location");
        } else {
            aliases.add("bluetoothLegacy");
            aliases.add("wifiState");
            aliases.add("locationCoarse");
        }

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

        String strategy = call.getString("strategy", null);
        if (strategy == null) {
            call.reject(Constants.MISSING_STRATEGY);
            return;
        }
        switch (strategy) {
            case "cluster":
                this.strategy = Strategy.P2P_CLUSTER;
                break;
            case "star":
                this.strategy = Strategy.P2P_STAR;
                break;
            case "p2p":
                this.strategy = Strategy.P2P_POINT_TO_POINT;
                break;
            default:
                call.reject(Constants.UNKNOWN_STRATEGY);
                return;
        }

        this.name = call.getString("name");
        this.serviceId = call.getString("serviceId");

        Boolean lowPower = call.getBoolean("lowPower", false);

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

    /**
     * Reset
     */

    @PluginMethod
    public void reset(PluginCall call) {
        try {
            stop();

            uuid = null;

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
        if (mConnectionsClient == null) {
            call.reject(Constants.NOT_INITIALIZED);
            return;
        }

        if (!isAdvertising()) {
            mIsAdvertising = true;

            try {
                String beaconUUID = call.getString("uuid", null);
                if (beaconUUID != null && !beaconUUID.isEmpty()) {
                    uuid = UUID.fromString(beaconUUID);
                } else {
                    call.reject(Constants.UUID_NOT_FOUND);
                    return;
                }

                name = beaconUUID;

                AdvertisingOptions.Builder advertisingOptions = new AdvertisingOptions.Builder();
                advertisingOptions.setStrategy(getStrategy());

                mConnectionsClient
                    .startAdvertising(getName(), getServiceId(), mConnectionLifecycleCallback, advertisingOptions.build())
                    .addOnSuccessListener(unusedResult -> {
                        Log.v(getLogTag(), "Now advertising endpoint " + getName());
                        onAdvertisingStarted();

                        call.resolve();
                    })
                    .addOnFailureListener(e -> {
                        mIsAdvertising = false;

                        Log.w(getLogTag(), "startAdvertising() failed.", e);
                        onAdvertisingFailed();

                        call.reject(e.getLocalizedMessage(), e);
                    });
            } catch (Exception e) {
                Log.e(getLogTag(), "publish", e);

                call.reject(e.getLocalizedMessage(), e);
            }
        } else {
            stopAdvertising();
        }
    }

    @PluginMethod
    public void unpublish(PluginCall call) {
        if (mConnectionsClient == null) {
            call.reject(Constants.NOT_INITIALIZED);
            return;
        }

        try {
            stopAdvertising();

            uuid = null;
            name = null;

            call.resolve();
        } catch (Exception e) {
            Log.e(getLogTag(), "stopAdvertising", e);

            call.reject(e.getLocalizedMessage(), e);
        }
    }

    //    private void publishExpired() {
    //        if (mAdvertiser == null) {
    //            return;
    //        }
    //
    //        if (mAdvertiser.isAdvertising()) {
    //            notifyListeners("onPublishExpired", null);
    //        }
    //
    //        mAdvertiser.stop();
    //    }

    /**
     * Stops advertising.
     */
    protected void stopAdvertising() {
        mIsAdvertising = false;
        mConnectionsClient.stopAdvertising();
    }

    /**
     * Returns {@code true} if currently advertising.
     */
    protected boolean isAdvertising() {
        return mIsAdvertising;
    }

    /**
     * Called when advertising successfully starts. Override this method to act on the event.
     */
    protected void onAdvertisingStarted() {}

    /**
     * Called when advertising fails to start. Override this method to act on the event.
     */
    protected void onAdvertisingFailed() {}

    /**
     * Subscribe
     */

    @PluginMethod
    public void subscribe(final PluginCall call) {
        if (mConnectionsClient == null) {
            call.reject(Constants.NOT_INITIALIZED);
            return;
        }

        if (!isDiscovering()) {
            mIsDiscovering = true;
            mDiscoveredEndpoints.clear();

            try {
                DiscoveryOptions.Builder discoveryOptions = new DiscoveryOptions.Builder();
                discoveryOptions.setStrategy(getStrategy());

                mConnectionsClient
                    .startDiscovery(getServiceId(), mEndpointDiscoveryCallback, discoveryOptions.build())
                    .addOnSuccessListener(unusedResult -> {
                        Log.v(getLogTag(), "Now starting discovery");
                        onDiscoveryStarted();

                        call.resolve();
                    })
                    .addOnFailureListener(e -> {
                        mIsDiscovering = false;

                        Log.w(getLogTag(), "startDiscovering() failed.", e);
                        onDiscoveryFailed();

                        call.reject(e.getLocalizedMessage(), e);
                    });
            } catch (Exception e) {
                Log.e(getLogTag(), "scan", e);

                call.reject(e.getLocalizedMessage(), e);
            }
        } else {
            stopDiscovering();
        }
    }

    @PluginMethod
    public void unsubscribe(PluginCall call) {
        if (mConnectionsClient == null) {
            call.reject(Constants.NOT_INITIALIZED);
            return;
        }

        try {
            stopDiscovering();

            call.resolve();
        } catch (Exception e) {
            call.reject(e.getLocalizedMessage(), e);
        }
    }

    //    private void subscribeExpired() {
    //        if (mScanner == null) {
    //            return;
    //        }
    //
    //        if (mScanner.isScanning()) {
    //            notifyListeners("onSubscribeExpired", null);
    //        }
    //
    //        mScanner.stop();
    //    }

    /**
     * Stops discovery.
     */
    protected void stopDiscovering() {
        mIsDiscovering = false;
        mConnectionsClient.stopDiscovery();
    }

    /**
     * Returns {@code true} if currently discovering.
     */
    protected boolean isDiscovering() {
        return mIsDiscovering;
    }

    /**
     * Called when discovery successfully starts. Override this method to act on the event.
     */
    protected void onDiscoveryStarted() {}

    /**
     * Called when discovery fails to start. Override this method to act on the event.
     */
    protected void onDiscoveryFailed() {}

    /**
     * Status
     */

    @PluginMethod
    public void status(PluginCall call) {
        try {
            boolean isPublishing = mConnectionsClient != null && isAdvertising();
            boolean isSubscribing = mConnectionsClient != null && isDiscovering();

            Set<String> uuids = mConnectionsClient != null ? mEstablishedConnections.keySet() : Collections.emptySet();

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

    private void stop() {
        mConnectionsClient.stopAllEndpoints();

        mIsAdvertising = false;
        mIsDiscovering = false;
        mIsConnecting = false;

        mDiscoveredEndpoints.clear();
        mPendingConnections.clear();
        mEstablishedConnections.clear();
    }

    private boolean isBluetoothEnabled() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            return false;
        }

        return bluetoothAdapter.isEnabled();
    }

    public String getName() {
        return this.name;
    }

    public String getServiceId() {
        return this.serviceId;
    }

    public Strategy getStrategy() {
        return this.strategy;
    }

    /**
     * Callback for discovering endpoints.
     */
    private final EndpointDiscoveryCallback mEndpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo info) {
            Log.d(
                getLogTag(),
                String.format(
                    "onEndpointFound(endpointId=%s, serviceId=%s, endpointName=%s)",
                    endpointId,
                    info.getServiceId(),
                    info.getEndpointName()
                )
            );

            if (getServiceId().equals(info.getServiceId())) {
                Endpoint endpoint = new Endpoint(endpointId, info.getEndpointName());
                mDiscoveredEndpoints.put(endpointId, endpoint);

                NearbyPlugin.this.onEndpointFound(endpoint);
            }
        }

        @Override
        public void onEndpointLost(@NonNull String endpointId) {
            Log.d(getLogTag(), String.format("onEndpointLost(endpointId=%s)", endpointId));

            NearbyPlugin.this.onEndpointLost(endpointId);
        }
    };

    /**
     * Callbacks for connections to other devices.
     */
    private final ConnectionLifecycleCallback mConnectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(@NonNull String endpointId, ConnectionInfo connectionInfo) {
            Log.d(
                getLogTag(),
                String.format("onConnectionInitiated(endpointId=%s, endpointName=%s)", endpointId, connectionInfo.getEndpointName())
            );

            Endpoint endpoint = new Endpoint(endpointId, connectionInfo.getEndpointName());
            mPendingConnections.put(endpointId, endpoint);

            NearbyPlugin.this.onConnectionInitiated(endpoint, connectionInfo);
        }

        @Override
        public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution result) {
            Log.d(getLogTag(), String.format("onConnectionResponse(endpointId=%s, result=%s)", endpointId, result));

            // We're no longer connecting
            mIsConnecting = false;

            if (!result.getStatus().isSuccess()) {
                Log.w(getLogTag(), String.format("Connection failed. Received status %s.", NearbyPlugin.toString(result.getStatus())));

                onConnectionFailed(mPendingConnections.remove(endpointId));
                return;
            }

            connectedToEndpoint(mPendingConnections.remove(endpointId));
        }

        @Override
        public void onDisconnected(@NonNull String endpointId) {
            if (!mEstablishedConnections.containsKey(endpointId)) {
                Log.w(getLogTag(), "Unexpected disconnection from endpoint " + endpointId);
                return;
            }

            disconnectedFromEndpoint(mEstablishedConnections.get(endpointId));
        }
    };

    /**
     * Callback for payloads (bytes of data) sent from another device to us.
     */
    private final PayloadCallback mPayloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
            Log.d(getLogTag(), String.format("onPayloadReceived(endpointId=%s, payload=%s)", endpointId, payload));

            onReceive(mEstablishedConnections.get(endpointId), payload);
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {
            Log.d(getLogTag(), String.format("onPayloadTransferUpdate(endpointId=%s, update=%s)", endpointId, update));
        }
    };

    /**
     * Represents a device we can talk to.
     */
    protected static class Endpoint {

        @NonNull
        private final String id;

        @NonNull
        private final String name;

        private Endpoint(@NonNull String id, @NonNull String name) {
            this.id = id;
            this.name = name;
        }

        @NonNull
        public String getId() {
            return id;
        }

        @NonNull
        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Endpoint) {
                Endpoint other = (Endpoint) obj;
                return id.equals(other.id);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @NonNull
        @Override
        public String toString() {
            return String.format("Endpoint{id=%s, name=%s}", id, name);
        }
    }

    /**
     * Sends a connection request to the endpoint. Either {@link #onConnectionInitiated(Endpoint,
     * ConnectionInfo)} or {@link #onConnectionFailed(Endpoint)} will be called once we've found out
     * if we successfully reached the device.
     */
    protected void connectToEndpoint(final Endpoint endpoint) {
        Log.v(getLogTag(), "Sending a connection request to endpoint " + endpoint);
        // Mark ourselves as connecting so we don't connect multiple times
        mIsConnecting = true;

        // Ask to connect
        mConnectionsClient
            .requestConnection(getName(), endpoint.getId(), mConnectionLifecycleCallback)
            .addOnFailureListener(e -> {
                Log.w(getLogTag(), "requestConnection() failed.", e);
                mIsConnecting = false;

                onConnectionFailed(endpoint);
            });
    }

    private void connectedToEndpoint(Endpoint endpoint) {
        Log.d(getLogTag(), String.format("connectedToEndpoint(endpoint=%s)", endpoint));

        mEstablishedConnections.put(endpoint.getId(), endpoint);

        onEndpointConnected(endpoint);
    }

    private void disconnectedFromEndpoint(Endpoint endpoint) {
        Log.d(getLogTag(), String.format("disconnectedFromEndpoint(endpoint=%s)", endpoint));

        mEstablishedConnections.remove(endpoint.getId());

        onEndpointDisconnected(endpoint);
    }

    /**
     * Transforms a {@link Status} into a English-readable message for logging.
     *
     * @param status The current status
     * @return A readable String. eg. [404]File not found.
     */
    private static String toString(Status status) {
        return String.format(
            Locale.US,
            "[%d]%s",
            status.getStatusCode(),
            status.getStatusMessage() != null
                ? status.getStatusMessage()
                : ConnectionsStatusCodes.getStatusCodeString(status.getStatusCode())
        );
    }

    /**
     * Called when a pending connection with a remote endpoint is created. Use {@link ConnectionInfo}
     * for metadata about the connection (like incoming vs outgoing, or the authentication token). If
     * we want to continue with the connection, call {@link #acceptConnection(Endpoint)}. Otherwise,
     * call {@link #rejectConnection(Endpoint)}.
     */
    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {}

    /**
     * Called when a connection with this endpoint has failed. Override this method to act on the
     * event.
     */
    protected void onConnectionFailed(Endpoint endpoint) {}

    /**
     * Called when someone has connected to us. Override this method to act on the event.
     */
    protected void onEndpointConnected(Endpoint endpoint) {}

    /**
     * Called when someone has disconnected. Override this method to act on the event.
     */
    protected void onEndpointDisconnected(Endpoint endpoint) {}

    /**
     * Called when a remote endpoint is discovered. To connect to the device, call {@link
     * #connectToEndpoint(Endpoint)}.
     */
    protected void onEndpointFound(Endpoint endpoint) {}

    protected void onEndpointLost(String endpointId) {}

    /**
     * Returns a list of currently connected endpoints.
     */
    protected Set<Endpoint> getDiscoveredEndpoints() {
        return new HashSet<>(mDiscoveredEndpoints.values());
    }

    /**
     * Returns a list of currently connected endpoints.
     */
    protected Set<Endpoint> getConnectedEndpoints() {
        return new HashSet<>(mEstablishedConnections.values());
    }

    /**
     * Someone connected to us has sent us data. Override this method to act on the event.
     *
     * @param endpoint The sender.
     * @param payload  The data.
     */
    protected void onReceive(Endpoint endpoint, Payload payload) {}

    /**
     * Accepts a connection request.
     */
    protected void acceptConnection(final Endpoint endpoint) {
        mConnectionsClient
            .acceptConnection(endpoint.getId(), mPayloadCallback)
            .addOnFailureListener(e -> Log.w(getLogTag(), "acceptConnection() failed.", e));
    }

    /**
     * Rejects a connection request.
     */
    protected void rejectConnection(Endpoint endpoint) {
        mConnectionsClient
            .rejectConnection(endpoint.getId())
            .addOnFailureListener(e -> Log.w(getLogTag(), "rejectConnection() failed.", e));
    }
}
