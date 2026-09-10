import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent, within } from '@testing-library/react';
import { ThemeProvider, createTheme, CssBaseline } from '@mui/material';
import MessagesPage from './MessagesPage';

const { mockRecent, mockGetById } = vi.hoisted(() => ({
  mockRecent: vi.fn(),
  mockGetById: vi.fn(),
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
    messages: { recent: mockRecent, getById: mockGetById },
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
  sender: 'manager',
  receiver: 'architect',
  performative: 'request',
  protocol: 'fipa-request',
  ontology: 'dev-team-ontology',
  content: '(task architect DESIGN)',
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
    await waitFor(() => expect(screen.getByText('manager')).toBeInTheDocument());
    expect(screen.getByText('architect')).toBeInTheDocument();
    expect(screen.getByText('(task architect DESIGN)')).toBeInTheDocument();
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
      sender: 'implementer',
      receiver: 'tester',
      performative: 'request',
      protocol: 'fipa-request',
      ontology: 'dev-team-ontology',
      content: '(peer-task tester TEST-REPORT)',
    });

    // --- Assert ---
    await waitFor(() => expect(screen.getByText('implementer')).toBeInTheDocument());
    const rows = screen.getAllByRole('row');
    expect(rows[1]).toHaveTextContent('implementer'); // first data row = newest message
    expect(rows[2]).toHaveTextContent('manager');      // older history below
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
        { ...sampleMessage, id: '5', sender: 'reviewer', receiver: 'manager', content: '(round-1 REJECTED)' },
      ],
      total: 2,
      dropped: 0,
    });
    renderWithTheme(<MessagesPage />);
    await waitFor(() => expect(screen.getByText(/REJECTED/)).toBeInTheDocument());

    // --- Act ---
    fireEvent.change(screen.getByPlaceholderText(/filter by agent or content/i), {
      target: { value: 'REJECTED' },
    });

    // --- Assert ---
    expect(screen.queryByText('(task architect DESIGN)')).not.toBeInTheDocument();
    expect(screen.getByText(/REJECTED/)).toBeInTheDocument();
  });

  it('Given a message row, When the eye button is clicked, Then a modal shows all details including full content', async () => {
    // --- Arrange ---
    const longContent = '(task implementer IMPLEMENT) -- ' + 'x'.repeat(120) + ' --';
    mockRecent.mockResolvedValue({
      messages: [{ ...sampleMessage, content: longContent }],
      total: 1,
      dropped: 0,
    });
    // The list carries truncated content; getById returns the FULL message.
    const fullContent = longContent + ' TAIL-BEYOND-TRUNCATION';
    mockGetById.mockResolvedValue({ ...sampleMessage, content: fullContent });
    renderWithTheme(<MessagesPage />);
    await waitFor(() => expect(screen.getByText(longContent)).toBeInTheDocument());

    // --- Act ---
    fireEvent.click(screen.getByRole('button', { name: /view message details/i }));

    // --- Assert ---
    await waitFor(() => expect(mockGetById).toHaveBeenCalledWith('1'));
    const dialog = await screen.findByRole('dialog');
    expect(dialog).toHaveTextContent('#1');                  // id chip
    expect(dialog).toHaveTextContent('manager → architect');     // route
    expect(dialog).toHaveTextContent('fipa-request');        // protocol
    expect(dialog).toHaveTextContent('dev-team-ontology');       // ontology
    // Full untruncated content fetched by id and rendered in the modal
    expect(within(dialog).getByText(fullContent)).toBeInTheDocument();
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
