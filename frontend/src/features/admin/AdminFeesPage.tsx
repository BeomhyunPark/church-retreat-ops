import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getFeeRoster, updateFeeStatus } from "./adminApi";
import { EmptyState } from "../../shared/ui/EmptyState";
import { StatusMessage } from "../../shared/ui/StatusMessage";

export function AdminFeesPage() {
  const queryClient = useQueryClient();
  const [keyword, setKeyword] = useState("");
  const [feeFilter, setFeeFilter] = useState("ALL");
  const feePaid = feeFilter === "ALL" ? undefined : feeFilter === "PAID";
  const query = useQuery({
    queryKey: ["admin", "fees", { feePaid, keyword }],
    queryFn: () => getFeeRoster({ feePaid, keyword })
  });
  const mutation = useMutation({
    mutationFn: ({ participantId, nextFeePaid }: { participantId: number; nextFeePaid: boolean }) =>
      updateFeeStatus(
        participantId,
        nextFeePaid,
        nextFeePaid ? "관리자 화면에서 납부 확인" : "관리자 화면에서 미납 처리"
      ),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["admin", "fees"] });
    }
  });
  const roster = query.data?.content ?? [];

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Fees</p>
          <h1>참가비 관리</h1>
        </div>
        <span className="pill">CHAIR 이상 변경 가능</span>
      </div>

      {query.isError ? <StatusMessage message={query.error.message} tone="error" /> : null}
      {mutation.isError ? <StatusMessage message={mutation.error.message} tone="error" /> : null}

      <section className="filter-panel" aria-label="참가비 목록 필터">
        <label>
          검색
          <input
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="이름, 전화 끝자리, 공동체, 조"
            type="search"
            value={keyword}
          />
        </label>
        <label>
          참가비
          <select onChange={(event) => setFeeFilter(event.target.value)} value={feeFilter}>
            <option value="ALL">전체</option>
            <option value="PAID">납부</option>
            <option value="UNPAID">미납</option>
          </select>
        </label>
        <div className="filter-summary">
          <span>검색 결과</span>
          <strong>{query.data?.totalElements ?? 0}</strong>
        </div>
      </section>

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
              <th>처리</th>
            </tr>
          </thead>
          <tbody>
            {roster.map((item) => (
              <tr key={item.participantId}>
                <td>
                  <strong>{item.name}</strong>
                  <span className="table-note">
                    {item.gender} · {item.birthYear}
                  </span>
                </td>
                <td>{item.phoneLast4}</td>
                <td>
                  <span className={item.feePaid ? "status-pill status-pill--success" : "status-pill status-pill--warning"}>
                    {item.feePaid ? "납부" : "미납"}
                  </span>
                </td>
                <td>{item.churchCellName ?? "-"}</td>
                <td>{item.retreatGroupName ?? "-"}</td>
                <td>{item.feeStatusUpdatedAt ? new Date(item.feeStatusUpdatedAt).toLocaleString() : "-"}</td>
                <td>
                  <button
                    className={item.feePaid ? "table-action table-action--warning" : "table-action"}
                    disabled={mutation.isPending}
                    onClick={() => mutation.mutate({ participantId: item.participantId, nextFeePaid: !item.feePaid })}
                    type="button"
                  >
                    {item.feePaid ? "미납 처리" : "납부 처리"}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {query.isLoading ? <EmptyState title="참가비 목록을 불러오는 중입니다" message="잠시만 기다려 주세요." /> : null}
        {!query.isLoading && !roster.length ? (
          <EmptyState title="조건에 맞는 참가비 항목이 없습니다" message="검색어나 납부 상태 필터를 다시 확인해 주세요." />
        ) : null}
      </div>
    </section>
  );
}
