import { useState } from 'react';
import { Snackbar, Alert } from '@mui/material';

export interface Feedback {
  open: boolean;
  message: string;
  severity: 'success' | 'error';
}

const closed: Feedback = { open: false, message: '', severity: 'success' };

export function useFeedback() {
  const [feedback, setFeedback] = useState<Feedback>(closed);

  const notify = (message: string, severity: 'success' | 'error' = 'success') =>
    setFeedback({ open: true, message, severity });

  const close = () => setFeedback(closed);

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
