import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import * as apiClient from '../../../lib/api-client';
import { weightClient } from './weight-client';

describe('weightClient', () => {
  let fetchJsonSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    fetchJsonSpy = vi.spyOn(apiClient, 'fetchJson').mockResolvedValue({} as never);
  });
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('list calls GET /api/v1/weight-entries', async () => {
    await weightClient.list();
    expect(fetchJsonSpy).toHaveBeenCalledWith(
      '/api/v1/weight-entries',
      expect.objectContaining({ method: 'GET' }),
    );
  });

  it('get calls GET /api/v1/weight-entries/:id', async () => {
    await weightClient.get(42);
    expect(fetchJsonSpy).toHaveBeenCalledWith(
      '/api/v1/weight-entries/42',
      expect.objectContaining({ method: 'GET' }),
    );
  });

  it('create calls POST /api/v1/weight-entries', async () => {
    const body = { weight_value: 175.5, weight_unit: 'lbs', observation_date: '2026-05-20' };
    await weightClient.create(body);
    expect(fetchJsonSpy).toHaveBeenCalledWith(
      '/api/v1/weight-entries',
      expect.objectContaining({ method: 'POST', body }),
    );
  });

  it('update calls PUT /api/v1/weight-entries/:id', async () => {
    const body = { weight_value: 180, weight_unit: 'lbs', observation_date: '2026-05-20' };
    await weightClient.update(42, body);
    expect(fetchJsonSpy).toHaveBeenCalledWith(
      '/api/v1/weight-entries/42',
      expect.objectContaining({ method: 'PUT', body }),
    );
  });

  it('remove calls DELETE /api/v1/weight-entries/:id', async () => {
    await weightClient.remove(42);
    expect(fetchJsonSpy).toHaveBeenCalledWith(
      '/api/v1/weight-entries/42',
      expect.objectContaining({ method: 'DELETE' }),
    );
  });
});
