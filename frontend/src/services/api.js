import axios from "axios";
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "/api",
  timeout: 20000,
});
let token = null;
export function setToken(value) {
  token = value;
}
api.interceptors.request.use((config) => {
  const publicAuth =
    /^\/auth\/(login|register|forgot-password|reset-password|verify-email|resend-verification)$/.test(
      config.url,
    );
  if (token && !publicAuth) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (
      error.response?.status === 401 &&
      !error.config.url.includes("/auth/login")
    )
      window.dispatchEvent(new Event("session-expired"));
    return Promise.reject(error);
  },
);
export function errorText(error) {
  const data = error.response?.data;
  return (
    data?.errors?.map((e) => `${e.field}: ${e.message}`).join(" · ") ||
    data?.detail ||
    data?.title ||
    (error.response
      ? "The request could not be completed."
      : "Unable to connect. Check that the school API is running.")
  );
}
export async function download(path, name) {
  const { data } = await api.get(path, { responseType: "blob" });
  const url = URL.createObjectURL(data);
  const a = document.createElement("a");
  a.href = url;
  a.download = name;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
export async function options(path) {
  let page = 0,
    all = [];
  while (true) {
    const { data } = await api.get(path, { params: { page, size: 100 } });
    if (Array.isArray(data)) return data;
    const rows = data.items || data.content || [];
    all.push(...rows);
    if (++page >= data.totalPages) return all;
  }
}
