import { HttpClient } from './http-client';
import type {
  RemotePlatformInfo,
  RemotePlatformListResponse,
  RemotePlatformAddRequest,
  RemotePlatformAddResponse,
  RemotePlatformFetchRequest,
  RemotePlatformAgentListResponse,
  AgentActionResponse,
} from './types';

export class RemotePlatformAPI {
  constructor(private httpClient: HttpClient) {}

  list = async (): Promise<RemotePlatformListResponse> => {
    const res = await this.httpClient.get<RemotePlatformListResponse>('/platforms');
    return res.data;
  };

  add = async (request: RemotePlatformAddRequest): Promise<RemotePlatformAddResponse> => {
    const res = await this.httpClient.post<RemotePlatformAddResponse>('/platforms', request);
    return res.data;
  };

  fetch = async (request: RemotePlatformFetchRequest): Promise<RemotePlatformAddResponse> => {
    const res = await this.httpClient.post<RemotePlatformAddResponse>('/platforms/fetch', request);
    return res.data;
  };

  remove = async (name: string): Promise<AgentActionResponse> => {
    const res = await this.httpClient.delete<AgentActionResponse>(`/platforms/${name}`);
    return res.data;
  };

  getDescription = async (name: string): Promise<RemotePlatformInfo> => {
    const res = await this.httpClient.get<RemotePlatformInfo>(`/platforms/${name}/description`);
    return res.data;
  };

  refreshDescription = async (name: string): Promise<AgentActionResponse & RemotePlatformInfo> => {
    const res = await this.httpClient.post<AgentActionResponse & RemotePlatformInfo>(
      `/platforms/${name}/refresh`
    );
    return res.data;
  };

  listAgents = async (name: string): Promise<RemotePlatformAgentListResponse> => {
    const res = await this.httpClient.get<RemotePlatformAgentListResponse>(
      `/platforms/${name}/agents`
    );
    return res.data;
  };
}
