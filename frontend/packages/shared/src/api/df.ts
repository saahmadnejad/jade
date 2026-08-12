import { HttpClient } from './http-client';
import type { ErrorResponse } from './types';

export interface DFServiceInfo {
  type: string;
  name: string;
  ownership?: string;
}

export interface DFRegistrationInfo {
  name: string;
  addresses: string[];
  services: DFServiceInfo[];
  ownership: string;
}

export interface DFRegistrationListResponse {
  registrations: DFRegistrationInfo[];
}

export interface DFRegisterRequest {
  agentName: string;
  addresses?: string[];
  services?: DFServiceInfo[];
}

export interface DFRegisterResponse {
  message: string;
  registration: DFRegistrationInfo;
}

export interface DFModifyRequest {
  addresses?: string[];
  services?: DFServiceInfo[];
}

export interface DFModifyResponse {
  message: string;
  registration: DFRegistrationInfo;
}

export interface DFSearchRequest {
  description?: {
    name?: string;
    services?: { type: string; name?: string }[];
  };
  constraints?: {
    maxDepth?: number;
    maxResults?: number;
  };
}

export interface DFSearchResponse {
  results: DFRegistrationInfo[];
}

export interface DFDescriptionResponse {
  name: string;
  addresses: string[];
  services: DFServiceInfo[];
}

export interface DFStatusResponse {
  running: boolean;
  agent?: string;
  container?: string;
  registeredAgentCount?: number;
  parentCount?: number;
  childCount?: number;
}

export interface DFParentInfo {
  name: string;
  addresses: string[];
}

export interface DFParentsResponse {
  parents: DFParentInfo[];
}

export interface DFChildrenResponse {
  children: DFParentInfo[];
}

export interface DfFederateRequest {
  parentDF: string;
  parentDFAddresses?: string[];
  thisDFDescription?: unknown;
}

export interface DfFederateResponse {
  message: string;
  parent: DFParentInfo;
}

export interface DFRefreshResponse {
  message: string;
  registeredAgentCount: number;
}

export class DFAPI {
  constructor(private httpClient: HttpClient) {}

  listRegistrations = async (): Promise<DFRegistrationListResponse> => {
    const res = await this.httpClient.get<DFRegistrationListResponse>('/df/registrations');
    return res.data;
  };

  register = async (request: DFRegisterRequest): Promise<DFRegisterResponse> => {
    const res = await this.httpClient.post<DFRegisterResponse>('/df/registrations', request);
    return res.data;
  };

  deregister = async (agentName: string): Promise<{ message: string }> => {
    const res = await this.httpClient.delete<{ message: string }>(`/df/registrations/${agentName}`);
    return res.data;
  };

  getRegistration = async (agentName: string): Promise<DFRegistrationInfo> => {
    const res = await this.httpClient.get<DFRegistrationInfo>(`/df/registrations/${agentName}`);
    return res.data;
  };

  modifyRegistration = async (agentName: string, request: DFModifyRequest): Promise<DFModifyResponse> => {
    const res = await this.httpClient.put<DFModifyResponse>(`/df/registrations/${agentName}`, request);
    return res.data;
  };

  search = async (request: DFSearchRequest): Promise<DFSearchResponse> => {
    const res = await this.httpClient.post<DFSearchResponse>('/df/search', request);
    return res.data;
  };

  getDescription = async (): Promise<DFDescriptionResponse> => {
    const res = await this.httpClient.get<DFDescriptionResponse>('/df/description');
    return res.data;
  };

  getStatus = async (): Promise<DFStatusResponse> => {
    const res = await this.httpClient.get<DFStatusResponse>('/tools/df-gui/status');
    return res.data;
  };

  refresh = async (): Promise<DFRefreshResponse> => {
    const res = await this.httpClient.post<DFRefreshResponse>('/df/refresh');
    return res.data;
  };

  // ---- DF Federation ----

  getParents = async (): Promise<DFParentsResponse> => {
    const res = await this.httpClient.get<DFParentsResponse>('/df/federation/parents');
    return res.data;
  };

  getChildren = async (): Promise<DFChildrenResponse> => {
    const res = await this.httpClient.get<DFChildrenResponse>('/df/federation/children');
    return res.data;
  };

  federate = async (request: DfFederateRequest): Promise<DfFederateResponse> => {
    const res = await this.httpClient.post<DfFederateResponse>('/df/federation', request);
    return res.data;
  };

  deregisterParent = async (parentDFName: string): Promise<{ message: string }> => {
    const res = await this.httpClient.delete<{ message: string }>(`/df/federation/${encodeURIComponent(parentDFName)}`);
    return res.data;
  };

  deregisterChild = async (childDFName: string): Promise<{ message: string }> => {
    const res = await this.httpClient.delete<{ message: string }>(`/df/federation/children/${encodeURIComponent(childDFName)}`);
    return res.data;
  };
}
