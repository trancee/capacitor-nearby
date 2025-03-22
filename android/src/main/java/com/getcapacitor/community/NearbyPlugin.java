package com.getcapacitor.community;

import android.Manifest;
import android.os.Build;
import android.util.Base64;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import com.getcapacitor.community.classes.Endpoint;
import com.getcapacitor.community.classes.events.EndpointConnectedEvent;
import com.getcapacitor.community.classes.events.EndpointDisconnectedEvent;
import com.getcapacitor.community.classes.events.EndpointFailedEvent;
import com.getcapacitor.community.classes.events.EndpointFoundEvent;
import com.getcapacitor.community.classes.events.EndpointInitiatedEvent;
import com.getcapacitor.community.classes.events.EndpointLostEvent;
import com.getcapacitor.community.classes.events.EndpointRejectedEvent;
import com.getcapacitor.community.classes.events.PayloadReceivedEvent;
import com.getcapacitor.community.classes.options.AcceptConnectionOptions;
import com.getcapacitor.community.classes.options.ConnectOptions;
import com.getcapacitor.community.classes.options.DisconnectOptions;
import com.getcapacitor.community.classes.options.InitializeOptions;
import com.getcapacitor.community.classes.options.RejectConnectionOptions;
import com.getcapacitor.community.classes.options.RequestConnectionOptions;
import com.getcapacitor.community.classes.options.SendPayloadOptions;
import com.getcapacitor.community.classes.options.StartAdvertisingOptions;
import com.getcapacitor.community.interfaces.Callback;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.JSONException;

@CapacitorPlugin(
    name = "Nearby",
    permissions = {
        @Permission(
            strings = {
                // Required to be able to connect to paired Bluetooth devices.
                Manifest.permission.BLUETOOTH_CONNECT,
                // Required to be able to advertise to nearby Bluetooth devices.
                Manifest.permission.BLUETOOTH_ADVERTISE,
                // Required to be able to discover and pair nearby Bluetooth devices.
                Manifest.permission.BLUETOOTH_SCAN
            },
            alias = "bluetoothNearby"
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

    static final String ENDPOINT_FOUND_EVENT = "onEndpointFound";
    static final String ENDPOINT_LOST_EVENT = "onEndpointLost";
    static final String ENDPOINT_INITIATED_EVENT = "onEndpointInitiated";
    static final String ENDPOINT_CONNECTED_EVENT = "onEndpointConnected";
    static final String ENDPOINT_REJECTED_EVENT = "onEndpointRejected";
    static final String ENDPOINT_FAILED_EVENT = "onEndpointFailed";
    static final String ENDPOINT_DISCONNECTED_EVENT = "onEndpointDisconnected";
    static final String PAYLOAD_RECEIVED_EVENT = "onPayloadReceived";

    private Nearby implementation;

    private NearbyConfig config;

    @Override
    public void load() {
        super.load();

        config = getNearbyConfig();
        implementation = new Nearby(config, this);
    }

    /**
     * Initialize
     */

    @PluginMethod
    public void initialize(PluginCall call) {
        Callback callback = new Callback(call) {};

        try {
            InitializeOptions options = new InitializeOptions(call, config);

            implementation.initialize(options, callback);
        } catch (Exception exception) {
            callback.error(exception);
        }
    }

    /**
     * Reset
     */

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @PluginMethod
    public void reset(PluginCall call) {
        Callback callback = new Callback(call) {};

        try {
            implementation.reset(callback);
        } catch (Exception exception) {
            callback.error(exception);
        }
    }

    /**
     * Advertising
     */

    @PluginMethod
    public void startAdvertising(PluginCall call) {
        Callback callback = new Callback(call) {};

        try {
            StartAdvertisingOptions options = new StartAdvertisingOptions(call, config);

            implementation.startAdvertising(options, callback);
        } catch (Exception exception) {
            callback.error(exception);
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @PluginMethod
    public void stopAdvertising(PluginCall call) {
        Callback callback = new Callback(call) {};

        try {
            implementation.stopAdvertising(callback);
        } catch (Exception exception) {
            callback.error(exception);
        }
    }

    /**
     * Discovery
     */

    @PluginMethod
    public void startDiscovering(PluginCall call) {
        Callback callback = new Callback(call) {};

        try {
            implementation.startDiscovering(callback);
        } catch (Exception exception) {
            callback.error(exception);
        }
    }

    @PluginMethod
    public void stopDiscovering(PluginCall call) {
        Callback callback = new Callback(call) {};

        try {
            implementation.stopDiscovering(callback);
        } catch (Exception exception) {
            callback.error(exception);
        }
    }

    /**
     * Connection
     */

    @PluginMethod
    public void requestConnection(PluginCall call) {
        Callback callback = new Callback(call) {};

        try {
            RequestConnectionOptions options = new RequestConnectionOptions(call, config);

            implementation.requestConnection(options, callback);
        } catch (Exception exception) {
            callback.error(exception);
        }
    }

    @PluginMethod
    public void acceptConnection(PluginCall call) {
        Callback callback = new Callback(call) {};

        try {
            AcceptConnectionOptions options = new AcceptConnectionOptions(call);

            implementation.acceptConnection(options, callback);
        } catch (Exception exception) {
            callback.error(exception);
        }
    }

    @PluginMethod
    public void rejectConnection(PluginCall call) {
        Callback callback = new Callback(call) {};

        try {
            RejectConnectionOptions options = new RejectConnectionOptions(call);

            implementation.rejectConnection(options, callback);
        } catch (Exception exception) {
            callback.error(exception);
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @PluginMethod
    public void connect(PluginCall call) {
        Callback callback = new Callback(call) {};

        try {
            ConnectOptions options = new ConnectOptions(call);

            implementation.connect(options, callback);
        } catch (Exception exception) {
            callback.error(exception);
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @PluginMethod
    public void disconnect(PluginCall call) {
        Callback callback = new Callback(call) {};

        try {
            DisconnectOptions options = new DisconnectOptions(call);

            implementation.disconnect(options, callback);
        } catch (Exception exception) {
            callback.error(exception);
        }
    }

    /**
     * Payload
     */

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @PluginMethod
    public void sendPayload(PluginCall call) {
        Callback callback = new Callback(call) {};

        try {
            SendPayloadOptions options = new SendPayloadOptions(call);

            implementation.sendPayload(options, callback);
        } catch (Exception exception) {
            callback.error(exception);
        }
    }

    /**
     * Status
     */

    @PluginMethod
    public void status(PluginCall call) {
        Callback callback = new Callback(call) {};

        try {
            implementation.status(callback);
        } catch (Exception exception) {
            callback.error(exception);
        }
    }

    /**
     * Permissions
     */

    @Override
    @PluginMethod
    public void checkPermissions(PluginCall call) {
        // super.checkPermissions(call);

        Map<String, PermissionState> permissionsResult = getPermissionStates();

        if (permissionsResult.isEmpty()) {
            call.resolve();
        } else {
            List<String> aliases = getAliases();

            JSObject result = new JSObject();

            for (Map.Entry<String, PermissionState> entry : permissionsResult.entrySet()) {
                if (aliases.contains(entry.getKey())) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }

            call.resolve(result);
        }
    }

    private List<String> getAliases() {
        List<String> aliases = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            aliases.add("bluetoothNearby");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            aliases.add("bluetoothNearby");
            aliases.add("location");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            aliases.add("bluetoothLegacy");
            aliases.add("location");
        } else {
            aliases.add("bluetoothLegacy");
            aliases.add("locationCoarse");
        }

        return aliases;
    }

    @Override
    @PluginMethod
    public void requestPermissions(PluginCall call) {
        List<String> aliases = getAliases();

        JSArray permissions = call.getArray("permissions");
        if (permissions != null) {
            try {
                List<String> permissionsList = permissions.toList();
                for (String permission : permissionsList) {
                    switch (permission) {
                        case "bluetooth":
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                aliases.add("bluetoothNearby");
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                aliases.add("bluetoothNearby");
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                aliases.add("bluetoothLegacy");
                            } else {
                                aliases.add("bluetoothLegacy");
                            }
                            break;
                        case "location":
                            //noinspection StatementWithEmptyBody
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                // does not require location permission
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                aliases.add("location");
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                aliases.add("location");
                            } else {
                                aliases.add("locationCoarse");
                            }
                            break;
                    }
                }
            } catch (JSONException ignored) {}
        }

        requestPermissionForAliases(aliases.toArray(new String[0]), call, "permissionsCallback");
    }

    @PermissionCallback
    private void permissionsCallback(PluginCall call) {
        this.checkPermissions(call);
    }

    /**
     * Configuration
     */

    private NearbyConfig getNearbyConfig() {
        @Nullable
        String endpointName = getConfig().getString("endpointName");
        @Nullable
        byte[] endpointInfo = null;

        @Nullable
        String value = getConfig().getString("endpointInfo");
        if (value != null && !value.isEmpty()) {
            try {
                endpointInfo = Base64.decode(value, Base64.NO_WRAP);
            } catch (IllegalArgumentException ignored) {
                endpointInfo = value.getBytes();
            }
        }

        @Nullable
        String serviceID = getConfig().getString("serviceID");

        return new NearbyConfig(endpointName, endpointInfo, serviceID);
    }

    /**
     * Called when a remote endpoint is discovered.
     */
    protected void onEndpointFound(Endpoint endpoint) {
        EndpointFoundEvent event = new EndpointFoundEvent(endpoint);

        notifyListeners(ENDPOINT_FOUND_EVENT, event.toJSObject());
    }

    /**
     * Called when a remote endpoint is no longer discoverable.
     */
    protected void onEndpointLost(Endpoint endpoint) {
        EndpointLostEvent event = new EndpointLostEvent(endpoint);

        notifyListeners(ENDPOINT_LOST_EVENT, event.toJSObject());
    }

    /**
     * A basic encrypted channel has been created between you and the endpoint.
     */
    protected void onEndpointInitiated(Endpoint endpoint) {
        EndpointInitiatedEvent event = new EndpointInitiatedEvent(endpoint);

        notifyListeners(ENDPOINT_INITIATED_EVENT, event.toJSObject());
    }

    /**
     * Called after both sides have accepted the connection.
     */
    protected void onEndpointConnected(Endpoint endpoint) {
        EndpointConnectedEvent event = new EndpointConnectedEvent(endpoint);

        notifyListeners(ENDPOINT_CONNECTED_EVENT, event.toJSObject());
    }

    /**
     * Called after one side has rejected the connection.
     */
    protected void onEndpointRejected(Endpoint endpoint) {
        EndpointRejectedEvent event = new EndpointRejectedEvent(endpoint);

        notifyListeners(ENDPOINT_REJECTED_EVENT, event.toJSObject());
    }

    /**
     * Called after the connection has failed.
     */
    protected void onEndpointFailed(Endpoint endpoint) {
        EndpointFailedEvent event = new EndpointFailedEvent(endpoint);

        notifyListeners(ENDPOINT_FAILED_EVENT, event.toJSObject());
    }

    /**
     * Called when a remote endpoint is disconnected or has become unreachable.
     */
    protected void onEndpointDisconnected(Endpoint endpoint) {
        EndpointDisconnectedEvent event = new EndpointDisconnectedEvent(endpoint);

        notifyListeners(ENDPOINT_DISCONNECTED_EVENT, event.toJSObject());
    }

    /**
     * Called when a Payload is received from a remote endpoint.
     */
    protected void onPayloadReceived(Endpoint endpoint, byte[] payload) {
        PayloadReceivedEvent event = new PayloadReceivedEvent(endpoint, payload);

        notifyListeners(PAYLOAD_RECEIVED_EVENT, event.toJSObject());
    }
}
