import { describe, it, expect, beforeEach, vi } from 'vitest';
import { RemotePlatformAPI } from '../src/api/remote-platform';
import type { HttpClient } from '../src/api/http-client';

describe('RemotePlatformAPI', () => {
  let mockHttpClient: HttpClient;
  let remotePlatformAPI: RemotePlatformAPI;

  beforeEach(() => {
    mockHttpClient = {
      get: vi.fn(),
      post: vi.fn(),
      patch: vi.fn(),
      delete: vi.fn(),
    };
    remotePlatformAPI = new RemotePlatformAPI(mockHttpClient);
  });

  it('Given platforms endpoint called, When list() resolves, Then returns RemotePlatformListResponse', async () => {
    // Arrange
    const mockPlatforms = { platforms: [{ name: 'jade-main', ams: 'ams@main', addresses: [], services: [] }] };
    vi.mocked(mockHttpClient.get).mockResolvedValue({ data: mockPlatforms });

    // Act
    const result = await remotePlatformAPI.list();

    // Assert
    expect(mockHttpClient.get).toHaveBeenCalledWith('/platforms');
    expect(result.platforms).toHaveLength(1);
    expect(result.platforms[0].name).toBe('jade-main');
  });

  it('Given platform add request, When add() resolves, Then returns RemotePlatformAddResponse', async () => {
    // Arrange
    vi.mocked(mockHttpClient.post).mockResolvedValue({ data: { message: 'added', name: 'remote', ams: 'ams@remote' } });
    const request = { ams: 'ams@remote', addresses: ['jades://192.168.1.10:1099'] };

    // Act
    const result = await remotePlatformAPI.add(request);

    // Assert
    expect(mockHttpClient.post).toHaveBeenCalledWith('/platforms', request);
    expect(result.message).toContain('added');
  });

  it('Given platform name, When remove() resolves, Then calls DELETE', async () => {
    // Arrange
    vi.mocked(mockHttpClient.delete).mockResolvedValue({ data: { message: 'removed' } });

    // Act
    await remotePlatformAPI.remove('jade-remote');

    // Assert
    expect(mockHttpClient.delete).toHaveBeenCalledWith('/platforms/jade-remote');
  });

  it('Given platform name, When getDescription() resolves, Then returns RemotePlatformInfo', async () => {
    // Arrange
    const mockDesc = { name: 'jade-main', ams: 'ams@main', addresses: [], services: [] };
    vi.mocked(mockHttpClient.get).mockResolvedValue({ data: mockDesc });

    // Act
    const result = await remotePlatformAPI.getDescription('jade-main');

    // Assert
    expect(mockHttpClient.get).toHaveBeenCalledWith('/platforms/jade-main/description');
    expect(result.name).toBe('jade-main');
  });

  it('Given platform name, When listAgents() resolves, Then returns agent list', async () => {
    // Arrange
    const mockAgents = { agents: [{ name: 'rma@remote', addresses: ['jades://...'] }] };
    vi.mocked(mockHttpClient.get).mockResolvedValue({ data: mockAgents });

    // Act
    const result = await remotePlatformAPI.listAgents('jade-remote');

    // Assert
    expect(mockHttpClient.get).toHaveBeenCalledWith('/platforms/jade-remote/agents');
    expect(result.agents).toHaveLength(1);
  });

  it('Given platform name, When refreshDescription() resolves, Then calls POST refresh', async () => {
    // Arrange
    const mockResponse = { message: 'refreshed', name: 'jade-main', ams: 'ams@main', addresses: [], services: [] };
    vi.mocked(mockHttpClient.post).mockResolvedValue({ data: mockResponse });

    // Act
    const result = await remotePlatformAPI.refreshDescription('jade-main');

    // Assert
    expect(mockHttpClient.post).toHaveBeenCalledWith('/platforms/jade-main/refresh');
    expect(result.message).toBe('refreshed');
  });
});
