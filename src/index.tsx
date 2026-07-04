import {
  nativeGetAndroidSetupStatus,
  nativeInitialize,
  nativeIsRunning,
  nativeOpenBatteryOptimizationSettings,
  nativeOpenManufacturerSettings,
  nativeRequestIgnoreBatteryOptimizations,
  nativeStart,
  nativeStop,
} from './BackgroundLocationRelayHybrid';
import type {
  AndroidSetupStatus,
  BackgroundLocationRelayConfig,
} from './types';
import { validateConfig } from './validation';

export function isAndroidSetupReady(status: AndroidSetupStatus): boolean {
  return (
    status.location &&
    status.backgroundLocation &&
    status.notifications &&
    status.batteryOptimizationIgnored
  );
}

const BackgroundLocationRelay = {
  async initialize(config: BackgroundLocationRelayConfig): Promise<void> {
    validateConfig(config);
    await nativeInitialize(config);
  },

  start(): Promise<void> {
    return nativeStart();
  },

  stop(): Promise<void> {
    return nativeStop();
  },

  isRunning(): Promise<boolean> {
    return nativeIsRunning();
  },

  getAndroidSetupStatus(): Promise<AndroidSetupStatus> {
    return nativeGetAndroidSetupStatus();
  },

  requestIgnoreBatteryOptimizations(): Promise<boolean> {
    return nativeRequestIgnoreBatteryOptimizations();
  },

  openBatteryOptimizationSettings(): Promise<void> {
    return nativeOpenBatteryOptimizationSettings();
  },

  openManufacturerSettings(): Promise<boolean> {
    return nativeOpenManufacturerSettings();
  },
};

export default BackgroundLocationRelay;
export type {
  AndroidSetupStatus,
  BackgroundLocationRelayConfig,
  GeoLocation,
  LocationAccuracy,
} from './types';
export {
  containsGeoPlaceholder,
  GEO_PLACEHOLDER,
  validateConfig,
} from './validation';
