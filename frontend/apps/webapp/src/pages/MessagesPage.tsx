import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Box, Typography, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Paper, Chip, IconButton, Tooltip, TextField,
  Button, InputAdornment, Dialog, DialogTitle, DialogContent, DialogActions,
} from '@mui/material';
import PauseIcon from '@mui/icons-material/Pause';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import SearchIcon from '@mui/icons-material/Search';
import ClearAllIcon from '@mui/icons-material/ClearAll';
import CircleIcon from '@mui/icons-material/Circle';
import VisibilityIcon from '@mui/icons-material/Visibility';
import { api, subscribeMessagesStream, type AclMessageEvent, type MessagesStreamStatus } from 'shared';
import PageHeader from '../components/PageHeader';
import EmptyState from '../components/EmptyState';

const MAX_DISPLAYED_MESSAGES = 500;

const statusColor: Record<MessagesStreamStatus, 'success' | 'warning' | 'error' | 'default'> = {
  open: 'success',
  connecting: 'warning',
  error: 'error',
  closed: 'default',
};

const formatTime = (isoTimestamp: string): string => {
  try {
    return new Date(isoTimestamp).toLocaleTimeString();
  } catch {
    return isoTimestamp;
  }
};

export default function MessagesPage() {
  const [messages, setMessages] = useState<AclMessageEvent[]>([]);
  const [paused, setPaused] = useState(false);
  const [filter, setFilter] = useState('');
  const [status, setStatus] = useState<MessagesStreamStatus>('connecting');
  const [detailsMessage, setDetailsMessage] = useState<AclMessageEvent | null>(null);
  const pausedRef = useRef(paused);
  pausedRef.current = paused;

  useEffect(() => {
    let cancelled = false;

    const loadHistory = async () => {
      try {
        const data = await api.messages.recent({ limit: MAX_DISPLAYED_MESSAGES });
        if (!cancelled) {
          // API returns oldest-first; show newest at the top.
          setMessages([...data.messages].reverse());
        }
      } catch (e) {
        console.error('Failed to fetch recent messages', e);
      }
    };

    loadHistory();

    const unsubscribe = subscribeMessagesStream({
      onMessage: (msg) => {
        if (pausedRef.current) return;
        setMessages((prev) => {
          const next = [msg, ...prev];
          return next.length > MAX_DISPLAYED_MESSAGES
            ? next.slice(0, MAX_DISPLAYED_MESSAGES)
            : next;
        });
      },
      onStatusChange: (s) => {
        if (!cancelled) setStatus(s);
      },
    });

    return () => {
      cancelled = true;
      unsubscribe();
    };
  }, []);

  const visibleMessages = useMemo(() => {
    const needle = filter.trim().toLowerCase();
    if (!needle) return messages;
    return messages.filter(
      (m) =>
        m.sender.toLowerCase().includes(needle) ||
        m.receiver.toLowerCase().includes(needle) ||
        m.content.toLowerCase().includes(needle),
    );
  }, [messages, filter]);

  const clearAll = useCallback(() => {
    setMessages([]);
  }, []);

  /** Open the modal with the row data, then upgrade to the full message. */
  const openDetails = useCallback((row: AclMessageEvent) => {
    setDetailsMessage(row);
    api.messages.getById(row.id)
      .then((full) => setDetailsMessage((current) =>
        current && current.id === full.id ? full : current))
      .catch((e) => console.error('Failed to fetch full message', e));
  }, []);

  return (
    <Box>
      <PageHeader
        title="Messages"
        actions={
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Chip
              icon={<CircleIcon fontSize="small" />}
              label={status}
              color={statusColor[status]}
              size="small"
            />
            <Tooltip title={paused ? 'Resume live stream' : 'Pause live stream'}>
              <IconButton onClick={() => setPaused((p) => !p)} color={paused ? 'warning' : 'primary'}>
                {paused ? <PlayArrowIcon /> : <PauseIcon />}
              </IconButton>
            </Tooltip>
            <Tooltip title="Clear list">
              <IconButton onClick={clearAll}>
                <ClearAllIcon />
              </IconButton>
            </Tooltip>
          </Box>
        }
      />

      <Box sx={{ mb: 2, maxWidth: 480 }}>
        <TextField
          size="small"
          fullWidth
          placeholder="Filter by agent or content..."
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          slotProps={{
            input: {
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon fontSize="small" />
                </InputAdornment>
              ),
            },
          }}
        />
      </Box>

      {paused && (
        <Typography variant="body2" color="warning.main" gutterBottom>
          Stream paused - incoming messages are not shown until resumed.
        </Typography>
      )}

      {visibleMessages.length === 0 ? (
        <EmptyState message="No messages captured yet. Deploy agents and watch the traffic flow." />
      ) : (
        <TableContainer component={Paper} sx={{ maxHeight: '65vh' }}>
          <Table size="small" stickyHeader>
            <TableHead>
              <TableRow>
                <TableCell>Time</TableCell>
                <TableCell>From</TableCell>
                <TableCell>To</TableCell>
                <TableCell>Performative</TableCell>
                <TableCell>Protocol</TableCell>
                <TableCell>Ontology</TableCell>
                <TableCell>Content</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {visibleMessages.map((m) => (
                <TableRow key={m.id} hover>
                  <TableCell sx={{ whiteSpace: 'nowrap' }}>{formatTime(m.timestamp)}</TableCell>
                  <TableCell>{m.sender}</TableCell>
                  <TableCell>{m.receiver}</TableCell>
                  <TableCell>
                    <Chip label={m.performative} size="small" variant="outlined" />
                  </TableCell>
                  <TableCell>{m.protocol}</TableCell>
                  <TableCell>{m.ontology}</TableCell>
                  <TableCell sx={{ maxWidth: 420, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {m.content}
                  </TableCell>
                  <TableCell align="right">
                    <Tooltip title="View message details">
                      <IconButton size="small" onClick={() => openDetails(m)}>
                        <VisibilityIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Dialog
        open={detailsMessage !== null}
        onClose={() => setDetailsMessage(null)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Chip label={`#${detailsMessage?.id}`} size="small" />
          <Chip label={detailsMessage?.performative ?? ''} size="small" color="primary" variant="outlined" />
          {detailsMessage?.sender} → {detailsMessage?.receiver}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'grid', gridTemplateColumns: '140px 1fr', gap: 1, mb: 2 }}>
            <Typography variant="body2" color="text.secondary">Time</Typography>
            <Typography variant="body2">{detailsMessage ? new Date(detailsMessage.timestamp).toLocaleString() : ''}</Typography>
            <Typography variant="body2" color="text.secondary">Protocol</Typography>
            <Typography variant="body2">{detailsMessage?.protocol || '—'}</Typography>
            <Typography variant="body2" color="text.secondary">Ontology</Typography>
            <Typography variant="body2">{detailsMessage?.ontology || '—'}</Typography>
          </Box>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
            <Typography variant="subtitle2">Content</Typography>
            <Typography variant="caption" color="text.secondary">
              {detailsMessage?.content.length.toLocaleString()} chars · full message
            </Typography>
          </Box>
          <Paper variant="outlined" sx={{ p: 1.5, maxHeight: 380, overflowY: 'auto' }}>
            <Box
              component="pre"
              sx={{ m: 0, whiteSpace: 'pre-wrap', wordBreak: 'break-word', fontFamily: 'monospace', fontSize: 13 }}
            >
              {detailsMessage?.content}
            </Box>
          </Paper>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDetailsMessage(null)}>Close</Button>
        </DialogActions>
      </Dialog>

      <Box sx={{ mt: 1 }}>
        <Button size="small" disabled>
          Showing {visibleMessages.length} of {messages.length} captured messages
        </Button>
      </Box>
    </Box>
  );
}
