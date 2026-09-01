import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { cancelCheckIn, checkInByQr, getCheckInRoster, manuallyCheckIn, type CheckInRosterItem } from "./adminApi";
import { EmptyState } from "../../shared/ui/EmptyState";
import { StatusMessage } from "../../shared/ui/StatusMessage";
import { AdminQrScanner } from "./AdminQrScanner";

const PAGE_SIZE_DEFAULT = 50;
const PAGE_SIZE_OPTIONS = [20, 50, 100] as const;

export function AdminCheckInsPage() {
  const queryClient = useQueryClient();
  const [keywordInput, setKeywordInput] = useState("");
  const [keyword, setKeyword] = useState("");
  const [checkedInFilter, setCheckedInFilter] = useState("ALL");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(PAGE_SIZE_DEFAULT);
  const [scannerOpen, setScannerOpen] = useState(false);
  const [scanResult, setScanResult] = useState<CheckInRosterItem | null>(null);
  const checkedIn = checkedInFilter === "ALL" ? undefined : checkedInFilter === "CHECKED_IN";
  const query = useQuery({
    queryKey: ["admin", "check-ins", { checkedIn, keyword, page, size }],
    queryFn: () => getCheckInRoster({ checkedIn, keyword, page, size })
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
  const qrMutation = useMutation({
    mutationFn: checkInByQr,
    onSuccess: (result) => {
      setScanResult(result);
      setScannerOpen(false);
      void queryClient.invalidateQueries({ queryKey: ["admin", "check-ins"] });
    }
  });
  const roster = query.data?.content ?? [];
  const mutationError = qrMutation.error ?? checkInMutation.error ?? cancelMutation.error;
  const actionPending = qrMutation.isPending || checkInMutation.isPending || cancelMutation.isPending;

  function clearFilters() {
    setKeywordInput("");
    setKeyword("");
    setCheckedInFilter("ALL");
    setPage(0);
  }

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Check-ins</p>
          <div className="page-heading-title-row">
            <h1>체크인 관리</h1>
            <span className="masking-badge">현장 도착자 처리</span>
          </div>
        </div>
        <div className="result-count">
          <span className="result-count__label">검색 결과</span>
          <strong className="result-count__number">{query.data?.totalElements ?? "–"}</strong>
          <span className="result-count__unit">명</span>
        </div>
      </div>

      {query.isError ? <StatusMessage message={query.error.message} tone="error" /> : null}
      {mutationError ? <StatusMessage message={mutationError.message} tone="error" /> : null}
      {scanResult ? (
        <div className="qr-checkin-success" role="status">
          <div>
            <span className="status-pill status-pill--success">QR 체크인 완료</span>
            <strong>{scanResult.name}</strong>
            <small>{scanResult.retreatGroupName ?? "조 미배정"} · {scanResult.checkedInAt ? new Date(scanResult.checkedInAt).toLocaleString("ko-KR") : "처리 완료"}</small>
          </div>
          <button className="button button--outline button--sm" onClick={() => { setScanResult(null); setScannerOpen(true); }} type="button">다음 참가자 스캔</button>
        </div>
      ) : null}

      <section className="qr-checkin-entry">
        <div>
          <p className="eyebrow">Arrival check-in</p>
          <h2>도착 QR 체크인</h2>
          <p className="muted">수련회장에 도착한 참가자의 QR을 운영자 기기로 스캔합니다.</p>
        </div>
        <button className="button button--primary" onClick={() => { setScanResult(null); setScannerOpen(true); }} type="button">카메라로 QR 스캔</button>
      </section>

      {scannerOpen ? <AdminQrScanner pending={qrMutation.isPending} onClose={() => setScannerOpen(false)} onDetected={(token) => qrMutation.mutate(token)} /> : null}

      <div className="search-card" aria-label="체크인 목록 필터">
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
          <span className="filter-label">체크인</span>
          <select className={checkedInFilter === "ALL" ? "filter-select" : "filter-select filter-select--active"} onChange={(event) => { setCheckedInFilter(event.target.value); setPage(0); }} value={checkedInFilter}>
            <option value="ALL">전체</option>
            <option value="CHECKED_IN">체크인 완료</option>
            <option value="NOT_CHECKED_IN">미체크인</option>
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
                  <Link className="table-link" to={`/admin/participants/${item.participantId}`}>{item.name}</Link>
                  <span className="table-note">
                    {item.gender === "MALE" ? "남" : "여"} · {item.birthYear}
                  </span>
                </td>
                <td>{item.phoneLast4}</td>
                <td>
                  <span className={item.checkedIn ? "status-pill status-pill--success" : "status-pill status-pill--neutral"}>
                    {item.checkedIn ? "체크인 완료" : "미체크인"}
                  </span>
                </td>
                <td>{item.cellName ?? "-"}</td>
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
                    {item.checkedIn ? "취소" : "수동 처리"}
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

      <div className="participant-table-footer">
        <span className="table-page-summary">현재 페이지 {roster.length}명 표시</span>
        {query.data ? (
          <div className="pagination-bar" aria-label="체크인 목록 페이지">
            <button className="button button--ghost" disabled={page <= 0} onClick={() => setPage((current) => Math.max(0, current - 1))} type="button">이전</button>
            <span>{query.data.totalPages === 0 ? 0 : page + 1} / {query.data.totalPages}</span>
            <button className="button button--ghost" disabled={page + 1 >= query.data.totalPages} onClick={() => setPage((current) => current + 1)} type="button">다음</button>
          </div>
        ) : null}
      </div>
    </section>
  );
}
