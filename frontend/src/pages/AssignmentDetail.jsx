import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useApi } from "../hooks/useApi";
import { useAuth } from "../context/AuthContext";
import { api, download, errorText } from "../services/api";
import Form from "../components/Form";
import Table from "../components/Table";
const submissionFields = [
  { name: "studentId", label: "Student", source: "/students", numeric: true },
  { name: "body", label: "Your submission", type: "textarea" },
];
export default function AssignmentDetail() {
  const { id } = useParams(),
    { has } = useAuth(),
    assignment = useApi(`/assignments/${id}`),
    docs = useApi(`/assignments/${id}/documents`),
    subs = useApi(`/assignments/${id}/submissions`),
    [message, setMessage] = useState(""),
    [busy, setBusy] = useState(false);
  async function run(fn) {
    setBusy(true);
    try {
      await fn();
      docs.refresh();
      subs.refresh();
      setMessage("Completed successfully.");
    } catch (e) {
      setMessage(errorText(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <Link to="/assignments" className="back-link">
        ← Assignments
      </Link>
      <div className="page-heading">
        <div>
          <span className="eyebrow">LEARNING SPACE</span>
          <h1>{assignment.data?.title || "Assignment materials"}</h1>
          <p>
            {assignment.data?.subject} · {assignment.data?.className}
          </p>
        </div>
      </div>
      {(message || docs.error || subs.error) && (
        <p role="status" className="info-banner">
          {message || docs.error || subs.error}
        </p>
      )}
      <section className="panel detail-panel">
        <h2>Instructions</h2>
        <p className="pre-wrap">{assignment.data?.description}</p>
        <p className="muted">
          Due:{" "}
          {assignment.data && new Date(assignment.data.dueAt).toLocaleString()}
        </p>
      </section>
      <section className="panel detail-panel">
        <h2>Learning materials</h2>
        <Table
          rows={docs.data}
          columns={[{ key: "filename" }, { key: "uploadedAt" }]}
          actions={(d) => (
            <button
              onClick={() =>
                run(() => download(`/documents/${d.id}`, d.filename))
              }
            >
              Download
            </button>
          )}
        />
        {has("ADMINISTRATOR", "TEACHER") && (
          <label className="upload-box">
            Upload PDF, text, PNG or JPEG (up to 10 MB)
            <input
              aria-label="Upload learning material"
              type="file"
              accept=".pdf,.txt,.png,.jpg,.jpeg"
              disabled={busy}
              onChange={(e) => {
                const file = e.target.files[0];
                if (file) {
                  const form = new FormData();
                  form.append("file", file);
                  run(() => api.post(`/assignments/${id}/documents`, form));
                }
              }}
            />
          </label>
        )}
      </section>
      {has("STUDENT") && (
        <section className="panel detail-panel">
          <h2>Submit your work</h2>
          <Form
            fields={submissionFields}
            busy={busy}
            onSubmit={(p) =>
              run(() => api.post(`/assignments/${id}/submissions`, p))
            }
            submitLabel="Submit assignment"
          />
        </section>
      )}
      <section className="panel detail-panel">
        <h2>Submissions</h2>
        <Table
          rows={subs.data}
          columns={[
            { key: "student" },
            { key: "body" },
            { key: "submittedAt" },
          ]}
        />
      </section>
    </>
  );
}
