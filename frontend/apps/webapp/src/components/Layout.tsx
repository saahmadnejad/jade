import { ReactNode, useState } from 'react';
import { Outlet } from 'react-router-dom';
import {
  AppBar,
  Toolbar,
  Typography,
  Drawer,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  CssBaseline,
  Box,
  Button,
  IconButton,
  Tooltip,
} from '@mui/material';
import { useNavigate, useLocation } from 'react-router-dom';
import SendIcon from '@mui/icons-material/Send';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import MessageComposerDialog from './MessageComposerDialog';
import AboutDialog from './AboutDialog';
import NotificationSnackbar, { useFeedback } from './NotificationSnackbar';

interface NavItem {
  text: string;
  icon: ReactNode;
  path: string;
}

interface LayoutProps {
  navItems: NavItem[];
  children?: ReactNode;
}

const drawerWidth = 240;

export default function Layout({ navItems, children }: LayoutProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const [composerOpen, setComposerOpen] = useState(false);
  const [aboutOpen, setAboutOpen] = useState(false);
  const { feedback, notify, close } = useFeedback();

  return (
    <Box sx={{ display: 'flex' }}>
      <CssBaseline />
      <AppBar position="fixed" sx={{ zIndex: (t) => t.zIndex.drawer + 1 }}>
        <Toolbar sx={{ display: 'flex', justifyContent: 'space-between' }}>
          <Typography variant="h6" noWrap component="div">
            Jade Platform
          </Typography>

          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <Button
              variant="contained"
              color="primary"
              size="small"
              startIcon={<SendIcon />}
              onClick={() => setComposerOpen(true)}
              sx={{ bgcolor: 'primary.dark' }}
            >
              Compose Message
            </Button>
            <Tooltip title="About JADE">
              <IconButton color="inherit" onClick={() => setAboutOpen(true)} size="small">
                <InfoOutlinedIcon />
              </IconButton>
            </Tooltip>
          </Box>
        </Toolbar>
      </AppBar>
      <Box component="nav" sx={{ width: { md: drawerWidth }, flexShrink: { md: 0 } }}>
        <Drawer
          variant="permanent"
          open
          sx={{
            display: { xs: 'none', md: 'block' },
            '& .MuiDrawer-paper': { width: drawerWidth, boxSizing: 'border-box' },
          }}
        >
          <Toolbar />
          <Box sx={{ overflow: 'auto' }}>
            <List>
              {navItems.map((item) => (
                <ListItemButton
                  key={item.text}
                  selected={location.pathname === item.path}
                  onClick={() => navigate(item.path)}
                >
                  <ListItemIcon>{item.icon}</ListItemIcon>
                  <ListItemText primary={item.text} />
                </ListItemButton>
              ))}
            </List>
          </Box>
        </Drawer>
      </Box>
      <Box component="main" sx={{ flexGrow: 1, p: 3 }}>
        <Toolbar />
        {children ?? <Outlet />}
      </Box>

      {/* Global Message Composer Modal */}
      <MessageComposerDialog
        open={composerOpen}
        onClose={() => setComposerOpen(false)}
        onSent={(res) => notify(`Message sent to ${res.receivers.join(', ')}`, 'success')}
      />

      {/* Global About Modal */}
      <AboutDialog open={aboutOpen} onClose={() => setAboutOpen(false)} />

      {/* Notification Snackbar */}
      <NotificationSnackbar feedback={feedback} onClose={close} />
    </Box>
  );
}
