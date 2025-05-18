import Foundation
import Capacitor

@objc public class PermissionsResult: NSObject, Result {
    let bluetooth: String
    let location: String

    init(bluetooth: String, location: String) {
        self.bluetooth = bluetooth
        self.location = location
    }

    public func toJSObject() -> AnyObject {
        var result = JSObject()

        result["bluetooth"] = bluetooth
        result["location"] = location

        return result as AnyObject
    }
}
