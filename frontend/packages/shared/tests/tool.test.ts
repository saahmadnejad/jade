import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ToolAPI } from '../src/api/tool';
import type { HttpClient } from '../src/api/http-client';

describe('ToolAPI', () => {
  let mockHttpClient: HttpClient;
  let toolAPI: ToolAPI;

  beforeEach(() => {
    mockHttpClient = {
      get: vi.fn(),
      post: vi.fn(),
      patch: vi.fn(),
      delete: vi.fn(),
    };
    toolAPI = new ToolAPI(mockHttpClient);
  });

  it('Given tool name and container, When start() resolves, Then returns ToolLaunchResponse', async () => {
    // Arrange
    const mockResponse = { message: 'Sniffer started', agent: 'sniffer@jade-main' };
    vi.mocked(mockHttpClient.post).mockResolvedValue({ data: mockResponse });

    // Act
    const result = await toolAPI.start('sniffer', { container: 'Main-Container' });

    // Assert
    expect(mockHttpClient.post).toHaveBeenCalledWith('/tools/sniffer/start', { container: 'Main-Container' });
    expect(result.message).toContain('started');
    expect(result.agent).toBe('sniffer@jade-main');
  });

  it('Given unknown tool name, When start() rejects, Then error propagates', async () => {
    // Arrange
    vi.mocked(mockHttpClient.post).mockRejectedValue(new Error('Unknown tool'));

    // Act + Assert
    await expect(toolAPI.start('unknowntool', { container: 'Main-Container' })).rejects.toThrow('Unknown tool');
  });
});
