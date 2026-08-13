import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { ThemeProvider, createTheme, CssBaseline } from '@mui/material';
import PlatformsPage from './PlatformsPage';

const { mockList, mockAdd, mockFetch, mockRemove, mockGetDescription } = vi.hoisted(() => ({
  mockList: vi.fn(),
  mockAdd: vi.fn(),
  mockFetch: vi.fn(),
  mockRemove: vi.fn(),
  mockGetDescription: vi.fn(),
}));

vi.mock('shared/api/factory', () => ({
  api: {
    platform: { health: vi.fn(), getInfo: vi.fn(), shutdown: vi.fn(), version: vi.fn() },
    containers: { list: vi.fn(), get: vi.fn(), kill: vi.fn(), save: vi.fn(), load: vi.fn(), listMTPs: vi.fn(), installMTP: vi.fn(), uninstallMTP: vi.fn() },
    agents: { list: vi.fn(), get: vi.fn(), deploy: vi.fn(), kill: vi.fn(), suspend: vi.fn(), resume: vi.fn(), freeze: vi.fn(), thaw: vi.fn(), clone: vi.fn(), move: vi.fn(), save: vi.fn(), load: vi.fn(), changeOwnership: vi.fn(), registerRemote: vi.fn() },
    tools: { start: vi.fn() },
    platforms: {
      list: mockList,
      add: mockAdd,
      fetch: mockFetch,
      remove: mockRemove,
      getDescription: mockGetDescription,
      refreshDescription: vi.fn(),
      listAgents: vi.fn(),
    },
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

describe('PlatformsPage', () => {
  beforeEach(() => {
    mockList.mockReset();
    mockAdd.mockReset();
    mockFetch.mockReset();
    mockRemove.mockReset();
    mockGetDescription.mockReset();
  });

  it('Given backend reachable, When page loads, Then shows platform list', async () => {
    // Arrange
    mockList.mockResolvedValue({
      platforms: [
        { name: 'remote-platform-1', ams: 'ams@rp1', addresses: ['jades://192.168.1.10:1099'], services: ['dirg'] },
      ],
    });

    // Act
    renderWithTheme(<PlatformsPage />);

    // Assert
    await waitFor(() => {
      expect(screen.getByText('Remote Platforms')).toBeInTheDocument();
    });
    expect(screen.getByText('remote-platform-1')).toBeInTheDocument();
    expect(screen.getByText('ams@rp1')).toBeInTheDocument();
  });

  it('Given empty platform list, When page loads, Then shows no platforms message', async () => {
    // Arrange
    mockList.mockResolvedValue({ platforms: [] });

    // Act
    renderWithTheme(<PlatformsPage />);

    // Assert
    await waitFor(() => {
      expect(screen.getByText('No platforms found')).toBeInTheDocument();
    });
  });

  it('Given backend unreachable, When page loads, Then shows error snackbar', async () => {
    // Arrange
    mockList.mockRejectedValue(new Error('Network error'));

    // Act
    renderWithTheme(<PlatformsPage />);

    // Assert
    await waitFor(() => {
      expect(screen.getByText(/Failed to fetch platforms/)).toBeInTheDocument();
    });
  });

  it('Given "Add Platform" clicked, When form filled and submitted via AMS, Then calls add API', async () => {
    // Arrange
    mockList.mockResolvedValue({ platforms: [] });
    mockAdd.mockResolvedValue({ message: 'added', name: 'rp1', ams: 'ams@rp1' });

    // Act
    renderWithTheme(<PlatformsPage />);
    await waitFor(() => screen.getByText('Add Platform'));
    fireEvent.click(screen.getByText('Add Platform'));
    await waitFor(() => screen.getByLabelText('AMS Agent Identifier'));
    fireEvent.change(screen.getByLabelText('AMS Agent Identifier'), { target: { value: 'ams@rp1' } });
    fireEvent.click(screen.getByText('Add'));

    // Assert
    await waitFor(() => {
      expect(mockAdd).toHaveBeenCalledWith({ ams: 'ams@rp1', addresses: [] });
    });
  });

  it('Given "Add Platform" clicked, When via URL mode and submitted, Then calls fetch API', async () => {
    // Arrange
    mockList.mockResolvedValue({ platforms: [] });
    mockFetch.mockResolvedValue({ message: 'fetched', name: 'rp1', ams: 'ams@rp1' });

    // Act
    renderWithTheme(<PlatformsPage />);
    await waitFor(() => screen.getByText('Add Platform'));
    fireEvent.click(screen.getByText('Add Platform'));
    await waitFor(() => screen.getByText('Via URL'));
    fireEvent.click(screen.getByText('Via URL'));
    await waitFor(() => screen.getByLabelText('AP Description URL'));
    fireEvent.change(screen.getByLabelText('AP Description URL'), { target: { value: 'http://192.168.1.10:1099/ap' } });
    fireEvent.click(screen.getByText('Fetch'));

    // Assert
    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalledWith({ url: 'http://192.168.1.10:1099/ap' });
    });
  });

  it('Given platform listed, When remove button clicked and confirmed, Then calls remove API', async () => {
    // Arrange
    mockList.mockResolvedValue({
      platforms: [
        { name: 'rp1', ams: 'ams@rp1', addresses: [], services: [] },
      ],
    });
    mockRemove.mockResolvedValue({ message: 'removed' });
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    // Act
    renderWithTheme(<PlatformsPage />);
    await waitFor(() => screen.getByText('rp1'));
    const removeButton = screen.getByRole('button', { name: 'Remove Platform' });
    fireEvent.click(removeButton);

    // Assert
    await waitFor(() => {
      expect(mockRemove).toHaveBeenCalledWith('rp1');
    });
    vi.restoreAllMocks();
  });

  it('Given platform listed, When view description clicked, Then shows description dialog', async () => {
    // Arrange
    mockList.mockResolvedValue({
      platforms: [
        { name: 'rp1', ams: 'ams@rp1', addresses: ['addr1'], services: ['dirg'] },
      ],
    });
    mockGetDescription.mockResolvedValue({
      name: 'rp1', ams: 'ams@rp1', addresses: ['addr1'], services: ['dirg'],
    });

    // Act
    renderWithTheme(<PlatformsPage />);
    await waitFor(() => screen.getByText('rp1'));
    const viewButton = screen.getByRole('button', { name: 'View Description' });
    fireEvent.click(viewButton);

    // Assert
    await waitFor(() => {
      expect(mockGetDescription).toHaveBeenCalledWith('rp1');
    });
    await waitFor(() => {
      expect(screen.getByText('Platform Description')).toBeInTheDocument();
    });
  });
});
