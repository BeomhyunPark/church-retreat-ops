import { Link } from "react-router-dom";

export function PublicHomePage() {
  return (
    <section className="hero-card">
      <p className="eyebrow">지구촌교회 청년2부 드림공동체</p>
      <h1>수련회 등록</h1>
      <p>
        환영합니다.
      </p>
      <div className="stack">
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
