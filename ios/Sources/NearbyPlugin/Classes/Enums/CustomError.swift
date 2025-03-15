import Foundation

public enum CustomError: Error {
    case endpointIDMissing
    case endpointIDUnknown
    case endpointNameMissing

    case serviceIDMissing
    case strategyMissing
    case strategyUnknown

    case payloadIDMissing
    case payloadMissing

    case initializationError
    case advertiserError
    case discovererError
}

extension CustomError: LocalizedError {
    public var errorDescription: String? {
        switch self {
        case .endpointIDMissing:
            return NSLocalizedString("missing endpoint identifier", comment: "endpointIDMissing")
        case .endpointIDUnknown:
            return NSLocalizedString("unknown endpoint identifier", comment: "endpointIDUnknown")
        case .endpointNameMissing:
            return NSLocalizedString("missing endpoint name", comment: "endpointNameMissing")

        case .serviceIDMissing:
            return NSLocalizedString("missing service identifier", comment: "serviceIDMissing")
        case .strategyMissing:
            return NSLocalizedString("missing strategy", comment: "strategyMissing")
        case .strategyUnknown:
            return NSLocalizedString("unknown strategy", comment: "strategyUnknown")

        case .payloadIDMissing:
            return NSLocalizedString("missing payload identifier", comment: "payloadIDMissing")
        case .payloadMissing:
            return NSLocalizedString("missing payload", comment: "payloadMissing")

        case .initializationError:
            return NSLocalizedString("initialization error", comment: "initializationError")
        case .advertiserError:
            return NSLocalizedString("advertiser error", comment: "advertiserError")
        case .discovererError:
            return NSLocalizedString("discoverer error", comment: "discovererError")
        }
    }
}
