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
    case found(_ endpointID: EndpointID, endpointName: String?, endpointInfo: Data?)
    case lost(_ endpointID: EndpointID)
}

class NearbyEndpoint: NSObject {
    let endpointID: EndpointID

    let endpointName: String?
    let endpointInfo: Data?

    let psm: Short?
    let rssi: NSNumber?

    let timestamp: Date

    private var lastSeen: Date

    private var timer: Timer?

    private static let ttlSeconds: TimeInterval = 10

    // private var managerQueue = DispatchQueue.global(qos: .utility)
    // private var peripheralManager: CBPeripheralManager?
    private var centralManager: CBCentralManager?
    private var peripheral: CBPeripheral?

    private var channel: CBL2CAPChannel?
    private var inputStream: InputStream?
    private var outputStream: OutputStream?

    private var callback: EndpointCallback?

    private let queue = DispatchQueue(label: "NearbyEndpoint")

    init(_ endpointID: EndpointID, endpointName: String?, endpointInfo: Data?, psm: Short?, rssi: NSNumber? = nil, _ peripheral: CBPeripheral, callback: @escaping EndpointCallback) {
        self.endpointID = endpointID

        self.endpointName = endpointName
        self.endpointInfo = endpointInfo

        self.psm = psm
        self.rssi = rssi

        self.timestamp = Date()

        self.lastSeen = Date()

        self.peripheral = peripheral

        self.callback = callback

        super.init()

        self.centralManager = CBCentralManager()

        //        self.peripheralManager = CBPeripheralManager(delegate: nil, queue: managerQueue)
        //        self.peripheralManager?.delegate = self

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

    func lost() {
        kill()

        if let callback {
            callback(.lost(self.endpointID))
        }
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
            // The PSM of the channel to open
            if let psm {
                // Attempt to open an L2CAP channel to the peripheral using the supplied PSM.
                peripheral.openL2CAPChannel(psm)
            }
        }
    }

    func disconnect() throws {
        if let peripheral {
            if let centralManager {
                centralManager.cancelPeripheralConnection(peripheral)
            }
        }

        if let inputStream {
            inputStream.close()
            inputStream.remove(from: .main, forMode: .default)
            inputStream.delegate = nil
        }

        if let outputStream {
            outputStream.close()
            outputStream.remove(from: .main, forMode: .default)
            outputStream.delegate = nil
        }

        self.channel = nil
        self.inputStream = nil
        self.outputStream = nil
    }

    func sendPayload(_ payload: Data) throws {
        if let inputStream, let outputStream {
            guard outputStream.hasSpaceAvailable else {
                throw CustomError.spaceNotAvailable
            }

            let length = payload.count

            if length >= MAXIMUM_PAYLOAD_SIZE {
                throw CustomError.payloadTooLarge
            }

            let checksum = payload.crc32()

            // 1. Identity
            print("sendPayload::endpointID", endpointID)
            outputStream.write(endpointID.data)

            // 2. Payload Length
            print("sendPayload::length", length)
            outputStream.write(
                Data([
                    (UInt8) ((length) & 0xff),
                    (UInt8) ((length >> 8) & 0xff),
                    (UInt8) ((length >> 16) & 0xff)
                ])
            )

            // 3. Payload
            print("sendPayload::payload", payload)
            outputStream.write(payload)

            // 4. Checksum
            print("sendPayload::checksum", checksum)
            outputStream.write(
                Data([
                    (UInt8) ((checksum) & 0xff),
                    (UInt8) ((checksum >> 8) & 0xff),
                    (UInt8) ((checksum >> 16) & 0xff),
                    (UInt8) ((checksum >> 24) & 0xff)
                ])
            )

            // A Boolean value that indicates whether the receiver has bytes available to read.
            for i in 1...10 {
                print("sendPayload::hasBytesAvailable", i)

                if inputStream.hasBytesAvailable {
                    break
                }

                sleep(1)
            }

            // 5. (N)ACK
            let ok = inputStream.read()
            print("sendPayload::checksum ok", ok)
            if !(ok > 0) {
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
        lost()
    }
}

// extension NearbyEndpoint: CBCentralManagerDelegate {
//    func centralManagerDidUpdateState(_ central: CBCentralManager) {
//        print("NearbyEndpoint::centralManagerDidUpdateState", central.state)
//
//        if central.state == .poweredOn {
//            print("NearbyEndpoint::connect", peripheral)
//            if let peripheral {
//                if let centralManager {
//                    centralManager.connect(peripheral, options: nil)
//                }
//            }
//        }
//    }
//
//    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
//        print("NearbyEndpoint::didConnect", peripheral)
//
//        if let psm {
//            // Attempt to open an L2CAP channel to the peripheral using the supplied PSM.
//            peripheral.openL2CAPChannel(psm)
//        }
//    }
//
//    func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?) {
//        print("NearbyEndpoint::didFailToConnect", peripheral, error)
//    }
//
//    func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
//        print("NearbyEndpoint::didDisconnectPeripheral", peripheral, error)
//    }
// }

extension NearbyEndpoint: CBPeripheralManagerDelegate {
    func peripheralManagerDidUpdateState(_ peripheral: CBPeripheralManager) {
        print("NearbyEndpoint::peripheralManagerDidUpdateState", peripheral.state)
    }
}

extension NearbyEndpoint: CBPeripheralDelegate {
    func peripheral(_ peripheral: CBPeripheral, didOpen channel: CBL2CAPChannel?, error: Error?) {
        print("NearbyEndpoint::didOpen", peripheral, socket, error)
        self.channel = channel

        if let channel {
            self.inputStream = channel.inputStream

            if let inputStream {
                inputStream.delegate = self
                inputStream.schedule(in: .main, forMode: .default)
                inputStream.open()
            }

            self.outputStream = channel.outputStream

            if let outputStream {
                outputStream.delegate = self
                outputStream.schedule(in: .main, forMode: .default)
                outputStream.open()
            }
        }
    }
}

extension NearbyEndpoint: StreamDelegate {
    func stream(_ stream: Stream, handle eventCode: Stream.Event) {
        // print("NearbyEndpoint::stream")
        switch eventCode {
        case .openCompleted:
            print("Stream is open", stream == inputStream ? "(Input)" : stream == outputStream ? "(Output)" : "")

            if let callback, stream == outputStream {
                callback(.found(self.endpointID, endpointName: self.endpointName, endpointInfo: self.endpointInfo))
            }

        case .endEncountered:
            print("Stream encountered end")
            try! self.disconnect()

        // receive
        case .hasBytesAvailable:
            print("Stream has bytes available")

        // send
        case .hasSpaceAvailable:
            print("Stream has space available")

        case .errorOccurred:
            print("Stream Error on Central: \(stream.streamError?.localizedDescription ?? "Unknown error")")
            lost()

        default:
            print("Unknown stream event")
            try! self.disconnect()
        }
    }
}
