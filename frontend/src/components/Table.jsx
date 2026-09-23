import { display, human } from "../utils/format";
export default function Table({ rows, columns, actions }) {
  if (!rows?.length)
    return (
      <div className="empty">
        <span>◇</span>
        <h3>No records yet</h3>
        <p>New records will appear here when they are added.</p>
      </div>
    );
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {columns.map((c) => (
              <th key={c.key}>
                {c.label || human(c.key.replace(/([A-Z])/g, " $1"))}
              </th>
            ))}
            {actions && <th>Actions</th>}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, index) => (
            <tr key={row.id || index}>
              {columns.map((c) => (
                <td key={c.key}>
                  {c.render ? (
                    c.render(row)
                  ) : ["status", "priority", "type", "category"].includes(
                      c.key,
                    ) ? (
                    <span
                      className={`badge ${String(row[c.key]).toLowerCase()}`}
                    >
                      {human(row[c.key])}
                    </span>
                  ) : (
                    display(row[c.key])
                  )}
                </td>
              ))}
              {actions && (
                <td>
                  <div className="row-actions">{actions(row)}</div>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
