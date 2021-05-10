import Foundation
import Capacitor
import CoreBluetooth

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
struct Constants {
    static let SERVICE_UUID = CBUUID(string: "1c2cceae-0000-1000-8000-00805f9b34fb")
    
    static let BLUETOOTH_NOT_SUPPORTED = "Bluetooth not supported"
    static let BLE_NOT_SUPPORTED = "Bluetooth Low Energy not supported"
    static let NOT_INITIALIZED = "not initialized"
    static let PERMISSION_DENIED = "permission denied"
    
    static let UUID_NOT_FOUND = "UUID not found"
}

@objc(Nearby)
public class Nearby: CAPPlugin {
    private var scanner: Scanner!
    private var scanTimeout: TimeInterval?
    
    private var advertiser: Advertiser!
    private var advertiseTimeout: TimeInterval?
    
    private var uuid: CBUUID?
    private var data: Data?
    
    /**
     * Public functions
     */
    @objc func initialize(_ call: CAPPluginCall) {
        self.scanner = Scanner() { [self] result in
            switch result {
            case .found(let uuid, let data, let rssi):
                if (self.scanner.isScanning()) {
                    var jsData: [String : Any] = [
                        "uuid": uuid.uuidString.lowercased(),
                    ]
                    
                    if let data = data {
                        jsData["content"] = Data(base64Encoded: data)
                    }
                    if let rssi = rssi {
                        jsData["rssi"] = rssi.stringValue
                    }
                    
                    notifyListeners("onFound", data: jsData)
                }
                
                break
            case .lost(let uuid, let data, let rssi):
                if (self.scanner.isScanning()) {
                    var jsData: [String : Any] = [
                        "uuid": uuid.uuidString.lowercased(),
                    ]
                    
                    if let data = data {
                        jsData["content"] = Data(base64Encoded: data)
                    }
                    if let rssi = rssi {
                        jsData["rssi"] = rssi.stringValue
                    }
                    
                    notifyListeners("onLost", data: jsData)
                }
                
                break
            }
        }
        self.scanTimeout = nil
        
        self.advertiser = Advertiser()
        self.advertiseTimeout = nil
        
        self.uuid = nil
        self.data = nil
        
        call.success()
    }
    @objc func reset(_ call: CAPPluginCall) {
        stop()
        
        self.scanTimeout = nil
        self.advertiseTimeout = nil
        
        self.uuid = nil
        self.data = nil
        
        call.success()
    }
    
    @objc func publish(_ call: CAPPluginCall) {
        if let messageObject = call.getObject("message") {
            guard let messageUUID = messageObject["uuid"] as? String else {
                call.reject(Constants.UUID_NOT_FOUND)
                return
            }
            
            if (messageUUID.count > 0) {
                self.uuid = CBUUID(string: messageUUID)
            }
            
            guard let content = messageObject["content"] as? String else {
                call.reject(Constants.UUID_NOT_FOUND)
                return
            };
            
            if (content.count > 0) {
                self.data = Data(base64Encoded: content)
            }
        }
        
        self.advertiseTimeout = nil
        if let optionsObject = call.getObject("options") {
            self.advertiseTimeout = optionsObject["ttlSeconds"] as? TimeInterval
        }
        
        if (!self.advertiser.isAdvertising()) {
            let beacon = Advertiser.Beacon(self.uuid!, data: self.data)
            
            advertiser.start(beacon, timeout: self.advertiseTimeout) { result in
                switch result {
                case .success(let result):
                    switch result {
                    case .started:
                        call.success()
                        
                        break
                    case .stopped(let e):
                        if let e = e {
                            call.error(e.localizedDescription, e)
                        }
                        
                        break
                    case .expired:
                        self.publishExpired()
                        
                        break
                    }
                    
                    break
                case .failure(let e):
                    call.error(e.localizedDescription, e)
                    
                    break
                }
            }
        } else {
            call.success()
        }
    }
    @objc func unpublish(_ call: CAPPluginCall) {
        advertiser.stop()
        
        self.advertiseTimeout = nil
        
        self.uuid = nil
        self.data = nil
        
        call.success()
    }
    
    @objc func subscribe(_ call: CAPPluginCall) {
        self.scanTimeout = nil
        if let optionsObject = call.getObject("options") {
            self.scanTimeout = optionsObject["ttlSeconds"] as? TimeInterval
        }
        
        if (!self.scanner.isScanning()) {
            scanner.start(timeout: self.scanTimeout) { result in
                switch result {
                case .success(let result):
                    switch result {
                    case .started:
                        call.success()
                        
                        break
                    case .stopped(let e):
                        if let e = e {
                            call.error(e.localizedDescription, e)
                        }
                        
                        break
                    case .expired:
                        self.subscribeExpired()
                        
                        break
                    }
                    
                    break
                case .failure(let e):
                    call.error(e.localizedDescription, e)
                    
                    break
                }
            }
        } else {
            call.success()
        }
    }
    @objc func unsubscribe(_ call: CAPPluginCall) {
        scanner.stop()
        
        self.scanTimeout = nil
        
        call.success()
    }
    
    @objc func pause(_ call: CAPPluginCall) {
        stop()
        
        call.success()
    }
    @objc func resume(_ call: CAPPluginCall) {
        start(call);
        
        call.success()
    }
    
    @objc func status(_ call: CAPPluginCall) {
        let isPublishing = advertiser.isAdvertising()
        let isSubscribing = scanner.isScanning()
        
        call.success([
            "isPublishing": isPublishing,
            "isSubscribing": isSubscribing,
        ])
    }
    
    /**
     * Private functions
     */
    private func start(_ call: CAPPluginCall) {
        if (self.uuid != nil) {
            let beacon = Advertiser.Beacon(self.uuid!, data: self.data)
            
            advertiser.start(beacon, timeout: self.advertiseTimeout) { result in
                switch result {
                case .success(let result):
                    switch result {
                    case .started:
                        call.success()
                        
                        break
                    case .stopped(let e):
                        if let e = e {
                            call.error(e.localizedDescription, e)
                        }
                        
                        break
                    case .expired:
                        self.publishExpired()
                        
                        break
                    }
                    
                    break
                case .failure(let e):
                    call.error(e.localizedDescription, e)
                    
                    break
                }
            }
        }
        
        scanner.start(timeout: self.scanTimeout) { result in
            switch result {
            case .success(let result):
                switch result {
                case .started:
                    call.success()
                    
                    break
                case .stopped(let e):
                    if let e = e {
                        call.error(e.localizedDescription, e)
                    }
                    
                    break
                case .expired:
                    self.subscribeExpired()
                    
                    break
                }
                
                break
            case .failure(let e):
                call.error(e.localizedDescription, e)
                
                break
            }
        }
    }
    private func stop() {
        self.scanner.stop()
        
        self.advertiser.stop()
    }
    
    private func publishExpired() {
        if (advertiser.isAdvertising()) {
            notifyListeners("onPublishExpired", data: nil)
        }
        
        advertiser.stop()
    }
    
    private func subscribeExpired() {
        if (scanner.isScanning()) {
            notifyListeners("onSubscribeExpired", data: nil)
        }
        
        scanner.stop()
    }
}
