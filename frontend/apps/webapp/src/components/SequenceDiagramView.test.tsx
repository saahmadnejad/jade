import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ThemeProvider, createTheme } from '@mui/material';
import SequenceDiagramView from './SequenceDiagramView';
import type { AclMessageEvent } from 'shared';

const sampleMessages: AclMessageEvent[] = [
  {
    id: '1',
    timestamp: '2026-10-31T10:00:00Z',
    sender: 'rma',
    receiver: 'ams',
    performative: 'SUBSCRIBE',
    protocol: 'fipa-request',
    ontology: 'JADE-Introspection',
    content: 'platform-events',
  },
  {
    id: '2',
    timestamp: '2026-10-31T10:00:01Z',
    sender: 'ams',
    receiver: 'rma',
    performative: 'INFORM',
    protocol: 'fipa-request',
    ontology: 'JADE-Introspection',
    content: '((occurred (added-container)))',
  },
  {
    id: '3',
    timestamp: '2026-10-31T10:00:02Z',
    sender: 'worker1',
    receiver: 'ams',
    performative: 'REQUEST',
    protocol: 'fipa-request',
    ontology: 'task-onto',
    content: 'do-task',
  },
];

const theme = createTheme({ palette: { mode: 'dark' } });

const renderWithTheme = (ui: React.ReactElement) => {
  return render(
    <ThemeProvider theme={theme}>
      {ui}
    </ThemeProvider>
  );
};

describe('SequenceDiagramView', () => {
  it('Given messages, When rendered, Then auto-selects top 2 agents and shows sequence arrows', () => {
    const onSelect = vi.fn();
    renderWithTheme(
      <SequenceDiagramView
        messages={sampleMessages}
        onSelectMessage={onSelect}
      />
    );

    // Shows selection bar
    expect(screen.getByText('Select Agents for Sequence Lifelines:')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Top 2 Agents/i })).toBeInTheDocument();

    // Communicators are available as chips
    expect(screen.getByText(/ams \(/)).toBeInTheDocument();
    expect(screen.getByText(/rma \(/)).toBeInTheDocument();

    // Since top 2 are ams and rma, messages between them are shown
    expect(screen.getByText('SUBSCRIBE')).toBeInTheDocument();
    expect(screen.getByText('INFORM')).toBeInTheDocument();
  });

  it('Given a message row in sequence diagram, When clicked, Then triggers onSelectMessage', () => {
    const onSelect = vi.fn();
    renderWithTheme(
      <SequenceDiagramView
        messages={sampleMessages}
        onSelectMessage={onSelect}
      />
    );

    const subscribeMsg = screen.getByText('SUBSCRIBE');
    fireEvent.click(subscribeMsg);

    expect(onSelect).toHaveBeenCalledWith(
      expect.objectContaining({
        id: '1',
        sender: 'rma',
        receiver: 'ams',
        performative: 'SUBSCRIBE',
      })
    );
  });

  it('Given selection cleared, Then prompts to select 2 or more agents', () => {
    const onSelect = vi.fn();
    renderWithTheme(
      <SequenceDiagramView
        messages={sampleMessages}
        onSelectMessage={onSelect}
      />
    );

    const clearBtn = screen.getByRole('button', { name: /Clear/i });
    fireEvent.click(clearBtn);

    expect(screen.getByText(/Please choose 2 or more agents above/i)).toBeInTheDocument();
  });
});
