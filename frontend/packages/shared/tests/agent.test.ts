import { describe, it, expect, beforeEach, vi } from 'vitest';
import { AgentAPI } from '../src/api/agent';
import type { HttpClient } from '../src/api/http-client';

describe('AgentAPI', () => {
  let mockHttpClient: HttpClient;
  let agentAPI: AgentAPI;

  beforeEach(() => {
    mockHttpClient = {
      get: vi.fn(),
      post: vi.fn(),
      patch: vi.fn(),
      delete: vi.fn(),
    };
    agentAPI = new AgentAPI(mockHttpClient);
  });

  it('Given agents endpoint called, When list() resolves, Then returns AgentListResponse', async () => {
    // Arrange
    const mockAgents = { agents: [{ name: 'rma@main', state: 'ACTIVE', ownership: '', container: 'Main', addresses: [] }] };
    vi.mocked(mockHttpClient.get).mockResolvedValue({ data: mockAgents });

    // Act
    const result = await agentAPI.list();

    // Assert
    expect(mockHttpClient.get).toHaveBeenCalledWith('/agents', { params: undefined });
    expect(result.agents).toHaveLength(1);
    expect(result.agents[0].name).toBe('rma@main');
  });

  it('Given agent name provided, When get() resolves, Then returns AgentInfo', async () => {
    // Arrange
    const mockAgent = { name: 'rma@main', state: 'ACTIVE', ownership: 'init', container: 'Main', addresses: [] };
    vi.mocked(mockHttpClient.get).mockResolvedValue({ data: mockAgent });

    // Act
    const result = await agentAPI.get('rma');

    // Assert
    expect(mockHttpClient.get).toHaveBeenCalledWith('/agents/rma');
    expect(result).toEqual(mockAgent);
  });

  it('Given deploy request, When deploy() resolves, Then returns AgentDeployResponse', async () => {
    // Arrange
    vi.mocked(mockHttpClient.post).mockResolvedValue({ data: { message: 'created', name: 'rma@main' } });
    const request = { name: 'rma', class: 'com.example.MyAgent' };

    // Act
    const result = await agentAPI.deploy(request);

    // Assert
    expect(mockHttpClient.post).toHaveBeenCalledWith('/agents', request);
    expect(result.message).toContain('created');
  });

  it('Given agent name, When kill() resolves, Then calls DELETE', async () => {
    // Arrange
    vi.mocked(mockHttpClient.delete).mockResolvedValue({ data: { message: 'killed' } });

    // Act
    await agentAPI.kill('rma');

    // Assert
    expect(mockHttpClient.delete).toHaveBeenCalledWith('/agents/rma');
  });

  it('Given ownership request, When changeOwnership() resolves, Then calls PATCH', async () => {
    // Arrange
    vi.mocked(mockHttpClient.patch).mockResolvedValue({ data: { message: 'changed' } });

    // Act
    await agentAPI.changeOwnership('rma', { ownership: 'new-owner' });

    // Assert
    expect(mockHttpClient.patch).toHaveBeenCalledWith('/agents/rma', { ownership: 'new-owner' });
  });

  it('Given clone request, When clone() resolves, Then calls POST clone endpoint', async () => {
    // Arrange
    vi.mocked(mockHttpClient.post).mockResolvedValue({ data: { message: 'cloned', name: 'cloned@main' } });
    const request = { name: 'orig', newName: 'clone', container: 'Main' };

    // Act
    const result = await agentAPI.clone(request);

    // Assert
    expect(mockHttpClient.post).toHaveBeenCalledWith('/agents/clone', request);
    expect(result.name).toBe('cloned@main');
  });
});
