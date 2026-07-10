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
      'Authorization': 'Bearer TOKEN',
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

Отправка управляется таймером: запрос уходит каждые `interval` миллисекунд, **даже если устройство неподвижно** (в этом случае повторно отправляются последние известные координаты). Первый запрос уходит сразу после получения первой геопозиции, не дожидаясь истечения `interval`.

`distanceFilter` влияет только на то, как часто ОС сообщает библиотеке новую геопозицию (экономия батареи), но не на периодичность отправки. Чтобы координаты всегда были свежими даже без движения, не задавайте `distanceFilter`.

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

- `initialize(config)` — создаёт или заменяет текущую конфигурацию. Конфигурация сохраняется в native-хранилище. Если трекинг уже запущен, новая конфигурация применяется автоматически.
- `start()` — запускает отслеживание геолокации и отправку HTTP-запросов в фоне. Проверяет обязательные разрешения и бросает ошибку с именем недостающего (разрешения запрашивайте через `react-native-permissions`).
- `stop()` — останавливает трекинг и отправку. Сохранённая конфигурация остаётся доступной для повторного `start()`.
- `isRunning()` — возвращает, активен ли сейчас трекинг.

#### Battery optimization (Android, проверяемо)

Рекомендуется перед `start()`. Состояние можно проверить повторным вызовом `checkBatteryOptimization()` после возврата пользователя из настроек.

- `checkBatteryOptimization()` — `true`, если приложение уже исключено из battery optimization.
- `requestBatteryOptimization()` — открывает системный запрос на исключение. Если диалог недоступен, автоматически открывает экран настроек. Возвращает `true`, если уже исключено.
- `openBatteryOptimizationSettings()` — открывает системный экран battery optimization напрямую.

#### Autostart (Android, опционально)

На некоторых прошивках (Xiaomi, Huawei, Oppo, Vivo и др.) есть отдельный экран **автозапуска**. На Samsung отдельного autostart-меню нет — вместо него используются **Battery → Background usage limits → Never sleeping apps** или **Apps → [приложение] → Battery → Unrestricted**. Библиотека **не может проверить**, включил ли пользователь нужную настройку — только открыть подходящий экран, если он есть на устройстве. Без этого шага трекинг тоже может работать, но на агрессивных OEM надёжность фона выше, если пользователь настроит вручную. Добавляйте этот шаг **на своё усмотрение**.

- `enableAutostartSettings()` — `true`, если на устройстве есть хотя бы один поддерживаемый вендорский экран. На stock Android (Pixel и др.) — `false`.
- `openAutostartSettings()` — перебирает цепочку вариантов для текущего производителя и открывает первый успешный экран. Возвращает `true`, если экран реально открылся. Если ни один вариант не сработал, показывает нативный диалог с кнопкой OK (текст на русском или английском по языку системы).

## Запрос разрешений (react-native-permissions)

Библиотека **сама не запрашивает** разрешения. `start()` только проверяет их и, если чего-то не хватает, выбрасывает ошибку с именем конкретного отсутствующего разрешения. Запрашивать разрешения нужно в приложении через `[react-native-permissions](https://github.com/zoontek/react-native-permissions)` **до** вызова `start()`.

Какие разрешения нужны:

| Платформа | Разрешение                   | Когда                 |
| --------- | ---------------------------- | --------------------- |
| iOS       | `LOCATION_ALWAYS` («Всегда») | обязательно для фона  |
| Android   | `ACCESS_FINE_LOCATION`       | всегда                |
| Android   | `ACCESS_BACKGROUND_LOCATION` | Android 10+ (API 29+) |
| Android   | `POST_NOTIFICATIONS`         | Android 13+ (API 33+) |

Установка:

```sh
yarn add react-native-permissions
```

Единая кросс-платформенная функция запроса (можно вызывать прямо перед `start()`):

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
 * Запрашивает все разрешения, необходимые для фонового трекинга.
 * Возвращает true, только если выданы ВСЕ обязательные разрешения.
 */
async function requestRelayPermissions(): Promise<boolean> {
  if (Platform.OS === 'ios') {
    // На iOS для фона обязателен доступ «Всегда». react-native-permissions
    // при необходимости повысит уже выданный «При использовании» до «Всегда».
    const status = await request(PERMISSIONS.IOS.LOCATION_ALWAYS);
    if (status === RESULTS.BLOCKED) {
      // Пользователь отказал ранее — остаётся только системный экран настроек.
      await openSettings().catch(() => undefined);
    }
    return status === RESULTS.GRANTED;
  }

  // 1. Базовая геолокация (foreground).
  const fine = await request(PERMISSIONS.ANDROID.ACCESS_FINE_LOCATION);
  if (fine !== RESULTS.GRANTED) {
    if (fine === RESULTS.BLOCKED) {
      await openSettings().catch(() => undefined);
    }
    return false;
  }

  // 2. Фоновая геолокация «Всегда» (Android 10+). На Android 11+ запрашивается
  //    отдельным системным диалогом — вызывайте его после foreground-разрешения.
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

  // 3. Уведомления (Android 13+) — нужны foreground service.
  if (Number(Platform.Version) >= 33) {
    const { status } = await requestNotifications(['alert']);
    if (status !== RESULTS.GRANTED) {
      return false;
    }
  }

  return true;
}

// Использование:
if (await requestRelayPermissions()) {
  await BackgroundLocationRelay.start();
}
```

Значения статусов `react-native-permissions`:

- `RESULTS.GRANTED` — разрешение выдано, можно вызывать `start()`.
- `RESULTS.DENIED` — ещё не запрошено или отклонено, но запрашиваемо (можно вызвать `request` снова).
- `RESULTS.BLOCKED` — отклонено «навсегда»; запросить нельзя, только `openSettings()`.
- `RESULTS.UNAVAILABLE` — функция недоступна на устройстве.

> **iOS:** запрос `LOCATION_ALWAYS` требует настройки `setup_permissions([... 'LocationAlways', 'LocationWhenInUse'])` в `Podfile` — см. раздел «Настройка платформ → iOS».

## Настройка платформ

### iOS

Добавьте в `Info.plist` приложения фоновый режим Location Updates и строки для запроса разрешений:

- `UIBackgroundModes` со значением `location`
- `NSLocationWhenInUseUsageDescription`
- `NSLocationAlwaysAndWhenInUseUsageDescription`

Чтобы `react-native-permissions` мог запрашивать геолокацию, включите нужные разрешения в `ios/Podfile` и выполните `pod install`:

```ruby
setup_permissions([
  'LocationAlways',
  'LocationWhenInUse',
])
```

Библиотека **не запрашивает** разрешения сама. Запросите их в приложении через `[react-native-permissions](https://github.com/zoontek/react-native-permissions)` **до** вызова `start()` (полный пример — в разделе [«Запрос разрешений»](#запрос-разрешений-react-native-permissions)). Для работы в фоне обязателен доступ **«Всегда» (**`LOCATION_ALWAYS` **/** `authorizedAlways`**)** — с ним `start()` пройдёт, иначе он выбросит ошибку с указанием отсутствующего разрешения.

Работа в фоне:

- **При активном приложении (foreground)** трекинг и отправка работают с разрешением «При использовании» или «Всегда».
- **В фоне и после выгрузки приложения** нужен доступ к геолокации **«Всегда» (**`authorizedAlways`**)**. При нём библиотека включает significant-location-change monitoring, благодаря чему трекинг продолжается в фоне, а iOS автоматически перезапускает приложение и возобновляет отправку даже после того, как система его выгрузила. Без разрешения «Всегда» `start()` выбросит ошибку `Background location permission (Always) has not been granted` — это ограничение iOS.

Автовозобновление после перезапуска системой не требует изменений в `AppDelegate` — библиотека подключается к запуску приложения самостоятельно.

### Android

Библиотека использует foreground location service и показывает постоянное уведомление, пока активен фоновый трекинг. Сервис автоматически:

- перезапускается после kill системой (`START_STICKY`);
- продолжает работу при свайпе из recents (`stopWithTask="false"`);
- восстанавливается после перезагрузки устройства (`BOOT_COMPLETED`);
- проверяет своё состояние каждые 5 минут (AlarmManager heartbeat);
- перезапускает GPS при включении провайдеров локации.

Разрешения `RECEIVE_BOOT_COMPLETED` и `POST_NOTIFICATIONS` добавляются в манифест библиотеки автоматически. Библиотека **не запрашивает** разрешения сама — **runtime-запрос разрешений это ответственность приложения** (через `[react-native-permissions](https://github.com/zoontek/react-native-permissions)`).

#### Порядок запроса разрешений

Запросите разрешения **до** вызова `start()`, в таком порядке (готовый код — в разделе [«Запрос разрешений»](#запрос-разрешений-react-native-permissions)):

1. `ACCESS_FINE_LOCATION` — базовое разрешение на геолокацию.
2. `ACCESS_BACKGROUND_LOCATION` (Android 10+, API 29+) — «Всегда». На Android 11+ запрашивается **отдельно**, после foreground-разрешения, желательно с объяснением пользователю.
3. `POST_NOTIFICATIONS` (Android 13+, API 33+) — без него foreground service не сможет показать уведомление и упадёт.

`start()` проверяет все три разрешения на native-стороне и выбрасывает понятную ошибку с именем отсутствующего разрешения (например `Background location permission (ACCESS_BACKGROUND_LOCATION) has not been granted`).

#### Battery optimization (рекомендуется)

Разрешение `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` добавляется в манифест библиотеки. Состояние **можно проверить** через `checkBatteryOptimization()`:

```js
import BackgroundLocationRelay from 'react-native-background-location-relay';

if (!(await BackgroundLocationRelay.checkBatteryOptimization())) {
  await BackgroundLocationRelay.requestBatteryOptimization();
}

await BackgroundLocationRelay.start();
```

> **Google Play:** прямой запрос `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` может потребовать обоснования для модерации. Если системный диалог недоступен, `requestBatteryOptimization()` автоматически откроет экран настроек. Для прямого перехода в настройки без диалога используйте `openBatteryOptimizationSettings()`.

#### Autostart (опционально, на своё усмотрение)

На Xiaomi, Huawei, Oppo, Vivo и др. есть экран автозапуска, который **нельзя проверить программно** — только открыть, если он есть. На Samsung (в т.ч. Galaxy S24) библиотека открывает экран **Never sleeping apps** (официальный Samsung deeplink) или, если он недоступен, настройки батареи приложения. Трекинг работает и без этого шага, но на таких устройствах фон стабильнее, если пользователь настроит вручную. Добавляйте в своё приложение по желанию:

```js
// Опционально — только если экран автозапуска есть на этом устройстве
if (await BackgroundLocationRelay.enableAutostartSettings()) {
  await BackgroundLocationRelay.openAutostartSettings();
  // Спросите пользователя в UI, включил ли он нужную настройку — проверить нельзя
}
```

На stock Android (Pixel и др.) `enableAutostartSettings()` вернёт `false` — этот блок можно пропустить.

Трекинг и отправка HTTP выполняются в native-коде и не зависят от активности JavaScript runtime React Native.

## Участие в разработке

- [Development workflow](CONTRIBUTING.md#development-workflow)
- [Sending a pull request](CONTRIBUTING.md#sending-a-pull-request)
- [Code of conduct](CODE_OF_CONDUCT.md)

## Лицензия

MIT
