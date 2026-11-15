import type { ReactNode } from "react";
import "./ui.css";

/** A small rounded label, for example a role. */
export function Badge({ children }: { children: ReactNode }) {
  return <span className="badge">{children}</span>;
}
