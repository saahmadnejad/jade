import { useState } from 'react';
import {
  Box, Typography, Button, Chip, Grid, CircularProgress,
} from '@mui/material';
import { api } from 'shared';

const tools = ['sniffer', 'dummy', 'logger', 'introspector', 'df-gui'];

export default function ToolsPage() {
  const [launching, setLaunching] = useState<string | null>(null);
  const [launched, setLaunched] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleLaunch = async (tool: string) => {
    setLaunching(tool);
    setError(null);
    try {
      await api.tools.start(tool, { container: 'Main-Container' });
      setLaunched(tool);
    } catch (e) {
      setError(`Failed to launch ${tool}`);
    } finally {
      setLaunching(null);
    }
  };

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Tools
      </Typography>

      {error && (
        <Typography color="error" gutterBottom>{error}</Typography>
      )}

      {launched && (
        <Chip label={`${launched} started`} color="success" sx={{ mb: 2 }} />
      )}

      <Grid container spacing={2}>
        {tools.map((tool) => (
          <Grid item xs={12} sm={6} md={3} key={tool}>
            <Button
              variant="contained"
              fullWidth
              onClick={() => handleLaunch(tool)}
              disabled={launching === tool}
              startIcon={launching === tool ? <CircularProgress size={16} /> : null}
            >
              Start {tool}
            </Button>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
}
