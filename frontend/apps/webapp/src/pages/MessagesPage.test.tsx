import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent, within } from '@testing-library/react';
import { ThemeProvider, createTheme, CssBaseline } from '@mui/material';
import MessagesPage from './MessagesPage';

const { mockRecent } = vi.hoisted(() => ({
  mockRecent: vi.fn(),
}));

const mockUnsubscribe = vi.fn();

let streamListener: ((data: unknown) => void) | null = null;

vi.mock('shared/api/factory', () => ({
  api: {
    platform: { health: vi.fn(), getInfo: vi.fn(), shutdown: vi.fn(), version: vi.fn() },
    containers: { list: vi.fn(), get: vi.fn(), kill: vi.fn(), save: vi.fn(), load: vi.fn(), listMTPs: vi.fn(), installMTPs: vi.fn(), installMTP: vi.fn(), uninstallMTP: vi.fn() },
    agents: {
      list: vi.fn(), get: vi.fn(), deploy: vi.fn(), kill: vi.fn(), suspend: vi.fn(),
      resume: vi.fn(), freeze: vi.fn(), thaw: vi.fn(), clone: vi.fn(), move: vi.fn(),
      save: vi.fn(), load: vi.fn(), changeOwnership: vi.fn(), registerRemote: vi.fn(),
    },
    tools: { start: vi.fn() },
    platforms: { list: vi.fn(), add: vi.fn(), fetch: vi.fn(), remove: vi.fn(), getDescription: vi.fn(), refreshDescription: vi.fn(), listAgents: vi.fn() },
    df: {
      listRegistrations: vi.fn(), register: vi.fn(), deregister: vi.fn(),
      getRegistration: vi.fn(), modifyRegistration: vi.fn(), search: vi.fn(),
      getDescription: vi.fn(), getStatus: vi.fn(), refresh: vi.fn(),
      getParents: vi.fn(), getChildren: vi.fn(), federate: vi.fn(),
      deregisterParent: vi.fn(), deregisterChild: vi.fn(),
    },
    messages: { recent: mockRecent },
  },
}));

vi.mock('shared', async (importOriginal) => {
  const actual = await importOriginal<Record<string, unknown>>();
  return {
    ...actual,
    subscribeMessagesStream: (options: { onMessage: (data: unknown) => void }) => {
      streamListener = options.onMessage;
      return mockUnsubscribe;
    },
  };
});

const theme = createTheme({ palette: { mode: 'dark' } });

const renderWithTheme = (ui: React.ReactElement) => {
  return render(
    <ThemeProvider theme={theme}>
      <CssBaseline />
      {ui}
    </ThemeProvider>
  );
};

const sampleMessage = {
  id: '1',
  timestamp: '2026-08-25T10:15:30.123Z',
  sender: 'shop',
  receiver: 'inventory',
  performative: 'request',
  protocol: 'fipa-request',
  ontology: 'shop-ontology',
  content: '(reserve sku-1 2)',
};

describe('MessagesPage', () => {
  beforeEach(() => {
    mockRecent.mockReset();
    mockUnsubscribe.mockReset();
    streamListener = null;
    // jsdom WebSocket stub: records handlers, never connects
    (window as any).WebSocket = class {
      onopen: (() => void) | null = null;
      onmessage: ((event: { data: string }) => void) | null = null;
      onclose: (() => void) | null = null;
      onerror: (() => void) | null = null;
      close = vi.fn();
      send = vi.fn();
    };
  });

  it('Given backend reachable, When page loads, Then recent message history is shown', async () => {
    // --- Arrange ---
    mockRecent.mockResolvedValue({ messages: [sampleMessage], total: 1, dropped: 0 });

    // --- Act ---
    renderWithTheme(<MessagesPage />);

    // --- Assert ---
    await waitFor(() => expect(screen.getByText('shop')).toBeInTheDocument());
    expect(screen.getByText('inventory')).toBeInTheDocument();
    expect(screen.getByText('(reserve sku-1 2)')).toBeInTheDocument();
  });

  it('Given live stream frame arrives, When page is open, Then the new message appears at the top', async () => {
    // --- Arrange ---
    mockRecent.mockResolvedValue({ messages: [sampleMessage], total: 1, dropped: 0 });
    renderWithTheme(<MessagesPage />);
    await waitFor(() => expect(streamListener).not.toBeNull());

    // --- Act ---
    streamListener?.({
      id: '2',
      timestamp: '2026-08-25T10:15:31.000Z',
      sender: 'customer1',
      receiver: 'shop',
      performative: 'request',
      protocol: 'fipa-request',
      ontology: 'shop-ontology',
      content: '(buy sku-9)',
    });

    // --- Assert ---
    await waitFor(() => expect(screen.getByText('customer1')).toBeInTheDocument());
    const rows = screen.getAllByRole('row');
    expect(rows[1]).toHaveTextContent('customer1'); // first data row = newest message
    expect(rows[2]).toHaveTextContent('shop');      // older history below
  });

  it('Given stream is paused, When a live frame arrives, Then it is not displayed', async () => {
    // --- Arrange ---
    mockRecent.mockResolvedValue({ messages: [], total: 0, dropped: 0 });
    renderWithTheme(<MessagesPage />);
    await waitFor(() => expect(streamListener).not.toBeNull());

    const pauseButton = screen.getByRole('button', { name: /pause live stream/i });
    fireEvent.click(pauseButton);

    // --- Act ---
    streamListener?.({ ...sampleMessage, id: '3', sender: 'late-sender' });

    // --- Assert ---
    expect(screen.queryByText('late-sender')).not.toBeInTheDocument();
  });

  it('Given filter text entered, When messages match content, Then only matching rows are shown', async () => {
    // --- Arrange ---
    mockRecent.mockResolvedValue({
      messages: [
        sampleMessage,
        { ...sampleMessage, id: '5', sender: 'restock', receiver: 'supplier', content: '(ship sku-7)' },
      ],
      total: 2,
      dropped: 0,
    });
    renderWithTheme(<MessagesPage />);
    await waitFor(() => expect(screen.getByText(/sku-7/)).toBeInTheDocument());

    // --- Act ---
    fireEvent.change(screen.getByPlaceholderText(/filter by agent or content/i), {
      target: { value: 'sku-7' },
    });

    // --- Assert ---
    expect(screen.queryByText('(reserve sku-1 2)')).not.toBeInTheDocument();
    expect(screen.getByText(/sku-7/)).toBeInTheDocument();
  });

  it('Given a message row, When the eye button is clicked, Then a modal shows all details including full content', async () => {
    // --- Arrange ---
    const longContent = '(action shop (buy sku-123 2)) /* ' + 'x'.repeat(120) + ' */';
    mockRecent.mockResolvedValue({
      messages: [{ ...sampleMessage, content: longContent }],
      total: 1,
      dropped: 0,
    });
    renderWithTheme(<MessagesPage />);
    await waitFor(() => expect(screen.getByText(longContent)).toBeInTheDocument());

    // --- Act ---
    fireEvent.click(screen.getByRole('button', { name: /view message details/i }));

    // --- Assert ---
    const dialog = await screen.findByRole('dialog');
    expect(dialog).toHaveTextContent('Message #1');          // id
    expect(dialog).toHaveTextContent('shop');                // sender
    expect(dialog).toHaveTextContent('inventory');           // receiver
    expect(dialog).toHaveTextContent('fipa-request');        // protocol
    expect(dialog).toHaveTextContent('shop-ontology');       // ontology
    // Full untruncated content inside the modal (also present in the table cell)
    expect(within(dialog).getByText(longContent)).toBeInTheDocument();
  });

  it('Given page unmounts, When leaving page, Then stream subscription is cancelled', async () => {
    // --- Arrange ---
    mockRecent.mockResolvedValue({ messages: [], total: 0, dropped: 0 });
    const { unmount } = renderWithTheme(<MessagesPage />);
    await waitFor(() => expect(streamListener).not.toBeNull());

    // --- Act ---
    unmount();

    // --- Assert ---
    expect(mockUnsubscribe).toHaveBeenCalled();
  });
});
