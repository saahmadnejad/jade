import { HttpClient } from './http-client';
import type {
  AclMessageEvent,
  MessagesRecentResponse,
  MessagesStreamOptions,
  MessagesStreamStatus,
} from './types';

const STREAM_PATH = '/api/messages/stream';
const RECONNECT_DELAY_MS = 3000;

export class MessageAPI {
  constructor(private httpClient: HttpClient) {}

  recent = async (params?: { limit?: number; from?: string; to?: string }): Promise<MessagesRecentResponse> => {
    const res = await this.httpClient.get<MessagesRecentResponse>('/messages/recent', { params });
    return res.data;
  };

  /** Full single message (untruncated content) by capture id. */
  getById = async (id: string): Promise<AclMessageEvent> => {
    const res = await this.httpClient.get<AclMessageEvent>(`/messages/${id}`);
    return res.data;
  };
}

/**
 * Build the WebSocket URL for the message stream, based on the current
 * page location (ws:// or wss:// depending on the page protocol).
 */
export const messagesStreamUrl = (): string => {
  const proto = window.location.protocol === 'https:' ? 'wss' : 'ws';
  return `${proto}://${window.location.host}${STREAM_PATH}`;
};

/**
 * Subscribe to the live ACL message stream.
 * Returns an unsubscribe function that closes the socket and stops
 * reconnection attempts.
 */
export const subscribeMessagesStream = (options: MessagesStreamOptions): (() => void) => {
  let closedByClient = false;
  let socket: WebSocket | null = null;
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null;

  const connect = (): void => {
    options.onStatusChange?.('connecting');
    socket = new WebSocket(messagesStreamUrl());

    socket.onopen = () => options.onStatusChange?.('open');

    socket.onmessage = (event: MessageEvent<string>) => {
      try {
        options.onMessage(JSON.parse(event.data) as AclMessageEvent);
      } catch {
        // Ignore malformed frames
      }
    };

    socket.onclose = () => {
      if (closedByClient) {
        options.onStatusChange?.('closed');
        return;
      }
      options.onStatusChange?.('error');
      reconnectTimer = setTimeout(connect, RECONNECT_DELAY_MS);
    };

    socket.onerror = () => {
      // onclose fires afterwards and handles reconnection
    };
  };

  connect();

  return () => {
    closedByClient = true;
    if (reconnectTimer) clearTimeout(reconnectTimer);
    socket?.close();
  };
};

export type { MessagesStreamStatus, MessagesStreamOptions };
