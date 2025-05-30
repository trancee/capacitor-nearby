import Foundation
import Capacitor

@objc public class Endpoint: NSObject {
    let endpointID: EndpointID
    let endpointName: String?
    let endpointInfo: Data?

    let rssi: NSNumber?
    let power: NSNumber?
    let distance: Double?

    init(_ endpointID: EndpointID, endpointName: String? = nil, endpointInfo: Data? = nil, rssi: NSNumber? = nil, power: NSNumber? = nil, distance: Double? = nil) {
        self.endpointID = endpointID
        self.endpointName = endpointName
        self.endpointInfo = endpointInfo

        self.rssi = rssi
        self.power = power
        self.distance = distance
    }
}
