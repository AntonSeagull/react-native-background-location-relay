import { describe, expect, it } from '@jest/globals';
import {
  containsGeoPlaceholder,
  GEO_PLACEHOLDER,
  validateConfig,
} from '../validation';

describe('validation', () => {
  const validConfig = {
    location: {
      interval: 60_000,
    },
    request: {
      endpoint: 'https://api.example.com/location',
      bodyTemplate: JSON.stringify({
        location: GEO_PLACEHOLDER,
      }),
    },
  };

  it('accepts a valid config with nested $GEO$ placeholder', () => {
    expect(() =>
      validateConfig({
        ...validConfig,
        request: {
          ...validConfig.request,
          bodyTemplate: JSON.stringify({
            event: 'location_update',
            payload: {
              location: GEO_PLACEHOLDER,
            },
          }),
        },
      })
    ).not.toThrow();
  });

  it('rejects configs without $GEO$', () => {
    expect(() =>
      validateConfig({
        ...validConfig,
        request: {
          ...validConfig.request,
          bodyTemplate: JSON.stringify({ location: { lat: 1 } }),
        },
      })
    ).toThrow('request.bodyTemplate must contain the $GEO$ placeholder');
  });

  it('rejects invalid endpoint URLs', () => {
    expect(() =>
      validateConfig({
        ...validConfig,
        request: {
          ...validConfig.request,
          endpoint: 'not-a-url',
        },
      })
    ).toThrow('request.endpoint must be a valid URL.');
  });

  it('detects $GEO$ recursively', () => {
    expect(
      containsGeoPlaceholder({
        nested: {
          values: [GEO_PLACEHOLDER],
        },
      })
    ).toBe(true);
  });
});
