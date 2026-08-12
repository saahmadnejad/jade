import { useEffect, useState } from 'react';
import {
  Box, Typography, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Paper, CircularProgress, Chip,
  IconButton, Tooltip, Snackbar, Alert, RefreshIcon as RefreshIcon_,
} from '@mui/material';
import DeleteForeverIcon from '@mui/icons-material/DeleteForever';
import RefreshIcon from '@mui/icons-material/Refresh';
import { api, type ContainerInfo, type ContainerListResponse } from 'shared';

export default function ContainersPage() {
  const [containers, setContainers] = useState<ContainerInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [snackbar, setSnackbar] = useState<{open: boolean; message: string; severity: 'success' | 'error'}>({
    open: false, message: '', severity: 'success',
  });

  const fetchContainers = async () => {
    setLoading(true);
    try {
      const data: ContainerListResponse = await api.containers.list();
      setContainers(data.containers);
    } catch (e) {
      console.error('Failed to fetch containers', e);
      setSnackbar({ open: true, message: 'Failed to fetch containers', severity: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchContainers();
  }, []);

  const handleKill = async (containerName: string) => {
    setActionLoading('kill:' + containerName);
    try {
      await api.containers.kill(containerName);
      setSnackbar({ open: true, message: `Container '${containerName}' killed`, severity: 'success' });
      fetchContainers();
    } catch (e: any) {
      setSnackbar({ open: true, message: `Kill failed - ${e.message || 'unknown error'}`, severity: 'error' });
    } finally {
      setActionLoading(null);
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
        <Typography variant="h4">Containers</Typography>
        <Tooltip title="Refresh">
          <IconButton onClick={fetchContainers} disabled={loading}>
            <RefreshIcon />
          </IconButton>
        </Tooltip>
      </Box>

      {containers.length === 0 ? (
        <Typography>No containers found</Typography>
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
