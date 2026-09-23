import { createContext, useContext, useEffect, useState } from "react";
import { api, setToken } from "../services/api";
const AuthContext = createContext(null);
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  useEffect(() => {
    const expire = () => {
      setToken(null);
      setUser(null);
    };
    window.addEventListener("session-expired", expire);
    return () => window.removeEventListener("session-expired", expire);
  }, []);
  const login = async (values) => {
    const { data } = await api.post("/auth/login", values);
    setToken(data.accessToken);
    setUser(data.user);
  };
  const logout = async () => {
    try {
      await api.post("/auth/logout");
    } finally {
      setToken(null);
      setUser(null);
    }
  };
  return (
    <AuthContext.Provider
      value={{
        user,
        setUser,
        login,
        logout,
        has: (...roles) => roles.some((r) => user?.roles.includes(r)),
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
export const useAuth = () => useContext(AuthContext);
