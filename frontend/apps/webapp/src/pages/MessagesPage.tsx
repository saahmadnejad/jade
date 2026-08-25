import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Box, Typography, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Paper, Chip, IconButton, Tooltip, TextField,
  Button, InputAdornment,
} from '@mui/material';
import PauseIcon from '@mui/icons-material/Pause';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import SearchIcon from '@mui/icons-material/Search';
import ClearAllIcon from '@mui/icons-material/ClearAll';
import CircleIcon from '@mui/icons-material/Circle';
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
  const pausedRef = useRef(paused);
  pausedRef.current = paused;

  useEffect(() => {
    let cancelled = false;

    const loadHistory = async () => {
      try {
        const data = await api.messages.recent({ limit: MAX_DISPLAYED_MESSAGES });
        if (!cancelled) {
          setMessages(data.messages);
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
          const next = [...prev, msg];
          return next.length > MAX_DISPLAYED_MESSAGES
            ? next.slice(next.length - MAX_DISPLAYED_MESSAGES)
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
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Box sx={{ mt: 1 }}>
        <Button size="small" disabled>
          Showing {visibleMessages.length} of {messages.length} captured messages
        </Button>
      </Box>
    </Box>
  );
}
