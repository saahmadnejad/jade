import { useEffect, useState } from 'react';
import {
  Box, Typography, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Paper, CircularProgress, IconButton,
  Tooltip, Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Button, Snackbar, Alert, Chip, Tabs, Tab,
} from '@mui/material';
import DeleteForeverIcon from '@mui/icons-material/DeleteForever';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import RefreshIcon from '@mui/icons-material/Refresh';
import SearchIcon from '@mui/icons-material/Search';
import PublicIcon from '@mui/icons-material/Public';
import {
  api,
  type DFRegistrationInfo,
  type DFServiceInfo,
  type DFRegistrationListResponse,
  type DFParentInfo,
  type DfFederateRequest,
  type DFStatusResponse,
  type DFDescriptionResponse,
} from 'shared';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index } = props;
  return (
    <div hidden={value !== index} style={{ display: value === index ? 'block' : 'none' }}>
      {value === index && <Box pt={2}>{children}</Box>}
    </div>
  );
}

export default function DFPage() {
  const [registrations, setRegistrations] = useState<DFRegistrationInfo[]>([]);
  const [parents, setParents] = useState<DFParentInfo[]>([]);
  const [children, setChildren] = useState<DFParentInfo[]>([]);
  const [dfStatus, setDfStatus] = useState<DFStatusResponse | null>(null);
  const [dfDescription, setDfDescription] = useState<DFDescriptionResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [tabValue, setTabValue] = useState(0);
  const [registerOpen, setRegisterOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [modifyOpen, setModifyOpen] = useState(false);
  const [modifyTarget, setModifyTarget] = useState<DFRegistrationInfo | null>(null);
  const [federateOpen, setFederateOpen] = useState(false);
  const [descOpen, setDescOpen] = useState(false);
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
  const [federateForm, setFederateForm] = useState({
    parentDF: '',
    parentDFAddresses: '',
  });
  const [snackbar, setSnackbar] = useState<{open: boolean; message: string; severity: 'success' | 'error'}>({
    open: false, message: '', severity: 'success',
  });

  const fetchAll = async () => {
    setLoading(true);
    try {
      const [regData, parentData, childData, statusData, descData] = await Promise.all([
        api.df.listRegistrations(),
        api.df.getParents(),
        api.df.getChildren(),
        api.df.getStatus(),
        api.df.getDescription(),
      ]);
      setRegistrations(regData.registrations || []);
      setParents(parentData.parents || []);
      setChildren(childData.children || []);
      setDfStatus(statusData);
      setDfDescription(descData);
    } catch (e: any) {
      setSnackbar({ open: true, message: `Failed to fetch DF data: ${e.message || 'unknown error'}`, severity: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAll();
  }, []);

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

  const fetchDfStatus = async () => {
    try {
      const status = await api.df.getStatus();
      setDfStatus(status);
    } catch (e) {
      console.error('Failed to fetch DF status', e);
    }
  };

  const handleRefreshDf = async () => {
    setActionLoading('refresh');
    try {
      const resp = await api.df.refresh();
      setSnackbar({ open: true, message: `DF refreshed: ${resp.registeredAgentCount} registered agents`, severity: 'success' });
      fetchAll();
    } catch (e: any) {
      setSnackbar({ open: true, message: `DF refresh failed: ${e.message || 'unknown error'}`, severity: 'error' });
    } finally {
      setActionLoading(null);
    }
  };

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

  const handleModify = async () => {
    if (!modifyTarget) return;
    try {
      const services: DFServiceInfo[] = [];
      if (registerForm.serviceType) {
        services.push({
          type: registerForm.serviceType,
          name: registerForm.serviceName || '',
          ownership: '',
        });
      }
      await api.df.modifyRegistration(modifyTarget.name, {
        addresses: registerForm.addresses ? registerForm.addresses.split(',').map(s => s.trim()).filter(Boolean) : [],
        services: services.length > 0 ? services : undefined,
      });
      setSnackbar({ open: true, message: `Registration for '${modifyTarget.name}' modified`, severity: 'success' });
      setModifyOpen(false);
      setModifyTarget(null);
      fetchRegistrations();
    } catch (e: any) {
      setSnackbar({ open: true, message: `Modify failed: ${e.message || 'unknown error'}`, severity: 'error' });
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

  const handleFederate = async () => {
    try {
      const request: DfFederateRequest = {
        parentDF: federateForm.parentDF,
        parentDFAddresses: federateForm.parentDFAddresses
          ? federateForm.parentDFAddresses.split(',').map(s => s.trim()).filter(Boolean)
          : [],
      };
      await api.df.federate(request);
      setSnackbar({ open: true, message: `DF federated with '${federateForm.parentDF}'`, severity: 'success' });
      setFederateOpen(false);
      setFederateForm({ parentDF: '', parentDFAddresses: '' });
      setParents([...parents, { name: federateForm.parentDF, addresses: request.parentDFAddresses }]);
    } catch (e: any) {
      setSnackbar({ open: true, message: `Federation failed: ${e.message || 'unknown error'}`, severity: 'error' });
    }
  };

  const handleDeregisterParent = async (name: string) => {
    const confirmed = window.confirm(`Deregister from parent DF '${name}'?`);
    if (!confirmed) return;

    setActionLoading('deregisterParent:' + name);
    try {
      await api.df.deregisterParent(name);
      setSnackbar({ open: true, message: `Deregistered from parent DF '${name}'`, severity: 'success' });
      setParents(parents.filter(p => p.name !== name));
    } catch (e: any) {
      setSnackbar({ open: true, message: `Deregister parent failed: ${e.message || 'unknown error'}`, severity: 'error' });
    } finally {
      setActionLoading(null);
    }
  };

  const handleDeregisterChild = async (name: string) => {
    const confirmed = window.confirm(`Deregister child DF '${name}' from this DF?`);
    if (!confirmed) return;

    setActionLoading('deregisterChild:' + name);
    try {
      await api.df.deregisterChild(name);
      setSnackbar({ open: true, message: `Child DF '${name}' deregistered`, severity: 'success' });
      setChildren(children.filter(c => c.name !== name));
    } catch (e: any) {
      setSnackbar({ open: true, message: `Deregister child failed: ${e.message || 'unknown error'}`, severity: 'error' });
    } finally {
      setActionLoading(null);
    }
  };

  const handleModifyClick = (reg: DFRegistrationInfo) => {
    setModifyTarget(reg);
    setRegisterForm({
      name: reg.name,
      addresses: reg.addresses?.join(', ') || '',
      serviceType: reg.services?.[0]?.type || '',
      serviceName: reg.services?.[0]?.name || '',
    });
    setModifyOpen(true);
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
          <Tooltip title="Refresh DF Data">
            <IconButton onClick={handleRefreshDf} disabled={loading || !!actionLoading}>
              <RefreshIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title="View DF Description">
            <IconButton
              color="info"
              size="small"
              onClick={async () => {
                try {
                  const desc = await api.df.getDescription();
                  setDfDescription(desc);
                  setDescOpen(true);
                } catch (e: any) {
                  setSnackbar({ open: true, message: `Failed to load DF description: ${e.message || 'unknown error'}`, severity: 'error' });
                }
              }}
            >
              <SearchIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Button variant="outlined" onClick={() => setSearchOpen(true)} startIcon={<SearchIcon />}>
            Search
          </Button>
          <Button variant="contained" onClick={() => setRegisterOpen(true)} startIcon={<AddIcon />}>
            Register Agent
          </Button>
        </Box>
      </Box>

      {dfStatus && (
        <Paper sx={{ p: 2, mb: 2 }}>
          <Box display="flex" gap={2} alignItems="center" flexWrap="wrap">
            <Chip
              label={dfStatus.running ? 'DF Running' : 'DF Not Running'}
              color={dfStatus.running ? 'success' : 'default'}
              size="small"
            />
            {dfStatus.agent && <Chip label={`Agent: ${dfStatus.agent}`} size="small" variant="outlined" />}
            {dfStatus.container && <Chip label={`Container: ${dfStatus.container}`} size="small" variant="outlined" />}
            {dfStatus.registeredAgentCount !== undefined && <Chip label={`Registered: ${dfStatus.registeredAgentCount}`} size="small" variant="outlined" />}
            {dfStatus.parentCount !== undefined && <Chip label={`Parents: ${dfStatus.parentCount}`} size="small" variant="outlined" />}
            {dfStatus.childCount !== undefined && <Chip label={`Children: ${dfStatus.childCount}`} size="small" variant="outlined" />}
          </Box>
        </Paper>
      )}

      <Box display="flex" mb={2} gap={1}>
        <Chip
          icon={<PublicIcon />}
          label={`Parents: ${parents.length}`}
          size="small"
          variant={tabValue === 1 ? 'filled' : 'outlined'}
          onClick={() => setTabValue(1)}
        />
        <Chip
          label={`Children: ${children.length}`}
          size="small"
          variant={tabValue === 2 ? 'filled' : 'outlined'}
          onClick={() => setTabValue(2)}
        />
      </Box>

      <TabPanel value={tabValue} index={0}>
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
                          <Chip key={svc.type || svc.name} label={`${svc.name || svc.type} (${svc.type})`} size="small" sx={{ mr: 0.5, mb: 0.5 }} />
                        ))
                      ) : 'N/A'}
                    </TableCell>
                    <TableCell align="right">
                      <Tooltip title="Modify Registration">
                        <IconButton
                          color="info"
                          size="small"
                          disabled={actionLoading === 'modify:' + reg.name}
                          onClick={() => handleModifyClick(reg)}
                        >
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
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
      </TabPanel>

      <TabPanel value={tabValue} index={1}>
        {parents.length === 0 ? (
          <Typography>No parent DFs found</Typography>
        ) : (
          <TableContainer component={Paper}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Parent DF Name</TableCell>
                  <TableCell>Addresses</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {parents.map((p) => (
                  <TableRow key={p.name}>
                    <TableCell>{p.name}</TableCell>
                    <TableCell>{p.addresses.length > 0 ? p.addresses.join(', ') : 'N/A'}</TableCell>
                    <TableCell align="right">
                      <Tooltip title="Deregister from Parent">
                        <IconButton
                          color="error"
                          size="small"
                          disabled={actionLoading === 'deregisterParent:' + p.name}
                          onClick={() => handleDeregisterParent(p.name)}
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
        <Box mt={2}>
          <Button variant="contained" onClick={() => setFederateOpen(true)} startIcon={<AddIcon />}>
            Federate with Parent DF
          </Button>
        </Box>
      </TabPanel>

      <TabPanel value={tabValue} index={2}>
        {children.length === 0 ? (
          <Typography>No child DFs found</Typography>
        ) : (
          <TableContainer component={Paper}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Child DF Name</TableCell>
                  <TableCell>Addresses</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {children.map((c) => (
                  <TableRow key={c.name}>
                    <TableCell>{c.name}</TableCell>
                    <TableCell>{c.addresses.length > 0 ? c.addresses.join(', ') : 'N/A'}</TableCell>
                    <TableCell align="right">
                      <Tooltip title="Deregister Child">
                        <IconButton
                          color="error"
                          size="small"
                          disabled={actionLoading === 'deregisterChild:' + c.name}
                          onClick={() => handleDeregisterChild(c.name)}
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
      </TabPanel>

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

      <Dialog open={modifyOpen} onClose={() => setModifyOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Modify Registration</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField
              label="Agent Name"
              value={registerForm.name}
              disabled
              fullWidth
            />
            <TextField
              label="Addresses (comma-separated)"
              value={registerForm.addresses}
              onChange={e => setRegisterForm({ ...registerForm, addresses: e.target.value })}
              fullWidth
              helperText="Comma-separated agent addresses"
            />
            <TextField
              label="Service Type"
              value={registerForm.serviceType}
              onChange={e => setRegisterForm({ ...registerForm, serviceType: e.target.value })}
              fullWidth
            />
            <TextField
              label="Service Name"
              value={registerForm.serviceName}
              onChange={e => setRegisterForm({ ...registerForm, serviceName: e.target.value })}
              fullWidth
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setModifyOpen(false)}>Cancel</Button>
          <Button onClick={handleModify} variant="contained" disabled={!modifyTarget}>
            Modify
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

      <Dialog open={federateOpen} onClose={() => setFederateOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Federate with Parent DF</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField
              label="Parent DF Name"
              value={federateForm.parentDF}
              onChange={e => setFederateForm({ ...federateForm, parentDF: e.target.value })}
              fullWidth
              helperText="Full AID of the parent DF (e.g. df@remote-platform)"
            />
            <TextField
              label="Parent DF Addresses (comma-separated)"
              value={federateForm.parentDFAddresses}
              onChange={e => setFederateForm({ ...federateForm, parentDFAddresses: e.target.value })}
              fullWidth
              helperText="e.g., jades://10.0.0.1:1099"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setFederateOpen(false)}>Cancel</Button>
          <Button onClick={handleFederate} variant="contained" disabled={!federateForm.parentDF}>
            Federate
          </Button>
        </DialogActions>
       </Dialog>

      <Dialog open={descOpen} onClose={() => setDescOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>DF Description</DialogTitle>
        <DialogContent>
          {dfDescription && (
            <Box display="flex" flexDirection="column" gap={1} pt={1}>
              <Typography><strong>Name:</strong> {dfDescription.name}</Typography>
              <Typography><strong>Addresses:</strong> {dfDescription.addresses?.join(', ') || 'N/A'}</Typography>
              <Typography><strong>Services:</strong>
                {dfDescription.services && dfDescription.services.length > 0 ? (
                  <Box display="flex" gap={1} mt={1} flexWrap="wrap">
                    {dfDescription.services.map((svc, i) => (
                      <Chip key={i} label={`${svc.name || svc.type} (${svc.type})`} size="small" />
                    ))}
                  </Box>
                ) : 'N/A'}
              </Typography>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDescOpen(false)}>Close</Button>
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
