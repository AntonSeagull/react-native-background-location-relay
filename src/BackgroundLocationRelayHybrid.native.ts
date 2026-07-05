import { NitroModules } from 'react-native-nitro-modules';
import type { BackgroundLocationRelayConfig as NitroConfig } from './BackgroundLocationRelay.nitro';
import type { BackgroundLocationRelay } from './BackgroundLocationRelay.nitro';
import type { BackgroundLocationRelayConfig } from './types';

const hybridObject = NitroModules.createHybridObject<BackgroundLocationRelay>(
  'BackgroundLocationRelay'
);

function toNitroConfig(config: BackgroundLocationRelayConfig): NitroConfig {
  return {
    location: {
      interval: config.location.interval,
      fastestInterval: config.location.fastestInterval,
      distanceFilter: config.location.distanceFilter,
      accuracy: config.location.accuracy,
      pausesLocationUpdatesAutomatically:
        config.location.pausesLocationUpdatesAutomatically,
      showsBackgroundLocationIndicator:
        config.location.showsBackgroundLocationIndicator,
    },
    request: {
      endpoint: config.request.endpoint,
      headersJson: config.request.headers
        ? JSON.stringify(config.request.headers)
        : undefined,
      timeout: config.request.timeout,
      bodyTemplate: config.request.bodyTemplate,
    },
  };
}

export function nativeInitialize(
  config: BackgroundLocationRelayConfig
): Promise<void> {
  return hybridObject.initialize(toNitroConfig(config));
}

export function nativeStart(): Promise<void> {
  return hybridObject.start();
}

export function nativeStop(): Promise<void> {
  return hybridObject.stop();
}

export function nativeIsRunning(): Promise<boolean> {
  return hybridObject.isRunning();
}

export function nativeCheckBatteryOptimization(): Promise<boolean> {
  return hybridObject.checkBatteryOptimization();
}

export function nativeRequestBatteryOptimization(): Promise<boolean> {
  return hybridObject.requestBatteryOptimization();
}

export function nativeOpenBatteryOptimizationSettings(): Promise<void> {
  return hybridObject.openBatteryOptimizationSettings();
}

export function nativeEnableAutostartSettings(): Promise<boolean> {
  return hybridObject.enableAutostartSettings();
}

export function nativeOpenAutostartSettings(): Promise<boolean> {
  return hybridObject.openAutostartSettings();
}
