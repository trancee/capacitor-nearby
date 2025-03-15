import Foundation
import Capacitor

import NearbyConnections

@objc public class SendPayloadOptions: NSObject {
    private var endpointIDs: [EndpointID]?

    private var payload: Data?

    init(_ call: CAPPluginCall) {
        var endpointIDs: [EndpointID] = []

        if let endpointID = call.getString("endpointID") {
            endpointIDs.append(endpointID)
        }

        if let endpointArray = call.getArray("endpointIDs") {
            endpointIDs.append(contentsOf: endpointArray.capacitor.replacingNullValues() as! [EndpointID])
        }

        if endpointIDs.isEmpty {} else {
            self.endpointIDs = endpointIDs
        }

        if let payload = call.getString("payload") {
            self.payload = payload.data(using: .utf8)
        }
    }

    func getEndpointIDs() -> [EndpointID]? {
        return endpointIDs
    }

    func getPayload() -> Data? {
        return payload
    }
}
