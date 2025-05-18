//
//  NearbyAdvertiser.swift
//  Plugin
//
//  Created by Philipp Grosswiler on 5/7/21.
//  Copyright © 2021 Max Lynch. All rights reserved.
//

import CoreBluetooth

let MAXIMUM_DATA_SIZE = 14

public typealias AdvertiseCallback = (AdvertiseResult) -> Void

public enum AdvertiseResult {
    case started
    case stopped(_ error: Error? = nil)

    case connected(_ endpointID: EndpointID)
    case disconnected(_ endpointID: EndpointID)
    case received(_ endpointID: EndpointID, payload: Data)
}

public final class NearbyAdvertiser: NSObject {
    // An object that manages and advertises peripheral services exposed by this app.
    private var peripheralManager: CBPeripheralManager?
    private var callback: AdvertiseCallback?

    private var timer: Timer?

    private static var serviceUUID: CBUUID?

    private static var endpointName: String?
    private static var endpointUUID: CBUUID?

    private static var stateCallback: StateCallback?

    private var psm: CBL2CAPPSM?

    init(_ serviceUUID: UUID,
         _ endpointName: String?,
         _ endpointUUID: UUID,
         stateCallback: @escaping StateCallback) {
        super.init()

        NearbyAdvertiser.serviceUUID = CBUUID(nsuuid: serviceUUID)

        NearbyAdvertiser.endpointName = endpointName
        NearbyAdvertiser.endpointUUID = CBUUID(nsuuid: endpointUUID)

        NearbyAdvertiser.stateCallback = stateCallback

        // Keys used to specify options when creating a peripheral manager.
        let options: [String: Any] = [
            // A Boolean value specifying whether the system should warn if Bluetooth is in the powered-off state when instantiating the peripheral manager.
            CBPeripheralManagerOptionShowPowerAlertKey: true
        ]

        self.peripheralManager = CBPeripheralManager(delegate: self, queue: nil, options: options)
        self.callback = nil

        self.timer = nil
    }

    deinit {
        stop()
    }
}

extension NearbyAdvertiser {
    // Start advertising this device as a peripheral
    public func start(
        _ endpointInfo: Data?,
        // _ ttlSeconds: Int?,
        callback: @escaping AdvertiseCallback) {
        self.callback = callback

        stop()

        guard let peripheralManager else {
            if let callback = self.callback {
                callback(.stopped())
            }

            return
        }

        let channel: Short? = psm

        func makeUUID() -> CBUUID? {
            var size = endpointInfo?.count ?? 0

            if size > MAXIMUM_DATA_SIZE {
                if let callback = self.callback {
                    callback(.stopped(CustomError.dataTooLarge))
                }

                return nil
            }

            var data = Data(count: 16)

            data[0] = UInt8(size)

            if channel != nil {
                data[0] |= 0x80
            }

            if let endpointInfo {
                data[1...] = endpointInfo
            }
            if let channel {
                data[size + 1] = UInt8(channel)
            }

            return CBUUID(data: data)
        }

        let dataUUID = makeUUID()

        // An optional dictionary containing the data you want to advertise.
        let advertisementData: [String: Any] = [
            // An array of service UUIDs.
            CBAdvertisementDataServiceUUIDsKey: [
                NearbyAdvertiser.serviceUUID,
                NearbyAdvertiser.endpointUUID,
                dataUUID
            ]
        ]

        // Advertises peripheral manager data.
        peripheralManager.startAdvertising(advertisementData)

        //        if let ttlSeconds = ttlSeconds {
        //            startTimer(TimeInterval(ttlSeconds))
        //        }
    }

    public func stop(_ error: Error? = nil) {
        // stopTimer()

        if let peripheralManager {
            // Stops advertising peripheral manager data.
            peripheralManager.stopAdvertising()
        }

        if let callback {
            callback(.stopped(error))
        }
    }

    public func isAdvertising() -> Bool {
        guard let peripheralManager else { return false }

        // A Boolean value that indicates whether the peripheral is advertising data.
        return peripheralManager.isAdvertising
    }

    //    private func startTimer(_ timeout: TimeInterval) {
    //        stopTimer()
    //
    //        self.timer = Timer.scheduledTimer(
    //            timeInterval: timeout,
    //            target: self,
    //            selector: #selector(self.onTimer),
    //            userInfo: nil,
    //            repeats: false)
    //    }
    //
    //    private func stopTimer() {
    //        if let timer = self.timer {
    //            if timer.isValid { timer.invalidate() }
    //
    //            self.timer = nil
    //        }
    //    }
    //
    //    @objc fileprivate func onTimer(_ timer: Timer) {
    //        stopTimer()
    //
    //        if let callback = self.callback {
    //            callback(.expired)
    //        }
    //    }
}

// A protocol that provides updates for local peripheral state and interactions with remote central devices.
extension NearbyAdvertiser: CBPeripheralManagerDelegate {
    public func bluetoothState() -> StateResult {
        switch self.peripheralManager?.state {
        case .unknown:
            (.unknown)
        case .resetting:
            (.resetting)
        case .unsupported:
            (.unsupported)
        case .unauthorized:
            (.unauthorized)
        case .poweredOff:
            (.poweredOff)
        case .poweredOn:
            (.poweredOn)
        case .none:
            (.unknown)
        @unknown default:
            (.unknown)
        }
    }

    // Tells the delegate the peripheral manager’s state updated.
    public func peripheralManagerDidUpdateState(_ peripheral: CBPeripheralManager) {
        if let callback = NearbyAdvertiser.stateCallback {
            switch peripheral.state {
            case .unknown:
                callback(.unknown)
            case .resetting:
                stop()
                if let psm { peripheral.unpublishL2CAPChannel(psm) }
                callback(.resetting)
            case .unsupported:
                callback(.unsupported)
            case .unauthorized:
                callback(.unauthorized)
            case .poweredOff:
                stop()
                if let psm { peripheral.unpublishL2CAPChannel(psm) }
                callback(.poweredOff)
            case .poweredOn:
                peripheral.publishL2CAPChannel(withEncryption: false)
                callback(.poweredOn)
            @unknown default:
                callback(.unknown)
            }
        }
    }

    // Tells the delegate the peripheral manager started advertising the local peripheral device’s data.
    public func peripheralManagerDidStartAdvertising(_ peripheral: CBPeripheralManager,
                                                     error: Error?) {
        if let callback {
            if let error {
                stop(error)
            } else {
                callback(.started)
            }
        }
    }
}

extension NearbyAdvertiser: CBPeripheralDelegate {
    // Tells the delegate that the peripheral manager removed a published service from the local system.
    public func peripheralManager(
        // The peripheral manager that stopped publishing.
        _ peripheral: CBPeripheralManager,
        // The Protocol/Service Multiplexer (PSM) of the channel that was unpublished.
        didUnpublishL2CAPChannel PSM: CBL2CAPPSM,
        // The error that occurred, or nil if no error occurred.
        error: (any Error)?
    ) {
        print("didUnpublishL2CAPChannel")

        self.psm = PSM

        if let error {
            stop(error)
        }
    }

    // Tells the delegate that the peripheral manager created a listener for incoming L2CAP channel connections.
    public func peripheralManager(
        // The peripheral manager that published the channel.
        _ peripheral: CBPeripheralManager,
        // The Protocol/Service Multiplexer (PSM) of the published channel.
        didPublishL2CAPChannel PSM: CBL2CAPPSM,
        // The error that prevented publishing, or nil if no error occurred.
        error: (any Error)?
    ) {
        print("didPublishL2CAPChannel")

        self.psm = PSM

        if let error {
            stop(error)
        }
    }

    // Tells the delegate that the peripheral manager opened an L2CAP channel.
    public func peripheralManager(
        // The peripheral manager that opened the channel.
        _ peripheral: CBPeripheralManager,
        // The channel opened by the manager.
        didOpen channel: CBL2CAPChannel?,
        // The error that occurred, or nil if no error occurred.
        error: (any Error)?
    ) {
        print("didOpen")

        if let error {
            stop(error)
        }
    }
}
