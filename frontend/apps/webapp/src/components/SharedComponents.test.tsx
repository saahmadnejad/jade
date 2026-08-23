import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import PageHeader from './PageHeader';
import NotificationSnackbar, { useFeedback } from './NotificationSnackbar';
import EmptyState from './EmptyState';

describe('PageHeader', () => {
  it('Given a title, When rendered, Then shows the title', () => {
    render(<PageHeader title="Agents" />);
    expect(screen.getByText('Agents')).toBeInTheDocument();
  });

  it('Given actions, When rendered, Then shows them next to the title', () => {
    render(<PageHeader title="Agents" actions={<button>Refresh</button>} />);
    expect(screen.getByRole('button', { name: 'Refresh' })).toBeInTheDocument();
  });

  it('Given no actions, When rendered, Then no action area exists', () => {
    render(<PageHeader title="Agents" />);
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });
});

describe('NotificationSnackbar', () => {
  it('Given open feedback with success severity, When rendered, Then shows the message', () => {
    render(
      <NotificationSnackbar
        feedback={{ open: true, message: 'Agent killed', severity: 'success' }}
        onClose={() => {}}
      />
    );
    expect(screen.getByText('Agent killed')).toBeInTheDocument();
  });

  it('Given closed feedback, When rendered, Then nothing is visible', () => {
    render(
      <NotificationSnackbar
        feedback={{ open: false, message: 'hidden', severity: 'error' }}
        onClose={() => {}}
      />
    );
    expect(screen.queryByText('hidden')).not.toBeInTheDocument();
  });

  it('Given an open snackbar, When closed, Then onClose is called', () => {
    const onClose = vi.fn();
    render(
      <NotificationSnackbar
        feedback={{ open: true, message: 'msg', severity: 'success' }}
        onClose={onClose}
      />
    );
    fireEvent.click(screen.getByRole('button', { name: /close/i }));
    expect(onClose).toHaveBeenCalled();
  });
});

describe('useFeedback', () => {
  it('Given the hook, When notify is called, Then feedback opens with message and severity', async () => {
    let captured: ReturnType<typeof useFeedback> | null = null;
    function Probe() {
      captured = useFeedback();
      return (
        <NotificationSnackbar feedback={captured.feedback} onClose={captured.close} />
      );
    }
    render(<Probe />);
    captured!.notify('it worked', 'success');
    await waitFor(() => expect(screen.getByText('it worked')).toBeInTheDocument());
  });

  it('Given an open snackbar, When close is called, Then the snackbar hides', async () => {
    let captured: ReturnType<typeof useFeedback> | null = null;
    function Probe() {
      captured = useFeedback();
      return (
        <NotificationSnackbar feedback={captured.feedback} onClose={captured.close} />
      );
    }
    render(<Probe />);
    captured!.notify('visible');
    captured!.close();
    await waitFor(() => expect(screen.queryByText('visible')).not.toBeInTheDocument());
  });
});

describe('EmptyState', () => {
  it('Given a message, When rendered, Then shows the message inside a paper', () => {
    render(<EmptyState message="No agents found" />);
    expect(screen.getByText('No agents found')).toBeInTheDocument();
  });

  it('Given an icon, When rendered, Then shows the icon above the message', () => {
    render(<EmptyState message="empty" icon={<svg data-testid="icon" />} />);
    expect(screen.getByTestId('icon')).toBeInTheDocument();
  });
});
