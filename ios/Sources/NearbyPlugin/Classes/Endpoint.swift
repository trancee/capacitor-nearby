import Foundation
import Capacitor

@objc public class Endpoint: NSObject {
    let endpointID: EndpointID
    let endpointName: String?
    let endpointInfo: Data?

    init(_ endpointID: EndpointID, endpointName: String? = nil, endpointInfo: Data? = nil) {
        self.endpointID = endpointID
        self.endpointName = endpointName
        self.endpointInfo = endpointInfo
    }
}
