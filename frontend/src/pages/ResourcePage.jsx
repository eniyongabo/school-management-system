import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Plus, Search, Download, Check, ArrowUpRight } from "lucide-react";
import { modules } from "../utils/modules";
import { useApi } from "../hooks/useApi";
import { useAuth } from "../context/AuthContext";
import { api, download, errorText } from "../services/api";
import Form from "../components/Form";
import Table from "../components/Table";
import Modal from "../components/Modal";
export default function ResourcePage({ name }) {
  const config = modules[name],
    { has, user } = useAuth();
  const [page, setPage] = useState(0),
    [search, setSearch] = useState(""),
    [query, setQuery] = useState(""),
    [modal, setModal] = useState(null),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [notice, setNotice] = useState("");
  const {
    data,
    loading,
    error: loadError,
    refresh,
  } = useApi(
    `/${name}?page=${page}&size=20&search=${encodeURIComponent(query)}`,
  );
  const rows = data?.items || data?.content || [];
  const fields = useMemo(
    () =>
      config.fields?.filter((f) => !f.adminOnly || has("ADMINISTRATOR")) || [],
    [name, user],
  );
  async function run(operation) {
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await operation();
      setModal(null);
      refresh();
      setNotice("Saved successfully.");
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  }
  const create = (values) => run(() => api.post("/" + name, values));
  const payFields = useMemo(
    () => [
      {
        name: "amount",
        label: "Payment amount",
        type: "number",
        min: "0.01",
        step: "0.01",
        max: modal?.row?.balance,
        default: modal?.row?.balance,
      },
    ],
    [modal?.row?.id],
  );
  const action = (row) => (
    <>
      {name === "students" && (
        <Link className="text-button" to={`/students/${row.id}`}>
          View profile <ArrowUpRight size={14} />
        </Link>
      )}
      {name === "students" && has("ADMINISTRATOR") && (
        <button onClick={() => setModal({ type: "edit", row })}>Edit</button>
      )}
      {name === "assignments" && (
        <Link className="text-button" to={`/assignments/${row.id}`}>
          Open <ArrowUpRight size={14} />
        </Link>
      )}
      {name === "invoices" &&
        row.balance > 0 &&
        has("PARENT", "ADMINISTRATOR", "ACCOUNTANT") && (
          <button
            onClick={() =>
              setModal({ type: "pay", row, key: crypto.randomUUID() })
            }
          >
            Mock payment
          </button>
        )}
      {name === "payments" && row.receiptId && (
        <button
          onClick={() =>
            run(() =>
              download(
                `/receipts/${row.receiptId}`,
                `receipt-${row.receiptId}.txt`,
              ),
            )
          }
        >
          <Download size={14} /> Receipt
        </button>
      )}
      {name === "notifications" && !row.readAt && (
        <button
          onClick={() => run(() => api.put(`/notifications/${row.id}/read`))}
        >
          <Check size={14} /> Mark read
        </button>
      )}
      {name === "messages" && row.recipientId === user.id && !row.readAt && (
        <button onClick={() => run(() => api.put(`/messages/${row.id}/read`))}>
          Mark read
        </button>
      )}
      {[
        "grades",
        "events",
        "academic-years",
        "terms",
        "classes",
        "subjects",
        "offerings",
        "schedules",
        "assignments",
        "teachers",
        "staff",
        "fee-structures",
      ].includes(name) &&
        has(...(config.create || [])) && (
          <button onClick={() => setModal({ type: "edit", row })}>Edit</button>
        )}
      {name === "events" && has("ADMINISTRATOR") && (
        <button onClick={() => setModal({ type: "delete", row })}>
          Delete
        </button>
      )}
      {name === "users" && row.id !== user.id && (
        <button onClick={() => setModal({ type: "access", row })}>
          Manage access
        </button>
      )}
      {name === "announcements" && (
        <button onClick={() => setModal({ type: "read", row })}>Read</button>
      )}
      {name === "enrollments" && !row.withdrawnOn && has("ADMINISTRATOR") && (
        <button onClick={() => setModal({ type: "withdraw", row })}>
          Withdraw
        </button>
      )}
    </>
  );
  const initial =
    modal?.type === "edit"
      ? Object.fromEntries(
          fields.map((f) => [
            f.name,
            f.type === "datetime-local"
              ? new Date(
                  new Date(modal.row[f.name]).getTime() -
                    new Date(modal.row[f.name]).getTimezoneOffset() * 60000,
                )
                  .toISOString()
                  .slice(0, 16)
              : modal.row[f.name],
          ]),
        )
      : {};
  return (
    <>
      <div className="page-heading">
        <div>
          <span className="eyebrow">SCHOOL MANAGEMENT</span>
          <h1>{config.title}</h1>
          <p>{config.subtitle}</p>
        </div>
        {config.create && has(...config.create) && (
          <button
            className="primary"
            onClick={() => {
              setError("");
              setModal({ type: "create" });
            }}
          >
            <Plus size={17} /> Add {name === "users" ? "account" : "record"}
          </button>
        )}
      </div>
      {["invoices", "payments"].includes(name) && (
        <div className="info-banner">
          Demo payments only. No card details or real money are processed.
        </div>
      )}
      {(error || loadError) && (
        <div role="alert" className="error">
          {error || loadError}
        </div>
      )}
      {notice && (
        <div role="status" className="success">
          {notice}
        </div>
      )}
      <section className="panel">
        <div className="panel-toolbar">
          <form
            className="search"
            onSubmit={(e) => {
              e.preventDefault();
              setQuery(search);
              setPage(0);
            }}
          >
            <Search size={17} />
            <input
              aria-label="Search records"
              placeholder={`Search ${config.title.toLowerCase()}…`}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <button>Search</button>
          </form>
          <span className="muted">{data?.totalElements || 0} records</span>
        </div>
        {loading ? (
          <div className="loading">Loading school records…</div>
        ) : (
          <Table rows={rows} columns={config.columns} actions={action} />
        )}
        <div className="pagination">
          <span>
            Page {page + 1} of {Math.max(1, data?.totalPages || 1)}
          </span>
          <div>
            <button disabled={page === 0} onClick={() => setPage(page - 1)}>
              Previous
            </button>
            <button
              disabled={page + 1 >= (data?.totalPages || 1)}
              onClick={() => setPage(page + 1)}
            >
              Next
            </button>
          </div>
        </div>
      </section>
      {modal && (
        <Modal
          title={
            modal.type === "pay"
              ? "Make a mock payment"
              : modal.type === "read"
                ? modal.row.title
                : modal.type === "access"
                  ? "Manage account access"
                  : `${modal.type === "create" ? "Add" : modal.type === "edit" ? "Edit" : "Confirm"} ${config.title.toLowerCase()}`
          }
          onClose={() => !busy && setModal(null)}
        >
          {error && (
            <div className="error" role="alert">
              {error}
            </div>
          )}
          {["create", "edit"].includes(modal.type) && (
            <Form
              fields={fields}
              initial={initial}
              busy={busy}
              onSubmit={
                modal.type === "create"
                  ? create
                  : (values) =>
                      run(() => api.put(`/${name}/${modal.row.id}`, values))
              }
            />
          )}{" "}
          {modal.type === "pay" && (
            <>
              <p className="muted">
                This simulates a payment of {modal.row.currency}. No money will
                move.
              </p>
              <Form
                fields={payFields}
                busy={busy}
                submitLabel="Confirm mock payment"
                onSubmit={(values) =>
                  run(() =>
                    api.post(
                      "/payments",
                      { invoiceId: modal.row.id, ...values },
                      { headers: { "Idempotency-Key": modal.key } },
                    ),
                  )
                }
              />
            </>
          )}
          {modal.type === "read" && (
            <p className="pre-wrap">{modal.row.body}</p>
          )}
          {modal.type === "access" && (
            <Form
              fields={[
                {
                  name: "active",
                  label: "Account active",
                  type: "checkbox",
                  required: false,
                },
                {
                  name: "role",
                  label: "Role",
                  options: [
                    "ADMINISTRATOR",
                    "TEACHER",
                    "PARENT",
                    "STUDENT",
                    "ACCOUNTANT",
                  ],
                },
              ]}
              initial={{ active: modal.row.active, role: modal.row.roles[0] }}
              busy={busy}
              onSubmit={(values) =>
                run(() =>
                  api.put(`/users/${modal.row.id}/access`, {
                    active: values.active,
                    roles: [values.role],
                  }),
                )
              }
            />
          )}{" "}
          {["delete", "withdraw"].includes(modal.type) && (
            <>
              <p>
                {modal.type === "delete"
                  ? "Delete this calendar event?"
                  : "Withdraw this student from the class? Enrollment history is retained."}
              </p>
              <button
                className="primary"
                disabled={busy}
                onClick={() =>
                  run(() => api.delete(`/${name}/${modal.row.id}`))
                }
              >
                Confirm
              </button>
            </>
          )}
        </Modal>
      )}
    </>
  );
}
