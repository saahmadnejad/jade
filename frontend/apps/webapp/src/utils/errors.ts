/**
 * Extracts a human-readable and specific error message from an unknown error object.
 * Checks server response error data, Error.message, or falls back to String.
 */
export function getErrorMessage(error: unknown): string {
  if (typeof error === 'object' && error !== null) {
    const err = error as {
      response?: { data?: { error?: string; details?: string } };
      message?: string;
    };
    if (err.response?.data?.error) {
      const serverErr = err.response.data.error;
      const details = err.response.data.details;
      return details ? `${serverErr} (${details})` : serverErr;
    }
    if (err.message) {
      return err.message;
    }
  }
  return String(error || 'unknown error');
}
