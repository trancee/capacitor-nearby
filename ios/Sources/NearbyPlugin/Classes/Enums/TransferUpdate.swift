import Foundation

import NearbyConnections

extension TransferUpdate {
    func toString() -> String {
        switch self {
        case .success:
            return "success"
        case .canceled:
            return "canceled"
        case .failure:
            return "failure"
        case .progress(let progress):
            return "inProgress"
        }
    }
}
