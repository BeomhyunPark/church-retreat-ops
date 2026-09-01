import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { keepPreviousData, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useSearchParams } from "react-router-dom";
import {
  getAdminRegistrations,
  getAdminPreferences,
  updateAdminPreferences,
  type AdminRegistration,
  type AdminRegistrationFilters
} from "./adminApi";
import { EmptyState } from "../../shared/ui/EmptyState";
import { StatusMessage } from "../../shared/ui/StatusMessage";

const PAGE_SIZE_DEFAULT = 50;
const PAGE_SIZE_ALL = 9999;
const PAGE_SIZE_OPTIONS = [10, 20, 30, 50, 100, PAGE_SIZE_ALL] as const;

// ── Column definitions ────────────────────────────────────────────────────────

type ColumnKey =
  | "name" | "birthYear" | "gender" | "phone" | "middleGroup" | "cell"
  | "attendance" | "group" | "transportation" | "feePaid" | "checkedIn"
  | "createdAt" | "participantUpdatedAt" | "special";

const DEFAULT_COL_ORDER: ColumnKey[] = [
  "name", "birthYear", "gender", "phone", "middleGroup", "cell",
  "attendance", "group", "transportation", "feePaid", "checkedIn", "createdAt", "participantUpdatedAt", "special"
];

const COL_LABEL: Record<ColumnKey, string> = {
  name: "이름", birthYear: "또래", gender: "성별", phone: "연락처",
  middleGroup: "중그룹", cell: "셀", attendance: "참석여부", group: "조",
  transportation: "교통", feePaid: "참가비", checkedIn: "체크인",
  createdAt: "등록일", participantUpdatedAt: "본인 수정", special: "특이사항"
};

// bidirectional: [primaryAsc, primaryDesc]
const COL_BISORT: Partial<Record<ColumnKey, [string, string]>> = {
  name: ["name_asc", "name_desc"],
  birthYear: ["birth_year_asc", "birth_year_desc"],
  gender: ["gender_asc", "gender_desc"],
  phone: ["phone_asc", "phone_desc"],
  middleGroup: ["middle_group_asc", "middle_group_desc"],
  cell: ["cell_asc", "cell_desc"],
  attendance: ["attendance_asc", "attendance_desc"],
  transportation: ["transport_asc", "transport_desc"],
  createdAt: ["created_desc", "created_asc"],
};

const COL_ONEWAY: Partial<Record<ColumnKey, string>> = {
  group: "group_asc",
  feePaid: "fee_unpaid_first",
  checkedIn: "check_in_pending_first",
  special: "special_first",
};

const TD_CLASS: Record<ColumnKey, string> = {
  name:          "participant-name-cell",
  birthYear:     "participant-birth-year-cell",
  gender:        "participant-gender-cell",
  phone:         "participant-phone-cell",
  middleGroup:   "participant-middle-group-cell",
  cell:          "participant-cell-cell",
  attendance:    "participant-attendance-cell",
  group:         "participant-group-cell",
  transportation:"participant-transportation-cell",
  feePaid:       "participant-fee-paid-cell",
  checkedIn:     "participant-checked-in-cell",
  createdAt:     "participant-created-at-cell",
  participantUpdatedAt: "participant-updated-at-cell",
  special:       "participant-special-cell",
};

// ── Prefs keys ────────────────────────────────────────────────────────────────

const PREFS_WIDTHS_KEY = "participantTableColWidthsV2";
const PREFS_ORDER_KEY = "participantTableColOrder";
const PREFERENCES_QUERY_KEY = ["admin", "preferences"] as const;
const MIN_COL_WIDTH = 50;

// ── Helpers ───────────────────────────────────────────────────────────────────

function measureNaturalColWidths(table: HTMLTableElement): number[] {
  const clone = table.cloneNode(true) as HTMLTableElement;
  clone.style.cssText = "position:fixed;top:-9999px;left:0;visibility:hidden;table-layout:auto;width:auto;";
  clone.querySelectorAll("col").forEach((el) => el.remove());
  clone.querySelectorAll(".col-resize-handle").forEach((el) => el.remove());
  document.body.appendChild(clone);
  const ths = Array.from(clone.querySelectorAll("thead th")) as HTMLElement[];
  const widths = ths.map((th) => th.getBoundingClientRect().width);
  document.body.removeChild(clone);
  return widths;
}

// ── Hook: column resize + drag-and-drop reorder ───────────────────────────────

function useColumnCustomization() {
  const queryClient = useQueryClient();
  const tableRef = useRef<HTMLTableElement>(null);

  const [customColOrder, setColOrder] = useState<ColumnKey[] | null | undefined>(undefined);
  const [customWidths, setWidths] = useState<Record<ColumnKey, number> | null | undefined>(undefined);
  const [draggedKey, setDraggedKey] = useState<ColumnKey | null>(null);
  const [dragOverKey, setDragOverKey] = useState<ColumnKey | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);

  // refs so mouse-event closures always read the latest values
  const colOrderRef = useRef<ColumnKey[] | null>(null);
  const widthsRef = useRef<Record<ColumnKey, number> | null>(null);
  const saveQueueRef = useRef<Promise<void>>(Promise.resolve());
  const saveVersionRef = useRef(0);
  const prefsQuery = useQuery({
    queryKey: PREFERENCES_QUERY_KEY,
    queryFn: getAdminPreferences,
    staleTime: Infinity
  });

  const savedOrder = prefsQuery.data?.[PREFS_ORDER_KEY];
  const savedWidths = prefsQuery.data?.[PREFS_WIDTHS_KEY];
  const colOrder = customColOrder === undefined
    ? Array.isArray(savedOrder) && savedOrder.length === DEFAULT_COL_ORDER.length
      ? savedOrder as ColumnKey[]
      : null
    : customColOrder;
  const widths = customWidths === undefined
    ? savedWidths && typeof savedWidths === "object" && !Array.isArray(savedWidths)
      ? savedWidths as Record<ColumnKey, number>
      : null
    : customWidths;
  useEffect(() => { colOrderRef.current = colOrder; }, [colOrder]);
  useEffect(() => { widthsRef.current = widths; }, [widths]);

  const savePreferences = useCallback((patch: Record<string, unknown>) => {
    const current = queryClient.getQueryData<Record<string, unknown>>(PREFERENCES_QUERY_KEY) ?? {};
    const next = { ...current, ...patch };
    const version = ++saveVersionRef.current;

    // Keep later changes based on the latest local preferences while requests are queued.
    queryClient.setQueryData(PREFERENCES_QUERY_KEY, next);
    setSaveError(null);
    saveQueueRef.current = saveQueueRef.current
      .catch(() => undefined)
      .then(() => updateAdminPreferences(next))
      .then(() => undefined)
      .catch((error: unknown) => {
        if (version !== saveVersionRef.current) return;
        setSaveError(error instanceof Error ? error.message : "테이블 설정을 저장하지 못했습니다.");
        void queryClient.invalidateQueries({ queryKey: PREFERENCES_QUERY_KEY });
      });
  }, [queryClient]);

  const effectiveOrder = colOrder ?? DEFAULT_COL_ORDER;

  const getColWidth = useCallback((key: ColumnKey): string | undefined => {
    const w = widths?.[key];
    return w !== undefined ? `${w}%` : undefined;
  }, [widths]);

  const resetOrder = useCallback(() => {
    colOrderRef.current = null;
    setColOrder(null);
    savePreferences({ [PREFS_ORDER_KEY]: null });
  }, [savePreferences]);

  const resetWidths = useCallback(() => {
    widthsRef.current = null;
    setWidths(null);
    savePreferences({ [PREFS_WIDTHS_KEY]: null });
  }, [savePreferences]);

  // Resize: same algorithm as before, widths now saved as Record<ColumnKey, number>
  const startResize = useCallback((colIndex: number, startX: number) => {
    const table = tableRef.current;
    if (!table) return;

    const ths = Array.from(table.querySelectorAll("thead th")) as HTMLElement[];
    const cols = Array.from(table.querySelectorAll("col")) as HTMLElement[];
    const startWidths = ths.map((th) => th.offsetWidth);
    const startThis = startWidths[colIndex];
    const startNext = startWidths[colIndex + 1];
    if (startNext === undefined) return;

    const naturalWidths = measureNaturalColWidths(table);
    const minThis = Math.max(MIN_COL_WIDTH, naturalWidths[colIndex] ?? 0);
    const minNext = Math.max(MIN_COL_WIDTH, naturalWidths[colIndex + 1] ?? 0);
    const tableWidth = table.offsetWidth;
    const totalTwo = startThis + startNext;
    const clamp = (raw: number) => Math.min(totalTwo - minNext, Math.max(minThis, raw));

    document.body.style.userSelect = "none";
    document.body.style.cursor = "col-resize";

    const onMouseMove = (e: MouseEvent) => {
      const t = clamp(startThis + (e.clientX - startX));
      cols[colIndex].style.width = `${t}px`;
      cols[colIndex + 1].style.width = `${totalTwo - t}px`;
    };

    const onMouseUp = (e: MouseEvent) => {
      document.body.style.userSelect = "";
      document.body.style.cursor = "";
      document.removeEventListener("mousemove", onMouseMove);
      document.removeEventListener("mouseup", onMouseUp);

      const t = clamp(startThis + (e.clientX - startX));
      const finalPx = [...startWidths];
      finalPx[colIndex] = t;
      finalPx[colIndex + 1] = totalTwo - t;
      const pct = finalPx.map((w) => Math.round((w / tableWidth) * 1000) / 10);

      const order = colOrderRef.current ?? DEFAULT_COL_ORDER;
      const next: Record<string, number> = { ...(widthsRef.current ?? {}) };
      order.forEach((key, i) => { next[key] = pct[i]; });

      widthsRef.current = next as Record<ColumnKey, number>;
      setWidths(next as Record<ColumnKey, number>);
      savePreferences({ [PREFS_WIDTHS_KEY]: next });
    };

    document.addEventListener("mousemove", onMouseMove);
    document.addEventListener("mouseup", onMouseUp);
  }, [savePreferences]);

  // DnD: reorder columns
  const onDragStart = useCallback((key: ColumnKey, e: React.DragEvent) => {
    e.dataTransfer.effectAllowed = "move";
    setDraggedKey(key);
  }, []);

  const onDragOver = useCallback((key: ColumnKey, e: React.DragEvent) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = "move";
    if (key !== draggedKey) setDragOverKey(key);
  }, [draggedKey]);

  const onDrop = useCallback((targetKey: ColumnKey, e: React.DragEvent) => {
    e.preventDefault();
    const fromKey = draggedKey;
    setDraggedKey(null);
    setDragOverKey(null);
    if (!fromKey || fromKey === targetKey) return;

    const order = colOrderRef.current ?? DEFAULT_COL_ORDER;
    const next = [...order];
    next.splice(next.indexOf(fromKey), 1);
    next.splice(next.indexOf(targetKey), 0, fromKey);

    colOrderRef.current = next;
    setColOrder(next);
    savePreferences({ [PREFS_ORDER_KEY]: next });
  }, [draggedKey, savePreferences]);

  const onDragEnd = useCallback(() => {
    setDraggedKey(null);
    setDragOverKey(null);
  }, []);

  return {
    tableRef,
    effectiveOrder,
    getColWidth,
    startResize,
    resetOrder,
    resetWidths,
    saveError,
    hasOrderCustomization: !!colOrder,
    hasWidthCustomization: !!widths,
    draggedKey,
    dragOverKey,
    onDragStart,
    onDragOver,
    onDrop,
    onDragEnd,
  };
}

// ── Page component ────────────────────────────────────────────────────────────

export function AdminParticipantsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const filters = useMemo(() => filtersFromSearchParams(searchParams), [searchParams]);
  const [keyword, setKeyword] = useState(filters.keyword ?? "");

  const registrationsQuery = useQuery({
    queryKey: ["admin", "registrations", filters],
    queryFn: () => getAdminRegistrations(filters),
    placeholderData: keepPreviousData
  });

  const page = registrationsQuery.data;
  const participants = page?.content ?? [];
  const currentPage = filters.page ?? 0;

  const updateFilters = (next: Partial<AdminRegistrationFilters>) => {
    setSearchParams(searchParamsFromFilters({ ...filters, ...next, page: next.page ?? 0 }));
  };

  const clearFilters = () => {
    setKeyword("");
    setSearchParams(searchParamsFromFilters({ page: 0, size: filters.size ?? PAGE_SIZE_DEFAULT }));
  };

  const {
    tableRef, effectiveOrder, getColWidth, startResize,
    resetOrder, resetWidths, saveError, hasOrderCustomization, hasWidthCustomization,
    draggedKey, dragOverKey, onDragStart, onDragOver, onDrop, onDragEnd,
  } = useColumnCustomization();

  const currentSorts = filters.sorts ?? [];

  const sortInfo = (primary: string, alternate: string) => {
    const pi = currentSorts.indexOf(primary);
    const ai = currentSorts.indexOf(alternate);
    if (pi !== -1) return { direction: "asc" as const, priority: currentSorts.length > 1 ? pi + 1 : undefined };
    if (ai !== -1) return { direction: "desc" as const, priority: currentSorts.length > 1 ? ai + 1 : undefined };
    return { direction: undefined as undefined, priority: undefined };
  };

  const oneWayInfo = (sort: string) => {
    const i = currentSorts.indexOf(sort);
    if (i !== -1) return { direction: "asc" as const, priority: currentSorts.length > 1 ? i + 1 : undefined };
    return { direction: undefined as undefined, priority: undefined };
  };

  const toggleSort = (asc: string, desc: string) => {
    const hasAsc = currentSorts.includes(asc);
    const hasDesc = currentSorts.includes(desc);
    if (hasAsc) {
      updateFilters({ sorts: currentSorts.map((s) => (s === asc ? desc : s)), page: 0 });
    } else if (hasDesc) {
      updateFilters({ sorts: currentSorts.filter((s) => s !== desc), page: 0 });
    } else {
      updateFilters({ sorts: [asc, ...currentSorts], page: 0 });
    }
  };

  const toggleOneWaySort = (sort: string) => {
    if (currentSorts.includes(sort)) {
      updateFilters({ sorts: currentSorts.filter((s) => s !== sort), page: 0 });
    } else {
      updateFilters({ sorts: [sort, ...currentSorts], page: 0 });
    }
  };

  // Render sort header per column key
  const getSortHeader = (key: ColumnKey) => {
    const label = COL_LABEL[key];
    const bi = COL_BISORT[key];
    const ow = COL_ONEWAY[key];
    if (bi) return <SortableHeader {...sortInfo(bi[0], bi[1])} label={label} onClick={() => toggleSort(bi[0], bi[1])} />;
    if (ow) return <SortableHeader {...oneWayInfo(ow)} label={label} onClick={() => toggleOneWaySort(ow)} />;
    return <span className="col-label">{label}</span>;
  };

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Participants</p>
          <div className="page-heading-title-row">
            <h1>참가자 관리</h1>
            <span className="masking-badge" title="목록에서는 연락처가 마스킹되어 표시됩니다.">
              <svg aria-hidden="true" fill="none" height="12" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" viewBox="0 0 24 24" width="12">
                <path d="M12 2L3 7v5c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V7L12 2z" />
                <path d="M9 12l2 2 4-4" />
              </svg>
              연락처 마스킹 ON
            </span>
          </div>
        </div>
        <div className="result-count">
          <span className="result-count__label">검색 결과</span>
          <strong className="result-count__number">{page?.totalElements ?? "–"}</strong>
          <span className="result-count__unit">명</span>
        </div>
      </div>

      {registrationsQuery.isError ? (
        <StatusMessage message={registrationsQuery.error.message} tone="error" />
      ) : null}
      {saveError ? <StatusMessage message={saveError} tone="error" /> : null}

      <div className="search-card">
        <form
          className="search-card-top"
          onSubmit={(event) => {
            event.preventDefault();
            updateFilters({ keyword: keyword.trim() || undefined });
          }}
        >
          <div className="search-input-wrap">
            <svg aria-hidden="true" className="search-icon" fill="none" height="16" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" viewBox="0 0 24 24" width="16">
              <circle cx="11" cy="11" r="8" />
              <path d="M21 21l-4.35-4.35" />
            </svg>
            <input
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="이름, 연락처 끝 4자리, 공동체, 조 검색"
              type="search"
              value={keyword}
            />
          </div>
          <button className="button button--search-submit button--md" type="submit">검색</button>
          <button className="button button--ghost button--md" onClick={clearFilters} type="button">초기화</button>
        </form>
        <div aria-hidden="true" className="search-card-divider" />
        <div className="filter-select-row" aria-label="필터">
          <span className="filter-label">필터</span>
          <select
            className={filters.status ? "filter-select filter-select--active" : "filter-select"}
            onChange={(e) => updateFilters({ status: valueOrUndefined(e.target.value) as AdminRegistrationFilters["status"] })}
            value={filters.status ?? ""}
          >
            <option value="">등록 상태</option>
            <option value="REGISTERED">등록 완료</option>
            <option value="CANCELLED">취소</option>
          </select>
          <select
            className={filters.attendanceType ? "filter-select filter-select--active" : "filter-select"}
            onChange={(e) => updateFilters({ attendanceType: valueOrUndefined(e.target.value) as AdminRegistrationFilters["attendanceType"] })}
            value={filters.attendanceType ?? ""}
          >
            <option value="">참석</option>
            <option value="FULL">전체 참석</option>
            <option value="PARTIAL">부분 참석</option>
            <option value="WORSHIP_ONLY">예배만</option>
          </select>
          <select
            className={filters.feePaid !== undefined ? "filter-select filter-select--active" : "filter-select"}
            onChange={(e) => updateFilters({ feePaid: e.target.value === "" ? undefined : e.target.value === "true" })}
            value={filters.feePaid === undefined ? "" : String(filters.feePaid)}
          >
            <option value="">참가비</option>
            <option value="false">미납</option>
            <option value="true">납부</option>
          </select>
          <select
            className={filters.retreatGroupAssigned !== undefined || filters.cellAssigned !== undefined ? "filter-select filter-select--active" : "filter-select"}
            onChange={(e) => {
              const v = e.target.value;
              updateFilters({ retreatGroupAssigned: v === "NO_GROUP" ? false : undefined, cellAssigned: v === "NO_CELL" ? false : undefined });
            }}
            value={filters.retreatGroupAssigned === false ? "NO_GROUP" : filters.cellAssigned === false ? "NO_CELL" : ""}
          >
            <option value="">배정</option>
            <option value="NO_GROUP">조 미배정</option>
            <option value="NO_CELL">셀 미지정</option>
          </select>
          <select
            className={filters.checkedIn !== undefined ? "filter-select filter-select--active" : "filter-select"}
            onChange={(e) => updateFilters({ checkedIn: e.target.value === "" ? undefined : e.target.value === "true" })}
            value={filters.checkedIn === undefined ? "" : String(filters.checkedIn)}
          >
            <option value="">체크인</option>
            <option value="false">미완료</option>
            <option value="true">완료</option>
          </select>
          <select
            className={filters.transportationNeed ? "filter-select filter-select--active" : "filter-select"}
            onChange={(e) => updateFilters({ transportationNeed: valueOrUndefined(e.target.value) as AdminRegistrationFilters["transportationNeed"] })}
            value={filters.transportationNeed ?? ""}
          >
            <option value="">교통</option>
            <option value="CARPOOL_NEEDED">이동 지원 요청</option>
            <option value="CARPOOL_AVAILABLE">카풀 제공</option>
          </select>
          <select
            className={filters.newcomer !== undefined || filters.careTarget !== undefined ? "filter-select filter-select--active" : "filter-select"}
            onChange={(e) => {
              const v = e.target.value;
              updateFilters({ newcomer: v === "NEWCOMER" ? true : undefined, careTarget: v === "CARE_TARGET" ? true : undefined });
            }}
            value={filters.newcomer ? "NEWCOMER" : filters.careTarget ? "CARE_TARGET" : ""}
          >
            <option value="">태그</option>
            <option value="NEWCOMER">새가족</option>
            <option value="CARE_TARGET">돌봄</option>
          </select>
          <label className="page-size-select-wrap" style={{ marginLeft: "auto" }}>
            <span>행</span>
            <select
              className="page-size-select"
              onChange={(e) => updateFilters({ size: Number(e.target.value), page: 0 })}
              value={filters.size ?? PAGE_SIZE_DEFAULT}
            >
              {PAGE_SIZE_OPTIONS.map((n) => (
                <option key={n} value={n}>{n >= PAGE_SIZE_ALL ? "전체" : n}</option>
              ))}
            </select>
          </label>
        </div>
      </div>

      <div className="table-card participant-table-card">
        <table className={`participant-table${hasWidthCustomization ? " participant-table--custom-widths" : ""}`} ref={tableRef}>
          <colgroup>
            {effectiveOrder.map((key) => {
              const width = getColWidth(key);
              return <col key={key} style={width ? { width } : undefined} />;
            })}
          </colgroup>
          <thead>
            <tr>
              {effectiveOrder.map((key, i) => (
                <th
                  key={key}
                  className={
                    draggedKey === key ? "col-dragging"
                      : dragOverKey === key ? "col-drag-over"
                        : undefined
                  }
                  draggable
                  onDragEnd={onDragEnd}
                  onDragOver={(e) => onDragOver(key, e)}
                  onDragStart={(e) => onDragStart(key, e)}
                  onDrop={(e) => onDrop(key, e)}
                >
                  {getSortHeader(key)}
                  {i < effectiveOrder.length - 1 && (
                    <ColResizeHandle onMouseDown={(e) => startResize(i, e.clientX)} />
                  )}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {participants.map((item) => (
              <tr key={item.id}>
                {effectiveOrder.map((key, i) => (
                  <td key={key} className={TD_CLASS[key]}>
                    {getBodyCell(item, key)}
                    {i < effectiveOrder.length - 1 && (
                      <ColResizeHandle onMouseDown={(e) => startResize(i, e.clientX)} />
                    )}
                  </td>
                ))}
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

      <div className="participant-table-footer">
        <div className="col-reset-group">
          <button className="button button--ghost button--sm" disabled={!hasWidthCustomization} onClick={resetWidths} type="button">
            열 너비 초기화
          </button>
          <button className="button button--ghost button--sm" disabled={!hasOrderCustomization} onClick={resetOrder} type="button">
            열 순서 초기화
          </button>
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
      </div>
    </section>
  );
}

// ── Sub-components ────────────────────────────────────────────────────────────

function ColResizeHandle({ onMouseDown }: { onMouseDown: (e: React.MouseEvent) => void }) {
  return (
    <div
      className="col-resize-handle"
      draggable={false}
      onMouseDown={(e) => { e.preventDefault(); e.stopPropagation(); onMouseDown(e); }}
    />
  );
}

function SortableHeader({
  direction,
  priority,
  label,
  onClick
}: {
  direction?: "asc" | "desc";
  priority?: number;
  label: string;
  onClick: () => void;
}) {
  const cls = direction === "asc"
    ? "sort-header sort-header--asc"
    : direction === "desc"
      ? "sort-header sort-header--desc"
      : "sort-header";
  return (
    <button className={cls} onClick={onClick} type="button">
      <span>{label}</span>
      <span className="sort-header-icon" aria-hidden="true">
        {direction === "asc" ? "↑" : direction === "desc" ? "↓" : "↕"}
        {priority !== undefined ? <sup>{priority}</sup> : null}
      </span>
    </button>
  );
}

function SpecialTags({ newcomer, careTarget, cancelled }: { newcomer: boolean; careTarget: boolean; cancelled: boolean }) {
  if (!newcomer && !careTarget && !cancelled) return null;
  return (
    <div className="tag-list">
      {cancelled ? <span className="mini-tag mini-tag--danger">취소</span> : null}
      {newcomer ? <span className="mini-tag">새가족</span> : null}
      {careTarget ? <span className="mini-tag mini-tag--warning">돌봄</span> : null}
    </div>
  );
}

function StatusPill({ tone, children }: { tone: "success" | "warning" | "danger" | "neutral"; children: string }) {
  return <span className={`status-pill status-pill--${tone}`}>{children}</span>;
}

// ── Pure cell renderer (no component state needed) ────────────────────────────

function getBodyCell(item: AdminRegistration, key: ColumnKey) {
  switch (key) {
    case "name":
      return <Link className="table-link" to={`/admin/participants/${item.id}`}>{item.name}</Link>;
    case "birthYear":
      return String(item.birthYear % 100).padStart(2, "0");
    case "gender":
      return item.gender === "FEMALE" ? "여" : "남";
    case "phone":
      return item.phoneNumber;
    case "middleGroup":
      return item.middleGroupName ?? "-";
    case "cell":
      return item.cellName ?? "-";
    case "attendance":
      return formatAttendance(item.attendanceType);
    case "group":
      return (
        <>
          {item.retreatGroupName ?? "-"}
          {item.retreatGroupLeader ? <span className="table-note">조장</span> : null}
        </>
      );
    case "transportation":
      return <span className="participant-transportation-value">{formatTransportationSummary(item)}</span>;
    case "feePaid":
      return <StatusPill tone={item.feePaid ? "success" : "warning"}>{item.feePaid ? "납부" : "미납"}</StatusPill>;
    case "checkedIn":
      return <StatusPill tone={item.checkedIn ? "success" : "neutral"}>{item.checkedIn ? "완료" : "미완료"}</StatusPill>;
    case "createdAt":
      return formatDate(item.createdAt);
    case "participantUpdatedAt":
      return item.participantUpdatedAt
        ? <span className="mini-tag mini-tag--warning">{new Date(item.participantUpdatedAt).toLocaleString()}</span>
        : "-";
    case "special":
      return <SpecialTags newcomer={item.newcomer} careTarget={item.careTarget} cancelled={item.status === "CANCELLED"} />;
  }
}

// ── URL param helpers ─────────────────────────────────────────────────────────

function filtersFromSearchParams(params: URLSearchParams): AdminRegistrationFilters {
  return {
    page: numberParam(params.get("page"), 0),
    size: numberParam(params.get("size"), PAGE_SIZE_DEFAULT),
    keyword: valueOrUndefined(params.get("keyword") ?? ""),
    status: valueOrUndefined(params.get("status") ?? "") as AdminRegistrationFilters["status"],
    feePaid: booleanOrUndefined(params.get("feePaid") ?? ""),
    newcomer: booleanOrUndefined(params.get("newcomer") ?? ""),
    careTarget: booleanOrUndefined(params.get("careTarget") ?? ""),
    checkedIn: booleanOrUndefined(params.get("checkedIn") ?? ""),
    retreatGroupAssigned: booleanOrUndefined(params.get("retreatGroupAssigned") ?? ""),
    cellAssigned: booleanOrUndefined(params.get("cellAssigned") ?? ""),
    attendanceType: valueOrUndefined(params.get("attendanceType") ?? "") as AdminRegistrationFilters["attendanceType"],
    transportationNeed: valueOrUndefined(params.get("transportationNeed") ?? "") as AdminRegistrationFilters["transportationNeed"],
    sorts: params.getAll("sort").filter(Boolean)
  };
}

function searchParamsFromFilters(filters: AdminRegistrationFilters) {
  const params = new URLSearchParams();
  params.set("page", String(filters.page ?? 0));
  if ((filters.size ?? PAGE_SIZE_DEFAULT) !== PAGE_SIZE_DEFAULT) params.set("size", String(filters.size));
  if (filters.keyword) params.set("keyword", filters.keyword);
  if (filters.status) params.set("status", filters.status);
  if (filters.feePaid !== undefined) params.set("feePaid", String(filters.feePaid));
  if (filters.newcomer !== undefined) params.set("newcomer", String(filters.newcomer));
  if (filters.careTarget !== undefined) params.set("careTarget", String(filters.careTarget));
  if (filters.checkedIn !== undefined) params.set("checkedIn", String(filters.checkedIn));
  if (filters.retreatGroupAssigned !== undefined) params.set("retreatGroupAssigned", String(filters.retreatGroupAssigned));
  if (filters.cellAssigned !== undefined) params.set("cellAssigned", String(filters.cellAssigned));
  if (filters.attendanceType) params.set("attendanceType", filters.attendanceType);
  if (filters.transportationNeed) params.set("transportationNeed", filters.transportationNeed);
  (filters.sorts ?? []).forEach((s) => params.append("sort", s));
  return params;
}

function numberParam(value: string | null, fallback: number) {
  if (!value) return fallback;
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

// ── Format helpers ────────────────────────────────────────────────────────────

function formatAttendance(value: string) {
  switch (value) {
    case "FULL": return "전체 참석";
    case "PARTIAL": return "부분 참석";
    case "WORSHIP_ONLY": return "예배만";
    default: return value;
  }
}

function formatTransportationSummary(item: Pick<AdminRegistration, "inboundTransportationMethod" | "outboundTransportationMethod">) {
  const inbound = formatTransportation(item.inboundTransportationMethod);
  const outbound = formatTransportation(item.outboundTransportationMethod);
  return inbound === outbound ? inbound : `${inbound} / ${outbound}`;
}

function formatTransportation(value: string | null) {
  switch (value) {
    case "OWN_CAR": return "개인차량";
    case "GROUP_BUS": return "단체 이동 차량";
    case "WORSHIP_SHUTTLE": return "집회차량";
    case "PUBLIC_TRANSIT": return "대중교통";
    case "CARPOOL_NEEDED": return "이동 지원 요청";
    case "NOT_DECIDED": return "미정";
    default: return "-";
  }
}

function formatDate(value: string) {
  return new Date(value).toLocaleDateString();
}
