import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import {
  PERMISSIONS,
  RESULTS,
  openSettings,
  request,
  requestNotifications,
} from 'react-native-permissions';
import BackgroundLocationRelay from 'react-native-background-location-relay';

const demoConfig = {
  location: {
    interval: 60_000,
    fastestInterval: 30_000,
    distanceFilter: 50,
    accuracy: 'high' as const,
  },
  request: {
    endpoint: 'https://httpbin.org/post',
    headers: {
      'X-Device-ID': 'example-device',
    },
    timeout: 15_000,
    bodyTemplate: JSON.stringify({
      event: 'location_update',
      payload: {
        location: '$GEO$',
        source: 'mobile',
      },
    }),
  },
};

type PermissionOutcome = {
  granted: boolean;
  blocked: boolean;
  missing?: string;
};

const OK: PermissionOutcome = { granted: true, blocked: false };

async function requestLocationPermissions(): Promise<PermissionOutcome> {
  if (Platform.OS === 'ios') {
    // Always is required for background tracking to work and for start() to
    // succeed. react-native-permissions upgrades an existing "When In Use"
    // grant to "Always" when possible.
    const status = await request(PERMISSIONS.IOS.LOCATION_ALWAYS);
    if (status === RESULTS.GRANTED) {
      return OK;
    }
    return {
      granted: false,
      blocked: status === RESULTS.BLOCKED,
      missing: 'Location (Always)',
    };
  }

  const fine = await request(PERMISSIONS.ANDROID.ACCESS_FINE_LOCATION);
  if (fine !== RESULTS.GRANTED) {
    return {
      granted: false,
      blocked: fine === RESULTS.BLOCKED,
      missing: 'ACCESS_FINE_LOCATION',
    };
  }

  if (Number(Platform.Version) >= 29) {
    const background = await request(
      PERMISSIONS.ANDROID.ACCESS_BACKGROUND_LOCATION
    );
    if (background !== RESULTS.GRANTED) {
      return {
        granted: false,
        blocked: background === RESULTS.BLOCKED,
        missing: 'ACCESS_BACKGROUND_LOCATION',
      };
    }
  }

  if (Number(Platform.Version) >= 33) {
    const { status } = await requestNotifications(['alert']);
    if (status !== RESULTS.GRANTED) {
      return {
        granted: false,
        blocked: status === RESULTS.BLOCKED,
        missing: 'POST_NOTIFICATIONS',
      };
    }
  }

  return OK;
}

export default function App() {
  const [running, setRunning] = useState(false);
  const [status, setStatus] = useState('Idle');
  const [batteryOptimized, setBatteryOptimized] = useState<boolean | null>(
    null
  );
  const [autostartAvailable, setAutostartAvailable] = useState(false);

  const refreshRunningState = useCallback(async () => {
    const isRunning = await BackgroundLocationRelay.isRunning();
    setRunning(isRunning);
  }, []);

  const refreshAndroidStatus = useCallback(async () => {
    if (Platform.OS !== 'android') {
      return;
    }

    const [ignored, autostart] = await Promise.all([
      BackgroundLocationRelay.checkBatteryOptimization(),
      BackgroundLocationRelay.enableAutostartSettings(),
    ]);
    setBatteryOptimized(ignored);
    setAutostartAvailable(autostart);
  }, []);

  useEffect(() => {
    refreshRunningState().catch((error: unknown) => {
      setStatus(
        error instanceof Error ? error.message : 'Failed to read status.'
      );
    });
    refreshAndroidStatus().catch(() => {
      // Ignore status errors on unsupported platforms.
    });
  }, [refreshRunningState, refreshAndroidStatus]);

  const handleInitialize = async () => {
    try {
      setStatus('Initializing...');
      await BackgroundLocationRelay.initialize(demoConfig);
      setStatus('Configured');
    } catch (error: unknown) {
      const message =
        error instanceof Error ? error.message : 'Initialize failed.';
      setStatus(message);
      Alert.alert('Initialize failed', message);
    }
  };

  const handleStart = async () => {
    try {
      const outcome = await requestLocationPermissions();
      if (!outcome.granted) {
        const missing = outcome.missing ?? 'location';
        setStatus(`Permission missing: ${missing}`);
        Alert.alert(
          'Permission required',
          outcome.blocked
            ? `${missing} is blocked. Open settings to grant it manually.`
            : `Grant ${missing} to start tracking.`,
          outcome.blocked
            ? [
                { text: 'Cancel', style: 'cancel' },
                {
                  text: 'Open settings',
                  onPress: () => {
                    openSettings().catch(() => undefined);
                  },
                },
              ]
            : undefined
        );
        return;
      }

      setStatus('Starting...');
      await BackgroundLocationRelay.start();
      await refreshRunningState();
      await refreshAndroidStatus();
      setStatus('Tracking');
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Start failed.';
      setStatus(message);
      Alert.alert('Start failed', message);
    }
  };

  const handleStop = async () => {
    try {
      setStatus('Stopping...');
      await BackgroundLocationRelay.stop();
      await refreshRunningState();
      setStatus('Stopped');
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Stop failed.';
      setStatus(message);
      Alert.alert('Stop failed', message);
    }
  };

  const handleBatteryOptimization = async () => {
    try {
      const ignored =
        await BackgroundLocationRelay.requestBatteryOptimization();
      await refreshAndroidStatus();
      if (ignored) {
        Alert.alert('Battery optimization', 'Already disabled for this app.');
      }
    } catch (error: unknown) {
      const message =
        error instanceof Error ? error.message : 'Request failed.';
      Alert.alert('Battery optimization', message);
    }
  };

  const handleAutostartSettings = async () => {
    try {
      const opened = await BackgroundLocationRelay.openAutostartSettings();
      if (!opened) {
        Alert.alert(
          'Autostart settings',
          'No vendor autostart screen is available on this device.'
        );
      }
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Open failed.';
      Alert.alert('Autostart settings', message);
    }
  };

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.title}>Background Location Relay</Text>
      <Text style={styles.status}>Status: {status}</Text>
      <Text style={styles.status}>Running: {running ? 'yes' : 'no'}</Text>

      {Platform.OS === 'android' ? (
        <View style={styles.setupBox}>
          <Text style={styles.setupTitle}>Android reliability</Text>
          <Text style={styles.setupLine}>
            Battery optimization off:{' '}
            {batteryOptimized === null
              ? 'unknown'
              : batteryOptimized
                ? 'yes'
                : 'no'}
          </Text>
          <Text style={styles.setupLine}>
            Autostart screen available: {autostartAvailable ? 'yes' : 'no'}
          </Text>
        </View>
      ) : null}

      <View style={styles.buttonGroup}>
        <Button title="Initialize" onPress={handleInitialize} />
        <Button title="Start" onPress={handleStart} />
        <Button title="Stop" onPress={handleStop} />
        {Platform.OS === 'android' ? (
          <>
            <Button
              title="Refresh status"
              onPress={() => {
                refreshAndroidStatus().catch(() => undefined);
              }}
            />
            <Button
              title="Disable battery optimization"
              onPress={handleBatteryOptimization}
            />
            <Button
              title="Open autostart settings (optional)"
              onPress={handleAutostartSettings}
            />
          </>
        ) : null}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flexGrow: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
    gap: 12,
  },
  title: {
    fontSize: 20,
    fontWeight: '600',
    marginBottom: 8,
  },
  status: {
    fontSize: 16,
  },
  setupBox: {
    width: '100%',
    padding: 12,
    borderRadius: 8,
    backgroundColor: '#f3f3f3',
    gap: 4,
  },
  setupTitle: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 4,
  },
  setupLine: {
    fontSize: 14,
  },
  buttonGroup: {
    width: '100%',
    gap: 12,
    marginTop: 16,
  },
});
