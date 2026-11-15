import { useId, type InputHTMLAttributes } from "react";
import "./ui.css";

type TextFieldProps = InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  /** Message shown under the field; also marks the field as invalid for assistive technology. */
  error?: string;
};

export function TextField({ label, error, id, ...rest }: TextFieldProps) {
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const errorId = `${inputId}-error`;

  return (
    <div className="field">
      <label htmlFor={inputId}>{label}</label>
      <input
        id={inputId}
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
