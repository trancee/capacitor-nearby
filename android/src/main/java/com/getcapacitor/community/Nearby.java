package com.getcapacitor.community;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.community.classes.options.PublishOptions;
import com.getcapacitor.community.classes.results.StatusResult;
import com.getcapacitor.community.interfaces.EmptyCallback;
import com.getcapacitor.community.interfaces.NonEmptyCallback;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Nearby {

    interface EndpointListener {
        void onEndpointFound(String id, String name);

        void onEndpointLost(String id);
    }

    @Nullable
    private EndpointListener endpointListener;

    public void setEndpointListener(@Nullable EndpointListener listener) {
        this.endpointListener = listener;
    }

    @Nullable
    public EndpointListener getEndpointListener() {
        return endpointListener;
    }

    @NonNull
    private final NearbyPlugin plugin;

    @NonNull
    private final ConnectionsClient connectionsClient;

    private String name;

    /**
     * True if we are asking a discovered device to connect to us. While we ask, we cannot ask another
     * device.
     */
    private boolean isConnecting = false;

    /**
     * True if we are discovering.
     */
    private boolean isDiscovering = false;

    /**
     * True if we are advertising.
     */
    private boolean isAdvertising = false;

    /**
     * The devices we've discovered near us.
     */
    private final Map<String, NearbyPlugin.Endpoint> discoveredEndpoints = new HashMap<>();

    /**
     * The devices we have pending connections to. They will stay pending until we call {@link
     * #acceptConnection(NearbyPlugin.Endpoint)} or {@link #rejectConnection(NearbyPlugin.Endpoint)}.
     */
    private final Map<String, NearbyPlugin.Endpoint> pendingConnections = new HashMap<>();

    /**
     * The devices we are currently connected to. For advertisers, this may be large. For discoverers,
     * there will only be one entry in this map.
     */
    private final Map<String, NearbyPlugin.Endpoint> establishedConnections = new HashMap<>();

    public Nearby(@NonNull NearbyPlugin plugin) {
        this.plugin = plugin;

        connectionsClient = com.google.android.gms.nearby.Nearby.getConnectionsClient(plugin.getActivity());
    }

    public void reset() {
        stop();

        name = null;
    }

    /**
     * Publish
     */

    public void publish(@NonNull PublishOptions options, @NonNull EmptyCallback callback) {
        name = options.getName();

        if (isAdvertising()) {
            stopAdvertising();
        }

        AdvertisingOptions.Builder advertisingOptions = new AdvertisingOptions.Builder();
        advertisingOptions.setStrategy(getStrategy());

        connectionsClient
                .startAdvertising(name, getServiceId(), connectionLifecycleCallback, advertisingOptions.build())
                .addOnSuccessListener(unusedResult -> {
                    Log.v(getLogTag(), "Now advertising endpoint " + getName());
                    onAdvertisingStarted();

                    callback.success();
                })
                .addOnFailureListener(exception -> {
                    isAdvertising = false;

                    Log.w(getLogTag(), "startAdvertising failed.", e);
                    onAdvertisingFailed();

                    callback.error(exception);
                });
    }

    public void unpublish(@NonNull EmptyCallback callback) {
        stopAdvertising();

        name = null;
    }

    /**
     * Subscribe
     */

    public void subscribe(@NonNull EmptyCallback callback) {
        if (isDiscovering()) {
            stopDiscovering();
        }

        discoveredEndpoints.clear();

        DiscoveryOptions.Builder discoveryOptions = new DiscoveryOptions.Builder();
        discoveryOptions.setStrategy(getStrategy());

        connectionsClient
                .startDiscovery(getServiceId(), endpointDiscoveryCallback, discoveryOptions.build())
                .addOnSuccessListener(unusedResult -> {
                    Log.v(getLogTag(), "Now starting discovery");
                    onDiscoveryStarted();

                    callback.success();
                })
                .addOnFailureListener(e -> {
                    isDiscovering = false;

                    Log.w(getLogTag(), "startDiscovering failed.", e);
                    onDiscoveryFailed();

                    callback.error(exception);
                });
    }

    public void unsubscribe(@NonNull EmptyCallback callback) {
        stopDiscovering();
    }

    /**
     * Status
     */

    public void status(@NonNull NonEmptyCallback callback) {
        boolean isPublishing = isAdvertising();
        boolean isSubscribing = isDiscovering();

        String[] uuids = establishedConnections.keySet().toArray(new String[0]);

        StatusResult result = new StatusResult(isPublishing, isSubscribing, uuids);

        callback.success(result);
    }

    /**
     * Helper
     */

    /**
     * Stops advertising.
     */
    protected void stopAdvertising() {
        isAdvertising = false;

        connectionsClient.stopAdvertising();
    }

    /**
     * Stops discovery.
     */
    protected void stopDiscovering() {
        isDiscovering = false;

        connectionsClient.stopDiscovery();
    }

    /**
     * Returns {@code true} if currently advertising.
     */
    public boolean isAdvertising() {
        return isAdvertising;
    }

    /**
     * Returns {@code true} if currently discovering.
     */
    protected boolean isDiscovering() {
        return isDiscovering;
    }

    /**
     * Callback for discovering endpoints.
     */
    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo info) {
            if (getServiceId().equals(info.getServiceId())) {
                NearbyPlugin.Endpoint endpoint = new NearbyPlugin.Endpoint(endpointId, info.getEndpointName());
                discoveredEndpoints.put(endpointId, endpoint);

                plugin.onEndpointFound(endpointId, info);
            }
        }

        @Override
        public void onEndpointLost(@NonNull String endpointId) {
            plugin.onEndpointLost(endpointId);
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

            NearbyPlugin.Endpoint endpoint = new NearbyPlugin.Endpoint(endpointId, connectionInfo.getEndpointName());
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

    private void stop() {
        connectionsClient.stopAllEndpoints();

        isAdvertising = false;
        isDiscovering = false;
        isConnecting = false;

        discoveredEndpoints.clear();
        pendingConnections.clear();
        establishedConnections.clear();
    }

    /**
     * Listeners
     */

    /**
     * Called when advertising successfully starts. Override this method to act on the event.
     */
    protected void onAdvertisingStarted() {
    }

    /**
     * Called when advertising fails to start. Override this method to act on the event.
     */
    protected void onAdvertisingFailed() {
    }

    /**
     * Called when discovery successfully starts. Override this method to act on the event.
     */
    protected void onDiscoveryStarted() {
    }

    /**
     * Called when discovery fails to start. Override this method to act on the event.
     */
    protected void onDiscoveryFailed() {
    }

}
