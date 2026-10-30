import { useEffect, useState } from 'react';
import {
  Box, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Paper, IconButton,
  Tooltip, Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Button, Autocomplete,
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
import AddIcon from '@mui/icons-material/Add';
import SendIcon from '@mui/icons-material/Send';
import DesktopWindowsIcon from '@mui/icons-material/DesktopWindows';
import { api, type AgentInfo, type AgentListResponse } from 'shared';
import TopProgressBar from '../components/TopProgressBar';
import PageHeader from '../components/PageHeader';
import EmptyState from '../components/EmptyState';
import MessageComposerDialog from '../components/MessageComposerDialog';
import NotificationSnackbar, { useFeedback } from '../components/NotificationSnackbar';

const COMMON_AGENT_CLASSES = [
  'io.donbee.jade.core.Agent',
  'io.donbee.jade.tools.DummyAgent.DummyAgent',
  'io.donbee.jade.tools.sniffer.Sniffer',
  'io.donbee.jade.tools.introspector.Introspector',
  'io.donbee.jade.tools.testagent.TestAgent',
  'io.donbee.jade.tools.logging.LogManagerAgent',
];

export default function AgentsPage() {
  const [agents, setAgents] = useState<AgentInfo[]>([]);
  const [containers, setContainers] = useState<string[]>(['Main-Container']);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [deployOpen, setDeployOpen] = useState(false);
  const [deployForm, setDeployForm] = useState({ name: '', class: '', args: '', container: 'Main-Container' });
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
  const [loadForm, setLoadForm] = useState({ name: '', container: 'Main-Container', repository: 'default' });
  const [registerRemoteOpen, setRegisterRemoteOpen] = useState(false);
  const [registerRemoteForm, setRegisterRemoteForm] = useState({ aid: '', addresses: '' });
  const [composerTarget, setComposerTarget] = useState<string | null>(null);
  const { feedback, notify, close } = useFeedback();

  const fetchAgents = async () => {
    setLoading(true);
    try {
      const data: AgentListResponse = await api.agents.list({ detail: true });
      setAgents(data.agents);
    } catch (e) {
      console.error('Failed to fetch agents', e);
      notify('Failed to fetch agents', 'error');
    } finally {
      setLoading(false);
    }
  };

  const fetchContainers = async () => {
    try {
      const p = api.containers?.list?.();
      if (p && typeof p.then === 'function') {
        p.then((res: any) => {
          if (res?.containers) {
            const names = res.containers.map((c: any) => c.name);
            if (names.length > 0) setContainers(names);
          }
        }).catch(() => {});
      }
    } catch {
      // Ignore if containers listing fails
    }
  };

  useEffect(() => {
    fetchAgents();
    fetchContainers();
  }, []);

  const handleAction = async (action: () => Promise<unknown>, actionName: string, agentName: string) => {
    setActionLoading(actionName + ':' + agentName);
    try {
      await action();
      notify(`${agentName}: ${actionName} completed`, 'success');
      fetchAgents();
    } catch (e: any) {
      notify(`${agentName}: ${actionName} failed - ${e.message || 'unknown error'}`, 'error');
    } finally {
      setActionLoading(null);
    }
  };

  const handleKill = (agentName: string) => {
    if (!window.confirm(`Kill agent '${agentName}'? This cannot be undone.`)) return;
    handleAction(() => api.agents.kill(agentName), 'kill', agentName);
  };

  const handleDeploy = async () => {
    try {
      await api.agents.deploy({
        name: deployForm.name,
        class: deployForm.class,
        container: deployForm.container || undefined,
        args: deployForm.args ? deployForm.args.split(',').map(s => s.trim()).filter(Boolean) : undefined,
      });
      notify(`Agent '${deployForm.name}' deployed`, 'success');
      setDeployOpen(false);
      setDeployForm({ name: '', class: '', args: '', container: 'Main-Container' });
      fetchAgents();
    } catch (e: any) {
      notify(`Deploy failed - ${e.message || 'unknown error'}`, 'error');
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
    try {
      await api.agents.save(saveTarget, saveForm.repository);
      notify(`Agent '${saveTarget}' saved to '${saveForm.repository}'`, 'success');
      setSaveOpen(false);
      setSaveForm({ repository: 'default' });
      fetchAgents();
    } catch (e: any) {
      notify(`Save failed - ${e.message || 'unknown error'}`, 'error');
    }
  };

  const handleLoad = async () => {
    try {
      await api.agents.load(loadForm.name, loadForm.container, loadForm.repository);
      notify(`Agent '${loadForm.name}' loaded`, 'success');
      setLoadOpen(false);
      setLoadForm({ name: '', container: 'Main-Container', repository: 'default' });
      fetchAgents();
    } catch (e: any) {
      notify(`Load failed - ${e.message || 'unknown error'}`, 'error');
    }
  };

  const handleRegisterRemote = async () => {
    try {
      const addresses = registerRemoteForm.addresses
        ? registerRemoteForm.addresses.split(',').map(s => s.trim()).filter(Boolean)
        : undefined;
      await api.agents.registerRemote({ aid: registerRemoteForm.aid, addresses });
      notify(`Remote agent '${registerRemoteForm.aid}' registered`, 'success');
      setRegisterRemoteOpen(false);
      setRegisterRemoteForm({ aid: '', addresses: '' });
      fetchAgents();
    } catch (e: any) {
      notify(`Register remote failed - ${e.message || 'unknown error'}`, 'error');
    }
  };

  const handleLaunchSwing = async (tool: string) => {
    try {
      await api.tools.start(tool, { container: 'Main-Container' });
      notify(`Launched legacy Swing ${tool} GUI for comparison`, 'success');
    } catch (e: any) {
      notify(`Failed to launch ${tool} - ${e.message || 'unknown error'}`, 'error');
    }
  };

  if (loading) {
    return <TopProgressBar />;
  }

  return (
    <Box>
      <PageHeader
        title="Agents"
        actions={
          <>
            <Tooltip title="Launch legacy Swing DummyAgent on desktop for side-by-side comparison">
              <Button
                variant="outlined"
                color="inherit"
                size="small"
                startIcon={<DesktopWindowsIcon />}
                onClick={() => handleLaunchSwing('dummy')}
              >
                Swing DummyAgent
              </Button>
            </Tooltip>
            <Tooltip title="Launch legacy Swing Introspector on desktop for side-by-side comparison">
              <Button
                variant="outlined"
                color="inherit"
                size="small"
                startIcon={<DesktopWindowsIcon />}
                onClick={() => handleLaunchSwing('introspector')}
              >
                Swing Introspector
              </Button>
            </Tooltip>
            <Tooltip title="Refresh">
              <IconButton onClick={fetchAgents} disabled={loading}>
                <RefreshIcon />
              </IconButton>
            </Tooltip>
            <Button variant="outlined" startIcon={<SendIcon />} onClick={() => setComposerTarget('')}>
              Compose Message
            </Button>
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => setRegisterRemoteOpen(true)}>
              Register Remote
            </Button>
            <Button variant="contained" startIcon={<UploadIcon />} onClick={() => setLoadOpen(true)}>
              Load Agent
            </Button>
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDeployOpen(true)}>
              Deploy Agent
            </Button>
          </>
        }
      />

      {agents.length === 0 ? (
        <EmptyState message="No agents found" />
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
                    <Tooltip title="Send ACL Message">
                      <IconButton
                        color="primary"
                        size="small"
                        onClick={() => setComposerTarget(agent.name)}
                      >
                        <SendIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Kill Agent">
                      <IconButton
                        color="error"
                        size="small"
                        disabled={actionLoading === 'kill:' + agent.name}
                        onClick={() => handleKill(agent.name)}
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
                        color="primary"
                        size="small"
                        onClick={() => { setCloneTarget(agent.name); setCloneOpen(true); }}
                      >
                        <ContentCopyIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Move Agent">
                      <IconButton
                        color="primary"
                        size="small"
                        onClick={() => { setMoveTarget(agent.name); setMoveOpen(true); }}
                      >
                        <MergeIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Change Ownership">
                      <IconButton
                        color="primary"
                        size="small"
                        onClick={() => { setOwnershipTarget(agent.name); setOwnershipOpen(true); }}
                      >
                        <LockIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Save Agent">
                      <IconButton
                        color="primary"
                        size="small"
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

      {/* Deploy Agent Dialog */}
      <Dialog open={deployOpen} onClose={() => setDeployOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Deploy New Agent</DialogTitle>
        <DialogContent>
          <Box sx={{display: 'flex', flexDirection: 'column', gap: 2, pt: 1}}>
            <TextField
              label="Agent Name"
              value={deployForm.name}
              onChange={e => setDeployForm({ ...deployForm, name: e.target.value })}
              fullWidth
              required
            />
            <Autocomplete
              freeSolo
              options={COMMON_AGENT_CLASSES}
              value={deployForm.class}
              onChange={(_, val) => setDeployForm({ ...deployForm, class: val || '' })}
              onInputChange={(_, val) => setDeployForm({ ...deployForm, class: val })}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Class Name"
                  required
                  fullWidth
                  helperText="Choose common tool or enter custom class"
                />
              )}
            />
            <Autocomplete
              freeSolo
              options={containers}
              value={deployForm.container}
              onChange={(_, val) => setDeployForm({ ...deployForm, container: val || 'Main-Container' })}
              onInputChange={(_, val) => setDeployForm({ ...deployForm, container: val })}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Target Container"
                  fullWidth
                  helperText="Container to deploy agent into (defaults to Main-Container)"
                />
              )}
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

      {/* Clone Agent Dialog */}
      <Dialog open={cloneOpen} onClose={() => setCloneOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Clone Agent</DialogTitle>
        <DialogContent>
          <Box sx={{display: 'flex', flexDirection: 'column', gap: 2, pt: 1}}>
            <TextField
              label="New Agent Name"
              value={cloneForm.newName}
              onChange={e => setCloneForm({ ...cloneForm, newName: e.target.value })}
              fullWidth
              helperText="Name for the cloned agent"
            />
            <Autocomplete
              freeSolo
              options={containers}
              value={cloneForm.container}
              onChange={(_, val) => setCloneForm({ ...cloneForm, container: val || '' })}
              onInputChange={(_, val) => setCloneForm({ ...cloneForm, container: val })}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Target Container (optional)"
                  fullWidth
                  helperText="Leave empty to use the same container"
                />
              )}
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

      {/* Move Agent Dialog */}
      <Dialog open={moveOpen} onClose={() => setMoveOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Move Agent</DialogTitle>
        <DialogContent>
          <Box sx={{display: 'flex', flexDirection: 'column', gap: 2, pt: 1}}>
            <Autocomplete
              freeSolo
              options={containers}
              value={moveForm.targetContainer}
              onChange={(_, val) => setMoveForm({ ...moveForm, targetContainer: val || '' })}
              onInputChange={(_, val) => setMoveForm({ ...moveForm, targetContainer: val })}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Target Container"
                  fullWidth
                  helperText="Container to move the agent to"
                />
              )}
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

      {/* Ownership Dialog */}
      <Dialog open={ownershipOpen} onClose={() => setOwnershipOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Change Agent Ownership</DialogTitle>
        <DialogContent>
          <Box sx={{display: 'flex', flexDirection: 'column', gap: 2, pt: 1}}>
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

      {/* Save Agent Dialog */}
      <Dialog open={saveOpen} onClose={() => setSaveOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Save Agent</DialogTitle>
        <DialogContent>
          <Box sx={{display: 'flex', flexDirection: 'column', gap: 2, pt: 1}}>
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

      {/* Load Agent Dialog */}
      <Dialog open={loadOpen} onClose={() => setLoadOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Load Agent</DialogTitle>
        <DialogContent>
          <Box sx={{display: 'flex', flexDirection: 'column', gap: 2, pt: 1}}>
            <TextField
              label="Agent Name"
              value={loadForm.name}
              onChange={e => setLoadForm({ ...loadForm, name: e.target.value })}
              fullWidth
              helperText="Name for the loaded agent"
            />
            <Autocomplete
              freeSolo
              options={containers}
              value={loadForm.container}
              onChange={(_, val) => setLoadForm({ ...loadForm, container: val || 'Main-Container' })}
              onInputChange={(_, val) => setLoadForm({ ...loadForm, container: val })}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Container"
                  fullWidth
                  helperText="Container to load the agent into"
                />
              )}
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

      {/* Register Remote Agent Dialog */}
      <Dialog open={registerRemoteOpen} onClose={() => setRegisterRemoteOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Register Remote Agent</DialogTitle>
        <DialogContent>
          <Box sx={{display: 'flex', flexDirection: 'column', gap: 2, pt: 1}}>
            <TextField
              label="Remote Agent AID"
              value={registerRemoteForm.aid}
              onChange={e => setRegisterRemoteForm({ ...registerRemoteForm, aid: e.target.value })}
              fullWidth
              helperText="The AID (Agent Identifier) of the remote agent, e.g. agent@container"
            />
            <TextField
              label="Addresses (comma-separated)"
              value={registerRemoteForm.addresses}
              onChange={e => setRegisterRemoteForm({ ...registerRemoteForm, addresses: e.target.value })}
              fullWidth
              helperText="Optional: comma-separated addresses of the remote agent"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRegisterRemoteOpen(false)}>Cancel</Button>
          <Button onClick={handleRegisterRemote} variant="contained" disabled={!registerRemoteForm.aid}>
            Register
          </Button>
        </DialogActions>
      </Dialog>

      {/* Message Composer Dialog */}
      <MessageComposerDialog
        open={composerTarget !== null}
        onClose={() => setComposerTarget(null)}
        initialReceiver={composerTarget || undefined}
        onSent={(res) => notify(`Message sent to ${res.receivers.join(', ')}`, 'success')}
      />

      <NotificationSnackbar feedback={feedback} onClose={close} />
    </Box>
  );
}
