import Foundation
import Capacitor

@objc public class InitializeOptions: NSObject {
    private var endpointName: String?

    private var serviceID: String?
    private var strategy: String?

    private var autoConnect: Bool?
    private var payload: String?

    init(_ call: CAPPluginCall, _ config: NearbyConnectionsConfig) {
        self.endpointName = call.getString("endpointName") ?? config.getEndpointName()

        self.serviceID = call.getString("serviceID") ?? config.getServiceID()
        self.strategy = call.getString("strategy") ?? config.getStrategy()

        self.autoConnect = call.getBool("autoConnect") ?? config.getAutoConnect()
        self.payload = call.getString("payload") ?? config.getPayload()
    }
}
