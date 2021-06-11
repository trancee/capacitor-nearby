//
//  Scanner.swift
//  Plugin
//
//  Created by Philipp Grosswiler on 5/8/21.
//  Copyright © 2021 Max Lynch. All rights reserved.
//

import CoreBluetooth

public typealias ScanCallback = (ScanResult) -> Void

public enum ScanResult {
    case started
    case stopped(_ error: Error? = nil)
    case expired
}

public typealias BeaconCallback = (BeaconResult) -> Void

public enum BeaconResult {
    case found(_ uuid: CBUUID, data: Data? = nil, rssi: NSNumber? = nil)
    case lost(_ uuid: CBUUID, data: Data? = nil, rssi: NSNumber? = nil)
}

public final class Scanner: NSObject {
    // An object that scans for, discovers, connects to, and manages peripherals.
    private var centralManager: CBCentralManager?
    private var callback: ScanCallback?
    
    private var timer: Timer?
    
    private static var serviceUUID: CBUUID?;
    
    private static var stateCallback: StateCallback?
    
    private static var beaconCallback: BeaconCallback?
    private static var beacons: [CBUUID: Beacon] = [:]
    
    init(_ serviceUUID: CBUUID,
         stateCallback: @escaping StateCallback,
         beaconCallback: @escaping BeaconCallback) {
        super.init()
        
        Scanner.serviceUUID = serviceUUID
        
        Scanner.stateCallback = stateCallback
        Scanner.beaconCallback = beaconCallback
        
        // Keys used to pass options when initializing a central manager.
        let options: [String : Any] = [
            // A Boolean value that specifies whether the system warns the user if the app instantiates the central manager when Bluetooth service isn’t available.
            CBCentralManagerOptionShowPowerAlertKey: true,
        ]
        
        self.centralManager = CBCentralManager(delegate: self, queue: nil, options: options)
        self.callback = nil
        
        self.timer = nil
        
        Scanner.beacons = [:]
    }
    deinit {
        stop()
    }
}

extension Scanner {
    // Start scanning for peripherals
    public func start(
        withTimeout timeout: TimeInterval?,
        callback: @escaping ScanCallback)
    {
        self.callback = callback
        
        stop()
        
        guard let centralManager = self.centralManager else {
            if let callback = self.callback {
                callback(.stopped())
            }
            
            return
        }
        
        // Keys used to pass options when scanning for peripherals.
        let options: [String : Any] = [
            // A Boolean value that specifies whether the scan should run without duplicate filtering.
            CBCentralManagerScanOptionAllowDuplicatesKey: true,
        ]
        
        // Scans for peripherals that are advertising services.
        centralManager.scanForPeripherals(
            withServices: [
                Scanner.serviceUUID!,
            ],
            
            options: options
        )
        
        if let callback = self.callback {
            callback(.started)
        }
        
        if let timeout = timeout {
            startTimer(timeout)
        }
    }
    
    public func stop(_ error: Error? = nil)
    {
        stopTimer()
        
        if let centralManager = self.centralManager {
            // Asks the central manager to stop scanning for peripherals.
            centralManager.stopScan()
        }
        
        if let callback = self.callback {
            callback(.stopped(error))
        }
    }
    
    public func isScanning() -> Bool
    {
        guard let centralManager = self.centralManager else { return false }
        
        // A Boolean value that indicates whether the central is currently scanning.
        return centralManager.isScanning
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
        
        if let callback = self.callback {
            callback(.expired)
        }
    }
}

// A protocol that provides updates for the discovery and management of peripheral devices.
extension Scanner: CBCentralManagerDelegate {
    // Tells the delegate the central manager’s state updated.
    public func centralManagerDidUpdateState(_ central: CBCentralManager) {
        if let callback = Scanner.stateCallback {
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
    }
    
    // Tells the delegate the central manager discovered a peripheral while scanning for devices.
    public func centralManager(_ central: CBCentralManager,
                               didDiscover peripheral: CBPeripheral,
                               advertisementData: [String: Any],
                               rssi RSSI: NSNumber) {
        let rssi = RSSI.intValue != Int8.max ? RSSI : nil
        
        if let advertisementDataServiceUUIDs = advertisementData[CBAdvertisementDataServiceUUIDsKey] as? [CBUUID] {
            for uuid in advertisementDataServiceUUIDs {
                if uuid == Scanner.serviceUUID {
                    continue
                }
                
                if let beacon = Scanner.beacons[uuid] {
                    beacon.alive()
                } else {
                    Scanner.beacons[uuid] = Beacon(uuid, rssi: rssi)
                    
                    if let beaconCallback = Scanner.beaconCallback {
                        beaconCallback(.found(uuid, rssi: rssi))
                    }
                }
            }
        }
        
        if let advertisementDataServiceData = advertisementData[CBAdvertisementDataServiceDataKey] as? [CBUUID: Data] {
            for (uuid, data) in advertisementDataServiceData {
                if uuid == Scanner.serviceUUID {
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
    private static let ttlSeconds: TimeInterval = 10
    
    public func getBeacons() -> [CBUUID] {
        return Array(Scanner.beacons.keys)
    }
    
    public final class Beacon {
        let uuid: CBUUID
        let data: Data?
        let rssi: NSNumber?
        
        let timestamp: Date
        
        private var timer: Timer?
        
        private var lastSeen: Date
        
        init(_ uuid: CBUUID, data: Data? = nil, rssi: NSNumber? = nil) {
            self.uuid = uuid
            self.data = data
            self.rssi = rssi
            
            self.timestamp = Date()
            
            self.lastSeen = Date()
            
            startTimer(Scanner.ttlSeconds)
            
            Scanner.beacons[self.uuid] = self
        }
        deinit {
            kill()
        }
        
        public func kill() {
            stopTimer()
            
            Scanner.beacons[self.uuid] = nil
        }
        
        public func alive() {
            self.lastSeen = Date()
            
            stopTimer()
            
            if Scanner.beacons[self.uuid] != nil {
                startTimer(Scanner.ttlSeconds)
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
            kill()
            
            if let beaconCallback = Scanner.beaconCallback {
                beaconCallback(.lost(self.uuid, data: self.data, rssi: self.rssi))
            }
        }
    }
}
