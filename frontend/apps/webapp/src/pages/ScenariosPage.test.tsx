import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent, within } from '@testing-library/react';
import { ThemeProvider, createTheme, CssBaseline } from '@mui/material';
import ScenariosPage from './ScenariosPage';

const { mockList, mockListInstances, mockStart, mockStop } = vi.hoisted(() => ({
  mockList: vi.fn(),
  mockListInstances: vi.fn(),
  mockStart: vi.fn(),
  mockStop: vi.fn(),
}));

vi.mock('shared/api/factory', () => ({
  api: {
    platform: { health: vi.fn(), getInfo: vi.fn(), shutdown: vi.fn(), version: vi.fn() },
    containers: { list: vi.fn(), get: vi.fn(), kill: vi.fn(), save: vi.fn(), load: vi.fn(), listMTPs: vi.fn(), installMTP: vi.fn(), uninstallMTP: vi.fn() },
    agents: {
      list: vi.fn(), get: vi.fn(), deploy: vi.fn(), kill: vi.fn(), suspend: vi.fn(),
      resume: vi.fn(), freeze: vi.fn(), thaw: vi.fn(), clone: vi.fn(), move: vi.fn(),
      save: vi.fn(), load: vi.fn(), changeOwnership: vi.fn(), registerRemote: vi.fn(),
    },
    tools: { start: vi.fn() },
    platforms: { list: vi.fn(), add: vi.fn(), fetch: vi.fn(), remove: vi.fn(), getDescription: vi.fn(), refreshDescription: vi.fn(), listAgents: vi.fn() },
    df: {
      listRegistrations: vi.fn(), register: vi.fn(), deregister: vi.fn(),
      getRegistration: vi.fn(), modifyRegistration: vi.fn(), search: vi.fn(),
      getDescription: vi.fn(), getStatus: vi.fn(), refresh: vi.fn(),
      getParents: vi.fn(), getChildren: vi.fn(), federate: vi.fn(),
      deregisterParent: vi.fn(), deregisterChild: vi.fn(),
    },
    messages: { recent: vi.fn() },
    scenarios: {
      list: mockList,
      listInstances: mockListInstances,
      start: mockStart,
      stop: mockStop,
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

const devTeamScenario = {
  id: 'dev-team',
  title: 'Software Development Team',
  description: 'Five LLM-powered agents build a small project from a brief.',
  params: {
    maxRounds: { type: 'int', defaultValue: 5, minValue: 1, maxValue: 10, description: 'Review rounds cap' },
    maxTotalCalls: { type: 'int', defaultValue: 40, minValue: 1, maxValue: 200, description: 'LLM call cap' },
  },
};

describe('ScenariosPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    mockList.mockResolvedValue({ scenarios: [devTeamScenario] });
    mockListInstances.mockResolvedValue({ instances: [] });
  });

  it('Given scenarios available, When page loads, Then scenario cards are shown', async () => {
    // --- Act ---
    renderWithTheme(<ScenariosPage />);

    // --- Assert ---
    await waitFor(() => expect(screen.getByText('Software Development Team')).toBeInTheDocument());
  });

  it('Given a scenario card, When clicked, Then config modal opens prefilled with defaults', async () => {
    // --- Arrange ---
    renderWithTheme(<ScenariosPage />);
    await waitFor(() => expect(screen.getByText('Software Development Team')).toBeInTheDocument());

    // --- Act ---
    fireEvent.click(screen.getByText('Software Development Team'));

    // --- Assert ---
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByDisplayValue('5')).toBeInTheDocument();    // maxRounds default
    expect(within(dialog).getByDisplayValue('40')).toBeInTheDocument();  // maxTotalCalls default
    expect(within(dialog).getByDisplayValue('dev-team-1')).toBeInTheDocument(); // auto instance name
  });

  it('Given configured values, When Start clicked, Then start API called with typed config', async () => {
    // --- Arrange ---
    mockStart.mockResolvedValue({
      message: "Scenario 'dev-team' started as instance 'team-1'",
      instance: 'team-1', scenarioId: 'dev-team', container: 'scenario-team-1', agents: [],
    });
    renderWithTheme(<ScenariosPage />);
    await waitFor(() => expect(screen.getByText('Software Development Team')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Software Development Team'));
    const dialog = await screen.findByRole('dialog');

    // --- Act ---
    const roundsField = within(dialog).getByLabelText(/maxRounds/i);
    fireEvent.change(roundsField, { target: { value: '3' } });
    fireEvent.click(within(dialog).getByRole('button', { name: /start/i }));

    // --- Assert ---
    await waitFor(() => expect(mockStart).toHaveBeenCalledWith('dev-team', {
      instanceName: 'dev-team-1',
      config: { maxRounds: 3, maxTotalCalls: 40 },
    }));
  });

  it('Given a running instance, When page loads, Then it is listed with a kill button', async () => {
    // --- Arrange ---
    mockListInstances.mockResolvedValue({
      instances: [{
        instance: 'team-1', scenarioId: 'dev-team',
        container: 'scenario-team-1', agents: [{ name: 'team-1-manager' }],
      }],
    });
    renderWithTheme(<ScenariosPage />);

    // --- Act / Assert ---
    await waitFor(() => expect(screen.getByText('team-1')).toBeInTheDocument());
    expect(screen.getByText(/running ×1/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /kill this scenario instance/i })).toBeInTheDocument();
  });

  it('Given running instance, When kill confirmed, Then stop API called', async () => {
    // --- Arrange ---
    window.confirm = vi.fn().mockReturnValue(true);
    mockStop.mockResolvedValue({ message: "Instance 'team-1' stopped" });
    mockListInstances.mockResolvedValue({
      instances: [{
        instance: 'team-1', scenarioId: 'dev-team',
        container: 'scenario-team-1', agents: [{ name: 'team-1-manager' }],
      }],
    });
    renderWithTheme(<ScenariosPage />);
    await waitFor(() => expect(screen.getByText('team-1')).toBeInTheDocument());

    // --- Act ---
    fireEvent.click(screen.getByRole('button', { name: /kill this scenario instance/i }));

    // --- Assert ---
    await waitFor(() => expect(mockStop).toHaveBeenCalledWith('team-1'));
  });
});
