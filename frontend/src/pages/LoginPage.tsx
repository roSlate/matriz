import { useRef, useState, type FormEvent } from "react";
import { Navigate } from "react-router-dom";
import { errorMessage } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { Alert } from "../components/ui/Alert";
import { Button } from "../components/ui/Button";
import { Card } from "../components/ui/Card";
import { TextField } from "../components/ui/TextField";
import "./pages.css";

type FieldErrors = { email?: string; password?: string };

export default function LoginPage() {
  const { user, login, sessionExpired } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const inFlight = useRef(false); // blocks a second submission before the first has re-rendered

  if (user) {
    return <Navigate to="/" replace />;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (inFlight.current) {
      return;
    }

    const errors: FieldErrors = {};
    if (email.trim() === "") {
      errors.email = "Email is required.";
    }
    if (password.trim() === "") {
      errors.password = "Password is required.";
    }
    setFieldErrors(errors);
    setFormError(null);
    if (Object.keys(errors).length > 0) {
      return;
    }

    inFlight.current = true;
    setSubmitting(true);
    try {
      await login(email.trim(), password);
    } catch (error) {
      // The email stays in the form; the message is the server's own (the same for any bad login).
      setFormError(errorMessage(error));
    } finally {
      inFlight.current = false;
      setSubmitting(false);
    }
  }

  return (
    <main className="auth-page">
      <Card className="auth-card">
        <h1>Log in</h1>
        <p className="auth-card__intro">Matriz: projects and tasks for your team.</p>

        {sessionExpired && <Alert>Your session has expired. Please log in again.</Alert>}
        {formError && <Alert tone="error">{formError}</Alert>}

        <form onSubmit={handleSubmit} noValidate>
          <TextField
            label="Email"
            type="email"
            autoComplete="username"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            error={fieldErrors.email}
          />
          <TextField
            label="Password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            error={fieldErrors.password}
          />
          <Button type="submit" disabled={submitting}>
            {submitting ? "Logging in…" : "Log in"}
          </Button>
        </form>
      </Card>
    </main>
  );
}
