import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { ThemeProvider, createTheme, CssBaseline } from '@mui/material';
import ContainersPage from './ContainersPage';

const { mockList, mockKill } = vi.hoisted(() => ({
  mockList: vi.fn(),
  mockKill: vi.fn(),
}));

vi.mock('shared/api/factory', () => ({
  api: {
    platform: { health: vi.fn(), getInfo: vi.fn(), shutdown: vi.fn(), version: vi.fn() },
    containers: { list: mockList, get: vi.fn(), kill: mockKill, save: vi.fn(), load: vi.fn(), listMTPs: vi.fn(), installMTP: vi.fn(), uninstallMTP: vi.fn() },
    agents: { list: vi.fn(), get: vi.fn(), deploy: vi.fn(), kill: vi.fn(), suspend: vi.fn(), resume: vi.fn(), freeze: vi.fn(), thaw: vi.fn(), clone: vi.fn(), move: vi.fn(), save: vi.fn(), load: vi.fn(), changeOwnership: vi.fn(), registerRemote: vi.fn() },
    tools: { start: vi.fn() },
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

describe('ContainersPage', () => {
  beforeEach(() => {
    mockList.mockReset();
    mockKill.mockReset();
  });

  it('Given backend reachable, When page loads, Then shows container list', async () => {
    // Arrange
    mockList.mockResolvedValue({
      containers: [
        { name: 'Main-Container', address: '127.0.0.1', port: '1099', isMain: true },
        { name: 'Node1', address: '192.168.1.1', port: '1099', isMain: false },
      ],
    });

    // Act
    renderWithTheme(<ContainersPage />);

    // Assert
    await waitFor(() => {
      expect(screen.getByText('Containers')).toBeInTheDocument();
    });
    expect(screen.getByText('Main-Container')).toBeInTheDocument();
    expect(screen.getByText('Node1')).toBeInTheDocument();
    const mainChips = screen.getAllByText('Main');
    expect(mainChips.length).toBe(2);
  });

  it('Given empty container list, When page loads, Then shows no containers message', async () => {
    // Arrange
    mockList.mockResolvedValue({ containers: [] });

    // Act
    renderWithTheme(<ContainersPage />);

    // Assert
    await waitFor(() => {
      expect(screen.getByText('No containers found')).toBeInTheDocument();
    });
  });

  it('Given backend unreachable, When page loads, Then shows error snackbar', async () => {
    // Arrange
    mockList.mockRejectedValue(new Error('Network error'));

    // Act
    renderWithTheme(<ContainersPage />);

    // Assert
    await waitFor(() => {
      expect(screen.getByText(/Failed to fetch containers/)).toBeInTheDocument();
    });
  });

  it('Given secondary container listed, When kill button clicked, Then calls kill API', async () => {
    // Arrange
    mockList.mockResolvedValue({
      containers: [
        { name: 'Node1', address: '192.168.1.1', port: '1099', isMain: false },
      ],
    });
    mockKill.mockResolvedValue({ message: 'killed' });

    // Act
    renderWithTheme(<ContainersPage />);
    await waitFor(() => screen.getByText('Node1'));
    const killButton = screen.getByRole('button', { name: 'Kill Container' });
    fireEvent.click(killButton);

    // Assert
    await waitFor(() => {
      expect(mockKill).toHaveBeenCalledWith('Node1');
    });
  });
});
