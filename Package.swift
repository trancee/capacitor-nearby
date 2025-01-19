// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorTranceeNearby",
    platforms: [.iOS(.v13)],
    products: [
        .library(
            name: "CapacitorTranceeNearby",
            targets: ["NearbyPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", branch: "main")
    ],
    targets: [
        .target(
            name: "NearbyPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/NearbyPlugin"),
        .testTarget(
            name: "NearbyPluginTests",
            dependencies: ["NearbyPlugin"],
            path: "ios/Tests/NearbyPluginTests")
    ]
)