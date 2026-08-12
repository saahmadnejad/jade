import { describe, it, expect, beforeEach, vi } from 'vitest';
import { PlatformAPI } from '../src/api/platform';
import { getHttpClient } from '../src/api/http-client';
import type { HttpClient } from '../src/api/http-client';

vi.mock('../src/api/http-client');

describe('PlatformAPI', () => {
  let mockHttpClient: HttpClient;
  let platformAPI: PlatformAPI;

  beforeEach(() => {
    mockHttpClient = {
      get: vi.fn(),
      post: vi.fn(),
      patch: vi.fn(),
      delete: vi.fn(),
    };
    platformAPI = new PlatformAPI(mockHttpClient);
  });

  it('Given health endpoint called, When health() resolves, Then returns HealthStatus', async () => {
    // Arrange
    const mockResponse = { data: { status: 'ok' } };
    vi.mocked(mockHttpClient.get).mockResolvedValue(mockResponse);

    // Act
    const result = await platformAPI.health();

    // Assert
    expect(mockHttpClient.get).toHaveBeenCalledWith('/health');
    expect(result).toEqual({ status: 'ok' });
  });

  it('Given platform endpoint called, When getInfo() resolves, Then returns PlatformInfo', async () => {
    // Arrange
    const mockPlatform = {
      platformID: 'jade-main',
      containerName: 'Main-Container',
      isMain: true,
      ams: 'ams@jade-main',
      defaultDF: 'df@jade-main',
    };
    vi.mocked(mockHttpClient.get).mockResolvedValue({ data: mockPlatform });

    // Act
    const result = await platformAPI.getInfo();

    // Assert
    expect(mockHttpClient.get).toHaveBeenCalledWith('/platform');
    expect(result).toEqual(mockPlatform);
  });

  it('Given shutdown endpoint called, When shutdown() resolves, Then returns ShutdownResponse', async () => {
    // Arrange
    vi.mocked(mockHttpClient.post).mockResolvedValue({ data: { message: 'Platform shutdown initiated' } });

    // Act
    const result = await platformAPI.shutdown();

    // Assert
    expect(mockHttpClient.post).toHaveBeenCalledWith('/platform/shutdown', { confirm: true });
    expect(result).toEqual({ message: 'Platform shutdown initiated' });
  });

  it('Given version endpoint called, When version() resolves, Then returns VersionInfo', async () => {
    // Arrange
    const mockVersion = { version: '1.0', revision: '123', date: '2024-01-01' };
    vi.mocked(mockHttpClient.get).mockResolvedValue({ data: mockVersion });

    // Act
    const result = await platformAPI.version();

    // Assert
    expect(mockHttpClient.get).toHaveBeenCalledWith('/version');
    expect(result).toEqual(mockVersion);
  });
});
