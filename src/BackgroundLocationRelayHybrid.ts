import type { BackgroundLocationRelayConfig } from './types';

function unsupported(): never {
  throw new Error(
    'react-native-background-location-relay is only supported on native platforms.'
  );
}

export function nativeInitialize(
  _config: BackgroundLocationRelayConfig
): Promise<void> {
  unsupported();
}

export function nativeStart(): Promise<void> {
  unsupported();
}

export function nativeStop(): Promise<void> {
  unsupported();
}

export function nativeIsRunning(): Promise<boolean> {
  unsupported();
}

export function nativeGetAndroidSetupStatus(): Promise<
  import('./types').AndroidSetupStatus
> {
  unsupported();
}

export function nativeRequestIgnoreBatteryOptimizations(): Promise<boolean> {
  unsupported();
}

export function nativeOpenBatteryOptimizationSettings(): Promise<void> {
  unsupported();
}

export function nativeOpenManufacturerSettings(): Promise<boolean> {
  unsupported();
}
