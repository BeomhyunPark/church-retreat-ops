import { Link } from "react-router-dom";

const steps = [
  {
    step: "1",
    title: "참가 등록",
    description: "처음이신가요? 이름과 연락처만 입력하면 바로 등록할 수 있어요.",
    to: "/public/register",
    cta: "등록하기"
  },
  {
    step: "2",
    title: "내 등록 조회",
    description: "등록 시 받은 조회 키로 내 정보를 확인하고 수정할 수 있어요.",
    to: "/public/self-lookup",
    cta: "조회하기"
  },
  {
    step: "3",
    title: "현장 체크인",
    description: "수련회 현장에 도착하면 여기서 체크인을 진행해 주세요.",
    to: "/public/check-in",
    cta: "체크인하기"
  }
];

export function PublicHomePage() {
  return (
    <section className="page-stack">
      <section className="hero-card">
        <p className="eyebrow">GMC Retreat</p>
        <h1>드림공동체 수련회에 오신 것을 환영합니다</h1>
        <p>등록부터 현장 체크인까지, 필요한 절차를 이 화면에서 차례로 안내해 드려요.</p>
        <div className="home-status">
          <strong>참가 등록 진행 중</strong>
          <span className="muted">등록 후 발급되는 조회 키는 본인 확인에 필요하니 꼭 저장해 주세요.</span>
        </div>
      </section>

      <section aria-label="바로가기" className="action-list">
        {steps.map((item) => (
          <article className="action-card" key={item.to}>
            <span className="action-card__step" aria-hidden="true">
              {item.step}
            </span>
            <div className="action-card__body">
              <h2>{item.title}</h2>
              <p className="muted">{item.description}</p>
            </div>
            <Link className="button button--primary" to={item.to}>
              {item.cta}
            </Link>
          </article>
        ))}
      </section>
    </section>
  );
}
