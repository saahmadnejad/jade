export interface HealthStatus {
  status: string;
}

export interface VersionInfo {
  version: string;
  revision: string;
  date: string;
}

export interface PlatformInfo {
  platformID: string;
  containerName: string;
  isMain: boolean;
  ams: string;
  defaultDF: string;
}

export interface ShutdownResponse {
  message: string;
}

export interface ContainerInfo {
  name: string;
  address: string;
  port: string;
  isMain: boolean;
}

export interface ContainerListResponse {
  containers: ContainerInfo[];
}

export interface MTPInfo {
  address: string;
  className: string;
}

export interface MTPListResponse {
  mtps: MTPInfo[];
}

export interface MTPInstallRequest {
  className: string;
  address: string;
}

export interface SaveLoadRequest {
  repository: string;
}

export interface AgentInfo {
  name: string;
  state: string | null;
  ownership: string | null;
  container: string;
  addresses: string[];
}

export interface AgentListResponse {
  agents: AgentInfo[];
}

export interface AgentDeployRequest {
  name: string;
  class: string;
  args?: string[];
  container?: string;
}

export interface AgentDeployResponse {
  message: string;
  name: string;
}

export interface AgentActionResponse {
  message: string;
}

export interface AgentFreezeRequest {
  container: string;
  repository: string;
}

export interface AgentThawRequest {
  container: string;
  repository: string;
}

export interface AgentCloneRequest {
  name: string;
  newName?: string;
  container?: string;
}

export interface AgentMoveRequest {
  container: string;
}

export interface AgentOwnershipRequest {
  ownership: string;
}

export interface RemoteAgentRegisterRequest {
  aid: string;
  addresses?: string[];
}

export interface RemotePlatformInfo {
  name: string;
  ams: string;
  addresses: string[];
  services: string[];
}

export interface RemotePlatformListResponse {
  platforms: RemotePlatformInfo[];
}

export interface RemotePlatformAddRequest {
  ams: string;
  addresses: string[];
}

export interface RemotePlatformAddResponse {
  message: string;
  name: string;
  ams: string;
}

export interface RemotePlatformFetchRequest {
  url: string;
}

export interface RemotePlatformAgentInfo {
  name: string;
  addresses: string[];
}

export interface RemotePlatformAgentListResponse {
  agents: RemotePlatformAgentInfo[];
}

export interface ToolLaunchRequest {
  container: string;
}

export interface ToolLaunchResponse {
  message: string;
  agent: string;
}

export interface ErrorResponse {
  error: string;
  code: number;
}

export interface AclMessageEvent {
  id: string;
  timestamp: string;
  sender: string;
  receiver: string;
  performative: string;
  protocol: string;
  ontology: string;
  content: string;
}

export interface MessagesRecentResponse {
  messages: AclMessageEvent[];
  total: number;
  dropped: number;
}

export type MessagesStreamStatus = 'connecting' | 'open' | 'closed' | 'error';

export interface MessagesStreamOptions {
  onMessage: (message: AclMessageEvent) => void;
  onStatusChange?: (status: MessagesStreamStatus) => void;
}

export interface ScenarioParamInfo {
  type: 'int' | 'string' | 'boolean';
  defaultValue: unknown;
  minValue?: number;
  maxValue?: number;
  description?: string;
}

export interface ScenarioSummary {
  id: string;
  title: string;
  description: string;
  params: Record<string, ScenarioParamInfo>;
}

export interface ScenarioListResponse {
  scenarios: ScenarioSummary[];
}

export interface ScenarioAgentRef {
  name: string;
}

export interface ScenarioInstance {
  instance: string;
  scenarioId: string;
  container: string;
  agents: ScenarioAgentRef[];
}

export interface ScenarioInstancesResponse {
  instances: ScenarioInstance[];
}

export interface ScenarioStartRequest {
  instanceName?: string;
  config?: Record<string, unknown>;
}

export interface ScenarioStartResponse {
  message: string;
  instance: string;
  scenarioId: string;
  container: string;
  agents: string[];
}
