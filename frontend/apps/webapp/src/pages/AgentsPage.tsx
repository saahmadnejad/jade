import { useEffect, useState } from 'react';
import {
  Box, Typography, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Paper, CircularProgress, Chip,
} from '@mui/material';
import { api, type AgentInfo, type AgentListResponse } from 'shared';

export default function AgentsPage() {
  const [agents, setAgents] = useState<AgentInfo[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchAgents = async () => {
      try {
        const data: AgentListResponse = await api.agents.list();
        setAgents(data.agents);
      } catch (e) {
        console.error('Failed to fetch agents', e);
      } finally {
        setLoading(false);
      }
    };
    fetchAgents();
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
        Agents
      </Typography>
      {agents.length === 0 ? (
        <Typography>No agents found</Typography>
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>State</TableCell>
                <TableCell>Ownership</TableCell>
                <TableCell>Container</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {agents.map((agent) => (
                <TableRow key={agent.name}>
                  <TableCell>{agent.name}</TableCell>
                  <TableCell>{agent.state ?? 'N/A'}</TableCell>
                  <TableCell>{agent.ownership ?? 'N/A'}</TableCell>
                  <TableCell>{agent.container}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  );
}
