import { Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { Button } from "../components/ui/Button";
import "./pages.css";

/** Frame of every screen for a signed-in user: the header with the user's name and Log out. */
export default function AppLayout() {
  const { user, logout } = useAuth();

  return (
    <div>
      <header className="shell-header">
        <span className="shell-header__brand">Matriz</span>
        <div className="shell-header__user">
          <span>{user?.name}</span>
          <Button variant="secondary" onClick={() => void logout()}>
            Log out
          </Button>
        </div>
      </header>
      <main className="page">
        <Outlet />
      </main>
    </div>
  );
}
