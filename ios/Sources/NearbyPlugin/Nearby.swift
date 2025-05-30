import Foundation
import Capacitor
import CoreBluetooth

public typealias StateCallback = (StateResult) -> Void

public enum StateResult {
    case unknown
    case resetting
    case unsupported
    case unauthorized
    case poweredOff
    case poweredOn
}

@objc public class Nearby: NSObject {
    private let plugin: NearbyPlugin
    private let config: NearbyConfig

    private var scanner: NearbyScanner!
    private var advertiser: NearbyAdvertiser!

    private var isAdvertising = false
    private var isDiscovering = false

    private var endpoints: [EndpointID: NearbyEndpoint] = [:]

    init(plugin: NearbyPlugin, config: NearbyConfig) {
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

    @objc public func initialize(_ options: InitializeOptions, completion: @escaping (Result?, Error?) -> Void) {
        //        if config.getEndpointName() == nil {
        //            completion(nil, CustomError.endpointNameMissing)
        //            return
        //        }

        guard let serviceID = config.getServiceID() else {
            completion(nil, CustomError.serviceIDMissing)
            return
        }

        let serviceUUID = {
            let data = serviceID.data.hash(4)

            return CBUUID(data: data)
        }()

        guard let endpointID = config.getEndpointID() else {
            completion(nil, CustomError.endpointIDMissing)
            return
        }

        let endpointUUID = endpointID.uuid
        let endpointName = config.getEndpointName()

        self.scanner = NearbyScanner(serviceUUID) { [self] _ in
            /*
             notifyListeners("onBluetoothStateChanged", data: [
             "state": fromBluetoothState(result)
             ])

             switch result {
             case .poweredOn:
             call.resolve()
             default:
             call.reject("Bluetooth is not powered on.")
             }
             */
            /*
             }  beaconCallback: { [self] result in
             guard let scanner = self.scanner else {
             return
             }

             switch result {
             case .found(let uuid, let data, let rssi):
             if scanner.isScanning() {
             var jsData: [String: Any] = [
             "uuid": uuid.uuidString.lowercased()
             ]

             if let data = data {
             jsData["content"] = Data(base64Encoded: data)
             }
             if let rssi = rssi {
             jsData["rssi"] = rssi.stringValue
             }

             notifyListeners("onFound", data: jsData)
             }
             case .lost(let uuid, let data, let rssi):
             if scanner.isScanning() {
             var jsData: [String: Any] = [
             "uuid": uuid.uuidString.lowercased()
             ]

             if let data = data {
             jsData["content"] = Data(base64Encoded: data)
             }
             if let rssi = rssi {
             jsData["rssi"] = rssi.stringValue
             }

             notifyListeners("onLost", data: jsData)
             }
             }
             */
        }

        self.advertiser = NearbyAdvertiser(serviceUUID, endpointName, endpointUUID) { [self] _ in
            //            notifyListeners("onBluetoothStateChanged", data: [
            //                "state": fromBluetoothState(result)
            //            ])
            //
            //            switch result {
            //            case .poweredOn:
            //                call.resolve()
            //            default:
            //                call.reject("Bluetooth is not powered on.")
            //            }
        }

        endpoints = [:]

        let result = InitializeResult(endpointID)

        completion(result, nil)
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
        //        guard let endpointName = options.getEndpointName() ?? config.getEndpointName() else {
        //            completion(CustomError.endpointNameMissing)
        //            return
        //        }
        //        if endpointName.isEmpty {
        //            completion(CustomError.endpointNameMissing)
        //            return
        //        }

        let endpointInfo = options.getEndpointInfo() ?? config.getEndpointInfo()

        if config.getServiceID() == nil {
            completion(CustomError.serviceIDMissing)
            return
        }

        advertiser.start(endpointInfo) { result in
            switch result {
            case .started:
                completion(nil)
                return

            case .stopped(let error):
                completion(error)
                return

            case .connected(let endpointID):
                let endpoint = Endpoint(endpointID)

                self.plugin.onEndpointConnected(endpoint)

            case .disconnected(let endpointID):
                let endpoint = Endpoint(endpointID)

                self.plugin.onEndpointDisconnected(endpoint)

            case .received(let endpointID, let payload):
                let endpoint = Endpoint(endpointID)

                self.plugin.onPayloadReceived(endpoint, payload)
            }
        }
    }

    @objc public func stopAdvertising(completion: @escaping (Error?) -> Void) {
        self.stopAdvertising(completion)
    }

    /**
     * Discovery
     */
    @objc public func startDiscovering(completion: @escaping (Error?) -> Void) {
        if config.getServiceID() == nil {
            completion(CustomError.serviceIDMissing)
            return
        }

        scanner.start(callback: { result in
            switch result {
            case .started:
                self.isDiscovering = true

                completion(nil)

            case .stopped(let error):
                self.isDiscovering = false

                completion(error)

            case .found(let id, let name, let info, let psm, let rssi, let power, let distance, let peripheral):
                let endpointID = String(bytes: id.data.prefix(ENDPOINT_ID_LENGTH), encoding: .utf8)!
                let endpointName = name
                let endpointInfo = info

                if let endpoint = self.endpoints[endpointID] {
                    endpoint.alive()

                    if self.scanner.isScanning() {
                        let endpoint = Endpoint(endpointID, endpointName: endpointName, endpointInfo: endpointInfo, rssi: rssi, power: power, distance: distance)

                        self.plugin.onEndpointFound(endpoint)
                    }
                } else {
                    let endpoint = NearbyEndpoint(endpointID, endpointName: endpointName, endpointInfo: endpointInfo, psm: psm, rssi: rssi, peripheral, callback: {
                        result in
                        switch result {
                        case .found(let endpointID, let endpointName, let endpointInfo):
                            if self.scanner.isScanning() {
                                let endpoint = Endpoint(endpointID, endpointName: endpointName, endpointInfo: endpointInfo, rssi: rssi, power: power, distance: distance)

                                self.plugin.onEndpointFound(endpoint)
                            }

                        case .lost(let endpointID):
                            if self.scanner.isScanning() {
                                let endpoint = Endpoint(endpointID)

                                self.plugin.onEndpointLost(endpoint)
                            }

                            self.endpoints[endpointID] = nil
                        }
                    })

                    self.endpoints[endpointID] = endpoint
                }

            case .lost(let id):
                let endpointID = String(bytes: id.data.prefix(ENDPOINT_ID_LENGTH), encoding: .utf8)!

                if let endpoint = self.endpoints[endpointID] {
                    endpoint.lost()
                }
            }
        })
    }

    @objc public func stopDiscovering(completion: @escaping (Error?) -> Void) {
        self.stopDiscovering(completion)
    }

    /**
     * Connection
     */

    @objc public func connect(_ options: ConnectOptions, completion: @escaping (Error?) -> Void) {
        guard let endpointID = options.getEndpointID() else {
            completion(CustomError.endpointIDMissing)
            return
        }
        if endpointID.isEmpty {
            completion(CustomError.endpointIDMissing)
            return
        }

        guard let endpoint = endpoints[endpointID] else {
            completion(CustomError.endpointMissing)
            return
        }

        do {
            try endpoint.connect()
        } catch {
            completion(error)
            return
        }

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

        guard let endpoint = endpoints[endpointID] else {
            completion(CustomError.endpointMissing)
            return
        }

        do {
            try endpoint.disconnect()
        } catch {
            completion(error)
            return
        }

        completion(nil)
    }

    /**
     * Payload
     */

    @objc public func sendPayload(_ options: SendPayloadOptions, completion: @escaping (Error?) -> Void) {
        guard let endpointIDs = options.getEndpointIDs() else {
            completion(CustomError.endpointIDMissing)
            return
        }
        if endpointIDs.isEmpty {
            completion(CustomError.endpointIDMissing)
            return
        }

        guard let payload = options.getPayload() else {
            completion(CustomError.payloadMissing)
            return
        }

        for endpointID in endpointIDs {
            guard let endpoint = endpoints[endpointID] else {
                completion(CustomError.endpointMissing)
                return
            }

            do {
                try endpoint.sendPayload(payload)
            } catch {
                completion(error)
                return
            }
        }

        completion(nil)
    }

    /**
     * Status
     */

    @objc public func status(completion: @escaping (Result?, Error?) -> Void) {
        let result = StatusResult(isAdvertising: isAdvertising, isDiscovering: isDiscovering)

        completion(result, nil)
    }

    /**
     * Permissions
     */

    @objc public func checkPermissions(completion: @escaping (Result?, Error?) -> Void) {
        let bluetoothState = switch self.scanner.bluetoothState() {
        case .unknown:
            "denied"
        case .resetting:
            "prompt"
        case .unsupported:
            "denied"
        case .unauthorized:
            "denied"
        case .poweredOff:
            "prompt"
        case .poweredOn:
            "granted"
        }
        let locationState = "granted"

        let result = PermissionsResult(bluetooth: bluetoothState, location: locationState)

        completion(result, nil)
    }

    @objc public func requestPermissions(_ options: RequestPermissionsOptions, completion: @escaping (Error?) -> Void) {
        guard let permissions = options.getPermissions() else {
            return
        }

        for permission in permissions {
            switch permission {
            case "bluetooth":
                guard let settingsUrl = URL(string: UIApplication.openSettingsURLString) else {
                    completion(CustomError.openSettingsError)
                    return
                }

                DispatchQueue.main.async {
                    if UIApplication.shared.canOpenURL(settingsUrl) {
                        UIApplication.shared.open(settingsUrl, completionHandler: { (success) in
                            if !success {
                                completion(CustomError.openSettingsError)
                                return
                            }
                        })
                    } else {
                        completion(CustomError.openSettingsError)
                        return
                    }
                }
            case "location":
                break
            default:
                break
            }
        }

        completion(nil)
    }

    /**
     * Stops advertising.
     */

    private func stopAdvertising(_ completion: @escaping (Error?) -> Void) {
        if let advertiser = self.advertiser {
            advertiser.stop()
        }

        isAdvertising = false
    }

    /**
     * Stops discovering.
     */
    private func stopDiscovering(_ completion: @escaping (Error?) -> Void) {
        if let scanner = self.scanner {
            scanner.stop()
        }

        isDiscovering = false
    }

    private func stop(_ completion: @escaping (Error?) -> Void) {
        self.stopAdvertising(completion)
        self.stopDiscovering(completion)
    }
}
