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
    queryFn: () => getFeeRoster()
  });

  const participants = registrationsQuery.data?.totalElements ?? 0;
  const unpaid = feesQuery.data?.content.filter((item) => !item.feePaid).length ?? 0;

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Operations</p>
          <h1>운영 대시보드</h1>
        </div>
      </div>
      <div className="metric-grid">
        <MetricCard label="등록 인원" value={participants.toLocaleString()} to="/admin/participants" />
        <MetricCard label="현재 페이지 미납" value={unpaid.toLocaleString()} to="/admin/fees" />
        <MetricCard label="체크인" value="준비 중" to="/admin/check-ins" />
      </div>
      <section className="panel">
        <h2>오늘 확인할 항목</h2>
        <p className="muted">
          등록 인원과 참가비 확인 현황을 먼저 점검하고, 현장 운영 전에는 체크인 화면에서 도착자 처리를 진행합니다.
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
