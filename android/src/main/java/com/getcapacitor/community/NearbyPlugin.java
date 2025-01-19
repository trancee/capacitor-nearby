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
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Logger;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import com.getcapacitor.community.classes.options.PublishOptions;
import com.getcapacitor.community.interfaces.EmptyCallback;
import com.getcapacitor.community.interfaces.NonEmptyCallback;
import com.getcapacitor.community.interfaces.Result;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.util.Strings;
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
    String ENDPOINT_FOUND_EVENT = "onFound";
    String ENDPOINT_LOST_EVENT = "onLost";

    String BLUETOOTH_LE_NOT_SUPPORTED = "Bluetooth Low Energy not supported";
    String BLUETOOTH_NOT_AVAILABLE = "Bluetooth not available";
    String NOT_INITIALIZED = "not initialized";
    String PERMISSION_DENIED = "permission denied";

    String MISSING_STRATEGY = "missing strategy";
    String UNKNOWN_STRATEGY = "unknown strategy";

    String ERROR_NAME_MISSING = "missing name";

    String UNKNOWN_ERROR = "unknown error has occurred";

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
public class NearbyPlugin extends Plugin {

    @Nullable
    private Nearby implementation;

    private final List<String> aliases = new ArrayList<String>();

    private Strategy strategy;
    private String name;
    private String serviceId;

    @Override
    public void load() {
        try {
            implementation = new Nearby(this);
            implementation.setEndpointListener(this::endpoint);
        } catch (Exception exception) {
            Log.e(getLogTag(), exception.getMessage(), exception);
        }
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
        assert implementation != null;

        try {
            implementation.reset();

            resolveCall(call);
        } catch (Exception exception) {
            rejectCall(call, exception);
        }
    }

    /**
     * Publish
     */

    @PluginMethod
    public void publish(PluginCall call) {
        assert implementation != null;

        try {
            name = call.getString("name");
            if (name == null || name.isEmpty() || name.isBlank()) {
                call.reject(Constants.ERROR_NAME_MISSING);
                return;
            }

            PublishOptions options = new PublishOptions(name);
            EmptyCallback callback = new EmptyCallback() {
                @Override
                public void success() {
                    resolveCall(call);
                }

                @Override
                public void error(Exception exception) {
                    rejectCall(call, exception);
                }
            };

            implementation.publish(options, callback);
        } catch (Exception exception) {
            rejectCall(call, exception);
        }
    }

    @PluginMethod
    public void unpublish(PluginCall call) {
        assert implementation != null;

        try {
            EmptyCallback callback = new EmptyCallback() {
                @Override
                public void success() {
                    resolveCall(call);
                }

                @Override
                public void error(Exception exception) {
                    rejectCall(call, exception);
                }
            };

            implementation.unpublish(callback);
        } catch (Exception exception) {
            rejectCall(call, exception);
        }
    }

    /**
     * Subscribe
     */

    @PluginMethod
    public void subscribe(PluginCall call) {
        assert implementation != null;

        try {
            EmptyCallback callback = new EmptyCallback() {
                @Override
                public void success() {
                    resolveCall(call);
                }

                @Override
                public void error(Exception exception) {
                    rejectCall(call, exception);
                }
            };

            implementation.subscribe(callback);
        } catch (Exception exception) {
            rejectCall(call, exception);
        }
    }

    @PluginMethod
    public void unsubscribe(PluginCall call) {
        assert implementation != null;

        try {
            EmptyCallback callback = new EmptyCallback() {
                @Override
                public void success() {
                    resolveCall(call);
                }

                @Override
                public void error(Exception exception) {
                    rejectCall(call, exception);
                }
            };

            implementation.unsubscribe(callback);
        } catch (Exception exception) {
            rejectCall(call, exception);
        }
    }

    /**
     * Status
     */

    @PluginMethod
    public void status(PluginCall call) {
        assert implementation != null;

        try {
            NonEmptyCallback<Result> callback = new NonEmptyCallback<>() {
                @Override
                public void success(@NonNull Result result) {
                    resolveCall(call, result.toJSObject());
                }

                @Override
                public void error(Exception exception) {
                    rejectCall(call, exception);
                }
            };

            implementation.status(callback);
        } catch (Exception exception) {
            rejectCall(call, exception);
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
    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {
    }

    /**
     * Called when a connection with this endpoint has failed. Override this method to act on the
     * event.
     */
    protected void onConnectionFailed(Endpoint endpoint) {
    }

    /**
     * Called when someone has connected to us. Override this method to act on the event.
     */
    protected void onEndpointConnected(Endpoint endpoint) {
    }

    /**
     * Called when someone has disconnected. Override this method to act on the event.
     */
    protected void onEndpointDisconnected(Endpoint endpoint) {
    }

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
    protected void onReceive(Endpoint endpoint, Payload payload) {
    }

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

    private void resolveCall(@NonNull PluginCall call, @Nullable JSObject result) {
        if (result == null) {
            resolveCall(call);
        } else {
            call.resolve(result);
        }
    }

    private void resolveCall(@NonNull PluginCall call) {
        call.resolve();
    }

    private void rejectCall(@NonNull PluginCall call, @NonNull Exception exception) {
        String message = exception.getMessage();
        if (message == null) {
            message = Constants.UNKNOWN_ERROR;
        }
        Log.e(getLogTag(), message, exception);
        call.reject(message, exception);
    }

    /**
     * Called when a remote endpoint is discovered.
     */
    protected void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
        Log.d(
                getLogTag(),
                String.format(
                        "onEndpointFound(endpointId=%s, serviceId=%s, endpointName=%s)",
                        endpointId,
                        info.getServiceId(),
                        info.getEndpointName()
                )
        );

        JSObject jsData = new JSObject()
                // The ID of the remote endpoint that was discovered.
                .put("id", endpointId)
                // The human readable name of the remote endpoint.
                .put("name", info.getEndpointName());

        notifyListeners(Constants.ENDPOINT_FOUND_EVENT, jsData);
    }


    /**
     * Called when a remote endpoint is no longer discoverable.
     */
    protected void onEndpointLost(String endpointId) {
        Log.d(
                getLogTag(),
                String.format(
                        "onEndpointLost(endpointId=%s)",
                        endpointId
                )
        );

        JSObject jsData = new JSObject()
                // The ID of the remote endpoint that was lost.
                .put("id", endpointId);

        notifyListeners(Constants.ENDPOINT_LOST_EVENT, jsData);
    }

}
