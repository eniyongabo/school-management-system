export const human = (value) =>
  String(value || "")
    .toLowerCase()
    .replaceAll("_", " ")
    .replace(/\b\w/g, (c) => c.toUpperCase());
export const money = (amount, currency = "USD") =>
  new Intl.NumberFormat(undefined, { style: "currency", currency }).format(
    amount || 0,
  );
export function display(value) {
  if (value === null || value === undefined || value === "") return "—";
  if (typeof value === "boolean") return value ? "Yes" : "No";
  if (Array.isArray(value)) return value.map(display).join(", ");
  if (typeof value === "object")
    return Object.entries(value)
      .map(([k, v]) => `${human(k)}: ${display(v)}`)
      .join(" · ");
  if (typeof value === "string" && /^\d{4}-\d{2}-\d{2}T/.test(value))
    return new Date(value).toLocaleString(undefined, {
      dateStyle: "medium",
      timeStyle: "short",
    });
  return String(value);
}
