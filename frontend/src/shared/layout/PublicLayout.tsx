import { Link, NavLink, Outlet } from "react-router-dom";
import { brandInitials, useAppIdentity } from "../identity/appIdentity";

const links = [
  { to: "/public/register", label: "등록" },
  { to: "/public/self-lookup", label: "확인" }
];

export function PublicLayout() {
  const { identity } = useAppIdentity();

  return (
    <div className="public-shell">
      <header className="public-header">
        <Link className="public-header__brand" to="/">
          <span className="brand-mark">{brandInitials(identity.organizationName)}</span>
          <div>
            <p className="eyebrow">{identity.organizationName}</p>
            <strong>{identity.eventName}</strong>
          </div>
        </Link>
      </header>
      <main className="public-main">
        <Outlet />
      </main>
      <nav className="bottom-nav" aria-label="공개 화면 메뉴">
        {links.map((link) => (
          <NavLink key={link.to} to={link.to}>
            {link.label}
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
