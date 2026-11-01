import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { ThemeProvider, createTheme } from '@mui/material';
import PlatformTreeView from './PlatformTreeView';

const mockGetTree = vi.fn();

vi.mock('shared/api/factory', () => ({
  api: {
    platform: {
      getTree: () => mockGetTree(),
    },
  },
}));

const theme = createTheme({ palette: { mode: 'dark' } });

describe('PlatformTreeView', () => {
  beforeEach(() => {
    mockGetTree.mockReset();
  });

  it('Given tree data available, When loaded, Then renders containers and agents', async () => {
    mockGetTree.mockResolvedValue({
      platformID: 'jade-main',
      containers: [
        {
          name: 'Main-Container',
          address: '127.0.0.1',
          port: '1099',
          isMain: true,
          agents: [
            {
              name: 'ams@jade-main',
              state: 'ACTIVE',
              ownership: 'init',
              container: 'Main-Container',
              addresses: [],
            },
            {
              name: 'df@jade-main',
              state: 'ACTIVE',
              ownership: 'init',
              container: 'Main-Container',
              addresses: [],
            },
          ],
        },
      ],
      remotePlatforms: [],
    });

    render(
      <ThemeProvider theme={theme}>
        <PlatformTreeView />
      </ThemeProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('Platform Hierarchy (AgentTree)')).toBeInTheDocument();
      expect(screen.getByText('Main-Container')).toBeInTheDocument();
      expect(screen.getByText('ams@jade-main')).toBeInTheDocument();
      expect(screen.getByText('df@jade-main')).toBeInTheDocument();
    });
  });

  it('Given agent action clicked, When Send Message clicked, Then triggers onSendMessage callback', async () => {
    mockGetTree.mockResolvedValue({
      platformID: 'jade-main',
      containers: [
        {
          name: 'Main-Container',
          address: '127.0.0.1',
          port: '1099',
          isMain: true,
          agents: [
            {
              name: 'worker@jade-main',
              state: 'ACTIVE',
              ownership: 'init',
              container: 'Main-Container',
              addresses: [],
            },
          ],
        },
      ],
      remotePlatforms: [],
    });

    const onSendMessage = vi.fn();

    render(
      <ThemeProvider theme={theme}>
        <PlatformTreeView onSendMessage={onSendMessage} />
      </ThemeProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('worker@jade-main')).toBeInTheDocument();
    });

    // Open context menu
    const menuButtons = screen.getAllByRole('button', { name: 'Agent Actions' });
    fireEvent.click(menuButtons[0]);

    // Click "Send Message"
    const sendMenuItem = await screen.findByText('Send Message');
    fireEvent.click(sendMenuItem);

    expect(onSendMessage).toHaveBeenCalledWith('worker@jade-main');
  });

  it('Given backend fails to load tree, When rendered, Then displays error alert', async () => {
    mockGetTree.mockRejectedValue(new Error('Network error loading tree'));

    render(
      <ThemeProvider theme={theme}>
        <PlatformTreeView />
      </ThemeProvider>
    );

    await waitFor(() => {
      expect(screen.getByText(/Network error loading tree/)).toBeInTheDocument();
    });
  });

  it('Given agent context menu opened, When action items clicked, Then triggers callbacks', async () => {
    mockGetTree.mockResolvedValue({
      platformID: 'jade-main',
      containers: [
        {
          name: 'Main-Container',
          address: '127.0.0.1',
          port: '1099',
          isMain: true,
          agents: [
            {
              name: 'target@jade-main',
              state: 'ACTIVE',
              ownership: 'user',
              container: 'Main-Container',
              addresses: ['http://127.0.0.1/acc'],
            },
          ],
        },
      ],
      remotePlatforms: [],
    });

    const onMoveAgent = vi.fn();
    const onCloneAgent = vi.fn();
    const onFreezeAgent = vi.fn();
    const onInspectAgent = vi.fn();
    const onKillAgent = vi.fn();

    render(
      <ThemeProvider theme={theme}>
        <PlatformTreeView
          onMoveAgent={onMoveAgent}
          onCloneAgent={onCloneAgent}
          onFreezeAgent={onFreezeAgent}
          onInspectAgent={onInspectAgent}
          onKillAgent={onKillAgent}
        />
      </ThemeProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('target@jade-main')).toBeInTheDocument();
    });

    // Inspect Details
    const menuButtons = screen.getAllByRole('button', { name: 'Agent Actions' });
    fireEvent.click(menuButtons[0]);
    const inspectItem = await screen.findByText('Inspect Details');
    fireEvent.click(inspectItem);
    expect(onInspectAgent).toHaveBeenCalledWith(expect.objectContaining({ name: 'target@jade-main' }));

    // Move Agent
    fireEvent.click(menuButtons[0]);
    const moveItem = await screen.findByText('Move to Container');
    fireEvent.click(moveItem);
    expect(onMoveAgent).toHaveBeenCalledWith('target@jade-main');

    // Clone Agent
    fireEvent.click(menuButtons[0]);
    const cloneItem = await screen.findByText('Clone Agent');
    fireEvent.click(cloneItem);
    expect(onCloneAgent).toHaveBeenCalledWith('target@jade-main');

    // Freeze Agent
    fireEvent.click(menuButtons[0]);
    const freezeItem = await screen.findByText('Freeze Agent');
    fireEvent.click(freezeItem);
    expect(onFreezeAgent).toHaveBeenCalledWith('target@jade-main');

    // Kill Agent
    fireEvent.click(menuButtons[0]);
    const killItem = await screen.findByText('Kill Agent');
    fireEvent.click(killItem);
    expect(onKillAgent).toHaveBeenCalledWith('target@jade-main');
  });

  it('Given peripheral container, When container menu opened, Then triggers container actions', async () => {
    mockGetTree.mockResolvedValue({
      platformID: 'jade-main',
      containers: [
        {
          name: 'Peripheral-1',
          address: '127.0.0.1',
          port: '1100',
          isMain: false,
          agents: [],
        },
      ],
      remotePlatforms: [
        {
          name: 'RemotePlatform-1',
          ams: 'ams@remote',
          addresses: ['http://remote:7778/acc'],
          services: [],
        },
      ],
    });

    const onKillContainer = vi.fn();
    const onSaveContainer = vi.fn();
    const onRemoveRemotePlatform = vi.fn();

    render(
      <ThemeProvider theme={theme}>
        <PlatformTreeView
          onKillContainer={onKillContainer}
          onSaveContainer={onSaveContainer}
          onRemoveRemotePlatform={onRemoveRemotePlatform}
        />
      </ThemeProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('Peripheral-1')).toBeInTheDocument();
    });

    // Container menu button
    const containerMenuButton = screen.getAllByTestId('MoreVertIcon')[0];
    fireEvent.click(containerMenuButton);

    const saveItem = await screen.findByText('Save Container');
    fireEvent.click(saveItem);
    expect(onSaveContainer).toHaveBeenCalledWith('Peripheral-1');

    fireEvent.click(containerMenuButton);
    const killItem = await screen.findByText('Kill Container');
    fireEvent.click(killItem);
    expect(onKillContainer).toHaveBeenCalledWith('Peripheral-1');

    // Remote platform menu button
    const remoteMenuButtons = screen.getAllByRole('button', { name: 'Remove Platform' });
    fireEvent.click(remoteMenuButtons[0]);
    const removeRemoteItem = await screen.findByText('Remove Remote Platform');
    fireEvent.click(removeRemoteItem);
    expect(onRemoveRemotePlatform).toHaveBeenCalledWith('RemotePlatform-1');
  });
});
