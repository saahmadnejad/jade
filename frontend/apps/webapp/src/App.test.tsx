import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { ThemeProvider, createTheme, CssBaseline } from '@mui/material';
import DashboardPage from './pages/DashboardPage';

const { mockHealthCheck, mockGetPlatformInfo } = vi.hoisted(() => ({
  mockHealthCheck: vi.fn(),
  mockGetPlatformInfo: vi.fn(),
}));

vi.mock('shared/api/factory', () => ({
  api: {
    platform: {
      health: mockHealthCheck,
      getInfo: mockGetPlatformInfo,
      shutdown: vi.fn(),
      version: vi.fn(),
    },
  },
}));

const theme = createTheme({ palette: { mode: 'dark' } });

describe('DashboardPage', () => {
  beforeEach(() => {
    mockHealthCheck.mockReset();
    mockGetPlatformInfo.mockReset();
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
