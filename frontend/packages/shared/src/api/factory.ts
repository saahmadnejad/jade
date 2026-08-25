import { getHttpClient } from './http-client';
import { PlatformAPI } from './platform';
import { ContainerAPI } from './container';
import { AgentAPI } from './agent';
import { ToolAPI } from './tool';
import { RemotePlatformAPI } from './remote-platform';
import { DFAPI } from './df';
import { MessageAPI } from './messages';
import { ScenarioAPI } from './scenario';

export interface ApiClient {
  platform: PlatformAPI;
  containers: ContainerAPI;
  agents: AgentAPI;
  tools: ToolAPI;
  platforms: RemotePlatformAPI;
  df: DFAPI;
  messages: MessageAPI;
  scenarios: ScenarioAPI;
}

export const createApiClient = (): ApiClient => {
  const httpClient = getHttpClient();
  return {
    platform: new PlatformAPI(httpClient),
    containers: new ContainerAPI(httpClient),
    agents: new AgentAPI(httpClient),
    tools: new ToolAPI(httpClient),
    platforms: new RemotePlatformAPI(httpClient),
    df: new DFAPI(httpClient),
    messages: new MessageAPI(httpClient),
    scenarios: new ScenarioAPI(httpClient),
  };
};

export const api = createApiClient();
