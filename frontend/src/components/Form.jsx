import { useEffect, useState } from "react";
import { options, errorText } from "../services/api";
import { human } from "../utils/format";
export default function Form({
  fields,
  onSubmit,
  initial = {},
  submitLabel = "Save record",
  busy = false,
}) {
  const [values, setValues] = useState(initial),
    [choices, setChoices] = useState({}),
    [error, setError] = useState("");
  useEffect(() => {
    let live = true;
    Promise.all(
      fields
        .filter((f) => f.source)
        .map(async (f) => [f.name, await options(f.source)]),
    )
      .then((entries) => {
        if (live) setChoices(Object.fromEntries(entries));
      })
      .catch((e) => {
        if (live) setError(errorText(e));
      });
    return () => {
      live = false;
    };
  }, [fields]);
  const submit = (e) => {
    e.preventDefault();
    const result = {};
    for (const f of fields) {
      let value = values[f.name] ?? f.default ?? "";
      if (f.type === "checkbox") value = Boolean(value);
      else if (value === "") value = f.required === false ? null : "";
      else if (f.type === "number" || f.numeric) value = Number(value);
      else if (f.type === "datetime-local")
        value = new Date(value).toISOString();
      result[f.name] = value;
    }
    onSubmit(result);
  };
  return (
    <form onSubmit={submit} className="form-grid">
      {error && (
        <div className="error full" role="alert">
          {error}
        </div>
      )}
      {fields.map((f) => (
        <label key={f.name} className={f.type === "textarea" ? "full" : ""}>
          <span>
            {f.label || human(f.name.replace(/([A-Z])/g, " $1"))}
            {f.required !== false && f.type !== "checkbox" ? " *" : ""}
          </span>
          {f.source || f.options ? (
            <select
              required={f.required !== false}
              value={values[f.name] ?? f.default ?? ""}
              onChange={(e) =>
                setValues({ ...values, [f.name]: e.target.value })
              }
            >
              <option value="">Select {f.label || human(f.name)}</option>
              {(f.options || choices[f.name] || []).map((o) => {
                const value = typeof o === "object" ? o.id : o;
                const label =
                  typeof o === "object"
                    ? f.optionLabel
                      ? f.optionLabel(o)
                      : o.name ||
                        o.title ||
                        [o.firstName, o.lastName].filter(Boolean).join(" ") ||
                        o.email ||
                        o.id
                    : human(o);
                return (
                  <option key={value} value={value}>
                    {label}
                  </option>
                );
              })}
            </select>
          ) : f.type === "textarea" ? (
            <textarea
              required={f.required !== false}
              maxLength={f.maxLength || 10000}
              value={values[f.name] ?? ""}
              onChange={(e) =>
                setValues({ ...values, [f.name]: e.target.value })
              }
            />
          ) : (
            <input
              type={f.type || "text"}
              required={f.required !== false && f.type !== "checkbox"}
              min={f.min}
              max={f.max}
              step={f.step}
              minLength={f.minLength}
              maxLength={f.maxLength || 255}
              autoComplete={f.autoComplete || "off"}
              checked={
                f.type === "checkbox"
                  ? Boolean(values[f.name] ?? f.default)
                  : undefined
              }
              value={
                f.type === "checkbox"
                  ? undefined
                  : (values[f.name] ?? f.default ?? "")
              }
              onChange={(e) =>
                setValues({
                  ...values,
                  [f.name]:
                    f.type === "checkbox" ? e.target.checked : e.target.value,
                })
              }
            />
          )}
        </label>
      ))}
      <div className="full form-actions">
        <button className="primary" disabled={busy || Boolean(error)}>
          {busy ? "Saving…" : submitLabel}
        </button>
      </div>
    </form>
  );
}
