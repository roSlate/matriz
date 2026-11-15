import { useRef, useState, type FormEvent } from "react";
import { ApiError } from "../../api/client";
import type { TaskInput, TaskPriority, TaskStatus } from "../../api/tasks";
import { Alert } from "../../components/ui/Alert";
import { Button } from "../../components/ui/Button";
import { Dialog } from "../../components/ui/Dialog";
import { SelectField } from "../../components/ui/SelectField";
import { TextArea } from "../../components/ui/TextArea";
import { TextField } from "../../components/ui/TextField";
import { PRIORITY_OPTIONS, STATUS_OPTIONS } from "./taskLabels";

const TITLE_MIN = 3;
const TITLE_MAX = 150;
const DESCRIPTION_MAX = 2000;

type FieldErrors = { title?: string; description?: string };

type TaskFormDialogProps = {
  title: string;
  submitLabel: string;
  initial?: TaskInput;
  onSubmit: (input: TaskInput) => Promise<void>;
  onClose: () => void;
};

const DEFAULTS: TaskInput = { title: "", description: "", status: "TODO", priority: "MEDIUM" };

// Characters are counted like the server counts them: after trimming, by code point.
const length = (text: string) => [...text.trim()].length;

function check(input: TaskInput): FieldErrors {
  const errors: FieldErrors = {};
  const size = length(input.title);
  if (size === 0) {
    errors.title = "Must not be empty.";
  } else if (size < TITLE_MIN || size > TITLE_MAX) {
    errors.title = `Must be between ${TITLE_MIN} and ${TITLE_MAX} characters.`;
  }
  if (length(input.description) > DESCRIPTION_MAX) {
    errors.description = `Must be at most ${DESCRIPTION_MAX} characters.`;
  }
  return errors;
}

/** Form to create or edit a task. What was typed is kept when the server refuses it. */
export function TaskFormDialog({ title, submitLabel, initial = DEFAULTS, onSubmit, onClose }: TaskFormDialogProps) {
  const [taskTitle, setTaskTitle] = useState(initial.title);
  const [description, setDescription] = useState(initial.description);
  const [status, setStatus] = useState<TaskStatus>(initial.status);
  const [priority, setPriority] = useState<TaskPriority>(initial.priority);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const inFlight = useRef(false); // blocks a second submission before the first has re-rendered

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (inFlight.current) {
      return;
    }
    const input: TaskInput = { title: taskTitle.trim(), description: description.trim(), status, priority };
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
          if (field === "title" || field === "description") {
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
          label="Title"
          autoFocus
          autoComplete="off"
          value={taskTitle}
          onChange={(event) => setTaskTitle(event.target.value)}
          error={fieldErrors.title}
        />
        <TextArea
          label="Description (optional)"
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          error={fieldErrors.description}
        />
        <div className="form-row">
          <SelectField
            label="Status"
            options={STATUS_OPTIONS}
            value={status}
            onChange={(event) => setStatus(event.target.value as TaskStatus)}
          />
          <SelectField
            label="Priority"
            options={PRIORITY_OPTIONS}
            value={priority}
            onChange={(event) => setPriority(event.target.value as TaskPriority)}
          />
        </div>
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
