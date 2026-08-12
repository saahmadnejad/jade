import { useEffect, useState } from 'react';
import {
  Box, Typography, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Paper, CircularProgress, Chip,
} from '@mui/material';
import { api, type ContainerInfo, type ContainerListResponse } from 'shared';

export default function ContainersPage() {
  const [containers, setContainers] = useState<ContainerInfo[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchContainers = async () => {
      try {
        const data: ContainerListResponse = await api.containers.list();
        setContainers(data.containers);
      } catch (e) {
        console.error('Failed to fetch containers', e);
      } finally {
        setLoading(false);
      }
    };
    fetchContainers();
  }, []);

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="60vh">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Containers
      </Typography>
      {containers.length === 0 ? (
        <Typography>No containers found</Typography>
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>Address</TableCell>
                <TableCell>Port</TableCell>
                <TableCell>Main</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {containers.map((container) => (
                <TableRow key={container.name}>
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
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  );
}
