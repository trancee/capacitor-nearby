//
//  Advertiser.swift
//  Plugin
//
//  Created by Philipp Grosswiler on 5/7/21.
//  Copyright © 2021 Max Lynch. All rights reserved.
//

import CoreBluetooth

public typealias AdvertiseCallback = (_ result: Result<AdvertiseResult, Error>) -> Void

public enum AdvertiseResult {
    case started
    case stopped(error: Error?)
    case expired
}

public final class Advertiser: NSObject {
    // An object that manages and advertises peripheral services exposed by this app.
    private var peripheralManager: CBPeripheralManager?
    private var callback: AdvertiseCallback?

    private var timer: Timer?
    
    override init() {
        super.init()

        peripheralManager = CBPeripheralManager(delegate: self, queue: nil)
        callback = nil
        
        timer = nil
    }
    deinit {
        stopTimer()
    }
}

extension Advertiser {
    // Start advertising this device as a peripheral
    public func start(
        _ beacon: Beacon,
        timeout: TimeInterval?,
        completion: @escaping AdvertiseCallback)
    {
        callback = completion
        
        stopTimer()
        
        if (isAdvertising()) {
            stop()
        }
        
        // An optional dictionary containing the data you want to advertise.
        let advertisementData: [String : Any] = [
            // An array of service UUIDs.
            CBAdvertisementDataServiceUUIDsKey: [
                Constants.SERVICE_UUID,
                beacon.uuid,
            ],

            // A dictionary that contains service-specific advertisement data.
//            CBAdvertisementDataServiceDataKey: "",
            
            // The transmit power of a peripheral.
//            CBAdvertisementDataTxPowerLevelKey: "",
            
            // A Boolean value that indicates whether the advertising event type is connectable.
            CBAdvertisementDataIsConnectable: false,
        ]

        // Advertises peripheral manager data.
        peripheralManager?.startAdvertising(advertisementData)
        
        if let timeout = timeout {
            startTimer(timeout)
        }
    }

    public func stop()
    {
        // Stops advertising peripheral manager data.
        peripheralManager?.stopAdvertising()

        if let callback = callback {
            callback(.success(.stopped(error: nil)))
        }
    }
    
    public func isAdvertising() -> Bool
    {
        return ((peripheralManager?.isAdvertising) != nil)
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

// A protocol that provides updates for local peripheral state and interactions with remote central devices.
extension Advertiser: CBPeripheralManagerDelegate {
    // Tells the delegate the peripheral manager’s state updated.
    public func peripheralManagerDidUpdateState(_ peripheral: CBPeripheralManager) {
        guard peripheral.state == .poweredOn else { return }
    }
    
    // Tells the delegate the peripheral manager started advertising the local peripheral device’s data.
    public func peripheralManagerDidStartAdvertising(_ peripheral: CBPeripheralManager,
                                                       error: Error?) {
        if let callback = callback {
            if let error = error {
                callback(.failure(error))
            } else {
                callback(.success(.started))
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
            
            self.timestamp = Date() // .timeIntervalSince1970
            // let myTimeInterval = TimeInterval(timestamp)
            // let time = NSDate(timeIntervalSince1970: TimeInterval(myTimeInterval))
        }
    }
}
