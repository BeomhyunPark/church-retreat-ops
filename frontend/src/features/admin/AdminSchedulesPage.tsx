import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getSchedules, updateScheduleActive, type ScheduleItem } from "./adminApi";
import { EmptyState } from "../../shared/ui/EmptyState";
import { StatusMessage } from "../../shared/ui/StatusMessage";

const categories = [
  "WORSHIP",
  "PRAYER",
  "MEAL",
  "GROUP_ACTIVITY",
  "LECTURE",
  "BREAK",
  "MOVE",
  "CHECK_IN",
  "CHECK_OUT",
  "NOTICE",
  "ETC"
];

export function AdminSchedulesPage() {
  const queryClient = useQueryClient();
  const [date, setDate] = useState("");
  const [category, setCategory] = useState("");
  const [activeFilter, setActiveFilter] = useState("ALL");
  const active = activeFilter === "ALL" ? undefined : activeFilter === "ACTIVE";
  const query = useQuery({
    queryKey: ["admin", "schedules", { active, category, date }],
    queryFn: () => getSchedules({ active, category: category || undefined, date: date || undefined })
  });
  const mutation = useMutation({
    mutationFn: ({ id, active: nextActive }: { id: number; active: boolean }) => updateScheduleActive(id, nextActive),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["admin", "schedules"] });
    }
  });
  const schedules = query.data ?? [];

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Schedules</p>
          <h1>일정 관리</h1>
        </div>
        <span className="pill">CHAIR 이상 변경 가능</span>
      </div>

      {query.isError ? <StatusMessage message={query.error.message} tone="error" /> : null}
      {mutation.isError ? <StatusMessage message={mutation.error.message} tone="error" /> : null}

      <section className="filter-panel" aria-label="일정 목록 필터">
        <label>
          날짜
          <input onChange={(event) => setDate(event.target.value)} type="date" value={date} />
        </label>
        <label>
          카테고리
          <select onChange={(event) => setCategory(event.target.value)} value={category}>
            <option value="">전체</option>
            {categories.map((value) => (
              <option key={value} value={value}>
                {categoryLabel(value)}
              </option>
            ))}
          </select>
        </label>
        <label>
          상태
          <select onChange={(event) => setActiveFilter(event.target.value)} value={activeFilter}>
            <option value="ALL">전체</option>
            <option value="ACTIVE">공개</option>
            <option value="INACTIVE">비공개</option>
          </select>
        </label>
        <div className="filter-summary">
          <span>검색 결과</span>
          <strong>{schedules.length}</strong>
        </div>
      </section>

      <div className="table-card">
        <table>
          <thead>
            <tr>
              <th>일정</th>
              <th>시간</th>
              <th>장소</th>
              <th>대상</th>
              <th>상태</th>
              <th>수정자</th>
              <th>처리</th>
            </tr>
          </thead>
          <tbody>
            {schedules.map((item) => (
              <ScheduleRow
                actionPending={mutation.isPending}
                key={item.id}
                onActiveChange={(nextActive) => mutation.mutate({ id: item.id, active: nextActive })}
                schedule={item}
              />
            ))}
          </tbody>
        </table>
        {query.isLoading ? <EmptyState title="일정 목록을 불러오는 중입니다" message="잠시만 기다려 주세요." /> : null}
        {!query.isLoading && !schedules.length ? (
          <EmptyState title="조건에 맞는 일정이 없습니다" message="날짜, 카테고리, 공개 상태 필터를 다시 확인해 주세요." />
        ) : null}
      </div>
    </section>
  );
}

function ScheduleRow({
  actionPending,
  onActiveChange,
  schedule
}: {
  actionPending: boolean;
  onActiveChange: (active: boolean) => void;
  schedule: ScheduleItem;
}) {
  return (
    <tr>
      <td>
        <strong>{schedule.title}</strong>
        <span className="table-note">{categoryLabel(schedule.category)}</span>
      </td>
      <td>
        {new Date(schedule.startsAt).toLocaleString()} - {new Date(schedule.endsAt).toLocaleTimeString()}
      </td>
      <td>{schedule.location ?? "-"}</td>
      <td>{audienceLabel(schedule.targetAudience)}</td>
      <td>
        <span className={schedule.active ? "status-pill status-pill--success" : "status-pill status-pill--neutral"}>
          {schedule.active ? "공개" : "비공개"}
        </span>
      </td>
      <td>{schedule.updatedBy?.name ?? "-"}</td>
      <td>
        <button
          className={schedule.active ? "table-action table-action--warning" : "table-action"}
          disabled={actionPending}
          onClick={() => onActiveChange(!schedule.active)}
          type="button"
        >
          {schedule.active ? "비공개" : "공개"}
        </button>
      </td>
    </tr>
  );
}

function categoryLabel(value: string) {
  const labels: Record<string, string> = {
    WORSHIP: "예배",
    PRAYER: "기도",
    MEAL: "식사",
    GROUP_ACTIVITY: "조별 활동",
    LECTURE: "강의",
    BREAK: "휴식",
    MOVE: "이동",
    CHECK_IN: "체크인",
    CHECK_OUT: "체크아웃",
    NOTICE: "안내",
    ETC: "기타"
  };

  return labels[value] ?? value;
}

function audienceLabel(value: string) {
  const labels: Record<string, string> = {
    ALL: "전체",
    STAFF_ONLY: "스태프",
    LEADERS_ONLY: "리더",
    NEWCOMERS: "새가족",
    CARE_TARGETS: "돌봄"
  };

  return labels[value] ?? value;
}
