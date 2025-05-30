import Capacitor

public class NearbyConfig {
    private var endpointID: EndpointID?

    private var endpointName: String?
    private var endpointInfo: Data?

    private var serviceID: String?

    init(config: PluginConfig) {
        if let uuid = UIDevice.current.identifierForVendor {
            self.endpointID = EndpointID.fromBytes(uuid.bytes)
        }

        self.endpointName = config.getString("endpointName")
        if let endpointInfo = config.getString("endpointInfo") {
            self.endpointInfo = Data(base64Encoded: endpointInfo, options: .ignoreUnknownCharacters)
        }

        self.serviceID = config.getString("serviceID")
    }

    func setEndpointName(_ endpointName: String?) {
        self.endpointName = endpointName
    }
    func setEndpointInfo(_ endpointInfo: Data?) {
        self.endpointInfo = endpointInfo
    }

    func setServiceID(_ serviceID: String?) {
        self.serviceID = serviceID
    }

    func getEndpointName() -> String? {
        return self.endpointName
    }
    func getEndpointInfo() -> Data? {
        return self.endpointInfo
    }

    func getServiceID() -> String? {
        return self.serviceID
    }

    func getEndpointID() -> EndpointID? {
        return self.endpointID
    }
}
