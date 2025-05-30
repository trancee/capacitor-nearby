import Foundation

public enum CustomError: Error {
    case endpointIDMissing
    case endpointIDUnknown
    case endpointMissing
    case endpointNameMissing

    case serviceIDMissing

    case payloadMissing
    case payloadTooLarge

    case notAcknowledged

    case initializationError
    case advertiserError
    case discovererError

    case dataTooLarge

    case openSettingsError

    case spaceNotAvailable
}

extension CustomError: LocalizedError {
    public var errorDescription: String? {
        switch self {
        case .endpointIDMissing:
            return NSLocalizedString("missing endpoint identifier", comment: "endpointIDMissing")
        case .endpointIDUnknown:
            return NSLocalizedString("unknown endpoint identifier", comment: "endpointIDUnknown")
        case .endpointMissing:
            return NSLocalizedString("missing endpoint", comment: "endpointMissing")
        case .endpointNameMissing:
            return NSLocalizedString("missing endpoint name", comment: "endpointNameMissing")

        case .serviceIDMissing:
            return NSLocalizedString("missing service identifier", comment: "serviceIDMissing")

        case .payloadMissing:
            return NSLocalizedString("missing payload", comment: "payloadMissing")
        case .payloadTooLarge:
            return NSLocalizedString("payload too large", comment: "payloadTooLarge")

        case .notAcknowledged:
            return NSLocalizedString("not acknowledged", comment: "notAcknowledged")

        case .initializationError:
            return NSLocalizedString("initialization error", comment: "initializationError")
        case .advertiserError:
            return NSLocalizedString("advertiser error", comment: "advertiserError")
        case .discovererError:
            return NSLocalizedString("discoverer error", comment: "discovererError")

        case .dataTooLarge:
            return NSLocalizedString("data too large", comment: "dataTooLarge")

        case .openSettingsError:
            return NSLocalizedString("open settings error", comment: "openSettingsError")

        case .spaceNotAvailable:
            return NSLocalizedString("space not available", comment: "spaceNotAvailable")
        }
    }
}
