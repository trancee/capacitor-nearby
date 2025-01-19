# @capacitor-trancee/nearby

Uses Bluetooth LE to scan and advertise for nearby devices

## Install

```bash
npm install @capacitor-trancee/nearby
npx cap sync
```

## API

<docgen-index>

* [`initialize(...)`](#initialize)
* [`reset()`](#reset)
* [`publish(...)`](#publish)
* [`unpublish()`](#unpublish)
* [`subscribe()`](#subscribe)
* [`unsubscribe()`](#unsubscribe)
* [`status()`](#status)
* [`addListener('onPermissionChanged', ...)`](#addlisteneronpermissionchanged-)
* [`addListener('onBluetoothStateChanged', ...)`](#addlisteneronbluetoothstatechanged-)
* [`addListener('onFound', ...)`](#addlisteneronfound-)
* [`addListener('onLost', ...)`](#addlisteneronlost-)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)
* [Enums](#enums)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### initialize(...)

```typescript
initialize(options: InitializeOptions) => Promise<void>
```

Initializes Nearby Connections for advertising and discovering of endpoints.

| Param         | Type                                                            |
| ------------- | --------------------------------------------------------------- |
| **`options`** | <code><a href="#initializeoptions">InitializeOptions</a></code> |

**Since:** 1.0.0

--------------------


### reset()

```typescript
reset() => Promise<void>
```

Stops and resets advertising and discovering of endpoints.

**Since:** 1.0.0

--------------------


### publish(...)

```typescript
publish(options: PublishOptions) => Promise<void>
```

Start publishing nearby token.

| Param         | Type                                                      |
| ------------- | --------------------------------------------------------- |
| **`options`** | <code><a href="#publishoptions">PublishOptions</a></code> |

**Since:** 1.0.0

--------------------


### unpublish()

```typescript
unpublish() => Promise<void>
```

Stop publishing nearby token.

**Since:** 1.0.0

--------------------


### subscribe()

```typescript
subscribe() => Promise<void>
```

Start listening to nearby tokens.

**Since:** 1.0.0

--------------------


### unsubscribe()

```typescript
unsubscribe() => Promise<void>
```

Stop listening to nearby tokens.

**Since:** 1.0.0

--------------------


### status()

```typescript
status() => Promise<Status>
```

Returns status of operations and found tokens.

**Returns:** <code>Promise&lt;<a href="#status">Status</a>&gt;</code>

**Since:** 1.0.0

--------------------


### addListener('onPermissionChanged', ...)

```typescript
addListener(eventName: 'onPermissionChanged', listenerFunc: (granted: boolean) => void) => Promise<PluginListenerHandle>
```

Called when permission is granted or revoked for this app to use Nearby.

| Param              | Type                                       |
| ------------------ | ------------------------------------------ |
| **`eventName`**    | <code>'onPermissionChanged'</code>         |
| **`listenerFunc`** | <code>(granted: boolean) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 1.0.0

--------------------


### addListener('onBluetoothStateChanged', ...)

```typescript
addListener(eventName: 'onBluetoothStateChanged', listenerFunc: (state: BluetoothState) => void) => Promise<PluginListenerHandle>
```

Called when state of Bluetooth has changed.

| Param              | Type                                                                          |
| ------------------ | ----------------------------------------------------------------------------- |
| **`eventName`**    | <code>'onBluetoothStateChanged'</code>                                        |
| **`listenerFunc`** | <code>(state: <a href="#bluetoothstate">BluetoothState</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 1.0.0

--------------------


### addListener('onFound', ...)

```typescript
addListener(eventName: 'onFound', listenerFunc: BeaconCallback) => Promise<PluginListenerHandle>
```

Called when beacons are found.

| Param              | Type                                                      |
| ------------------ | --------------------------------------------------------- |
| **`eventName`**    | <code>'onFound'</code>                                    |
| **`listenerFunc`** | <code><a href="#beaconcallback">BeaconCallback</a></code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 1.1.0

--------------------


### addListener('onLost', ...)

```typescript
addListener(eventName: 'onLost', listenerFunc: BeaconCallback) => Promise<PluginListenerHandle>
```

Called when a beacon is no longer detectable nearby.

| Param              | Type                                                      |
| ------------------ | --------------------------------------------------------- |
| **`eventName`**    | <code>'onLost'</code>                                     |
| **`listenerFunc`** | <code><a href="#beaconcallback">BeaconCallback</a></code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 1.1.0

--------------------


### Interfaces


#### InitializeOptions

| Prop            | Type                                          | Description                                                                                               | Default            | Since |
| --------------- | --------------------------------------------- | --------------------------------------------------------------------------------------------------------- | ------------------ | ----- |
| **`name`**      | <code>string</code>                           | A human readable name for this endpoint, to appear on the remote device.                                  |                    | 4.0.0 |
| **`serviceId`** | <code>string</code>                           | An identifier to advertise your app to other endpoints.                                                   |                    | 4.0.0 |
| **`strategy`**  | <code><a href="#strategy">Strategy</a></code> | Sets the <a href="#strategy">`Strategy`</a> to be used when discovering or advertising to Nearby devices. |                    | 4.0.0 |
| **`lowPower`**  | <code>boolean</code>                          | Sets whether low power should be used.                                                                    | <code>false</code> | 4.0.0 |


#### PublishOptions

| Prop       | Type                                  | Description                                                         | Since |
| ---------- | ------------------------------------- | ------------------------------------------------------------------- | ----- |
| **`uuid`** | <code><a href="#uuid">UUID</a></code> | Sets the beacon <a href="#uuid">UUID</a> for the publish operation. | 1.1.0 |


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


#### Beacon

| Prop       | Type                                  |
| ---------- | ------------------------------------- |
| **`uuid`** | <code><a href="#uuid">UUID</a></code> |


### Type Aliases


#### UUID

<code>string</code>


#### Status

<code>{ isPublishing: boolean; isSubscribing: boolean; uuids: UUID[]; }</code>


#### BeaconCallback

<code>(_: <a href="#beacon">Beacon</a>): void</code>


### Enums


#### Strategy

| Members              | Value                  |
| -------------------- | ---------------------- |
| **`CLUSTER`**        | <code>'cluster'</code> |
| **`STAR`**           | <code>'star'</code>    |
| **`POINT_TO_POINT`** | <code>'p2p'</code>     |


#### BluetoothState

| Members            | Value                       |
| ------------------ | --------------------------- |
| **`UNKNOWN`**      | <code>'unknown'</code>      |
| **`RESETTING`**    | <code>'resetting'</code>    |
| **`UNSUPPORTED`**  | <code>'unsupported'</code>  |
| **`UNAUTHORIZED`** | <code>'unauthorized'</code> |
| **`POWERED_OFF`**  | <code>'poweredOff'</code>   |
| **`POWERED_ON`**   | <code>'poweredOn'</code>    |

</docgen-api>
