//
//  NearbyEndpoint.swift
//  CapacitorTranceeNearby
//
//  Created by Philipp Grosswiler on 03.04.2025.
//

import Foundation
import CoreBluetooth

let MAXIMUM_PAYLOAD_SIZE = 0x1000000

public typealias Short = UInt16

public typealias EndpointCallback = (EndpointResult) -> Void

public enum EndpointResult {
    case lost(_ endpointID: EndpointID)
}

class NearbyEndpoint: NSObject {
    // func centralManagerDidUpdateState(_ central: CBCentralManager) {}

    /*
     func centralManagerDidUpdateState(_ central: CBCentralManager) {
     // Tells the delegate the central manager’s state updated.
     switch central.state {
     case .unknown:
     callback(.unknown)
     case .resetting:
     callback(.resetting)
     case .unsupported:
     callback(.unsupported)
     case .unauthorized:
     callback(.unauthorized)
     case .poweredOff:
     callback(.poweredOff)
     case .poweredOn:
     callback(.poweredOn)
     @unknown default:
     callback(.unknown)
     }
     }
     */

    /*
     func peripheralManagerDidUpdateState(_ peripheral: CBPeripheralManager) {
     // Tells the delegate the peripheral manager’s state updated.
     switch peripheral.state {
     case .unknown:
     callback(.unknown)
     case .resetting:
     callback(.resetting)
     case .unsupported:
     callback(.unsupported)
     case .unauthorized:
     callback(.unauthorized)
     case .poweredOff:
     callback(.poweredOff)
     case .poweredOn:
     callback(.poweredOn)
     @unknown default:
     callback(.unknown)
     }
     }
     */

    let endpointID: EndpointID

    let endpointName: String?
    let endpointInfo: Data?

    let channel: Short?
    let rssi: NSNumber?

    let timestamp: Date

    private var lastSeen: Date

    private var timer: Timer?

    private static let ttlSeconds: TimeInterval = 10

    // private var managerQueue = DispatchQueue.global(qos: .utility)
    // private var peripheralManager: CBPeripheralManager?
    // private var centralManager: CBCentralManager?
    private var peripheral: CBPeripheral?
    private var socket: CBL2CAPChannel?

    private var callback: EndpointCallback?

    init(_ endpointID: EndpointID, endpointName: String?, endpointInfo: Data?, channel: Short?, rssi: NSNumber? = nil, _ peripheral: CBPeripheral, callback: @escaping EndpointCallback) {
        self.endpointID = endpointID

        self.endpointName = endpointName
        self.endpointInfo = endpointInfo

        self.channel = channel
        self.rssi = rssi

        self.timestamp = Date()

        self.lastSeen = Date()

        self.peripheral = peripheral

        self.callback = callback

        super.init()

        // self.centralManager = CBCentralManager(delegate: self, queue: nil)

        // self.peripheralManager = CBPeripheralManager(delegate: nil, queue: managerQueue)
        // self.peripheralManager?.delegate = self

        peripheral.delegate = self

        alive()
    }
    deinit {
        kill()
    }

    func kill() {
        do {
            try disconnect()
        } catch {
            // ignore
        }

        stopTimer()
    }

    func alive() {
        self.lastSeen = Date()

        stopTimer()

        self.timer = Timer.scheduledTimer(
            timeInterval: NearbyEndpoint.ttlSeconds,
            target: self,
            selector: #selector(self.onTimer),
            userInfo: nil,
            repeats: false)
    }

    func connect() throws {
        if let peripheral {
            // if let centralManager {
            //    centralManager.connect(peripheral)
            // }

            // The PSM of the channel to open
            if let channel {
                // Attempt to open an L2CAP channel to the peripheral using the supplied PSM.
                peripheral.openL2CAPChannel(channel)
            }
        }
    }

    func disconnect() throws {
        if let peripheral {
            // if let centralManager {
            //    centralManager.cancelPeripheralConnection(peripheral)
            // }

            if let socket {
                socket.inputStream.close()
                socket.inputStream.remove(from: .main, forMode: .default)
                socket.inputStream.delegate = nil

                socket.outputStream.close()
                socket.outputStream.remove(from: .main, forMode: .default)
                socket.outputStream.delegate = nil
            }
        }

        self.socket = nil
    }

    func sendPayload(_ payload: Data) throws {
        if let socket {
            let length = payload.count

            if length >= MAXIMUM_PAYLOAD_SIZE {
                throw CustomError.payloadTooLarge
            }

            let checksum = payload.crc32()

            // 1. Identity
            _ = socket.outputStream.write(endpointID.data)
            // 2. Payload Length
            _ = socket.outputStream.write(
                Data([
                    (UInt8) ((length) & 0xff),
                    (UInt8) ((length >> 8) & 0xff),
                    (UInt8) ((length >> 16) & 0xff)
                ])
            )
            // 3. Payload
            _ = socket.outputStream.write(payload)
            // 4. Checksum
            _ = socket.outputStream.write(
                Data([
                    (UInt8) ((checksum) & 0xff),
                    (UInt8) ((checksum >> 8) & 0xff),
                    (UInt8) ((checksum >> 16) & 0xff),
                    (UInt8) ((checksum >> 24) & 0xff)
                ])
            )

            // 5. (N)ACK
            if !(socket.inputStream.read() > 0) {
                throw CustomError.notAcknowledged
            }
        }
    }

    private func stopTimer() {
        if let timer {
            if timer.isValid { timer.invalidate() }

            self.timer = nil
        }
    }

    @objc fileprivate func onTimer(_ timer: Timer) {
        kill()

        if let callback {
            callback(.lost(self.endpointID))
        }
    }
}

extension NearbyEndpoint: CBPeripheralDelegate {
    public func peripheral(_ peripheral: CBPeripheral, didOpen socket: CBL2CAPChannel?, error: Error?) {
        self.socket = socket

        if let socket {
            socket.inputStream.delegate = self
            socket.inputStream.schedule(in: RunLoop.main, forMode: .default)
            socket.inputStream.open()

            socket.outputStream.delegate = self
            socket.outputStream.schedule(in: RunLoop.main, forMode: .default)
            socket.outputStream.open()

            _ = socket.outputStream.write(endpointID.data)
        }
    }
    /*
     public func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
     }
     public func peripheral(_ peripheral: CBPeripheral, didWriteValueFor characteristic: CBCharacteristic, error: Error?) {
     }
     */
}

extension NearbyEndpoint: StreamDelegate {
    public func stream(_ stream: Stream, handle eventCode: Stream.Event) {
        switch eventCode {
        case Stream.Event.openCompleted:
            print("Stream is open")
        case Stream.Event.endEncountered:
            print("Stream encountered end")
        case Stream.Event.hasBytesAvailable:
            print("Stream has bytes available")
        // receive
        case Stream.Event.hasSpaceAvailable:
            print("Stream has space available")
        // send
        case Stream.Event.errorOccurred:
            print("Stream error occurred")
        default:
            print("Unknown stream event")
        }
    }
}
