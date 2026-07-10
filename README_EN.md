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

// Android only — battery optimization (verifiable, recommended before start)
const batteryOk = await BackgroundLocationRelay.checkBatteryOptimization();
const alreadyIgnored =
  await BackgroundLocationRelay.requestBatteryOptimization();
await BackgroundLocationRelay.openBatteryOptimizationSettings();

// Android only — vendor autostart (optional, not verifiable)
const hasAutostartScreen =
  await BackgroundLocationRelay.enableAutostartSettings();
const opened = await BackgroundLocationRelay.openAutostartSettings();
```

- `initialize(config)` creates or replaces the current configuration. The configuration is persisted in native storage. If tracking is already running, the new configuration is applied automatically.
- `start()` starts location tracking and background HTTP request delivery. It validates the required permissions and throws an error naming the missing one (request permissions via `react-native-permissions`).
- `stop()` stops location tracking and HTTP delivery. The stored configuration is preserved.
- `isRunning()` returns whether location tracking is currently active.

#### Battery optimization (Android, verifiable)

Recommended before `start()`. You can re-check with `checkBatteryOptimization()` after the user returns from settings.

- `checkBatteryOptimization()` returns `true` if the app is already exempt from battery optimization.
- `requestBatteryOptimization()` opens the system prompt to request exemption. Falls back to settings if the dialog is unavailable. Returns `true` if already exempt.
- `openBatteryOptimizationSettings()` opens the system battery optimization screen directly.

#### Autostart (Android, optional)

Some OEM firmware (Xiaomi, Huawei, Oppo, Vivo, etc.) has a separate **autostart** screen. Samsung does not — instead it uses **Battery → Background usage limits → Never sleeping apps** or **Apps → [app] → Battery → Unrestricted**. The library **cannot verify** whether the user enabled the setting — it can only open a relevant screen when one exists on the device. Tracking may work without it, but background reliability improves on aggressive OEM devices when the user configures this manually. Add this step **at your discretion**.

- `enableAutostartSettings()` returns `true` if at least one supported vendor screen exists on the device. Returns `false` on stock Android (Pixel, etc.).
- `openAutostartSettings()` tries a fallback chain for the current manufacturer and opens the first screen that succeeds. Returns `true` if a screen was actually opened. If every option fails, shows a native OK dialog (Russian or English based on the device language).

## Requesting permissions (react-native-permissions)

The library does **not** request permissions itself. `start()` only checks them and, if something is missing, throws an error naming the specific missing permission. Request permissions from your app using [`react-native-permissions`](https://github.com/zoontek/react-native-permissions) **before** calling `start()`.

Which permissions are needed:

| Platform | Permission | When |
|---|---|---|
| iOS | `LOCATION_ALWAYS` ("Always") | required for background |
| Android | `ACCESS_FINE_LOCATION` | always |
| Android | `ACCESS_BACKGROUND_LOCATION` | Android 10+ (API 29+) |
| Android | `POST_NOTIFICATIONS` | Android 13+ (API 33+) |

Install:

```sh
yarn add react-native-permissions
```

A single cross-platform request function (call it right before `start()`):

```ts
import { Platform } from 'react-native';
import {
  PERMISSIONS,
  RESULTS,
  openSettings,
  request,
  requestNotifications,
} from 'react-native-permissions';

/**
 * Requests every permission required for background tracking.
 * Returns true only when ALL required permissions are granted.
 */
async function requestRelayPermissions(): Promise<boolean> {
  if (Platform.OS === 'ios') {
    // iOS requires "Always" for background. react-native-permissions upgrades
    // an existing "When In Use" grant to "Always" when possible.
    const status = await request(PERMISSIONS.IOS.LOCATION_ALWAYS);
    if (status === RESULTS.BLOCKED) {
      // Previously denied — only the system settings screen remains.
      await openSettings().catch(() => undefined);
    }
    return status === RESULTS.GRANTED;
  }

  // 1. Base (foreground) location.
  const fine = await request(PERMISSIONS.ANDROID.ACCESS_FINE_LOCATION);
  if (fine !== RESULTS.GRANTED) {
    if (fine === RESULTS.BLOCKED) {
      await openSettings().catch(() => undefined);
    }
    return false;
  }

  // 2. Background ("Allow all the time") location (Android 10+). On Android 11+
  //    this is a separate system dialog — request it after the foreground one.
  if (Number(Platform.Version) >= 29) {
    const background = await request(
      PERMISSIONS.ANDROID.ACCESS_BACKGROUND_LOCATION
    );
    if (background !== RESULTS.GRANTED) {
      if (background === RESULTS.BLOCKED) {
        await openSettings().catch(() => undefined);
      }
      return false;
    }
  }

  // 3. Notifications (Android 13+) — required by the foreground service.
  if (Number(Platform.Version) >= 33) {
    const { status } = await requestNotifications(['alert']);
    if (status !== RESULTS.GRANTED) {
      return false;
    }
  }

  return true;
}

// Usage:
if (await requestRelayPermissions()) {
  await BackgroundLocationRelay.start();
}
```

`react-native-permissions` status values:

- `RESULTS.GRANTED` — granted, you can call `start()`.
- `RESULTS.DENIED` — not requested yet or denied but still requestable (you may call `request` again).
- `RESULTS.BLOCKED` — denied for good; can't be requested, only `openSettings()`.
- `RESULTS.UNAVAILABLE` — the feature is not available on this device.

> **iOS:** requesting `LOCATION_ALWAYS` requires `setup_permissions([... 'LocationAlways', 'LocationWhenInUse'])` in your `Podfile` — see "Platform setup → iOS".

## Platform setup

### iOS

Add the Location Updates background mode and location permission strings to your app `Info.plist`:

- `UIBackgroundModes` with `location`
- `NSLocationWhenInUseUsageDescription`
- `NSLocationAlwaysAndWhenInUseUsageDescription`

To let `react-native-permissions` request location, enable the needed permissions in your `ios/Podfile` and run `pod install`:

```ruby
setup_permissions([
  'LocationAlways',
  'LocationWhenInUse',
])
```

The library does **not** request permissions itself. Request them from your app using [`react-native-permissions`](https://github.com/zoontek/react-native-permissions) **before** calling `start()` (full example in the [Requesting permissions](#requesting-permissions-react-native-permissions) section). Background tracking requires **Always** access (`LOCATION_ALWAYS` / `authorizedAlways`); otherwise `start()` throws an error naming the missing permission.

### Android

The library uses a foreground location service and displays a persistent notification while background location tracking is active. The service automatically:

- restarts after the system kills it (`START_STICKY`);
- keeps running when the app is swiped from recents (`stopWithTask="false"`);
- restores after device reboot (`BOOT_COMPLETED`);
- checks its own state every 5 minutes (AlarmManager heartbeat);
- restarts GPS when location providers are re-enabled.

`RECEIVE_BOOT_COMPLETED` and `POST_NOTIFICATIONS` are added to the merged manifest by the library. The library does **not** request permissions itself — **runtime permission requests remain the app's responsibility** (use [`react-native-permissions`](https://github.com/zoontek/react-native-permissions)).

#### Permission request order

Request permissions **before** calling `start()`, in this order (ready-to-use code in the [Requesting permissions](#requesting-permissions-react-native-permissions) section):

1. **`ACCESS_FINE_LOCATION`** — base location permission.
2. **`ACCESS_BACKGROUND_LOCATION`** (Android 10+, API 29+) — "Allow all the time". On Android 11+, this must be requested **separately** after the foreground permission, ideally with a user-facing explanation.
3. **`POST_NOTIFICATIONS`** (Android 13+, API 33+) — without it, the foreground service cannot show a notification and will crash.

`start()` validates all three permissions on the native side and throws a descriptive error naming the missing permission (for example `Background location permission (ACCESS_BACKGROUND_LOCATION) has not been granted`).

#### Battery optimization (recommended)

The library adds `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` to the merged manifest. Status **can be verified** with `checkBatteryOptimization()`:

```js
import BackgroundLocationRelay from 'react-native-background-location-relay';

if (!(await BackgroundLocationRelay.checkBatteryOptimization())) {
  await BackgroundLocationRelay.requestBatteryOptimization();
}

await BackgroundLocationRelay.start();
```

> **Google Play:** a direct `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` prompt may require policy justification during review. If the system dialog is unavailable, `requestBatteryOptimization()` automatically opens the settings screen. Use `openBatteryOptimizationSettings()` to go straight to settings without attempting the dialog.

#### Autostart (optional, at your discretion)

Xiaomi, Huawei, Oppo, Vivo, and other OEMs may have an autostart screen that **cannot be verified programmatically** — only opened when available. On Samsung (including Galaxy S24), the library opens the **Never sleeping apps** screen (official Samsung deeplink) or, if unavailable, the app's battery settings. Tracking works without this step, but background reliability improves on those devices when the user configures this manually. Add to your app if you want:

```js
// Optional — only when a vendor autostart screen exists on this device
if (await BackgroundLocationRelay.enableAutostartSettings()) {
  await BackgroundLocationRelay.openAutostartSettings();
  // Ask the user in your UI whether they enabled the setting — cannot be verified
}
```

On stock Android (Pixel, etc.) `enableAutostartSettings()` returns `false` — you can skip this block.

Location tracking and HTTP delivery run in native code and do not depend on the React Native JavaScript runtime remaining active.

## Contributing

- [Development workflow](CONTRIBUTING.md#development-workflow)
- [Sending a pull request](CONTRIBUTING.md#sending-a-pull-request)
- [Code of conduct](CODE_OF_CONDUCT.md)

## License

MIT
