import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { ThemeProvider, createTheme, CssBaseline } from '@mui/material';
import AgentsPage from './AgentsPage';

const {
  mockList, mockKill, mockSuspend, mockResume, mockFreeze, mockThaw, mockDeploy,
  mockClone, mockMove, mockChangeOwnership,
} = vi.hoisted(() => ({
  mockList: vi.fn(),
  mockKill: vi.fn(),
  mockSuspend: vi.fn(),
  mockResume: vi.fn(),
  mockFreeze: vi.fn(),
  mockThaw: vi.fn(),
  mockDeploy: vi.fn(),
  mockClone: vi.fn(),
  mockMove: vi.fn(),
  mockChangeOwnership: vi.fn(),
}));

vi.mock('shared/api/factory', () => ({
  api: {
    platform: { health: vi.fn(), getInfo: vi.fn(), shutdown: vi.fn(), version: vi.fn() },
    containers: { list: vi.fn(), get: vi.fn(), kill: vi.fn(), save: vi.fn(), load: vi.fn(), listMTPs: vi.fn(), installMTP: vi.fn(), uninstallMTP: vi.fn() },
    agents: {
      list: mockList,
      get: vi.fn(),
      deploy: mockDeploy,
      kill: mockKill,
      suspend: mockSuspend,
      resume: mockResume,
      freeze: mockFreeze,
      thaw: mockThaw,
      clone: mockClone,
      move: mockMove,
      save: vi.fn(),
      load: vi.fn(),
      changeOwnership: mockChangeOwnership,
      registerRemote: vi.fn(),
    },
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

describe('AgentsPage', () => {
  beforeEach(() => {
    mockList.mockReset();
    mockKill.mockReset();
    mockDeploy.mockReset();
    mockClone.mockReset();
    mockMove.mockReset();
    mockChangeOwnership.mockReset();
  });

  it('Given backend reachable, When page loads, Then shows agent list', async () => {
    // Arrange
    mockList.mockResolvedValue({
      agents: [
        { name: 'rma@main', state: 'ACTIVE', ownership: 'init', container: 'Main-Container', addresses: [] },
      ],
    });

    // Act
    renderWithTheme(<AgentsPage />);

    // Assert
    await waitFor(() => {
      expect(screen.getByText('Agents')).toBeInTheDocument();
    });
    expect(screen.getByText('rma@main')).toBeInTheDocument();
    expect(screen.getByText('ACTIVE')).toBeInTheDocument();
  });

  it('Given empty agent list, When page loads, Then shows no agents message', async () => {
    // Arrange
    mockList.mockResolvedValue({ agents: [] });

    // Act
    renderWithTheme(<AgentsPage />);

    // Assert
    await waitFor(() => {
      expect(screen.getByText('No agents found')).toBeInTheDocument();
    });
  });

  it('Given backend unreachable, When page loads, Then shows error snackbar', async () => {
    // Arrange
    mockList.mockRejectedValue(new Error('Network error'));

    // Act
    renderWithTheme(<AgentsPage />);

    // Assert
    await waitFor(() => {
      expect(screen.getByText(/Failed to fetch agents/)).toBeInTheDocument();
    });
  });

  it('Given agent list loaded, When kill button clicked, Then calls kill API', async () => {
    // Arrange
    mockList.mockResolvedValue({
      agents: [
        { name: 'rma@main', state: 'ACTIVE', ownership: 'init', container: 'Main-Container', addresses: [] },
      ],
    });
    mockKill.mockResolvedValue({ message: 'killed' });

    // Act
    renderWithTheme(<AgentsPage />);
    await waitFor(() => screen.getByText('rma@main'));
    const killButton = screen.getByRole('button', { name: 'Kill Agent' });
    fireEvent.click(killButton);

    // Assert
    await waitFor(() => {
      expect(mockKill).toHaveBeenCalledWith('rma@main');
    });
  });

  it('Given agent list loaded, When clone button clicked and form submitted, Then calls clone API', async () => {
    // Arrange
    mockList.mockResolvedValue({
      agents: [
        { name: 'rma@main', state: 'ACTIVE', ownership: 'init', container: 'Main-Container', addresses: [] },
      ],
    });
    mockClone.mockResolvedValue({ message: 'cloned', name: 'rma-clone@main' });

    // Act
    renderWithTheme(<AgentsPage />);
    await waitFor(() => screen.getByText('rma@main'));
    const cloneButton = screen.getByRole('button', { name: 'Clone Agent' });
    fireEvent.click(cloneButton);
    await waitFor(() => screen.getByLabelText('New Agent Name'));
    fireEvent.change(screen.getByLabelText('New Agent Name'), { target: { value: 'rma-clone' } });
    fireEvent.click(screen.getByText('Clone'));

    // Assert
    await waitFor(() => {
      expect(mockClone).toHaveBeenCalledWith({ name: 'rma', newName: 'rma-clone', container: undefined });
    });
  });

  it('Given agent list loaded, When move button clicked and form submitted, Then calls move API', async () => {
    // Arrange
    mockList.mockResolvedValue({
      agents: [
        { name: 'rma@main', state: 'ACTIVE', ownership: 'init', container: 'Main-Container', addresses: [] },
      ],
    });
    mockMove.mockResolvedValue({ message: 'moved' });

    // Act
    renderWithTheme(<AgentsPage />);
    await waitFor(() => screen.getByText('rma@main'));
    const moveButton = screen.getByRole('button', { name: 'Move Agent' });
    fireEvent.click(moveButton);
    await waitFor(() => screen.getByLabelText('Target Container'));
    fireEvent.change(screen.getByLabelText('Target Container'), { target: { value: 'Node1' } });
    fireEvent.click(screen.getByText('Move'));

    // Assert
    await waitFor(() => {
      expect(mockMove).toHaveBeenCalledWith('rma@main', { container: 'Node1' });
    });
  });

  it('Given agent list loaded, When ownership button clicked and form submitted, Then calls changeOwnership API', async () => {
    // Arrange
    mockList.mockResolvedValue({
      agents: [
        { name: 'rma@main', state: 'ACTIVE', ownership: 'init', container: 'Main-Container', addresses: [] },
      ],
    });
    mockChangeOwnership.mockResolvedValue({ message: 'changed' });

    // Act
    renderWithTheme(<AgentsPage />);
    await waitFor(() => screen.getByText('rma@main'));
    const ownershipButton = screen.getByRole('button', { name: 'Change Ownership' });
    fireEvent.click(ownershipButton);
    await waitFor(() => screen.getByLabelText('New Ownership'));
    fireEvent.change(screen.getByLabelText('New Ownership'), { target: { value: 'new-owner' } });
    fireEvent.click(screen.getByText('Change'));

    // Assert
    await waitFor(() => {
      expect(mockChangeOwnership).toHaveBeenCalledWith('rma@main', { ownership: 'new-owner' });
    });
  });
});
