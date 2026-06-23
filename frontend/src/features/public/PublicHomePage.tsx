import { Link } from "react-router-dom";

export function PublicHomePage() {
  return (
    <section className="hero-card">
      <p className="eyebrow">GMC Retreat</p>
      <h1>드림공동체 수련회</h1>
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
