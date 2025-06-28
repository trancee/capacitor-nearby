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

    case found(_ uuid: CBUUID, name: String? = nil, info: Data? = nil, psm: Short? = nil, rssi: NSNumber? = nil, power: NSNumber? = nil, distance: Double?, _ device: CBPeripheral)
    case lost(_ uuid: CBUUID)
}

public final class NearbyScanner: NSObject {
    // An object that scans for, discovers, connects to, and manages peripherals.
    private var centralManager: CBCentralManager!

    private var callback: ScanCallback?

    private var timer: Timer?

    private static var serviceUUID: CBUUID?

    private static var stateCallback: StateCallback?

    // private let queue = DispatchQueue(label: "NearbyScanner")
    private var endpoints: [UUID: Endpoint] = [:]

    class Endpoint {
        let id: CBUUID
        let psm: CBL2CAPPSM

        init(_ id: CBUUID, _ psm: CBL2CAPPSM) {
            self.id = id
            self.psm = psm
        }
    }

    init(_ serviceUUID: CBUUID,
         stateCallback: @escaping StateCallback) {
        super.init()

        NearbyScanner.serviceUUID = serviceUUID

        NearbyScanner.stateCallback = stateCallback

        // Keys used to pass options when initializing a central manager.
        let options: [String: Any] = [
            // A Boolean value that specifies whether the system warns the user if the app instantiates the central manager when Bluetooth service isn’t available.
            CBCentralManagerOptionShowPowerAlertKey: true
        ]

        self.centralManager = CBCentralManager(delegate: self, queue: nil, options: options)
        self.callback = nil

        self.endpoints = [:]

        self.timer = nil
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
    }

    public func stop(_ error: Error? = nil) {
        // stopTimer()

        if let centralManager = self.centralManager {
            // Asks the central manager to stop scanning for peripherals.
            centralManager.stopScan()
        }

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
        print("NearbyScanner::centralManagerDidUpdateState", central.state)
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

    // https://heraldprox.io/bluetooth/distance
    // Distance = 10 ^ ((Measured Power – RSSI)/(10 * N))
    // Note: The values 0.89976, 7.7095 and 0.111 are the three constants calculated when solving for a best fit curve to our measured data points. YMMV
    func calculateDistance(power: NSNumber?, rssi: NSNumber?) -> Double? {
        guard let rssi = rssi else { return nil }

        let power = 1 // The default calibrated transmit (TX) power on iOS devices is +8 dBm.
        let ratio = Double(exactly:rssi)!/Double(power)

        if ratio < 1.0 {
            return pow(10.0, ratio)
        } else {
            return 0.89976 * pow(ratio, 7.7095) + 0.111
        }
    }
    
    // Tells the delegate the central manager discovered a peripheral while scanning for devices.
    public func centralManager(_ central: CBCentralManager,
                               didDiscover peripheral: CBPeripheral,
                               advertisementData: [String: Any],
                               rssi RSSI: NSNumber) {
        let power = advertisementData[CBAdvertisementDataTxPowerLevelKey] as? NSNumber // containing the transmit power of a peripheral.
        let rssi = RSSI.intValue != Int8.max ? RSSI : nil // current RSSI of peripheral, in dBm.
        let distance = calculateDistance(power: power, rssi: rssi)
        
        if let advertisementDataServiceUUIDs = advertisementData[CBAdvertisementDataServiceUUIDsKey] as? [CBUUID] {
            var id: CBUUID?
            var name: String? = peripheral.name
            var info: Data?
            var psm: Short?

            for uuid in advertisementDataServiceUUIDs {
                if uuid == NearbyScanner.serviceUUID {
                    continue
                }

                if id == nil && uuid.data.count == ENDPOINT_ID_LENGTH {
                    id = uuid
                    continue
                }

                if info == nil {
                    psm = Short(uuid.data[0])
                    
                    info = (uuid.data[1..<16])
                }
            }

            if let endpoint = self.endpoints[peripheral.identifier] {
            } else {
                self.endpoints[peripheral.identifier] = Endpoint(id!, psm!)

                print("connecting to new endpoint", psm)
                centralManager.connect(peripheral, options: nil)
            }

            if let callback = self.callback {
                callback(.found(id!, name: name, info: info, psm: psm, rssi: rssi, power: power, distance: distance, peripheral))
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

    public func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        print("NearbyScanner::didConnect", peripheral)

        if let endpoint = self.endpoints[peripheral.identifier] {
            print("opening channel \(endpoint.psm) to endpoint", endpoint)
            // Attempt to open an L2CAP channel to the peripheral using the supplied PSM.
            peripheral.openL2CAPChannel(endpoint.psm)
        }
    }

    public func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?) {
        print("NearbyScanner::didFailToConnect", peripheral, error)

        self.endpoints[peripheral.identifier] = nil
    }

    public func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        print("NearbyScanner::didDisconnectPeripheral", peripheral, error)

        if let endpoint = self.endpoints[peripheral.identifier] {
            if let callback = self.callback {
                callback(.lost(endpoint.id))
            }
        }

        self.endpoints[peripheral.identifier] = nil
    }
}
