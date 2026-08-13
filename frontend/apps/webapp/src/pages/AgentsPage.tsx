import { useEffect, useState } from 'react';
import {
  Box, Typography, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Paper, CircularProgress, IconButton,
  Tooltip, Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Button, Snackbar, Alert,
} from '@mui/material';
import DeleteForeverIcon from '@mui/icons-material/Delete';
import PauseIcon from '@mui/icons-material/Pause';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import AcUnitIcon from '@mui/icons-material/AcUnit';
import AcUnitOutlinedIcon from '@mui/icons-material/AcUnitOutlined';
import RefreshIcon from '@mui/icons-material/Refresh';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import MergeIcon from '@mui/icons-material/Merge';
import LockIcon from '@mui/icons-material/Lock';
import SaveIcon from '@mui/icons-material/Save';
import UploadIcon from '@mui/icons-material/Upload';
import { api, type AgentInfo, type AgentListResponse } from 'shared';

export default function AgentsPage() {
  const [agents, setAgents] = useState<AgentInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [deployOpen, setDeployOpen] = useState(false);
  const [deployForm, setDeployForm] = useState({ name: '', class: '', args: '' });
  const [cloneOpen, setCloneOpen] = useState(false);
  const [cloneForm, setCloneForm] = useState({ newName: '', container: '' });
  const [cloneTarget, setCloneTarget] = useState<string | null>(null);
  const [moveOpen, setMoveOpen] = useState(false);
  const [moveForm, setMoveForm] = useState({ targetContainer: '' });
  const [moveTarget, setMoveTarget] = useState<string | null>(null);
  const [ownershipOpen, setOwnershipOpen] = useState(false);
  const [ownershipForm, setOwnershipForm] = useState({ ownership: '' });
  const [ownershipTarget, setOwnershipTarget] = useState<string | null>(null);
  const [saveOpen, setSaveOpen] = useState(false);
  const [saveForm, setSaveForm] = useState({ repository: 'default' });
  const [saveTarget, setSaveTarget] = useState<string | null>(null);
  const [loadOpen, setLoadOpen] = useState(false);
  const [loadForm, setLoadForm] = useState({ name: '', container: '', repository: 'default' });
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

  const handleClone = async () => {
    if (!cloneTarget) return;
    await handleAction(
      () => api.agents.clone({
        name: cloneTarget.split('@')[0],
        newName: cloneForm.newName,
        container: cloneForm.container || undefined,
      }),
      'clone',
      cloneTarget
    );
    setCloneOpen(false);
    setCloneForm({ newName: '', container: '' });
  };

  const handleMove = async () => {
    if (!moveTarget) return;
    await handleAction(
      () => api.agents.move(moveTarget, { container: moveForm.targetContainer }),
      'move',
      moveTarget
    );
    setMoveOpen(false);
    setMoveForm({ targetContainer: '' });
  };

  const handleOwnership = async () => {
    if (!ownershipTarget) return;
    await handleAction(
      () => api.agents.changeOwnership(ownershipTarget, { ownership: ownershipForm.ownership }),
      'ownership',
      ownershipTarget
    );
    setOwnershipOpen(false);
    setOwnershipForm({ ownership: '' });
  };

  const handleSave = async () => {
    if (!saveTarget) return;
    const agentName = saveTarget;
    await handleAction(
      () => api.agents.save(agentName, saveForm.repository),
      'save',
      agentName
    );
    setSaveOpen(false);
    setSaveForm({ repository: '' });
    setSaveTarget(null);
  };

  const handleLoad = async () => {
    try {
      await api.agents.load(loadForm.name, loadForm.container, loadForm.repository);
      setSnackbar({ open: true, message: `Agent '${loadForm.name}' loaded`, severity: 'success' });
      setLoadOpen(false);
      setLoadForm({ name: '', container: '', repository: '' });
      fetchAgents();
    } catch (e: any) {
      setSnackbar({ open: true, message: `Load failed - ${e.message || 'unknown error'}`, severity: 'error' });
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
          <Button variant="contained" onClick={() => setLoadOpen(true)}>
            Load Agent
          </Button>
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
                    <Tooltip title="Clone Agent">
                      <IconButton
                        color="secondary"
                        size="small"
                        disabled={actionLoading === 'clone:' + agent.name}
                        onClick={() => { setCloneTarget(agent.name); setCloneOpen(true); }}
                      >
                        <ContentCopyIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Move Agent">
                      <IconButton
                        color="secondary"
                        size="small"
                        disabled={actionLoading === 'move:' + agent.name}
                        onClick={() => { setMoveTarget(agent.name); setMoveOpen(true); }}
                      >
                        <MergeIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                     <Tooltip title="Change Ownership">
                       <IconButton
                         color="secondary"
                         size="small"
                         disabled={actionLoading === 'ownership:' + agent.name}
                         onClick={() => { setOwnershipTarget(agent.name); setOwnershipOpen(true); }}
                       >
                         <LockIcon fontSize="small" />
                       </IconButton>
                     </Tooltip>
                     <Tooltip title="Save Agent">
                       <IconButton
                         color="secondary"
                         size="small"
                         disabled={actionLoading === 'save:' + agent.name}
                         onClick={() => { setSaveTarget(agent.name); setSaveOpen(true); }}
                       >
                         <SaveIcon fontSize="small" />
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

      <Dialog open={cloneOpen} onClose={() => setCloneOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Clone Agent</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField
              label="New Agent Name"
              value={cloneForm.newName}
              onChange={e => setCloneForm({ ...cloneForm, newName: e.target.value })}
              fullWidth
              helperText="Name for the cloned agent"
            />
            <TextField
              label="Target Container (optional)"
              value={cloneForm.container}
              onChange={e => setCloneForm({ ...cloneForm, container: e.target.value })}
              fullWidth
              helperText="Leave empty to use the same container"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCloneOpen(false)}>Cancel</Button>
          <Button onClick={handleClone} variant="contained" disabled={!cloneForm.newName}>
            Clone
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={moveOpen} onClose={() => setMoveOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Move Agent</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField
              label="Target Container"
              value={moveForm.targetContainer}
              onChange={e => setMoveForm({ ...moveForm, targetContainer: e.target.value })}
              fullWidth
              helperText="Container to move the agent to"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setMoveOpen(false)}>Cancel</Button>
          <Button onClick={handleMove} variant="contained" disabled={!moveForm.targetContainer}>
            Move
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={ownershipOpen} onClose={() => setOwnershipOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Change Agent Ownership</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField
              label="New Ownership"
              value={ownershipForm.ownership}
              onChange={e => setOwnershipForm({ ...ownershipForm, ownership: e.target.value })}
              fullWidth
              helperText="New ownership identifier for the agent"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOwnershipOpen(false)}>Cancel</Button>
          <Button onClick={handleOwnership} variant="contained" disabled={!ownershipForm.ownership}>
            Change
          </Button>
        </DialogActions>
       </Dialog>

      <Dialog open={saveOpen} onClose={() => setSaveOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Save Agent</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField
              label="Repository"
              value={saveForm.repository}
              onChange={e => setSaveForm({ ...saveForm, repository: e.target.value })}
              fullWidth
              helperText="Repository URL to save the agent state to"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSaveOpen(false)}>Cancel</Button>
          <Button onClick={handleSave} variant="contained" disabled={!saveForm.repository}>
            Save
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={loadOpen} onClose={() => setLoadOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Load Agent</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField
              label="Agent Name"
              value={loadForm.name}
              onChange={e => setLoadForm({ ...loadForm, name: e.target.value })}
              fullWidth
              helperText="Name for the loaded agent"
            />
            <TextField
              label="Container"
              value={loadForm.container}
              onChange={e => setLoadForm({ ...loadForm, container: e.target.value })}
              fullWidth
              helperText="Container to load the agent into"
            />
            <TextField
              label="Repository"
              value={loadForm.repository}
              onChange={e => setLoadForm({ ...loadForm, repository: e.target.value })}
              fullWidth
              helperText="Repository URL to load the agent state from"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setLoadOpen(false)}>Cancel</Button>
          <Button onClick={handleLoad} variant="contained" disabled={!loadForm.name || !loadForm.container || !loadForm.repository}>
            Load
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
