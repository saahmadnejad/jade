import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import App from '../src/App';

const { mockHealthCheck } = vi.hoisted(() => ({
  mockHealthCheck: vi.fn(),
}));

vi.mock('shared/api/factory', () => ({
  api: {
    platform: {
      health: mockHealthCheck,
    },
  },
  createApiClient: () => ({
    platform: {
      health: mockHealthCheck,
    },
  }),
}));

describe('App', () => {
  beforeEach(() => {
    mockHealthCheck.mockReset();
  });

  it('Given backend reachable, When App mounts, Then shows connected status with health data', async () => {
    // Arrange
    mockHealthCheck.mockResolvedValue({ status: 'ok' });

    // Act
    render(<App />);

    // Assert
    expect(screen.getByText('Jade UI')).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getByText(/connected/, { exact: false })).toBeInTheDocument();
    });
  });

  it('Given backend unreachable, When App mounts, Then shows "backend unreachable"', async () => {
    // Arrange
    mockHealthCheck.mockRejectedValue(new Error('Network error'));

    // Act
    render(<App />);

    // Assert
    await waitFor(() => {
      expect(screen.getByText(/backend unreachable/, { exact: false })).toBeInTheDocument();
    });
  });
});
