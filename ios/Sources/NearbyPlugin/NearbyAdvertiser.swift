//
//  NearbyAdvertiser.swift
//  Plugin
//
//  Created by Philipp Grosswiler on 5/7/21.
//  Copyright © 2021 Max Lynch. All rights reserved.
//

import CoreBluetooth

let MAXIMUM_DATA_SIZE = 15

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

    private let queue = DispatchQueue(label: "NearbyAdvertiser")

    private var psm: CBL2CAPPSM?

    init(_ serviceUUID: CBUUID,
         _ endpointName: String?,
         _ endpointUUID: CBUUID,
         stateCallback: @escaping StateCallback) {
        super.init()

        NearbyAdvertiser.serviceUUID = serviceUUID

        NearbyAdvertiser.endpointName = endpointName
        NearbyAdvertiser.endpointUUID = endpointUUID

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
        if let psm {
            peripheralManager?.unpublishL2CAPChannel(psm)
        }

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

        let channel: Short? = self.psm

        func makeUUID() -> CBUUID? {
            var size = endpointInfo?.count ?? 0

            if size > MAXIMUM_DATA_SIZE {
                if let callback = self.callback {
                    callback(.stopped(CustomError.dataTooLarge))
                }

                return nil
            }

            var data = Data(count: 16, repeating: 0)

            if let channel {
                data[0] = UInt8(channel)
            }
            if let endpointInfo {
                data[1...] = endpointInfo
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
        print("NearbyAdvertiser::peripheralManagerDidUpdateState", peripheral.state)
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
        print("NearbyAdvertiser::didUnpublishL2CAPChannel", PSM)

        self.psm = PSM

        if let error {
            return stop(error)
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
        print("NearbyAdvertiser::didPublishL2CAPChannel", PSM)

        self.psm = PSM

        if let error {
            return stop(error)
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
        print("NearbyAdvertiser::didOpen", channel)

        if let error {
            return stop(error)
        }

        if let channel {
            queue.async {
                var endpointID: String?

                channel.inputStream.delegate = self
                channel.inputStream.schedule(in: .main, forMode: .default)
                channel.inputStream.open()

                channel.outputStream.delegate = self
                channel.outputStream.schedule(in: .main, forMode: .default)
                channel.outputStream.open()

                let bufferSize = 8192
                let buffer = UnsafeMutablePointer<UInt8>.allocate(capacity: bufferSize)
                defer {
                    buffer.deallocate()
                }

                // 1. Identity
                if channel.inputStream.read(buffer, maxLength: ENDPOINT_ID_LENGTH) == ENDPOINT_ID_LENGTH {
                    endpointID = String(cString: buffer)
                    print("receive::endpointID", endpointID)

                    if let endpointID {
                        if let callback = self.callback {
                            callback(.connected(endpointID))
                        }
                    }

                    // 2. Payload Length
                    if channel.inputStream.read(buffer, maxLength: 3) == 3 {
                        var length = Int(buffer[0]) | Int(buffer[1]) << 8 | Int(buffer[2]) << 16
                        print("receive::length", length)

                        // 3. Payload
                        var payload = Data(capacity: length)
                        while length > 0 {
                            let read = channel.inputStream.read(buffer, maxLength: min(length, bufferSize))
                            print("receive::payload \(read) bytes of total: \(payload.count), remaining: \(length)")
                            if read == 0 { break }

                            payload.append(buffer, count: read)

                            length -= read
                        }

                        // 4. Checksum
                        if channel.inputStream.read(buffer, maxLength: 4) == 4 {
                            let checksum = UInt32(buffer[0]) | UInt32(buffer[1]) << 8 | UInt32(buffer[2]) << 16 | UInt32(buffer[3]) << 24
                            print("receive::checksum", checksum)

                            let ok = (checksum == payload.crc32())
                            print("receive::checksum ok", ok)

                            // 5. (N)ACK
                            channel.outputStream.write(ok ? 0x01 : 0x00)
                            print("receive::checksum ok", ok)
                        } else {
                            // 5. NAK
                            channel.outputStream.write(0x00)
                            print("receive::checksum NOK")
                        }

                        if let endpointID {
                            if let callback = self.callback {
                                callback(.received(endpointID, payload: payload))
                            }
                        }
                    }
                }

                channel.inputStream.close()
                channel.inputStream.remove(from: .main, forMode: .default)

                channel.outputStream.close()
                channel.outputStream.remove(from: .main, forMode: .default)

                if let endpointID {
                    if let callback = self.callback {
                        callback(.disconnected(endpointID))
                    }
                }
            }
        }
    }
}

extension NearbyAdvertiser: StreamDelegate {
    //    public func stream(_ stream: Stream, handle eventCode: Stream.Event) {
    //        switch eventCode {
    //        case Stream.Event.openCompleted:
    //            print("Stream is open")
    //        case Stream.Event.endEncountered:
    //            print("Stream encountered end")
    //        case Stream.Event.hasBytesAvailable:
    //            print("Stream has bytes available")
    //        // receive
    //        case Stream.Event.hasSpaceAvailable:
    //            print("Stream has space available")
    //        // send
    //        case Stream.Event.errorOccurred:
    //            print("Stream error occurred")
    //        default:
    //            print("Unknown stream event")
    //        }
    //    }
}
