import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { ThemeProvider, createTheme, CssBaseline } from '@mui/material';
import ContainersPage from './ContainersPage';

const {
  mockList, mockKill, mockSave, mockLoad,
  mockListMtp, mockInstallMtp, mockUninstallMtp,
} = vi.hoisted(() => ({
  mockList: vi.fn(),
  mockKill: vi.fn(),
  mockSave: vi.fn(),
  mockLoad: vi.fn(),
  mockListMtp: vi.fn(),
  mockInstallMtp: vi.fn(),
  mockUninstallMtp: vi.fn(),
}));

vi.mock('shared/api/factory', () => ({
  api: {
    platform: { health: vi.fn(), getInfo: vi.fn(), shutdown: vi.fn(), version: vi.fn() },
    containers: {
      list: mockList,
      get: vi.fn(),
      kill: mockKill,
      save: mockSave,
      load: mockLoad,
      listMTPs: mockListMtp,
      installMTP: mockInstallMtp,
      uninstallMTP: mockUninstallMtp,
    },
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

const mockContainers = (containers: any[]) => {
  mockList.mockResolvedValue({ containers });
};

describe('ContainersPage', () => {
  beforeEach(() => {
    mockList.mockReset();
    mockKill.mockReset();
    mockSave.mockReset();
    mockLoad.mockReset();
    mockListMtp.mockReset();
    mockInstallMtp.mockReset();
    mockUninstallMtp.mockReset();
  });

  it('Given backend reachable, When page loads, Then shows container list', async () => {
    // Arrange
    mockContainers([
      { name: 'Main-Container', address: '127.0.0.1', port: '1099', isMain: true },
      { name: 'Node1', address: '192.168.1.1', port: '1099', isMain: false },
    ]);

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
    expect(screen.getAllByText('Secondary')).toHaveLength(1);
  });

  it('Given empty container list, When page loads, Then shows no containers message', async () => {
    // Arrange
    mockContainers([]);

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

  it('Given secondary container listed, When kill button clicked and confirmed, Then calls kill API', async () => {
    // Arrange
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    mockContainers([
      { name: 'Node1', address: '192.168.1.1', port: '1099', isMain: false },
    ]);
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

  it('Given secondary container listed, When kill button clicked and dismissed, Then kill API is not called', async () => {
    // Arrange
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    mockContainers([
      { name: 'Node1', address: '192.168.1.1', port: '1099', isMain: false },
    ]);

    // Act
    renderWithTheme(<ContainersPage />);
    await waitFor(() => screen.getByText('Node1'));
    const killButton = screen.getByRole('button', { name: 'Kill Container' });
    fireEvent.click(killButton);

    // Assert
    expect(mockKill).not.toHaveBeenCalled();
  });

  it('Given container listed, When save button clicked and repository entered, Then calls save API', async () => {
    // Arrange
    mockContainers([
      { name: 'Node1', address: '192.168.1.1', port: '1099', isMain: false },
    ]);
    mockSave.mockResolvedValue({ message: 'saved' });

    // Act
    renderWithTheme(<ContainersPage />);
    await waitFor(() => screen.getByText('Node1'));
    fireEvent.click(screen.getByRole('button', { name: 'Save Container' }));
    await waitFor(() => screen.getByLabelText('Repository URL'));
    fireEvent.change(screen.getByLabelText('Repository URL'), { target: { value: 'file:///tmp/store' } });
    fireEvent.click(screen.getByText('Save'));

    // Assert
    await waitFor(() => {
      expect(mockSave).toHaveBeenCalledWith('Node1', 'file:///tmp/store');
    });
  });

  it('Given container listed, When load button clicked and repository entered, Then calls load API', async () => {
    // Arrange
    mockContainers([
      { name: 'Node1', address: '192.168.1.1', port: '1099', isMain: false },
    ]);
    mockLoad.mockResolvedValue({ message: 'loaded' });

    // Act
    renderWithTheme(<ContainersPage />);
    await waitFor(() => screen.getByText('Node1'));
    fireEvent.click(screen.getByRole('button', { name: 'Load Container' }));
    await waitFor(() => screen.getByLabelText('Repository URL'));
    fireEvent.change(screen.getByLabelText('Repository URL'), { target: { value: 'file:///tmp/store' } });
    fireEvent.click(screen.getByText('Load'));

    // Assert
    await waitFor(() => {
      expect(mockLoad).toHaveBeenCalledWith('Node1', 'file:///tmp/store');
    });
  });

  it('Given container listed, When MTP management button clicked, Then lists MTPs', async () => {
    // Arrange
    mockContainers([
      { name: 'Node1', address: '192.168.1.1', port: '1099', isMain: false },
    ]);
    mockListMtp.mockResolvedValue({
      mtps: [{ address: 'jades://10.0.0.1:1099', className: 'io.jade.mtp.MPI' }],
    });

    // Act
    renderWithTheme(<ContainersPage />);
    await waitFor(() => screen.getByText('Node1'));
    const mtpButton = screen.getByRole('button', { name: 'Manage MTPs' });
    fireEvent.click(mtpButton);

    // Assert
    await waitFor(() => {
      expect(mockListMtp).toHaveBeenCalledWith('Node1');
    });
    expect(screen.getByText(/io\.jade\.mtp\.MPI/)).toBeInTheDocument();
  });

  it('Given container listed, When install MTP form submitted, Then calls installMTP API', async () => {
    // Arrange
    mockContainers([
      { name: 'Node1', address: '192.168.1.1', port: '1099', isMain: false },
    ]);
    mockListMtp.mockResolvedValue({ mtps: [] });
    mockInstallMtp.mockResolvedValue({ address: 'addr', className: 'cls' });

    // Act
    renderWithTheme(<ContainersPage />);
    await waitFor(() => screen.getByText('Node1'));
    fireEvent.click(screen.getByRole('button', { name: 'Manage MTPs' }));
    await waitFor(() => screen.getByRole('button', { name: 'Install MTP' }));
    fireEvent.click(screen.getByRole('button', { name: 'Install MTP' }));
    await waitFor(() => screen.getByLabelText('MTP Class Name'));
    fireEvent.change(screen.getByLabelText('MTP Class Name'), { target: { value: 'io.jade.mtp.MPI' } });
    fireEvent.change(screen.getByLabelText('Address'), { target: { value: 'jades://10.0.0.1:1099' } });
    fireEvent.click(screen.getByText('Install'));

    // Assert
    await waitFor(() => {
      expect(mockInstallMtp).toHaveBeenCalledWith('Node1', { className: 'io.jade.mtp.MPI', address: 'jades://10.0.0.1:1099' });
    });
  });
});
