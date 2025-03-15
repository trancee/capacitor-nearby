import Foundation
import Capacitor

import NearbyConnections

@objc public class EndpointEvent: NSObject {
    let endpointID: EndpointID
    let endpointName: String?

    init(_ endpointID: EndpointID, endpointName: String? = nil) {
        self.endpointID = endpointID
        self.endpointName = endpointName
    }

    public func toJSObject() -> JSObject {
        var result = JSObject()

        result["endpointID"] = endpointID

        if let endpointName = self.endpointName {
            result["endpointName"] = endpointName
        }

        return result
    }
}
