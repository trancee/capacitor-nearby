import Foundation

public enum PayloadTransferUpdateStatus: String, Codable {
    case success
    case canceled
    case failure
    case inProgress// (Progress)

    func toString() -> String {
        return self.rawValue
    }
}

extension PayloadTransferUpdateStatus {
    static func fromString(_ status: String) -> PayloadTransferUpdateStatus? {
        switch status {
        case "success": return .success
        case "canceled": return .canceled
        case "failure": return .failure
        case "inProgress": return .inProgress
        default: return nil
        }
    }
}
