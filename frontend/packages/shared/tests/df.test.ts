import { describe, it, expect, beforeEach, vi } from 'vitest';
import { DFAPI } from '../src/api/df';
import type { HttpClient } from '../src/api/http-client';

describe('DFAPI', () => {
  let mockHttpClient: HttpClient;
  let dfAPI: DFAPI;

  beforeEach(() => {
    mockHttpClient = {
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      patch: vi.fn(),
      delete: vi.fn(),
    };
    dfAPI = new DFAPI(mockHttpClient);
  });

  it('Given df registrations endpoint called, When listRegistrations() resolves, Then returns DFRegistrationListResponse', async () => {
    // Arrange
    const mockData = {
      registrations: [
        { name: 'agent1@host', addresses: ['addr1'], services: [], ownership: '' },
      ],
    };
    vi.mocked(mockHttpClient.get).mockResolvedValue({ data: mockData });

    // Act
    const result = await dfAPI.listRegistrations();

    // Assert
    expect(mockHttpClient.get).toHaveBeenCalledWith('/df/registrations');
    expect(result.registrations).toHaveLength(1);
    expect(result.registrations[0].name).toBe('agent1@host');
  });

  it('Given register request, When register() resolves, Then returns DFRegisterResponse', async () => {
    // Arrange
    const mockResponse = { message: 'registered', registration: { name: 'a1@host', addresses: [], services: [], ownership: '' } };
    vi.mocked(mockHttpClient.post).mockResolvedValue({ data: mockResponse });
    const request = { agentName: 'a1@host', addresses: ['addr1'], services: [{ type: 'svc', name: 'svc1' }] };

    // Act
    const result = await dfAPI.register(request);

    // Assert
    expect(mockHttpClient.post).toHaveBeenCalledWith('/df/registrations', request);
    expect(result.message).toBe('registered');
  });

  it('Given agent name, When deregister() resolves, Then calls DELETE', async () => {
    // Arrange
    vi.mocked(mockHttpClient.delete).mockResolvedValue({ data: { message: 'deregistered' } });

    // Act
    const result = await dfAPI.deregister('a1@host');

    // Assert
    expect(mockHttpClient.delete).toHaveBeenCalledWith('/df/registrations/a1@host');
    expect(result.message).toBe('deregistered');
  });

  it('Given agent name, When getRegistration() resolves, Then returns DFRegistrationInfo', async () => {
    // Arrange
    const mockReg = { name: 'a1@host', addresses: ['addr1'], services: [], ownership: 'own' };
    vi.mocked(mockHttpClient.get).mockResolvedValue({ data: mockReg });

    // Act
    const result = await dfAPI.getRegistration('a1@host');

    // Assert
    expect(mockHttpClient.get).toHaveBeenCalledWith('/df/registrations/a1@host');
    expect(result.name).toBe('a1@host');
  });

  it('Given modify request, When modifyRegistration() resolves, Then calls PUT', async () => {
    // Arrange
    const mockResponse = { message: 'modified', registration: { name: 'a1@host', addresses: ['addr2'], services: [], ownership: '' } };
    vi.mocked(mockHttpClient.put).mockResolvedValue({ data: mockResponse });
    const request = { addresses: ['addr2'] };

    // Act
    const result = await dfAPI.modifyRegistration('a1@host', request);

    // Assert
    expect(mockHttpClient.put).toHaveBeenCalledWith('/df/registrations/a1@host', request);
    expect(result.message).toBe('modified');
  });

  it('Given search request, When search() resolves, Then returns DFSearchResponse', async () => {
    // Arrange
    const mockResponse = { results: [{ name: 'a1@host', addresses: [], services: [], ownership: '' }] };
    vi.mocked(mockHttpClient.post).mockResolvedValue({ data: mockResponse });
    const request = { description: { services: [{ type: 'svc' }] }, constraints: { maxResults: -1, maxDepth: 0 } };

    // Act
    const result = await dfAPI.search(request);

    // Assert
    expect(mockHttpClient.post).toHaveBeenCalledWith('/df/search', request);
    expect(result.results).toHaveLength(1);
  });

  it('Given no args, When getDescription() resolves, Then returns DFDescriptionResponse', async () => {
    // Arrange
    const mockDesc = { name: 'df@host', addresses: ['addr1'], services: [] };
    vi.mocked(mockHttpClient.get).mockResolvedValue({ data: mockDesc });

    // Act
    const result = await dfAPI.getDescription();

    // Assert
    expect(mockHttpClient.get).toHaveBeenCalledWith('/df/description');
    expect(result.name).toBe('df@host');
  });

  it('Given no args, When getStatus() resolves, Then returns DFStatusResponse', async () => {
    // Arrange
    const mockStatus = { running: true, agent: 'df@host', container: 'Main-Container', registeredAgentCount: 3 };
    vi.mocked(mockHttpClient.get).mockResolvedValue({ data: mockStatus });

    // Act
    const result = await dfAPI.getStatus();

    // Assert
    expect(mockHttpClient.get).toHaveBeenCalledWith('/tools/df-gui/status');
    expect(result.running).toBe(true);
  });

  it('Given no args, When refresh() resolves, Then returns DFRefreshResponse', async () => {
    // Arrange
    vi.mocked(mockHttpClient.post).mockResolvedValue({ data: { message: 'DF refreshed', registeredAgentCount: 2 } });

    // Act
    const result = await dfAPI.refresh();

    // Assert
    expect(mockHttpClient.post).toHaveBeenCalledWith('/df/refresh');
    expect(result.registeredAgentCount).toBe(2);
  });

  it('Given no args, When getParents() resolves, Then returns DFParentsResponse', async () => {
    // Arrange
    const mockParents = { parents: [{ name: 'parent@host', addresses: ['addr1'] }] };
    vi.mocked(mockHttpClient.get).mockResolvedValue({ data: mockParents });

    // Act
    const result = await dfAPI.getParents();

    // Assert
    expect(mockHttpClient.get).toHaveBeenCalledWith('/df/federation/parents');
    expect(result.parents).toHaveLength(1);
  });

  it('Given no args, When getChildren() resolves, Then returns DFChildrenResponse', async () => {
    // Arrange
    const mockChildren = { children: [{ name: 'child@host', addresses: ['addr1'] }] };
    vi.mocked(mockHttpClient.get).mockResolvedValue({ data: mockChildren });

    // Act
    const result = await dfAPI.getChildren();

    // Assert
    expect(mockHttpClient.get).toHaveBeenCalledWith('/df/federation/children');
    expect(result.children).toHaveLength(1);
  });

  it('Given federate request, When federate() resolves, Then returns DfFederateResponse', async () => {
    // Arrange
    const mockResponse = { message: 'federated', parent: { name: 'parent@host', addresses: ['addr1'] } };
    vi.mocked(mockHttpClient.post).mockResolvedValue({ data: mockResponse });
    const request = { parentDF: 'parent@host', parentDFAddresses: ['addr1'] };

    // Act
    const result = await dfAPI.federate(request);

    // Assert
    expect(mockHttpClient.post).toHaveBeenCalledWith('/df/federation', request);
    expect(result.parent.name).toBe('parent@host');
  });

  it('Given parent DF name, When deregisterParent() resolves, Then calls DELETE', async () => {
    // Arrange
    vi.mocked(mockHttpClient.delete).mockResolvedValue({ data: { message: 'deregistered' } });

    // Act
    await dfAPI.deregisterParent('parent@host');

    // Assert
    expect(mockHttpClient.delete).toHaveBeenCalledWith('/df/federation/parent%40host');
  });

  it('Given child DF name, When deregisterChild() resolves, Then calls DELETE', async () => {
    // Arrange
    vi.mocked(mockHttpClient.delete).mockResolvedValue({ data: { message: 'deregistered' } });

    // Act
    await dfAPI.deregisterChild('child@host');

    // Assert
    expect(mockHttpClient.delete).toHaveBeenCalledWith('/df/federation/children/child%40host');
  });
});
