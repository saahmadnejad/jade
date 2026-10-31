import { useState, useMemo, useEffect } from 'react';
import {
  Box,
  Typography,
  Chip,
  Paper,
  Tooltip,
  Button,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import FilterListIcon from '@mui/icons-material/FilterList';
import ClearIcon from '@mui/icons-material/Clear';
import AutoModeIcon from '@mui/icons-material/AutoMode';
import type { AclMessageEvent } from 'shared';

interface SequenceDiagramViewProps {
  messages: AclMessageEvent[];
  onSelectMessage: (msg: AclMessageEvent) => void;
  availableAgents?: string[];
}

const performativeColors: Record<string, 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning'> = {
  INFORM: 'info',
  REQUEST: 'primary',
  AGREE: 'success',
  PROPOSE: 'secondary',
  REFUSE: 'error',
  FAILURE: 'error',
  'NOT-UNDERSTOOD': 'warning',
  'QUERY-IF': 'primary',
  'QUERY-REF': 'primary',
  SUBSCRIBE: 'info',
  CONFIRM: 'success',
  DISCONFIRM: 'warning',
};

export default function SequenceDiagramView({
  messages,
  onSelectMessage,
  availableAgents: initialAvailableAgents,
}: SequenceDiagramViewProps) {
  // Discover all unique agents from messages and their message counts
  const { allAgents, agentCounts } = useMemo(() => {
    const counts: Record<string, number> = {};
    const set = new Set<string>();

    if (initialAvailableAgents) {
      initialAvailableAgents.forEach((a) => set.add(a));
    }

    messages.forEach((m) => {
      if (m.sender) {
        set.add(m.sender);
        counts[m.sender] = (counts[m.sender] || 0) + 1;
      }
      if (m.receiver) {
        set.add(m.receiver);
        counts[m.receiver] = (counts[m.receiver] || 0) + 1;
      }
    });

    const sortedAgents = Array.from(set).sort((a, b) => (counts[b] || 0) - (counts[a] || 0));
    return { allAgents: sortedAgents, agentCounts: counts };
  }, [messages, initialAvailableAgents]);

  // Selected agents whose lifelines to display
  const [selectedAgents, setSelectedAgents] = useState<string[]>([]);
  const [messageLimit, setMessageLimit] = useState<number>(25);

  // Auto-select top 2-3 communicators on initial load if not selected
  useEffect(() => {
    if (selectedAgents.length === 0 && allAgents.length >= 2) {
      // Pick top 2 most active agents by default
      setSelectedAgents(allAgents.slice(0, Math.min(2, allAgents.length)));
    }
  }, [allAgents]);

  const toggleAgent = (agent: string) => {
    setSelectedAgents((prev) =>
      prev.includes(agent) ? prev.filter((a) => a !== agent) : [...prev, agent]
    );
  };

  const handleSelectTop2 = () => {
    if (allAgents.length >= 2) {
      setSelectedAgents(allAgents.slice(0, 2));
    }
  };

  const handleSelectTop3 = () => {
    if (allAgents.length >= 3) {
      setSelectedAgents(allAgents.slice(0, 3));
    } else {
      setSelectedAgents([...allAgents]);
    }
  };

  const handleSelectAll = () => {
    setSelectedAgents([...allAgents]);
  };

  const handleClearSelection = () => {
    setSelectedAgents([]);
  };

  // Filter messages for chronological sequence between selected agents
  const diagramMessages = useMemo(() => {
    if (selectedAgents.length === 0) return [];

    const selectedSet = new Set(selectedAgents);

    // Filter messages involving the selected agents
    const filtered = messages.filter((m) => {
      if (selectedAgents.length === 1) {
        return selectedSet.has(m.sender) || selectedSet.has(m.receiver);
      }
      return selectedSet.has(m.sender) && selectedSet.has(m.receiver);
    });

    // Invert to chronological order (oldest -> newest for top-down sequence diagram)
    const chronological = [...filtered].reverse();

    if (messageLimit > 0 && chronological.length > messageLimit) {
      return chronological.slice(chronological.length - messageLimit);
    }
    return chronological;
  }, [messages, selectedAgents, messageLimit]);

  if (allAgents.length === 0 || messages.length === 0) {
    return (
      <Box sx={{ p: 4, textAlign: 'center', color: 'text.secondary' }}>
        <Typography variant="body1">No message traffic captured yet.</Typography>
        <Typography variant="caption">Sent ACL messages will appear as visual sequence flow arrows.</Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      {/* Agent Selector Controls Card */}
      <Paper variant="outlined" sx={{ p: 2, bgcolor: 'background.paper', borderRadius: 2 }}>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 1 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <FilterListIcon color="primary" fontSize="small" />
              <Typography variant="subtitle2" sx={{ fontWeight: 'bold' }}>
                Select Agents for Sequence Lifelines:
              </Typography>
            </Box>

            <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', flexWrap: 'wrap' }}>
              <Button size="small" variant="outlined" onClick={handleSelectTop2} startIcon={<AutoModeIcon />}>
                Top 2 Agents
              </Button>
              {allAgents.length > 2 && (
                <Button size="small" variant="outlined" onClick={handleSelectTop3}>
                  Top 3 Agents
                </Button>
              )}
              <Button size="small" variant="text" onClick={handleSelectAll}>
                Select All
              </Button>
              <Button size="small" color="inherit" onClick={handleClearSelection} startIcon={<ClearIcon />}>
                Clear
              </Button>

              <FormControl size="small" sx={{ minWidth: 120 }}>
                <InputLabel>Show</InputLabel>
                <Select
                  value={messageLimit}
                  label="Show"
                  onChange={(e) => setMessageLimit(Number(e.target.value))}
                >
                  <MenuItem value={10}>Last 10 msgs</MenuItem>
                  <MenuItem value={25}>Last 25 msgs</MenuItem>
                  <MenuItem value={50}>Last 50 msgs</MenuItem>
                  <MenuItem value={100}>Last 100 msgs</MenuItem>
                  <MenuItem value={0}>All msgs</MenuItem>
                </Select>
              </FormControl>
            </Box>
          </Box>

          {/* Quick Clickable Agent Chips */}
          <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', alignItems: 'center' }}>
            <Typography variant="caption" color="text.secondary">
              Available Communicators:
            </Typography>
            {allAgents.map((agent) => {
              const isSelected = selectedAgents.includes(agent);
              const count = agentCounts[agent] || 0;
              return (
                <Chip
                  key={agent}
                  avatar={<SmartToyIcon />}
                  label={`${agent} (${count})`}
                  color={isSelected ? 'primary' : 'default'}
                  variant={isSelected ? 'filled' : 'outlined'}
                  size="small"
                  onClick={() => toggleAgent(agent)}
                  sx={{ cursor: 'pointer' }}
                />
              );
            })}
          </Box>
        </Box>
      </Paper>

      {/* Main Diagram Area */}
      {selectedAgents.length < 2 ? (
        <Paper
          variant="outlined"
          sx={{
            p: 5,
            textAlign: 'center',
            bgcolor: 'background.default',
            borderRadius: 2,
            borderStyle: 'dashed',
          }}
        >
          <SmartToyIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 1, opacity: 0.7 }} />
          <Typography variant="h6" color="text.secondary">
            {selectedAgents.length === 1
              ? `Selected: ${selectedAgents[0]}. Please pick at least one more agent to draw conversations.`
              : 'Please choose 2 or more agents above to view their sequence diagram.'}
          </Typography>
          <Typography variant="body2" color="text.disabled" sx={{ mt: 1 }}>
            Click on any communicator chip above or click &quot;Top 2 Agents&quot; for instant pairwise focus.
          </Typography>
        </Paper>
      ) : diagramMessages.length === 0 ? (
        <Paper variant="outlined" sx={{ p: 4, textAlign: 'center', bgcolor: 'background.default', borderRadius: 2 }}>
          <Typography variant="body1" color="text.secondary">
            No messages exchanged directly between [{selectedAgents.join(', ')}].
          </Typography>
          <Typography variant="caption" color="text.disabled">
            Try choosing different agents or send a message using the &quot;Compose Message&quot; button.
          </Typography>
        </Paper>
      ) : (
        <Box sx={{ overflowX: 'auto', p: 2, bgcolor: 'background.default', borderRadius: 2 }}>
          {/* Lifeline Headers */}
          <Box
            sx={{
              display: 'flex',
              minWidth: Math.max(selectedAgents.length * 240, 500),
              mb: 3,
            }}
          >
            {/* Margin column for sequence badges & timestamps */}
            <Box sx={{ width: 80, flexShrink: 0 }} />

            {selectedAgents.map((agent) => (
              <Box
                key={agent}
                sx={{
                  flex: 1,
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  px: 1,
                }}
              >
                <Paper
                  elevation={3}
                  sx={{
                    p: 1.2,
                    display: 'flex',
                    alignItems: 'center',
                    gap: 1,
                    borderRadius: 2,
                    bgcolor: 'background.paper',
                    border: '2px solid',
                    borderColor: 'primary.main',
                    maxWidth: 220,
                    width: '100%',
                    justifyContent: 'center',
                  }}
                >
                  <SmartToyIcon fontSize="small" color="primary" />
                  <Typography variant="body2" noWrap sx={{ fontWeight: 'bold' }} title={agent}>
                    {agent}
                  </Typography>
                </Paper>
                {/* Vertical Lifeline Line Start */}
                <Box sx={{ width: 2, height: 16, bgcolor: 'primary.main', opacity: 0.6 }} />
              </Box>
            ))}
          </Box>

          {/* Chronological Message Flow Rows */}
          <Box
            sx={{
              display: 'flex',
              flexDirection: 'column',
              gap: 2,
              minWidth: Math.max(selectedAgents.length * 240, 500),
              position: 'relative',
            }}
          >
            {diagramMessages.map((msg, index) => {
              const senderIdx = selectedAgents.indexOf(msg.sender);
              const receiverIdx = selectedAgents.indexOf(msg.receiver);

              // If an agent is not in selected set
              if (senderIdx === -1 || receiverIdx === -1) {
                return null;
              }

              const leftIdx = Math.min(senderIdx, receiverIdx);
              const rightIdx = Math.max(senderIdx, receiverIdx);
              const isLeftToRight = senderIdx <= receiverIdx;

              const totalCols = selectedAgents.length;
              const leftPercent = (leftIdx / totalCols) * 100 + 100 / (2 * totalCols);
              const rightPercent = (rightIdx / totalCols) * 100 + 100 / (2 * totalCols);
              const widthPercent = rightPercent - leftPercent;

              const timeStr = msg.timestamp
                ? new Date(msg.timestamp).toLocaleTimeString()
                : '';

              const contentPreview = msg.content
                ? msg.content.length > 50
                  ? msg.content.substring(0, 50) + '...'
                  : msg.content
                : '(empty content)';

              return (
                <Tooltip
                  key={msg.id || index}
                  title={`Click to inspect #${msg.id} | ${msg.sender} → ${msg.receiver} (${msg.performative})`}
                  arrow
                >
                  <Box
                    onClick={() => onSelectMessage(msg)}
                    sx={{
                      position: 'relative',
                      height: 56,
                      display: 'flex',
                      alignItems: 'center',
                      cursor: 'pointer',
                      borderRadius: 1,
                      px: 0.5,
                      '&:hover': { bgcolor: 'action.hover' },
                      transition: 'background-color 0.15s ease',
                    }}
                  >
                    {/* Timestamp & Step Badge Margin */}
                    <Box sx={{ width: 80, flexShrink: 0, display: 'flex', flexDirection: 'column' }}>
                      <Chip
                        label={`#${index + 1}`}
                        size="small"
                        sx={{ height: 20, fontSize: '0.7rem', width: 44, mb: 0.2 }}
                      />
                      <Typography variant="caption" color="text.secondary" sx={{ fontSize: '0.65rem' }}>
                        {timeStr}
                      </Typography>
                    </Box>

                    {/* Lifeline Guides for each column */}
                    <Box sx={{ position: 'relative', flexGrow: 1, height: '100%' }}>
                      {selectedAgents.map((ag) => (
                        <Box
                          key={ag}
                          sx={{
                            position: 'absolute',
                            left: `${(selectedAgents.indexOf(ag) / totalCols) * 100 + 100 / (2 * totalCols)}%`,
                            top: 0,
                            bottom: 0,
                            width: 1,
                            borderLeft: '1px dashed rgba(255,255,255,0.15)',
                            zIndex: 0,
                          }}
                        />
                      ))}

                      {/* Directed Message Arrow */}
                      {widthPercent > 0 ? (
                        <Box
                          sx={{
                            position: 'absolute',
                            left: `${leftPercent}%`,
                            width: `${widthPercent}%`,
                            top: '50%',
                            transform: 'translateY(-50%)',
                            display: 'flex',
                            flexDirection: 'column',
                            alignItems: 'center',
                            zIndex: 1,
                          }}
                        >
                          {/* Performative Chip */}
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mb: -0.8, zIndex: 2 }}>
                            <Chip
                              size="small"
                              label={msg.performative.toUpperCase()}
                              color={performativeColors[msg.performative.toUpperCase()] || 'default'}
                              sx={{
                                fontSize: '0.72rem',
                                fontWeight: 'bold',
                                height: 22,
                                boxShadow: 1,
                              }}
                            />
                          </Box>

                          {/* Line and Arrowhead */}
                          <Box sx={{ width: '100%', position: 'relative', height: 16, display: 'flex', alignItems: 'center' }}>
                            <Box
                              sx={{
                                width: '100%',
                                height: 2,
                                bgcolor: 'primary.light',
                              }}
                            />
                            <ArrowForwardIcon
                              fontSize="small"
                              color="primary"
                              sx={{
                                position: 'absolute',
                                right: isLeftToRight ? -6 : undefined,
                                left: !isLeftToRight ? -6 : undefined,
                                transform: !isLeftToRight ? 'rotate(180deg)' : undefined,
                              }}
                            />
                          </Box>

                          {/* Content Preview Snippet */}
                          <Typography
                            variant="caption"
                            noWrap
                            sx={{
                              maxWidth: '90%',
                              fontFamily: 'monospace',
                              fontSize: '0.7rem',
                              color: 'text.secondary',
                              mt: -0.5,
                            }}
                          >
                            {contentPreview}
                          </Typography>
                        </Box>
                      ) : (
                        // Self-message Loopback
                        <Box
                          sx={{
                            position: 'absolute',
                            left: `${leftPercent}%`,
                            top: '50%',
                            transform: 'translateY(-50%)',
                            display: 'flex',
                            alignItems: 'center',
                            gap: 1,
                            zIndex: 1,
                            pl: 1,
                          }}
                        >
                          <Chip
                            size="small"
                            label={`Self: ${msg.performative.toUpperCase()}`}
                            color="default"
                            sx={{ fontSize: '0.7rem', height: 22 }}
                          />
                          <Typography
                            variant="caption"
                            noWrap
                            sx={{ fontFamily: 'monospace', fontSize: '0.7rem', color: 'text.secondary', maxWidth: 180 }}
                          >
                            {contentPreview}
                          </Typography>
                        </Box>
                      )}
                    </Box>
                  </Box>
                </Tooltip>
              );
            })}
          </Box>
        </Box>
      )}
    </Box>
  );
}
