import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { CssBaseline, ThemeProvider, createTheme } from '@mui/material';
import DashboardIcon from '@mui/icons-material/Dashboard';
import AgentsIcon from '@mui/icons-material/Groups';
import ContainersIcon from '@mui/icons-material/Storage';
import BuildIcon from '@mui/icons-material/Build';
import PublicIcon from '@mui/icons-material/Public';
import DashboardPage from './pages/DashboardPage';
import AgentsPage from './pages/AgentsPage';
import ContainersPage from './pages/ContainersPage';
import ToolsPage from './pages/ToolsPage';
import PlatformsPage from './pages/PlatformsPage';
import Layout from './components/Layout';

const navItems = [
  { text: 'Dashboard', icon: <DashboardIcon />, path: '/' },
  { text: 'Agents', icon: <AgentsIcon />, path: '/agents' },
  { text: 'Containers', icon: <ContainersIcon />, path: '/containers' },
  { text: 'Tools', icon: <BuildIcon />, path: '/tools' },
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
            <Route path="/agents" element={<AgentsPage />} />
            <Route path="/containers" element={<ContainersPage />} />
            <Route path="/tools" element={<ToolsPage />} />
            <Route path="/platforms" element={<PlatformsPage />} />
          </Routes>
        </Layout>
      </BrowserRouter>
    </ThemeProvider>
  );
}

export default App;
