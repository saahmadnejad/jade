import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Box, Typography, Card, CardContent, CardActionArea, Chip, Button,
  Dialog, DialogTitle, DialogContent, DialogActions, TextField, Grid,
  IconButton, Tooltip,
} from '@mui/material';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import StopCircleIcon from '@mui/icons-material/StopCircle';
import RefreshIcon from '@mui/icons-material/Refresh';
import {
  api,
  type ScenarioSummary,
  type ScenarioInstance,
} from 'shared';
import PageHeader from '../components/PageHeader';
import EmptyState from '../components/EmptyState';
import NotificationSnackbar, { useFeedback } from '../components/NotificationSnackbar';

interface ConfigFormState {
  instanceName: string;
  values: Record<string, string>;
}

export default function ScenariosPage() {
  const [scenarios, setScenarios] = useState<ScenarioSummary[]>([]);
  const [instances, setInstances] = useState<ScenarioInstance[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<ScenarioSummary | null>(null);
  const [form, setForm] = useState<ConfigFormState>({ instanceName: '', values: {} });
  const [starting, setStarting] = useState(false);
  const [stopping, setStopping] = useState<string | null>(null);
  const { feedback, notify, close } = useFeedback();

  const fetchAll = useCallback(async () => {
    setLoading(true);
    try {
      const [listResp, instResp] = await Promise.all([
        api.scenarios.list(),
        api.scenarios.listInstances(),
      ]);
      setScenarios(listResp.scenarios);
      setInstances(instResp.instances);
    } catch (e) {
      console.error('Failed to fetch scenarios', e);
      notify('Failed to fetch scenarios', 'error');
    } finally {
      setLoading(false);
    }
  }, [notify]);

  useEffect(() => {
    fetchAll();
  }, [fetchAll]);

  /** Count running instances per scenario id. */
  const runningCount = useMemo(() => {
    const counts: Record<string, number> = {};
    for (const inst of instances) {
      counts[inst.scenarioId] = (counts[inst.scenarioId] ?? 0) + 1;
    }
    return counts;
  }, [instances]);

  const openConfigDialog = (scenario: ScenarioSummary) => {
    // Prefill form with declared defaults.
    const values: Record<string, string> = {};
    for (const [name, param] of Object.entries(scenario.params)) {
      values[name] = String(param.defaultValue);
    }
    let n = 1;
    while (instances.some((i) => i.instance === `${scenario.id}-${n}`)) {
      n++;
    }
    setForm({ instanceName: `${scenario.id}-${n}`, values });
    setSelected(scenario);
  };

  const handleStart = async () => {
    if (!selected) return;
    setStarting(true);
    try {
      const config: Record<string, unknown> = {};
      for (const [name, raw] of Object.entries(form.values)) {
        const param = selected.params[name];
        if (param.type === 'int') {
          config[name] = parseInt(raw, 10);
        } else if (param.type === 'boolean') {
          config[name] = raw === 'true';
        } else {
          config[name] = raw;
        }
      }
      const resp = await api.scenarios.start(selected.id, {
        instanceName: form.instanceName.trim(),
        config,
      });
      notify(resp.message, 'success');
      setSelected(null);
      fetchAll();
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : 'unknown error';
      notify(`Start failed - ${message}`, 'error');
    } finally {
      setStarting(false);
    }
  };

  const handleStop = async (instance: string) => {
    if (!window.confirm(`Stop scenario instance '${instance}' and all its agents?`)) return;
    setStopping(instance);
    try {
      await api.scenarios.stop(instance);
      notify(`Instance '${instance}' stopped`, 'success');
      fetchAll();
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : 'unknown error';
      notify(`Stop failed - ${message}`, 'error');
    } finally {
      setStopping(null);
    }
  };

  return (
    <Box>
      <PageHeader
        title="Scenarios"
        actions={
          <Tooltip title="Refresh">
            <IconButton onClick={fetchAll}>
              <RefreshIcon />
            </IconButton>
          </Tooltip>
        }
      />

      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Launch pre-built multi-agent demos. Each instance runs in its own container
        so you can start several at once and stop them independently.
      </Typography>

      {scenarios.length === 0 && !loading ? (
        <EmptyState message="No scenarios available. Add a jar with Scenario implementations to the platform classpath." />
      ) : (
        <Grid container spacing={2} sx={{ mb: 3 }}>
          {scenarios.map((scenario) => (
            <Grid size={{ xs: 12, sm: 6, md: 4 }} key={scenario.id}>
              <Card variant="outlined" sx={{ height: '100%' }}>
                <CardActionArea onClick={() => openConfigDialog(scenario)} sx={{ height: '100%' }}>
                  <CardContent>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                      <Typography variant="h6">{scenario.title}</Typography>
                      {(runningCount[scenario.id] ?? 0) > 0 && (
                        <Chip
                          size="small"
                          color="success"
                          label={`running ×${runningCount[scenario.id]}`}
                        />
                      )}
                    </Box>
                    <Typography variant="body2" color="text.secondary">{scenario.description}</Typography>
                  </CardContent>
                </CardActionArea>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {instances.length > 0 && (
        <>
          <Typography variant="h6" gutterBottom>Running instances</Typography>
          {instances.map((inst) => (
            <Box
              key={inst.instance}
              sx={{
                display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                p: 1.5, mb: 1, border: 1, borderColor: 'divider', borderRadius: 1,
              }}
            >
              <Box>
                <Typography variant="body1">{inst.instance}</Typography>
                <Typography variant="caption" color="text.secondary">
                  scenario: {inst.scenarioId} · container: {inst.container} · {inst.agents.length} agents
                </Typography>
              </Box>
              <Tooltip title="Kill this scenario instance">
                <span>
                  <IconButton
                    color="error"
                    aria-label="Kill this scenario instance"
                    disabled={stopping === inst.instance}
                    onClick={() => handleStop(inst.instance)}
                  >
                    <StopCircleIcon />
                  </IconButton>
                </span>
              </Tooltip>
            </Box>
          ))}
        </>
      )}

      <Dialog open={selected !== null} onClose={() => setSelected(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Configure '{selected?.title}'</DialogTitle>
        <DialogContent>
          {selected && (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
              <TextField
                label="Instance name"
                value={form.instanceName}
                onChange={(e) => setForm({ ...form, instanceName: e.target.value })}
                helperText="Used as prefix for the agents of this instance"
              />
              {Object.entries(selected.params).map(([name, param]) => (
                <TextField
                  key={name}
                  label={name}
                  type={param.type === 'int' ? 'number' : 'text'}
                  value={form.values[name] ?? ''}
                  onChange={(e) => setForm({ ...form, values: { ...form.values, [name]: e.target.value } })}
                  slotProps={
                    param.type === 'int'
                      ? { htmlInput: { min: param.minValue, max: param.maxValue } }
                      : undefined
                  }
                  helperText={`${param.description ?? ''} (default: ${String(param.defaultValue)})`}
                />
              ))}
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSelected(null)}>Cancel</Button>
          <Button
            onClick={handleStart}
            variant="contained"
            startIcon={<PlayArrowIcon />}
            disabled={starting || !form.instanceName.trim()}
          >
            Start
          </Button>
        </DialogActions>
      </Dialog>

      <NotificationSnackbar feedback={feedback} onClose={close} />
    </Box>
  );
}
