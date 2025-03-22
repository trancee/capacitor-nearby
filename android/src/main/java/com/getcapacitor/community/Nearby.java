package com.getcapacitor.community;

import static com.getcapacitor.community.NearbyHelper.BLUETOOTH_BASE_UUID_LSB;
import static com.getcapacitor.community.NearbyHelper.BLUETOOTH_BASE_UUID_MSB;
import static com.getcapacitor.community.NearbyHelper.EndpointID;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import com.getcapacitor.community.classes.Endpoint;
import com.getcapacitor.community.classes.options.AcceptConnectionOptions;
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

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Nearby {

    private static final String NOT_INITIALIZED = "not initialized";
    private static final String NOT_CONNECTED = "not connected";

    static final String MISSING_ENDPOINT_ID = "missing endpoint identifier";
    private static final String MISSING_ENDPOINT_NAME = "missing endpoint name";
    private static final String MISSING_ENDPOINT_INFO = "missing endpoint information";
    private static final String MISSING_ENDPOINT = "missing endpoint";

    private static final String MISSING_SERVICE_ID = "missing service identifier";

    private static final String MISSING_PAYLOAD_ID = "missing payload identifier";
    private static final String MISSING_PAYLOAD = "missing payload";

    @NonNull
    private final NearbyPlugin plugin;

    @NonNull
    private final NearbyConfig config;

    private final Context context;

    private boolean isAdvertising = false;
    private boolean isDiscovering = false;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGattServer bluetoothGattServer;

    private NearbyScanner nearbyScanner;
    private NearbyAdvertiser nearbyAdvertiser;

    protected UUID serviceUUID;
    protected UUID serviceMask = UUID.fromString("ffffffff-0000-0000-0000-000000000000");

    private static final UUID SERVICE_UUID = UUID.fromString("57494e4b-0000-1000-8000-0805f9b34fb");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("57494e4b-0000-1000-8000-0805f9b34fb");
    private static final UUID PING_CHARACTERISTIC_UUID = UUID.fromString("50494e47-0000-1000-8000-0805f9b34fb");
    private static final UUID PONG_CHARACTERISTIC_UUID = UUID.fromString("504f4e47-0000-1000-8000-0805f9b34fb");

    public static Map<String, NearbyEndpoint> endpoints;
    public static Map<String, NearbyEndpoint> connections;

    public Nearby(@NonNull NearbyConfig config, @NonNull NearbyPlugin plugin) {
        this.config = config;
        this.plugin = plugin;

        this.context = plugin.getContext();

        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        endpoints = new HashMap<>();
        connections = new HashMap<>();
    }

    /**
     * Initialize
     */
    public void initialize(@NonNull InitializeOptions options, @NonNull Callback callback) {
        if (config.getEndpointName() == null) {
            Exception exception = new Exception(MISSING_ENDPOINT_NAME);
            callback.error(exception);
            return;
        }

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

        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                Exception exception = new Exception(NOT_INITIALIZED);
                callback.error(exception);
                return;
            }
        }

        if (bluetoothAdapter == null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter == null) {
                Exception exception = new Exception(MISSING_SERVICE_ID);
                callback.error(exception);
                return;
            }
        }

        //        // Ensure the device supports BLE and is enabled
        //        if (!bluetoothAdapter.isEnabled()) {
        //            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        //            plugin.startActivityForResult(call, enableBtIntent, 1);
        //        }

        {
            byte[] data = NearbyHelper.hash(config.getServiceID().getBytes(), 4);

            //          0000-1000-8000-00805f9b34fb
            // ffffffff-0000-0000-0000-000000000000

            ByteBuffer byteBuffer = ByteBuffer.allocate(16).putLong(BLUETOOTH_BASE_UUID_MSB).putLong(BLUETOOTH_BASE_UUID_LSB);

            byteBuffer.rewind();
            byteBuffer.put(data);

            long msb = byteBuffer.getLong(0);
            long lsb = byteBuffer.getLong(8);

            this.serviceUUID = new UUID(msb, lsb);
        }

        @NonNull
        String endpointID = config.getEndpointID();
        @Nullable
        UUID endpointUUID = EndpointID.toUUID(endpointID);

        @Nullable
        String endpointName = config.getEndpointName();

        //        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //        if (bluetoothAdapter == null) {
        //            callback.error(new Exception("no bluetooth adapter"));
        //            return;
        //        }

        nearbyScanner = NearbyScanner.getInstance(this.bluetoothAdapter, this.serviceUUID, this.serviceMask);

        // if (scanMode != null) {
        //     nearbyScanner.setScanMode(scanMode);
        // }

        nearbyAdvertiser = NearbyAdvertiser.getInstance(this.bluetoothAdapter, this.serviceUUID, endpointName, endpointUUID);

        // if (advertiseMode != null) {
        //     nearbyAdvertiser.setAdvertiseMode(advertiseMode);
        // }
        // if (txPowerLevel != null) {
        //     nearbyAdvertiser.setTxPowerLevel(txPowerLevel);
        // }

        endpoints.clear();

        InitializeResult result = new InitializeResult(endpointID);
        callback.success(result);
    }

    /**
     * Reset
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
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

        @Nullable
        byte[] endpointInfo = options.getEndpointInfo();
        //        if (endpointInfo == null || endpointInfo.length == 0) {
        //            Exception exception = new Exception(MISSING_ENDPOINT_INFO);
        //            callback.error(exception);
        //            return;
        //        }

        String serviceID = config.getServiceID();
        if (serviceID == null || serviceID.isEmpty()) {
            Exception exception = new Exception(MISSING_SERVICE_ID);
            callback.error(exception);
            return;
        }

        nearbyAdvertiser.start(
                endpointInfo,
                new NearbyAdvertiser.Callback() {
                    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    @Override
                    public void onSuccess(AdvertiseSettings settings) {
                        // startGattServer();

                        isAdvertising = true;

                        callback.success();
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        isAdvertising = false;

                        callback.error(exception);
                    }

                    @Override
                    public void onConnected(String endpointID) {
                        Endpoint endpoint = new Endpoint(endpointID);

                        plugin.onEndpointConnected(endpoint);
                    }

                    @Override
                    public void onDisconnected(String endpointID) {
                        Endpoint endpoint = new Endpoint(endpointID);

                        plugin.onEndpointDisconnected(endpoint);
                    }

                    @Override
                    public void onPayload(@NonNull String endpointID, byte[] payload) {
                        Endpoint endpoint = new Endpoint(endpointID);

                        plugin.onPayloadReceived(endpoint, payload);
                    }
                }
        );
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void stopAdvertising(@NonNull Callback callback) {
        stopAdvertising();

        callback.success();
    }

    /*
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void startGattServer() {
        bluetoothGattServer = bluetoothManager.openGattServer(
                context,
                new BluetoothGattServerCallback() {
                    @Override
                    // Callback indicating when a remote device has been connected or disconnected.
                    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                        super.onConnectionStateChange(device, status, newState);

                        for (Entry<String, NearbyEndpoint> entry : endpoints.entrySet()) {
                            NearbyEndpoint endpoint = entry.getValue();

                            if (endpoint.getAddress().equals(device.getAddress())) {
                                if (newState == BluetoothProfile.STATE_CONNECTED) {
                                    endpoint.isConnected(true);
                                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                    endpoint.isConnected(false);
                                }

                                break;
                            }
                        }
                    }

                    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    @Override
                    public void onCharacteristicReadRequest(
                            BluetoothDevice device,
                            int requestId,
                            int offset,
                            BluetoothGattCharacteristic characteristic
                    ) {
                        super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

                        byte[] dataToSend = "Hello World".getBytes();
                        bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, dataToSend);
                    }

                    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    @Override
                    public void onCharacteristicWriteRequest(
                            BluetoothDevice device,
                            int requestId,
                            BluetoothGattCharacteristic characteristic,
                            boolean preparedWrite,
                            boolean responseNeeded,
                            int offset,
                            byte[] value
                    ) {
                        super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);

                        // Handle the write request here
                        bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
                    }

                    @Override
                    public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
                        super.onDescriptorReadRequest(device, requestId, offset, descriptor);
                    }

                    @Override
                    public void onDescriptorWriteRequest(
                            BluetoothDevice device,
                            int requestId,
                            BluetoothGattDescriptor descriptor,
                            boolean preparedWrite,
                            boolean responseNeeded,
                            int offset,
                            byte[] value
                    ) {
                        super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
                    }
                }
        );

        BluetoothGattService service = new BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE
        );

        service.addCharacteristic(characteristic);

        bluetoothGattServer.addService(service);
    }
    */

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
                    public void onFound(@Nullable UUID id, @Nullable String name, @Nullable byte[] info, @Nullable Short channel, Integer rssi, String address) {
                        @Nullable
                        String endpointID = EndpointID.fromUUID(id);
                        @Nullable
                        String endpointName = name;
                        @Nullable
                        byte[] endpointInfo = info;

                        if (endpointID == null) return;

                        NearbyEndpoint nearbyEndpoint = endpoints.get(endpointID);
                        if (nearbyEndpoint != null) {
                            nearbyEndpoint.alive();
                        } else {
                            new NearbyEndpoint(endpointID, endpointName, endpointInfo, channel, rssi, address, () -> {
                                if (nearbyScanner.isScanning()) {
                                    Endpoint endpoint = new Endpoint(endpointID);

                                    plugin.onEndpointLost(endpoint);
                                }

                                NearbyEndpoint endpoint = endpoints.get(endpointID);
                                if (endpoint != null) {
                                    endpoint.kill();
                                }
                            });
                            // endpoints.put(endpointID, nearbyEndpoint);

                            if (nearbyScanner.isScanning()) {
                                Endpoint endpoint = new Endpoint(endpointID, endpointName, endpointInfo);

                                plugin.onEndpointFound(endpoint);
                            }
                        }
                    }

                    @Override
                    public void onLost(@Nullable UUID id) {
                        @Nullable
                        String endpointID = EndpointID.fromUUID(id);

                        if (endpointID == null) return;

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
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void connect(@NonNull ConnectOptions options, @NonNull Callback callback) {
        if (bluetoothAdapter == null) {
            Exception exception = new Exception(NOT_INITIALIZED);
            callback.error(exception);
        }

        @Nullable
        String endpointID = options.getEndpointID();
        if (endpointID == null) {
            Exception exception = new Exception(MISSING_ENDPOINT_ID);
            callback.error(exception);
            return;
        }

        @Nullable
        byte[] endpointInfo = options.getEndpointInfo();

        @Nullable
        NearbyEndpoint endpoint = endpoints.get(endpointID);
        if (endpoint == null) {
            Exception exception = new Exception(MISSING_ENDPOINT);
            callback.error(exception);
            return;
        }

        String address = endpoint.getAddress();

        BluetoothDevice device;

        if ((device = bluetoothAdapter.getRemoteDevice(address)) != null) {
            if (
                    !endpoint.connect(device)
            ) {
                Exception exception = new Exception(NOT_CONNECTED);

                callback.error(exception);
                return;
            }
        }

        /*
        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(endpoint.getAddress());

        // Connect to the GATT server
        final BluetoothGatt gatt = device.connectGatt(
            context,
            false,
            new BluetoothGattCallback() {
                @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    super.onConnectionStateChange(gatt, status, newState);

                    endpoint.state = newState;

                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        // Attempts to discover services after successful connection.
                        //gatt.discoverServices();

                        plugin.onEndpointConnected(new Endpoint(endpoint.endpointID));
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        gatt.close();
                        endpoint.closeGatt();

                        plugin.onEndpointDisconnected(new Endpoint(endpoint.endpointID));
                    }
                }

                    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        super.onServicesDiscovered(gatt, status);

                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            // gatt.requestMtu(512); // Request a higher MTU size if needed

                            byte[] data = new byte[100];

                            BluetoothGattService service = gatt.getService(SERVICE_UUID);
                            if (service != null) {
                                BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);

                                if (
                                        characteristic != null &&
                                                (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0
                                ) {
                                    characteristic.setValue(data);

                                    boolean success = gatt.writeCharacteristic(characteristic);
                                }
                            }
                        } else {
                            // Log.w(TAG, "onServicesDiscovered received: " + status);
                        }
                    }

                    @Override
                    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            //Log.d("BLE", "MTU changed to: " + mtu);
                            // Proceed with data exchange
                        } else {
                            //Log.e("BLE", "Failed to change MTU");
                        }
                    }

                    @Override
                    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            byte[] data = characteristic.getValue();
                            // Log.d(TAG, "onCharacteristicRead: " + new String(data));
                        }
                    }

                    @Override
                    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            byte[] data = characteristic.getValue();
                            // Log.d(TAG, "onCharacteristicWrite: " + new String(data));
                        }
                    }

                    @Override
                    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                        final int format = BluetoothGattCharacteristic.FORMAT_UINT8;
                        final int offset = 0;
                        byte[] data = characteristic.getValue();
                        // Log.d(TAG, "onCharacteristicChanged: " + new String(data));
                    }
            }
        );

        endpoint.setGatt(gatt);
        */
        //        if (endpointInfo == null || endpointInfo.isEmpty()) {
        //            Exception exception = new Exception(MISSING_ENDPOINT_INFO);
        //            callback.error(exception);
        //            return;
        //        }

        callback.success();
    }

    public void requestConnection(@NonNull RequestConnectionOptions options, @NonNull Callback callback) {
        String endpointID = options.getEndpointID();
        if (endpointID == null) {
            Exception exception = new Exception(MISSING_ENDPOINT_ID);
            callback.error(exception);
            return;
        }

        byte[] endpointInfo = options.getEndpointInfo();
        //        if (endpointInfo == null || endpointInfo.isEmpty()) {
        //            Exception exception = new Exception(MISSING_ENDPOINT_INFO);
        //            callback.error(exception);
        //            return;
        //        }
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

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void disconnect(@NonNull DisconnectOptions options, @NonNull Callback callback) {
        if (bluetoothAdapter == null) {
            Exception exception = new Exception(NOT_INITIALIZED);
            callback.error(exception);
        }

        @Nullable
        String endpointID = options.getEndpointID();
        if (endpointID == null) {
            Exception exception = new Exception(MISSING_ENDPOINT_ID);
            callback.error(exception);
            return;
        }

        @Nullable
        NearbyEndpoint endpoint = endpoints.get(endpointID);
        if (endpoint == null) {
            Exception exception = new Exception(MISSING_ENDPOINT);
            callback.error(exception);
            return;
        }

        endpoint.disconnect();

        /*
        final BluetoothGatt gatt = endpoint.getGatt();

        if (gatt != null) {
            endpoint.state = BluetoothProfile.STATE_DISCONNECTING;

            gatt.disconnect();

            endpoint.setGatt(null);
        }

        endpoint.state = BluetoothProfile.STATE_DISCONNECTED;
        */

        callback.success();
    }

    /**
     * Payload
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
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

        for (String endpointID : endpointIDs) {
            NearbyEndpoint endpoint;

            if ((endpoint = endpoints.get(endpointID)) != null) {
                if (
                        !endpoint.send(config.endpointID, payload)
                ) {
                    Exception exception = new Exception(NOT_CONNECTED);

                    callback.error(exception);
                    return;
                }
            }
        }

        callback.success();
        //        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
        //                UUID.fromString("0000ffe4-0000-1000-8000-00805f9b34fb"),
        //                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
        //                BluetoothGattCharacteristic.PERMISSION_READ);
        //
        //        boolean result = characteristic.setValue(payload);
        //        gatt.writeCharacteristic(characteristic);

        //        connectionsClient
        //            .sendPayload(endpointIDs, Payload.fromBytes(payload))
        //            .addOnSuccessListener(callback::success)
        //            .addOnFailureListener(callback::error);
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
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    protected void stopAdvertising() {
        isAdvertising = false;

        nearbyAdvertiser.stop();
        // connectionsClient.stopAdvertising();

        if (bluetoothGattServer != null) {
            bluetoothGattServer.close();

            bluetoothGattServer = null;
        }
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

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void stop() {
        stopAdvertising();
        stopDiscovering();
        // connectionsClient.stopAllEndpoints();
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
