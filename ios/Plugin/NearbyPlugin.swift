import Foundation
import Capacitor
import CoreBluetooth

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
struct Constants {
    static let BLUETOOTH_NOT_SUPPORTED = "Bluetooth not supported"
    static let BLE_NOT_SUPPORTED = "Bluetooth Low Energy not supported"
    static let NOT_INITIALIZED = "not initialized"
    static let PERMISSION_DENIED = "permission denied"

    static let UUID_NOT_FOUND = "UUID not found"
}

public typealias StateCallback = (StateResult) -> Void

public enum StateResult {
    case unknown
    case resetting
    case unsupported
    case unauthorized
    case poweredOff
    case poweredOn
}

@objc(NearbyPlugin)
public class NearbyPlugin: CAPPlugin {
    private var scanner: Scanner!
    private var advertiser: Advertiser!

    private var serviceUUID: CBUUID?

    private var uuid: CBUUID?
    private var data: Data?

    /**
     * Initialize
     */
    @objc func initialize(_ call: CAPPluginCall) {
        guard let serviceUUID = call.getString("serviceUUID") else {
            call.reject(Constants.UUID_NOT_FOUND)
            return
        }

        if serviceUUID.count > 0 {
            self.serviceUUID = CBUUID(string: serviceUUID)
        } else {
            call.reject(Constants.UUID_NOT_FOUND)
            return
        }

        self.scanner = Scanner(self.serviceUUID!) { [self] result in
            notifyListeners("onBluetoothStateChanged", data: [
                "state": fromBluetoothState(result)
            ])

            switch result {
            case .poweredOn:
                call.resolve()
            default:
                call.reject("Bluetooth is not powered on.")
            }
        } beaconCallback: { [self] result in
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

                break
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

                break
            }
        }

        self.advertiser = Advertiser(self.serviceUUID!) { [self] result in
            notifyListeners("onBluetoothStateChanged", data: [
                "state": fromBluetoothState(result)
            ])

            switch result {
            case .poweredOn:
                call.resolve()
            default:
                call.reject("Bluetooth is not powered on.")
            }
        }

        self.uuid = nil
        self.data = nil

        // call.success()
    }

    /**
     * Reset
     */
    @objc func reset(_ call: CAPPluginCall) {
        stop()

        self.uuid = nil
        self.data = nil

        call.resolve()
    }

    /**
     * Publish
     */
    @objc func publish(_ call: CAPPluginCall) {
        guard let advertiser = self.advertiser else {
            call.reject(Constants.NOT_INITIALIZED)
            return
        }

        guard let beaconUUID = call.getString("uuid") else {
            call.reject(Constants.UUID_NOT_FOUND)
            return
        }

        if beaconUUID.count > 0 {
            self.uuid = CBUUID(string: beaconUUID)
        } else {
            call.reject(Constants.UUID_NOT_FOUND)
            return
        }

        if !advertiser.isAdvertising() {
            advertiser.start(uuid!, call.getInt("ttlSeconds")) { result in
                switch result {
                case .started:
                    call.resolve()

                    break
                case .stopped(let e):
                    if let e = e {
                        call.reject(e.localizedDescription, String((e as NSError).code))
                    }

                    break
                case .expired:
                    self.publishExpired()

                    break
                }
            }
        } else {
            call.resolve()
        }
    }

    @objc func unpublish(_ call: CAPPluginCall) {
        guard let advertiser = self.advertiser else {
            call.reject(Constants.NOT_INITIALIZED)
            return
        }

        advertiser.stop()

        self.uuid = nil
        self.data = nil

        call.resolve()
    }

    private func publishExpired() {
        guard let advertiser = self.advertiser else { return }

        if advertiser.isAdvertising() {
            notifyListeners("onPublishExpired", data: nil)
        }

        advertiser.stop()
    }

    /**
     * Subscribe
     */
    @objc func subscribe(_ call: CAPPluginCall) {
        guard let scanner = self.scanner else {
            call.reject(Constants.NOT_INITIALIZED)
            return
        }

        if !scanner.isScanning() {
            scanner.start(call.getInt("ttlSeconds")) { result in
                switch result {
                case .started:
                    call.resolve()

                    break
                case .stopped(let e):
                    if let e = e {
                        call.reject(e.localizedDescription, String((e as NSError).code))
                    }

                    break
                case .expired:
                    self.subscribeExpired()

                    break
                }
            }
        } else {
            call.resolve()
        }
    }

    @objc func unsubscribe(_ call: CAPPluginCall) {
        guard let scanner = self.scanner else {
            call.reject(Constants.NOT_INITIALIZED)
            return
        }

        scanner.stop()

        call.resolve()
    }

    private func subscribeExpired() {
        guard let scanner = self.scanner else { return }

        if scanner.isScanning() {
            notifyListeners("onSubscribeExpired", data: nil)
        }

        scanner.stop()
    }

    /**
     * Status
     */
    @objc func status(_ call: CAPPluginCall) {
        let isPublishing = (self.advertiser != nil) ? self.advertiser.isAdvertising() : false
        let isSubscribing = (self.scanner != nil) ? self.scanner.isScanning() : false

        let uuids = (self.scanner != nil) ? scanner.getBeacons() : []

        call.resolve([
            "isPublishing": isPublishing,
            "isSubscribing": isSubscribing,
            "uuids": uuids
        ])
    }

    /**
     * Helper
     */
    private func stop() {
        if let scanner = self.scanner {
            scanner.stop()
        }

        if let advertiser = self.advertiser {
            advertiser.stop()
        }
    }

    private func fromBluetoothState(_ state: StateResult) -> String {
        switch state {
        case .poweredOn:
            return "poweredOn"
        case .poweredOff:
            return "poweredOff"
        case .resetting:
            return "resetting"
        case .unsupported:
            return "unsupported"
        case .unauthorized:
            return "unauthorized"
        default:
            return "unknown"
        }
    }
}
