import { useQuery } from "@tanstack/react-query";
import { getFeeRoster } from "./adminApi";
import { StatusMessage } from "../../shared/ui/StatusMessage";

export function AdminFeesPage() {
  const query = useQuery({
    queryKey: ["admin", "fees"],
    queryFn: getFeeRoster
  });

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Admin</p>
          <h1>참가비 관리</h1>
        </div>
        <span className="pill">CHAIR 이상 변경 가능</span>
      </div>

      {query.isError ? <StatusMessage message={query.error.message} tone="error" /> : null}

      <div className="table-card">
        <table>
          <thead>
            <tr>
              <th>이름</th>
              <th>전화 끝자리</th>
              <th>참가비</th>
              <th>공동체</th>
              <th>수련회 조</th>
              <th>변경 시각</th>
            </tr>
          </thead>
          <tbody>
            {(query.data?.content ?? []).map((item) => (
              <tr key={item.participantId}>
                <td>
                  <strong>{item.name}</strong>
                  <span className="table-note">
                    {item.gender} · {item.birthYear}
                  </span>
                </td>
                <td>{item.phoneLast4}</td>
                <td>{item.feePaid ? "납부" : "미납"}</td>
                <td>{item.churchCellName ?? "-"}</td>
                <td>{item.retreatGroupName ?? "-"}</td>
                <td>{item.feeStatusUpdatedAt ? new Date(item.feeStatusUpdatedAt).toLocaleString() : "-"}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {query.isLoading ? <p className="table-empty">불러오는 중...</p> : null}
        {!query.isLoading && !query.data?.content.length ? <p className="table-empty">참가비 항목이 없습니다.</p> : null}
      </div>
    </section>
  );
}
