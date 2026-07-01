import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { getAdminRegistrations, getFeeRoster } from "./adminApi";

export function AdminDashboardPage() {
  const registrationsQuery = useQuery({
    queryKey: ["admin", "registrations", "summary"],
    queryFn: () => getAdminRegistrations()
  });
  const feesQuery = useQuery({
    queryKey: ["admin", "fees", "summary"],
    queryFn: () => getFeeRoster()
  });

  const participants = registrationsQuery.data?.totalElements ?? 0;
  const unpaid = feesQuery.data?.content.filter((item) => !item.feePaid).length ?? 0;
  const today = new Intl.DateTimeFormat("ko-KR", {
    month: "long",
    day: "numeric",
    weekday: "long"
  }).format(new Date());

  return (
    <section className="page-stack dashboard-page">
      <div className="dashboard-welcome">
        <div>
          <p className="eyebrow">Retreat operations</p>
          <h1>수련회 운영 현황</h1>
          <p className="muted">참가자 준비부터 현장 체크인까지, 지금 필요한 업무를 한곳에서 확인하세요.</p>
        </div>
        <span className="dashboard-date">{today}</span>
      </div>
      <div className="metric-grid">
        <MetricCard label="등록 인원" value={`${participants.toLocaleString()}명`} meta="전체 참가자 명단" tone="primary" to="/admin/participants" />
        <MetricCard label="미납 확인 필요" value={`${unpaid.toLocaleString()}명`} meta="현재 조회된 명단 기준" tone={unpaid > 0 ? "warning" : "success"} to="/admin/fees" />
        <MetricCard label="현장 체크인" value="바로가기" meta="도착자 확인 및 처리" tone="info" to="/admin/check-ins" />
      </div>
      <div className="dashboard-workspace">
        <section className="panel dashboard-queue">
          <div className="dashboard-section-heading">
            <div>
              <p className="eyebrow">Next actions</p>
              <h2>지금 확인할 운영 업무</h2>
            </div>
            <span className="status-pill status-pill--neutral">운영 순서</span>
          </div>
          <DashboardTask index="01" title="참가자 명단 점검" description="등록 상태와 참석 정보를 확인합니다." to="/admin/participants" />
          <DashboardTask index="02" title="참가비 미납 확인" description={unpaid > 0 ? `현재 조회된 명단에서 ${unpaid}명의 확인이 필요합니다.` : "현재 조회된 명단의 납부 확인이 완료되었습니다."} to="/admin/fees" />
          <DashboardTask index="03" title="수련회 조 편성 확인" description="조별 인원과 조장 배정을 최종 점검합니다." to="/admin/retreat-groups" />
          <DashboardTask index="04" title="현장 체크인 준비" description="참가자 도착 시 체크인 화면에서 처리합니다." to="/admin/check-ins" />
        </section>
        <aside className="panel dashboard-guide">
          <p className="eyebrow">Guide</p>
          <h2>운영 흐름 안내</h2>
          <ol>
            <li><span>준비</span><p>명단·참가비·조 편성을 먼저 확정합니다.</p></li>
            <li><span>안내</span><p>공지와 일정을 확인해 운영진에게 공유합니다.</p></li>
            <li><span>현장</span><p>도착 순서대로 체크인을 처리합니다.</p></li>
          </ol>
        </aside>
      </div>
    </section>
  );
}

function MetricCard({
  label,
  value,
  meta,
  tone,
  to
}: {
  label: string;
  value: string;
  meta: string;
  tone: "primary" | "success" | "warning" | "info";
  to: string;
}) {
  return (
    <Link className={`metric-card metric-card--${tone}`} to={to}>
      <div className="metric-card__top">
        <span>{label}</span>
        <span className="metric-card__arrow" aria-hidden="true">→</span>
      </div>
      <strong>{value}</strong>
      <small>{meta}</small>
    </Link>
  );
}

function DashboardTask({ index, title, description, to }: { index: string; title: string; description: string; to: string }) {
  return (
    <Link className="dashboard-task" to={to}>
      <span className="dashboard-task__index">{index}</span>
      <span className="dashboard-task__copy">
        <strong>{title}</strong>
        <small>{description}</small>
      </span>
      <span className="dashboard-task__arrow" aria-hidden="true">→</span>
    </Link>
  );
}
