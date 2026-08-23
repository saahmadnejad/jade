import { useEffect, useState } from 'react';
import { Box, Card, CardContent, Chip, Alert, Button, Typography } from '@mui/material';
import { api, type HealthStatus, type PlatformInfo } from 'shared';
import PageHeader from '../components/PageHeader';
import TopProgressBar from '../components/TopProgressBar';
import NotificationSnackbar, { useFeedback } from '../components/NotificationSnackbar';

export default function DashboardPage() {
  const [health, setHealth] = useState<HealthStatus | null>(null);
  const [platform, setPlatform] = useState<PlatformInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
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
    } catch (e) {
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

  if (loading) {
    return <TopProgressBar />;
  }

  return (
    <Box>
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
        <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>
      )}

      {platform && !error && (
        <Card>
          <CardContent>
            <Typography variant="h6">Platform Info</Typography>
            <Box mt={1}>
              <Typography><strong>Platform ID:</strong> {platform.platformID}</Typography>
              <Typography><strong>Container:</strong> {platform.containerName}</Typography>
              <Typography><strong>Main Container:</strong> {platform.isMain ? 'Yes' : 'No'}</Typography>
              <Typography><strong>AMS:</strong> {platform.ams}</Typography>
              <Typography><strong>Default DF:</strong> {platform.defaultDF}</Typography>
            </Box>
          </CardContent>
        </Card>
      )}

      <NotificationSnackbar feedback={feedback} onClose={close} />
    </Box>
  );
}
