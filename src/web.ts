import { WebPlugin } from '@capacitor/core';

import type {
  NearbyPlugin,
  PermissionStatus,
  InitializeOptions,
  InitializeResult,
  StartAdvertisingOptions,
  RequestConnectionOptions,
  AcceptConnectionOptions,
  RejectConnectionOptions,
  ConnectOptions,
  DisconnectOptions,
  SendPayloadOptions,
  SendPayloadResult,
  CancelPayloadOptions,
  StatusResult,
} from './definitions';
import { PayloadTransferUpdateStatus } from './definitions';

export class NearbyWeb extends WebPlugin implements NearbyPlugin {
  async initialize(options?: InitializeOptions): Promise<InitializeResult> {
    console.info('initialize', options);
    // throw this.unimplemented('Method not implemented.');
    return {
      endpointID: 'XXXX',
    };
  }
  async reset(): Promise<void> {
    console.info('reset');
    // throw this.unimplemented('Method not implemented.');
  }

  async startAdvertising(options?: StartAdvertisingOptions): Promise<void> {
    console.info('startAdvertising', options);
    // throw this.unimplemented('Method not implemented.');
  }
  async stopAdvertising(): Promise<void> {
    console.info('stopAdvertising');
    // throw this.unimplemented('Method not implemented.');
  }

  async startDiscovering(): Promise<void> {
    console.info('startDiscovering');
    // throw this.unimplemented('Method not implemented.');
  }
  async stopDiscovering(): Promise<void> {
    console.info('stopDiscovering');
    // throw this.unimplemented('Method not implemented.');
  }

  async requestConnection(options: RequestConnectionOptions): Promise<void> {
    console.info('requestConnection', options);
    // throw this.unimplemented('Method not implemented.');
  }
  async acceptConnection(options: AcceptConnectionOptions): Promise<void> {
    console.info('acceptConnection', options);
    // throw this.unimplemented('Method not implemented.');
  }
  async rejectConnection(options: RejectConnectionOptions): Promise<void> {
    console.info('rejectConnection', options);
    // throw this.unimplemented('Method not implemented.');
  }
  async connect(options: ConnectOptions): Promise<void> {
    console.info('connect', options);
    // throw this.unimplemented('Method not implemented.');
  }
  async disconnect(options: DisconnectOptions): Promise<void> {
    console.info('disconnect', options);
    // throw this.unimplemented('Method not implemented.');
  }

  async sendPayload(options: SendPayloadOptions): Promise<SendPayloadResult> {
    console.info('sendPayload', options);
    // throw this.unimplemented('Method not implemented.');
    return {
      payloadID: -1,

      status: PayloadTransferUpdateStatus.SUCCESS,
    };
  }
  async cancelPayload(options: CancelPayloadOptions): Promise<void> {
    console.info('cancelPayload', options);
    // throw this.unimplemented('Method not implemented.');
  }

  async status(): Promise<StatusResult> {
    console.info('status');
    // throw this.unimplemented('Method not implemented.');
    return {
      isAdvertising: false,
      isDiscovering: false,
    };
  }

  async checkPermissions(): Promise<PermissionStatus> {
    console.info('checkPermissions');
    // throw this.unimplemented('Method not implemented.');
    return {
      bluetooth: 'prompt',
      location: 'prompt',
    };
  }
  async requestPermissions(): Promise<PermissionStatus> {
    console.info('requestPermissions');
    // throw this.unimplemented('Method not implemented.');
    return {
      bluetooth: 'granted',
      location: 'granted',
    };
  }
}
