import Foundation
import Capacitor

@objc public class StatusResult: NSObject, Result {
    let isAdvertising: Bool
    let isDiscovering: Bool

    init(isAdvertising: Bool, isDiscovering: Bool) {
        self.isAdvertising = isAdvertising
        self.isDiscovering = isDiscovering
    }

    public func toJSObject() -> AnyObject {
        var result = JSObject()

        result["isAdvertising"] = isAdvertising
        result["isDiscovering"] = isDiscovering

        return result as AnyObject
    }
}
