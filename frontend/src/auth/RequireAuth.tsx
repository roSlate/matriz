import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "./AuthContext";

/** Shows the nested routes only to a signed-in user; everyone else goes to the login page. */
export default function RequireAuth() {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <p className="page" role="status">
        Loading…
      </p>
    );
  }
  return user ? <Outlet /> : <Navigate to="/login" replace />;
}
