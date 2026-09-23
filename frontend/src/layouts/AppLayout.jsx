import { useState } from "react";
import { NavLink, Outlet, Link, useNavigate } from "react-router-dom";
import {
  GraduationCap,
  LayoutDashboard,
  Bell,
  Users,
  Contact,
  BriefcaseBusiness,
  School,
  LibraryBig,
  BookOpen,
  UserPlus,
  Clock,
  NotebookPen,
  ChartNoAxesCombined,
  ClipboardCheck,
  Megaphone,
  MessagesSquare,
  CalendarDays,
  ReceiptText,
  Wallet,
  CreditCard,
  CalendarRange,
  ListTree,
  FileChartColumn,
  ShieldCheck,
  Circle,
  Settings,
  LogOut,
  ChevronRight,
  Menu,
  Sun,
  Moon,
} from "lucide-react";
const Icons = {
  GraduationCap,
  LayoutDashboard,
  Bell,
  Users,
  Contact,
  BriefcaseBusiness,
  School,
  LibraryBig,
  BookOpen,
  UserPlus,
  Clock,
  NotebookPen,
  ChartNoAxesCombined,
  ClipboardCheck,
  Megaphone,
  MessagesSquare,
  CalendarDays,
  ReceiptText,
  Wallet,
  CreditCard,
  CalendarRange,
  ListTree,
  FileChartColumn,
  ShieldCheck,
  Circle,
  Settings,
  LogOut,
  ChevronRight,
  Menu,
  Sun,
  Moon,
};
import { navigation } from "../utils/modules";
import { human } from "../utils/format";
import { useAuth } from "../context/AuthContext";
export default function AppLayout() {
  const { user, has, logout } = useAuth(),
    navigate = useNavigate();
  const [open, setOpen] = useState(false),
    [dark, setDark] = useState(false);
  return (
    <div className={`app-shell ${dark ? "dark" : ""}`}>
      <aside className={`sidebar ${open ? "open" : ""}`}>
        <Link className="brand" to="/">
          <Icons.GraduationCap size={31} />
          <span>
            schoolhouse<span className="brand-dot">.</span>
          </span>
        </Link>
        <div className="school-label">
          <div className="school-avatar">S</div>
          <div>
            <strong>School workspace</strong>
            <small>{human(user.roles[0])} portal</small>
          </div>
        </div>
        <nav aria-label="Main navigation">
          {navigation.map((group) => {
            const items = group.items.filter(
              (item) => !item[3] || has(...item[3]),
            );
            return items.length ? (
              <div className="nav-group" key={group.label}>
                <span>{group.label}</span>
                {items.map(([path, label, icon]) => {
                  const Icon = Icons[icon] || Icons.Circle;
                  return (
                    <NavLink
                      key={path}
                      to={"/" + path}
                      end={path === ""}
                      onClick={() => setOpen(false)}
                    >
                      <Icon size={18} />
                      {label}
                    </NavLink>
                  );
                })}
              </div>
            ) : null;
          })}
        </nav>
        <div className="sidebar-footer">
          <Link to="/profile">
            <Icons.Settings size={18} /> Profile & settings
          </Link>
          <button
            onClick={async () => {
              try {
                await logout();
              } finally {
                navigate("/login");
              }
            }}
          >
            <Icons.LogOut size={18} /> Sign out
          </button>
        </div>
      </aside>
      {open && (
        <button
          className="sidebar-overlay"
          aria-label="Close navigation"
          onClick={() => setOpen(false)}
        />
      )}
      <div className="main-shell">
        <header className="topbar">
          <div className="topbar-left">
            <button
              className="icon-button mobile-toggle"
              aria-label="Open navigation"
              onClick={() => setOpen(!open)}
            >
              <Icons.Menu />
            </button>
            <span className="breadcrumb">
              Workspace <Icons.ChevronRight size={14} />{" "}
              <strong>{human(user.roles[0])} portal</strong>
            </span>
          </div>
          <div className="topbar-right">
            <span className="today">
              {new Date().toLocaleDateString(undefined, {
                weekday: "short",
                month: "short",
                day: "numeric",
              })}
            </span>
            <button
              className="icon-button"
              aria-label="Toggle color theme"
              onClick={() => setDark(!dark)}
            >
              {dark ? <Icons.Sun size={19} /> : <Icons.Moon size={19} />}
            </button>
            <Link
              className="icon-button"
              aria-label="Notifications"
              to="/notifications"
            >
              <Icons.Bell size={20} />
            </Link>
            <Link className="avatar" to="/profile" aria-label="Your profile">
              {user.firstName[0]}
              {user.lastName[0]}
            </Link>
          </div>
        </header>
        <main className="page-content">
          <Outlet />
        </main>
        <footer className="main-footer">
          Schoolhouse · A little more connected.
          <span>School Management System</span>
        </footer>
      </div>
    </div>
  );
}
