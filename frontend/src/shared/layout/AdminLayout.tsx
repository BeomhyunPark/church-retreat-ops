import { useEffect, type ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import { Navigate, NavLink, Outlet, useNavigate } from "react-router-dom";
import { getAdminProfile } from "../../features/admin/adminApi";
import { ApiRequestError } from "../api/client";
import { clearAccessToken, getAccessToken } from "../auth/tokenStore";
import { brandInitials, useAppIdentity } from "../identity/appIdentity";

const links = [
  { to: "/admin/dashboard", label: "대시보드", icon: "dashboard" },
  { to: "/admin/retreats", label: "수련회 관리", icon: "retreats" },
  { to: "/admin/schedules", label: "시간표", icon: "schedules" },
  { to: "/admin/participants", label: "참가자", icon: "participants" },
  { to: "/admin/fees", label: "참가비", icon: "fees" },
  { to: "/admin/retreat-groups", label: "수련회 조", icon: "groups" },
  { to: "/admin/announcements", label: "공지", icon: "announcements" },
  { to: "/admin/check-ins", label: "체크인", icon: "checkins" }
];

const systemAdminLinks = [{ to: "/admin/accounts", label: "계정 관리", icon: "accounts" }];

type NavIconName = (typeof links)[number]["icon"] | "accounts";

function NavIcon({ name }: { name: NavIconName }) {
  const paths: Record<NavIconName, ReactNode> = {
    dashboard: <><rect x="3" y="3" width="7" height="7" rx="2" /><rect x="14" y="3" width="7" height="7" rx="2" /><rect x="3" y="14" width="7" height="7" rx="2" /><rect x="14" y="14" width="7" height="7" rx="2" /></>,
    retreats: <><path d="M4 20V9l8-5 8 5v11" /><path d="M9 20v-6h6v6M3 20h18" /></>,
    participants: <><circle cx="9" cy="8" r="3" /><path d="M3.5 20c.4-4 2.2-6 5.5-6s5.1 2 5.5 6" /><path d="M15.5 5.5a3 3 0 0 1 0 5.5M17 14c2.2.7 3.3 2.6 3.5 6" /></>,
    fees: <><rect x="3" y="5" width="18" height="14" rx="3" /><path d="M3 10h18M7 15h3" /></>,
    groups: <><circle cx="8" cy="8" r="3" /><circle cx="17" cy="9" r="2.5" /><path d="M2.5 20c.4-4 2.2-6 5.5-6s5.1 2 5.5 6M14 15c3.7-.5 6 1.2 6.5 5" /></>,
    announcements: <><path d="M4 13V8l13-4v13L4 13Z" /><path d="m7 14 1 6h4l-2-5" /><path d="M20 8v5" /></>,
    schedules: <><rect x="3" y="5" width="18" height="16" rx="3" /><path d="M8 3v4M16 3v4M3 10h18M8 14h2M14 14h2M8 18h2" /></>,
    checkins: <><path d="M12 22c4-3.2 7-7.1 7-12a7 7 0 1 0-14 0c0 4.9 3 8.8 7 12Z" /><path d="m9 10 2 2 4-4" /></>,
    accounts: <><circle cx="12" cy="8" r="4" /><path d="M4 21c.5-5 3.2-7.5 8-7.5s7.5 2.5 8 7.5" /><path d="M18 5v4M16 7h4" /></>
  };

  return <svg className="admin-nav__icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">{paths[name]}</svg>;
}

function roleLabel(role?: string) {
  return ({ STAFF: "운영 스태프", CHAIR: "준비위원장", PASTOR: "목회자", SYSTEM_ADMIN: "시스템 관리자" } as Record<string, string>)[role ?? ""] ?? "관리자";
}

export function AdminLayout() {
  const navigate = useNavigate();
  const hasToken = Boolean(getAccessToken());
  const { identity } = useAppIdentity();
  const profileQuery = useQuery({
    queryKey: ["admin", "me"],
    queryFn: getAdminProfile,
    enabled: hasToken,
    retry: false
  });

  const unauthorized = profileQuery.error instanceof ApiRequestError && profileQuery.error.status === 401;

  useEffect(() => {
    if (unauthorized) {
      clearAccessToken();
      navigate("/admin/login", { replace: true });
    }
  }, [navigate, unauthorized]);

  function logout() {
    clearAccessToken();
    navigate("/admin/login");
  }

  if (!hasToken || unauthorized) {
    return <Navigate replace to="/admin/login" />;
  }

  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <NavLink className="admin-sidebar__brand" to="/">
          <span className="brand-mark">{brandInitials(identity.appName)}</span>
          <div>
            <p className="eyebrow">{identity.organizationName}</p>
            <strong>{identity.appName}</strong>
          </div>
        </NavLink>
        <p className="admin-nav__section">운영 메뉴</p>
        <nav className="admin-nav" aria-label="관리자 메뉴">
          {links.map((link) => (
            <NavLink key={link.to} to={link.to}>
              <NavIcon name={link.icon} />
              {link.label}
            </NavLink>
          ))}
          {profileQuery.data?.role === "SYSTEM_ADMIN"
            ? systemAdminLinks.map((link) => (
              <NavLink key={link.to} to={link.to}>
                <NavIcon name={link.icon} />
                {link.label}
              </NavLink>
            ))
            : null}
        </nav>
        <div className="admin-sidebar__footer">
          <span className="admin-user-avatar">{profileQuery.data?.name?.slice(0, 1) ?? "관"}</span>
          <div>
            <strong>{profileQuery.data?.name ?? "관리자"}</strong>
            <span>{roleLabel(profileQuery.data?.role)}</span>
          </div>
        </div>
      </aside>
      <div className="admin-content">
        <header className="admin-topbar">
          <div className="admin-topbar__context">
            <span className="admin-topbar__indicator" />
            <div>
              <p>{identity.eventName}</p>
              <strong>운영센터</strong>
            </div>
          </div>
          <div className="admin-topbar__actions">
            <NavLink className="button button--ghost button--sm" to="/">
              앱 홈
            </NavLink>
            <NavLink className="button button--ghost button--sm" to="/admin/profile">
              내 정보
            </NavLink>
            <button className="button button--ghost button--sm" onClick={logout} type="button">
              로그아웃
            </button>
          </div>
        </header>
        <main className="admin-main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
