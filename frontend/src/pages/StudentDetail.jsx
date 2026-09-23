import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useApi } from "../hooks/useApi";
import { useAuth } from "../context/AuthContext";
import { api, download, errorText } from "../services/api";
import { display } from "../utils/format";
import Table from "../components/Table";
import Form from "../components/Form";
const guardianFields = [
  { name: "parentId", label: "Parent", source: "/parents", numeric: true },
  { name: "relationship", label: "Relationship", default: "Guardian" },
  {
    name: "verified",
    label: "Verified guardian relationship",
    type: "checkbox",
    required: false,
  },
];
function StudentAcademics({ id }) {
  const grades = useApi(`/grades/student/${id}`),
    attendance = useApi(`/attendance/student/${id}`),
    averages = useApi(`/grades/student/${id}/averages`);
  return (
    <>
      <section className="panel detail-panel">
        <h2>Subject averages</h2>
        {averages.error && <p className="error">{averages.error}</p>}
        <Table
          rows={averages.data?.subjects}
          columns={[
            { key: "subject" },
            { key: "term" },
            { key: "average" },
            { key: "letter" },
          ]}
        />
        <p className="muted">
          Overall average: {display(averages.data?.overallAverage)}
        </p>
      </section>
      <section className="panel detail-panel">
        <h2>Grades</h2>
        <Table
          rows={grades.data}
          columns={[
            { key: "subject" },
            { key: "title" },
            { key: "type" },
            { key: "score" },
            { key: "maximum" },
            { key: "letter" },
          ]}
        />
      </section>
      <section className="panel detail-panel">
        <h2>Attendance history</h2>
        <Table
          rows={attendance.data}
          columns={[{ key: "date" }, { key: "className" }, { key: "status" }]}
        />
      </section>
    </>
  );
}
function Guardians({ id, run }) {
  const { data, refresh } = useApi(`/students/${id}/guardians`);
  return (
    <section className="panel detail-panel">
      <h2>Guardian approval</h2>
      <Table
        rows={data}
        columns={[{ key: "parentId" }, { key: "name" }, { key: "verified" }]}
      />
      <Form
        fields={guardianFields}
        onSubmit={(values) =>
          run(async () => {
            await api.put(`/students/${id}/guardians`, values);
            refresh();
          })
        }
      />
    </section>
  );
}
export default function StudentDetail() {
  const { id } = useParams(),
    { has } = useAuth(),
    { data: s, error, refresh } = useApi(`/students/${id}`),
    [message, setMessage] = useState("");
  async function run(fn) {
    try {
      await fn();
      refresh();
      setMessage("Saved successfully.");
    } catch (e) {
      setMessage(errorText(e));
    }
  }
  if (error) return <div className="error">{error}</div>;
  if (!s) return <div className="loading">Loading student…</div>;
  return (
    <>
      <Link className="back-link" to="/students">
        ← All students
      </Link>
      <div className="page-heading">
        <div>
          <span className="eyebrow">STUDENT PROFILE · {s.studentNumber}</span>
          <h1>
            {s.firstName} {s.lastName}
          </h1>
          <p>Learning, attendance, and school information.</p>
        </div>
        {!has("ACCOUNTANT") && (
          <button
            onClick={() =>
              run(() =>
                download(
                  `/students/${id}/report-card`,
                  `report-card-${id}.txt`,
                ),
              )
            }
          >
            Download report card
          </button>
        )}
      </div>
      {message && (
        <p role="status" className="info-banner">
          {message}
        </p>
      )}
      <section className="panel detail-panel">
        <div className="profile-grid">
          {[
            "dateOfBirth",
            "gender",
            "address",
            "emergencyContactName",
            "emergencyContactPhone",
            "enrollmentDate",
            "status",
          ].map((k) => (
            <div key={k}>
              <span className="eyebrow">{k.replace(/([A-Z])/g, " $1")}</span>
              <p>{display(s[k])}</p>
            </div>
          ))}
        </div>
        {has("ADMINISTRATOR") && (
          <Form
            fields={[
              {
                name: "status",
                label: "Enrollment status",
                options: ["PENDING", "ACTIVE", "WITHDRAWN", "GRADUATED"],
              },
            ]}
            initial={{ status: s.status }}
            onSubmit={(p) => run(() => api.put(`/students/${id}/status`, p))}
          />
        )}
      </section>
      {has("ADMINISTRATOR") && <Guardians id={id} run={run} />}{" "}
      {!has("ACCOUNTANT") && <StudentAcademics id={id} />}
    </>
  );
}
