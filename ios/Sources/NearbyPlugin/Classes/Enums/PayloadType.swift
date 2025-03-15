import Foundation

public enum PayloadType: String, Codable {
    case bytes
    case stream
    case file

    func toString() -> String {
        return self.rawValue
    }
}

extension PayloadType {
    static func fromString(_ payloadType: String) -> PayloadType? {
        switch payloadType {
        case "bytes": return .bytes
        case "stream": return .stream
        case "file": return .file
        default: return nil
        }
    }
}
