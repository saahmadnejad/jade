import { HttpClient } from './http-client';
import type {
  AgentInfo,
  AgentListResponse,
  AgentDeployRequest,
  AgentDeployResponse,
  AgentActionResponse,
  AgentFreezeRequest,
  AgentThawRequest,
  AgentCloneRequest,
  AgentMoveRequest,
  AgentOwnershipRequest,
  RemoteAgentRegisterRequest,
} from './types';

export class AgentAPI {
  constructor(private httpClient: HttpClient) {}

  list = async (params?: { container?: string; state?: string; detail?: boolean }): Promise<AgentListResponse> => {
    const res = await this.httpClient.get<AgentListResponse>('/agents', { params });
    return res.data;
  };

  get = async (name: string): Promise<AgentInfo> => {
    const res = await this.httpClient.get<AgentInfo>(`/agents/${name}`);
    return res.data;
  };

  deploy = async (request: AgentDeployRequest): Promise<AgentDeployResponse> => {
    const res = await this.httpClient.post<AgentDeployResponse>('/agents', request);
    return res.data;
  };

  kill = async (name: string): Promise<AgentActionResponse> => {
    const res = await this.httpClient.delete<AgentActionResponse>(`/agents/${name}`);
    return res.data;
  };

  suspend = async (name: string): Promise<AgentActionResponse> => {
    const res = await this.httpClient.post<AgentActionResponse>(`/agents/${name}/suspend`);
    return res.data;
  };

  resume = async (name: string): Promise<AgentActionResponse> => {
    const res = await this.httpClient.post<AgentActionResponse>(`/agents/${name}/resume`);
    return res.data;
  };

  freeze = async (name: string, request: AgentFreezeRequest): Promise<AgentActionResponse> => {
    const res = await this.httpClient.post<AgentActionResponse>(`/agents/${name}/freeze`, request);
    return res.data;
  };

  thaw = async (name: string, request: AgentThawRequest): Promise<AgentActionResponse> => {
    const res = await this.httpClient.post<AgentActionResponse>(`/agents/${name}/thaw`, request);
    return res.data;
  };

  clone = async (request: AgentCloneRequest): Promise<AgentDeployResponse> => {
    const res = await this.httpClient.post<AgentDeployResponse>('/agents/clone', request);
    return res.data;
  };

  move = async (name: string, request: AgentMoveRequest): Promise<AgentActionResponse> => {
    const res = await this.httpClient.post<AgentActionResponse>(`/agents/${name}/move`, request);
    return res.data;
  };

  save = async (name: string, repository: string): Promise<AgentActionResponse> => {
    const res = await this.httpClient.post<AgentActionResponse>(
      `/agents/${name}/save`, { repository: repository }
    );
    return res.data;
  };

  load = async (name: string, container: string, repository: string): Promise<AgentActionResponse> => {
    const res = await this.httpClient.post<AgentActionResponse>('/agents/load', {
      name, container, repository,
    });
    return res.data;
  };

  changeOwnership = async (name: string, request: AgentOwnershipRequest): Promise<AgentActionResponse> => {
    const res = await this.httpClient.patch<AgentActionResponse>(`/agents/${name}`, request);
    return res.data;
  };

  registerRemote = async (request: RemoteAgentRegisterRequest): Promise<AgentActionResponse> => {
    const res = await this.httpClient.post<AgentActionResponse>('/agents/register-remote', request);
    return res.data;
  };
}
