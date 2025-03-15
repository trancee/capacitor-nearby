import Foundation
import Capacitor

import NearbyConnections

@objc public class NearbyConnectionsImpl: NSObject {
    private let plugin: NearbyConnectionsPlugin
    private let config: NearbyConnectionsConfig

    private var connectionManager: ConnectionManager?
    private var advertiser: Advertiser?
    private var discoverer: Discoverer?

    private var isAdvertising = false
    private var isDiscovering = false

    private var requests: [EndpointID: ConnectionRequest] = [:]
    private var payloads: [PayloadID: CancellationToken] = [:]

    struct ConnectionRequest {
        let verificationCode: String
        let verificationHandler: (Bool) -> Void
    }

    init(plugin: NearbyConnectionsPlugin, config: NearbyConnectionsConfig) {
        self.plugin = plugin
        self.config = config

        super.init()
    }

    deinit {
        stop {_ in}
    }

    /**
     * Initialize
     */

    @objc public func initialize(_ options: InitializeOptions, completion: @escaping (Error?) -> Void) {
        if config.getEndpointName() == nil {
            completion(CustomError.endpointNameMissing)
            return
        }

        guard let serviceID = config.getServiceID() else {
            completion(CustomError.serviceIDMissing)
            return
        }
        guard let strategy = config.getStrategy() else {
            completion(CustomError.strategyMissing)
            return
        }

        guard let strategy = Strategy.fromString(strategy) else {
            completion(CustomError.strategyUnknown)
            return
        }

        connectionManager = ConnectionManager(serviceID: serviceID, strategy: strategy)
        guard let connectionManager = connectionManager else {
            completion(CustomError.initializationError)
            return
        }
        connectionManager.delegate = self

        advertiser = Advertiser(connectionManager: connectionManager)
        guard let advertiser = advertiser else {
            completion(CustomError.advertiserError)
            return
        }
        advertiser.delegate = self

        discoverer = Discoverer(connectionManager: connectionManager)
        guard let discoverer = discoverer else {
            completion(CustomError.discovererError)
            return
        }
        discoverer.delegate = self

        completion(nil)
    }

    /**
     * Reset
     */

    @objc public func reset(completion: @escaping (Error?) -> Void) {
        stop(completion)
    }

    /**
     * Advertising
     */

    @objc public func startAdvertising(_ options: StartAdvertisingOptions, completion: @escaping (Error?) -> Void) {
        guard let endpointName = options.getEndpointName() ?? config.getEndpointName() else {
            completion(CustomError.endpointNameMissing)
            return
        }
        if endpointName.isEmpty {
            completion(CustomError.endpointNameMissing)
            return
        }

        if config.getServiceID() == nil {
            completion(CustomError.serviceIDMissing)
            return
        }
        if config.getStrategy() == nil {
            completion(CustomError.strategyMissing)
            return
        }

        guard let advertiser = advertiser else {
            completion(CustomError.advertiserError)
            return
        }

        // The endpoint info can be used to provide arbitrary information to the
        // discovering device (e.g. device name or type).
        advertiser.startAdvertising(using: endpointName.data(using: .utf8)!, completionHandler: { error in
            self.isAdvertising = error == nil

            completion(error)
        })
    }

    @objc public func stopAdvertising(completion: @escaping (Error?) -> Void) {
        self.stopAdvertising(completion)
    }

    /**
     * Discovery
     */
    @objc public func startDiscovery(_ options: StartDiscoveryOptions, completion: @escaping (Error?) -> Void) {
        if config.getServiceID() == nil {
            completion(CustomError.serviceIDMissing)
            return
        }
        if config.getStrategy() == nil {
            completion(CustomError.strategyMissing)
            return
        }

        guard let discoverer = discoverer else {
            completion(CustomError.discovererError)
            return
        }

        discoverer.startDiscovery { error in
            self.isDiscovering = error == nil

            completion(error)
        }
    }

    @objc public func stopDiscovery(completion: @escaping (Error?) -> Void) {
        self.stopDiscovery(completion)
    }

    /**
     * Connection
     */

    @objc public func requestConnection(_ options: RequestConnectionOptions, completion: @escaping (Error?) -> Void) {
        guard let endpointID = options.getEndpointID() else {
            completion(CustomError.endpointIDMissing)
            return
        }
        if endpointID.isEmpty {
            completion(CustomError.endpointIDMissing)
            return
        }

        guard let endpointName = options.getEndpointName() ?? config.getEndpointName() else {
            completion(CustomError.endpointNameMissing)
            return
        }
        if endpointName.isEmpty {
            completion(CustomError.endpointNameMissing)
            return
        }

        guard let discoverer = discoverer else {
            completion(CustomError.discovererError)
            return
        }

        discoverer.requestConnection(to: endpointID, using: endpointName.data(using: .utf8)!, completionHandler: { error in
            completion(error)
        })
    }

    @objc public func acceptConnection(_ options: AcceptConnectionOptions, completion: @escaping (Error?) -> Void) {
        guard let endpointID = options.getEndpointID() else {
            completion(CustomError.endpointIDMissing)
            return
        }
        if endpointID.isEmpty {
            completion(CustomError.endpointIDMissing)
            return
        }

        guard let request = requests[endpointID] else {
            completion(CustomError.endpointIDUnknown)
            return
        }

        request.verificationHandler(true)
        requests[endpointID] = nil

        completion(nil)
    }

    @objc public func rejectConnection(_ options: RejectConnectionOptions, completion: @escaping (Error?) -> Void) {
        guard let endpointID = options.getEndpointID() else {
            completion(CustomError.endpointIDMissing)
            return
        }
        if endpointID.isEmpty {
            completion(CustomError.endpointIDMissing)
            return
        }

        guard let request = requests[endpointID] else {
            completion(CustomError.endpointIDUnknown)
            return
        }

        request.verificationHandler(false)
        requests[endpointID] = nil

        completion(nil)
    }

    @objc public func disconnect(_ options: DisconnectOptions, completion: @escaping (Error?) -> Void) {
        guard let endpointID = options.getEndpointID() else {
            completion(CustomError.endpointIDMissing)
            return
        }
        if endpointID.isEmpty {
            completion(CustomError.endpointIDMissing)
            return
        }

        guard let connectionManager = connectionManager else {
            completion(CustomError.initializationError)
            return
        }

        connectionManager.disconnect(from: endpointID, completionHandler: { error in
            completion(error)
        })
    }

    /**
     * Payload
     */

    @objc public func sendPayload(_ options: SendPayloadOptions, completion: @escaping (Result?, Error?) -> Void) {
        guard let endpointIDs = options.getEndpointIDs() else {
            completion(nil, CustomError.endpointIDMissing)
            return
        }
        if endpointIDs.isEmpty {
            completion(nil, CustomError.endpointIDMissing)
            return
        }

        guard let payload = options.getPayload() else {
            completion(nil, CustomError.payloadMissing)
            return
        }

        guard let connectionManager = connectionManager else {
            completion(nil, CustomError.initializationError)
            return
        }

        let payloadID = PayloadID.unique()

        let cancellationToken = connectionManager.send(payload, to: endpointIDs, id: payloadID, completionHandler: { error in
            completion(nil, error)
        })

        payloads[payloadID] = cancellationToken

        let result = SendPayloadResult(
            payloadID: payloadID,
            payloadType: .bytes,

            status: .inProgress
            // status: .inProgress(Progress())
        )

        completion(result, nil)
    }

    @objc public func cancelPayload(_ options: CancelPayloadOptions, completion: @escaping (Error?) -> Void) {
        guard let payloadID = options.getPayloadID() else {
            completion(CustomError.payloadIDMissing)
            return
        }

        guard let cancellationToken = payloads[payloadID] else {
            completion(CustomError.payloadIDMissing)
            return
        }

        cancellationToken.cancel { error in
            if error == nil {
                self.payloads[payloadID] = nil
            }

            completion(error)
        }
    }

    /**
     * Status
     */

    @objc public func status(completion: @escaping (Result?, Error?) -> Void) {
        let result = StatusResult(isAdvertising: isAdvertising, isDiscovering: isDiscovering)

        completion(result, nil)
    }

    /**
     * Stops advertising.
     */

    private func stopAdvertising(_ completion: @escaping (Error?) -> Void) {
        isAdvertising = false

        guard let advertiser = advertiser else {
            completion(CustomError.advertiserError)
            return
        }

        advertiser.stopAdvertising(completionHandler: completion)
    }

    /**
     * Stops discovery.
     */
    private func stopDiscovery(_ completion: @escaping (Error?) -> Void) {
        isDiscovering = false

        guard let discoverer = discoverer else {
            completion(CustomError.discovererError)
            return
        }

        discoverer.stopDiscovery(completionHandler: completion)
    }

    private func stop(_ completion: @escaping (Error?) -> Void) {
        self.stopAdvertising(completion)
        self.stopDiscovery(completion)

        requests.removeAll()
        payloads.removeAll()
    }
}

extension NearbyConnectionsImpl: ConnectionManagerDelegate {
    public func connectionManager(
        _ connectionManager: ConnectionManager, didReceive verificationCode: String,
        from endpointID: EndpointID, verificationHandler: @escaping (Bool) -> Void) {
        requests[endpointID] = ConnectionRequest(
            verificationCode: verificationCode,
            verificationHandler: { accept in
                verificationHandler(accept)
            }
        )

        let event = EndpointInitiatedEvent(endpointID, verificationCode)
        self.plugin.onEndpointInitiated(event)
    }

    public func connectionManager(
        _ connectionManager: ConnectionManager, didReceive data: Data,
        withID payloadID: PayloadID, from endpointID: EndpointID) {
        // A simple byte payload has been received. This will always include the full data.

        let event = PayloadReceivedEvent(endpointID, payloadID, payloadType: .bytes, payload: data)
        self.plugin.onPayloadReceived(event)
    }

    public func connectionManager(
        _ connectionManager: ConnectionManager, didReceive stream: InputStream,
        withID payloadID: PayloadID, from endpointID: EndpointID,
        cancellationToken token: CancellationToken) {
        // We have received a readable stream.
    }

    public func connectionManager(
        _ connectionManager: ConnectionManager,
        didStartReceivingResourceWithID payloadID: PayloadID,
        from endpointID: EndpointID, at localURL: URL,
        withName name: String, cancellationToken token: CancellationToken) {
        // We have started receiving a file. We will receive a separate transfer update
        // event when complete.
    }

    public func connectionManager(
        _ connectionManager: ConnectionManager,
        didReceiveTransferUpdate update: TransferUpdate,
        from endpointID: EndpointID, forPayload payloadID: PayloadID) {
        // A success, failure, cancelation or progress update.

        switch update {
        case .success, .canceled, .failure:
            payloads[payloadID] = nil
        case .progress:
            break
        }

        let event = PayloadTransferUpdateEvent(endpointID, payloadID, update: update)
        self.plugin.onPayloadTransferUpdate(event)
    }

    public func connectionManager(
        _ connectionManager: ConnectionManager, didChangeTo state: ConnectionState,
        for endpointID: EndpointID) {
        switch state {
        case .connecting:
            // A connection to the remote endpoint is currently being established.
            break
        case .connected:
            // We're connected! Can now start sending and receiving data.

            requests[endpointID] = nil

            let event = EndpointConnectedEvent(endpointID)
            self.plugin.onEndpointConnected(event)
        case .disconnected:
            // We've been disconnected from this endpoint. No more data can be sent or received.

            let event = EndpointDisconnectedEvent(endpointID)
            self.plugin.onEndpointDisconnected(event)
        case .rejected:
            // The connection was rejected by one or both sides.

            requests[endpointID] = nil

            let event = EndpointRejectedEvent(endpointID)
            self.plugin.onEndpointRejected(event)
        }
    }
}

extension NearbyConnectionsImpl: AdvertiserDelegate {
    public func advertiser(
        _ advertiser: Advertiser, didReceiveConnectionRequestFrom endpointID: EndpointID,
        with context: Data, connectionRequestHandler: @escaping (Bool) -> Void) {
        // Accept or reject any incoming connection requests. The connection will still need to
        // be verified in the connection manager delegate.

        connectionRequestHandler(true)
    }
}

extension NearbyConnectionsImpl: DiscovererDelegate {
    public func discoverer(
        _ discoverer: Discoverer, didFind endpointID: EndpointID, with context: Data) {
        // An endpoint was found. We request a connection to it. The endpoint info can be used
        // to provide arbitrary information to the discovering device (e.g. device name or type).

        // discoverer.requestConnection(to: endpointID, using: "My Device".data(using: .utf8)!)

        let event = EndpointFoundEvent(endpointID, endpointName: String(data: context, encoding: .utf8)!)
        self.plugin.onEndpointFound(event)
    }

    public func discoverer(_ discoverer: Discoverer, didLose endpointID: EndpointID) {
        // A previously discovered endpoint has gone away.

        requests[endpointID] = nil

        let event = EndpointLostEvent(endpointID)
        self.plugin.onEndpointLost(event)
    }
}
