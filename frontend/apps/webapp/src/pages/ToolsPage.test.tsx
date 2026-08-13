import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { ThemeProvider, createTheme, CssBaseline } from '@mui/material';
import ToolsPage from './ToolsPage';

const { mockStart } = vi.hoisted(() => ({
  mockStart: vi.fn(),
}));

vi.mock('shared/api/factory', () => ({
  api: {
    platform: { health: vi.fn(), getInfo: vi.fn(), shutdown: vi.fn(), version: vi.fn() },
    containers: { list: vi.fn(), get: vi.fn(), kill: vi.fn(), save: vi.fn(), load: vi.fn(), listMTPs: vi.fn(), installMTP: vi.fn(), uninstallMTP: vi.fn() },
    agents: { list: vi.fn(), get: vi.fn(), deploy: vi.fn(), kill: vi.fn(), suspend: vi.fn(), resume: vi.fn(), freeze: vi.fn(), thaw: vi.fn(), clone: vi.fn(), move: vi.fn(), save: vi.fn(), load: vi.fn(), changeOwnership: vi.fn(), registerRemote: vi.fn() },
    tools: { start: mockStart },
    platforms: { list: vi.fn(), add: vi.fn(), fetch: vi.fn(), remove: vi.fn(), getDescription: vi.fn(), refreshDescription: vi.fn(), listAgents: vi.fn() },
    df: {
      listRegistrations: vi.fn(),
      register: vi.fn(),
      deregister: vi.fn(),
      getRegistration: vi.fn(),
      modifyRegistration: vi.fn(),
      search: vi.fn(),
      getDescription: vi.fn(),
      getStatus: vi.fn(),
      refresh: vi.fn(),
      getParents: vi.fn(),
      getChildren: vi.fn(),
      federate: vi.fn(),
      deregisterParent: vi.fn(),
      deregisterChild: vi.fn(),
    },
  },
}));

const theme = createTheme({ palette: { mode: 'dark' } });

const renderWithTheme = (ui: React.ReactElement) => {
  return render(
    <ThemeProvider theme={theme}>
      <CssBaseline />
      {ui}
    </ThemeProvider>
  );
};

describe('ToolsPage', () => {
  beforeEach(() => {
    mockStart.mockReset();
  });

  it('Given page loads, When renders, Then shows all tool buttons', async () => {
    // Arrange

    // Act
    renderWithTheme(<ToolsPage />);

    // Assert
    await waitFor(() => {
      expect(screen.getByText('Tools')).toBeInTheDocument();
    });
    expect(screen.getByText('Start sniffer')).toBeInTheDocument();
    expect(screen.getByText('Start dummy')).toBeInTheDocument();
    expect(screen.getByText('Start logger')).toBeInTheDocument();
    expect(screen.getByText('Start introspector')).toBeInTheDocument();
    expect(screen.getByText('Start df-gui')).toBeInTheDocument();
  });

  it('Given tool launched successfully, When start clicked, Then shows success chip', async () => {
    // Arrange
    mockStart.mockResolvedValue({ message: 'Sniffer started', agent: 'sniffer@jade-main' });

    // Act
    renderWithTheme(<ToolsPage />);
    await waitFor(() => screen.getByText('Start sniffer'));
    fireEvent.click(screen.getByText('Start sniffer'));

    // Assert
    await waitFor(() => {
      expect(mockStart).toHaveBeenCalledWith('sniffer', { container: 'Main-Container' });
    });
    expect(screen.getByText('sniffer started')).toBeInTheDocument();
  });

  it('Given tool launch fails, When start clicked, Then shows error', async () => {
    // Arrange
    mockStart.mockRejectedValue(new Error('Tool unavailable'));

    // Act
    renderWithTheme(<ToolsPage />);
    await waitFor(() => screen.getByText('Start sniffer'));
    fireEvent.click(screen.getByText('Start sniffer'));

    // Assert
    await waitFor(() => {
      expect(screen.getByText(/Failed to launch sniffer/)).toBeInTheDocument();
    });
  });
});
