import { useCallback, useState } from 'react';
import { Snackbar, Alert } from '@mui/material';

export interface Feedback {
  open: boolean;
  message: string;
  severity: 'success' | 'error';
}

const closed: Feedback = { open: false, message: '', severity: 'success' };

export function useFeedback() {
  const [feedback, setFeedback] = useState<Feedback>(closed);

  // Stable identities: pages use `notify` in useCallback/useEffect deps, and
  // an unstable function there causes fetch loops (API-call spam).
  const notify = useCallback(
    (message: string, severity: 'success' | 'error' = 'success') =>
      setFeedback({ open: true, message, severity }),
    [],
  );
  const close = useCallback(() => setFeedback(closed), []);

  return { feedback, notify, close };
}

interface NotificationSnackbarProps {
  feedback: Feedback;
  onClose: () => void;
}

export default function NotificationSnackbar({ feedback, onClose }: NotificationSnackbarProps) {
  return (
    <Snackbar open={feedback.open} autoHideDuration={5000} onClose={onClose}>
      <Alert severity={feedback.severity} onClose={onClose}>
        {feedback.message}
      </Alert>
    </Snackbar>
  );
}
