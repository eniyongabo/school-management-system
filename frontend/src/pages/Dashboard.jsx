import { Link } from "react-router-dom";
import {
  GraduationCap,
  BookOpen,
  Users,
  Wallet,
  ArrowUpRight,
  CalendarDays,
  ArrowRight,
  CheckCircle2,
  Bell,
} from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { useApi } from "../hooks/useApi";
import { money, human } from "../utils/format";
export default function Dashboard() {
  const { user, has } = useAuth(),
    { data: d, error, loading } = useApi("/dashboard");
  const parent = has("PARENT"),
    teacher = has("TEACHER"),
    accountant = has("ACCOUNTANT");
  if (error)
    return (
      <div className="error" role="alert">
        {error}
      </div>
    );
  if (loading)
    return <div className="loading">Preparing your school overview…</div>;
  const metrics = [
    {
      label: parent ? "Your children" : "Students",
      value: d.students,
      icon: GraduationCap,
      path: "students",
      note: parent ? "Linked student profiles" : "In your school community",
    },
    {
      label: "Classes",
      value: d.classes,
      icon: BookOpen,
      path: "classes",
      note: teacher ? "Your assigned classes" : "Connected learning spaces",
    },
    {
      label: teacher ? "Awaiting first grade" : "Teachers",
      value: teacher ? d.pendingGrades : d.teachers,
      icon: Users,
      path: teacher ? "grades" : "teachers",
      note: teacher
        ? "Enrolled student–subject records"
        : "Supporting student progress",
    },
    {
      label: accountant ? "Open balances" : "Notifications",
      value: accountant
        ? Object.values(d.outstandingByCurrency).filter((v) => v > 0).length
        : d.unreadNotifications,
      icon: accountant ? Wallet : Bell,
      path: accountant ? "invoices" : "notifications",
      note: accountant
        ? "Currencies with unpaid fees"
        : "Unread school updates",
    },
  ];
  return (
    <>
      <div className="page-heading">
        <div>
          <span className="eyebrow">YOUR SCHOOL AT A GLANCE</span>
          <h1>
            Hello, {user.firstName}
            <span className="greeting-dot">.</span>
          </h1>
          <p>Here’s what’s happening in your school community.</p>
        </div>
        <Link to="/events" className="button">
          <CalendarDays size={17} /> School calendar
        </Link>
      </div>
      <section className="welcome-banner">
        <div>
          <span className="banner-kicker">
            ONE COMMUNITY. MANY POSSIBILITIES.
          </span>
          <h2>
            A good day to
            <br />
            make a difference.
          </h2>
          <p>
            {parent
              ? "Stay close to your children’s learning journey."
              : teacher
                ? "Everything you need to support your students."
                : "Keep your school moving forward, together."}
          </p>
          <Link
            to={
              parent ? "/students" : teacher ? "/assignments" : "/announcements"
            }
          >
            {parent
              ? "View your children"
              : teacher
                ? "View assignments"
                : "See school updates"}{" "}
            <ArrowRight size={17} />
          </Link>
        </div>
        <div className="banner-art" aria-hidden="true">
          <div className="art-circle" />
          <div className="art-card card-one">
            <BookOpen size={30} />
            <span>Room to grow.</span>
            <div className="art-lines" />
          </div>
          <div className="art-card card-two">
            <GraduationCap size={40} />
            <span>
              Every student.
              <br />
              Every possibility.
            </span>
          </div>
          <span className="art-star">✳</span>
          <div className="art-check">
            <CheckCircle2 size={23} />
          </div>
        </div>
      </section>
      <div className="metric-grid">
        {metrics.map((m) => (
          <Link to={"/" + m.path} className="metric-card" key={m.label}>
            <div className="metric-top">
              <span className="metric-icon">
                <m.icon size={20} />
              </span>
              <ArrowUpRight size={17} />
            </div>
            <strong>{m.value ?? 0}</strong>
            <h3>{m.label}</h3>
            <p>{m.note}</p>
          </Link>
        ))}
      </div>
      {parent && (
        <section className="panel dashboard-summary">
          <div className="panel-heading">
            <h2>Your children</h2>
          </div>
          <div className="child-grid">
            {d.children?.length ? (
              d.children.map((child) => (
                <Link
                  key={child.id}
                  to={`/students/${child.id}`}
                  className="child-card"
                >
                  <GraduationCap size={24} />
                  <div>
                    <h3>
                      {child.firstName} {child.lastName}
                    </h3>
                    <p>
                      {child.studentNumber} · {human(child.status)}
                    </p>
                  </div>
                  <ArrowUpRight size={16} />
                </Link>
              ))
            ) : (
              <p className="muted">
                Register a child from Students. Their records become visible
                after the school verifies your guardian relationship.
              </p>
            )}
          </div>
        </section>
      )}
      <div className="dashboard-grid dashboard-summary">
        {!accountant && (
          <section className="panel">
            <div className="panel-heading">
              <div>
                <h2>Attendance overview</h2>
                <p>All recorded attendance in your scope</p>
              </div>
              <Link to="/attendance">View history</Link>
            </div>
            <div className="attendance-summary">
              {["PRESENT", "ABSENT", "LATE", "EXCUSED"].map((status) => (
                <div key={status}>
                  <strong>{d.attendance?.[status] || 0}</strong>
                  <span className={`badge ${status.toLowerCase()}`}>
                    {human(status)}
                  </span>
                </div>
              ))}
            </div>
          </section>
        )}
        {!teacher && (
          <section className="panel">
            <div className="panel-heading">
              <div>
                <h2>Fee overview</h2>
                <p>Mock payments only · grouped by currency</p>
              </div>
              <Link to="/invoices">View invoices</Link>
            </div>
            {Object.keys(d.outstandingByCurrency).length ? (
              Object.entries(d.outstandingByCurrency).map(
                ([currency, balance]) => (
                  <div className="progress-row" key={currency}>
                    <div>
                      <h3>{money(balance, currency)} outstanding</h3>
                      <p>
                        {money(
                          d.collectedByCurrency?.[currency] || 0,
                          currency,
                        )}{" "}
                        paid
                      </p>
                    </div>
                    <Wallet size={20} />
                  </div>
                ),
              )
            ) : (
              <div className="empty">
                <p>No invoices issued yet.</p>
              </div>
            )}
          </section>
        )}
        {!accountant && (
          <section className="panel">
            <div className="panel-heading">
              <div>
                <h2>Assignment deadlines</h2>
                <p>What’s due next</p>
              </div>
              <Link to="/assignments">View assignments</Link>
            </div>
            {d.upcomingAssignments?.length ? (
              d.upcomingAssignments.map((a) => (
                <Link
                  key={a.id}
                  to={`/assignments/${a.id}`}
                  className="progress-row"
                >
                  <div>
                    <h3>{a.title}</h3>
                    <p>
                      {a.className} · {a.subject}
                    </p>
                  </div>
                  <small>{new Date(a.dueAt).toLocaleDateString()}</small>
                </Link>
              ))
            ) : (
              <div className="empty">
                <p>No upcoming assignment deadlines.</p>
              </div>
            )}
          </section>
        )}
        {has("ADMINISTRATOR") && (
          <section className="panel">
            <div className="panel-heading">
              <div>
                <h2>Enrollment by class</h2>
                <p>{d.parents || 0} parent profiles in the school community</p>
              </div>
              <Link to="/enrollments">Manage</Link>
            </div>
            {d.enrollmentByClass?.length ? (
              d.enrollmentByClass.map((c) => (
                <div className="progress-row" key={c.id}>
                  <div>
                    <h3>{c.name}</h3>
                    <p>{c.academicYear}</p>
                  </div>
                  <strong>
                    {c.studentCount} / {c.capacity}
                  </strong>
                </div>
              ))
            ) : (
              <div className="empty">
                <p>Create classes to see enrollment statistics.</p>
              </div>
            )}
          </section>
        )}
      </div>
      <div className="dashboard-grid">
        <section className="panel">
          <div className="panel-heading">
            <div>
              <h2>School announcements</h2>
              <p>The latest from your community</p>
            </div>
            <Link to="/announcements">
              View all <ArrowUpRight size={15} />
            </Link>
          </div>
          {d.announcements.length ? (
            d.announcements.map((a) => (
              <article className="announcement" key={a.id}>
                <span
                  className={`announcement-icon ${a.priority.toLowerCase()}`}
                >
                  <Bell size={19} />
                </span>
                <div>
                  <div className="article-meta">
                    <span>{a.className}</span>
                    <span>{new Date(a.publishedAt).toLocaleDateString()}</span>
                  </div>
                  <h3>{a.title}</h3>
                  <p>{a.body}</p>
                </div>
              </article>
            ))
          ) : (
            <div className="empty">
              <Bell />
              <h3>You’re all caught up</h3>
              <p>School announcements will appear here.</p>
            </div>
          )}
        </section>
        <section className="panel">
          <div className="panel-heading">
            <div>
              <h2>Coming up</h2>
              <p>Dates to keep in mind</p>
            </div>
            <CalendarDays size={20} />
          </div>
          {d.upcomingEvents.length ? (
            d.upcomingEvents.map((e) => (
              <Link to="/events" className="event-row" key={e.id}>
                <div className="date-block">
                  <span>
                    {new Date(e.startAt).toLocaleDateString(undefined, {
                      month: "short",
                    })}
                  </span>
                  <strong>{new Date(e.startAt).getDate()}</strong>
                </div>
                <div>
                  <span className="event-category">{human(e.category)}</span>
                  <h3>{e.title}</h3>
                  <p>
                    {e.allDay
                      ? "All day"
                      : new Date(e.startAt).toLocaleTimeString(undefined, {
                          hour: "numeric",
                          minute: "2-digit",
                        })}
                  </p>
                </div>
              </Link>
            ))
          ) : (
            <div className="empty">
              <CalendarDays />
              <h3>A little breathing room</h3>
              <p>No upcoming events scheduled.</p>
            </div>
          )}
          <Link className="panel-bottom-link" to="/events">
            Open school calendar <ArrowRight size={16} />
          </Link>
        </section>
        <section className="panel">
          <div className="panel-heading">
            <div>
              <h2>{accountant ? "Outstanding balances" : "Recent progress"}</h2>
              <p>
                {accountant
                  ? "Grouped by currency"
                  : "A snapshot of recent grades"}
              </p>
            </div>
            <Link to={accountant ? "/invoices" : "/grades"}>
              View all <ArrowUpRight size={15} />
            </Link>
          </div>
          {accountant ? (
            Object.entries(d.outstandingByCurrency).map(([currency, value]) => (
              <div className="progress-row" key={currency}>
                <h3>{currency}</h3>
                <strong>{money(value, currency)}</strong>
              </div>
            ))
          ) : d.recentGrades.length ? (
            d.recentGrades.map((g) => (
              <div className="progress-row" key={g.id}>
                <div>
                  <h3>{g.student}</h3>
                  <p>
                    {g.subject} · {g.title}
                  </p>
                </div>
                <span className="grade-pill">
                  {g.letter} <small>{Number(g.percentage).toFixed(0)}%</small>
                </span>
              </div>
            ))
          ) : (
            <div className="empty">
              <BookOpen />
              <h3>Progress starts here</h3>
              <p>Published marks will appear here.</p>
            </div>
          )}
        </section>
        <section className="quick-panel">
          <span className="eyebrow">MAKE IT A LITTLE EASIER</span>
          <h2>Your everyday essentials</h2>
          <div>
            <Link to="/schedules">
              <BookOpen size={19} /> Class schedule <ArrowUpRight size={16} />
            </Link>
            <Link to={teacher ? "/attendance" : "/invoices"}>
              <Wallet size={19} />
              {teacher ? "Record attendance" : "Fees & invoices"}
              <ArrowUpRight size={16} />
            </Link>
            <Link to="/profile">
              <Users size={19} /> Your profile <ArrowUpRight size={16} />
            </Link>
          </div>
        </section>
      </div>
    </>
  );
}
