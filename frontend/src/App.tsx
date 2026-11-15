import { useEffect, useState } from "react";

type Status = "checking" | "up" | "down";

// Placeholder page for the project skeleton: it only shows whether the backend answers
// through the /api proxy. Real screens arrive with the increments of docs/implementation-plan.md.
export default function App() {
  const [status, setStatus] = useState<Status>("checking");

  useEffect(() => {
    fetch("/api/v1/actuator/health")
      .then((response) => setStatus(response.ok ? "up" : "down"))
      .catch(() => setStatus("down"));
  }, []);

  return (
    <main className="page">
      <section className="panel">
        <h1>Matriz</h1>
        <p>Project and task management for teams.</p>
        <p role="status">
          Backend: <strong data-status={status}>{status}</strong>
        </p>
      </section>
    </main>
  );
}
