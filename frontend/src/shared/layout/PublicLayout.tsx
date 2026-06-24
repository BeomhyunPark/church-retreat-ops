import { NavLink, Outlet } from "react-router-dom";
import { brandInitials, useAppIdentity } from "../identity/appIdentity";

const links = [
  { to: "/public", label: "홈" },
  { to: "/public/register", label: "등록" },
  { to: "/public/self-lookup", label: "조회" },
  { to: "/public/check-in", label: "체크인" }
];

export function PublicLayout() {
  const { identity } = useAppIdentity();

  return (
    <div className="public-shell">
      <header className="public-header">
        <span className="brand-mark">{brandInitials(identity.organizationName)}</span>
        <div>
          <p className="eyebrow">{identity.organizationName}</p>
          <strong>{identity.eventName}</strong>
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
