import Foundation
import Capacitor

import NearbyConnections

@objc public class EndpointInitiatedEvent: EndpointEvent {
    let verificationCode: String

    init(_ endpointID: EndpointID, _ verificationCode: String) {
        self.verificationCode = verificationCode

        super.init(endpointID)
    }

    override public func toJSObject() -> JSObject {
        var result = super.toJSObject()

        result["verificationCode"] = verificationCode

        return result
    }
}
