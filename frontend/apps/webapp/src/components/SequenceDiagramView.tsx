import { useMemo } from 'react';
import {
  Box,
  Typography,
  Chip,
  Paper,
  Tooltip,
} from '@mui/material';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import type { AclMessageEvent } from 'shared';

interface SequenceDiagramViewProps {
  messages: AclMessageEvent[];
  onSelectMessage: (msg: AclMessageEvent) => void;
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
};

export default function SequenceDiagramView({ messages, onSelectMessage }: SequenceDiagramViewProps) {
  // Extract distinct agents as lifelines
  const agents = useMemo(() => {
    const set = new Set<string>();
    messages.forEach((m) => {
      if (m.sender) set.add(m.sender);
      if (m.receiver) set.add(m.receiver);
    });
    return Array.from(set).sort();
  }, [messages]);

  if (agents.length === 0 || messages.length === 0) {
    return (
      <Box sx={{ p: 4, textAlign: 'center', color: 'text.secondary' }}>
        <Typography variant="body1">No message traffic captured yet.</Typography>
        <Typography variant="caption">Sent ACL messages will appear as visual sequence flow arrows.</Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ overflowX: 'auto', p: 2, bgcolor: 'background.default', borderRadius: 2 }}>
      {/* Agent Lifeline Headers */}
      <Box sx={{ display: 'flex', minWidth: agents.length * 180, mb: 3 }}>
        {agents.map((agent) => (
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
              elevation={2}
              sx={{
                p: 1,
                display: 'flex',
                alignItems: 'center',
                gap: 0.5,
                borderRadius: 2,
                bgcolor: 'background.paper',
                border: '1px solid',
                borderColor: 'primary.main',
                maxWidth: 170,
                width: '100%',
                justifyContent: 'center',
              }}
            >
              <SmartToyIcon fontSize="small" color="primary" />
              <Typography variant="caption" noWrap sx={{ fontWeight: 'bold' }} title={agent}>
                {agent}
              </Typography>
            </Paper>
            {/* Vertical Lifeline Line */}
            <Box sx={{ width: 2, height: 16, bgcolor: 'primary.main', opacity: 0.5 }} />
          </Box>
        ))}
      </Box>

      {/* Message Sequence Rows */}
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, minWidth: agents.length * 180 }}>
        {messages.map((msg) => {
          const senderIdx = agents.indexOf(msg.sender);
          const receiverIdx = agents.indexOf(msg.receiver);

          // Fallback if one agent is outside current set
          if (senderIdx === -1 || receiverIdx === -1) {
            return null;
          }

          const leftIdx = Math.min(senderIdx, receiverIdx);
          const rightIdx = Math.max(senderIdx, receiverIdx);
          const isLeftToRight = senderIdx <= receiverIdx;

          const totalCols = agents.length;
          const leftPercent = (leftIdx / totalCols) * 100 + 100 / (2 * totalCols);
          const rightPercent = (rightIdx / totalCols) * 100 + 100 / (2 * totalCols);
          const widthPercent = rightPercent - leftPercent;

          return (
            <Tooltip
              key={msg.id}
              title={`Click to view full message | Content: ${msg.content || '(empty)'}`}
              arrow
            >
              <Box
                onClick={() => onSelectMessage(msg)}
                sx={{
                  position: 'relative',
                  height: 44,
                  display: 'flex',
                  alignItems: 'center',
                  cursor: 'pointer',
                  borderRadius: 1,
                  '&:hover': { bgcolor: 'action.hover' },
                }}
              >
                {/* Lifeline Guides for each column */}
                {agents.map((ag) => (
                  <Box
                    key={ag}
                    sx={{
                      position: 'absolute',
                      left: `${(agents.indexOf(ag) / totalCols) * 100 + 100 / (2 * totalCols)}%`,
                      top: 0,
                      bottom: 0,
                      width: 1,
                      borderLeft: '1px dashed rgba(255,255,255,0.1)',
                      zIndex: 0,
                    }}
                  />
                ))}

                {/* Arrow connecting sender to receiver */}
                {widthPercent > 0 ? (
                  <Box
                    sx={{
                      position: 'absolute',
                      left: `${leftPercent}%`,
                      width: `${widthPercent}%`,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      zIndex: 1,
                    }}
                  >
                    <Box
                      sx={{
                        width: '100%',
                        height: 2,
                        bgcolor: 'primary.light',
                        position: 'relative',
                      }}
                    />
                    <Chip
                      size="small"
                      label={msg.performative}
                      color={performativeColors[msg.performative] || 'default'}
                      sx={{
                        position: 'absolute',
                        fontSize: '0.7rem',
                        height: 20,
                        cursor: 'pointer',
                      }}
                    />
                    <ArrowForwardIcon
                      fontSize="small"
                      color="primary"
                      sx={{
                        position: 'absolute',
                        right: isLeftToRight ? -8 : undefined,
                        left: !isLeftToRight ? -8 : undefined,
                        transform: !isLeftToRight ? 'rotate(180deg)' : undefined,
                      }}
                    />
                  </Box>
                ) : (
                  // Loopback message to self
                  <Box
                    sx={{
                      position: 'absolute',
                      left: `${leftPercent}%`,
                      display: 'flex',
                      alignItems: 'center',
                      gap: 0.5,
                      zIndex: 1,
                    }}
                  >
                    <Chip
                      size="small"
                      label={`Self: ${msg.performative}`}
                      color="default"
                      sx={{ fontSize: '0.7rem', height: 20 }}
                    />
                  </Box>
                )}
              </Box>
            </Tooltip>
          );
        })}
      </Box>
    </Box>
  );
}
