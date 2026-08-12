import { HttpClient } from './http-client';
import type { ToolLaunchRequest, ToolLaunchResponse } from './types';

export class ToolAPI {
  constructor(private httpClient: HttpClient) {}

  start = async (tool: string, request: ToolLaunchRequest): Promise<ToolLaunchResponse> => {
    const res = await this.httpClient.post<ToolLaunchResponse>(
      `/tools/${tool}/start`, request
    );
    return res.data;
  };
}
