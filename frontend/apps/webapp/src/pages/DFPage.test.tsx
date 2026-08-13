import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { ThemeProvider, createTheme, CssBaseline } from '@mui/material';
import DFPage from './DFPage';

const {
  mockListRegistrations, mockRegister, mockDeregister, mockSearch,
  mockModifyRegistration, mockGetParents, mockGetChildren,
  mockFederate, mockDeregisterParent, mockDeregisterChild,
} = vi.hoisted(() => ({
  mockListRegistrations: vi.fn(),
  mockRegister: vi.fn(),
  mockDeregister: vi.fn(),
  mockSearch: vi.fn(),
  mockModifyRegistration: vi.fn(),
  mockGetParents: vi.fn(),
  mockGetChildren: vi.fn(),
  mockFederate: vi.fn(),
  mockDeregisterParent: vi.fn(),
  mockDeregisterChild: vi.fn(),
}));

vi.mock('shared/api/factory', () => ({
  api: {
    platform: { health: vi.fn(), getInfo: vi.fn(), shutdown: vi.fn(), version: vi.fn() },
    containers: { list: vi.fn(), get: vi.fn(), kill: vi.fn(), save: vi.fn(), load: vi.fn(), listMTPs: vi.fn(), installMTP: vi.fn(), uninstallMTP: vi.fn() },
    agents: { list: vi.fn(), get: vi.fn(), deploy: vi.fn(), kill: vi.fn(), suspend: vi.fn(), resume: vi.fn(), freeze: vi.fn(), thaw: vi.fn(), clone: vi.fn(), move: vi.fn(), save: vi.fn(), load: vi.fn(), changeOwnership: vi.fn(), registerRemote: vi.fn() },
    tools: { start: vi.fn() },
    platforms: { list: vi.fn(), add: vi.fn(), fetch: vi.fn(), remove: vi.fn(), getDescription: vi.fn(), refreshDescription: vi.fn(), listAgents: vi.fn() },
    df: {
      listRegistrations: mockListRegistrations,
      register: mockRegister,
      deregister: mockDeregister,
      getRegistration: vi.fn(),
      modifyRegistration: mockModifyRegistration,
      search: mockSearch,
      getDescription: vi.fn(),
      getStatus: vi.fn(),
      refresh: vi.fn(),
      getParents: mockGetParents,
      getChildren: mockGetChildren,
      federate: mockFederate,
      deregisterParent: mockDeregisterParent,
      deregisterChild: mockDeregisterChild,
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

describe('DFPage', () => {
  beforeEach(() => {
    mockListRegistrations.mockReset();
    mockRegister.mockReset();
    mockDeregister.mockReset();
    mockSearch.mockReset();
    mockModifyRegistration.mockReset();
    mockGetParents.mockReset();
    mockGetChildren.mockReset();
    mockFederate.mockReset();
    mockDeregisterParent.mockReset();
    mockDeregisterChild.mockReset();
  });

  it('Given backend reachable, When page loads, Then shows registrations table', async () => {
    // Arrange
    mockListRegistrations.mockResolvedValue({
      registrations: [
        { name: 'agent1@host', addresses: ['addr1'], services: [{ type: 'svc', name: 'svc1', ownership: '' }], ownership: 'own' },
      ],
    });
    mockGetParents.mockResolvedValue({ parents: [] });
    mockGetChildren.mockResolvedValue({ children: [] });

    // Act
    renderWithTheme(<DFPage />);

    // Assert
    await waitFor(() => {
      expect(screen.getByText('Directory Facilitator (DF)')).toBeInTheDocument();
    });
    expect(screen.getByText('agent1@host')).toBeInTheDocument();
    expect(screen.getByText('svc1 (svc)')).toBeInTheDocument();
  });

  it('Given empty registrations, When page loads, Then shows no registrations message', async () => {
    // Arrange
    mockListRegistrations.mockResolvedValue({ registrations: [] });
    mockGetParents.mockResolvedValue({ parents: [] });
    mockGetChildren.mockResolvedValue({ children: [] });

    // Act
    renderWithTheme(<DFPage />);

    // Assert
    await waitFor(() => {
      expect(screen.getByText('No agents registered with DF')).toBeInTheDocument();
    });
  });

  it('Given backend unreachable, When page loads, Then shows error snackbar', async () => {
    // Arrange
    mockListRegistrations.mockRejectedValue(new Error('Network error'));
    mockGetParents.mockResolvedValue({ parents: [] });
    mockGetChildren.mockResolvedValue({ children: [] });

    // Act
    renderWithTheme(<DFPage />);

    // Assert
    await waitFor(() => {
      expect(screen.getByText(/Failed to fetch DF data/)).toBeInTheDocument();
    });
  });

  it('Given registration form filled, When register submitted, Then calls register API', async () => {
    // Arrange
    mockListRegistrations.mockResolvedValue({ registrations: [] });
    mockGetParents.mockResolvedValue({ parents: [] });
    mockGetChildren.mockResolvedValue({ children: [] });
    mockRegister.mockResolvedValue({ message: 'registered', registration: { name: 'a1@host', addresses: [], services: [], ownership: '' } });

    // Act
    renderWithTheme(<DFPage />);
    await waitFor(() => screen.getByText('Register Agent'));
    fireEvent.click(screen.getByText('Register Agent'));
    await waitFor(() => screen.getByLabelText('Agent Name'));
    fireEvent.change(screen.getByLabelText('Agent Name'), { target: { value: 'a1@host' } });
    fireEvent.click(screen.getByText('Register'));

    // Assert
    await waitFor(() => {
      expect(mockRegister).toHaveBeenCalled();
    });
  });

  it('Given registration listed, When deregister clicked and confirmed, Then calls deregister API', async () => {
    // Arrange
    mockListRegistrations.mockResolvedValue({
      registrations: [
        { name: 'agent1@host', addresses: [], services: [], ownership: '' },
      ],
    });
    mockGetParents.mockResolvedValue({ parents: [] });
    mockGetChildren.mockResolvedValue({ children: [] });
    mockDeregister.mockResolvedValue({ message: 'deregistered' });
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    // Act
    renderWithTheme(<DFPage />);
    await waitFor(() => screen.getByText('agent1@host'));
    const deregButton = screen.getByRole('button', { name: 'Deregister' });
    fireEvent.click(deregButton);

    // Assert
    await waitFor(() => {
      expect(mockDeregister).toHaveBeenCalledWith('agent1@host');
    });
    vi.restoreAllMocks();
  });

  it('Given registration listed, When modify clicked and form submitted, Then calls modifyRegistration API', async () => {
    // Arrange
    mockListRegistrations.mockResolvedValue({
      registrations: [
        { name: 'agent1@host', addresses: ['addr1'], services: [{ type: 'svc', name: 'svc1', ownership: '' }], ownership: 'own' },
      ],
    });
    mockGetParents.mockResolvedValue({ parents: [] });
    mockGetChildren.mockResolvedValue({ children: [] });
    mockModifyRegistration.mockResolvedValue({ message: 'modified', registration: { name: 'agent1@host', addresses: [], services: [], ownership: '' } });

    // Act
    renderWithTheme(<DFPage />);
    await waitFor(() => screen.getByText('agent1@host'));
    const modifyButton = screen.getByRole('button', { name: 'Modify Registration' });
    fireEvent.click(modifyButton);
    await waitFor(() => screen.getByText('Modify Registration'));
    fireEvent.click(screen.getByText('Modify'));

    // Assert
    await waitFor(() => {
      expect(mockModifyRegistration).toHaveBeenCalledWith('agent1@host', expect.objectContaining({
        addresses: ['addr1'],
        services: expect.arrayContaining([expect.objectContaining({ type: 'svc', name: 'svc1' })]),
      }));
    });
  });

  it('Given parent DFs exist, When page loads, Then shows federation summary chips', async () => {
    // Arrange
    mockListRegistrations.mockResolvedValue({ registrations: [] });
    mockGetParents.mockResolvedValue({
      parents: [{ name: 'parent-df@rp1', addresses: ['jades://10.0.0.1:1099'] }],
    });
    mockGetChildren.mockResolvedValue({ children: [] });

    // Act
    renderWithTheme(<DFPage />);

    // Assert
    await waitFor(() => {
      expect(screen.getByText('Parents: 1')).toBeInTheDocument();
    });
    expect(screen.getByText('Children: 0')).toBeInTheDocument();
  });

  it('Given parents tab active, When parent listed, Then shows parent name and deregister button', async () => {
    // Arrange
    mockListRegistrations.mockResolvedValue({ registrations: [] });
    mockGetParents.mockResolvedValue({
      parents: [{ name: 'parent-df@rp1', addresses: ['jades://10.0.0.1:1099'] }],
    });
    mockGetChildren.mockResolvedValue({ children: [] });

    // Act
    renderWithTheme(<DFPage />);
    await waitFor(() => screen.getByText('Parents: 1'));
    fireEvent.click(screen.getByText('Parents: 1'));

    // Assert
    await waitFor(() => {
      expect(screen.getByText('parent-df@rp1')).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: 'Deregister from Parent' })).toBeInTheDocument();
  });

  it('Given children tab active, When child listed, Then shows child name and deregister button', async () => {
    // Arrange
    mockListRegistrations.mockResolvedValue({ registrations: [] });
    mockGetParents.mockResolvedValue({ parents: [] });
    mockGetChildren.mockResolvedValue({
      children: [{ name: 'child-df@rp1', addresses: ['jades://10.0.0.2:1099'] }],
    });

    // Act
    renderWithTheme(<DFPage />);
    await waitFor(() => screen.getByText('Children: 1'));
    fireEvent.click(screen.getByText('Children: 1'));

    // Assert
    await waitFor(() => {
      expect(screen.getByText('child-df@rp1')).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: 'Deregister Child' })).toBeInTheDocument();
  });

  it('Given "Federate with Parent DF" clicked, When form submitted, Then calls federate API', async () => {
    // Arrange
    mockListRegistrations.mockResolvedValue({ registrations: [] });
    mockGetParents.mockResolvedValue({ parents: [] });
    mockGetChildren.mockResolvedValue({ children: [] });
    mockFederate.mockResolvedValue({ message: 'federated', parent: { name: 'p@rp1', addresses: ['addr'] } });

    // Act
    renderWithTheme(<DFPage />);
    await waitFor(() => screen.getByText('Register Agent'));
    fireEvent.click(screen.getByText('Parents: 0'));
    await waitFor(() => screen.getByText('Federate with Parent DF'));
    fireEvent.click(screen.getByText('Federate with Parent DF'));
    await waitFor(() => screen.getByLabelText('Parent DF Name'));
    fireEvent.change(screen.getByLabelText('Parent DF Name'), { target: { value: 'p@rp1' } });
    fireEvent.change(screen.getByLabelText('Parent DF Addresses (comma-separated)'), { target: { value: 'jades://10.0.0.1:1099' } });
    fireEvent.click(screen.getByText('Federate'));

    // Assert
    await waitFor(() => {
      expect(mockFederate).toHaveBeenCalledWith({ parentDF: 'p@rp1', parentDFAddresses: ['jades://10.0.0.1:1099'] });
    });
  });

  it('Given parent DF listed, When deregister clicked and confirmed, Then calls deregisterParent API', async () => {
    // Arrange
    mockListRegistrations.mockResolvedValue({ registrations: [] });
    mockGetParents.mockResolvedValue({
      parents: [{ name: 'parent-df@rp1', addresses: ['jades://10.0.0.1:1099'] }],
    });
    mockGetChildren.mockResolvedValue({ children: [] });
    mockDeregisterParent.mockResolvedValue({ message: 'deregistered' });
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    // Act
    renderWithTheme(<DFPage />);
    await waitFor(() => screen.getByText('Parents: 1'));
    fireEvent.click(screen.getByText('Parents: 1'));
    await waitFor(() => screen.getByText('parent-df@rp1'));
    fireEvent.click(screen.getByRole('button', { name: 'Deregister from Parent' }));

    // Assert
    await waitFor(() => {
      expect(mockDeregisterParent).toHaveBeenCalledWith('parent-df@rp1');
    });
    vi.restoreAllMocks();
  });

  it('Given child DF listed, When deregister clicked and confirmed, Then calls deregisterChild API', async () => {
    // Arrange
    mockListRegistrations.mockResolvedValue({ registrations: [] });
    mockGetParents.mockResolvedValue({ parents: [] });
    mockGetChildren.mockResolvedValue({
      children: [{ name: 'child-df@rp1', addresses: ['jades://10.0.0.2:1099'] }],
    });
    mockDeregisterChild.mockResolvedValue({ message: 'deregistered' });
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    // Act
    renderWithTheme(<DFPage />);
    await waitFor(() => screen.getByText('Children: 1'));
    fireEvent.click(screen.getByText('Children: 1'));
    await waitFor(() => screen.getByText('child-df@rp1'));
    fireEvent.click(screen.getByRole('button', { name: 'Deregister Child' }));

    // Assert
    await waitFor(() => {
      expect(mockDeregisterChild).toHaveBeenCalledWith('child-df@rp1');
    });
    vi.restoreAllMocks();
  });
});
