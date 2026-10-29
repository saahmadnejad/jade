import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Box, Typography, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Paper, Chip, IconButton, Tooltip, TextField,
  Button, InputAdornment, Dialog, DialogTitle, DialogContent, DialogActions,
  ToggleButtonGroup, ToggleButton, FormControl, InputLabel, Select, MenuItem,
} from '@mui/material';
import PauseIcon from '@mui/icons-material/Pause';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import SearchIcon from '@mui/icons-material/Search';
import ClearAllIcon from '@mui/icons-material/ClearAll';
import CircleIcon from '@mui/icons-material/Circle';
import VisibilityIcon from '@mui/icons-material/Visibility';
import TableRowsIcon from '@mui/icons-material/TableRows';
import TimelineIcon from '@mui/icons-material/Timeline';
import FileDownloadIcon from '@mui/icons-material/FileDownload';
import SendIcon from '@mui/icons-material/Send';
import ReplyIcon from '@mui/icons-material/Reply';
import { api, subscribeMessagesStream, type AclMessageEvent, type MessagesStreamStatus } from 'shared';
import PageHeader from '../components/PageHeader';
import EmptyState from '../components/EmptyState';
import SequenceDiagramView from '../components/SequenceDiagramView';
import MessageComposerDialog, { FIPA_PERFORMATIVES } from '../components/MessageComposerDialog';
import NotificationSnackbar, { useFeedback } from '../components/NotificationSnackbar';

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
  const [perfFilter, setPerfFilter] = useState('ALL');
  const [viewMode, setViewMode] = useState<'table' | 'sequence'>('table');
  const [status, setStatus] = useState<MessagesStreamStatus>('connecting');
  const [detailsMessage, setDetailsMessage] = useState<AclMessageEvent | null>(null);
  const [composerOpen, setComposerOpen] = useState(false);
  const [replyTarget, setReplyTarget] = useState<AclMessageEvent | null>(null);
  const { feedback, notify, close } = useFeedback();

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
    return messages.filter((m) => {
      const matchesSearch =
        !needle ||
        m.sender.toLowerCase().includes(needle) ||
        m.receiver.toLowerCase().includes(needle) ||
        m.content.toLowerCase().includes(needle);

      const matchesPerf =
        perfFilter === 'ALL' ||
        m.performative.toUpperCase() === perfFilter.toUpperCase();

      return matchesSearch && matchesPerf;
    });
  }, [messages, filter, perfFilter]);

  const clearAll = useCallback(() => {
    setMessages([]);
  }, []);

  const exportJson = useCallback(() => {
    const blob = new Blob([JSON.stringify(visibleMessages, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `jade-messages-${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
    notify('Exported messages to JSON', 'success');
  }, [visibleMessages, notify]);

  /** Open the modal with the row data, then upgrade to the full message. */
  const openDetails = useCallback((row: AclMessageEvent) => {
    setDetailsMessage(row);
    api.messages.getById(row.id)
      .then((full) => setDetailsMessage((current) =>
        current && current.id === full.id ? full : current))
      .catch((e) => console.error('Failed to fetch full message', e));
  }, []);

  const handleReply = (m: AclMessageEvent) => {
    setReplyTarget(m);
    setComposerOpen(true);
  };

  return (
    <Box>
      <PageHeader
        title="Messages"
        actions={
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Button
              variant="contained"
              size="small"
              startIcon={<SendIcon />}
              onClick={() => {
                setReplyTarget(null);
                setComposerOpen(true);
              }}
            >
              Compose Message
            </Button>
            <Tooltip title="Export current messages to JSON file">
              <span>
                <Button
                  variant="outlined"
                  size="small"
                  startIcon={<FileDownloadIcon />}
                  onClick={exportJson}
                  disabled={visibleMessages.length === 0}
                >
                  Export JSON
                </Button>
              </span>
            </Tooltip>
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

      {/* Filter and View Mode Controls */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2, flexWrap: 'wrap', gap: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, flexGrow: 1, maxWidth: 640 }}>
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

          <FormControl size="small" sx={{ minWidth: 180 }}>
            <InputLabel>Performative</InputLabel>
            <Select
              value={perfFilter}
              label="Performative"
              onChange={(e) => setPerfFilter(e.target.value)}
            >
              <MenuItem value="ALL">All Performatives</MenuItem>
              {FIPA_PERFORMATIVES.map((p) => (
                <MenuItem key={p} value={p}>{p}</MenuItem>
              ))}
            </Select>
          </FormControl>
        </Box>

        <ToggleButtonGroup
          value={viewMode}
          exclusive
          size="small"
          onChange={(_, val) => { if (val) setViewMode(val); }}
        >
          <ToggleButton value="table">
            <TableRowsIcon fontSize="small" sx={{ mr: 0.5 }} />
            Table View
          </ToggleButton>
          <ToggleButton value="sequence">
            <TimelineIcon fontSize="small" sx={{ mr: 0.5 }} />
            Sequence Diagram
          </ToggleButton>
        </ToggleButtonGroup>
      </Box>

      {paused && (
        <Typography variant="body2" color="warning.main" gutterBottom>
          Stream paused - incoming messages are not shown until resumed.
        </Typography>
      )}

      {visibleMessages.length === 0 ? (
        <EmptyState message="No messages captured yet. Deploy agents and watch the traffic flow." />
      ) : viewMode === 'sequence' ? (
        <SequenceDiagramView
          messages={visibleMessages}
          onSelectMessage={openDetails}
        />
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
                  <TableCell>{m.protocol || '—'}</TableCell>
                  <TableCell>{m.ontology || '—'}</TableCell>
                  <TableCell sx={{ maxWidth: 420, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {m.content}
                  </TableCell>
                  <TableCell align="right">
                    <Tooltip title="Reply to sender">
                      <IconButton size="small" color="primary" onClick={() => handleReply(m)}>
                        <ReplyIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
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

      {/* Message Details Modal */}
      <Dialog
        open={detailsMessage !== null}
        onClose={() => setDetailsMessage(null)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Chip label={`#${detailsMessage?.id}`} size="small" />
            <Chip label={detailsMessage?.performative ?? ''} size="small" color="primary" variant="outlined" />
            <Typography variant="subtitle1" sx={{ fontWeight: 'bold' }}>
              {detailsMessage?.sender} → {detailsMessage?.receiver}
            </Typography>
          </Box>
          <Button
            size="small"
            startIcon={<ReplyIcon />}
            variant="contained"
            onClick={() => {
              const current = detailsMessage;
              setDetailsMessage(null);
              if (current) handleReply(current);
            }}
          >
            Reply
          </Button>
        </DialogTitle>
        <DialogContent dividers>
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

      {/* Message Composer Dialog */}
      <MessageComposerDialog
        open={composerOpen}
        onClose={() => {
          setComposerOpen(false);
          setReplyTarget(null);
        }}
        initialReceiver={replyTarget?.sender}
        initialInReplyTo={replyTarget?.id}
        initialProtocol={replyTarget?.protocol}
        initialOntology={replyTarget?.ontology}
        initialPerformative={replyTarget ? 'INFORM' : 'INFORM'}
        onSent={(res) => notify(`Message sent to ${res.receivers.join(', ')}`, 'success')}
      />

      <Box sx={{ mt: 1 }}>
        <Button size="small" disabled>
          Showing {visibleMessages.length} of {messages.length} captured messages
        </Button>
      </Box>

      <NotificationSnackbar feedback={feedback} onClose={close} />
    </Box>
  );
}
