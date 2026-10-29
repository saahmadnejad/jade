import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { ThemeProvider, createTheme } from '@mui/material';
import AboutDialog from './AboutDialog';

vi.mock('shared/api/factory', () => ({
  api: {
    platform: {
      version: vi.fn().mockResolvedValue({
        version: '1.0.0-react',
        revision: 'rev-42',
        date: '2026-10-30',
      }),
      getInfo: vi.fn().mockResolvedValue({
        platformID: 'jade-main',
        containerName: 'Main-Container',
        isMain: true,
        ams: 'ams@jade-main',
        defaultDF: 'df@jade-main',
      }),
    },
  },
}));

const theme = createTheme({ palette: { mode: 'dark' } });

describe('AboutDialog', () => {
  it('Given open=true, When rendered, Then shows JADE info and version', async () => {
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
      expect(screen.getByText('jade-main')).toBeInTheDocument();
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
