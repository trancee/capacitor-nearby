import Foundation
import Capacitor

@objc public class PayloadReceivedEvent: EndpointEvent {
    let payload: Data

    init(_ endpointID: EndpointID, payload: Data) {
        self.payload = payload

        super.init(endpointID)
    }

    override public func toJSObject() -> JSObject {
        var result = super.toJSObject()

        result["payload"] = payload.base64EncodedString()

        return result
    }
}
