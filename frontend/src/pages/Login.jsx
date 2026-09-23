import { useState } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import {
  GraduationCap,
  ArrowRight,
  BookOpen,
  Users,
  CalendarDays,
} from "lucide-react";
import { api, errorText } from "../services/api";
import { useAuth } from "../context/AuthContext";
import Form from "../components/Form";
const email = { name: "email", type: "email", label: "Email address" };
const password = {
  name: "password",
  type: "password",
  minLength: 12,
  maxLength: 72,
  autoComplete: "new-password",
};
const forms = {
  login: [
    email,
    { ...password, minLength: undefined, autoComplete: "current-password" },
  ],
  register: [
    { name: "firstName", label: "First name" },
    { name: "lastName", label: "Last name" },
    email,
    password,
  ],
  forgot: [email],
  reset: [password],
  verify: [],
};
export default function Login() {
  const location = useLocation(),
    navigate = useNavigate(),
    { login } = useAuth();
  const mode =
    location.pathname === "/register"
      ? "register"
      : location.pathname === "/forgot-password"
        ? "forgot"
        : location.pathname === "/reset-password"
          ? "reset"
          : location.pathname === "/verify-email"
            ? "verify"
            : "login";
  const [error, setError] = useState(""),
    [message, setMessage] = useState(""),
    [busy, setBusy] = useState(false);
  const title = {
    login: "Welcome back.",
    register: "Join your school community.",
    forgot: "Reset your password.",
    reset: "Choose a new password.",
    verify: "Verify your email.",
  }[mode];
  async function submit(values) {
    setError("");
    setMessage("");
    setBusy(true);
    try {
      if (mode === "login") {
        await login(values);
        navigate("/");
      } else {
        const token = new URLSearchParams(location.hash.slice(1)).get("token");
        if (["reset", "verify"].includes(mode) && !token)
          throw new Error("Open the complete link from your email.");
        const path = {
          register: "register",
          forgot: "forgot-password",
          reset: "reset-password",
          verify: "verify-email",
        }[mode];
        const { data } = await api.post("/auth/" + path, {
          ...values,
          ...(["reset", "verify"].includes(mode) ? { token } : {}),
        });
        setMessage(
          mode === "register"
            ? "Account created. Check your email to verify it before signing in."
            : data.message,
        );
      }
    } catch (e) {
      setError(e.response || e.request ? errorText(e) : e.message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="auth-shell">
      <aside className="auth-story">
        <Link className="brand" to="/">
          <GraduationCap size={32} />
          <span>
            schoolhouse<span className="brand-dot">.</span>
          </span>
        </Link>
        <div>
          <span className="eyebrow">A CONNECTED SCHOOL COMMUNITY</span>
          <h1>
            Big futures.
            <br />
            Better together.
          </h1>
          <p>
            One place for the people, progress, and everyday moments that make
            your school.
          </p>
          <div className="story-items">
            <span>
              <BookOpen />
              Learning & progress
            </span>
            <span>
              <Users />
              Families & teachers
            </span>
            <span>
              <CalendarDays />
              Every school day
            </span>
          </div>
        </div>
        <small>School management, thoughtfully connected.</small>
      </aside>
      <main className="auth-main">
        <div className="auth-card">
          <span className="eyebrow">YOUR SCHOOL, IN ONE PLACE</span>
          <h2>{title}</h2>
          <p className="muted">
            {mode === "login"
              ? "Sign in to your school account to continue."
              : mode === "register"
                ? "Create a parent account. School staff approve child enrollment."
                : "We’ll help you securely access your account."}
          </p>
          {error && (
            <p className="error" role="alert">
              {error}
            </p>
          )}
          {message && (
            <p className="success" role="status">
              {message}
            </p>
          )}
          <Form
            key={mode}
            fields={forms[mode]}
            busy={busy}
            onSubmit={submit}
            submitLabel={
              {
                login: "Sign in",
                register: "Create parent account",
                forgot: "Send reset link",
                reset: "Reset password",
                verify: "Verify email",
              }[mode]
            }
          />
          <div className="auth-links">
            {mode === "login" ? (
              <>
                <Link to="/forgot-password">Forgot password?</Link>
                <Link to="/register">
                  Create parent account <ArrowRight size={14} />
                </Link>
              </>
            ) : (
              <Link to="/login">Back to sign in</Link>
            )}
          </div>
          <p className="auth-footnote">
            Teachers, students, and staff receive their accounts from the school
            administrator.
          </p>
        </div>
      </main>
    </div>
  );
}
