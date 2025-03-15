import Foundation
import Capacitor

import NearbyConnections

@objc public class PayloadTransferUpdateEvent: EndpointEvent {
    let payloadID: PayloadID

    let update: TransferUpdate

    init(_ endpointID: EndpointID, _ payloadID: PayloadID, update: TransferUpdate) {
        self.payloadID = payloadID

        self.update = update

        super.init(endpointID)
    }

    override public func toJSObject() -> JSObject {
        var result = super.toJSObject()

        result["payloadID"] = String(payloadID)

        result["status"] = update.toString()

        switch update {
        case .success, .canceled, .failure:
            break
        case .progress(let progress):
            result["bytesTransferred"] = String(progress.completedUnitCount)
            result["totalBytes"] = String(progress.totalUnitCount)
        }

        return result
    }
}
