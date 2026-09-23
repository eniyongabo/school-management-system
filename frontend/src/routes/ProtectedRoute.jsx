import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
export default function ProtectedRoute({ roles }) {
  const { user, has } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (roles && !has(...roles)) return <Navigate to="/" replace />;
  return <Outlet />;
}
