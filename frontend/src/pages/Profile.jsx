import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { api, errorText, setToken } from "../services/api";
import Form from "../components/Form";
import { useApi } from "../hooks/useApi";
const fields = [
  { name: "firstName", label: "First name" },
  { name: "lastName", label: "Last name" },
  { name: "phone", label: "Phone", required: false, maxLength: 30 },
];
const passwordFields = [
  {
    name: "currentPassword",
    label: "Current password",
    type: "password",
    autoComplete: "current-password",
  },
  {
    name: "newPassword",
    label: "New password (12–72 characters)",
    type: "password",
    minLength: 12,
    maxLength: 72,
    autoComplete: "new-password",
  },
];
const parentFields = [
  {
    name: "address",
    label: "Mailing address",
    type: "textarea",
    required: false,
    maxLength: 500,
  },
];
function ParentAddress() {
  const { data, error, refresh } = useApi("/parents"),
    [message, setMessage] = useState("");
  const parent = data?.items?.[0];
  return (
    <section className="panel detail-panel">
      <h2>Parent profile</h2>
      {(error || message) && (
        <p className="info-banner" role="status">
          {error || message}
        </p>
      )}
      {parent && (
        <Form
          key={parent.id}
          fields={parentFields}
          initial={{ address: parent.address }}
          onSubmit={async (values) => {
            try {
              await api.put(`/parents/${parent.id}`, values);
              refresh();
              setMessage("Address updated.");
            } catch (e) {
              setMessage(errorText(e));
            }
          }}
        />
      )}
    </section>
  );
}
export default function Profile() {
  const { user, setUser, has } = useAuth(),
    navigate = useNavigate(),
    [error, setError] = useState(""),
    [message, setMessage] = useState(""),
    [busy, setBusy] = useState(false);
  async function save(values, password = false) {
    setBusy(true);
    setError("");
    setMessage("");
    try {
      if (password) {
        await api.post("/auth/change-password", values);
        setToken(null);
        setUser(null);
        navigate("/login");
      } else {
        const { data } = await api.put("/auth/profile", values);
        setUser(data);
        setMessage("Your profile has been updated.");
      }
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <div className="page-heading">
        <div>
          <span className="eyebrow">YOUR ACCOUNT</span>
          <h1>Profile & settings</h1>
          <p>Keep your details up to date.</p>
        </div>
      </div>
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
      <section className="panel detail-panel">
        <h2>Personal information</h2>
        <p className="muted">{user.email}</p>
        <Form fields={fields} initial={user} busy={busy} onSubmit={save} />
      </section>
      {has("PARENT") && <ParentAddress />}
      <section className="panel detail-panel">
        <h2>Change password</h2>
        <p className="muted">
          Changing your password signs out all active sessions.
        </p>
        <Form
          fields={passwordFields}
          busy={busy}
          onSubmit={(p) => save(p, true)}
          submitLabel="Change password"
        />
      </section>
    </>
  );
}
