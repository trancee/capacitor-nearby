import Foundation
import Capacitor

@objc public class StartAdvertisingOptions: NSObject {
    private var endpointName: String?
    private var endpointInfo: Data?

    init(_ call: CAPPluginCall) {
        self.endpointName = call.getString("endpointName")

        if let endpointInfo = call.getString("endpointInfo") {
            self.endpointInfo = Data(base64Encoded: endpointInfo, options: .ignoreUnknownCharacters)
        }
    }

    func setEndpointName(_ endpointName: String?) {
        self.endpointName = endpointName
    }
    func setEndpointInfo(_ endpointInfo: Data?) {
        self.endpointInfo = endpointInfo
    }

    func getEndpointName() -> String? {
        return self.endpointName
    }
    func getEndpointInfo() -> Data? {
        return self.endpointInfo
    }
}
