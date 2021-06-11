//
//  Advertiser.swift
//  Plugin
//
//  Created by Philipp Grosswiler on 5/7/21.
//  Copyright © 2021 Max Lynch. All rights reserved.
//

import CoreBluetooth

public typealias AdvertiseCallback = (AdvertiseResult) -> Void

public enum AdvertiseResult {
    case started
    case stopped(_ error: Error? = nil)
    case expired
}

public final class Advertiser: NSObject {
    // An object that manages and advertises peripheral services exposed by this app.
    private var peripheralManager: CBPeripheralManager?
    private var callback: AdvertiseCallback?
    
    private var timer: Timer?
    
    private static var serviceUUID: CBUUID?;
    
    private static var stateCallback: StateCallback?

    init(_ serviceUUID: CBUUID,
         stateCallback: @escaping StateCallback) {
        super.init()
        
        Advertiser.serviceUUID = serviceUUID

        Advertiser.stateCallback = stateCallback

        // Keys used to specify options when creating a peripheral manager.
        let options: [String : Any] = [
            // A Boolean value specifying whether the system should warn if Bluetooth is in the powered-off state when instantiating the peripheral manager.
            CBPeripheralManagerOptionShowPowerAlertKey: true,
        ]

        self.peripheralManager = CBPeripheralManager(delegate: self, queue: nil, options: options)
        self.callback = nil
        
        self.timer = nil
    }
    deinit {
        stop()
    }
}

extension Advertiser {
    // Start advertising this device as a peripheral
    public func start(
        _ beacon: Beacon,
        withTimeout timeout: TimeInterval?,
        callback: @escaping AdvertiseCallback)
    {
        self.callback = callback
        
        stop()
        
        guard let peripheralManager = self.peripheralManager else {
            if let callback = self.callback {
                callback(.stopped())
            }
            
            return
        }
        
        // An optional dictionary containing the data you want to advertise.
        let advertisementData: [String : Any] = [
            // An array of service UUIDs.
            CBAdvertisementDataServiceUUIDsKey: [
                Advertiser.serviceUUID,
                beacon.uuid,
            ],
        ]
        
        // Advertises peripheral manager data.
        peripheralManager.startAdvertising(advertisementData)
        
        if let timeout = timeout {
            startTimer(timeout)
        }
    }
    
    public func stop(_ error: Error? = nil)
    {
        stopTimer()

        if let peripheralManager = self.peripheralManager {
            // Stops advertising peripheral manager data.
            peripheralManager.stopAdvertising()
        }

        if let callback = self.callback {
            callback(.stopped(error))
        }
    }
    
    public func isAdvertising() -> Bool
    {
        guard let peripheralManager = self.peripheralManager else { return false }

        // A Boolean value that indicates whether the peripheral is advertising data.
        return peripheralManager.isAdvertising
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

// A protocol that provides updates for local peripheral state and interactions with remote central devices.
extension Advertiser: CBPeripheralManagerDelegate {
    // Tells the delegate the peripheral manager’s state updated.
    public func peripheralManagerDidUpdateState(_ peripheral: CBPeripheralManager) {
        if let callback = Advertiser.stateCallback {
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
    }
    
    // Tells the delegate the peripheral manager started advertising the local peripheral device’s data.
    public func peripheralManagerDidStartAdvertising(_ peripheral: CBPeripheralManager,
                                                     error: Error?) {
        if let callback = self.callback {
            if let error = error {
                stop(error)
            } else {
                callback(.started)
            }
        }
    }
}

extension Advertiser {
    public final class Beacon {
        let uuid: CBUUID
        let data: Data?
        
        let timestamp: Date
        
        init(_ uuid: CBUUID, data: Data?) {
            self.uuid = uuid
            self.data = data
            
            self.timestamp = Date()
        }
    }
}
