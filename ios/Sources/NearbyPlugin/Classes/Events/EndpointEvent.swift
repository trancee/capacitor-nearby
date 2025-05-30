import Foundation
import Capacitor

@objc public class EndpointEvent: NSObject {
    let endpoint: Endpoint

    init(_ endpoint: Endpoint) {
        self.endpoint = endpoint
    }

    public func toJSObject() -> JSObject {
        var result = JSObject()

        result["endpointID"] = endpoint.endpointID

        if let endpointName = endpoint.endpointName {
            if !endpointName.isEmpty {
                result["endpointName"] = endpointName
            }
        }

        if let endpointInfo = endpoint.endpointInfo {
            if !endpointInfo.isEmpty {
                result["endpointInfo"] = endpointInfo.base64EncodedString()
            }
        }

        if let rssi = endpoint.rssi {
            result["rssi"] = rssi
        }
        if let power = endpoint.power {
            result["power"] = power
        }
        if let distance = endpoint.distance {
            result["distance"] = distance
        }

        return result
    }
}
