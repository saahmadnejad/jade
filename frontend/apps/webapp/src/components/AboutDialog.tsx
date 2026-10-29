import { useEffect, useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
  Divider,
  Chip,
  CircularProgress,
} from '@mui/material';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import { api, type VersionInfo, type PlatformInfo } from 'shared';

interface AboutDialogProps {
  open: boolean;
  onClose: () => void;
}

export default function AboutDialog({ open, onClose }: AboutDialogProps) {
  const [version, setVersion] = useState<VersionInfo | null>(null);
  const [platform, setPlatform] = useState<PlatformInfo | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (open) {
      setLoading(true);
      Promise.all([
        api.platform.version().catch(() => null),
        api.platform.getInfo().catch(() => null),
      ]).then(([v, p]) => {
        setVersion(v);
        setPlatform(p);
        setLoading(false);
      });
    }
  }, [open]);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <InfoOutlinedIcon color="primary" />
        About JADE Platform
      </DialogTitle>
      <DialogContent dividers>
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
            <CircularProgress />
          </Box>
        ) : (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <Box>
              <Typography variant="h6" color="primary">
                JADE — Java Agent Development Framework
              </Typography>
              <Typography variant="body2" color="text.secondary">
                FIPA-compliant Multi-Agent Platform &amp; Management Environment
              </Typography>
            </Box>

            <Divider />

            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                <Typography variant="body2" color="text.secondary">Version:</Typography>
                <Typography variant="body2" sx={{ fontWeight: 'bold' }}>{version?.version || 'Unknown'}</Typography>
              </Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                <Typography variant="body2" color="text.secondary">Revision:</Typography>
                <Typography variant="body2">{version?.revision || 'N/A'}</Typography>
              </Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                <Typography variant="body2" color="text.secondary">Build Date:</Typography>
                <Typography variant="body2">{version?.date || 'N/A'}</Typography>
              </Box>
            </Box>

            <Divider />

            {platform && (
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Typography variant="body2" color="text.secondary">Platform ID:</Typography>
                  <Chip size="small" label={platform.platformID} color="primary" variant="outlined" />
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="body2" color="text.secondary">Main Container:</Typography>
                  <Typography variant="body2">{platform.containerName}</Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="body2" color="text.secondary">AMS Agent:</Typography>
                  <Typography variant="body2">{platform.ams}</Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="body2" color="text.secondary">Default DF:</Typography>
                  <Typography variant="body2">{platform.defaultDF}</Typography>
                </Box>
              </Box>
            )}

            <Divider />

            <Typography variant="caption" color="text.secondary">
              Legacy Swing GUI source preserved in <code>io.donbee.jade.tools.*</code>.
              React Management Console powered by Vert.x REST API &amp; WebSocket bus.
            </Typography>
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} variant="contained">Close</Button>
      </DialogActions>
    </Dialog>
  );
}
