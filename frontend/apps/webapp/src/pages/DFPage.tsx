import { useEffect, useState } from 'react';
import {
  Box, Typography, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Paper, CircularProgress, IconButton,
  Tooltip, Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Button, Snackbar, Alert, Chip,
} from '@mui/material';
import DeleteForeverIcon from '@mui/icons-material/DeleteForever';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import RefreshIcon from '@mui/icons-material/Refresh';
import { api, type DFRegistrationInfo, type DFServiceInfo, type DFRegistrationListResponse } from 'shared';

export default function DFPage() {
  const [registrations, setRegistrations] = useState<DFRegistrationInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [registerOpen, setRegisterOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [registerForm, setRegisterForm] = useState({
    name: '',
    addresses: '',
    serviceType: '',
    serviceName: '',
  });
  const [searchForm, setSearchForm] = useState({
    serviceName: '',
    serviceType: '',
  });
  const [snackbar, setSnackbar] = useState<{open: boolean; message: string; severity: 'success' | 'error'}>({
    open: false, message: '', severity: 'success',
  });

  const fetchRegistrations = async () => {
    setLoading(true);
    try {
      const data: DFRegistrationListResponse = await api.df.listRegistrations();
      setRegistrations(data.registrations || []);
    } catch (e: any) {
      setSnackbar({ open: true, message: `Failed to fetch DF registrations: ${e.message || 'unknown error'}`, severity: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRegistrations();
  }, []);

  const handleRegister = async () => {
    try {
      const services: DFServiceInfo[] = [];
      if (registerForm.serviceType) {
        services.push({
          type: registerForm.serviceType,
          name: registerForm.serviceName || '',
          ownership: '',
        });
      }

      await api.df.register({
        agentName: registerForm.name,
        addresses: registerForm.addresses ? registerForm.addresses.split(',').map(s => s.trim()).filter(Boolean) : [],
        services: services.length > 0 ? services : undefined,
      });
      setSnackbar({ open: true, message: `Agent '${registerForm.name}' registered with DF`, severity: 'success' });
      setRegisterOpen(false);
      setRegisterForm({ name: '', addresses: '', serviceType: '', serviceName: '' });
      fetchRegistrations();
    } catch (e: any) {
      setSnackbar({ open: true, message: `Registration failed: ${e.message || 'unknown error'}`, severity: 'error' });
    }
  };

  const handleDeregister = async (agentName: string) => {
    const confirmed = window.confirm(`Deregister agent '${agentName}' from DF?`);
    if (!confirmed) return;

    setActionLoading('deregister:' + agentName);
    try {
      await api.df.deregister(agentName);
      setSnackbar({ open: true, message: `Agent '${agentName}' deregistered`, severity: 'success' });
      fetchRegistrations();
    } catch (e: any) {
      setSnackbar({ open: true, message: `Deregister failed: ${e.message || 'unknown error'}`, severity: 'error' });
    } finally {
      setActionLoading(null);
    }
  };

  const handleSearch = async () => {
    try {
      const resp = await api.df.search({
        description: {
          services: searchForm.serviceType ? [{ type: searchForm.serviceType, name: searchForm.serviceName || undefined }] : undefined,
        },
        constraints: { maxResults: -1, maxDepth: 0 },
      });
      setRegistrations(resp.results || []);
      setSearchOpen(false);
      setSearchForm({ serviceName: '', serviceType: '' });
    } catch (e: any) {
      setSnackbar({ open: true, message: `Search failed: ${e.message || 'unknown error'}`, severity: 'error' });
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
        <Typography variant="h4">Directory Facilitator (DF)</Typography>
        <Box display="flex" gap={1}>
          <Tooltip title="Refresh">
            <IconButton onClick={fetchRegistrations} disabled={loading}>
              <RefreshIcon />
            </IconButton>
          </Tooltip>
          <Button variant="outlined" onClick={() => setSearchOpen(true)}>
            Search
          </Button>
          <Button variant="contained" onClick={() => setRegisterOpen(true)} startIcon={<AddIcon />}>
            Register Agent
          </Button>
        </Box>
      </Box>

      {registrations.length === 0 ? (
        <Typography>No agents registered with DF</Typography>
      ) : (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Agent Name</TableCell>
                <TableCell>Addresses</TableCell>
                <TableCell>Services</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {registrations.map((reg) => (
                <TableRow key={reg.name} hover>
                  <TableCell>{reg.name}</TableCell>
                  <TableCell>
                    {reg.addresses && reg.addresses.length > 0
                      ? reg.addresses.map(addr => <Chip key={addr} label={addr} size="small" sx={{ mr: 0.5, mb: 0.5 }} />)
                      : 'N/A'}
                  </TableCell>
                  <TableCell>
                    {reg.services && reg.services.length > 0 ? (
                      reg.services.map(svc => (
                        <Chip key={svc.name || svc.type} label={`${svc.name || svc.type} (${svc.type})`} size="small" sx={{ mr: 0.5, mb: 0.5 }} />
                      ))
                    ) : 'N/A'}
                  </TableCell>
                  <TableCell align="right">
                    <Tooltip title="Deregister">
                      <IconButton
                        color="error"
                        size="small"
                        disabled={actionLoading === 'deregister:' + reg.name}
                        onClick={() => handleDeregister(reg.name)}
                      >
                        <DeleteForeverIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Dialog open={registerOpen} onClose={() => setRegisterOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Register Agent with DF</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField
              label="Agent Name"
              value={registerForm.name}
              onChange={e => setRegisterForm({ ...registerForm, name: e.target.value })}
              fullWidth
              helperText="Full agent AID (e.g. my-agent@jade-main)"
            />
            <TextField
              label="Addresses (comma-separated)"
              value={registerForm.addresses}
              onChange={e => setRegisterForm({ ...registerForm, addresses: e.target.value })}
              fullWidth
              helperText="Optional: comma-separated agent addresses"
            />
            <TextField
              label="Service Type"
              value={registerForm.serviceType}
              onChange={e => setRegisterForm({ ...registerForm, serviceType: e.target.value })}
              fullWidth
              helperText="e.g., weather-forecast"
            />
            <TextField
              label="Service Name"
              value={registerForm.serviceName}
              onChange={e => setRegisterForm({ ...registerForm, serviceName: e.target.value })}
              fullWidth
              helperText="Optional: human-readable service name"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRegisterOpen(false)}>Cancel</Button>
          <Button onClick={handleRegister} variant="contained" disabled={!registerForm.name}>
            Register
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={searchOpen} onClose={() => setSearchOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Search DF</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField
              label="Service Type"
              value={searchForm.serviceType}
              onChange={e => setSearchForm({ ...searchForm, serviceType: e.target.value })}
              fullWidth
              helperText="Filter by service type (leave empty to match all)"
            />
            <TextField
              label="Service Name"
              value={searchForm.serviceName}
              onChange={e => setSearchForm({ ...searchForm, serviceName: e.target.value })}
              fullWidth
              helperText="Optional: filter by service name"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSearchOpen(false)}>Cancel</Button>
          <Button onClick={handleSearch} variant="contained">
            Search
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
