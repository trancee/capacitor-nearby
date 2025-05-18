import Foundation
import Capacitor

@objc public class RequestPermissionsOptions: NSObject {
    private var permissions: [String]?

    init(_ call: CAPPluginCall) {
        if let permissions = call.getArray("permissions") {
            for permission in permissions {
                self.permissions?.append(permission as! String)
            }
        }
    }

    func getPermissions() -> [String]? {
        return permissions
    }
}
