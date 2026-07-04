import type { BackgroundLocationRelayConfig } from './types';

export const GEO_PLACEHOLDER = '$GEO$';

export function containsGeoPlaceholder(value: unknown): boolean {
  if (value === GEO_PLACEHOLDER) {
    return true;
  }

  if (Array.isArray(value)) {
    return value.some(containsGeoPlaceholder);
  }

  if (value !== null && typeof value === 'object') {
    return Object.values(value).some(containsGeoPlaceholder);
  }

  return false;
}

export function validateConfig(config: BackgroundLocationRelayConfig): void {
  if (!config?.location || !config?.request) {
    throw new Error(
      'BackgroundLocationRelay config must include location and request sections.'
    );
  }

  const { location, request } = config;

  if (!Number.isFinite(location.interval) || location.interval <= 0) {
    throw new Error('location.interval must be a positive number.');
  }

  if (
    location.fastestInterval !== undefined &&
    (!Number.isFinite(location.fastestInterval) ||
      location.fastestInterval <= 0)
  ) {
    throw new Error('location.fastestInterval must be a positive number.');
  }

  if (
    location.distanceFilter !== undefined &&
    (!Number.isFinite(location.distanceFilter) || location.distanceFilter < 0)
  ) {
    throw new Error('location.distanceFilter must be a non-negative number.');
  }

  if (
    typeof request.endpoint !== 'string' ||
    request.endpoint.trim().length === 0
  ) {
    throw new Error('request.endpoint must be a non-empty URL string.');
  }

  try {
    // eslint-disable-next-line no-new
    new URL(request.endpoint);
  } catch {
    throw new Error('request.endpoint must be a valid URL.');
  }

  if (
    request.timeout !== undefined &&
    (!Number.isFinite(request.timeout) || request.timeout <= 0)
  ) {
    throw new Error('request.timeout must be a positive number.');
  }

  if (
    typeof request.bodyTemplate !== 'string' ||
    request.bodyTemplate.trim().length === 0
  ) {
    throw new Error('request.bodyTemplate must be a non-empty JSON string.');
  }

  let parsedBody: unknown;
  try {
    parsedBody = JSON.parse(request.bodyTemplate);
  } catch {
    throw new Error('request.bodyTemplate must be valid JSON.');
  }

  if (!containsGeoPlaceholder(parsedBody)) {
    throw new Error(
      'request.bodyTemplate must contain the $GEO$ placeholder as a JSON string value.'
    );
  }
}
