import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { ThemeProvider, createTheme } from '@mui/material';
import AboutDialog from './AboutDialog';

const mockVersion = vi.fn();
const mockGetInfo = vi.fn();
const mockGetTree = vi.fn();

vi.mock('shared/api/factory', () => ({
  api: {
    platform: {
      version: () => mockVersion(),
      getInfo: () => mockGetInfo(),
      getTree: () => mockGetTree(),
    },
  },
}));

const theme = createTheme({ palette: { mode: 'dark' } });

describe('AboutDialog', () => {
  beforeEach(() => {
    mockVersion.mockReset();
    mockGetInfo.mockReset();
    mockGetTree.mockReset();

    mockVersion.mockResolvedValue({
      version: '1.0.0-react',
      revision: 'rev-42',
      date: '2026-10-30',
      jvm: '21.0.2 (Eclipse Temurin)',
      os: 'Linux amd64',
    });
    mockGetInfo.mockResolvedValue({
      platformID: 'jade-main',
      containerName: 'Main-Container',
      isMain: true,
      ams: 'ams@jade-main',
      defaultDF: 'df@jade-main',
    });
    mockGetTree.mockResolvedValue({
      platformID: 'jade-main',
      containers: [
        {
          name: 'Main-Container',
          isMain: true,
          agents: [{ name: 'ams' }, { name: 'df' }, { name: 'rma' }],
        },
      ],
      remotePlatforms: [],
    });
  });

  it('Given open=true, When rendered, Then shows JADE info, JVM platform, counts, and credits', async () => {
    render(
      <ThemeProvider theme={theme}>
        <AboutDialog open={true} onClose={() => {}} />
      </ThemeProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('About JADE Platform')).toBeInTheDocument();
      expect(screen.getByText('1.0.0-react')).toBeInTheDocument();
      expect(screen.getByText('rev-42')).toBeInTheDocument();
      expect(screen.getByText('2026-10-30')).toBeInTheDocument();
      expect(screen.getByText(/21.0.2/)).toBeInTheDocument();
      expect(screen.getByText(/Linux amd64/)).toBeInTheDocument();
      expect(screen.getByText('jade-main')).toBeInTheDocument();
      expect(screen.getByText(/Telecom Italia Lab/)).toBeInTheDocument();
    });
  });

  it('Given backend API fails, When rendered, Then handles errors gracefully without crashing', async () => {
    mockVersion.mockRejectedValue(new Error('Version network error'));
    mockGetInfo.mockRejectedValue(new Error('Info network error'));
    mockGetTree.mockRejectedValue(new Error('Tree network error'));

    render(
      <ThemeProvider theme={theme}>
        <AboutDialog open={true} onClose={() => {}} />
      </ThemeProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('About JADE Platform')).toBeInTheDocument();
      expect(screen.getByText('Unknown')).toBeInTheDocument();
    });
  });

  it('Given open=false, When rendered, Then modal is not displayed', () => {
    render(
      <ThemeProvider theme={theme}>
        <AboutDialog open={false} onClose={() => {}} />
      </ThemeProvider>
    );

    expect(screen.queryByText('About JADE Platform')).not.toBeInTheDocument();
  });
});
