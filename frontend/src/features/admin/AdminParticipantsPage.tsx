import { useEffect, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link, useSearchParams } from "react-router-dom";
import { getAdminRegistrations, type AdminRegistrationFilters } from "./adminApi";
import { EmptyState } from "../../shared/ui/EmptyState";
import { StatusMessage } from "../../shared/ui/StatusMessage";

const PAGE_SIZE = 25;

const PRESETS: Array<{
  key: string;
  label: string;
  filters: Partial<AdminRegistrationFilters>;
}> = [
  { key: "unpaid", label: "미납자", filters: { feePaid: false, sort: "fee_unpaid_first" } },
  { key: "not-checked-in", label: "미체크인", filters: { checkedIn: false, sort: "check_in_pending_first" } },
  { key: "newcomer", label: "새가족", filters: { newcomer: true } },
  { key: "care-target", label: "돌봄", filters: { careTarget: true } },
  { key: "no-group", label: "조 미배정", filters: { retreatGroupAssigned: false, sort: "group_asc" } },
  { key: "no-cell", label: "셀 미지정", filters: { churchCellAssigned: false } },
  { key: "partial", label: "부분 참석", filters: { attendanceType: "PARTIAL" } },
  { key: "carpool", label: "카풀 필요", filters: { transportationNeed: "CARPOOL_NEEDED" } }
];

export function AdminParticipantsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const filters = useMemo(() => filtersFromSearchParams(searchParams), [searchParams]);
  const [keyword, setKeyword] = useState(filters.keyword ?? "");
  const hasAdvancedFilters = Boolean(
    filters.status ||
      filters.newcomer !== undefined ||
      filters.careTarget !== undefined ||
      filters.retreatGroupAssigned !== undefined ||
      filters.churchCellAssigned !== undefined ||
      filters.attendanceType ||
      filters.transportationNeed
  );

  useEffect(() => {
    setKeyword(filters.keyword ?? "");
  }, [filters.keyword]);

  const registrationsQuery = useQuery({
    queryKey: ["admin", "registrations", filters],
    queryFn: () => getAdminRegistrations(filters)
  });

  const page = registrationsQuery.data;
  const participants = page?.content ?? [];
  const currentPage = filters.page ?? 0;
  const activePreset = PRESETS.find((preset) => presetMatches(filters, preset.filters))?.key;

  const updateFilters = (next: Partial<AdminRegistrationFilters>) => {
    setSearchParams(searchParamsFromFilters({ ...filters, ...next, page: next.page ?? 0 }));
  };

  const applyPreset = (preset: (typeof PRESETS)[number]) => {
    setSearchParams(searchParamsFromFilters({ size: PAGE_SIZE, sort: "created_desc", ...preset.filters, page: 0 }));
  };

  const clearFilters = () => {
    setSearchParams(searchParamsFromFilters({ page: 0, size: PAGE_SIZE, sort: "created_desc" }));
  };

  const updateSort = (sort: AdminRegistrationFilters["sort"]) => {
    updateFilters({ sort });
  };

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Participants</p>
          <h1>참가자 관리</h1>
        </div>
        <span className="pill">목록 연락처는 마스킹 표시</span>
      </div>

      {registrationsQuery.isError ? (
        <StatusMessage message={registrationsQuery.error.message} tone="error" />
      ) : null}

      <section className="ops-toolbar" aria-label="참가자 빠른 필터">
        <div className="preset-row">
          {PRESETS.map((preset) => (
            <button
              className={activePreset === preset.key ? "preset-chip preset-chip--active" : "preset-chip"}
              key={preset.key}
              onClick={() => applyPreset(preset)}
              type="button"
            >
              {preset.label}
            </button>
          ))}
        </div>
      </section>

      <section className="participant-list-controls" aria-label="참가자 목록 필터">
        <form
          className="participant-search"
          onSubmit={(event) => {
            event.preventDefault();
            updateFilters({ keyword: keyword.trim() || undefined });
          }}
        >
          <label>
            검색
            <input
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="이름, 연락처 끝 4자리, 공동체, 조"
              type="search"
              value={keyword}
            />
          </label>
          <button className="button button--secondary" type="submit">
            검색
          </button>
        </form>
        <div className="quick-filter-group" aria-label="핵심 상태 필터">
          <ToggleFilter active={filters.feePaid === false} onClick={() => updateFilters({ feePaid: filters.feePaid === false ? undefined : false })}>
            미납
          </ToggleFilter>
          <ToggleFilter
            active={filters.checkedIn === false}
            onClick={() => updateFilters({ checkedIn: filters.checkedIn === false ? undefined : false })}
          >
            미체크인
          </ToggleFilter>
          <ToggleFilter active={filters.status === "CANCELLED"} onClick={() => updateFilters({ status: filters.status === "CANCELLED" ? undefined : "CANCELLED" })}>
            취소
          </ToggleFilter>
        </div>
        <div className="filter-summary">
          <span>결과</span>
          <strong>{page ? `${page.totalElements}명` : "-"}</strong>
        </div>
        <button className="button button--ghost filter-clear" onClick={clearFilters} type="button">
          초기화
        </button>
      </section>

      <details className="advanced-filter-panel" open={hasAdvancedFilters}>
        <summary>상세 필터</summary>
        <div className="filter-panel filter-panel--participants" aria-label="참가자 상세 필터">
          <label>
            등록 상태
            <select
              onChange={(event) => updateFilters({ status: valueOrUndefined(event.target.value) as AdminRegistrationFilters["status"] })}
              value={filters.status ?? ""}
            >
              <option value="">전체</option>
              <option value="REGISTERED">등록 완료</option>
              <option value="CANCELLED">취소</option>
            </select>
          </label>
          <label>
            태그
            <select
              onChange={(event) => {
                const value = event.target.value;
                updateFilters({
                  newcomer: value === "NEWCOMER" ? true : undefined,
                  careTarget: value === "CARE_TARGET" ? true : undefined
                });
              }}
              value={filters.newcomer ? "NEWCOMER" : filters.careTarget ? "CARE_TARGET" : ""}
            >
              <option value="">전체</option>
              <option value="NEWCOMER">새가족</option>
              <option value="CARE_TARGET">돌봄</option>
            </select>
          </label>
          <label>
            배정
            <select
              onChange={(event) => {
                const value = event.target.value;
                updateFilters({
                  retreatGroupAssigned: value === "NO_GROUP" ? false : undefined,
                  churchCellAssigned: value === "NO_CELL" ? false : undefined
                });
              }}
              value={filters.retreatGroupAssigned === false ? "NO_GROUP" : filters.churchCellAssigned === false ? "NO_CELL" : ""}
            >
              <option value="">전체</option>
              <option value="NO_GROUP">조 미배정</option>
              <option value="NO_CELL">셀 미지정</option>
            </select>
          </label>
          <label>
            참석
            <select
              onChange={(event) => updateFilters({ attendanceType: valueOrUndefined(event.target.value) as AdminRegistrationFilters["attendanceType"] })}
              value={filters.attendanceType ?? ""}
            >
              <option value="">전체</option>
              <option value="FULL">전체 참석</option>
              <option value="PARTIAL">부분 참석</option>
              <option value="WORSHIP_ONLY">예배만</option>
            </select>
          </label>
          <label>
            교통
            <select
              onChange={(event) =>
                updateFilters({ transportationNeed: valueOrUndefined(event.target.value) as AdminRegistrationFilters["transportationNeed"] })
              }
              value={filters.transportationNeed ?? ""}
            >
              <option value="">전체</option>
              <option value="CARPOOL_NEEDED">카풀 필요</option>
              <option value="CARPOOL_AVAILABLE">카풀 제공</option>
            </select>
          </label>
        </div>
      </details>

      <div className="table-card participant-table-card">
        <table className="participant-table">
          <thead>
            <tr>
              <th>
                <SortableHeader active={filters.sort === "name_asc"} label="참가자" onClick={() => updateSort("name_asc")} />
              </th>
              <th>연락처</th>
              <th>등록 상태</th>
              <th>
                <SortableHeader active={filters.sort === "fee_unpaid_first"} label="참가비" onClick={() => updateSort("fee_unpaid_first")} />
              </th>
              <th>
                <SortableHeader active={filters.sort === "check_in_pending_first"} label="체크인" onClick={() => updateSort("check_in_pending_first")} />
              </th>
              <th>참석/교통</th>
              <th>소속</th>
              <th>
                <SortableHeader active={filters.sort === "group_asc"} label="수련회 조" onClick={() => updateSort("group_asc")} />
              </th>
              <th>
                <SortableHeader active={(filters.sort ?? "created_desc") === "created_desc"} label="등록일" onClick={() => updateSort("created_desc")} />
              </th>
            </tr>
          </thead>
          <tbody>
            {participants.map((item) => (
              <tr key={item.id}>
                <td className="participant-name-cell">
                  <Link className="table-link" to={`/admin/participants/${item.id}`}>
                    {item.name}
                  </Link>
                  <span className="table-note">
                    {item.gender === "FEMALE" ? "여성" : "남성"} · {item.birthYear}
                  </span>
                  <TagList newcomer={item.newcomer} careTarget={item.careTarget} />
                </td>
                <td>{item.phoneNumber}</td>
                <td>
                  <StatusPill tone={item.status === "REGISTERED" ? "success" : "danger"}>
                    {item.status === "REGISTERED" ? "등록 완료" : "취소"}
                  </StatusPill>
                </td>
                <td>
                  <StatusPill tone={item.feePaid ? "success" : "warning"}>{item.feePaid ? "납부" : "미납"}</StatusPill>
                </td>
                <td>
                  <StatusPill tone={item.checkedIn ? "success" : "neutral"}>{item.checkedIn ? "완료" : "미완료"}</StatusPill>
                </td>
                <td>
                  <strong className="cell-primary">{formatAttendance(item.attendanceType)}</strong>
                  <span className="table-note">{formatTransportationSummary(item)}</span>
                </td>
                <td>
                  <strong className="cell-primary">{item.churchCellName ?? item.churchCellDepartment ?? "-"}</strong>
                  <span className="table-note">{item.middleGroupName ?? "중그룹 미지정"}</span>
                </td>
                <td>
                  <strong className="cell-primary">{item.retreatGroupName ?? "-"}</strong>
                  {item.retreatGroupLeader ? <span className="table-note">조장</span> : null}
                </td>
                <td>{formatDate(item.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {registrationsQuery.isLoading ? (
          <EmptyState title="참가자 목록을 불러오는 중입니다" message="잠시만 기다려 주세요." />
        ) : null}
        {!registrationsQuery.isLoading && !participants.length ? (
          <EmptyState title="조건에 맞는 참가자가 없습니다" message="검색어나 필터 조건을 조금 넓혀 보세요." />
        ) : null}
      </div>

      {page ? (
        <div className="pagination-bar" aria-label="참가자 목록 페이지">
          <button className="button button--ghost" disabled={currentPage <= 0} onClick={() => updateFilters({ page: currentPage - 1 })} type="button">
            이전
          </button>
          <span>
            {page.totalPages === 0 ? 0 : currentPage + 1} / {page.totalPages}
          </span>
          <button
            className="button button--ghost"
            disabled={currentPage + 1 >= page.totalPages}
            onClick={() => updateFilters({ page: currentPage + 1 })}
            type="button"
          >
            다음
          </button>
        </div>
      ) : null}
    </section>
  );
}

function ToggleFilter({ active, children, onClick }: { active: boolean; children: string; onClick: () => void }) {
  return (
    <button className={active ? "toggle-chip toggle-chip--active" : "toggle-chip"} onClick={onClick} type="button">
      {children}
    </button>
  );
}

function SortableHeader({ active, label, onClick }: { active: boolean; label: string; onClick: () => void }) {
  return (
    <button className={active ? "sort-header sort-header--active" : "sort-header"} onClick={onClick} type="button">
      <span>{label}</span>
      <span aria-hidden="true">{active ? "↓" : "↕"}</span>
    </button>
  );
}

function filtersFromSearchParams(params: URLSearchParams): AdminRegistrationFilters {
  return {
    page: numberParam(params.get("page"), 0),
    size: PAGE_SIZE,
    keyword: valueOrUndefined(params.get("keyword") ?? ""),
    status: valueOrUndefined(params.get("status") ?? "") as AdminRegistrationFilters["status"],
    feePaid: booleanOrUndefined(params.get("feePaid") ?? ""),
    newcomer: booleanOrUndefined(params.get("newcomer") ?? ""),
    careTarget: booleanOrUndefined(params.get("careTarget") ?? ""),
    checkedIn: booleanOrUndefined(params.get("checkedIn") ?? ""),
    retreatGroupAssigned: booleanOrUndefined(params.get("retreatGroupAssigned") ?? ""),
    churchCellAssigned: booleanOrUndefined(params.get("churchCellAssigned") ?? ""),
    attendanceType: valueOrUndefined(params.get("attendanceType") ?? "") as AdminRegistrationFilters["attendanceType"],
    transportationNeed: valueOrUndefined(params.get("transportationNeed") ?? "") as AdminRegistrationFilters["transportationNeed"],
    sort: (valueOrUndefined(params.get("sort") ?? "") as AdminRegistrationFilters["sort"]) ?? "created_desc"
  };
}

function searchParamsFromFilters(filters: AdminRegistrationFilters) {
  const params = new URLSearchParams();
  params.set("page", String(filters.page ?? 0));
  if (filters.keyword) params.set("keyword", filters.keyword);
  if (filters.status) params.set("status", filters.status);
  if (filters.feePaid !== undefined) params.set("feePaid", String(filters.feePaid));
  if (filters.newcomer !== undefined) params.set("newcomer", String(filters.newcomer));
  if (filters.careTarget !== undefined) params.set("careTarget", String(filters.careTarget));
  if (filters.checkedIn !== undefined) params.set("checkedIn", String(filters.checkedIn));
  if (filters.retreatGroupAssigned !== undefined) params.set("retreatGroupAssigned", String(filters.retreatGroupAssigned));
  if (filters.churchCellAssigned !== undefined) params.set("churchCellAssigned", String(filters.churchCellAssigned));
  if (filters.attendanceType) params.set("attendanceType", filters.attendanceType);
  if (filters.transportationNeed) params.set("transportationNeed", filters.transportationNeed);
  if (filters.sort && filters.sort !== "created_desc") params.set("sort", filters.sort);
  return params;
}

function presetMatches(filters: AdminRegistrationFilters, preset: Partial<AdminRegistrationFilters>) {
  return Object.entries(preset).every(([key, value]) => filters[key as keyof AdminRegistrationFilters] === value);
}

function numberParam(value: string | null, fallback: number) {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : fallback;
}

function valueOrUndefined(value: string) {
  return value.trim() ? value : undefined;
}

function booleanOrUndefined(value: string) {
  if (value === "true") return true;
  if (value === "false") return false;
  return undefined;
}

function stringFromBoolean(value?: boolean) {
  if (value === true) return "true";
  if (value === false) return "false";
  return "";
}

function TagList({ newcomer, careTarget }: { newcomer: boolean; careTarget: boolean }) {
  if (!newcomer && !careTarget) return null;
  return (
    <div className="tag-list">
      {newcomer ? <span className="mini-tag">새가족</span> : null}
      {careTarget ? <span className="mini-tag mini-tag--warning">돌봄</span> : null}
    </div>
  );
}

function StatusPill({ tone, children }: { tone: "success" | "warning" | "danger" | "neutral"; children: string }) {
  return <span className={`status-pill status-pill--${tone}`}>{children}</span>;
}

function formatAttendance(value: string) {
  switch (value) {
    case "FULL":
      return "전체 참석";
    case "PARTIAL":
      return "부분 참석";
    case "WORSHIP_ONLY":
      return "예배만";
    default:
      return value;
  }
}

function formatTransportationSummary(item: { inboundTransportationMethod: string | null; outboundTransportationMethod: string | null }) {
  const inbound = formatTransportation(item.inboundTransportationMethod);
  const outbound = formatTransportation(item.outboundTransportationMethod);
  return inbound === outbound ? inbound : `${inbound} / ${outbound}`;
}

function formatTransportation(value: string | null) {
  switch (value) {
    case "OWN_CAR":
      return "개인차량";
    case "GROUP_BUS":
      return "단체버스";
    case "WORSHIP_SHUTTLE":
      return "경배 셔틀";
    case "PUBLIC_TRANSIT":
      return "대중교통";
    case "CARPOOL_NEEDED":
      return "카풀 필요";
    case "NOT_DECIDED":
      return "미정";
    default:
      return "-";
  }
}

function formatDate(value: string) {
  return new Date(value).toLocaleDateString();
}
