import { useState } from 'react';
import {
  Box, Button, Grid, CircularProgress,
} from '@mui/material';
import { api } from 'shared';
import PageHeader from '../components/PageHeader';
import NotificationSnackbar, { useFeedback } from '../components/NotificationSnackbar';

const tools = ['sniffer', 'dummy', 'logger', 'introspector', 'df-gui'];

export default function ToolsPage() {
  const [launching, setLaunching] = useState<string | null>(null);
  const { feedback, notify, close } = useFeedback();

  const handleLaunch = async (tool: string) => {
    setLaunching(tool);
    try {
      await api.tools.start(tool, { container: 'Main-Container' });
      notify(`${tool} started`, 'success');
    } catch (e: any) {
      notify(`Failed to launch ${tool} - ${e.message || 'unknown error'}`, 'error');
    } finally {
      setLaunching(null);
    }
  };

  return (
    <Box>
      <PageHeader title="Tools" />

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

      <NotificationSnackbar feedback={feedback} onClose={close} />
    </Box>
  );
}
