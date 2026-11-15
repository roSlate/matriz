import { useRef, useState, type ReactNode } from "react";
import { ApiError } from "../../api/client";
import { Alert } from "./Alert";
import { Button } from "./Button";
import { Dialog } from "./Dialog";

type ConfirmDialogProps = {
  title: string;
  message: ReactNode;
  confirmLabel: string;
  onConfirm: () => Promise<void>;
  onCancel: () => void;
};

/**
 * Asks the user to confirm an action that cannot be undone. If the action fails, the dialog stays
 * open and shows the error: a failure is never reported as a success.
 */
export function ConfirmDialog({ title, message, confirmLabel, onConfirm, onCancel }: ConfirmDialogProps) {
  const [working, setWorking] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const inFlight = useRef(false);

  async function confirm() {
    if (inFlight.current) {
      return;
    }
    inFlight.current = true;
    setWorking(true);
    setError(null);
    try {
      await onConfirm();
    } catch (failure) {
      setError(failure instanceof ApiError ? failure.message : "Something went wrong. Please try again.");
    } finally {
      inFlight.current = false;
      setWorking(false);
    }
  }

  return (
    <Dialog title={title} onClose={onCancel}>
      {error && <Alert tone="error">{error}</Alert>}
      <div>{message}</div>
      <div className="dialog__actions">
        <Button variant="secondary" onClick={onCancel} disabled={working}>
          Cancel
        </Button>
        <Button onClick={() => void confirm()} disabled={working}>
          {working ? "Working…" : confirmLabel}
        </Button>
      </div>
    </Dialog>
  );
}
