import { useState, useEffect } from 'react';
import { health, type HealthStatus } from 'shared';

function App() {
  const [status, setStatus] = useState<string>('loading');

  useEffect(() => {
    fetchBackendHealth();
  }, []);

  async function fetchBackendHealth() {
    try {
      const data: HealthStatus = await health.check();
      setStatus(`connected: ${JSON.stringify(data)}`);
    } catch {
      setStatus('backend unreachable');
    }
  }

  return (
    <div style={containerStyle}>
      <h1 style={{ color: '#2563eb' }}>Jade UI</h1>
      <p>Backend status: {status}</p>
    </div>
  );
}

const containerStyle: React.CSSProperties = {
  minHeight: '100vh',
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  justifyContent: 'center',
  fontFamily: 'system-ui, -apple-system, sans-serif',
  margin: 0,
};

export default App;
