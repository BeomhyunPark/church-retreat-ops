import { Link } from "react-router-dom";
import { brandInitials, useAppIdentity } from "../../shared/identity/appIdentity";

export function AppHomePage() {
  const { identity } = useAppIdentity();

  return (
    <main className="app-home">
      <header className="app-home__top">
        <div className="app-home__brand">
          <span className="brand-mark">{brandInitials(identity.organizationName)}</span>
          <div>
            <p className="eyebrow">{identity.organizationName}</p>
            <strong>{identity.appName}</strong>
          </div>
        </div>
        <Link className="text-link" to="/admin/login">
          관리자
        </Link>
      </header>

      <section className="app-home__stage">
        <div className="app-home__copy">
          <p className="eyebrow">Retreat is loading</p>
          <h1>{identity.eventName}</h1>
          <p>등록은 빠르게, 확인은 간단하게. 시작부터 현장까지 한 번에 이어집니다.</p>
          <Link className="button button--primary" to="/public/register">
            참가자 시작
          </Link>
          <Link className="text-link text-link--quiet" to="/public/self-lookup">
            이미 등록했다면 확인하기
          </Link>
        </div>

        <aside className="home-pass" aria-label={`${identity.eventName} 앱 입장 카드`}>
          <span className="home-pass__label">Retreat Pass</span>
          <strong>{identity.eventName}</strong>
          <div className="home-pass__code">
            <span />
            <span />
            <span />
            <span />
          </div>
          <div className="home-pass__meta">
            <span>READY</span>
            <span>2026</span>
          </div>
        </aside>
      </section>
    </main>
  );
}
