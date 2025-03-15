import Foundation
import Capacitor

@objc public class RequestConnectionOptions: NSObject {
    private var endpointID: String?
    private var endpointName: String?

    init(_ call: CAPPluginCall) {
        self.endpointID = call.getString("endpointID")
        self.endpointName = call.getString("endpointName")
    }

    func getEndpointID() -> String? {
        return endpointID
    }
    func getEndpointName() -> String? {
        return endpointName
    }
}
