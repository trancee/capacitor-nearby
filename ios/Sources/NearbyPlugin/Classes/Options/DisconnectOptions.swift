import Foundation
import Capacitor

@objc public class DisconnectOptions: NSObject {
    private var endpointID: String?

    init(_ call: CAPPluginCall) {
        self.endpointID = call.getString("endpointID")
    }

    func getEndpointID() -> String? {
        return endpointID
    }
}
