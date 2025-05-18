//
//  NearbyScanner.swift
//  Plugin
//
//  Created by Philipp Grosswiler on 02.04.2025.
//

import CoreBluetooth

public typealias ScanCallback = (ScanResult) -> Void

public enum ScanResult {
    case started
    case stopped(_ error: Error? = nil)

    case found(_ uuid: CBUUID, name: String? = nil, info: Data? = nil, channel: Short? = nil, rssi: NSNumber? = nil, _ device: CBPeripheral)
}

public final class NearbyScanner: NSObject {
    // An object that scans for, discovers, connects to, and manages peripherals.
    private var centralManager: CBCentralManager!
    private var peripheral: CBPeripheral!
    private var l2capChannel: CBL2CAPChannel?

    private var callback: ScanCallback?

    private var timer: Timer?

    private static var serviceUUID: CBUUID?

    private static var stateCallback: StateCallback?

    // private static var beaconCallback: BeaconCallback?
    // private static var beacons: [CBUUID: Beacon] = [:]

    init(_ serviceUUID: UUID,
         stateCallback: @escaping StateCallback) {
        super.init()

        NearbyScanner.serviceUUID = CBUUID(nsuuid: serviceUUID)

        NearbyScanner.stateCallback = stateCallback

        // Keys used to pass options when initializing a central manager.
        let options: [String: Any] = [
            // A Boolean value that specifies whether the system warns the user if the app instantiates the central manager when Bluetooth service isn’t available.
            CBCentralManagerOptionShowPowerAlertKey: true
        ]

        self.centralManager = CBCentralManager(delegate: self, queue: nil, options: options)
        self.callback = nil

        self.timer = nil

        // clearBeacons()
    }

    deinit {
        stop()
    }
}

extension NearbyScanner {
    // Start scanning for peripherals
    public func start(
        callback: @escaping ScanCallback) {
        self.callback = callback

        stop()

        guard let centralManager = self.centralManager else {
            if let callback = self.callback {
                callback(.stopped())
            }

            return
        }

        // Keys used to pass options when scanning for peripherals.
        let options: [String: Any] = [
            // A Boolean value that specifies whether the scan should run without duplicate filtering.
            CBCentralManagerScanOptionAllowDuplicatesKey: true
        ]

        // Scans for peripherals that are advertising services.
        centralManager.scanForPeripherals(
            withServices: [
                NearbyScanner.serviceUUID!
            ],

            options: options
        )

        if let callback = self.callback {
            callback(.started)
        }

        //        if let ttlSeconds = ttlSeconds {
        //            startTimer(TimeInterval(ttlSeconds))
        //        }
    }

    public func stop(_ error: Error? = nil) {
        // stopTimer()

        if let centralManager = self.centralManager {
            // Asks the central manager to stop scanning for peripherals.
            centralManager.stopScan()
        }

        // clearBeacons()

        if let callback = self.callback {
            callback(.stopped(error))
        }
    }

    public func isScanning() -> Bool {
        guard let centralManager = self.centralManager else { return false }

        // A Boolean value that indicates whether the central is currently scanning.
        return centralManager.isScanning
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

// A protocol that provides updates for the discovery and management of peripheral devices.
extension NearbyScanner: CBCentralManagerDelegate {
    public func bluetoothState() -> StateResult {
        switch self.centralManager.state {
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
        @unknown default:
            (.unknown)
        }
    }

    // Tells the delegate the central manager’s state updated.
    public func centralManagerDidUpdateState(_ central: CBCentralManager) {
        if let callback = NearbyScanner.stateCallback {
            switch central.state {
            case .unknown:
                callback(.unknown)
            case .resetting:
                stop()
                callback(.resetting)
            case .unsupported:
                callback(.unsupported)
            case .unauthorized:
                callback(.unauthorized)
            case .poweredOff:
                stop()
                callback(.poweredOff)
            case .poweredOn:
                callback(.poweredOn)
            @unknown default:
                callback(.unknown)
            }
        }
    }

    // Tells the delegate the central manager discovered a peripheral while scanning for devices.
    public func centralManager(_ central: CBCentralManager,
                               didDiscover peripheral: CBPeripheral,
                               advertisementData: [String: Any],
                               rssi RSSI: NSNumber) {
        let rssi = RSSI.intValue != Int8.max ? RSSI : nil

        peripheral.delegate = self

        if let advertisementDataServiceUUIDs = advertisementData[CBAdvertisementDataServiceUUIDsKey] as? [CBUUID] {
            var id: CBUUID?
            var name: String? = peripheral.name
            var info: Data?
            var channel: Short?

            for uuid in advertisementDataServiceUUIDs {
                if uuid == NearbyScanner.serviceUUID {
                    continue
                }

                if id == nil {
                    id = uuid
                    continue
                }

                if info == nil {
                    let size = uuid.data[0]

                    info = Data(uuid.data[1..<(size & 0x7F)+1])

                    if size & 0x80 != 0 {
                        channel = Short(uuid.data[Int(size & 0x7F)+1])
                    }

                    break
                }

                //                if let beacon = NearbyScanner.beacons[uuid] {
                //                    beacon.alive()
                //                } else {
                //                    NearbyScanner.beacons[uuid] = Beacon(uuid, rssi: rssi)
                //
                //                    if let beaconCallback = NearbyScanner.beaconCallback {
                //                        beaconCallback(.found(uuid, rssi: rssi))
                //                    }
                //                }
            }

            if let callback = self.callback {
                callback(.found(id!, name: name, info: info, channel: channel, rssi: rssi, peripheral))
            }
        }

        //        if let advertisementDataServiceData = advertisementData[CBAdvertisementDataServiceDataKey] as? [CBUUID: Data] {
        //            for (uuid, _) in advertisementDataServiceData {
        //                if uuid == NearbyScanner.serviceUUID {
        //                    continue
        //                }
        //
        //                if let beacon = NearbyScanner.beacons[uuid] {
        //                    beacon.alive()
        //                } else {
        //                    NearbyScanner.beacons[uuid] = Beacon(uuid, rssi: rssi)
        //
        //                    if let beaconCallback = NearbyScanner.beaconCallback {
        //                        beaconCallback(.found(uuid, rssi: rssi))
        //                    }
        //                }
        //            }
        //        }
    }
}

extension NearbyScanner: CBPeripheralDelegate {
    //    private static let ttlSeconds: TimeInterval = 10
    //
    //    public func clearBeacons() {
    //        NearbyScanner.beacons = [:]
    //    }
    //
    //    public func getBeacons() -> [String] {
    //        var result: [String] = []
    //
    //        for key in NearbyScanner.beacons.keys {
    //            result.append(key.uuidString.lowercased())
    //        }
    //
    //        return result
    //    }
    //
    //    public final class Beacon {
    //        let uuid: CBUUID
    //        let rssi: NSNumber?
    //
    //        let timestamp: Date
    //
    //        private var timer: Timer?
    //
    //        private var lastSeen: Date
    //
    //        init(_ uuid: CBUUID, rssi: NSNumber? = nil) {
    //            self.uuid = uuid
    //            self.rssi = rssi
    //
    //            self.timestamp = Date()
    //
    //            self.lastSeen = Date()
    //
    //            startTimer(NearbyScanner.ttlSeconds)
    //
    //            NearbyScanner.beacons[self.uuid] = self
    //        }
    //        deinit {
    //            kill()
    //        }
    //
    //        public func kill() {
    //            stopTimer()
    //
    //            NearbyScanner.beacons[self.uuid] = nil
    //        }
    //
    //        public func alive() {
    //            self.lastSeen = Date()
    //
    //            stopTimer()
    //
    //            if NearbyScanner.beacons[self.uuid] != nil {
    //                startTimer(NearbyScanner.ttlSeconds)
    //            }
    //        }
    //
    //        private func startTimer(_ timeout: TimeInterval) {
    //            stopTimer()
    //
    //            self.timer = Timer.scheduledTimer(
    //                timeInterval: timeout,
    //                target: self,
    //                selector: #selector(self.onTimer),
    //                userInfo: nil,
    //                repeats: false)
    //        }
    //
    //        private func stopTimer() {
    //            if let timer = self.timer {
    //                if timer.isValid { timer.invalidate() }
    //
    //                self.timer = nil
    //            }
    //        }
    //
    //        @objc fileprivate func onTimer(_ timer: Timer) {
    //            kill()
    //
    //            if let beaconCallback = NearbyScanner.beaconCallback {
    //                beaconCallback(.lost(self.uuid, rssi: self.rssi))
    //            }
    //        }
    //    }

    func setupL2CAPChannel(_ psm: UInt16) {
        if let peripheral {
            // Attempt to open an L2CAP channel to the peripheral using the supplied PSM.
            peripheral.openL2CAPChannel(psm)
        }
    }
}
