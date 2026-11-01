import { useEffect, useState, useRef } from 'react';
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
import { api, type VersionInfo, type PlatformInfo, type PlatformTreeResponse } from 'shared';

interface AboutDialogProps {
  open: boolean;
  onClose: () => void;
}

export default function AboutDialog({ open, onClose }: AboutDialogProps) {
  const [version, setVersion] = useState<VersionInfo | null>(null);
  const [platform, setPlatform] = useState<PlatformInfo | null>(null);
  const [tree, setTree] = useState<PlatformTreeResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const isMountedRef = useRef(true);

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  useEffect(() => {
    if (open) {
      setLoading(true);
      Promise.all([
        api.platform.version().catch(() => null),
        api.platform.getInfo().catch(() => null),
        api.platform.getTree().catch(() => null),
      ]).then(([v, p, t]) => {
        if (!isMountedRef.current) return;
        setVersion(v);
        setPlatform(p);
        setTree(t);
        setLoading(false);
      });
    }
  }, [open]);

  const activeContainersCount = tree?.containers?.length ?? 1;
  const activeAgentsCount = tree?.containers?.reduce((acc, c) => acc + (c.agents?.length || 0), 0) ?? 0;

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
              {version?.jvm && (
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="body2" color="text.secondary">JVM Platform:</Typography>
                  <Typography variant="body2">{version.jvm}</Typography>
                </Box>
              )}
              {version?.os && (
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="body2" color="text.secondary">Operating System:</Typography>
                  <Typography variant="body2">{version.os}</Typography>
                </Box>
              )}
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
                  <Typography variant="body2" color="text.secondary">Active Containers:</Typography>
                  <Typography variant="body2" sx={{ fontWeight: 'bold' }}>{activeContainersCount}</Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="body2" color="text.secondary">Active Agents:</Typography>
                  <Typography variant="body2" sx={{ fontWeight: 'bold' }}>{activeAgentsCount}</Typography>
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

            <Box>
              <Typography variant="caption" sx={{ fontWeight: 'bold', display: 'block', mb: 0.5 }}>
                Credits &amp; License
              </Typography>
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                Original JADE Platform: Telecom Italia Lab (TILAB), CSELT S.p.A., and Università di Parma.
              </Typography>
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                Modernized Architecture &amp; Vert.x REST API: Donbee &amp; DeepMind Agentic Harness.
              </Typography>
            </Box>
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} variant="contained">Close</Button>
      </DialogActions>
    </Dialog>
  );
}
