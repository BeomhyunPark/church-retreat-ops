import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { Navigate, NavLink, Outlet, useNavigate } from "react-router-dom";
import { getAdminProfile } from "../../features/admin/adminApi";
import { ApiRequestError } from "../api/client";
import { clearAccessToken, getAccessToken } from "../auth/tokenStore";
import { brandInitials, useAppIdentity } from "../identity/appIdentity";

const links = [
  { to: "/admin/dashboard", label: "대시보드" },
  { to: "/admin/participants", label: "참가자" },
  // { to: "/admin/community", label: "공동체" },
  { to: "/admin/retreat-groups", label: "수련회 조" },
  { to: "/admin/announcements", label: "공지" },
  { to: "/admin/schedules", label: "일정" },
  { to: "/admin/check-ins", label: "체크인" }
];

const systemAdminLinks = [{ to: "/admin/accounts", label: "계정 관리" }];

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
        <nav className="admin-nav" aria-label="관리자 메뉴">
          {links.map((link) => (
            <NavLink key={link.to} to={link.to}>
              {link.label}
            </NavLink>
          ))}
          {profileQuery.data?.role === "SYSTEM_ADMIN"
            ? systemAdminLinks.map((link) => (
              <NavLink key={link.to} to={link.to}>
                {link.label}
              </NavLink>
            ))
            : null}
        </nav>
      </aside>
      <div className="admin-content">
        <header className="admin-topbar">
          <div>
            <p className="eyebrow">관리자</p>
            <strong>{profileQuery.data?.name ?? "로그인이 필요합니다"}</strong>
          </div>
          <div className="table-actions">
            <NavLink className="button button--ghost" to="/">
              앱 홈
            </NavLink>
            <NavLink className="button button--ghost" to="/admin/profile">
              비밀번호 변경
            </NavLink>
            <button className="button button--ghost" onClick={logout} type="button">
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
