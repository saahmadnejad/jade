import { ReactNode } from 'react';
import { Box, Typography } from '@mui/material';

interface PageHeaderProps {
  title: string;
  actions?: ReactNode;
}

export default function PageHeader({ title, actions }: PageHeaderProps) {
  return (
    <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
      <Typography variant="h4">{title}</Typography>
      {actions && <Box display="flex" gap={1} alignItems="center">{actions}</Box>}
    </Box>
  );
}
