import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { CssBaseline, ThemeProvider, createTheme } from '@mui/material';
import DashboardIcon from '@mui/icons-material/Dashboard';
import AgentsIcon from '@mui/icons-material/Groups';
import ContainersIcon from '@mui/icons-material/Storage';
import BuildIcon from '@mui/icons-material/Build';
import PublicIcon from '@mui/icons-material/Public';
import DnsIcon from '@mui/icons-material/Dns';
import MessagesIcon from '@mui/icons-material/Sms';
import ScenariosIcon from '@mui/icons-material/Science';
import DashboardPage from './pages/DashboardPage';
import AgentsPage from './pages/AgentsPage';
import ContainersPage from './pages/ContainersPage';
import ToolsPage from './pages/ToolsPage';
import PlatformsPage from './pages/PlatformsPage';
import DFPage from './pages/DFPage';
import MessagesPage from './pages/MessagesPage';
import ScenariosPage from './pages/ScenariosPage';
import Layout from './components/Layout';

const navItems = [
  { text: 'Dashboard', icon: <DashboardIcon />, path: '/' },
  { text: 'Scenarios', icon: <ScenariosIcon />, path: '/scenarios' },
  { text: 'Agents', icon: <AgentsIcon />, path: '/agents' },
  { text: 'Containers', icon: <ContainersIcon />, path: '/containers' },
  { text: 'Messages', icon: <MessagesIcon />, path: '/messages' },
  { text: 'Tools', icon: <BuildIcon />, path: '/tools' },
  { text: 'DF', icon: <DnsIcon />, path: '/df' },
  { text: 'Platforms', icon: <PublicIcon />, path: '/platforms' },
];

const theme = createTheme({
  palette: {
    mode: 'dark',
    primary: { main: '#2563eb' },
  },
});

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <BrowserRouter>
        <Layout navItems={navItems}>
          <Routes>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/scenarios" element={<ScenariosPage />} />
            <Route path="/agents" element={<AgentsPage />} />
            <Route path="/containers" element={<ContainersPage />} />
            <Route path="/messages" element={<MessagesPage />} />
            <Route path="/tools" element={<ToolsPage />} />
            <Route path="/df" element={<DFPage />} />
            <Route path="/platforms" element={<PlatformsPage />} />
          </Routes>
        </Layout>
      </BrowserRouter>
    </ThemeProvider>
  );
}

export default App;
