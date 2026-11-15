import { useEffect, useId, type ReactNode } from "react";
import "./ui.css";

type DialogProps = { title: string; onClose: () => void; children: ReactNode };

/**
 * A modal window. Escape closes it; clicking outside does not, so typed text is not lost by a
 * stray click. Focus moves to the field marked with autoFocus. It does not trap focus yet.
 */
export function Dialog({ title, onClose, children }: DialogProps) {
  const titleId = useId();

  useEffect(() => {
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
      }
    };
    document.addEventListener("keydown", closeOnEscape);
    return () => document.removeEventListener("keydown", closeOnEscape);
  }, [onClose]);

  return (
    <div className="dialog-backdrop">
      <div className="dialog" role="dialog" aria-modal="true" aria-labelledby={titleId}>
        <h2 id={titleId}>{title}</h2>
        {children}
      </div>
    </div>
  );
}
