import Foundation
import Capacitor

import NearbyConnections

@objc public class PayloadReceivedEvent: EndpointEvent {
    let payloadID: PayloadID

    let payloadType: PayloadType
    let payload: Data

    init(_ endpointID: EndpointID, _ payloadID: PayloadID, payloadType: PayloadType, payload: Data) {
        self.payloadID = payloadID

        self.payloadType = payloadType
        self.payload = payload

        super.init(endpointID)
    }

    override public func toJSObject() -> JSObject {
        var result = super.toJSObject()

        result["payloadID"] = String(payloadID)

        result["payloadType"] = payloadType.toString()
        result["payload"] = payload.base64EncodedString()

        return result
    }
}
