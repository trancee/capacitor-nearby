import Foundation
import Capacitor

@objc public class InitializeResult: NSObject, Result {
    let endpointID: EndpointID

    init(_ endpointID: EndpointID) {
        self.endpointID = endpointID
    }

    public func toJSObject() -> AnyObject {
        var result = JSObject()

        result["endpointID"] = endpointID

        return result as AnyObject
    }
}
