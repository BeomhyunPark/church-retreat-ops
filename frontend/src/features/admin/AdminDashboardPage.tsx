import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { getAdminRegistrations, getFeeRoster } from "./adminApi";

export function AdminDashboardPage() {
  const registrationsQuery = useQuery({
    queryKey: ["admin", "registrations", "summary"],
    queryFn: getAdminRegistrations
  });
  const feesQuery = useQuery({
    queryKey: ["admin", "fees", "summary"],
    queryFn: getFeeRoster
  });

  const participants = registrationsQuery.data?.totalElements ?? 0;
  const unpaid = feesQuery.data?.content.filter((item) => !item.feePaid).length ?? 0;

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Overview</p>
          <h1>운영 대시보드</h1>
        </div>
      </div>
      <div className="metric-grid">
        <MetricCard label="등록 인원" value={participants.toLocaleString()} to="/admin/participants" />
        <MetricCard label="현재 페이지 미납" value={unpaid.toLocaleString()} to="/admin/fees" />
        <MetricCard label="체크인" value="준비 중" to="/admin/check-ins" />
      </div>
      <section className="panel">
        <h2>다음 화면 확장 위치</h2>
        <p className="muted">
          공지, 일정, 체크인, 조 편성 API는 라우트가 준비되어 있습니다. 새 기능은 API 함수와 페이지 컴포넌트를 추가해
          붙이면 됩니다.
        </p>
      </section>
    </section>
  );
}

function MetricCard({ label, value, to }: { label: string; value: string; to: string }) {
  return (
    <Link className="metric-card" to={to}>
      <span>{label}</span>
      <strong>{value}</strong>
    </Link>
  );
}
