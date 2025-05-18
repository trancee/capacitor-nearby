import Foundation
import Capacitor

@objc public class PayloadReceivedEvent: EndpointEvent {
    let payload: Data

    init(_ endpoint: Endpoint, _ payload: Data) {
        self.payload = payload

        super.init(endpoint)
    }

    override public func toJSObject() -> JSObject {
        var result = super.toJSObject()

        result["payload"] = payload.base64EncodedString()

        return result
    }
}
