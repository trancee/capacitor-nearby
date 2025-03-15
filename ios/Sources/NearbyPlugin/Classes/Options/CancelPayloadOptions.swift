import Foundation
import Capacitor

import NearbyConnections

@objc public class CancelPayloadOptions: NSObject {
    private var payloadID: PayloadID?

    init(_ call: CAPPluginCall) {
        if let payloadID = call.getString("payloadID") {
            self.payloadID = PayloadID(payloadID)
        }
    }

    func getPayloadID() -> PayloadID? {
        return payloadID
    }
}
