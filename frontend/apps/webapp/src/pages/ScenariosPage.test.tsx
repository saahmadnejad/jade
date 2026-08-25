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

const shopScenario = {
  id: 'online-shop',
  title: 'Online Shop',
  description: 'Customers buy from a storefront.',
  params: {
    initialStock: { type: 'int', defaultValue: 10, minValue: 0, maxValue: 1000, description: 'Starting quantity' },
    customerCount: { type: 'int', defaultValue: 2, minValue: 0, maxValue: 20, description: 'Customers' },
  },
};

describe('ScenariosPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    mockList.mockResolvedValue({ scenarios: [shopScenario] });
    mockListInstances.mockResolvedValue({ instances: [] });
  });

  it('Given scenarios available, When page loads, Then scenario cards are shown', async () => {
    // --- Act ---
    renderWithTheme(<ScenariosPage />);

    // --- Assert ---
    await waitFor(() => expect(screen.getByText('Online Shop')).toBeInTheDocument());
  });

  it('Given a scenario card, When clicked, Then config modal opens prefilled with defaults', async () => {
    // --- Arrange ---
    renderWithTheme(<ScenariosPage />);
    await waitFor(() => expect(screen.getByText('Online Shop')).toBeInTheDocument());

    // --- Act ---
    fireEvent.click(screen.getByText('Online Shop'));

    // --- Assert ---
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByDisplayValue('10')).toBeInTheDocument();   // initialStock default
    expect(within(dialog).getByDisplayValue('2')).toBeInTheDocument();    // customerCount default
    expect(within(dialog).getByDisplayValue('online-shop-1')).toBeInTheDocument(); // auto instance name
  });

  it('Given configured values, When Start clicked, Then start API called with typed config', async () => {
    // --- Arrange ---
    mockStart.mockResolvedValue({
      message: "Scenario 'online-shop' started as instance 'shop-1'",
      instance: 'shop-1', scenarioId: 'online-shop', container: 'scenario-shop-1', agents: [],
    });
    renderWithTheme(<ScenariosPage />);
    await waitFor(() => expect(screen.getByText('Online Shop')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Online Shop'));
    const dialog = await screen.findByRole('dialog');

    // --- Act ---
    const stockField = within(dialog).getByLabelText(/initialStock/i);
    fireEvent.change(stockField, { target: { value: '42' } });
    fireEvent.click(within(dialog).getByRole('button', { name: /start/i }));

    // --- Assert ---
    await waitFor(() => expect(mockStart).toHaveBeenCalledWith('online-shop', {
      instanceName: 'online-shop-1',
      config: { initialStock: 42, customerCount: 2 },
    }));
  });

  it('Given a running instance, When page loads, Then it is listed with a kill button', async () => {
    // --- Arrange ---
    mockListInstances.mockResolvedValue({
      instances: [{
        instance: 'shop-1', scenarioId: 'online-shop',
        container: 'scenario-shop-1', agents: [{ name: 'shop-1-shop' }],
      }],
    });
    renderWithTheme(<ScenariosPage />);

    // --- Act / Assert ---
    await waitFor(() => expect(screen.getByText('shop-1')).toBeInTheDocument());
    expect(screen.getByText(/running ×1/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /kill this scenario instance/i })).toBeInTheDocument();
  });

  it('Given running instance, When kill confirmed, Then stop API called', async () => {
    // --- Arrange ---
    window.confirm = vi.fn().mockReturnValue(true);
    mockStop.mockResolvedValue({ message: "Instance 'shop-1' stopped" });
    mockListInstances.mockResolvedValue({
      instances: [{
        instance: 'shop-1', scenarioId: 'online-shop',
        container: 'scenario-shop-1', agents: [{ name: 'shop-1-shop' }],
      }],
    });
    renderWithTheme(<ScenariosPage />);
    await waitFor(() => expect(screen.getByText('shop-1')).toBeInTheDocument());

    // --- Act ---
    fireEvent.click(screen.getByRole('button', { name: /kill this scenario instance/i }));

    // --- Assert ---
    await waitFor(() => expect(mockStop).toHaveBeenCalledWith('shop-1'));
  });
});

