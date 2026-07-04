export type LocationAccuracy =
  'lowest' | 'low' | 'balanced' | 'high' | 'highest';

export type GeoLocation = {
  latitude: number;
  longitude: number;
  accuracy: number | null;
  altitude: number | null;
  altitudeAccuracy: number | null;
  speed: number | null;
  speedAccuracy: number | null;
  heading: number | null;
  headingAccuracy: number | null;
  timestamp: number;
};

export type BackgroundLocationRelayConfig = {
  location: {
    interval: number;
    fastestInterval?: number;
    distanceFilter?: number;
    accuracy?: LocationAccuracy;
    pausesLocationUpdatesAutomatically?: boolean;
    showsBackgroundLocationIndicator?: boolean;
  };
  request: {
    endpoint: string;
    headers?: Record<string, string>;
    timeout?: number;
    bodyTemplate: string;
  };
};

export type AndroidSetupStatus = {
  location: boolean;
  backgroundLocation: boolean;
  notifications: boolean;
  batteryOptimizationIgnored: boolean;
  manufacturer: string | null;
  manufacturerSettingsAvailable: boolean;
};
