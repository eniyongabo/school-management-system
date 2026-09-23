import { useState } from "react";
import { Download } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { useApi } from "../hooks/useApi";
import { download, errorText } from "../services/api";
import { human } from "../utils/format";
import Table from "../components/Table";
export default function Reports() {
  const { has } = useAuth(),
    [type, setType] = useState(has("ADMINISTRATOR") ? "enrollment" : "fees"),
    [message, setMessage] = useState("");
  const { data, error, loading } = useApi(`/reports/${type}`);
  const types = has("ADMINISTRATOR")
    ? [
        "enrollment",
        "attendance",
        "grades",
        "fees",
        "payments",
        "outstanding",
        "teachers",
        "classes",
        "audit",
      ]
    : ["fees", "payments", "outstanding"];
  return (
    <>
      <div className="page-heading">
        <div>
          <span className="eyebrow">INSIGHTS & RECORDS</span>
          <h1>School reports</h1>
          <p>Turn school activity into a clearer picture.</p>
        </div>
        <button
          className="primary"
          onClick={async () => {
            try {
              await download(`/reports/${type}/export`, `${type}-report.csv`);
            } catch (e) {
              setMessage(errorText(e));
            }
          }}
        >
          <Download size={17} /> Export CSV
        </button>
      </div>
      <div className="tabs" role="group" aria-label="Report type">
        {types.map((t) => (
          <button
            className={type === t ? "active" : ""}
            key={t}
            onClick={() => setType(t)}
          >
            {human(t)}
          </button>
        ))}
      </div>
      {(error || message) && (
        <p className="error" role="alert">
          {error || message}
        </p>
      )}
      <section className="panel">
        {loading ? (
          <div className="loading">Building report…</div>
        ) : (
          <Table
            rows={data}
            columns={Object.keys(data?.[0] || {}).map((key) => ({ key }))}
          />
        )}
      </section>
    </>
  );
}
