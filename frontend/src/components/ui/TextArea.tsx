import { useId, type TextareaHTMLAttributes } from "react";
import "./ui.css";

type TextAreaProps = TextareaHTMLAttributes<HTMLTextAreaElement> & {
  label: string;
  /** Message shown under the field; also marks the field as invalid for assistive technology. */
  error?: string;
};

export function TextArea({ label, error, id, ...rest }: TextAreaProps) {
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const errorId = `${inputId}-error`;

  return (
    <div className="field">
      <label htmlFor={inputId}>{label}</label>
      <textarea
        id={inputId}
        rows={4}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        {...rest}
      />
      {error && (
        <p id={errorId} className="field__error">
          {error}
        </p>
      )}
    </div>
  );
}
