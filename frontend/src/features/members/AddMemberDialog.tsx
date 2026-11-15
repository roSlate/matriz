import { useRef, useState, type FormEvent } from "react";
import { ApiError } from "../../api/client";
import type { AssignableRole, MemberInput } from "../../api/members";
import { Alert } from "../../components/ui/Alert";
import { Button } from "../../components/ui/Button";
import { Dialog } from "../../components/ui/Dialog";
import { SelectField } from "../../components/ui/SelectField";
import { TextField } from "../../components/ui/TextField";

const EMAIL_MAX = 254;

const ROLE_OPTIONS = [
  { value: "MEMBER", label: "Member" },
  { value: "VIEWER", label: "Viewer" },
];

type AddMemberDialogProps = {
  onSubmit: (input: MemberInput) => Promise<void>;
  onClose: () => void;
};

/**
 * Form to add an existing user by email. What was typed is kept when the server refuses it, and
 * the server's reason ("no user has that email", "already a member") is shown next to the field.
 */
export function AddMemberDialog({ onSubmit, onClose }: AddMemberDialogProps) {
  const [email, setEmail] = useState("");
  const [role, setRole] = useState<AssignableRole>("MEMBER");
  const [emailError, setEmailError] = useState<string | undefined>();
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const inFlight = useRef(false); // blocks a second submission before the first has re-rendered

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (inFlight.current) {
      return;
    }
    const trimmed = email.trim();
    setFormError(null);
    if (trimmed === "") {
      setEmailError("Must not be empty.");
      return;
    }
    if (trimmed.length > EMAIL_MAX) {
      setEmailError(`Must be at most ${EMAIL_MAX} characters.`);
      return;
    }
    setEmailError(undefined);

    inFlight.current = true;
    setSubmitting(true);
    try {
      await onSubmit({ email: trimmed, role });
    } catch (error) {
      if (error instanceof ApiError && (error.status === 422 || error.status === 409)) {
        setEmailError(error.message);
      } else if (error instanceof ApiError && error.fieldErrors.length > 0) {
        setEmailError(error.fieldErrors.find((item) => item.field === "email")?.message);
        setFormError(error.fieldErrors.find((item) => item.field !== "email")?.message ?? null);
      } else {
        setFormError(error instanceof ApiError ? error.message : "Something went wrong. Please try again.");
      }
    } finally {
      inFlight.current = false;
      setSubmitting(false);
    }
  }

  return (
    <Dialog title="Add member" onClose={onClose}>
      {formError && <Alert tone="error">{formError}</Alert>}
      <form onSubmit={handleSubmit} noValidate>
        <TextField
          label="Email"
          type="email"
          autoFocus
          autoComplete="off"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          error={emailError}
        />
        <SelectField
          label="Role"
          options={ROLE_OPTIONS}
          value={role}
          onChange={(event) => setRole(event.target.value as AssignableRole)}
        />
        <div className="dialog__actions">
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button type="submit" disabled={submitting}>
            {submitting ? "Adding…" : "Add member"}
          </Button>
        </div>
      </form>
    </Dialog>
  );
}
