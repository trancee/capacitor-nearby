/// <reference types="@capacitor-trancee/nearby" />

import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: "com.example.myapp",
  appName: "My App",
  webDir: "dist",
  plugins: {
    Nearby: {
      endpointName: "My App",

      serviceID: "com.example.myapp",
    },
  },
};

export default config;
