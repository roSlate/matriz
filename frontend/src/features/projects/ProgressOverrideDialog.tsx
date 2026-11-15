import { useRef, useState, type FormEvent } from "react";
import { ApiError, errorMessage } from "../../api/client";
import { Alert } from "../../components/ui/Alert";
import { Button } from "../../components/ui/Button";
import { Dialog } from "../../components/ui/Dialog";
import { TextField } from "../../components/ui/TextField";

const RANGE_MESSAGE = "Must be a whole number from 0 to 100.";

type ProgressOverrideDialogProps = {
  /** The value to start from: the current progress. */
  initial: number;
  onSubmit: (percentage: number) => Promise<void>;
  onClose: () => void;
};

/** Returns the number, or an error message. Only digits are accepted, so 80.5, -5 and "abc" are refused. */
function check(text: string): { value: number } | { error: string } {
  const trimmed = text.trim();
  if (trimmed === "") {
    return { error: "Must not be empty." };
  }
  if (!/^\d{1,3}$/.test(trimmed) || Number(trimmed) > 100) {
    return { error: RANGE_MESSAGE };
  }
  return { value: Number(trimmed) };
}

/** Form for the Owner's manual progress. What was typed is kept when the server refuses it. */
export function ProgressOverrideDialog({ initial, onSubmit, onClose }: ProgressOverrideDialogProps) {
  const [text, setText] = useState(String(initial));
  const [fieldError, setFieldError] = useState<string | undefined>();
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const inFlight = useRef(false); // blocks a second submission before the first has re-rendered

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (inFlight.current) {
      return;
    }
    setFormError(null);
    const result = check(text);
    if ("error" in result) {
      setFieldError(result.error);
      return;
    }
    setFieldError(undefined);

    inFlight.current = true;
    setSubmitting(true);
    try {
      await onSubmit(result.value);
    } catch (error) {
      if (error instanceof ApiError && error.fieldErrors.some((item) => item.field === "percentage")) {
        setFieldError(error.fieldErrors.find((item) => item.field === "percentage")?.message);
      } else {
        setFormError(errorMessage(error));
      }
    } finally {
      inFlight.current = false;
      setSubmitting(false);
    }
  }

  return (
    <Dialog title="Set manual progress" onClose={onClose}>
      {formError && <Alert tone="error">{formError}</Alert>}
      <form onSubmit={handleSubmit} noValidate>
        <TextField
          label="Progress (%)"
          inputMode="numeric"
          autoFocus
          autoComplete="off"
          value={text}
          onChange={(event) => setText(event.target.value)}
          error={fieldError}
        />
        <div className="dialog__actions">
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button type="submit" disabled={submitting}>
            {submitting ? "Saving…" : "Save"}
          </Button>
        </div>
      </form>
    </Dialog>
  );
}
