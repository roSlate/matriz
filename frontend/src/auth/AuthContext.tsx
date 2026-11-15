import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { apiRequest, setUnauthorizedHandler } from "../api/client";

export type User = { id: number; name: string; email: string };

type AuthContextValue = {
  user: User | null;
  /** True until the first check of the session has finished. */
  loading: boolean;
  /** True when a signed-in user's session ended, so the login page can explain why. */
  sessionExpired: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [sessionExpired, setSessionExpired] = useState(false);
  const wasSignedIn = useRef(false);

  useEffect(() => {
    wasSignedIn.current = user !== null;
  }, [user]);

  // First check of the session. A 401 here only means "nobody is signed in".
  useEffect(() => {
    apiRequest<User>("/users/me", { handleUnauthorized: false })
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);

  // Any later 401 means the session ended while the user was working.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      if (wasSignedIn.current) {
        setSessionExpired(true);
      }
      setUser(null);
    });
    return () => setUnauthorizedHandler(null);
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const signedIn = await apiRequest<User>("/auth/login", {
      method: "POST",
      body: { email, password },
      handleUnauthorized: false,
    });
    setSessionExpired(false);
    setUser(signedIn);
  }, []);

  const logout = useCallback(async () => {
    try {
      await apiRequest<void>("/auth/logout", { method: "POST", handleUnauthorized: false });
    } catch {
      // Whatever the server answered, this browser is done with the session.
    }
    setSessionExpired(false);
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({ user, loading, sessionExpired, login, logout }),
    [user, loading, sessionExpired, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside an AuthProvider");
  }
  return context;
}
