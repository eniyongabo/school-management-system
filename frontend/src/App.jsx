import { Routes, Route, Navigate } from "react-router-dom";
import ProtectedRoute from "./routes/ProtectedRoute";
import AppLayout from "./layouts/AppLayout";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import ResourcePage from "./pages/ResourcePage";
import StudentDetail from "./pages/StudentDetail";
import AssignmentDetail from "./pages/AssignmentDetail";
import Reports from "./pages/Reports";
import Profile from "./pages/Profile";
import { modules, navigation } from "./utils/modules";
export default function App() {
  return (
    <Routes>
      {[
        "login",
        "register",
        "forgot-password",
        "reset-password",
        "verify-email",
      ].map((path) => (
        <Route key={path} path={"/" + path} element={<Login />} />
      ))}
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route index element={<Dashboard />} />
          <Route path="profile" element={<Profile />} />
          <Route path="students/:id" element={<StudentDetail />} />
          <Route path="assignments/:id" element={<AssignmentDetail />} />
          {Object.keys(modules).map((name) => {
            const entry = navigation
              .flatMap((g) => g.items)
              .find((i) => i[0] === name);
            return (
              <Route key={name} element={<ProtectedRoute roles={entry?.[3]} />}>
                <Route
                  path={name}
                  element={<ResourcePage key={name} name={name} />}
                />
              </Route>
            );
          })}
          <Route
            element={<ProtectedRoute roles={["ADMINISTRATOR", "ACCOUNTANT"]} />}
          >
            <Route path="reports" element={<Reports />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
