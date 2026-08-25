import { HttpClient } from './http-client';
import type {
  ScenarioListResponse,
  ScenarioInstancesResponse,
  ScenarioStartRequest,
  ScenarioStartResponse,
} from './types';

export class ScenarioAPI {
  constructor(private httpClient: HttpClient) {}

  list = async (): Promise<ScenarioListResponse> => {
    const res = await this.httpClient.get<ScenarioListResponse>('/scenarios');
    return res.data;
  };

  listInstances = async (): Promise<ScenarioInstancesResponse> => {
    const res = await this.httpClient.get<ScenarioInstancesResponse>('/scenarios/instances');
    return res.data;
  };

  start = async (scenarioId: string, request: ScenarioStartRequest): Promise<ScenarioStartResponse> => {
    const res = await this.httpClient.post<ScenarioStartResponse>(`/scenarios/${scenarioId}/instances`, request);
    return res.data;
  };

  stop = async (instanceName: string): Promise<{ message: string }> => {
    const res = await this.httpClient.delete<{ message: string }>(`/scenarios/instances/${instanceName}`);
    return res.data;
  };
}
