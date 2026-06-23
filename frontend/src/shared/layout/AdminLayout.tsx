import { useQuery } from "@tanstack/react-query";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { getAdminProfile } from "../../features/admin/adminApi";
import { clearAccessToken } from "../auth/tokenStore";

const links = [
  { to: "/admin/dashboard", label: "대시보드" },
  { to: "/admin/participants", label: "참가자" },
  { to: "/admin/fees", label: "참가비" },
  { to: "/admin/community", label: "공동체" },
  { to: "/admin/retreat-groups", label: "수련회 조" },
  { to: "/admin/announcements", label: "공지" },
  { to: "/admin/schedules", label: "일정" },
  { to: "/admin/check-ins", label: "체크인" }
];

export function AdminLayout() {
  const navigate = useNavigate();
  const profileQuery = useQuery({
    queryKey: ["admin", "me"],
    queryFn: getAdminProfile
  });

  function logout() {
    clearAccessToken();
    navigate("/admin/login");
  }

  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <div className="admin-sidebar__brand">
          <span className="brand-mark">GMC</span>
          <div>
            <p className="eyebrow">Admin</p>
            <strong>Retreat Ops</strong>
          </div>
        </div>
        <nav className="admin-nav" aria-label="관리자 메뉴">
          {links.map((link) => (
            <NavLink key={link.to} to={link.to}>
              {link.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className="admin-content">
        <header className="admin-topbar">
          <div>
            <p className="eyebrow">관리자</p>
            <strong>{profileQuery.data?.name ?? "로그인이 필요합니다"}</strong>
          </div>
          <button className="button button--ghost" onClick={logout} type="button">
            로그아웃
          </button>
        </header>
        <main className="admin-main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
