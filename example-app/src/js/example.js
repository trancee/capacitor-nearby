import { Nearby } from '@capacitor-trancee/nearby';

window.testInitialize = async () => {
    let options = {}

    const endpointName = document.getElementById("initialize-endpointName").value;
    const serviceID = document.getElementById("initialize-serviceID").value;

    if (endpointName !== undefined && endpointName.length > 0) {
        options.endpointName = endpointName
    }
    if (serviceID !== undefined && serviceID.length > 0) {
        options.serviceID = serviceID
    }

    const result = await window.execute("initialize", options)
}

window.testReset = async () => {
    let options = {}

    const result = await window.execute("reset", options)

    document.getElementById("status").value = ""
    document.getElementById("events").value = ""
}

window.testStartAdvertising = async () => {
    let options = {}

    const endpointName = document.getElementById("startAdvertising-endpointName").value;

    if (endpointName !== undefined && endpointName.length > 0) {
        options.endpointName = endpointName
    }

    const result = await window.execute("startAdvertising", options)
}

window.testStopAdvertising = async () => {
    let options = {}

    const result = await window.execute("stopAdvertising", options)
}

window.testStartDiscovering = async () => {
    let options = {}

    const result = await window.execute("startDiscovering", options)
}

window.testStopDiscovering = async () => {
    let options = {};

    const result = await window.execute("stopDiscovering", options);
}

window.testStatus = async () => {
    let options = {};

    const result = await window.execute("status", options);
}

window.testCheckPermissions = async () => {
    let options = {};

    const result = await window.execute("checkPermissions", options);
}

window.testRequestPermissions = async () => {
    let options = {};

    const aliases = document.getElementById("aliases").selectedOptions;

    if (aliases !== undefined && aliases.length > 0) {
        options.permissions = Array.from(aliases).map(option => option.value)
    }

    const result = await window.execute("requestPermissions", options);
}

window.execute = async (method, options) => {
    try {
        options = Object.keys(options).length > 0 ? options : undefined

        document.getElementById("status").value += `⚪ ${method}(${JSON.stringify(options) || ""})` + "\n"

        const result = await Nearby[method](options)

        document.getElementById("status").value += `⚫ ${method}(${JSON.stringify(result) || ""})` + "\n"

        return result
    } catch (error) {
        document.getElementById("status").value += `⛔ ${error}` + "\n";
    }
}

Nearby.addListener('onEndpointFound', (endpoint) => {
    console.log('onEndpointFound', endpoint);

    document.getElementById("events").value += `⚡ onEndpointFound(${JSON.stringify(endpoint) || ""})` + "\n";
});

Nearby.addListener('onEndpointLost', (endpoint) => {
    console.log('onEndpointLost', endpoint);

    document.getElementById("events").value += `⚡ onEndpointLost(${JSON.stringify(endpoint) || ""})` + "\n";
});

Nearby.addListener('onEndpointInitiated', (endpoint) => {
    console.log('onEndpointInitiated', endpoint);

    document.getElementById("events").value += `⚡ onEndpointInitiated(${JSON.stringify(endpoint) || ""})` + "\n";
});

Nearby.addListener('onEndpointConnected', (endpoint) => {
    console.log('onEndpointConnected', endpoint);

    document.getElementById("events").value += `⚡ onEndpointConnected(${JSON.stringify(endpoint) || ""})` + "\n";
});

Nearby.addListener('onEndpointRejected', (endpoint) => {
    console.log('onEndpointRejected', endpoint);

    document.getElementById("events").value += `⚡ onEndpointRejected(${JSON.stringify(endpoint) || ""})` + "\n";
});

Nearby.addListener('onEndpointFailed', (endpoint) => {
    console.log('onEndpointFailed', endpoint);

    document.getElementById("events").value += `⚡ onEndpointFailed(${JSON.stringify(endpoint) || ""})` + "\n";
});

Nearby.addListener('onEndpointDisconnected', (endpoint) => {
    console.log('onEndpointDisconnected', endpoint);

    document.getElementById("events").value += `⚡ onEndpointDisconnected(${JSON.stringify(endpoint) || ""})` + "\n";
});

Nearby.addListener('onPayloadReceived', (endpoint) => {
    console.log('onPayloadReceived', endpoint);

    document.getElementById("events").value += `⚡ onPayloadReceived(${JSON.stringify(endpoint) || ""})` + "\n";
});

Nearby.addListener('onPayloadTransferUpdate', (endpoint) => {
    console.log('onPayloadTransferUpdate', endpoint);

    document.getElementById("events").value += `⚡ onPayloadTransferUpdate(${JSON.stringify(endpoint) || ""})` + "\n";
});

document.getElementById("status").onchange = () => {
    document.getElementById("status").scrollTop = document.getElementById("status").scrollHeight;
}
document.getElementById("events").onchange = () => {
    document.getElementById("events").scrollTop = document.getElementById("events").scrollHeight;
}
