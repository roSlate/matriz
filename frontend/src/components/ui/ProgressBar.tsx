import "./ui.css";

type ProgressBarProps = { value: number; label: string };

/** A horizontal bar for a whole percentage from 0 to 100. The text label tells the same thing. */
export function ProgressBar({ value, label }: ProgressBarProps) {
  const clamped = Math.min(100, Math.max(0, value));
  return (
    <div
      className="progress"
      role="progressbar"
      aria-label={label}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={clamped}
    >
      <span className="progress__fill" style={{ width: `${clamped}%` }} />
    </div>
  );
}
