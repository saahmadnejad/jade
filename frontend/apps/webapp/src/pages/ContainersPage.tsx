import { useEffect, useState } from 'react';
import {
  Box, Typography, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Paper, CircularProgress, Chip,
  IconButton, Tooltip,
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Button,
} from '@mui/material';
import DeleteForeverIcon from '@mui/icons-material/DeleteForever';
import RefreshIcon from '@mui/icons-material/Refresh';
import SaveIcon from '@mui/icons-material/Save';
import UploadIcon from '@mui/icons-material/Upload';
import AddIcon from '@mui/icons-material/Add';
import TuneIcon from '@mui/icons-material/Tune';
import {
  api,
  type ContainerInfo,
  type ContainerListResponse,
  type MTPInfo,
  type MTPListResponse,
} from 'shared';
import TopProgressBar from '../components/TopProgressBar';
import PageHeader from '../components/PageHeader';
import EmptyState from '../components/EmptyState';
import NotificationSnackbar, { useFeedback } from '../components/NotificationSnackbar';

export default function ContainersPage() {
  const [containers, setContainers] = useState<ContainerInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [mtps, setMtps] = useState<Record<string, MTPInfo[]>>({});
  const [mtpsLoading, setMtpsLoading] = useState<Record<string, boolean>>({});
  const { feedback, notify, close } = useFeedback();

  const [saveOpen, setSaveOpen] = useState(false);
  const [saveTarget, setSaveTarget] = useState<string | null>(null);
  const [saveForm, setSaveForm] = useState({ repository: '' });
  const [loadOpen, setLoadOpen] = useState(false);
  const [loadTarget, setLoadTarget] = useState<string | null>(null);
  const [loadForm, setLoadForm] = useState({ repository: '' });
  const [mtpInstallOpen, setMtpInstallOpen] = useState(false);
  const [mtpInstallTarget, setMtpInstallTarget] = useState<string | null>(null);
  const [mtpForm, setMtpForm] = useState({ className: '', address: '' });

  const fetchContainers = async () => {
    setLoading(true);
    try {
      const data: ContainerListResponse = await api.containers.list();
      setContainers(data.containers);
    } catch (e) {
      console.error('Failed to fetch containers', e);
      notify('Failed to fetch containers', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchContainers();
  }, []);

  const fetchMtps = async (containerName: string) => {
    setMtpsLoading(prev => ({ ...prev, [containerName]: true }));
    try {
      const data: MTPListResponse = await api.containers.listMTPs(containerName);
      setMtps(prev => ({ ...prev, [containerName]: data.mtps || [] }));
    } catch (e: any) {
      notify(`Failed to fetch MTPs: ${e.message || 'unknown error'}`, 'error');
    } finally {
      setMtpsLoading(prev => ({ ...prev, [containerName]: false }));
    }
  };

  const handleKill = async (containerName: string) => {
    if (!window.confirm(`Kill container '${containerName}'? This cannot be undone.`)) return;
    setActionLoading('kill:' + containerName);
    try {
      await api.containers.kill(containerName);
      notify(`Container '${containerName}' killed`, 'success');
      fetchContainers();
    } catch (e: any) {
      notify(`Kill failed - ${e.message || 'unknown error'}`, 'error');
    } finally {
      setActionLoading(null);
    }
  };

  const handleSave = async () => {
    if (!saveTarget) return;
    try {
      await api.containers.save(saveTarget, saveForm.repository);
      notify(`Container '${saveTarget}' saved to ${saveForm.repository}`, 'success');
      setSaveOpen(false);
      setSaveForm({ repository: '' });
    } catch (e: any) {
      notify(`Save failed - ${e.message || 'unknown error'}`, 'error');
    }
  };

  const handleLoad = async () => {
    if (!loadTarget) return;
    try {
      await api.containers.load(loadTarget, loadForm.repository);
      notify(`Container '${loadTarget}' loaded from ${loadForm.repository}`, 'success');
      setLoadOpen(false);
      setLoadForm({ repository: '' });
      fetchContainers();
    } catch (e: any) {
      notify(`Load failed - ${e.message || 'unknown error'}`, 'error');
    }
  };

  const handleInstallMtp = async () => {
    if (!mtpInstallTarget) return;
    try {
      await api.containers.installMTP(mtpInstallTarget, {
        className: mtpForm.className,
        address: mtpForm.address,
      });
      notify(`MTP installed on '${mtpInstallTarget}'`, 'success');
      setMtpInstallOpen(false);
      setMtpForm({ className: '', address: '' });
      fetchMtps(mtpInstallTarget);
    } catch (e: any) {
      notify(`MTP install failed - ${e.message || 'unknown error'}`, 'error');
    }
  };

  const handleUninstallMtp = async (containerName: string, address: string) => {
    setActionLoading('uninstall:' + containerName + ':' + address);
    try {
      await api.containers.uninstallMTP(containerName, address);
      notify(`MTP at '${address}' uninstalled from '${containerName}'`, 'success');
      fetchMtps(containerName);
    } catch (e: any) {
      notify(`MTP uninstall failed - ${e.message || 'unknown error'}`, 'error');
    } finally {
      setActionLoading(null);
    }
  };

  if (loading) {
    return <TopProgressBar />;
  }

  return (
    <Box>
      <PageHeader
        title="Containers"
        actions={
          <Tooltip title="Refresh">
            <IconButton onClick={fetchContainers} disabled={loading}>
              <RefreshIcon />
            </IconButton>
          </Tooltip>
        }
      />

      {containers.length === 0 ? (
        <EmptyState message="No containers found" />
      ) : (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>Address</TableCell>
                <TableCell>Port</TableCell>
                <TableCell>Main</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {containers.map((container) => (
                <TableRow key={container.name} hover>
                  <TableCell>{container.name}</TableCell>
                  <TableCell>{container.address}</TableCell>
                  <TableCell>{container.port}</TableCell>
                  <TableCell>
                    {container.isMain ? (
                      <Chip label="Main" color="primary" size="small" />
                    ) : (
                      <Chip label="Secondary" size="small" />
                    )}
                  </TableCell>
                  <TableCell align="right">
                    {!container.isMain && (
                      <>
                        <Tooltip title="Save Container">
                          <IconButton
                            color="info"
                            size="small"
                            onClick={() => { setSaveTarget(container.name); setSaveOpen(true); }}
                          >
                            <SaveIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Load Container">
                          <IconButton
                            color="info"
                            size="small"
                            onClick={() => { setLoadTarget(container.name); setLoadOpen(true); }}
                          >
                            <UploadIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                         <Tooltip title="Manage MTPs">
                           <IconButton
                             color="secondary"
                             size="small"
                             onClick={() => fetchMtps(container.name)}
                           >
                             <TuneIcon fontSize="small" />
                           </IconButton>
                         </Tooltip>
                        <Tooltip title="Kill Container">
                          <IconButton
                            color="error"
                            size="small"
                            disabled={actionLoading === 'kill:' + container.name}
                            onClick={() => handleKill(container.name)}
                          >
                            <DeleteForeverIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </>
                    )}
                    {container.isMain && (
                      <Tooltip title="Kill Container">
                        <IconButton
                          color="error"
                          size="small"
                          disabled={actionLoading === 'kill:' + container.name}
                          onClick={() => handleKill(container.name)}
                        >
                          <DeleteForeverIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {containers.length > 0 && (
        <Box mt={3}>
          <Typography variant="h6" gutterBottom>MTP Management</Typography>
          {Object.entries(mtps).map(([containerName, containerMtps]) => (
            <Box key={containerName} mb={2}>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                <Typography variant="subtitle1">{containerName}</Typography>
                {mtpsLoading[containerName] ? (
                  <CircularProgress size={16} />
                ) : (
                  <Box display="flex" gap={1} alignItems="center">
                    {containerMtps.map((mtp) => (
                      <Chip
                        key={mtp.address}
                        label={`${mtp.className} (${mtp.address})`}
                        size="small"
                        onDelete={() => handleUninstallMtp(containerName, mtp.address)}
                        disabled={actionLoading === 'uninstall:' + containerName + ':' + mtp.address}
                        DeleteIcon={<DeleteForeverIcon fontSize="small" />}
                      />
                    ))}
                    <Tooltip title="Install MTP">
                      <IconButton
                        size="small"
                        onClick={() => { setMtpInstallTarget(containerName); setMtpInstallOpen(true); }}
                      >
                        <AddIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </Box>
                )}
              </Box>
            </Box>
          ))}
          {Object.keys(mtps).length === 0 && (
            <Typography variant="body2" color="text.secondary">
              Click the MTP management button on a container to view installed MTPs.
            </Typography>
          )}
        </Box>
      )}

      <Dialog open={saveOpen} onClose={() => setSaveOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Save Container</DialogTitle>
        <DialogContent>
          <Box pt={1}>
            <TextField
              label="Repository URL"
              value={saveForm.repository}
              onChange={e => setSaveForm({ repository: e.target.value })}
              fullWidth
              helperText="e.g., file:///tmp/container-store"
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
        <DialogTitle>Load Container</DialogTitle>
        <DialogContent>
          <Box pt={1}>
            <TextField
              label="Repository URL"
              value={loadForm.repository}
              onChange={e => setLoadForm({ repository: e.target.value })}
              fullWidth
              helperText="e.g., file:///tmp/container-store"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setLoadOpen(false)}>Cancel</Button>
          <Button onClick={handleLoad} variant="contained" disabled={!loadForm.repository}>
            Load
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={mtpInstallOpen} onClose={() => setMtpInstallOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Install MTP on Container</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField
              label="MTP Class Name"
              value={mtpForm.className}
              onChange={e => setMtpForm({ ...mtpForm, className: e.target.value })}
              fullWidth
              helperText="e.g., io.donbee.jade.mtp.mpi.MPIMessageTransportProtocol"
            />
            <TextField
              label="Address"
              value={mtpForm.address}
              onChange={e => setMtpForm({ ...mtpForm, address: e.target.value })}
              fullWidth
              helperText="e.g., jades://127.0.0.1:1099"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setMtpInstallOpen(false)}>Cancel</Button>
          <Button onClick={handleInstallMtp} variant="contained" disabled={!mtpForm.className || !mtpForm.address}>
            Install
          </Button>
        </DialogActions>
      </Dialog>

      <NotificationSnackbar feedback={feedback} onClose={close} />
    </Box>
  );
}
