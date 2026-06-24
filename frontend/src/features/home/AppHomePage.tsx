import { Link } from "react-router-dom";
import { StatusMessage } from "../../shared/ui/StatusMessage";
import { useAppIdentity } from "../../shared/identity/appIdentity";
import { BrandHeader } from "../../shared/layout/BrandHeader";
import { AdminIcon } from "../../shared/ui/icons";

export function AppHomePage() {
  const { identity, isError, error } = useAppIdentity();

  return (
    <main className="app-home">
      <header className="app-home__top">
        <BrandHeader />
        <Link className="icon-link" to="/admin/login" aria-label="관리자">
          <AdminIcon />
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
