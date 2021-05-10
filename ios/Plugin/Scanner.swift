//
//  Scanner.swift
//  Plugin
//
//  Created by Philipp Grosswiler on 5/8/21.
//  Copyright © 2021 Max Lynch. All rights reserved.
//

import CoreBluetooth

public typealias ScanCallback = (_ result: Result<ScanResult, Error>) -> Void

public enum ScanResult {
    case started
    case stopped(error: Error?)
    case expired
}

public typealias BeaconCallback = (BeaconResult) -> Void

public enum BeaconResult {
    case found(_ uuid: CBUUID, data: Data?, rssi: NSNumber?)
    case lost(_ uuid: CBUUID, data: Data?, rssi: NSNumber?)
}

public final class Scanner: NSObject {
    // An object that scans for, discovers, connects to, and manages peripherals.
    private var centralManager: CBCentralManager?
    private var callback: ScanCallback?

    private var timer: Timer?
    
    private static var beaconCallback: BeaconCallback?
    private static var beacons: [CBUUID: Beacon] = [:]
    
    init(beaconCallback: @escaping BeaconCallback) {
        super.init()

        Scanner.beaconCallback = beaconCallback

        // Keys used to pass options when initializing a central manager.
        let options: [String : Any] = [
            // A Boolean value that specifies whether the system warns the user if the app instantiates the central manager when Bluetooth service isn’t available.
            CBCentralManagerOptionShowPowerAlertKey: true,
        ]

        centralManager = CBCentralManager(delegate: self, queue: nil, options: options)
        callback = nil
        
        timer = nil
        
        Scanner.beacons = [:]
    }
    deinit {
        stopTimer()
    }
}

extension Scanner {
    // Start scanning for peripherals
    public func start(
        timeout: TimeInterval?,
        completion: @escaping ScanCallback)
    {
        self.callback = completion
        
        stopTimer()
        
        if (isScanning()) {
            stop()
        }
        
        // Keys used to pass options when scanning for peripherals.
        let options: [String : Any] = [
            // A Boolean value that specifies whether the scan should run without duplicate filtering.
            CBCentralManagerScanOptionAllowDuplicatesKey: true,
        ]

        // Scans for peripherals that are advertising services.
        centralManager?.scanForPeripherals(
            withServices: [
                Constants.SERVICE_UUID,
            ],
            
            options: options
        )

        if let callback = callback {
            callback(.success(.started))
        }

        if let timeout = timeout {
            startTimer(timeout)
        }
    }

    public func stop()
    {
        // Asks the central manager to stop scanning for peripherals.
        centralManager?.stopScan()

        if let callback = callback {
            callback(.success(.stopped(error: nil)))
        }
    }
    
    public func isScanning() -> Bool
    {
        // A Boolean value that indicates whether the central is currently scanning.
        return ((centralManager?.isScanning) != nil)
    }
    
    private func startTimer(_ timeout: TimeInterval) {
        stopTimer()

        self.timer = Timer.scheduledTimer(
            timeInterval: timeout,
            target: self,
            selector: #selector(self.onTimer),
            userInfo: nil,
            repeats: false)
    }
    
    private func stopTimer() {
        if let timer = self.timer {
            if timer.isValid { timer.invalidate() }
            
            self.timer = nil
        }
    }

    @objc fileprivate func onTimer(_ timer: Timer) {
        stopTimer()
        
        if let callback = callback {
            callback(.success(.expired))
        }
    }
}

// A protocol that provides updates for the discovery and management of peripheral devices.
extension Scanner: CBCentralManagerDelegate {
    // Tells the delegate the central manager’s state updated.
    public func centralManagerDidUpdateState(_ central: CBCentralManager) {
        guard central.state == .poweredOn else { return }
    }
    
    // Tells the delegate the central manager discovered a peripheral while scanning for devices.
    public func centralManager(_ central: CBCentralManager,
                               didDiscover peripheral: CBPeripheral,
                               advertisementData: [String: Any],
                               rssi RSSI: NSNumber) {
        let rssi = RSSI.intValue != Int8.max ? RSSI : nil

        if let advertisementDataServiceUUIDs = advertisementData[CBAdvertisementDataServiceUUIDsKey] as? Array<CBUUID> {
            for uuid in advertisementDataServiceUUIDs {
                if uuid == Constants.SERVICE_UUID {
                    continue
                }

                if let beacon = Scanner.beacons[uuid] {
                    beacon.alive()
                } else {
                    Scanner.beacons[uuid] = Beacon(uuid, data: nil, rssi: rssi)

                    if let beaconCallback = Scanner.beaconCallback {
                        beaconCallback(.found(uuid, data: nil, rssi: rssi))
                    }
                }
            }
        }

        if let advertisementDataServiceData = advertisementData[CBAdvertisementDataServiceDataKey] as? [CBUUID: Data] {
            for (uuid, data) in advertisementDataServiceData {
                if uuid == Constants.SERVICE_UUID {
                    continue
                }

                if let beacon = Scanner.beacons[uuid] {
                    beacon.alive()
                } else {
                    Scanner.beacons[uuid] = Beacon(uuid, data: data, rssi: rssi)

                    if let beaconCallback = Scanner.beaconCallback {
                        beaconCallback(.found(uuid, data: data, rssi: rssi))
                    }
                }
            }
        }
    }
}

extension Scanner {
    private static let ttlSeconds: TimeInterval? = 10

    public final class Beacon {
        let uuid: CBUUID
        let data: Data?
        let rssi: NSNumber?

        let timestamp: Date

        private var timer: Timer?

        private var lastSeen: Date

        init(_ uuid: CBUUID, data: Data?, rssi: NSNumber?) {
            self.uuid = uuid
            self.data = data
            self.rssi = rssi
            
            self.timestamp = Date()

            self.lastSeen = Date()
            
            if let ttlSeconds = ttlSeconds {
                startTimer(ttlSeconds)
            }

            Scanner.beacons[self.uuid] = self
        }
        deinit {
            stopTimer()
        }

        public func kill() {
            stopTimer()
            
            Scanner.beacons[self.uuid] = nil
        }

        public func alive() {
            self.lastSeen = Date()
            
            stopTimer()
            
            if Scanner.beacons[self.uuid] != nil {
                if let ttlSeconds = ttlSeconds {
                    startTimer(ttlSeconds)
                }
            }
        }

        private func startTimer(_ timeout: TimeInterval) {
            stopTimer()

            self.timer = Timer.scheduledTimer(
                timeInterval: timeout,
                target: self,
                selector: #selector(self.onTimer),
                userInfo: nil,
                repeats: false)
        }
        
        private func stopTimer() {
            if let timer = self.timer {
                if timer.isValid { timer.invalidate() }
                
                self.timer = nil
            }
        }

        @objc fileprivate func onTimer(_ timer: Timer) {
            stopTimer()
            
            if let beaconCallback = Scanner.beaconCallback {
                beaconCallback(.lost(self.uuid, data: self.data, rssi: self.rssi))
            }
        }
    }
}
