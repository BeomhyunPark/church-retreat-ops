import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { cancelCheckIn, getCheckInRoster, manuallyCheckIn } from "./adminApi";
import { EmptyState } from "../../shared/ui/EmptyState";
import { StatusMessage } from "../../shared/ui/StatusMessage";

export function AdminCheckInsPage() {
  const queryClient = useQueryClient();
  const [keyword, setKeyword] = useState("");
  const [checkedInFilter, setCheckedInFilter] = useState("ALL");
  const checkedIn = checkedInFilter === "ALL" ? undefined : checkedInFilter === "CHECKED_IN";
  const query = useQuery({
    queryKey: ["admin", "check-ins", { checkedIn, keyword }],
    queryFn: () => getCheckInRoster({ checkedIn, keyword })
  });
  const checkInMutation = useMutation({
    mutationFn: manuallyCheckIn,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["admin", "check-ins"] });
    }
  });
  const cancelMutation = useMutation({
    mutationFn: (participantId: number) => cancelCheckIn(participantId, "관리자 화면에서 체크인 취소"),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["admin", "check-ins"] });
    }
  });
  const roster = query.data?.content ?? [];
  const mutationError = checkInMutation.error ?? cancelMutation.error;
  const actionPending = checkInMutation.isPending || cancelMutation.isPending;

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Check-ins</p>
          <h1>체크인 관리</h1>
        </div>
        <span className="pill">현장 도착자 처리</span>
      </div>

      {query.isError ? <StatusMessage message={query.error.message} tone="error" /> : null}
      {mutationError ? <StatusMessage message={mutationError.message} tone="error" /> : null}

      <section className="filter-panel" aria-label="체크인 목록 필터">
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
          체크인 상태
          <select onChange={(event) => setCheckedInFilter(event.target.value)} value={checkedInFilter}>
            <option value="ALL">전체</option>
            <option value="CHECKED_IN">체크인 완료</option>
            <option value="NOT_CHECKED_IN">미체크인</option>
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
              <th>상태</th>
              <th>공동체</th>
              <th>수련회 조</th>
              <th>처리 시각</th>
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
                  <span className={item.checkedIn ? "status-pill status-pill--success" : "status-pill status-pill--neutral"}>
                    {item.checkedIn ? "체크인 완료" : "미체크인"}
                  </span>
                </td>
                <td>{item.churchCellName ?? "-"}</td>
                <td>{item.retreatGroupName ?? "-"}</td>
                <td>{item.checkedInAt ? new Date(item.checkedInAt).toLocaleString() : "-"}</td>
                <td>
                  <button
                    className={item.checkedIn ? "table-action table-action--warning" : "table-action"}
                    disabled={actionPending}
                    onClick={() =>
                      item.checkedIn
                        ? cancelMutation.mutate(item.participantId)
                        : checkInMutation.mutate(item.participantId)
                    }
                    type="button"
                  >
                    {item.checkedIn ? "취소" : "체크인"}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {query.isLoading ? <EmptyState title="체크인 명단을 불러오는 중입니다" message="잠시만 기다려 주세요." /> : null}
        {!query.isLoading && !roster.length ? (
          <EmptyState title="조건에 맞는 체크인 항목이 없습니다" message="검색어나 체크인 상태 필터를 다시 확인해 주세요." />
        ) : null}
      </div>
    </section>
  );
}
