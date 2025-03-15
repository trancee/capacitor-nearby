import Foundation
import Capacitor

import NearbyConnections

@objc public class SendPayloadResult: NSObject, Result {
    let payloadID: PayloadID
    let payloadType: PayloadType

    let status: PayloadTransferUpdateStatus

    init(payloadID: PayloadID, payloadType: PayloadType, status: PayloadTransferUpdateStatus) {
        self.payloadID = payloadID
        self.payloadType = payloadType

        self.status = status
    }

    public func toJSObject() -> AnyObject {
        var result = JSObject()

        result["payloadID"] = String(payloadID)
        result["payloadType"] = payloadType.toString()

        result["status"] = status.toString()

        return result as AnyObject
    }
}
