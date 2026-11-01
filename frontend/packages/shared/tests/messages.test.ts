import { describe, it, expect, beforeEach, vi } from 'vitest';
import { MessageAPI } from '../src/api/messages';
import { isInternalProbe } from '../src/api/types';
import type { HttpClient } from '../src/api/http-client';
import type { SendMessageRequest, SendMessageResponse, AclMessageEvent, MessagesRecentResponse } from '../src/api/types';

describe('MessageAPI', () => {
  let mockHttpClient: HttpClient;
  let messageAPI: MessageAPI;

  beforeEach(() => {
    mockHttpClient = {
      get: vi.fn(),
      post: vi.fn(),
      patch: vi.fn(),
      delete: vi.fn(),
    };
    messageAPI = new MessageAPI(mockHttpClient);
  });

  it('Given recent messages called, When recent() resolves, Then returns recent messages', async () => {
    // Arrange
    const mockResponse: MessagesRecentResponse = {
      messages: [
        {
          id: '1',
          timestamp: '2026-10-30T00:00:00Z',
          sender: 'agentA',
          receiver: 'agentB',
          performative: 'INFORM',
          protocol: 'fipa-request',
          ontology: 'onto',
          content: 'hello',
        },
      ],
      total: 1,
      dropped: 0,
    };
    vi.mocked(mockHttpClient.get).mockResolvedValue({ data: mockResponse });

    // Act
    const result = await messageAPI.recent({ limit: 10 });

    // Assert
    expect(mockHttpClient.get).toHaveBeenCalledWith('/messages/recent', { params: { limit: 10 } });
    expect(result).toEqual(mockResponse);
  });

  it('Given message ID, When getById() resolves, Then returns single message', async () => {
    // Arrange
    const mockMessage: AclMessageEvent = {
      id: 'msg-42',
      timestamp: '2026-10-30T00:00:00Z',
      sender: 'agentA',
      receiver: 'agentB',
      performative: 'REQUEST',
      protocol: 'fipa-request',
      ontology: 'onto',
      content: 'full untruncated content',
    };
    vi.mocked(mockHttpClient.get).mockResolvedValue({ data: mockMessage });

    // Act
    const result = await messageAPI.getById('msg-42');

    // Assert
    expect(mockHttpClient.get).toHaveBeenCalledWith('/messages/msg-42');
    expect(result).toEqual(mockMessage);
  });

  it('Given valid message payload, When send() resolves, Then returns SendMessageResponse', async () => {
    // Arrange
    const request: SendMessageRequest = {
      performative: 'INFORM',
      sender: 'sender@jade-main',
      receivers: ['receiver@jade-main'],
      content: 'test content',
      conversationId: 'c-1',
    };
    const mockResponse: SendMessageResponse = {
      message: 'Message sent',
      performative: 'INFORM',
      sender: 'sender@jade-main',
      receivers: ['receiver@jade-main'],
      conversationId: 'c-1',
    };
    vi.mocked(mockHttpClient.post).mockResolvedValue({ data: mockResponse });

    // Act
    const result = await messageAPI.send(request);

    // Assert
    expect(mockHttpClient.post).toHaveBeenCalledWith('/messages', request);
    expect(result).toEqual(mockResponse);
  });

  it('Given invalid message payload, When send() rejects, Then propagates error', async () => {
    // Arrange
    const request: SendMessageRequest = {
      performative: 'INVALID',
      receivers: [],
    };
    vi.mocked(mockHttpClient.post).mockRejectedValue(new Error('At least one receiver is required'));

    // Act & Assert
    await expect(messageAPI.send(request)).rejects.toThrow('At least one receiver is required');
    expect(mockHttpClient.post).toHaveBeenCalledWith('/messages', request);
  });

  describe('isInternalProbe helper', () => {
    it('Given df_req agent name, Then returns true', () => {
      expect(isInternalProbe('df_req_1793472666006_14')).toBe(true);
      expect(isInternalProbe('df_req_1793472666006_14@172.17.0.1:1099/JADE')).toBe(true);
    });

    it('Given ams_req agent name, Then returns true', () => {
      expect(isInternalProbe('ams_req_12345')).toBe(true);
    });

    it('Given normal domain agent name, Then returns false', () => {
      expect(isInternalProbe('rma')).toBe(false);
      expect(isInternalProbe('ams')).toBe(false);
      expect(isInternalProbe('df')).toBe(false);
      expect(isInternalProbe('worker1@platform')).toBe(false);
      expect(isInternalProbe('')).toBe(false);
    });
  });
});
