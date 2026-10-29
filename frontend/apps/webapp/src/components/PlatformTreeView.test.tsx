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
});
