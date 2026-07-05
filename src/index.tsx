import {
  nativeEnableAutostartSettings,
  nativeCheckBatteryOptimization,
  nativeInitialize,
  nativeIsRunning,
  nativeOpenAutostartSettings,
  nativeOpenBatteryOptimizationSettings,
  nativeRequestBatteryOptimization,
  nativeStart,
  nativeStop,
} from './BackgroundLocationRelayHybrid';
import type { BackgroundLocationRelayConfig } from './types';
import { validateConfig } from './validation';

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

  /** Android only. Returns `true` if the app is already exempt from battery optimization. Always `true` on iOS. */
  checkBatteryOptimization(): Promise<boolean> {
    return nativeCheckBatteryOptimization();
  },

  /** Android only. Opens the system prompt to request battery optimization exemption. Falls back to openBatteryOptimizationSettings() if the dialog is unavailable. Returns `true` if already exempt. */
  requestBatteryOptimization(): Promise<boolean> {
    return nativeRequestBatteryOptimization();
  },

  /** Android only. Opens the system battery optimization settings screen. */
  openBatteryOptimizationSettings(): Promise<void> {
    return nativeOpenBatteryOptimizationSettings();
  },

  /** Android only. Returns `true` if a vendor autostart screen exists on this device (Xiaomi, Samsung, Huawei, etc.). Does not verify whether autostart is enabled — only whether the screen is available. Always `false` on iOS and stock Android. */
  enableAutostartSettings(): Promise<boolean> {
    return nativeEnableAutostartSettings();
  },

  /** Android only. Opens the vendor autostart screen when available. Returns `true` if a screen was found and opened. Optional — tracking works without it, but background reliability improves on some OEM devices. */
  openAutostartSettings(): Promise<boolean> {
    return nativeOpenAutostartSettings();
  },
};

export default BackgroundLocationRelay;
export type {
  BackgroundLocationRelayConfig,
  GeoLocation,
  LocationAccuracy,
} from './types';
export {
  containsGeoPlaceholder,
  GEO_PLACEHOLDER,
  validateConfig,
} from './validation';
