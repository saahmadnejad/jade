import { HttpClient } from './http-client';
import type {
  ContainerInfo,
  ContainerListResponse,
  MTPInfo,
  MTPListResponse,
  MTPInstallRequest,
  SaveLoadRequest,
  AgentActionResponse,
} from './types';

export class ContainerAPI {
  constructor(private httpClient: HttpClient) {}

  list = async (): Promise<ContainerListResponse> => {
    const res = await this.httpClient.get<ContainerListResponse>('/containers');
    return res.data;
  };

  get = async (name: string): Promise<ContainerInfo> => {
    const res = await this.httpClient.get<ContainerInfo>(`/containers/${name}`);
    return res.data;
  };

  kill = async (name: string): Promise<AgentActionResponse> => {
    const res = await this.httpClient.delete<AgentActionResponse>(`/containers/${name}`);
    return res.data;
  };

  save = async (name: string, repository: string): Promise<AgentActionResponse> => {
    const res = await this.httpClient.post<AgentActionResponse>(
      `/containers/${name}/save`, { repository } as SaveLoadRequest
    );
    return res.data;
  };

  load = async (name: string, repository: string): Promise<AgentActionResponse> => {
    const res = await this.httpClient.post<AgentActionResponse>(
      `/containers/${name}/load`, { repository } as SaveLoadRequest
    );
    return res.data;
  };

  listMTPs = async (name: string): Promise<MTPListResponse> => {
    const res = await this.httpClient.get<MTPListResponse>(`/containers/${name}/mtps`);
    return res.data;
  };

  installMTP = async (name: string, mtp: MTPInstallRequest): Promise<MTPInfo> => {
    const res = await this.httpClient.post<MTPInfo>(`/containers/${name}/mtps`, mtp);
    return res.data;
  };

  uninstallMTP = async (name: string, address: string): Promise<AgentActionResponse> => {
    const res = await this.httpClient.delete<AgentActionResponse>(
      `/containers/${name}/mtps/${address}`
    );
    return res.data;
  };
}
