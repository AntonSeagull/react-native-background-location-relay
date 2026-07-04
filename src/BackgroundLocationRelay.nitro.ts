import type { HybridObject } from 'react-native-nitro-modules';

export type LocationAccuracy =
  'lowest' | 'low' | 'balanced' | 'high' | 'highest';

export interface LocationConfig {
  interval: number;
  fastestInterval?: number;
  distanceFilter?: number;
  accuracy?: LocationAccuracy;
  pausesLocationUpdatesAutomatically?: boolean;
  showsBackgroundLocationIndicator?: boolean;
}

export interface RequestConfig {
  endpoint: string;
  headersJson?: string;
  timeout?: number;
  bodyTemplate: string;
}

export interface BackgroundLocationRelayConfig {
  location: LocationConfig;
  request: RequestConfig;
}

export interface AndroidSetupStatus {
  location: boolean;
  backgroundLocation: boolean;
  notifications: boolean;
  batteryOptimizationIgnored: boolean;
  manufacturer: string | null;
  manufacturerSettingsAvailable: boolean;
}

export interface BackgroundLocationRelay extends HybridObject<{
  ios: 'swift';
  android: 'kotlin';
}> {
  initialize(config: BackgroundLocationRelayConfig): Promise<void>;
  start(): Promise<void>;
  stop(): Promise<void>;
  isRunning(): Promise<boolean>;
  getAndroidSetupStatus(): Promise<AndroidSetupStatus>;
  requestIgnoreBatteryOptimizations(): Promise<boolean>;
  openBatteryOptimizationSettings(): Promise<void>;
  openManufacturerSettings(): Promise<boolean>;
}
