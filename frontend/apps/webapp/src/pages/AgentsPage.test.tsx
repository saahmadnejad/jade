import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { ThemeProvider, createTheme, CssBaseline } from '@mui/material';
import AgentsPage from './AgentsPage';

const {
  mockList, mockKill, mockSuspend, mockResume, mockFreeze, mockThaw, mockDeploy,
  mockClone, mockMove, mockChangeOwnership, mockSave, mockLoad, mockRegisterRemote,
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
  mockSave: vi.fn(),
  mockLoad: vi.fn(),
  mockRegisterRemote: vi.fn(),
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
       save: mockSave,
      load: mockLoad,
      changeOwnership: mockChangeOwnership,
      registerRemote: mockRegisterRemote,
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
    mockSave.mockReset();
    mockLoad.mockReset();
    mockRegisterRemote.mockReset();
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

  it('Given agent list loaded, When kill button clicked and confirmed, Then calls kill API', async () => {
    // Arrange
    vi.spyOn(window, 'confirm').mockReturnValue(true);
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

  it('Given agent list loaded, When kill button clicked and dismissed, Then kill API is not called', async () => {
    // Arrange
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    mockList.mockResolvedValue({
      agents: [
        { name: 'rma@main', state: 'ACTIVE', ownership: 'init', container: 'Main-Container', addresses: [] },
      ],
    });

    // Act
    renderWithTheme(<AgentsPage />);
    await waitFor(() => screen.getByText('rma@main'));
    const killButton = screen.getByRole('button', { name: 'Kill Agent' });
    fireEvent.click(killButton);

    // Assert
    expect(mockKill).not.toHaveBeenCalled();
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
       expect(mockChangeOwnership).toHaveBeenCalledWith('rma@main', { ownership: 'new-owner' });
    });
  });

  describe('Save Agent', () => {
    it('Given agent list loaded, When save button clicked and repository entered, Then calls save API', async () => {
      // Arrange
      mockList.mockResolvedValue({
        agents: [
          { name: 'rma@main', state: 'ACTIVE', ownership: 'init', container: 'Main-Container', addresses: [] },
        ],
      });
      mockSave.mockResolvedValue({ message: 'saved' });

      // Act
      renderWithTheme(<AgentsPage />);
      await waitFor(() => screen.getByText('rma@main'));
      const saveButton = screen.getByRole('button', { name: 'Save Agent' });
      fireEvent.click(saveButton);
      await waitFor(() => screen.getByLabelText('Repository'));
      fireEvent.change(screen.getByLabelText('Repository'), { target: { value: 'file:///tmp/repo' } });
      fireEvent.click(screen.getByText('Save'));

      // Assert
      await waitFor(() => {
        expect(mockSave).toHaveBeenCalledWith('rma@main', 'file:///tmp/repo');
      });
    });

    it('Given save dialog open and repository empty, Then Save button is disabled', async () => {
      // Arrange
      mockList.mockResolvedValue({
        agents: [
          { name: 'rma@main', state: 'ACTIVE', ownership: 'init', container: 'Main-Container', addresses: [] },
        ],
      });

      // Act
      renderWithTheme(<AgentsPage />);
      await waitFor(() => screen.getByText('rma@main'));
      fireEvent.click(screen.getByRole('button', { name: 'Save Agent' }));
      await waitFor(() => screen.getByLabelText('Repository'));
      fireEvent.change(screen.getByLabelText('Repository'), { target: { value: '' } });

      // Assert
      const saveButton = screen.getByRole('button', { name: 'Save' });
      expect(saveButton).toBeDisabled();
    });
  });

  describe('Load Agent', () => {
    it('Given empty form, When load button clicked and form filled, Then calls load API', async () => {
      // Arrange
      mockList.mockResolvedValue({ agents: [] });
      mockLoad.mockResolvedValue({ message: 'loaded', name: 'fresh@main' });
      mockList
        .mockResolvedValueOnce({ agents: [] })
        .mockResolvedValueOnce({ agents: [{ name: 'fresh@main', state: 'ACTIVE', ownership: 'init', container: 'main', addresses: [] }] });

      // Act
      renderWithTheme(<AgentsPage />);
      await waitFor(() => screen.getByText('Agents'));
      fireEvent.click(screen.getByRole('button', { name: 'Load Agent' }));
      await waitFor(() => screen.getByLabelText('Agent Name'));
      fireEvent.change(screen.getByLabelText('Agent Name'), { target: { value: 'fresh' } });
      fireEvent.change(screen.getByLabelText('Container'), { target: { value: 'main' } });
      fireEvent.change(screen.getByLabelText('Repository'), { target: { value: 'file:///tmp/repo' } });
      fireEvent.click(screen.getByText('Load'));

      // Assert
      await waitFor(() => {
        expect(mockLoad).toHaveBeenCalledWith('fresh', 'main', 'file:///tmp/repo');
      });
    });

    it('Given load dialog open and required fields empty, Then Load button is disabled', async () => {
      // Arrange
      mockList.mockResolvedValue({ agents: [] });

      // Act
      renderWithTheme(<AgentsPage />);
      await waitFor(() => screen.getByText('No agents found'));
      fireEvent.click(screen.getByRole('button', { name: 'Load Agent' }));
      await waitFor(() => screen.getByLabelText('Agent Name'));

      // Assert
      const loadButton = screen.getByRole('button', { name: 'Load' });
      expect(loadButton).toBeDisabled();
    });
  });

  describe('Register Remote Agent', () => {
    it('Given agent list loaded, When register remote button clicked and form submitted, Then calls registerRemote API', async () => {
      // Arrange
      mockList.mockResolvedValue({ agents: [] });
      mockRegisterRemote.mockResolvedValue({ message: 'registered' });
      mockList.mockResolvedValueOnce({ agents: [] }).mockResolvedValueOnce({ agents: [{ name: 'remote@container', state: 'ACTIVE', ownership: 'init', container: 'main', addresses: ['rmi://host:1099'] }] });

      // Act
      renderWithTheme(<AgentsPage />);
      await waitFor(() => screen.getByText('Register Remote'));
      fireEvent.click(screen.getByRole('button', { name: 'Register Remote' }));
      await waitFor(() => screen.getByLabelText('Remote Agent AID'));
      fireEvent.change(screen.getByLabelText('Remote Agent AID'), { target: { value: 'remote@container' } });
      fireEvent.change(screen.getByLabelText('Addresses (comma-separated)'), { target: { value: 'rmi://host:1099' } });
      fireEvent.click(screen.getByText('Register'));

      // Assert
      await waitFor(() => {
        expect(mockRegisterRemote).toHaveBeenCalledWith({ aid: 'remote@container', addresses: ['rmi://host:1099'] });
      });
    });

    it('Given register remote dialog open and AID empty, Then Register button is disabled', async () => {
      // Arrange
      mockList.mockResolvedValue({ agents: [] });

      // Act
      renderWithTheme(<AgentsPage />);
      await waitFor(() => screen.getByText('Register Remote'));
      fireEvent.click(screen.getByRole('button', { name: 'Register Remote' }));
      await waitFor(() => screen.getByLabelText('Remote Agent AID'));

      // Assert
      const registerButton = screen.getByRole('button', { name: 'Register' });
      expect(registerButton).toBeDisabled();
    });
  });
});
