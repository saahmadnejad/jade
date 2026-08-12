import axios, { AxiosInstance } from 'axios';
import * as types from './types';

const createClient = (baseURL: string = '/api'): AxiosInstance => {
  return axios.create({
    baseURL,
    headers: {
      'Content-Type': 'application/json',
    },
  });
};

const apiClient = createClient();

const handleResponse = <T>(response: { data: T }): T => response.data;

export const setBaseURL = (url: string) => {
  apiClient.defaults.baseURL = url;
};

export const health = {
  check: async (): Promise<types.HealthStatus> => {
    const response = await apiClient.get('/health');
    return handleResponse(response);
  },
};

export const version = {
  info: async (): Promise<types.VersionInfo> => {
    const response = await apiClient.get('/version');
    return handleResponse(response);
  },
};

export const platform = {
  getInfo: async (): Promise<types.PlatformInfo> => {
    const response = await apiClient.get('/platform');
    return handleResponse(response);
  },
  shutdown: async (): Promise<types.ShutdownResponse> => {
    const response = await apiClient.post('/platform/shutdown', { confirm: true });
    return handleResponse(response);
  },
};

export const containers = {
  list: async (): Promise<types.ContainerListResponse> => {
    const response = await apiClient.get('/containers');
    return handleResponse(response);
  },
  get: async (name: string): Promise<types.ContainerInfo> => {
    const response = await apiClient.get(`/containers/${name}`);
    return handleResponse(response);
  },
  kill: async (name: string): Promise<types.AgentActionResponse> => {
    const response = await apiClient.delete(`/containers/${name}`);
    return handleResponse(response);
  },
  save: async (name: string, repository: string): Promise<types.AgentActionResponse> => {
    const response = await apiClient.post(`/containers/${name}/save`, { repository });
    return handleResponse(response);
  },
  load: async (name: string, repository: string): Promise<types.AgentActionResponse> => {
    const response = await apiClient.post(`/containers/${name}/load`, { repository });
    return handleResponse(response);
  },
  listMTPs: async (name: string): Promise<types.MTPListResponse> => {
    const response = await apiClient.get(`/containers/${name}/mtps`);
    return handleResponse(response);
  },
  installMTP: async (name: string, mtp: types.MTPInstallRequest): Promise<types.MTPInfo> => {
    const response = await apiClient.post(`/containers/${name}/mtps`, mtp);
    return handleResponse(response);
  },
  uninstallMTP: async (name: string, address: string): Promise<types.AgentActionResponse> => {
    const response = await apiClient.delete(`/containers/${name}/mtps/${address}`);
    return handleResponse(response);
  },
};

export const agents = {
  list: async (params?: { container?: string; state?: string }): Promise<types.AgentListResponse> => {
    const response = await apiClient.get('/agents', { params });
    return handleResponse(response);
  },
  get: async (name: string): Promise<types.AgentInfo> => {
    const response = await apiClient.get(`/agents/${name}`);
    return handleResponse(response);
  },
  deploy: async (request: types.AgentDeployRequest): Promise<types.AgentDeployResponse> => {
    const response = await apiClient.post('/agents', request);
    return handleResponse(response);
  },
  kill: async (name: string): Promise<types.AgentActionResponse> => {
    const response = await apiClient.delete(`/agents/${name}`);
    return handleResponse(response);
  },
  suspend: async (name: string): Promise<types.AgentActionResponse> => {
    const response = await apiClient.post(`/agents/${name}/suspend`);
    return handleResponse(response);
  },
  resume: async (name: string): Promise<types.AgentActionResponse> => {
    const response = await apiClient.post(`/agents/${name}/resume`);
    return handleResponse(response);
  },
  freeze: async (name: string, request: types.AgentFreezeRequest): Promise<types.AgentActionResponse> => {
    const response = await apiClient.post(`/agents/${name}/freeze`, request);
    return handleResponse(response);
  },
  thaw: async (name: string, request: types.AgentThawRequest): Promise<types.AgentActionResponse> => {
    const response = await apiClient.post(`/agents/${name}/thaw`, request);
    return handleResponse(response);
  },
  clone: async (request: types.AgentCloneRequest): Promise<types.AgentDeployResponse> => {
    const response = await apiClient.post('/agents/clone', request);
    return handleResponse(response);
  },
  move: async (name: string, request: types.AgentMoveRequest): Promise<types.AgentActionResponse> => {
    const response = await apiClient.post(`/agents/${name}/move`, request);
    return handleResponse(response);
  },
  save: async (name: string, repository: string): Promise<types.AgentActionResponse> => {
    const response = await apiClient.post(`/agents/${name}/save`, { repository });
    return handleResponse(response);
  },
  load: async (name: string, container: string, repository: string): Promise<types.AgentActionResponse> => {
    const response = await apiClient.post('/agents/load', { name, container, repository });
    return handleResponse(response);
  },
  changeOwnership: async (name: string, request: types.AgentOwnershipRequest): Promise<types.AgentActionResponse> => {
    const response = await apiClient.patch(`/agents/${name}`, request);
    return handleResponse(response);
  },
  registerRemote: async (request: types.RemoteAgentRegisterRequest): Promise<types.AgentActionResponse> => {
    const response = await apiClient.post('/agents/register-remote', request);
    return handleResponse(response);
  },
};

export const tools = {
  start: async (tool: string, request: types.ToolLaunchRequest): Promise<types.ToolLaunchResponse> => {
    const response = await apiClient.post(`/tools/${tool}/start`, request);
    return handleResponse(response);
  },
};

export const platforms = {
  list: async (): Promise<types.RemotePlatformListResponse> => {
    const response = await apiClient.get('/platforms');
    return handleResponse(response);
  },
  add: async (request: types.RemotePlatformAddRequest): Promise<types.RemotePlatformAddResponse> => {
    const response = await apiClient.post('/platforms', request);
    return handleResponse(response);
  },
  fetch: async (request: types.RemotePlatformFetchRequest): Promise<types.RemotePlatformAddResponse> => {
    const response = await apiClient.post('/platforms/fetch', request);
    return handleResponse(response);
  },
  remove: async (name: string): Promise<types.AgentActionResponse> => {
    const response = await apiClient.delete(`/platforms/${name}`);
    return handleResponse(response);
  },
  getDescription: async (name: string): Promise<types.RemotePlatformInfo> => {
    const response = await apiClient.get(`/platforms/${name}/description`);
    return handleResponse(response);
  },
  refreshDescription: async (name: string): Promise<types.AgentActionResponse & types.RemotePlatformInfo> => {
    const response = await apiClient.post(`/platforms/${name}/refresh`);
    return handleResponse(response);
  },
  listAgents: async (name: string): Promise<types.RemotePlatformAgentListResponse> => {
    const response = await apiClient.get(`/platforms/${name}/agents`);
    return handleResponse(response);
  },
};

export { apiClient };
export * from './types';
