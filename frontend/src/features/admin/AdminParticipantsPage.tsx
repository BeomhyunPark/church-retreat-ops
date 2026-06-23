import { useQuery } from "@tanstack/react-query";
import { getAdminRegistrations } from "./adminApi";
import { StatusMessage } from "../../shared/ui/StatusMessage";

export function AdminParticipantsPage() {
  const query = useQuery({
    queryKey: ["admin", "registrations"],
    queryFn: getAdminRegistrations
  });

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Admin</p>
          <h1>참가자 관리</h1>
        </div>
        <span className="pill">상세 조회 시 개인정보 접근 로그가 남습니다</span>
      </div>

      {query.isError ? <StatusMessage message={query.error.message} tone="error" /> : null}

      <div className="table-card">
        <table>
          <thead>
            <tr>
              <th>이름</th>
              <th>연락처</th>
              <th>상태</th>
              <th>참가비</th>
              <th>공동체</th>
              <th>수련회 조</th>
            </tr>
          </thead>
          <tbody>
            {(query.data?.content ?? []).map((item) => (
              <tr key={item.id}>
                <td>
                  <strong>{item.name}</strong>
                  {item.newcomer ? <span className="table-note">새가족</span> : null}
                  {item.careTarget ? <span className="table-note">돌봄</span> : null}
                </td>
                <td>{item.phoneNumber}</td>
                <td>{item.status}</td>
                <td>{item.feePaid ? "납부" : "미납"}</td>
                <td>{item.churchCellName ?? item.churchCellDepartment ?? "-"}</td>
                <td>{item.retreatGroupName ?? "-"}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {query.isLoading ? <p className="table-empty">불러오는 중...</p> : null}
        {!query.isLoading && !query.data?.content.length ? <p className="table-empty">참가자가 없습니다.</p> : null}
      </div>
    </section>
  );
}
