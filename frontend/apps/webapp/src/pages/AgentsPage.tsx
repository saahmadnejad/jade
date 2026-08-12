import { useEffect, useState } from 'react';
import {
  Box, Typography, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Paper, CircularProgress, IconButton,
  Tooltip, Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Button, Snackbar, Alert,
} from '@mui/material';
import DeleteForeverIcon from '@mui/icons-material/DeleteForever';
import PauseIcon from '@mui/icons-material/Pause';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import AcUnitIcon from '@mui/icons-material/AcUnit';
import AcUnitOutlinedIcon from '@mui/icons-material/AcUnitOutlined';
import RefreshIcon from '@mui/icons-material/Refresh';
import { api, type AgentInfo, type AgentListResponse } from 'shared';

export default function AgentsPage() {
  const [agents, setAgents] = useState<AgentInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [deployOpen, setDeployOpen] = useState(false);
  const [deployForm, setDeployForm] = useState({ name: '', class: '', args: '' });
  const [snackbar, setSnackbar] = useState<{open: boolean; message: string; severity: 'success' | 'error'}>({
    open: false, message: '', severity: 'success',
  });

  const fetchAgents = async () => {
    setLoading(true);
    try {
      const data: AgentListResponse = await api.agents.list({ detail: true });
      setAgents(data.agents);
    } catch (e) {
      console.error('Failed to fetch agents', e);
      setSnackbar({ open: true, message: 'Failed to fetch agents', severity: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAgents();
  }, []);

  const handleAction = async (action: () => Promise<unknown>, actionName: string, agentName: string) => {
    setActionLoading(actionName + ':' + agentName);
    try {
      await action();
      setSnackbar({ open: true, message: `${agentName}: ${actionName} completed`, severity: 'success' });
      fetchAgents();
    } catch (e: any) {
      setSnackbar({ open: true, message: `${agentName}: ${actionName} failed - ${e.message || 'unknown error'}`, severity: 'error' });
    } finally {
      setActionLoading(null);
    }
  };

  const handleDeploy = async () => {
    try {
      await api.agents.deploy({
        name: deployForm.name,
        class: deployForm.class,
        args: deployForm.args ? deployForm.args.split(',').map(s => s.trim()).filter(Boolean) : undefined,
      });
      setSnackbar({ open: true, message: `Agent '${deployForm.name}' deployed`, severity: 'success' });
      setDeployOpen(false);
      setDeployForm({ name: '', class: '', args: '' });
      fetchAgents();
    } catch (e: any) {
      setSnackbar({ open: true, message: `Deploy failed - ${e.message || 'unknown error'}`, severity: 'error' });
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="60vh">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h4">Agents</Typography>
        <Box display="flex" gap={1}>
          <Tooltip title="Refresh">
            <IconButton onClick={fetchAgents} disabled={loading}>
              <RefreshIcon />
            </IconButton>
          </Tooltip>
          <Button variant="contained" onClick={() => setDeployOpen(true)}>
            Deploy Agent
          </Button>
        </Box>
      </Box>

      {agents.length === 0 ? (
        <Typography>No agents found</Typography>
      ) : (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>State</TableCell>
                <TableCell>Ownership</TableCell>
                <TableCell>Container</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {agents.map((agent) => (
                <TableRow key={agent.name} hover>
                  <TableCell>{agent.name}</TableCell>
                  <TableCell>{agent.state ?? 'N/A'}</TableCell>
                  <TableCell>{agent.ownership ?? 'N/A'}</TableCell>
                  <TableCell>{agent.container}</TableCell>
                  <TableCell align="right">
                    <Tooltip title="Kill Agent">
                      <IconButton
                        color="error"
                        size="small"
                        disabled={actionLoading === 'kill:' + agent.name}
                        onClick={() => handleAction(
                          () => api.agents.kill(agent.name),
                          'kill',
                          agent.name
                        )}
                      >
                        <DeleteForeverIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Suspend Agent">
                      <IconButton
                        color="primary"
                        size="small"
                        disabled={actionLoading === 'suspend:' + agent.name}
                        onClick={() => handleAction(
                          () => api.agents.suspend(agent.name),
                          'suspend',
                          agent.name
                        )}
                      >
                        <PauseIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Resume Agent">
                      <IconButton
                        color="primary"
                        size="small"
                        disabled={actionLoading === 'resume:' + agent.name}
                        onClick={() => handleAction(
                          () => api.agents.resume(agent.name),
                          'resume',
                          agent.name
                        )}
                      >
                        <PlayArrowIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Freeze Agent">
                      <IconButton
                        color="warning"
                        size="small"
                        disabled={actionLoading === 'freeze:' + agent.name}
                        onClick={() => handleAction(
                          () => api.agents.freeze(agent.name, { container: agent.container, repository: 'default' }),
                          'freeze',
                          agent.name
                        )}
                      >
                        <AcUnitIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Thaw Agent">
                      <IconButton
                        color="info"
                        size="small"
                        disabled={actionLoading === 'thaw:' + agent.name}
                        onClick={() => handleAction(
                          () => api.agents.thaw(agent.name, { container: agent.container, repository: 'default' }),
                          'thaw',
                          agent.name
                        )}
                      >
                        <AcUnitOutlinedIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Dialog open={deployOpen} onClose={() => setDeployOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Deploy New Agent</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField
              label="Agent Name"
              value={deployForm.name}
              onChange={e => setDeployForm({ ...deployForm, name: e.target.value })}
              fullWidth
            />
            <TextField
              label="Class Name"
              value={deployForm.class}
              onChange={e => setDeployForm({ ...deployForm, class: e.target.value })}
              fullWidth
              helperText="e.g., io.donbee.jade.tutorial.HelloWorldAgent"
            />
            <TextField
              label="Args (comma-separated)"
              value={deployForm.args}
              onChange={e => setDeployForm({ ...deployForm, args: e.target.value })}
              fullWidth
              helperText="Optional: comma-separated constructor arguments"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeployOpen(false)}>Cancel</Button>
          <Button onClick={handleDeploy} variant="contained" disabled={!deployForm.name || !deployForm.class}>
            Deploy
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={5000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
      >
        <Alert severity={snackbar.severity} onClose={() => setSnackbar({ ...snackbar, open: false })}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}
