import { HttpClient } from './http-client';
import type { HealthStatus, VersionInfo, PlatformInfo, ShutdownResponse } from './types';

export class PlatformAPI {
  constructor(private httpClient: HttpClient) {}

  getInfo = async (): Promise<PlatformInfo> => {
    const res = await this.httpClient.get<PlatformInfo>('/platform');
    return res.data;
  };

  shutdown = async (): Promise<ShutdownResponse> => {
    const res = await this.httpClient.post<ShutdownResponse>('/platform/shutdown', { confirm: true });
    return res.data;
  };

  health = async (): Promise<HealthStatus> => {
    const res = await this.httpClient.get<HealthStatus>('/health');
    return res.data;
  };

  version = async (): Promise<VersionInfo> => {
    const res = await this.httpClient.get<VersionInfo>('/version');
    return res.data;
  };
}
