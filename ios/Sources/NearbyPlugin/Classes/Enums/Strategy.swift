import Foundation

import NearbyConnections

extension Strategy {
    static func fromString(_ strategy: String) -> Strategy? {
        switch strategy {
        case "cluster": return .cluster
        case "star": return .star
        case "pointToPoint": return .pointToPoint
        default: return nil
        }
    }
}
