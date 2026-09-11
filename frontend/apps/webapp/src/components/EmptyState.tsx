import { ReactNode } from 'react';
import { Paper, Typography } from '@mui/material';

interface EmptyStateProps {
  message: string;
  icon?: ReactNode;
}

export default function EmptyState({ message, icon }: EmptyStateProps) {
  return (
    <Paper sx={{ p: 4, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 1 }}>
      {icon}
      <Typography color="text.secondary">{message}</Typography>
    </Paper>
  );
}
