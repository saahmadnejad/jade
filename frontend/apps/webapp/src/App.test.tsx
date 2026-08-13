import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { ThemeProvider, createTheme, CssBaseline } from '@mui/material';
import DashboardPage from './pages/DashboardPage';

const { mockHealthCheck, mockGetPlatformInfo, mockShutdown } = vi.hoisted(() => ({
  mockHealthCheck: vi.fn(),
  mockGetPlatformInfo: vi.fn(),
  mockShutdown: vi.fn(),
}));

vi.mock('shared/api/factory', () => ({
  api: {
    platform: {
      health: mockHealthCheck,
      getInfo: mockGetPlatformInfo,
      shutdown: mockShutdown,
      version: vi.fn(),
    },
  },
}));

const theme = createTheme({ palette: { mode: 'dark' } });

describe('DashboardPage', () => {
  beforeEach(() => {
    mockHealthCheck.mockReset();
    mockGetPlatformInfo.mockReset();
    mockShutdown.mockReset();
  });

  it('Given backend reachable, When page loads, Then shows platform info', async () => {
    // Arrange
    mockHealthCheck.mockResolvedValue({ status: 'ok' });
    mockGetPlatformInfo.mockResolvedValue({
      platformID: 'jade-main',
      containerName: 'Main-Container',
      isMain: true,
      ams: 'ams@main',
      defaultDF: 'df@main',
    });

    // Act
    render(
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <DashboardPage />
      </ThemeProvider>
    );

    // Assert
    await waitFor(() => {
      expect(screen.getByText('Platform Dashboard')).toBeInTheDocument();
    });
    expect(screen.getByText('Healthy')).toBeInTheDocument();
    expect(screen.getByText(/jade-main/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Shutdown Platform' })).toBeInTheDocument();
  });

  it('Given shutdown button clicked and confirmed, When confirmed, Then calls shutdown API', async () => {
    // Arrange
    mockHealthCheck.mockResolvedValue({ status: 'ok' });
    mockGetPlatformInfo.mockResolvedValue({
      platformID: 'jade-main',
      containerName: 'Main-Container',
      isMain: true,
      ams: 'ams@main',
      defaultDF: 'df@main',
    });
    mockShutdown.mockResolvedValue({ message: 'Platform shutdown initiated' });
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    // Act
    render(
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <DashboardPage />
      </ThemeProvider>
    );

    // Assert
    await waitFor(() => {
      expect(screen.getByText('Platform Dashboard')).toBeInTheDocument();
    });
    const shutdownButton = screen.getByRole('button', { name: 'Shutdown Platform' });
    fireEvent.click(shutdownButton);
    await waitFor(() => {
      expect(mockShutdown).toHaveBeenCalled();
    });
    vi.restoreAllMocks();
  });

  it('Given shutdown button clicked and cancelled, Then does not call shutdown API', async () => {
    // Arrange
    mockHealthCheck.mockResolvedValue({ status: 'ok' });
    mockGetPlatformInfo.mockResolvedValue({
      platformID: 'jade-main',
      containerName: 'Main-Container',
      isMain: true,
      ams: 'ams@main',
      defaultDF: 'df@main',
    });
    vi.spyOn(window, 'confirm').mockReturnValue(false);

    // Act
    render(
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <DashboardPage />
      </ThemeProvider>
    );

    // Assert
    await waitFor(() => {
      expect(screen.getByText('Platform Dashboard')).toBeInTheDocument();
    });
    const shutdownButton = screen.getByRole('button', { name: 'Shutdown Platform' });
    fireEvent.click(shutdownButton);
    await waitFor(() => {
      expect(mockShutdown).not.toHaveBeenCalled();
    });
    vi.restoreAllMocks();
  });

  it('Given backend unreachable, When page loads, Then shows error', async () => {
    // Arrange
    mockHealthCheck.mockRejectedValue(new Error('Network error'));
    mockGetPlatformInfo.mockRejectedValue(new Error('Network error'));

    // Act
    render(
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <DashboardPage />
      </ThemeProvider>
    );

    // Assert
    await waitFor(() => {
      expect(screen.getByText(/Failed to connect/)).toBeInTheDocument();
    });
  });
});
