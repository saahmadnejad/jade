import { useEffect, useState } from 'react';
import {
  Box,
  Typography,
  Card,
  CardHeader,
  CardContent,
  Chip,
  IconButton,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  List,
  ListItem,
  ListItemAvatar,
  Avatar,
  Tooltip,
  CircularProgress,
  Button,
  Divider,
} from '@mui/material';
import StorageIcon from '@mui/icons-material/Storage';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import RefreshIcon from '@mui/icons-material/Refresh';
import SendIcon from '@mui/icons-material/Send';
import PauseIcon from '@mui/icons-material/Pause';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import MergeIcon from '@mui/icons-material/Merge';
import AcUnitIcon from '@mui/icons-material/AcUnit';
import AcUnitOutlinedIcon from '@mui/icons-material/AcUnitOutlined';
import DeleteForeverIcon from '@mui/icons-material/Delete';
import PublicIcon from '@mui/icons-material/Public';
import { api, type PlatformTreeResponse, type ContainerTreeNode, type AgentInfo } from 'shared';

interface PlatformTreeViewProps {
  onSendMessage?: (agentName: string) => void;
  onKillAgent?: (agentName: string) => void;
  onSuspendAgent?: (agentName: string) => void;
  onResumeAgent?: (agentName: string) => void;
  onMoveAgent?: (agentName: string) => void;
  onCloneAgent?: (agentName: string) => void;
  onFreezeAgent?: (agentName: string) => void;
  onThawAgent?: (agentName: string) => void;
  onKillContainer?: (containerName: string) => void;
}

const stateColors: Record<string, 'success' | 'warning' | 'info' | 'default'> = {
  ACTIVE: 'success',
  SUSPENDED: 'warning',
  FROZEN: 'info',
};

export default function PlatformTreeView({
  onSendMessage,
  onKillAgent,
  onSuspendAgent,
  onResumeAgent,
  onMoveAgent,
  onCloneAgent,
  onFreezeAgent,
  onThawAgent,
  onKillContainer,
}: PlatformTreeViewProps) {
  const [tree, setTree] = useState<PlatformTreeResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Agent Menu State
  const [agentMenuAnchor, setAgentMenuAnchor] = useState<null | HTMLElement>(null);
  const [selectedAgent, setSelectedAgent] = useState<AgentInfo | null>(null);

  // Container Menu State
  const [containerMenuAnchor, setContainerMenuAnchor] = useState<null | HTMLElement>(null);
  const [selectedContainer, setSelectedContainer] = useState<ContainerTreeNode | null>(null);

  const fetchTree = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.platform.getTree();
      setTree(data);
    } catch (e: any) {
      setError(e.message || 'Failed to load platform hierarchy');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTree();
  }, []);

  const handleOpenAgentMenu = (event: React.MouseEvent<HTMLElement>, agent: AgentInfo) => {
    setAgentMenuAnchor(event.currentTarget);
    setSelectedAgent(agent);
  };

  const handleCloseAgentMenu = () => {
    setAgentMenuAnchor(null);
    setSelectedAgent(null);
  };

  const handleOpenContainerMenu = (event: React.MouseEvent<HTMLElement>, container: ContainerTreeNode) => {
    setContainerMenuAnchor(event.currentTarget);
    setSelectedContainer(container);
  };

  const handleCloseContainerMenu = () => {
    setContainerMenuAnchor(null);
    setSelectedContainer(null);
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      {/* Header bar */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Typography variant="h6">Platform Hierarchy (AgentTree)</Typography>
          {tree?.platformID && (
            <Chip label={`Platform: ${tree.platformID}`} size="small" color="primary" variant="outlined" />
          )}
        </Box>
        <Button
          size="small"
          startIcon={<RefreshIcon />}
          onClick={fetchTree}
          disabled={loading}
          variant="outlined"
        >
          Refresh Tree
        </Button>
      </Box>

      {loading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
          <CircularProgress />
        </Box>
      )}

      {error && (
        <Typography color="error" variant="body2" sx={{ p: 2, bgcolor: 'error.dark', borderRadius: 1 }}>
          {error}
        </Typography>
      )}

      {!loading && !error && tree && (
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: '1fr 1fr' }, gap: 2 }}>
          {tree.containers.map((container) => (
            <Card key={container.name} variant="outlined" sx={{ bgcolor: 'background.paper' }}>
              <CardHeader
                avatar={<StorageIcon color={container.isMain ? 'primary' : 'action'} />}
                action={
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Chip
                      size="small"
                      label={container.isMain ? 'Main Container' : 'Peripheral'}
                      color={container.isMain ? 'primary' : 'default'}
                    />
                    <Chip size="small" label={`${container.agents.length} agents`} variant="outlined" />
                    {!container.isMain && onKillContainer && (
                      <IconButton
                        size="small"
                        onClick={(e) => handleOpenContainerMenu(e, container)}
                      >
                        <MoreVertIcon fontSize="small" />
                      </IconButton>
                    )}
                  </Box>
                }
                title={<strong>{container.name}</strong>}
                subheader={`${container.address || '127.0.0.1'}:${container.port || '1099'}`}
              />
              <Divider />
              <CardContent sx={{ p: 1, '&:last-child': { pb: 1 } }}>
                {container.agents.length === 0 ? (
                  <Typography variant="body2" color="text.secondary" sx={{ p: 2, textAlign: 'center' }}>
                    No agents in this container
                  </Typography>
                ) : (
                  <List dense disablePadding>
                    {container.agents.map((agent) => (
                      <ListItem
                        key={agent.name}
                        divider
                        sx={{
                          borderRadius: 1,
                          '&:hover': { bgcolor: 'action.hover' },
                        }}
                        secondaryAction={
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Chip
                              size="small"
                              label={agent.state || 'UNKNOWN'}
                              color={stateColors[agent.state || ''] || 'default'}
                            />
                            <Tooltip title="Agent Actions">
                              <IconButton
                                edge="end"
                                size="small"
                                onClick={(e) => handleOpenAgentMenu(e, agent)}
                              >
                                <MoreVertIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          </Box>
                        }
                      >
                        <ListItemAvatar>
                          <Avatar sx={{ width: 32, height: 32, bgcolor: 'action.selected' }}>
                            <SmartToyIcon fontSize="small" color="primary" />
                          </Avatar>
                        </ListItemAvatar>
                        <ListItemText
                          primary={agent.name}
                          secondary={agent.ownership ? `Owner: ${agent.ownership}` : undefined}
                        />
                      </ListItem>
                    ))}
                  </List>
                )}
              </CardContent>
            </Card>
          ))}

          {/* Remote Platforms Section */}
          {tree.remotePlatforms.length > 0 && (
            <Card variant="outlined" sx={{ bgcolor: 'background.paper', gridColumn: '1 / -1' }}>
              <CardHeader
                avatar={<PublicIcon color="secondary" />}
                title={<strong>Federated Remote Platforms</strong>}
                subheader={`${tree.remotePlatforms.length} remote AMS connected`}
              />
              <Divider />
              <CardContent>
                <List dense>
                  {tree.remotePlatforms.map((rp) => (
                    <ListItem key={rp.name}>
                      <ListItemText
                        primary={rp.name}
                        secondary={`AMS: ${rp.ams} | Endpoints: ${rp.addresses?.join(', ') || 'none'}`}
                      />
                    </ListItem>
                  ))}
                </List>
              </CardContent>
            </Card>
          )}
        </Box>
      )}

      {/* Agent Context Menu */}
      <Menu
        anchorEl={agentMenuAnchor}
        open={Boolean(agentMenuAnchor)}
        onClose={handleCloseAgentMenu}
      >
        {selectedAgent && (
          <Box>
            <MenuItem
              onClick={() => {
                const name = selectedAgent.name;
                handleCloseAgentMenu();
                onSendMessage?.(name);
              }}
            >
              <ListItemIcon><SendIcon fontSize="small" color="primary" /></ListItemIcon>
              <ListItemText primary="Send Message" />
            </MenuItem>

            {selectedAgent.state === 'SUSPENDED' ? (
              <MenuItem
                onClick={() => {
                  const name = selectedAgent.name;
                  handleCloseAgentMenu();
                  onResumeAgent?.(name);
                }}
              >
                <ListItemIcon><PlayArrowIcon fontSize="small" color="success" /></ListItemIcon>
                <ListItemText primary="Resume Agent" />
              </MenuItem>
            ) : (
              <MenuItem
                onClick={() => {
                  const name = selectedAgent.name;
                  handleCloseAgentMenu();
                  onSuspendAgent?.(name);
                }}
              >
                <ListItemIcon><PauseIcon fontSize="small" color="warning" /></ListItemIcon>
                <ListItemText primary="Suspend Agent" />
              </MenuItem>
            )}

            <MenuItem
              onClick={() => {
                const name = selectedAgent.name;
                handleCloseAgentMenu();
                onMoveAgent?.(name);
              }}
            >
              <ListItemIcon><MergeIcon fontSize="small" /></ListItemIcon>
              <ListItemText primary="Move to Container" />
            </MenuItem>

            <MenuItem
              onClick={() => {
                const name = selectedAgent.name;
                handleCloseAgentMenu();
                onCloneAgent?.(name);
              }}
            >
              <ListItemIcon><ContentCopyIcon fontSize="small" /></ListItemIcon>
              <ListItemText primary="Clone Agent" />
            </MenuItem>

            {selectedAgent.state === 'FROZEN' ? (
              <MenuItem
                onClick={() => {
                  const name = selectedAgent.name;
                  handleCloseAgentMenu();
                  onThawAgent?.(name);
                }}
              >
                <ListItemIcon><AcUnitOutlinedIcon fontSize="small" color="info" /></ListItemIcon>
                <ListItemText primary="Thaw Agent" />
              </MenuItem>
            ) : (
              <MenuItem
                onClick={() => {
                  const name = selectedAgent.name;
                  handleCloseAgentMenu();
                  onFreezeAgent?.(name);
                }}
              >
                <ListItemIcon><AcUnitIcon fontSize="small" color="info" /></ListItemIcon>
                <ListItemText primary="Freeze Agent" />
              </MenuItem>
            )}

            <Divider />

            <MenuItem
              onClick={() => {
                const name = selectedAgent.name;
                handleCloseAgentMenu();
                onKillAgent?.(name);
              }}
              sx={{ color: 'error.main' }}
            >
              <ListItemIcon><DeleteForeverIcon fontSize="small" color="error" /></ListItemIcon>
              <ListItemText primary="Kill Agent" />
            </MenuItem>
          </Box>
        )}
      </Menu>

      {/* Container Context Menu */}
      <Menu
        anchorEl={containerMenuAnchor}
        open={Boolean(containerMenuAnchor)}
        onClose={handleCloseContainerMenu}
      >
        {selectedContainer && !selectedContainer.isMain && (
          <MenuItem
            onClick={() => {
              const name = selectedContainer.name;
              handleCloseContainerMenu();
              onKillContainer?.(name);
            }}
            sx={{ color: 'error.main' }}
          >
            <ListItemIcon><DeleteForeverIcon fontSize="small" color="error" /></ListItemIcon>
            <ListItemText primary="Kill Container" />
          </MenuItem>
        )}
      </Menu>
    </Box>
  );
}
