import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Button,
  Card,
  CardHeader,
  CardContent,
  CardActions,
  Typography,
  Chip,
  CircularProgress,
  Divider,
  Autocomplete,
  TextField,
  Tooltip,
} from '@mui/material';
import TimelineIcon from '@mui/icons-material/Timeline';
import SendIcon from '@mui/icons-material/Send';
import DnsIcon from '@mui/icons-material/Dns';
import BugReportIcon from '@mui/icons-material/BugReport';
import AssessmentIcon from '@mui/icons-material/Assessment';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import DesktopWindowsIcon from '@mui/icons-material/DesktopWindows';
import { api } from 'shared';
import PageHeader from '../components/PageHeader';
import MessageComposerDialog from '../components/MessageComposerDialog';
import NotificationSnackbar, { useFeedback } from '../components/NotificationSnackbar';

interface ToolDef {
  id: string;
  title: string;
  legacyName: string;
  icon: React.ReactNode;
  reactStatus: string;
  description: string;
  reactActionLabel: string;
  onReactAction: () => void;
}

export default function ToolsPage() {
  const navigate = useNavigate();
  const [containers, setContainers] = useState<string[]>(['Main-Container']);
  const [selectedContainer, setSelectedContainer] = useState('Main-Container');
  const [launching, setLaunching] = useState<string | null>(null);
  const [composerOpen, setComposerOpen] = useState(false);
  const { feedback, notify, close } = useFeedback();

  useEffect(() => {
    try {
      const p = api.containers?.list?.();
      if (p && typeof p.then === 'function') {
        p.then((res: any) => {
          if (res?.containers) {
            const names = res.containers.map((c: any) => c.name);
            if (names.length > 0) {
              setContainers(names);
              setSelectedContainer(names[0]);
            }
          }
        }).catch(() => {});
      }
    } catch {
      // Ignore if containers listing fails
    }
  }, []);

  const handleLegacyLaunch = async (tool: string) => {
    setLaunching(tool);
    try {
      await api.tools.start(tool, { container: selectedContainer || 'Main-Container' });
      notify(`${tool} started`, 'success');
    } catch (e: any) {
      notify(`Failed to launch ${tool} - ${e.message || 'unknown error'}`, 'error');
    } finally {
      setLaunching(null);
    }
  };

  const toolDefinitions: ToolDef[] = [
    {
      id: 'dummy',
      title: 'ACL Message Composer & Tester',
      legacyName: 'dummy',
      icon: <SendIcon color="primary" />,
      reactStatus: 'React UI Integrated',
      description: 'Interactive FIPA ACL composer replacing DummyAgent & TestAgent. Supports 22 performatives, presets, protocol slots, and live receiver autocompletes.',
      reactActionLabel: 'Open Message Composer',
      onReactAction: () => setComposerOpen(true),
    },
    {
      id: 'sniffer',
      title: 'Message Sniffer & Sequence Visualizer',
      legacyName: 'sniffer',
      icon: <TimelineIcon color="primary" />,
      reactStatus: 'React UI Integrated',
      description: 'Live ACL message interception replacing the Swing Sniffer canvas. Renders agent lifelines, sequence arrows, performative filtering, and JSON trace exports.',
      reactActionLabel: 'Open React Sniffer & Flow',
      onReactAction: () => navigate('/messages'),
    },
    {
      id: 'df-gui',
      title: 'Directory Facilitator (DF Yellow Pages)',
      legacyName: 'df-gui',
      icon: <DnsIcon color="primary" />,
      reactStatus: 'React UI Integrated',
      description: 'Yellow pages service browser replacing DF GUI. Manage agent service descriptions, search with depth constraints, and federate parent/child DFs.',
      reactActionLabel: 'Open DF Yellow Pages',
      onReactAction: () => navigate('/df'),
    },
    {
      id: 'introspector',
      title: 'Agent Introspector & Debugger',
      legacyName: 'introspector',
      icon: <BugReportIcon color="warning" />,
      reactStatus: 'React UI Integrated',
      description: 'Inspect agent lifecycle states, ownership, and communication endpoints across platform containers directly via the Platform Tree and Agents console.',
      reactActionLabel: 'Inspect Agents Console',
      onReactAction: () => navigate('/agents'),
    },
    {
      id: 'logger',
      title: 'Log Manager & Container Diagnostics',
      legacyName: 'logger',
      icon: <AssessmentIcon color="info" />,
      reactStatus: 'React UI Integrated',
      description: 'Manage platform container health, MTP message transport protocols, and container lifecycles in the modern React management console.',
      reactActionLabel: 'Manage Containers & MTPs',
      onReactAction: () => navigate('/containers'),
    },
  ];

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
      <PageHeader
        title="Tools"
        actions={
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Autocomplete
              freeSolo
              size="small"
              sx={{ width: 220 }}
              options={containers}
              value={selectedContainer}
              onChange={(_, val) => setSelectedContainer(val || 'Main-Container')}
              onInputChange={(_, val) => setSelectedContainer(val)}
              renderInput={(params) => (
                <TextField {...params} label="Target Container" size="small" />
              )}
            />
          </Box>
        }
      />

      <Typography variant="body2" color="text.secondary">
        All JADE diagnostic and testing tools are fully ported to the React UI. You can run all operations directly in the browser, or launch the legacy Swing tool windows as a source of truth for side-by-side comparison.
      </Typography>

      {/* Grid of Tool Cards */}
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr', lg: '1fr 1fr 1fr' }, gap: 2.5 }}>
        {toolDefinitions.map((tool) => (
          <Card
            key={tool.id}
            variant="outlined"
            sx={{
              display: 'flex',
              flexDirection: 'column',
              bgcolor: 'background.paper',
              borderRadius: 2,
              '&:hover': { borderColor: 'primary.main', boxShadow: 3 },
              transition: 'all 0.2s ease-in-out',
            }}
          >
            <CardHeader
              avatar={tool.icon}
              title={<Typography variant="subtitle1" sx={{ fontWeight: 'bold' }}>{tool.title}</Typography>}
              subheader={
                <Chip
                  label={tool.reactStatus}
                  color="success"
                  size="small"
                  variant="outlined"
                  sx={{ mt: 0.5 }}
                />
              }
            />
            <Divider />
            <CardContent sx={{ flexGrow: 1 }}>
              <Typography variant="body2" color="text.secondary">
                {tool.description}
              </Typography>
            </CardContent>
            <Divider />
            <CardActions sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 1 }}>
              <Button
                variant="contained"
                fullWidth
                startIcon={<OpenInNewIcon />}
                onClick={tool.onReactAction}
              >
                {tool.reactActionLabel}
              </Button>

              <Tooltip title={`Launch the legacy Swing ${tool.legacyName} GUI on the desktop for side-by-side comparison`}>
                <Button
                  variant="outlined"
                  color="inherit"
                  size="small"
                  fullWidth
                  onClick={() => handleLegacyLaunch(tool.legacyName)}
                  disabled={launching === tool.legacyName}
                  startIcon={launching === tool.legacyName ? <CircularProgress size={14} color="inherit" /> : <DesktopWindowsIcon fontSize="small" />}
                  sx={{ color: 'text.secondary', borderColor: 'divider' }}
                >
                  Start {tool.legacyName}
                </Button>
              </Tooltip>
            </CardActions>
          </Card>
        ))}
      </Box>

      {/* Embedded ACL Composer Modal */}
      <MessageComposerDialog
        open={composerOpen}
        onClose={() => setComposerOpen(false)}
        onSent={(res) => notify(`Message sent to ${res.receivers.join(', ')}`, 'success')}
      />

      <NotificationSnackbar feedback={feedback} onClose={close} />
    </Box>
  );
}
