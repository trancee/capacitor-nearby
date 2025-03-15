package com.getcapacitor.community;

import static com.getcapacitor.community.NearbyHelper.BLUETOOTH_BASE_UUID_LSB;
import static com.getcapacitor.community.NearbyHelper.BLUETOOTH_BASE_UUID_MSB;
import static com.getcapacitor.community.NearbyHelper.EndpointID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.AdvertiseSettings;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.getcapacitor.community.classes.Endpoint;
import com.getcapacitor.community.classes.options.AcceptConnectionOptions;
import com.getcapacitor.community.classes.options.CancelPayloadOptions;
import com.getcapacitor.community.classes.options.ConnectOptions;
import com.getcapacitor.community.classes.options.DisconnectOptions;
import com.getcapacitor.community.classes.options.InitializeOptions;
import com.getcapacitor.community.classes.options.RejectConnectionOptions;
import com.getcapacitor.community.classes.options.RequestConnectionOptions;
import com.getcapacitor.community.classes.options.SendPayloadOptions;
import com.getcapacitor.community.classes.options.StartAdvertisingOptions;
import com.getcapacitor.community.classes.results.InitializeResult;
import com.getcapacitor.community.classes.results.StatusResult;
import com.getcapacitor.community.interfaces.Callback;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Nearby {

    private static final String NOT_INITIALIZED = "not initialized";

    private static final String MISSING_ENDPOINT_ID = "missing endpoint identifier";
    private static final String MISSING_ENDPOINT_INFO = "missing endpoint information";
    private static final String MISSING_SERVICE_ID = "missing service identifier";

    private static final String MISSING_PAYLOAD_ID = "missing payload identifier";
    private static final String MISSING_PAYLOAD = "missing payload";

    @NonNull
    private final NearbyPlugin plugin;

    @NonNull
    private final NearbyConfig config;

    private boolean isAdvertising = false;
    private boolean isDiscovering = false;

    private BluetoothAdapter bluetoothAdapter;

    private NearbyScanner nearbyScanner;
    private NearbyAdvertiser nearbyAdvertiser;

    protected UUID serviceUUID;
    protected UUID serviceMask = UUID.fromString("ffffffff-0000-0000-0000-000000000000");

    public static Map<EndpointID, NearbyEndpoint> endpoints;

    public Nearby(@NonNull NearbyConfig config, @NonNull NearbyPlugin plugin) {
        this.config = config;
        this.plugin = plugin;

        endpoints = new HashMap<>();
    }

    /**
     * Initialize
     */
    public void initialize(@NonNull InitializeOptions options, @NonNull Callback callback) {
        if (config.getEndpointInfo() == null) {
            Exception exception = new Exception(MISSING_ENDPOINT_INFO);
            callback.error(exception);
            return;
        }

        if (config.getServiceID() == null) {
            Exception exception = new Exception(MISSING_SERVICE_ID);
            callback.error(exception);
            return;
        }

        {
            byte[] data = NearbyHelper.hash(config.getServiceID(), 8 + 8);

            //          0000-1000-8000-00805f9b34fb
            // ffffffff-0000-0000-0000-000000000000

            long msb =
                (((long) data[0] & 0xff) << 56) |
                (((long) data[1] & 0xff) << 48) | // 16-bits UUID
                (((long) data[2] & 0xff) << 40) |
                (((long) data[3] & 0xff) << 32); // 32-bits UUID
            long lsb = 0;

            this.serviceUUID = new UUID(BLUETOOTH_BASE_UUID_MSB | (msb & 0xffffffff), BLUETOOTH_BASE_UUID_LSB | (lsb & 0xffffffff));
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            callback.error(new Exception("no bluetooth adapter"));
            return;
        }

        nearbyScanner = NearbyScanner.getInstance(this.bluetoothAdapter, this.serviceUUID, this.serviceMask);

        // if (scanMode != null) {
        //     nearbyScanner.setScanMode(scanMode);
        // }

        nearbyAdvertiser = NearbyAdvertiser.getInstance(this.bluetoothAdapter, this.serviceUUID, config.getEndpointID());

        // if (advertiseMode != null) {
        //     nearbyAdvertiser.setAdvertiseMode(advertiseMode);
        // }
        // if (txPowerLevel != null) {
        //     nearbyAdvertiser.setTxPowerLevel(txPowerLevel);
        // }

        endpoints.clear();

        InitializeResult result = new InitializeResult(config.getEndpointID());
        callback.success(result);
    }

    /**
     * Reset
     */
    public void reset(@NonNull Callback callback) {
        stop();

        callback.success();
    }

    /**
     * Advertising
     */
    public void startAdvertising(@NonNull StartAdvertisingOptions options, @NonNull Callback callback) {
        if (bluetoothAdapter == null || nearbyAdvertiser == null) {
            Exception exception = new Exception(NOT_INITIALIZED);
            callback.error(exception);
        }

        String endpointInfo = options.getEndpointInfo();
        if (endpointInfo == null || endpointInfo.isEmpty()) {
            Exception exception = new Exception(MISSING_ENDPOINT_INFO);
            callback.error(exception);
            return;
        }

        String serviceID = config.getServiceID();
        if (serviceID == null || serviceID.isEmpty()) {
            Exception exception = new Exception(MISSING_SERVICE_ID);
            callback.error(exception);
            return;
        }

        nearbyAdvertiser.start(
            endpointInfo.getBytes(StandardCharsets.UTF_8),
            new NearbyAdvertiser.Callback() {
                @Override
                public void onSuccess(AdvertiseSettings settings) {
                    isAdvertising = true;

                    callback.success();
                }

                @Override
                public void onFailure(Exception exception) {
                    isAdvertising = false;

                    callback.error(exception);
                }
            }
        );
        //        connectionsClient
        //            .startAdvertising(name, serviceID, connectionLifecycleCallback, advertisingOptions.build())
        //            .addOnSuccessListener(unusedResult -> {
        //                isAdvertising = true;
        //
        //                callback.success();
        //            })
        //            .addOnFailureListener(exception -> {
        //                isAdvertising = false;
        //
        //                callback.error(exception);
        //            });
    }

    public void stopAdvertising(@NonNull Callback callback) {
        stopAdvertising();

        callback.success();
    }

    /**
     * Discovery
     */
    public void startDiscovering(@NonNull Callback callback) {
        if (bluetoothAdapter == null || nearbyScanner == null) {
            Exception exception = new Exception(NOT_INITIALIZED);
            callback.error(exception);
        }

        nearbyScanner.start(
            new NearbyScanner.Callback() {
                @Override
                public void onFound(EndpointID endpointID, @Nullable byte[] endpointInfo, Integer rssi, BluetoothDevice device) {
                    NearbyEndpoint nearbyEndpoint = endpoints.get(endpointID);
                    if (nearbyEndpoint != null) {
                        nearbyEndpoint.alive();
                    } else {
                        new NearbyEndpoint(endpointID, endpointInfo, rssi, device, () -> {
                            if (nearbyScanner.isScanning()) {
                                Endpoint endpoint = new Endpoint(endpointID, new String(endpointInfo));

                                plugin.onEndpointLost(endpoint);
                            }

                            NearbyEndpoint endpoint = endpoints.get(endpointID);
                            if (endpoint != null) {
                                endpoint.kill();
                            }
                        });
                        // endpoints.put(endpointID, nearbyEndpoint);

                        if (nearbyScanner.isScanning()) {
                            Endpoint endpoint = new Endpoint(endpointID);

                            plugin.onEndpointFound(endpoint);
                        }
                    }
                }

                @Override
                public void onLost(EndpointID endpointID) {
                    NearbyEndpoint nearbyEndpoint = endpoints.get(endpointID);
                    if (nearbyEndpoint != null) {
                        nearbyEndpoint.kill();
                    }
                    // endpoints.remove(endpointID);

                    {
                        Endpoint endpoint = new Endpoint(endpointID);

                        plugin.onEndpointLost(endpoint);
                    }
                }

                @Override
                public void onSuccess() {
                    isDiscovering = true;

                    callback.success();
                }

                @Override
                public void onFailure(Exception exception) {
                    isDiscovering = false;

                    callback.error(exception);
                }
            }
        );
        //        connectionsClient
        //            .startDiscovery(serviceID, endpointDiscoveryCallback, discoveryOptions.build())
        //            .addOnSuccessListener(unusedResult -> {
        //                isDiscovering = true;
        //
        //                callback.success();
        //            })
        //            .addOnFailureListener(exception -> {
        //                isDiscovering = false;
        //
        //                callback.error(exception);
        //            });
    }

    public void stopDiscovering(@NonNull Callback callback) {
        stopDiscovering();

        callback.success();
    }

    /**
     * Connection
     */
    public void connect(@NonNull ConnectOptions options, @NonNull Callback callback) {
        if (bluetoothAdapter == null) {
            Exception exception = new Exception(NOT_INITIALIZED);
            callback.error(exception);
        }

        String endpointID = options.getEndpointID();
        if (endpointID == null) {
            Exception exception = new Exception(MISSING_ENDPOINT_ID);
            callback.error(exception);
            return;
        }

        String endpointInfo = options.getEndpointInfo();
        if (endpointInfo == null || endpointInfo.isEmpty()) {
            Exception exception = new Exception(MISSING_ENDPOINT_INFO);
            callback.error(exception);
            return;
        }

        callback.success();
    }

    public void requestConnection(@NonNull RequestConnectionOptions options, @NonNull Callback callback) {
        String endpointID = options.getEndpointID();
        if (endpointID == null) {
            Exception exception = new Exception(MISSING_ENDPOINT_ID);
            callback.error(exception);
            return;
        }

        String endpointInfo = options.getEndpointInfo();
        if (endpointInfo == null || endpointInfo.isEmpty()) {
            Exception exception = new Exception(MISSING_ENDPOINT_INFO);
            callback.error(exception);
            return;
        }
        //        connectionsClient
        //            .requestConnection(name, endpointID, connectionLifecycleCallback, connectionOptions.build())
        //            .addOnSuccessListener(callback::success)
        //            .addOnFailureListener(callback::error);
    }

    public void acceptConnection(@NonNull AcceptConnectionOptions options, @NonNull Callback callback) {
        String endpointID = options.getEndpointID();
        if (endpointID == null) {
            Exception exception = new Exception(MISSING_ENDPOINT_ID);
            callback.error(exception);
            return;
        }
        //        connectionsClient
        //            .acceptConnection(endpointID, payloadCallback)
        //            .addOnSuccessListener(callback::success)
        //            .addOnFailureListener(callback::error);
    }

    public void rejectConnection(@NonNull RejectConnectionOptions options, @NonNull Callback callback) {
        String endpointID = options.getEndpointID();
        if (endpointID == null) {
            Exception exception = new Exception(MISSING_ENDPOINT_ID);
            callback.error(exception);
            return;
        }
        //        connectionsClient.rejectConnection(endpointID).addOnSuccessListener(callback::success).addOnFailureListener(callback::error);
    }

    public void disconnect(@NonNull DisconnectOptions options, @NonNull Callback callback) {
        String endpointID = options.getEndpointID();
        if (endpointID == null) {
            Exception exception = new Exception(MISSING_ENDPOINT_ID);
            callback.error(exception);
            return;
        }
        //        connectionsClient.disconnectFromEndpoint(endpointID);
    }

    /**
     * Payload
     */
    public void sendPayload(@NonNull SendPayloadOptions options, @NonNull Callback callback) {
        List<String> endpointIDs = options.getEndpointIDs();
        if (endpointIDs == null || endpointIDs.isEmpty()) {
            Exception exception = new Exception(MISSING_ENDPOINT_ID);
            callback.error(exception);
            return;
        }

        byte[] payload = options.getPayload();
        if (payload == null) {
            Exception exception = new Exception(MISSING_PAYLOAD);
            callback.error(exception);
            return;
        }
        //        connectionsClient
        //            .sendPayload(endpointIDs, Payload.fromBytes(payload))
        //            .addOnSuccessListener(callback::success)
        //            .addOnFailureListener(callback::error);
    }

    public void cancelPayload(@NonNull CancelPayloadOptions options, @NonNull Callback callback) {
        Long payloadID = options.getPayloadID();
        if (payloadID == null) {
            Exception exception = new Exception(MISSING_PAYLOAD_ID);
            callback.error(exception);
            return;
        }
        //        connectionsClient.cancelPayload(payloadID).addOnSuccessListener(callback::success).addOnFailureListener(callback::error);
    }

    /**
     * Status
     */
    public void status(@NonNull Callback callback) {
        StatusResult result = new StatusResult(isAdvertising, isDiscovering);

        callback.success(result);
    }

    /**
     * Stops advertising.
     */
    protected void stopAdvertising() {
        isAdvertising = false;

        nearbyAdvertiser.stop();
        // connectionsClient.stopAdvertising();
    }

    /**
     * Stops discovery.
     */
    protected void stopDiscovering() {
        isDiscovering = false;

        nearbyScanner.stop();
        // connectionsClient.stopDiscovery();

        // Make sure to clear all found beacons.
        for (NearbyEndpoint endpoint : endpoints.values()) {
            endpoint.kill();
        }
        endpoints.clear();
    }

    private void stop() {
        stopAdvertising();
        stopDiscovering();
        // connectionsClient.stopAllEndpoints();

        // isAdvertising = false;
        // isDiscovering = false;
    }
    /**
     * Callback for discovering endpoints.
     */

    //    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
    //        @Override
    //        public void onEndpointFound(@NonNull String endpointID, @NonNull DiscoveredEndpointInfo info) {
    //            String serviceID = config.serviceID();
    //
    //            if (serviceID != null && serviceID.equals(info.getServiceId())) {
    //                Endpoint endpoint = new Endpoint(endpointID, info.getEndpointName());
    //
    //                plugin.onEndpointFound(endpoint);
    //            }
    //        }
    //
    //        @Override
    //        public void onEndpointLost(@NonNull String endpointID) {
    //            Endpoint endpoint = new Endpoint(endpointID, null);
    //
    //            plugin.onEndpointLost(endpoint);
    //        }
    //    };

    //    /**
    //     * Listener for lifecycle events associated with a connection to a remote endpoint.
    //     */
    //    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
    //        /**
    //         * A basic encrypted channel has been created between you and the endpoint.
    //         * Both sides are now asked if they wish to accept or reject the connection before any data can be sent over this channel.
    //         *
    //         * @param endpointID The identifier for the remote endpoint.
    //         * @param connectionInfo Other relevant information about the connection.
    //         */
    //        @Override
    //        public void onConnectionInitiated(@NonNull String endpointID, @NonNull ConnectionInfo connectionInfo) {
    //            Endpoint endpoint = new Endpoint(endpointID, connectionInfo.getEndpointName());
    //
    //            plugin.onEndpointInitiated(endpoint);
    //        }
    //
    //        /**
    //         * Called after both sides have either accepted or rejected the connection.
    //         *
    //         * @param endpointID The identifier for the remote endpoint.
    //         * @param resolution The final result after tallying both devices' accept/reject responses.
    //         */
    //        @Override
    //        public void onConnectionResult(@NonNull String endpointID, @NonNull ConnectionResolution resolution) {
    //            Endpoint endpoint = new Endpoint(endpointID, null);
    //            Status status = resolution.getStatus();
    //
    //            if (status.isSuccess()) {
    //                acceptedEndpoint(endpoint);
    //            } else if (status.getStatusCode() == ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED) {
    //                rejectedEndpoint(endpoint);
    //            } else {
    //                failedEndpoint(endpoint, ConnectionsStatusCodes.getStatusCodeString(status.getStatusCode()));
    //            }
    //        }
    //
    //        /**
    //         * Called when a remote endpoint is disconnected or has become unreachable.
    //         *
    //         * @param endpointID The identifier for the remote endpoint that disconnected.
    //         */
    //        @Override
    //        public void onDisconnected(@NonNull String endpointID) {
    //            Endpoint endpoint = new Endpoint(endpointID, null);
    //
    //            disconnectedEndpoint(endpoint);
    //        }
    //    };
    //
    //    private void acceptedEndpoint(@NonNull Endpoint endpoint) {
    //        plugin.onEndpointConnected(endpoint);
    //    }
    //
    //    private void rejectedEndpoint(@NonNull Endpoint endpoint) {
    //        plugin.onEndpointRejected(endpoint);
    //    }
    //
    //    private void failedEndpoint(@NonNull Endpoint endpoint, @NonNull String status) {
    //        plugin.onEndpointFailed(endpoint, status);
    //    }
    //
    //    private void disconnectedEndpoint(@NonNull Endpoint endpoint) {
    //        plugin.onEndpointDisconnected(endpoint);
    //    }

    //    /**
    //     * Callbacks for payloads (bytes of data) sent from another device to us.
    //     */
    //    private final PayloadCallback payloadCallback = new PayloadCallback() {
    //        @Override
    //        public void onPayloadReceived(@NonNull String endpointID, @NonNull Payload payload) {
    //            Endpoint endpoint = new Endpoint(endpointID, null);
    //
    //            plugin.onPayloadReceived(
    //                endpoint,
    //                new com.getcapacitor.community.classes.Payload(payload.getId(), payload.getType(), payload.asBytes())
    //            );
    //        }
    //
    //        @Override
    //        public void onPayloadTransferUpdate(@NonNull String endpointID, @NonNull PayloadTransferUpdate update) {
    //            Endpoint endpoint = new Endpoint(endpointID, null);
    //
    //            plugin.onPayloadTransferUpdate(
    //                endpoint,
    //                new com.getcapacitor.community.classes.PayloadTransferUpdate(
    //                    update.getPayloadId(),
    //                    update.getStatus(),
    //                    update.getBytesTransferred(),
    //                    update.getTotalBytes()
    //                )
    //            );
    //        }
    //    };
}
