import { Nearby } from '@capacitor-trancee/nearby'

const scrollToBottom = t => t.scrollTop = t.scrollHeight

const statusEl = document.querySelector("#status")
statusEl.addEventListener("change", () => {
    scrollToBottom(this)
})
const logStatus = (status) => {
    statusEl.value += `${status}` + "\n"
    scrollToBottom(statusEl)
}

const eventsEl = document.querySelector("#events")
eventsEl.addEventListener("change", () => {
    scrollToBottom(this)
})
const logEvent = (event) => {
    eventsEl.value += `⚡ ${event}` + "\n"
    scrollToBottom(eventsEl)
}

const endpointsEl = document.querySelector("#endpoints")

window.testInitialize = async () => {
    let options = {}

    const endpointName = document.getElementById("initialize-endpointName").value
    const endpointInfo = document.getElementById("initialize-endpointInfo").value
    const serviceID = document.getElementById("initialize-serviceID").value

    if (endpointName !== undefined && endpointName.length > 0) {
        options.endpointName = endpointName
    }
    if (endpointInfo !== undefined && endpointInfo.length > 0) {
        options.endpointInfo = endpointInfo
    }
    if (serviceID !== undefined && serviceID.length > 0) {
        options.serviceID = serviceID
    }

    const result = await window.execute("initialize", options)
}

window.testReset = async () => {
    let options = {}

    const result = await window.execute("reset", options)

    endpointsEl.options.length = 0

    statusEl.value = ""
    eventsEl.value = ""
}

window.testStartAdvertising = async () => {
    let options = {}

    const endpointInfo = document.getElementById("startAdvertising-endpointInfo").value

    if (endpointInfo !== undefined && endpointInfo.length > 0) {
        options.endpointInfo = endpointInfo
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
    let options = {}

    const result = await window.execute("stopDiscovering", options)
}

window.testConnect = async () => {
    let options = {}

    const endpointID = endpointsEl.value

    if (endpointID !== undefined && endpointID.length > 0) {
        options.endpointID = endpointID
    }

    const result = await window.execute("connect", options)
}

window.testDisconnect = async () => {
    let options = {}

    const endpointID = endpointsEl.value

    if (endpointID !== undefined && endpointID.length > 0) {
        options.endpointID = endpointID
    }

    const result = await window.execute("disconnect", options)
}

window.testSendPayload = async () => {
    let options = {}

    if (endpointsEl.selectedOptions.length > 1) {
        const endpointIDs = []

        for (const option of endpointsEl.selectedOptions) {
            endpointIDs.push(option.value)
        }

        options.endpointIDs = endpointIDs
    } else {
        const endpointID = endpointsEl.value

        options.endpointID = endpointID
    }

    const payload = document.getElementById("sendPayload-payload").value

    if (payload !== undefined && payload.length > 0) {
        options.payload = payload
    }

    const result = await window.execute("sendPayload", options)
}

window.testStatus = async () => {
    let options = {}

    const result = await window.execute("status", options)
}

window.testCheckPermissions = async () => {
    let options = {}

    const result = await window.execute("checkPermissions", options)
}

window.testRequestPermissions = async () => {
    let options = {}

    const aliases = document.getElementById("aliases").selectedOptions

    if (aliases !== undefined && aliases.length > 0) {
        options.permissions = Array.from(aliases).map(option => option.value)
    }

    const result = await window.execute("requestPermissions", options)
}

window.execute = async (method, options) => {
    try {
        options = Object.keys(options).length > 0 ? options : undefined

        logStatus(`⚪ ${method}(${JSON.stringify(options) || ""})`)

        const result = await Nearby[method](options)

        logStatus(`⚫ ${method}(${JSON.stringify(result) || ""})`)

        return result
    } catch (error) {
        logStatus(`⛔ ${error}`)
    }
}

Nearby.addListener('onEndpointFound', (endpoint) => {
    console.log('onEndpointFound', endpoint)

    logEvent(`onEndpointFound(${JSON.stringify(endpoint) || ""})`)

    endpointsEl.add(
        new Option(
            endpoint.endpointID,
            endpoint.endpointID,
        )
    )
})

Nearby.addListener('onEndpointLost', (endpoint) => {
    console.log('onEndpointLost', endpoint)

    logEvent(`onEndpointLost(${JSON.stringify(endpoint) || ""})`)

    for (const option of endpointsEl.options) {
        if (option.value === endpoint.endpointID) {
            option.remove()
        }
    }
})

Nearby.addListener('onEndpointConnected', (endpoint) => {
    console.log('onEndpointConnected', endpoint)

    logEvent(`onEndpointConnected(${JSON.stringify(endpoint) || ""})`)
})

Nearby.addListener('onEndpointDisconnected', (endpoint) => {
    console.log('onEndpointDisconnected', endpoint)

    logEvent(`onEndpointDisconnected(${JSON.stringify(endpoint) || ""})`)
})

Nearby.addListener('onPayloadReceived', (endpoint) => {
    console.log('onPayloadReceived', endpoint)

    logEvent(`onPayloadReceived(${JSON.stringify(endpoint) || ""})`)
})

window.toggle = async (element) => {
    const legend = element.previousElementSibling
    const sibling = element.nextElementSibling

    const title = legend.title

    if (sibling.style.display === "none") {
        sibling.style.display = ""

        legend.title = legend.innerText
        legend.innerText = title

        element.innerText = "▲"
    } else {
        sibling.style.display = "none"

        legend.title = legend.innerText
        legend.innerText = title

        element.innerText = "▼"
    }
}