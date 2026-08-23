import { Fade, LinearProgress } from '@mui/material';

interface TopProgressBarProps {
  active?: boolean;
}

export default function TopProgressBar({ active = true }: TopProgressBarProps) {
  return (
    <Fade in={active} unmountOnExit>
      <LinearProgress
        aria-label="Loading"
        sx={{
          position: 'fixed',
          top: 0,
          left: 0,
          width: '100%',
          zIndex: (t) => t.zIndex.tooltip + 1,
        }}
      />
    </Fade>
  );
}
