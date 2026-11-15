import type { ReactNode } from "react";
import "./ui.css";

type AlertProps = { tone?: "info" | "error"; children: ReactNode };

/** A short message. Errors are announced at once; information is announced politely. */
export function Alert({ tone = "info", children }: AlertProps) {
  return (
    <div className={`alert alert--${tone}`} role={tone === "error" ? "alert" : "status"}>
      {children}
    </div>
  );
}
