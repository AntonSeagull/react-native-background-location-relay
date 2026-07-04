# react-native-background-location-relay

Библиотека React Native для периодического получения геолокации устройства и отправки данных на настраиваемый HTTP endpoint, пока приложение работает в foreground или background.

[English documentation](README_EN.md)

## Установка

```sh
npm install react-native-background-location-relay react-native-nitro-modules
```

> `react-native-nitro-modules` обязателен — библиотека построена на [Nitro Modules](https://nitro.margelo.com/).

Для iOS:

```sh
cd ios && pod install
```

## Использование

Инициализируйте библиотеку при запуске приложения:

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

Библиотека периодически получает геолокацию, подставляет вместо плейсхолдера `$GEO$` текущий объект координат и отправляет итоговый JSON на настроенный endpoint методом HTTP POST.

## Шаблон тела запроса

`bodyTemplate` должен быть валидной JSON-строкой с плейсхолдером `$GEO$`. Плейсхолдер может находиться на любом уровне вложенности.

Перед отправкой `$GEO$` заменяется на объект геолокации:

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

- `initialize(config)` — создаёт или заменяет текущую конфигурацию. Конфигурация сохраняется в native-хранилище. Если трекинг уже запущен, новая конфигурация применяется автоматически.
- `start()` — запускает отслеживание геолокации и отправку HTTP-запросов в фоне.
- `stop()` — останавливает трекинг и отправку. Сохранённая конфигурация остаётся доступной для повторного `start()`.
- `isRunning()` — возвращает, активен ли сейчас трекинг.
- `getAndroidSetupStatus()` — чеклист готовности Android: разрешения, battery optimization, производитель.
- `requestIgnoreBatteryOptimizations()` — открывает системный запрос на отключение battery optimization. Возвращает `true`, если уже отключено.
- `openBatteryOptimizationSettings()` — открывает системный экран battery optimization.
- `openManufacturerSettings()` — открывает экран автозапуска/battery производителя (Xiaomi, Samsung, Huawei и др.), если доступен. Возвращает `true`, если экран найден и открыт.

Хелпер `isAndroidSetupReady(status)` возвращает `true`, если выданы все обязательные разрешения и battery optimization отключена.

## Настройка платформ

### iOS

Добавьте в `Info.plist` приложения фоновый режим Location Updates и строки для запроса разрешений:

- `UIBackgroundModes` со значением `location`
- `NSLocationWhenInUseUsageDescription`
- `NSLocationAlwaysAndWhenInUseUsageDescription`

Запросите нужные разрешения на геолокацию в приложении до вызова `start()`.

### Android

Библиотека использует foreground location service и показывает постоянное уведомление, пока активен фоновый трекинг. Сервис автоматически:

- перезапускается после kill системой (`START_STICKY`);
- продолжает работу при свайпе из recents (`stopWithTask="false"`);
- восстанавливается после перезагрузки устройства (`BOOT_COMPLETED`);
- проверяет своё состояние каждые 5 минут (AlarmManager heartbeat);
- перезапускает GPS при включении провайдеров локации.

Разрешения `RECEIVE_BOOT_COMPLETED` и `POST_NOTIFICATIONS` добавляются в манифест библиотеки автоматически. **Runtime-запрос разрешений — ответственность приложения.**

#### Порядок запроса разрешений

Запросите разрешения **до** вызова `start()`, в таком порядке:

1. **`ACCESS_FINE_LOCATION`** — базовое разрешение на геолокацию.
2. **`ACCESS_BACKGROUND_LOCATION`** (Android 10+, API 29+) — «Всегда». На Android 11+ запрашивается **отдельно**, после foreground-разрешения, желательно с объяснением пользователю.
3. **`POST_NOTIFICATIONS`** (Android 13+, API 33+) — без него foreground service не сможет показать уведомление и упадёт.

Пример:

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

`start()` проверяет все три разрешения на native-стороне и выбрасывает понятную ошибку, если чего-то не хватает.

#### Battery optimization и производители

Разрешение `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` добавляется в манифест библиотеки. Перед `start()` рекомендуется проверить готовность устройства:

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

> **Google Play:** прямой запрос `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` может потребовать обоснования для модерации. Если запрос недоступен, используйте `openBatteryOptimizationSettings()` и попросите пользователя отключить оптимизацию вручную.

Трекинг и отправка HTTP выполняются в native-коде и не зависят от активности JavaScript runtime React Native.

## Участие в разработке

- [Development workflow](CONTRIBUTING.md#development-workflow)
- [Sending a pull request](CONTRIBUTING.md#sending-a-pull-request)
- [Code of conduct](CODE_OF_CONDUCT.md)

## Лицензия

MIT
