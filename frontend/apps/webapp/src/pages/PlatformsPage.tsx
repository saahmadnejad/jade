import { useEffect, useState } from 'react';
import {
  Box, Typography, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Paper, CircularProgress, Chip,
} from '@mui/material';
import { api, type RemotePlatformInfo, type RemotePlatformListResponse } from 'shared';

export default function PlatformsPage() {
  const [platforms, setPlatforms] = useState<RemotePlatformInfo[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchPlatforms = async () => {
      try {
        const data: RemotePlatformListResponse = await api.platforms.list();
        setPlatforms(data.platforms);
      } catch (e) {
        console.error('Failed to fetch platforms', e);
      } finally {
        setLoading(false);
      }
    };
    fetchPlatforms();
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
        Remote Platforms
      </Typography>
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
              </TableRow>
            </TableHead>
            <TableBody>
              {platforms.map((p) => (
                <TableRow key={p.name}>
                  <TableCell>{p.name}</TableCell>
                  <TableCell>{p.ams}</TableCell>
                  <TableCell>{p.addresses.join(', ')}</TableCell>
                  <TableCell>{p.services.join(', ')}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  );
}
