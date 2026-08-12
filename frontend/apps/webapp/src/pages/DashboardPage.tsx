import { useEffect, useState } from 'react';
import { Box, Typography, Card, CardContent, Chip, CircularProgress } from '@mui/material';
import { api, type HealthStatus, type PlatformInfo } from 'shared';

export default function DashboardPage() {
  const [health, setHealth] = useState<HealthStatus | null>(null);
  const [platform, setPlatform] = useState<PlatformInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchData = async () => {
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
    fetchData();
  }, []);

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="60vh">
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Typography color="error">{error}</Typography>
    );
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Platform Dashboard
      </Typography>

      <Box display="flex" gap={2} mb={2}>
        <Chip
          label={health?.status === 'ok' ? 'Healthy' : 'Unhealthy'}
          color={health?.status === 'ok' ? 'success' : 'error'}
        />
      </Box>

      {platform && (
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
    </Box>
  );
}
