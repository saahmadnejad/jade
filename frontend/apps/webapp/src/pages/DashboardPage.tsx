import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Chip,
  Alert,
  Button,
  Typography,
  Divider,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Autocomplete,
} from '@mui/material';
import { api, type HealthStatus, type PlatformInfo, type AgentInfo } from 'shared';
import PageHeader from '../components/PageHeader';
import TopProgressBar from '../components/TopProgressBar';
import PlatformTreeView from '../components/PlatformTreeView';
import MessageComposerDialog from '../components/MessageComposerDialog';
import NotificationSnackbar, { useFeedback } from '../components/NotificationSnackbar';
import { getErrorMessage } from '../utils/errors';

export default function DashboardPage() {
  const navigate = useNavigate();
  const [health, setHealth] = useState<HealthStatus | null>(null);
  const [platform, setPlatform] = useState<PlatformInfo | null>(null);
  const [containers, setContainers] = useState<string[]>(['Main-Container']);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [composerTarget, setComposerTarget] = useState<string | null>(null);
  const { feedback, notify, close } = useFeedback();

  // Action Dialog States for Tree interactions
  const [moveOpen, setMoveOpen] = useState(false);
  const [moveTarget, setMoveTarget] = useState<string | null>(null);
  const [moveContainer, setMoveContainer] = useState('');

  const [cloneOpen, setCloneOpen] = useState(false);
  const [cloneTarget, setCloneTarget] = useState<string | null>(null);
  const [cloneName, setCloneName] = useState('');
  const [cloneContainer, setCloneContainer] = useState('');

  const [freezeOpen, setFreezeOpen] = useState(false);
  const [freezeTarget, setFreezeTarget] = useState<string | null>(null);
  const [freezeContainer, setFreezeContainer] = useState('Main-Container');
  const [freezeRepo, setFreezeRepo] = useState('default');

  const [thawOpen, setThawOpen] = useState(false);
  const [thawTarget, setThawTarget] = useState<string | null>(null);
  const [thawContainer, setThawContainer] = useState('Main-Container');
  const [thawRepo, setThawRepo] = useState('default');

  const [inspectOpen, setInspectOpen] = useState(false);
  const [inspectedAgent, setInspectedAgent] = useState<AgentInfo | null>(null);

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [healthData, platformData] = await Promise.all([
        api.platform.health(),
        api.platform.getInfo(),
      ]);
      setHealth(healthData);
      setPlatform(platformData);

      try {
        const containersData = await api.containers?.list?.();
        if (containersData?.containers) {
          const names = containersData.containers.map((c) => c.name);
          if (names.length > 0) setContainers(names);
        }
      } catch {
        // Ignore if containers listing fails
      }
    } catch (e: unknown) {
      setError(`Failed to connect to backend: ${getErrorMessage(e)}`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleShutdown = async () => {
    if (!window.confirm('Shutdown the entire JADE platform? This cannot be undone.')) return;
    try {
      await api.platform.shutdown();
      notify('Platform shutdown initiated', 'success');
    } catch (e: unknown) {
      notify(`Shutdown failed - ${getErrorMessage(e)}`, 'error');
    }
  };

  const handleKillAgent = async (name: string) => {
    if (!window.confirm(`Kill agent '${name}'? This cannot be undone.`)) return;
    try {
      await api.agents.kill(name);
      notify(`Agent '${name}' killed`, 'success');
    } catch (e: unknown) {
      notify(`Failed to kill agent '${name}': ${getErrorMessage(e)}`, 'error');
    }
  };

  const handleSuspendAgent = async (name: string) => {
    try {
      await api.agents.suspend(name);
      notify(`Agent '${name}' suspended`, 'success');
    } catch (e: unknown) {
      notify(`Failed to suspend agent '${name}': ${getErrorMessage(e)}`, 'error');
    }
  };

  const handleResumeAgent = async (name: string) => {
    try {
      await api.agents.resume(name);
      notify(`Agent '${name}' resumed`, 'success');
    } catch (e: unknown) {
      notify(`Failed to resume agent '${name}': ${getErrorMessage(e)}`, 'error');
    }
  };

  const handleMoveSubmit = async () => {
    if (!moveTarget || !moveContainer) return;
    try {
      await api.agents.move(moveTarget, { container: moveContainer });
      notify(`Agent '${moveTarget}' moved to '${moveContainer}'`, 'success');
      setMoveOpen(false);
      setMoveContainer('');
    } catch (e: unknown) {
      notify(`Move failed - ${getErrorMessage(e)}`, 'error');
    }
  };

  const handleCloneSubmit = async () => {
    if (!cloneTarget || !cloneName) return;
    try {
      await api.agents.clone({
        name: cloneTarget.split('@')[0],
        newName: cloneName,
        container: cloneContainer || undefined,
      });
      notify(`Agent '${cloneTarget}' cloned as '${cloneName}'`, 'success');
      setCloneOpen(false);
      setCloneName('');
      setCloneContainer('');
    } catch (e: unknown) {
      notify(`Clone failed - ${getErrorMessage(e)}`, 'error');
    }
  };

  const handleFreezeSubmit = async () => {
    if (!freezeTarget) return;
    try {
      await api.agents.freeze(freezeTarget, { container: freezeContainer, repository: freezeRepo });
      notify(`Agent '${freezeTarget}' frozen`, 'success');
      setFreezeOpen(false);
    } catch (e: unknown) {
      notify(`Freeze failed - ${getErrorMessage(e)}`, 'error');
    }
  };

  const handleThawSubmit = async () => {
    if (!thawTarget) return;
    try {
      await api.agents.thaw(thawTarget, { container: thawContainer, repository: thawRepo });
      notify(`Agent '${thawTarget}' thawed`, 'success');
      setThawOpen(false);
    } catch (e: unknown) {
      notify(`Thaw failed - ${getErrorMessage(e)}`, 'error');
    }
  };

  const handleKillContainer = async (name: string) => {
    if (!window.confirm(`Kill container '${name}'? This cannot be undone.`)) return;
    try {
      await api.containers.kill(name);
      notify(`Container '${name}' killed`, 'success');
    } catch (e: unknown) {
      notify(`Failed to kill container '${name}': ${getErrorMessage(e)}`, 'error');
    }
  };

  const handleRemoveRemotePlatform = async (name: string) => {
    if (!window.confirm(`Remove remote platform '${name}'?`)) return;
    try {
      await api.platforms.remove(name);
      notify(`Remote platform '${name}' removed`, 'success');
    } catch (e: unknown) {
      notify(`Failed to remove platform '${name}': ${getErrorMessage(e)}`, 'error');
    }
  };

  if (loading) {
    return <TopProgressBar />;
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
      <PageHeader
        title="Platform Dashboard"
        actions={
          <>
            <Chip
              label={health?.status === 'ok' ? 'Healthy' : 'Unhealthy'}
              color={health?.status === 'ok' ? 'success' : 'error'}
            />
            <Button variant="outlined" color="error" onClick={handleShutdown}>
              Shutdown Platform
            </Button>
          </>
        }
      />

      {error && (
        <Alert severity="error">{error}</Alert>
      )}

      {platform && !error && (
        <Card variant="outlined">
          <CardContent>
            <Typography variant="h6" gutterBottom>Platform Overview</Typography>
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: '1fr 1fr 1fr 1fr' }, gap: 2 }}>
              <Box>
                <Typography variant="caption" color="text.secondary">Platform ID</Typography>
                <Typography variant="body1" sx={{ fontWeight: 'bold' }}>{platform.platformID}</Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">Main Container</Typography>
                <Typography variant="body1">{platform.containerName}</Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">AMS Address</Typography>
                <Typography variant="body2" sx={{ wordBreak: 'break-all' }}>{platform.ams}</Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">Default DF</Typography>
                <Typography variant="body2" sx={{ wordBreak: 'break-all' }}>{platform.defaultDF}</Typography>
              </Box>
            </Box>
          </CardContent>
        </Card>
      )}

      <Divider />

      {/* Platform Tree (AgentTree) with all action callbacks wired */}
      <PlatformTreeView
        onSendMessage={(name) => setComposerTarget(name)}
        onKillAgent={handleKillAgent}
        onSuspendAgent={handleSuspendAgent}
        onResumeAgent={handleResumeAgent}
        onMoveAgent={(name) => {
          setMoveTarget(name);
          setMoveContainer('');
          setMoveOpen(true);
        }}
        onCloneAgent={(name) => {
          setCloneTarget(name);
          setCloneName('');
          setCloneContainer('');
          setCloneOpen(true);
        }}
        onFreezeAgent={(name) => {
          setFreezeTarget(name);
          setFreezeContainer('Main-Container');
          setFreezeRepo('default');
          setFreezeOpen(true);
        }}
        onThawAgent={(name) => {
          setThawTarget(name);
          setThawContainer('Main-Container');
          setThawRepo('default');
          setThawOpen(true);
        }}
        onInspectAgent={(agent) => {
          setInspectedAgent(agent);
          setInspectOpen(true);
        }}
        onSniffAgent={() => navigate('/messages')}
        onSaveContainer={() => navigate('/containers')}
        onLoadContainer={() => navigate('/containers')}
        onManageMTPs={() => navigate('/containers')}
        onKillContainer={handleKillContainer}
        onRemoveRemotePlatform={handleRemoveRemotePlatform}
      />

      {/* Message Composer modal */}
      <MessageComposerDialog
        open={Boolean(composerTarget)}
        onClose={() => setComposerTarget(null)}
        initialReceiver={composerTarget || undefined}
        onSent={(res) => notify(`Message sent to ${res.receivers.join(', ')}`, 'success')}
      />

      {/* Move Agent Dialog */}
      <Dialog open={moveOpen} onClose={() => setMoveOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Move Agent</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
            {moveTarget && (
              <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                <Typography variant="body2" color="text.secondary">Target Agent:</Typography>
                <Chip size="small" label={moveTarget} />
              </Box>
            )}
            <Autocomplete
              freeSolo
              options={containers}
              value={moveContainer}
              onChange={(_, val) => setMoveContainer(val || '')}
              onInputChange={(_, val) => setMoveContainer(val)}
              renderInput={(params) => (
                <TextField {...params} label="Destination Container" fullWidth required />
              )}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setMoveOpen(false)}>Cancel</Button>
          <Button onClick={handleMoveSubmit} variant="contained" disabled={!moveContainer}>
            Move
          </Button>
        </DialogActions>
      </Dialog>

      {/* Clone Agent Dialog */}
      <Dialog open={cloneOpen} onClose={() => setCloneOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Clone Agent</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
            {cloneTarget && (
              <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                <Typography variant="body2" color="text.secondary">Original Agent:</Typography>
                <Chip size="small" label={cloneTarget} />
              </Box>
            )}
            <TextField
              label="New Agent Name"
              value={cloneName}
              onChange={(e) => setCloneName(e.target.value)}
              fullWidth
              required
            />
            <Autocomplete
              freeSolo
              options={containers}
              value={cloneContainer}
              onChange={(_, val) => setCloneContainer(val || '')}
              onInputChange={(_, val) => setCloneContainer(val)}
              renderInput={(params) => (
                <TextField {...params} label="Destination Container (Optional)" fullWidth />
              )}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCloneOpen(false)}>Cancel</Button>
          <Button onClick={handleCloneSubmit} variant="contained" disabled={!cloneName}>
            Clone
          </Button>
        </DialogActions>
      </Dialog>

      {/* Freeze Agent Dialog */}
      <Dialog open={freezeOpen} onClose={() => setFreezeOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Freeze Agent</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
            {freezeTarget && (
              <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                <Typography variant="body2" color="text.secondary">Agent:</Typography>
                <Chip size="small" label={freezeTarget} />
              </Box>
            )}
            <Autocomplete
              freeSolo
              options={containers}
              value={freezeContainer}
              onChange={(_, val) => setFreezeContainer(val || 'Main-Container')}
              onInputChange={(_, val) => setFreezeContainer(val)}
              renderInput={(params) => (
                <TextField {...params} label="Buffer Container" fullWidth />
              )}
            />
            <TextField
              label="Repository"
              value={freezeRepo}
              onChange={(e) => setFreezeRepo(e.target.value)}
              fullWidth
              required
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setFreezeOpen(false)}>Cancel</Button>
          <Button onClick={handleFreezeSubmit} variant="contained" color="warning" disabled={!freezeRepo}>
            Freeze
          </Button>
        </DialogActions>
      </Dialog>

      {/* Thaw Agent Dialog */}
      <Dialog open={thawOpen} onClose={() => setThawOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Thaw Agent</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
            {thawTarget && (
              <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                <Typography variant="body2" color="text.secondary">Agent:</Typography>
                <Chip size="small" label={thawTarget} />
              </Box>
            )}
            <Autocomplete
              freeSolo
              options={containers}
              value={thawContainer}
              onChange={(_, val) => setThawContainer(val || 'Main-Container')}
              onInputChange={(_, val) => setThawContainer(val)}
              renderInput={(params) => (
                <TextField {...params} label="Target Container" fullWidth />
              )}
            />
            <TextField
              label="Repository"
              value={thawRepo}
              onChange={(e) => setThawRepo(e.target.value)}
              fullWidth
              required
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setThawOpen(false)}>Cancel</Button>
          <Button onClick={handleThawSubmit} variant="contained" color="info" disabled={!thawRepo}>
            Thaw
          </Button>
        </DialogActions>
      </Dialog>

      {/* Inspect Agent Details Modal */}
      <Dialog open={inspectOpen} onClose={() => setInspectOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Agent Details</DialogTitle>
        <DialogContent dividers>
          {inspectedAgent && (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                <Typography variant="body2" color="text.secondary">Agent Name:</Typography>
                <Typography variant="body2" sx={{ fontWeight: 'bold' }}>{inspectedAgent.name}</Typography>
              </Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                <Typography variant="body2" color="text.secondary">Container:</Typography>
                <Typography variant="body2">{inspectedAgent.container}</Typography>
              </Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                <Typography variant="body2" color="text.secondary">Lifecycle State:</Typography>
                <Chip size="small" label={inspectedAgent.state || 'UNKNOWN'} color="primary" />
              </Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                <Typography variant="body2" color="text.secondary">Ownership:</Typography>
                <Typography variant="body2">{inspectedAgent.ownership || 'NONE'}</Typography>
              </Box>
              <Box>
                <Typography variant="body2" color="text.secondary" gutterBottom>Addresses:</Typography>
                {inspectedAgent.addresses && inspectedAgent.addresses.length > 0 ? (
                  inspectedAgent.addresses.map((addr) => (
                    <Typography key={addr} variant="caption" sx={{ display: 'block', wordBreak: 'break-all', fontFamily: 'monospace' }}>
                      {addr}
                    </Typography>
                  ))
                ) : (
                  <Typography variant="caption" color="text.secondary">None registered</Typography>
                )}
              </Box>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setInspectOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>

      <NotificationSnackbar feedback={feedback} onClose={close} />
    </Box>
  );
}
