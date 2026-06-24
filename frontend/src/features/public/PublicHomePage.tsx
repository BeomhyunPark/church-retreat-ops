import { Link } from "react-router-dom";
import { useAppIdentity } from "../../shared/identity/appIdentity";

export function PublicHomePage() {
  const { identity } = useAppIdentity();

  return (
    <section className="hero-card">
      <p className="eyebrow">{identity.organizationName}</p>
      <h1>{identity.eventName}</h1>
      <p>등록, 조회, 현장 체크인을 한 곳에서 확인할 수 있습니다.</p>
      <div className="home-status">
        <strong>참가 등록 진행 중</strong>
        <span className="muted">등록 후 발급되는 조회 키는 본인 확인에 필요합니다.</span>
      </div>
      <div className="hero-actions">
        <Link className="button button--primary" to="/public/register">
          참가 등록하기
        </Link>
        <Link className="button button--secondary" to="/public/self-lookup">
          내 등록 조회
        </Link>
      </div>
    </section>
  );
}
