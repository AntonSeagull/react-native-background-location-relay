import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Button,
  PermissionsAndroid,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import BackgroundLocationRelay, {
  isAndroidSetupReady,
} from 'react-native-background-location-relay';
import type { AndroidSetupStatus } from 'react-native-background-location-relay';

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

async function requestLocationPermissions(): Promise<boolean> {
  if (Platform.OS === 'ios') {
    return true;
  }

  const fineGranted = await PermissionsAndroid.request(
    PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION
  );

  if (fineGranted !== PermissionsAndroid.RESULTS.GRANTED) {
    return false;
  }

  if (Number(Platform.Version) >= 29) {
    const backgroundGranted = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.ACCESS_BACKGROUND_LOCATION
    );
    if (backgroundGranted !== PermissionsAndroid.RESULTS.GRANTED) {
      return false;
    }
  }

  if (Number(Platform.Version) >= 33) {
    const notificationsGranted = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS
    );
    if (notificationsGranted !== PermissionsAndroid.RESULTS.GRANTED) {
      return false;
    }
  }

  return true;
}

function formatSetupLine(label: string, value: boolean): string {
  return `${label}: ${value ? 'ok' : 'missing'}`;
}

export default function App() {
  const [running, setRunning] = useState(false);
  const [status, setStatus] = useState('Idle');
  const [setupStatus, setSetupStatus] = useState<AndroidSetupStatus | null>(
    null
  );

  const refreshRunningState = useCallback(async () => {
    const isRunning = await BackgroundLocationRelay.isRunning();
    setRunning(isRunning);
  }, []);

  const refreshSetupStatus = useCallback(async () => {
    if (Platform.OS !== 'android') {
      return;
    }

    const nextStatus = await BackgroundLocationRelay.getAndroidSetupStatus();
    setSetupStatus(nextStatus);
  }, []);

  useEffect(() => {
    refreshRunningState().catch((error: unknown) => {
      setStatus(
        error instanceof Error ? error.message : 'Failed to read status.'
      );
    });
    refreshSetupStatus().catch(() => {
      // Ignore setup status errors on unsupported platforms.
    });
  }, [refreshRunningState, refreshSetupStatus]);

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
      const granted = await requestLocationPermissions();
      if (!granted) {
        setStatus('Location permission denied');
        Alert.alert(
          'Permission required',
          'Grant location permission to start tracking.'
        );
        await refreshSetupStatus();
        return;
      }

      setStatus('Starting...');
      await BackgroundLocationRelay.start();
      await refreshRunningState();
      await refreshSetupStatus();
      setStatus('Tracking');
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Start failed.';
      setStatus(message);
      Alert.alert('Start failed', message);
      await refreshSetupStatus();
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
        await BackgroundLocationRelay.requestIgnoreBatteryOptimizations();
      await refreshSetupStatus();
      if (ignored) {
        Alert.alert('Battery optimization', 'Already disabled for this app.');
      }
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Request failed.';
      Alert.alert('Battery optimization', message);
    }
  };

  const handleManufacturerSettings = async () => {
    try {
      const opened = await BackgroundLocationRelay.openManufacturerSettings();
      if (!opened) {
        Alert.alert(
          'Manufacturer settings',
          'No manufacturer-specific screen is available on this device.'
        );
      }
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Open failed.';
      Alert.alert('Manufacturer settings', message);
    }
  };

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.title}>Background Location Relay</Text>
      <Text style={styles.status}>Status: {status}</Text>
      <Text style={styles.status}>Running: {running ? 'yes' : 'no'}</Text>

      {setupStatus ? (
        <View style={styles.setupBox}>
          <Text style={styles.setupTitle}>Android setup</Text>
          <Text style={styles.setupLine}>
            {formatSetupLine('Location', setupStatus.location)}
          </Text>
          <Text style={styles.setupLine}>
            {formatSetupLine('Background', setupStatus.backgroundLocation)}
          </Text>
          <Text style={styles.setupLine}>
            {formatSetupLine('Notifications', setupStatus.notifications)}
          </Text>
          <Text style={styles.setupLine}>
            {formatSetupLine(
              'Battery optimization off',
              setupStatus.batteryOptimizationIgnored
            )}
          </Text>
          <Text style={styles.setupLine}>
            Manufacturer: {setupStatus.manufacturer ?? 'unknown'}
          </Text>
          <Text style={styles.setupLine}>
            Ready: {isAndroidSetupReady(setupStatus) ? 'yes' : 'no'}
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
              title="Refresh setup status"
              onPress={() => {
                refreshSetupStatus().catch(() => undefined);
              }}
            />
            <Button
              title="Disable battery optimization"
              onPress={handleBatteryOptimization}
            />
            <Button
              title="Open manufacturer settings"
              onPress={handleManufacturerSettings}
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
