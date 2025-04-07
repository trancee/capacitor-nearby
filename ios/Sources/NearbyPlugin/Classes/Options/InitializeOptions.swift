import Foundation
import Capacitor

@objc public class InitializeOptions: NSObject {
    private var endpointName: String?
    private var endpointInfo: Data?

    private var serviceID: String?

    init(_ call: CAPPluginCall, _ config: NearbyConfig) {
        self.endpointName = call.getString("endpointName")
        if self.endpointName != nil && !self.endpointName!.isEmpty {
            config.setEndpointName(self.endpointName)
        }

        self.serviceID = call.getString("serviceID") ?? config.getServiceID()
    }
}
