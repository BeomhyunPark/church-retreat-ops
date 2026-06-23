import { useEffect, useRef, useState } from "react";
import { NavLink, Outlet, useLocation } from "react-router-dom";

const links = [
  { to: "/public", label: "홈" },
  { to: "/public/register", label: "등록" },
  { to: "/public/self-lookup", label: "조회" },
  { to: "/public/check-in", label: "체크인" }
];

export function PublicLayout() {
  const location = useLocation();
  const [isNavVisible, setIsNavVisible] = useState(false);
  const hideTimerRef = useRef<number | null>(null);
  const isHome = location.pathname === "/public";

  useEffect(() => {
    function showNavBriefly() {
      setIsNavVisible(true);

      if (hideTimerRef.current !== null) {
        window.clearTimeout(hideTimerRef.current);
      }

      hideTimerRef.current = window.setTimeout(() => {
        setIsNavVisible(false);
      }, 1800);
    }

    window.addEventListener("scroll", showNavBriefly, { passive: true });
    window.addEventListener("touchmove", showNavBriefly, { passive: true });
    window.addEventListener("wheel", showNavBriefly, { passive: true });
    window.addEventListener("pointerdown", showNavBriefly, { passive: true });

    return () => {
      window.removeEventListener("scroll", showNavBriefly);
      window.removeEventListener("touchmove", showNavBriefly);
      window.removeEventListener("wheel", showNavBriefly);
      window.removeEventListener("pointerdown", showNavBriefly);

      if (hideTimerRef.current !== null) {
        window.clearTimeout(hideTimerRef.current);
      }
    };
  }, []);

  return (
    <div className="public-shell">
      {!isHome ? (
        <header className="public-header" aria-label="GMC Retreat 공개 페이지">
          <NavLink className="public-brand" to="/public" end>
            <span className="brand-mark">GMC</span>
            <span>
              <strong>테스트</strong>
            </span>
          </NavLink>
        </header>
      ) : null}
      <main className="public-main">
        <Outlet />
      </main>
      <nav
        className={`bottom-nav${isNavVisible ? " bottom-nav--visible" : ""}`}
        aria-label="공개 화면 메뉴"
        onFocus={() => setIsNavVisible(true)}
        onBlur={() => setIsNavVisible(false)}
      >
        {links.map((link) => (
          <NavLink key={link.to} to={link.to} end={link.to === "/public"}>
            {link.label}
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
