import Foundation
import Capacitor

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(NearbyPlugin)
public class NearbyPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "NearbyPlugin"
    public let jsName = "Nearby"

    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "initialize", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "reset", returnType: CAPPluginReturnPromise),

        CAPPluginMethod(name: "startAdvertising", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stopAdvertising", returnType: CAPPluginReturnPromise),

        CAPPluginMethod(name: "startDiscovering", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stopDiscovering", returnType: CAPPluginReturnPromise),

        CAPPluginMethod(name: "connect", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "disconnect", returnType: CAPPluginReturnPromise),

        CAPPluginMethod(name: "sendPayload", returnType: CAPPluginReturnPromise),

        CAPPluginMethod(name: "status", returnType: CAPPluginReturnPromise),

        CAPPluginMethod(name: "checkPermissions", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestPermissions", returnType: CAPPluginReturnPromise)
    ]

    public let tag = "NearbyPlugin"

    let ENDPOINT_FOUND_EVENT = "onEndpointFound"
    let ENDPOINT_LOST_EVENT = "onEndpointLost"
    let ENDPOINT_CONNECTED_EVENT = "onEndpointConnected"
    let ENDPOINT_DISCONNECTED_EVENT = "onEndpointDisconnected"
    let PAYLOAD_RECEIVED_EVENT = "onPayloadReceived"

    private var implementation: Nearby!
    private var config: NearbyConfig!

    override public func load() {
        super.load()

        self.config = NearbyConfig(config: getConfig())
        self.implementation = Nearby(plugin: self, config: self.config)
    }

    /**
     * Initialize
     */

    @objc func initialize(_ call: CAPPluginCall) {
        let options = InitializeOptions(call, config)

        if let endpointName = call.getString("endpointName") {
            config.setEndpointName(endpointName)
        }

        if let serviceID = call.getString("serviceID") {
            config.setServiceID(serviceID)
        }

        implementation.initialize(options, completion: { result, error in
            if let error = error {
                self.rejectCall(call, error)
            } else if let result = result?.toJSObject() as? JSObject {
                self.resolveCall(call, result)
            }
        })
    }

    /**
     * Reset
     */

    @objc func reset(_ call: CAPPluginCall) {
        implementation.reset(completion: { error in
            if let error = error {
                self.rejectCall(call, error)
            } else {
                self.resolveCall(call, nil)
            }
        })
    }

    /**
     * Advertising
     */

    @objc func startAdvertising(_ call: CAPPluginCall) {
        let options = StartAdvertisingOptions(call)

        implementation.startAdvertising(options, completion: { error in
            if let error = error {
                self.rejectCall(call, error)
            } else {
                self.resolveCall(call, nil)
            }
        })
    }

    @objc func stopAdvertising(_ call: CAPPluginCall) {
        implementation.stopAdvertising(completion: { error in
            if let error = error {
                self.rejectCall(call, error)
            } else {
                self.resolveCall(call, nil)
            }
        })
    }

    /**
     * Discovering
     */

    @objc func startDiscovering(_ call: CAPPluginCall) {
        implementation.startDiscovering(completion: { error in
            if let error = error {
                self.rejectCall(call, error)
            } else {
                self.resolveCall(call, nil)
            }
        })
    }

    @objc func stopDiscovering(_ call: CAPPluginCall) {
        implementation.stopDiscovering(completion: { error in
            if let error = error {
                self.rejectCall(call, error)
            } else {
                self.resolveCall(call, nil)
            }
        })
    }

    /**
     * Connection
     */

    @objc func connect(_ call: CAPPluginCall) {
        let options = ConnectOptions(call)

        implementation.connect(options, completion: { error in
            if let error = error {
                self.rejectCall(call, error)
            } else {
                self.resolveCall(call, nil)
            }
        })
    }

    @objc func disconnect(_ call: CAPPluginCall) {
        let options = DisconnectOptions(call)

        implementation.disconnect(options, completion: { error in
            if let error = error {
                self.rejectCall(call, error)
            } else {
                self.resolveCall(call, nil)
            }
        })
    }

    /**
     * Payload
     */

    @objc func sendPayload(_ call: CAPPluginCall) {
        let options = SendPayloadOptions(call)

        implementation.sendPayload(options, completion: { error in
            if let error = error {
                self.rejectCall(call, error)
            } else {
                self.resolveCall(call, nil)
            }
        })
    }

    /**
     * Status
     */

    @objc func status(_ call: CAPPluginCall) {
        implementation.status(completion: { result, error in
            if let error = error {
                self.rejectCall(call, error)
            } else if let result = result?.toJSObject() as? JSObject {
                self.resolveCall(call, result)
            }
        })
    }

    /**
     * Permissions
     */

    @objc override public func checkPermissions(_ call: CAPPluginCall) {
        implementation.checkPermissions(completion: { result, error in
            if let error = error {
                self.rejectCall(call, error)
            } else if let result = result?.toJSObject() as? JSObject {
                self.resolveCall(call, result)
            }
        })
    }

    @objc override public func requestPermissions(_ call: CAPPluginCall) {
        let options = RequestPermissionsOptions(call)

        implementation.requestPermissions(options, completion: { error in
            if let error = error {
                self.rejectCall(call, error)
            } else {
                self.resolveCall(call, nil)
            }
        })
    }

    /**
     * Events
     */

    /**
     * Called when a remote endpoint is discovered.
     */
    func onEndpointFound(_ endpoint: Endpoint) {
        let event: EndpointFoundEvent = .init(endpoint)
        print(self.ENDPOINT_FOUND_EVENT, event.toJSObject())

        notifyListeners(self.ENDPOINT_FOUND_EVENT, data: event.toJSObject())
    }
    /**
     * Called when a remote endpoint is no longer discoverable.
     */
    func onEndpointLost(_ endpoint: Endpoint) {
        let event: EndpointLostEvent = .init(endpoint)
        print(self.ENDPOINT_FOUND_EVENT, event.toJSObject())

        notifyListeners(self.ENDPOINT_LOST_EVENT, data: event.toJSObject())
    }

    /**
     * Called after both sides have accepted the connection.
     */
    func onEndpointConnected(_ endpoint: Endpoint) {
        let event: EndpointConnectedEvent = .init(endpoint)
        print(self.ENDPOINT_FOUND_EVENT, event.toJSObject())

        notifyListeners(self.ENDPOINT_CONNECTED_EVENT, data: event.toJSObject())
    }
    /**
     * Called when a remote endpoint is disconnected or has become unreachable.
     */
    func onEndpointDisconnected(_ endpoint: Endpoint) {
        let event: EndpointDisconnectedEvent = .init(endpoint)
        print(self.ENDPOINT_FOUND_EVENT, event.toJSObject())

        notifyListeners(self.ENDPOINT_DISCONNECTED_EVENT, data: event.toJSObject())
    }

    /**
     * Called when a Payload is received from a remote endpoint.
     */
    func onPayloadReceived(_ endpoint: Endpoint, _ payload: Data) {
        let event: PayloadReceivedEvent = .init(endpoint, payload)
        print(self.ENDPOINT_FOUND_EVENT, event.toJSObject())

        notifyListeners(self.PAYLOAD_RECEIVED_EVENT, data: event.toJSObject())
    }

    /**
     * Calls
     */

    private func rejectCall(_ call: CAPPluginCall, _ error: Error) {
        CAPLog.print("[", self.tag, "] ", error)
        call.reject(error.localizedDescription)
    }

    private func resolveCall(_ call: CAPPluginCall, _ result: JSObject?) {
        if let result {
            call.resolve(result)
        } else {
            call.resolve()
        }
    }
}
