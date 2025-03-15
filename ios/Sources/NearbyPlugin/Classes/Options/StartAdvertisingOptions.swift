import Foundation
import Capacitor

@objc public class StartAdvertisingOptions: NSObject {
    private var endpointName: String?

    init(_ call: CAPPluginCall) {
        self.endpointName = call.getString("endpointName")
    }

    func getEndpointName() -> String? {
        return endpointName
    }
}
