import { useEffect, useState } from 'react';
import { Box, Card, CardContent, Chip, Alert, Button, Typography, Divider } from '@mui/material';
import { api, type HealthStatus, type PlatformInfo } from 'shared';
import PageHeader from '../components/PageHeader';
import TopProgressBar from '../components/TopProgressBar';
import PlatformTreeView from '../components/PlatformTreeView';
import MessageComposerDialog from '../components/MessageComposerDialog';
import NotificationSnackbar, { useFeedback } from '../components/NotificationSnackbar';

export default function DashboardPage() {
  const [health, setHealth] = useState<HealthStatus | null>(null);
  const [platform, setPlatform] = useState<PlatformInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [composerTarget, setComposerTarget] = useState<string | null>(null);
  const { feedback, notify, close } = useFeedback();

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [healthData, platformData] = await Promise.all([
        api.platform.health(),
        api.platform.getInfo(),
      ]);
      setHealth(healthData);
      setPlatform(platformData);
    } catch {
      setError('Failed to connect to backend');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleShutdown = async () => {
    if (!window.confirm('Shutdown the entire JADE platform? This cannot be undone.')) return;
    try {
      await api.platform.shutdown();
      notify('Platform shutdown initiated', 'success');
    } catch (e: any) {
      notify(`Shutdown failed - ${e.message || 'unknown error'}`, 'error');
    }
  };

  const handleKillAgent = async (name: string) => {
    if (!window.confirm(`Kill agent '${name}'? This cannot be undone.`)) return;
    try {
      await api.agents.kill(name);
      notify(`Agent '${name}' killed`, 'success');
    } catch (e: any) {
      notify(`Failed to kill agent '${name}': ${e.message}`, 'error');
    }
  };

  const handleSuspendAgent = async (name: string) => {
    try {
      await api.agents.suspend(name);
      notify(`Agent '${name}' suspended`, 'success');
    } catch (e: any) {
      notify(`Failed to suspend agent '${name}': ${e.message}`, 'error');
    }
  };

  const handleResumeAgent = async (name: string) => {
    try {
      await api.agents.resume(name);
      notify(`Agent '${name}' resumed`, 'success');
    } catch (e: any) {
      notify(`Failed to resume agent '${name}': ${e.message}`, 'error');
    }
  };

  const handleKillContainer = async (name: string) => {
    if (!window.confirm(`Kill container '${name}'? This cannot be undone.`)) return;
    try {
      await api.containers.kill(name);
      notify(`Container '${name}' killed`, 'success');
    } catch (e: any) {
      notify(`Failed to kill container '${name}': ${e.message}`, 'error');
    }
  };

  if (loading) {
    return <TopProgressBar />;
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
      <PageHeader
        title="Platform Dashboard"
        actions={
          <>
            <Chip
              label={health?.status === 'ok' ? 'Healthy' : 'Unhealthy'}
              color={health?.status === 'ok' ? 'success' : 'error'}
            />
            <Button variant="outlined" color="error" onClick={handleShutdown}>
              Shutdown Platform
            </Button>
          </>
        }
      />

      {error && (
        <Alert severity="error">{error}</Alert>
      )}

      {platform && !error && (
        <Card variant="outlined">
          <CardContent>
            <Typography variant="h6" gutterBottom>Platform Overview</Typography>
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: '1fr 1fr 1fr 1fr' }, gap: 2 }}>
              <Box>
                <Typography variant="caption" color="text.secondary">Platform ID</Typography>
                <Typography variant="body1" sx={{ fontWeight: 'bold' }}>{platform.platformID}</Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">Main Container</Typography>
                <Typography variant="body1">{platform.containerName}</Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">AMS Address</Typography>
                <Typography variant="body2" sx={{ wordBreak: 'break-all' }}>{platform.ams}</Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">Default DF</Typography>
                <Typography variant="body2" sx={{ wordBreak: 'break-all' }}>{platform.defaultDF}</Typography>
              </Box>
            </Box>
          </CardContent>
        </Card>
      )}

      <Divider />

      {/* Platform Tree (AgentTree) */}
      <PlatformTreeView
        onSendMessage={(name) => setComposerTarget(name)}
        onKillAgent={handleKillAgent}
        onSuspendAgent={handleSuspendAgent}
        onResumeAgent={handleResumeAgent}
        onKillContainer={handleKillContainer}
      />

      {/* Message Composer modal triggered from tree context menu */}
      <MessageComposerDialog
        open={Boolean(composerTarget)}
        onClose={() => setComposerTarget(null)}
        initialReceiver={composerTarget || undefined}
        onSent={(res) => notify(`Message sent to ${res.receivers.join(', ')}`, 'success')}
      />

      <NotificationSnackbar feedback={feedback} onClose={close} />
    </Box>
  );
}
