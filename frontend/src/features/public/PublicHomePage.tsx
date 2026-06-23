import { Link } from "react-router-dom";

export function PublicHomePage() {
  return (
    <section className="app-home">
      <div className="app-home__logo" aria-label="GMC Retreat">
        <span>GMC</span>
        <strong>RETREAT</strong>
      </div>

      <Link className="app-home__primary" to="/public/register">
        수련회 신청하기
      </Link>

      <div className="app-home__links" aria-label="보조 메뉴">
        <Link to="/public/self-lookup">내 정보 보기</Link>
        <Link to="/public/check-in">체크인</Link>
      </div>
    </section>
  );
}
