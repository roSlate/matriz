import { useRef, useState, type FormEvent } from "react";
import { ApiError } from "../../api/client";
import type { ProjectInput } from "../../api/projects";
import { Alert } from "../../components/ui/Alert";
import { Button } from "../../components/ui/Button";
import { Dialog } from "../../components/ui/Dialog";
import { TextArea } from "../../components/ui/TextArea";
import { TextField } from "../../components/ui/TextField";

const NAME_MIN = 3;
const NAME_MAX = 100;
const DESCRIPTION_MAX = 1000;

type FieldErrors = { name?: string; description?: string };

type ProjectFormDialogProps = {
  title: string;
  submitLabel: string;
  initial?: ProjectInput;
  onSubmit: (input: ProjectInput) => Promise<void>;
  onClose: () => void;
};

// Characters are counted like the server counts them: after trimming, by code point.
const length = (text: string) => [...text.trim()].length;

function check(input: ProjectInput): FieldErrors {
  const errors: FieldErrors = {};
  const name = length(input.name);
  if (name === 0) {
    errors.name = "Must not be empty.";
  } else if (name < NAME_MIN || name > NAME_MAX) {
    errors.name = `Must be between ${NAME_MIN} and ${NAME_MAX} characters.`;
  }
  if (length(input.description) > DESCRIPTION_MAX) {
    errors.description = `Must be at most ${DESCRIPTION_MAX} characters.`;
  }
  return errors;
}

/** Form to create or edit a project. What was typed is kept when the server refuses it. */
export function ProjectFormDialog({ title, submitLabel, initial, onSubmit, onClose }: ProjectFormDialogProps) {
  const [name, setName] = useState(initial?.name ?? "");
  const [description, setDescription] = useState(initial?.description ?? "");
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const inFlight = useRef(false); // blocks a second submission before the first has re-rendered

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (inFlight.current) {
      return;
    }
    const input = { name: name.trim(), description: description.trim() };
    const errors = check(input);
    setFieldErrors(errors);
    setFormError(null);
    if (Object.keys(errors).length > 0) {
      return;
    }

    inFlight.current = true;
    setSubmitting(true);
    try {
      await onSubmit(input);
    } catch (error) {
      if (error instanceof ApiError && error.fieldErrors.length > 0) {
        const fromServer: FieldErrors = {};
        for (const { field, message } of error.fieldErrors) {
          if (field === "name" || field === "description") {
            fromServer[field] = message;
          }
        }
        setFieldErrors(fromServer);
      } else {
        setFormError(error instanceof ApiError ? error.message : "Something went wrong. Please try again.");
      }
    } finally {
      inFlight.current = false;
      setSubmitting(false);
    }
  }

  return (
    <Dialog title={title} onClose={onClose}>
      {formError && <Alert tone="error">{formError}</Alert>}
      <form onSubmit={handleSubmit} noValidate>
        <TextField
          label="Name"
          autoFocus
          autoComplete="off"
          value={name}
          onChange={(event) => setName(event.target.value)}
          error={fieldErrors.name}
        />
        <TextArea
          label="Description (optional)"
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          error={fieldErrors.description}
        />
        <div className="dialog__actions">
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button type="submit" disabled={submitting}>
            {submitting ? "Saving…" : submitLabel}
          </Button>
        </div>
      </form>
    </Dialog>
  );
}
