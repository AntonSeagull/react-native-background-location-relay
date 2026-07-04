# react-native-background-location-relay

React Native library for periodically retrieving the device location and sending location data to a configurable HTTP endpoint while the application is running in the foreground or background.

## Installation

```sh
npm install react-native-background-location-relay react-native-nitro-modules
```

> `react-native-nitro-modules` is required as this library relies on [Nitro Modules](https://nitro.margelo.com/).

For iOS:

```sh
cd ios && pod install
```

## Usage

Initialize the library when the application starts:

```js
import BackgroundLocationRelay from 'react-native-background-location-relay';

await BackgroundLocationRelay.initialize({
  location: {
    interval: 60_000,
    fastestInterval: 30_000,
    distanceFilter: 50,
    accuracy: 'high',
  },
  request: {
    endpoint: 'https://api.example.com/location',
    headers: {
      Authorization: 'Bearer TOKEN',
      'X-Device-ID': 'device-id',
    },
    timeout: 15_000,
    bodyTemplate: JSON.stringify({
      test: {
        geo: '$GEO$',
      },
    }),
  },
});

await BackgroundLocationRelay.start();
```

The library periodically retrieves the device location, replaces the `$GEO$` placeholder with the current location object, and sends the resulting JSON document to the configured endpoint using an HTTP POST request.

## Request body template

`bodyTemplate` must be a valid JSON string containing the `$GEO$` placeholder. The placeholder may be placed at any nesting level.

Before sending the request, `$GEO$` is replaced with the current native location object:

```json
{
  "latitude": 52.520008,
  "longitude": 13.404954,
  "accuracy": 12.4,
  "altitude": 34.2,
  "altitudeAccuracy": 5.1,
  "speed": 4.8,
  "speedAccuracy": 1.2,
  "heading": 125.3,
  "headingAccuracy": 8.5,
  "timestamp": 1783175123456
}
```

## API

```js
await BackgroundLocationRelay.initialize(config);
await BackgroundLocationRelay.start();
await BackgroundLocationRelay.stop();
const running = await BackgroundLocationRelay.isRunning();

// Android only
const setup = await BackgroundLocationRelay.getAndroidSetupStatus();
const alreadyIgnored =
  await BackgroundLocationRelay.requestIgnoreBatteryOptimizations();
await BackgroundLocationRelay.openBatteryOptimizationSettings();
const opened = await BackgroundLocationRelay.openManufacturerSettings();
```

- `initialize(config)` creates or replaces the current configuration. The configuration is persisted in native storage. If tracking is already running, the new configuration is applied automatically.
- `start()` starts location tracking and background HTTP request delivery.
- `stop()` stops location tracking and HTTP delivery. The stored configuration is preserved.
- `isRunning()` returns whether location tracking is currently active.
- `getAndroidSetupStatus()` returns the Android readiness checklist: permissions, battery optimization, manufacturer.
- `requestIgnoreBatteryOptimizations()` opens the system prompt to disable battery optimization. Returns `true` if already disabled.
- `openBatteryOptimizationSettings()` opens the system battery optimization screen.
- `openManufacturerSettings()` opens the manufacturer autostart/battery screen when available. Returns `true` if a screen was found and opened.

The `isAndroidSetupReady(status)` helper returns `true` when all required permissions are granted and battery optimization is disabled.

## Platform setup

### iOS

Add the Location Updates background mode and location permission strings to your app `Info.plist`:

- `UIBackgroundModes` with `location`
- `NSLocationWhenInUseUsageDescription`
- `NSLocationAlwaysAndWhenInUseUsageDescription`

Request the appropriate location permissions from your app before calling `start()`.

### Android

The library uses a foreground location service and displays a persistent notification while background location tracking is active. The service automatically:

- restarts after the system kills it (`START_STICKY`);
- keeps running when the app is swiped from recents (`stopWithTask="false"`);
- restores after device reboot (`BOOT_COMPLETED`);
- checks its own state every 5 minutes (AlarmManager heartbeat);
- restarts GPS when location providers are re-enabled.

`RECEIVE_BOOT_COMPLETED` and `POST_NOTIFICATIONS` are added to the merged manifest by the library. **Runtime permission requests remain the app's responsibility.**

#### Permission request order

Request permissions **before** calling `start()`, in this order:

1. **`ACCESS_FINE_LOCATION`** — base location permission.
2. **`ACCESS_BACKGROUND_LOCATION`** (Android 10+, API 29+) — "Allow all the time". On Android 11+, this must be requested **separately** after the foreground permission, ideally with a user-facing explanation.
3. **`POST_NOTIFICATIONS`** (Android 13+, API 33+) — without it, the foreground service cannot show a notification and will crash.

Example:

```js
import { PermissionsAndroid, Platform } from 'react-native';

async function requestAndroidPermissions(): Promise<boolean> {
  if (Platform.OS !== 'android') {
    return true;
  }

  const fine = await PermissionsAndroid.request(
    PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
  );
  if (fine !== PermissionsAndroid.RESULTS.GRANTED) {
    return false;
  }

  if (Number(Platform.Version) >= 29) {
    const background = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.ACCESS_BACKGROUND_LOCATION,
    );
    if (background !== PermissionsAndroid.RESULTS.GRANTED) {
      return false;
    }
  }

  if (Number(Platform.Version) >= 33) {
    const notifications = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS,
    );
    if (notifications !== PermissionsAndroid.RESULTS.GRANTED) {
      return false;
    }
  }

  return true;
}

// ...
const granted = await requestAndroidPermissions();
if (granted) {
  await BackgroundLocationRelay.start();
}
```

`start()` validates all three permissions on the native side and throws a descriptive error if any are missing.

#### Battery optimization and manufacturers

The library adds `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` to the merged manifest. Before calling `start()`, check device readiness:

```js
import BackgroundLocationRelay, {
  isAndroidSetupReady,
} from 'react-native-background-location-relay';

const setup = await BackgroundLocationRelay.getAndroidSetupStatus();

if (!setup.batteryOptimizationIgnored) {
  await BackgroundLocationRelay.requestIgnoreBatteryOptimizations();
}

if (setup.manufacturerSettingsAvailable) {
  await BackgroundLocationRelay.openManufacturerSettings();
}

if (isAndroidSetupReady(await BackgroundLocationRelay.getAndroidSetupStatus())) {
  await BackgroundLocationRelay.start();
}
```

> **Google Play:** a direct `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` prompt may require policy justification during review. If the direct request is unavailable, use `openBatteryOptimizationSettings()` and ask the user to disable optimization manually.

Location tracking and HTTP delivery run in native code and do not depend on the React Native JavaScript runtime remaining active.

## Contributing

- [Development workflow](CONTRIBUTING.md#development-workflow)
- [Sending a pull request](CONTRIBUTING.md#sending-a-pull-request)
- [Code of conduct](CODE_OF_CONDUCT.md)

## License

MIT
