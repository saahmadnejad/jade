import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ContainerAPI } from '../src/api/container';
import type { HttpClient } from '../src/api/http-client';

describe('ContainerAPI', () => {
  let mockHttpClient: HttpClient;
  let containerAPI: ContainerAPI;

  beforeEach(() => {
    mockHttpClient = {
      get: vi.fn(),
      post: vi.fn(),
      patch: vi.fn(),
      delete: vi.fn(),
    };
    containerAPI = new ContainerAPI(mockHttpClient);
  });

  it('Given containers endpoint called, When list() resolves, Then returns ContainerListResponse', async () => {
    // Arrange
    const mockList = { containers: [{ name: 'Main-Container', address: '127.0.0.1', port: '1099', isMain: true }] };
    vi.mocked(mockHttpClient.get).mockResolvedValue({ data: mockList });

    // Act
    const result = await containerAPI.list();

    // Assert
    expect(mockHttpClient.get).toHaveBeenCalledWith('/containers');
    expect(result.containers).toHaveLength(1);
    expect(result.containers[0].name).toBe('Main-Container');
  });

  it('Given container name provided, When get() resolves, Then returns ContainerInfo', async () => {
    // Arrange
    const mockContainer = { name: 'Main-Container', address: '127.0.0.1', port: '1099', isMain: true };
    vi.mocked(mockHttpClient.get).mockResolvedValue({ data: mockContainer });

    // Act
    const result = await containerAPI.get('Main-Container');

    // Assert
    expect(mockHttpClient.get).toHaveBeenCalledWith('/containers/Main-Container');
    expect(result).toEqual(mockContainer);
  });

  it('Given container name provided, When kill() resolves, Then calls DELETE endpoint', async () => {
    // Arrange
    vi.mocked(mockHttpClient.delete).mockResolvedValue({ data: { message: "killed" } });

    // Act
    await containerAPI.kill('Main-Container');

    // Assert
    expect(mockHttpClient.delete).toHaveBeenCalledWith('/containers/Main-Container');
  });

  it('Given container name and repository, When save() resolves, Then calls POST with repository', async () => {
    // Arrange
    vi.mocked(mockHttpClient.post).mockResolvedValue({ data: { message: 'saved' } });

    // Act
    await containerAPI.save('Main-Container', 'file://./store');

    // Assert
    expect(mockHttpClient.post).toHaveBeenCalledWith('/containers/Main-Container/save', { repository: 'file://./store' });
  });

  it('Given container name and address, When uninstallMTP() resolves, Then calls DELETE', async () => {
    // Arrange
    vi.mocked(mockHttpClient.delete).mockResolvedValue({ data: { message: 'uninstalled' } });

    // Act
    await containerAPI.uninstallMTP('Main-Container', '127.0.0.1:1100');

    // Assert
    expect(mockHttpClient.delete).toHaveBeenCalledWith('/containers/Main-Container/mtps/127.0.0.1:1100');
  });
});
