import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { ThemeProvider, createTheme } from '@mui/material';
import MessageComposerDialog from './MessageComposerDialog';

const mockSend = vi.fn();
const mockListAgents = vi.fn();

vi.mock('shared/api/factory', () => ({
  api: {
    agents: {
      list: () => mockListAgents(),
    },
    messages: {
      send: (req: any) => mockSend(req),
    },
  },
}));

const theme = createTheme({ palette: { mode: 'dark' } });

describe('MessageComposerDialog', () => {
  beforeEach(() => {
    mockSend.mockReset();
    mockListAgents.mockReset();
    mockListAgents.mockResolvedValue({
      agents: [
        { name: 'receiver-agent@jade-main' },
        { name: 'sender-agent@jade-main' },
      ],
    });
  });

  it('Given open=true, When rendered, Then shows composer form with performative selector', async () => {
    render(
      <ThemeProvider theme={theme}>
        <MessageComposerDialog open={true} onClose={() => {}} />
      </ThemeProvider>
    );

    expect(screen.getByText('Compose ACL Message')).toBeInTheDocument();
    expect(screen.getByText('FIPA-2000 Compliant')).toBeInTheDocument();
    expect(screen.getByText('Send Message')).toBeInTheDocument();
  });

  it('Given recipient and content, When Send clicked, Then calls api.messages.send', async () => {
    mockSend.mockResolvedValue({
      message: 'Message sent',
      performative: 'INFORM',
      sender: 'ui-user@jade-main',
      receivers: ['receiver-agent@jade-main'],
    });

    const onSent = vi.fn();

    render(
      <ThemeProvider theme={theme}>
        <MessageComposerDialog
          open={true}
          onClose={() => {}}
          initialReceiver="receiver-agent@jade-main"
          initialContent="Hello JADE agent"
          onSent={onSent}
        />
      </ThemeProvider>
    );

    const sendBtn = screen.getByRole('button', { name: 'Send Message' });
    fireEvent.click(sendBtn);

    await waitFor(() => {
      expect(mockSend).toHaveBeenCalledWith(
        expect.objectContaining({
          performative: 'INFORM',
          receivers: ['receiver-agent@jade-main'],
          content: 'Hello JADE agent',
        })
      );
      expect(onSent).toHaveBeenCalled();
    });
  });
});
