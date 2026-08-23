import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import TopProgressBar from './TopProgressBar';

describe('TopProgressBar', () => {
  it('Given default props, When rendered, Then shows an indeterminate loading bar', () => {
    // Arrange & Act
    render(<TopProgressBar />);

    // Assert
    expect(screen.getByRole('progressbar', { name: 'Loading' })).toBeInTheDocument();
  });

  it('Given active=false, When rendered, Then no progress bar is shown', () => {
    // Arrange & Act
    render(<TopProgressBar active={false} />);

    // Assert
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
  });
});
