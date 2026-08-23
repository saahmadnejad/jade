import { useEffect, useState } from 'react';
import {
  Box, Typography, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Paper, Chip,
  IconButton, Tooltip, Dialog, DialogTitle, DialogContent,
  DialogActions, TextField, Button, Snackbar, Alert,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import RefreshIcon from '@mui/icons-material/Refresh';
import VisibilityIcon from '@mui/icons-material/Visibility';
import {
  api,
  type RemotePlatformInfo,
  type RemotePlatformListResponse,
  type RemotePlatformAddRequest,
  type RemotePlatformFetchRequest,
} from 'shared';
import TopProgressBar from '../components/TopProgressBar';

export default function PlatformsPage() {
  const [platforms, setPlatforms] = useState<RemotePlatformInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [addOpen, setAddOpen] = useState(false);
  const [addMode, setAddMode] = useState<'ams' | 'url'>('ams');
  const [addForm, setAddForm] = useState({ ams: '', addresses: '' });
  const [urlForm, setUrlForm] = useState({ url: '' });
  const [viewOpen, setViewOpen] = useState(false);
  const [selectedPlatform, setSelectedPlatform] = useState<RemotePlatformInfo | null>(null);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [snackbar, setSnackbar] = useState<{open: boolean; message: string; severity: 'success' | 'error'}>({
    open: false, message: '', severity: 'success',
  });

  const fetchPlatforms = async () => {
    setLoading(true);
    try {
      const data: RemotePlatformListResponse = await api.platforms.list();
      setPlatforms(data.platforms || []);
    } catch (e) {
      console.error('Failed to fetch platforms', e);
      setSnackbar({ open: true, message: 'Failed to fetch platforms', severity: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPlatforms();
  }, []);

  const handleAddViaAms = async () => {
    setActionLoading('add');
    try {
      const addresses = addForm.addresses
        ? addForm.addresses.split(',').map(s => s.trim()).filter(Boolean)
        : [];
      await api.platforms.add({ ams: addForm.ams, addresses });
      setSnackbar({ open: true, message: `Platform added via AMS '${addForm.ams}'`, severity: 'success' });
      setAddOpen(false);
      setAddForm({ ams: '', addresses: '' });
      fetchPlatforms();
    } catch (e: any) {
      setSnackbar({ open: true, message: `Add platform failed - ${e.message || 'unknown error'}`, severity: 'error' });
    } finally {
      setActionLoading(null);
    }
  };

  const handleAddViaUrl = async () => {
    setActionLoading('add-url');
    try {
      await api.platforms.fetch({ url: urlForm.url });
      setSnackbar({ open: true, message: `Platform fetched from URL`, severity: 'success' });
      setAddOpen(false);
      setUrlForm({ url: '' });
      fetchPlatforms();
    } catch (e: any) {
      setSnackbar({ open: true, message: `Fetch platform failed - ${e.message || 'unknown error'}`, severity: 'error' });
    } finally {
      setActionLoading(null);
    }
  };

  const handleRemove = async (name: string) => {
    const confirmed = window.confirm(`Remove remote platform '${name}'?`);
    if (!confirmed) return;

    setActionLoading('remove:' + name);
    try {
      await api.platforms.remove(name);
      setSnackbar({ open: true, message: `Platform '${name}' removed`, severity: 'success' });
      fetchPlatforms();
    } catch (e: any) {
      setSnackbar({ open: true, message: `Remove failed - ${e.message || 'unknown error'}`, severity: 'error' });
    } finally {
      setActionLoading(null);
    }
  };

  const handleViewDescription = async (name: string) => {
    setActionLoading('view:' + name);
    try {
      const desc = await api.platforms.getDescription(name);
      setSelectedPlatform(desc);
      setViewOpen(true);
    } catch (e: any) {
      setSnackbar({ open: true, message: `View description failed - ${e.message || 'unknown error'}`, severity: 'error' });
    } finally {
      setActionLoading(null);
    }
  };

  if (loading) {
    return <TopProgressBar />;
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h4">Remote Platforms</Typography>
        <Box display="flex" gap={1}>
          <Tooltip title="Refresh">
            <IconButton onClick={fetchPlatforms} disabled={loading}>
              <RefreshIcon />
            </IconButton>
          </Tooltip>
          <Button variant="contained" onClick={() => setAddOpen(true)} startIcon={<AddIcon />}>
            Add Platform
          </Button>
        </Box>
      </Box>

      {platforms.length === 0 ? (
        <Typography>No platforms found</Typography>
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>AMS</TableCell>
                <TableCell>Addresses</TableCell>
                <TableCell>Services</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {platforms.map((p) => (
                <TableRow key={p.name}>
                  <TableCell>{p.name}</TableCell>
                  <TableCell>{p.ams}</TableCell>
                  <TableCell>{p.addresses.length > 0 ? p.addresses.join(', ') : 'N/A'}</TableCell>
                  <TableCell>{p.services.length > 0 ? p.services.join(', ') : 'N/A'}</TableCell>
                  <TableCell align="right">
                    <Tooltip title="View Description">
                      <IconButton
                        color="info"
                        size="small"
                        disabled={actionLoading === 'view:' + p.name}
                        onClick={() => handleViewDescription(p.name)}
                      >
                        <VisibilityIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Remove Platform">
                      <IconButton
                        color="error"
                        size="small"
                        disabled={actionLoading === 'remove:' + p.name}
                        onClick={() => handleRemove(p.name)}
                      >
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Dialog open={addOpen} onClose={() => setAddOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add Remote Platform</DialogTitle>
        <DialogContent>
          <Box display="flex" gap={2} mb={2}>
            <Button
              variant={addMode === 'ams' ? 'contained' : 'outlined'}
              onClick={() => setAddMode('ams')}
            >
              Via AMS
            </Button>
            <Button
              variant={addMode === 'url' ? 'contained' : 'outlined'}
              onClick={() => setAddMode('url')}
            >
              Via URL
            </Button>
          </Box>
          {addMode === 'ams' ? (
            <Box display="flex" flexDirection="column" gap={2} pt={1}>
              <TextField
                label="AMS Agent Identifier"
                value={addForm.ams}
                onChange={e => setAddForm({ ...addForm, ams: e.target.value })}
                fullWidth
                helperText="e.g., ams@remote-platform"
              />
              <TextField
                label="Addresses (comma-separated)"
                value={addForm.addresses}
                onChange={e => setAddForm({ ...addForm, addresses: e.target.value })}
                fullWidth
                helperText="e.g., jades://192.168.1.10:1099"
              />
            </Box>
          ) : (
            <Box display="flex" flexDirection="column" gap={2} pt={1}>
              <TextField
                label="AP Description URL"
                value={urlForm.url}
                onChange={e => setUrlForm({ url: e.target.value })}
                fullWidth
                helperText="e.g., http://192.168.1.10:1099/ap"
              />
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAddOpen(false)}>Cancel</Button>
          <Button
            onClick={addMode === 'ams' ? handleAddViaAms : handleAddViaUrl}
            variant="contained"
            disabled={actionLoading === 'add' || actionLoading === 'add-url'}
          >
            {addMode === 'ams' ? 'Add' : 'Fetch'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={viewOpen} onClose={() => setViewOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Platform Description</DialogTitle>
        <DialogContent>
          {selectedPlatform && (
            <Box display="flex" flexDirection="column" gap={1} pt={1}>
              <Typography><strong>Name:</strong> {selectedPlatform.name}</Typography>
              <Typography><strong>AMS:</strong> {selectedPlatform.ams}</Typography>
              <Typography><strong>Addresses:</strong> {selectedPlatform.addresses.join(', ') || 'N/A'}</Typography>
              <Typography><strong>Services:</strong> {selectedPlatform.services.join(', ') || 'N/A'}</Typography>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setViewOpen(false)}>Close</Button>
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
