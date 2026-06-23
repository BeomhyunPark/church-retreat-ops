import { NavLink, Outlet } from "react-router-dom";

const links = [
  { to: "/public", label: "홈" },
  { to: "/public/register", label: "등록" },
  { to: "/public/self-lookup", label: "조회" },
  { to: "/public/check-in", label: "체크인" }
];

export function PublicLayout() {
  return (
    <div className="public-shell">
      <header className="public-header">
        <span className="brand-mark">GMC</span>
        <div>
          <p className="eyebrow">Retreat</p>
          <strong>수련회 안내</strong>
        </div>
      </header>
      <main className="public-main">
        <Outlet />
      </main>
      <nav className="bottom-nav" aria-label="공개 화면 메뉴">
        {links.map((link) => (
          <NavLink key={link.to} to={link.to} end={link.to === "/public"}>
            {link.label}
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
