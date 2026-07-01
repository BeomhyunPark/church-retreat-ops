import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { getFeeRoster, updateFeeStatus } from "./adminApi";
import { EmptyState } from "../../shared/ui/EmptyState";
import { StatusMessage } from "../../shared/ui/StatusMessage";

const PAGE_SIZE_DEFAULT = 50;
const PAGE_SIZE_OPTIONS = [20, 50, 100] as const;

export function AdminFeesPage() {
  const queryClient = useQueryClient();
  const [keywordInput, setKeywordInput] = useState("");
  const [keyword, setKeyword] = useState("");
  const [feeFilter, setFeeFilter] = useState("ALL");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(PAGE_SIZE_DEFAULT);
  const feePaid = feeFilter === "ALL" ? undefined : feeFilter === "PAID";
  const query = useQuery({
    queryKey: ["admin", "fees", { feePaid, keyword, page, size }],
    queryFn: () => getFeeRoster({ feePaid, keyword, page, size })
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

  function clearFilters() {
    setKeywordInput("");
    setKeyword("");
    setFeeFilter("ALL");
    setPage(0);
  }

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Fees</p>
          <div className="page-heading-title-row">
            <h1>참가비 관리</h1>
            <span className="masking-badge">CHAIR 이상 변경 가능</span>
          </div>
        </div>
        <div className="result-count">
          <span className="result-count__label">검색 결과</span>
          <strong className="result-count__number">{query.data?.totalElements ?? "–"}</strong>
          <span className="result-count__unit">명</span>
        </div>
      </div>

      {query.isError ? <StatusMessage message={query.error.message} tone="error" /> : null}
      {mutation.isError ? <StatusMessage message={mutation.error.message} tone="error" /> : null}

      <div className="search-card" aria-label="참가비 목록 필터">
        <form className="search-card-top" onSubmit={(event) => { event.preventDefault(); setKeyword(keywordInput.trim()); setPage(0); }}>
          <div className="search-input-wrap">
            <svg className="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
              <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
            </svg>
            <input onChange={(event) => setKeywordInput(event.target.value)} placeholder="이름, 전화 끝자리, 공동체, 조 검색" type="search" value={keywordInput} />
          </div>
          <button className="button button--search-submit button--md" type="submit">검색</button>
          <button className="button button--ghost button--md" onClick={clearFilters} type="button">초기화</button>
        </form>
        <div className="search-card-divider" />
        <div className="filter-select-row">
          <span className="filter-label">참가비</span>
          <select className={feeFilter === "ALL" ? "filter-select" : "filter-select filter-select--active"} onChange={(event) => { setFeeFilter(event.target.value); setPage(0); }} value={feeFilter}>
            <option value="ALL">전체</option>
            <option value="PAID">납부</option>
            <option value="UNPAID">미납</option>
          </select>
          <label className="page-size-select-wrap" style={{ marginLeft: "auto" }}>
            <span>행</span>
            <select className="page-size-select" onChange={(event) => { setSize(Number(event.target.value)); setPage(0); }} value={size}>
              {PAGE_SIZE_OPTIONS.map((option) => <option key={option} value={option}>{option}</option>)}
            </select>
          </label>
        </div>
      </div>

      <div className="table-card roster-table-card">
        <table className="roster-table">
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
                  <Link className="table-link" to={`/admin/participants/${item.participantId}`}>{item.name}</Link>
                  <span className="table-note">
                    {item.gender === "MALE" ? "남" : "여"} · {item.birthYear}
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

      <div className="participant-table-footer">
        <span className="table-page-summary">현재 페이지 {roster.length}명 표시</span>
        {query.data ? (
          <div className="pagination-bar" aria-label="참가비 목록 페이지">
            <button className="button button--ghost" disabled={page <= 0} onClick={() => setPage((current) => Math.max(0, current - 1))} type="button">이전</button>
            <span>{query.data.totalPages === 0 ? 0 : page + 1} / {query.data.totalPages}</span>
            <button className="button button--ghost" disabled={page + 1 >= query.data.totalPages} onClick={() => setPage((current) => current + 1)} type="button">다음</button>
          </div>
        ) : null}
      </div>
    </section>
  );
}
