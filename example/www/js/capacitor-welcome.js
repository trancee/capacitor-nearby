window.customElements.define(
  'capacitor-welcome',
  class extends HTMLElement {
    constructor() {
      super();

      Capacitor.Plugins.SplashScreen.hide();

      const root = this.attachShadow({ mode: 'open' });

      root.innerHTML = `
    <style>
      :host {
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol";
        display: block;
        width: 100%;
        height: 100%;
      }
      h1, h2, h3, h4, h5 {
        text-transform: uppercase;
      }
      .button {
        display: inline-block;
        padding: 10px;
        background-color: #73B5F6;
        color: #fff;
        font-size: 0.9em;
        border: 0;
        border-radius: 3px;
        text-decoration: none;
        cursor: pointer;
      }
      main {
        padding: 15px;
      }
      main hr { height: 1px; background-color: #eee; border: 0; }
      main h1 {
        font-size: 1.4em;
        text-transform: uppercase;
        letter-spacing: 1px;
      }
      main h2 {
        font-size: 1.1em;
      }
      main h3 {
        font-size: 0.9em;
      }
      main p {
        color: #333;
      }
      main pre {
        white-space: pre-line;
      }
    </style>
    <div>
      <capacitor-welcome-titlebar>
        <h1>Capacitor</h1>
      </capacitor-welcome-titlebar>
      <main>
        <fieldset title="Name"><legend>Name</legend>
          <input type="text" value="BEACON#1" id="beacon-name" style="width: 100%"></input>

          <button class="button" id="initialize">Initialize</button>
          <button class="button" id="reset">Reset</button>
          <button class="button" id="status">Status</button>

          <hr>

          <button class="button" id="publish">Publish</button>
          <button class="button" id="unpublish">Unpublish</button>

          <button class="button" id="publish2">Publish 2</button>
          <button class="button" id="unpublish2">Unpublish 2</button>

          <hr>

          <button class="button" id="subscribe">Subscribe</button>
          <button class="button" id="unsubscribe">Unsubscribe</button>
        </fieldset>

        <hr>

        <fieldset title="Devices"><legend>Devices</legend>
          <select id="devices" size=5 multiple style="width: 100%"></select>
        </fieldset>

        <p>

        <fieldset title="Logs"><legend>Logs</legend>
          <textarea id="log" rows=10 disabled readonly style="width: 100%; font-size: 75%;"></textarea>
        </fieldset>
      </main>
    </div>
    `;
    }

    connectedCallback() {
      const self = this;

      const { CapacitorNearby } = Capacitor.Plugins;

      self.shadowRoot
        .querySelector('#initialize')
        .addEventListener('click', async function (e) {
          try {
            const result = await CapacitorNearby.initialize({});
            console.log(result);
            self.log('initialize', result);
          } catch (e) {
            console.warn('User cancelled', e);
            self.log('initialize', e);
          }
        });

      self.shadowRoot
        .querySelector('#reset')
        .addEventListener('click', async function (e) {
          try {
            const result = await CapacitorNearby.reset({});
            console.log(result);
            self.log('reset', result);
          } catch (e) {
            console.warn('User cancelled', e);
            self.log('reset', e);
          }
        });

      self.shadowRoot
        .querySelector('#status')
        .addEventListener('click', async function (e) {
          try {
            const result = await CapacitorNearby.status({});
            console.log(result);
            self.log('status', result);
          } catch (e) {
            console.warn('User cancelled', e);
            self.log('status', e);
          }
        });

      self.shadowRoot
        .querySelector('#publish')
        .addEventListener('click', async function (e) {
          if (self.messageUUID) {
            self.log('publish', self.messageUUID);
            return;
          }
          try {
            const result = await CapacitorNearby.publish({
              message: exports.encode(
                new TextEncoder().encode('First Message'),
              ),
            });
            console.log(result);
            self.log('publish', result);
            self.messageUUID = result.uuid;
          } catch (e) {
            console.warn('User cancelled', e);
            self.log('publish', e);
            delete self.messageUUID;
          }
        });

      self.shadowRoot
        .querySelector('#unpublish')
        .addEventListener('click', async function (e) {
          try {
            const result = await CapacitorNearby.unpublish({
              uuid: self.messageUUID,
            });
            console.log(result);
            self.log('unpublish', result);
          } catch (e) {
            console.warn('User cancelled', e);
            self.log('unpublish', e);
          }
          delete self.messageUUID;
        });

      self.shadowRoot
        .querySelector('#publish2')
        .addEventListener('click', async function (e) {
          if (self.messageUUID2) {
            self.log('publish2', self.messageUUID2);
            return;
          }
          try {
            const result = await CapacitorNearby.publish({
              message: exports.encode(
                new TextEncoder().encode('Second Message'),
              ),
            });
            console.log(result);
            self.log('publish2', result);
            self.messageUUID2 = result.uuid;
          } catch (e) {
            console.warn('User cancelled', e);
            self.log('publish2', e);
            delete self.messageUUID2;
          }
        });

      self.shadowRoot
        .querySelector('#unpublish2')
        .addEventListener('click', async function (e) {
          try {
            const result = await CapacitorNearby.unpublish({
              uuid: self.messageUUID2,
            });
            console.log(result);
            self.log('unpublish2', result);
          } catch (e) {
            console.warn('User cancelled', e);
            self.log('unpublish2', e);
          }
          delete self.messageUUID2;
        });

      self.shadowRoot
        .querySelector('#subscribe')
        .addEventListener('click', async function (e) {
          try {
            const result = await CapacitorNearby.subscribe({});
            console.log(result);
            self.log('subscribe', result);
          } catch (e) {
            console.warn('User cancelled', e);
            self.log('subscribe', e);
          }
        });

      self.shadowRoot
        .querySelector('#unsubscribe')
        .addEventListener('click', async function (e) {
          try {
            const result = await CapacitorNearby.unsubscribe({});
            console.log(result);
            self.log('unsubscribe', result);
          } catch (e) {
            console.warn('User cancelled', e);
            self.log('unsubscribe', e);
          }
        });

      self.clearDevices = function () {
        const field = self.shadowRoot.querySelector('#devices');
        while (field.options.length) {
          field.remove(0);
        }
      };

      self.removeDevice = function (id) {
        const field = self.shadowRoot.querySelector('#devices');
        let option = field.namedItem(id);
        if (option) {
          field.remove(option.index);
        }
      };

      self.addDevice = function (id, name) {
        const field = self.shadowRoot.querySelector('#devices');
        let option = document.createElement('option');
        option.id = id;
        option.value = id;
        option.text = name;
        option.selected = true;
        field.add(option);
      };

      self.log = function (method, result) {
        const field = self.shadowRoot.querySelector('#log');
        field.value +=
          new Date().toTimeString().split(' ')[0] +
          ' ' +
          method +
          '=' +
          (result === undefined ? '' : JSON.stringify(result)) +
          '\n';
        field.scrollTop = field.scrollHeight;
      };

      self.onPermissionChanged = CapacitorNearby.addListener(
        'onPermissionChanged',
        data => {
          console.log('onPermissionChanged', data);
          self.log('onPermissionChanged', data);
        },
      );

      self.onStartSuccess = CapacitorNearby.addListener(
        'onStartSuccess',
        data => {
          console.log('onStartSuccess', data);
          self.log('onStartSuccess', data);
        },
      );
      self.onStartFailure = CapacitorNearby.addListener(
        'onStartFailure',
        data => {
          console.log('onStartFailure', data);
          self.log('onStartFailure', data);
        },
      );

      self.onFound = CapacitorNearby.addListener('onFound', data => {
        console.log('onFound', data);
        self.log('onFound', data);

        self.addDevice(data.uuid, `${data.uuid}`);
      });
      self.onLost = CapacitorNearby.addListener('onLost', data => {
        console.log('onLost', data);
        self.log('onLost', data);

        self.removeDevice(data.uuid);
      });

      self.onPublishExpired = CapacitorNearby.addListener(
        'onPublishExpired',
        data => {
          console.log('onPublishExpired', data);
          self.log('onPublishExpired', data);
        },
      );
      self.onSubscribeExpired = CapacitorNearby.addListener(
        'onSubscribeExpired',
        data => {
          console.log('onSubscribeExpired', data);
          self.log('onSubscribeExpired', data);
        },
      );

      self.onAdvertisingSetStarted = CapacitorNearby.addListener(
        'onAdvertisingSetStarted',
        data => {
          console.log('onAdvertisingSetStarted', data);
          self.log('onAdvertisingSetStarted', data);
        },
      );
      self.onAdvertisingDataSet = CapacitorNearby.addListener(
        'onAdvertisingDataSet',
        data => {
          console.log('onAdvertisingDataSet', data);
          self.log('onAdvertisingDataSet', data);
        },
      );
      self.onScanResponseDataSet = CapacitorNearby.addListener(
        'onScanResponseDataSet',
        data => {
          console.log('onScanResponseDataSet', data);
          self.log('onScanResponseDataSet', data);
        },
      );
      self.onAdvertisingSetStopped = CapacitorNearby.addListener(
        'onAdvertisingSetStopped',
        data => {
          console.log('onAdvertisingSetStopped', data);
          self.log('onAdvertisingSetStopped', data);
        },
      );

      self.onScanResult = CapacitorNearby.addListener('onScanResult', data => {
        console.log('onScanResult', data);
        self.log('onScanResult', data);
      });
      self.onScanFailed = CapacitorNearby.addListener('onScanFailed', data => {
        console.error('onScanFailed', data);
        self.log('onScanFailed', data);
      });

      // Peripheral (Server)
      self.onConnectionStateChange = CapacitorNearby.addListener(
        'onConnectionStateChange',
        data => {
          console.log('onConnectionStateChange', data);
          self.log('onConnectionStateChange', data);
        },
      );
      self.onServiceAdded = CapacitorNearby.addListener(
        'onServiceAdded',
        data => {
          console.log('onServiceAdded', data);
          self.log('onServiceAdded', data);
        },
      );

      // Central (Client)
      self.onMtuChanged = CapacitorNearby.addListener('onMtuChanged', data => {
        console.log('onMtuChanged', data);
        self.log('onMtuChanged', data);
      });
      self.onServicesDiscovered = CapacitorNearby.addListener(
        'onServicesDiscovered',
        data => {
          console.log('onServicesDiscovered', data);
          self.log('onServicesDiscovered', data);
        },
      );
      self.onCharacteristicRead = CapacitorNearby.addListener(
        'onCharacteristicRead',
        data => {
          console.log('onCharacteristicRead', data);
          self.log('onCharacteristicRead', data);
        },
      );
      self.onReadRemoteRssi = CapacitorNearby.addListener(
        'onReadRemoteRssi',
        data => {
          console.log('onReadRemoteRssi', data);
          self.log('onReadRemoteRssi', data);
        },
      );
    }
  },
);

window.customElements.define(
  'capacitor-welcome-titlebar',
  class extends HTMLElement {
    constructor() {
      super();
      const root = this.attachShadow({ mode: 'open' });
      root.innerHTML = `
    <style>
      :host {
        position: relative;
        display: block;
        padding: 15px 15px 15px 15px;
        text-align: center;
        background-color: #73B5F6;
      }
      ::slotted(h1) {
        margin: 0;
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol";
        font-size: 0.9em;
        font-weight: 600;
        color: #fff;
      }
    </style>
    <slot></slot>
    `;
    }
  },
);
