import { Link } from "react-router-dom";
import { StatusMessage } from "../../shared/ui/StatusMessage";
import { brandInitials, useAppIdentity } from "../../shared/identity/appIdentity";

export function AppHomePage() {
  const { identity, isLoading, isError, error } = useAppIdentity();

  return (
    <main className="app-home">
      <header className="app-home__top">
        <div className="app-home__brand">
          <span className="brand-mark">{isLoading ? "" : brandInitials(identity.organizationName)}</span>
          <div>
            <p className="eyebrow">{isLoading ? "불러오는 중..." : identity.organizationName}</p>
            <strong>{isLoading ? "" : identity.appName}</strong>
          </div>
        </div>
        <Link className="text-link" to="/admin/login">
          관리자
        </Link>
      </header>

      {isError ? (
        <StatusMessage
          message={`${error.message} 기본 정보로 표시합니다.`}
          tone="error"
        />
      ) : null}

      <section className="app-home__stage">
        <div className="app-home__copy">
          <p className="eyebrow">Retreat is loading</p>
          <h1>{identity.eventName}</h1>
          <p>등록은 빠르게, 확인은 간단하게. 시작부터 현장까지 한 번에 이어집니다.</p>
          <div className="app-home__actions">
            <Link className="button button--primary" to="/public/register">
              수련회 등록
            </Link>
            <Link className="button button--secondary" to="/public/self-lookup">
              내 정보 조회
            </Link>
          </div>
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
