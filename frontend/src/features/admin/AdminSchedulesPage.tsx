import { useMemo, useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createSchedule,
  getAdminProfile,
  getRetreats,
  getSchedules,
  updateSchedule,
  updateScheduleActive,
  type AdminRoleValue,
  type ScheduleItem,
  type SchedulePayload
} from "./adminApi";
import { EmptyState } from "../../shared/ui/EmptyState";
import { StatusMessage } from "../../shared/ui/StatusMessage";

type ScheduleDraft = Omit<SchedulePayload, "startsAt" | "endsAt"> & {
  startsTime: string;
  endsTime: string;
};

const roleLevel: Record<AdminRoleValue, number> = {
  STAFF: 1,
  CHAIR: 2,
  PASTOR: 3,
  SYSTEM_ADMIN: 4
};

const categories: Array<{ value: ScheduleItem["category"]; label: string }> = [
  { value: "PROGRAM", label: "프로그램" },
  { value: "MEAL", label: "식사" },
  { value: "WORSHIP", label: "예배·집회" },
  { value: "PRAYER", label: "기도" },
  { value: "GROUP_ACTIVITY", label: "조별 활동" },
  { value: "LECTURE", label: "강의" },
  { value: "BREAK", label: "휴식" },
  { value: "MOVE", label: "이동" },
  { value: "CHECK_IN", label: "체크인" },
  { value: "CHECK_OUT", label: "체크아웃" },
  { value: "NOTICE", label: "안내" },
  { value: "ETC", label: "기타" }
];

const audiences: Array<{ value: ScheduleItem["targetAudience"]; label: string }> = [
  { value: "ALL", label: "전체 참가자" },
  { value: "STAFF_ONLY", label: "스태프" },
  { value: "LEADERS_ONLY", label: "리더" },
  { value: "NEWCOMERS", label: "새가족" },
  { value: "CARE_TARGETS", label: "돌봄 대상" }
];

function emptyDraft(scheduleDate = ""): ScheduleDraft {
  return {
    title: "",
    description: "",
    scheduleDate,
    startsTime: "",
    endsTime: "",
    location: "",
    category: "PROGRAM",
    targetAudience: "ALL",
    active: true,
    displayOrder: 0,
    collectParticipation: false
  };
}

export function AdminSchedulesPage() {
  const queryClient = useQueryClient();
  const [editingId, setEditingId] = useState<number | null>(null);
  const [draft, setDraft] = useState<ScheduleDraft>(emptyDraft());
  const [formError, setFormError] = useState<string | null>(null);
  const schedulesQuery = useQuery({ queryKey: ["admin", "schedules"], queryFn: () => getSchedules() });
  const retreatsQuery = useQuery({ queryKey: ["admin", "retreats"], queryFn: getRetreats });
  const profileQuery = useQuery({ queryKey: ["admin", "me"], queryFn: getAdminProfile });
  const currentRetreat = retreatsQuery.data?.find((retreat) => retreat.status !== "CLOSED");
  const canManage = profileQuery.data
    ? roleLevel[profileQuery.data.role] >= roleLevel.CHAIR
    : false;
  const retreatDates = useMemo(
    () => currentRetreat ? datesBetween(currentRetreat.startsOn, currentRetreat.endsOn) : [],
    [currentRetreat]
  );
  const selectedScheduleDate = draft.scheduleDate || currentRetreat?.startsOn || "";

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: ["admin", "schedules"] });
    void queryClient.invalidateQueries({ queryKey: ["admin", "participation-options"] });
    void queryClient.invalidateQueries({ queryKey: ["public", "participation-options"] });
  }

  const saveMutation = useMutation({
    mutationFn: (payload: SchedulePayload) => editingId === null
      ? createSchedule(payload)
      : updateSchedule(editingId, payload),
    onSuccess: () => {
      setEditingId(null);
      setDraft(emptyDraft(currentRetreat?.startsOn));
      setFormError(null);
      refresh();
    }
  });
  const activeMutation = useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) => updateScheduleActive(id, active),
    onSuccess: refresh
  });
  const schedules = schedulesQuery.data ?? [];

  function submit(event: FormEvent) {
    event.preventDefault();
    if (Boolean(draft.startsTime) !== Boolean(draft.endsTime)) {
      setFormError("시작 시각과 종료 시각을 모두 입력하거나 둘 다 비워 주세요.");
      return;
    }
    setFormError(null);
    const displayOrder = draft.startsTime ? timeToMinutes(draft.startsTime) : draft.displayOrder;
    saveMutation.mutate({
      title: draft.title,
      description: draft.description,
      scheduleDate: selectedScheduleDate,
      startsAt: draft.startsTime ? toKoreaOffsetDateTime(selectedScheduleDate, draft.startsTime) : null,
      endsAt: draft.endsTime ? toKoreaOffsetDateTime(selectedScheduleDate, draft.endsTime) : null,
      location: draft.location,
      category: draft.category,
      targetAudience: draft.targetAudience,
      active: draft.active,
      displayOrder,
      collectParticipation: draft.collectParticipation
    });
  }

  function edit(item: ScheduleItem) {
    setEditingId(item.id);
    setFormError(null);
    setDraft({
      title: item.title,
      description: item.description ?? "",
      scheduleDate: item.scheduleDate,
      startsTime: timePart(item.startsAt),
      endsTime: timePart(item.endsAt),
      location: item.location ?? "",
      category: item.category,
      targetAudience: item.targetAudience,
      active: item.active,
      displayOrder: item.displayOrder,
      collectParticipation: item.collectParticipation
    });
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Retreat timetable</p>
          <h1>시간표</h1>
          <p className="muted">시간표를 한 번 만들고, 참가 확인이 필요한 프로그램과 식사만 신청서에 표시합니다.</p>
        </div>
        {currentRetreat ? <span className="pill">{currentRetreat.startsOn} ~ {currentRetreat.endsOn}</span> : null}
      </div>

      {schedulesQuery.isError ? <StatusMessage message={schedulesQuery.error.message} tone="error" /> : null}
      {retreatsQuery.isError ? <StatusMessage message={retreatsQuery.error.message} tone="error" /> : null}
      {saveMutation.isError ? <StatusMessage message={saveMutation.error.message} tone="error" /> : null}
      {activeMutation.isError ? <StatusMessage message={activeMutation.error.message} tone="error" /> : null}
      {formError ? <StatusMessage message={formError} tone="error" /> : null}

      {currentRetreat && canManage ? (
        <form className="panel page-stack schedule-editor" onSubmit={submit}>
          <div className="schedule-editor__heading">
            <div>
              <p className="eyebrow">{editingId === null ? "New item" : "Edit item"}</p>
              <h2>{editingId === null ? "일정 추가" : "일정 수정"}</h2>
            </div>
            {editingId !== null ? (
              <button
                className="button button--ghost"
                onClick={() => {
                  setEditingId(null);
                  setDraft(emptyDraft(currentRetreat.startsOn));
                }}
                type="button"
              >
                수정 취소
              </button>
            ) : null}
          </div>

          <div className="schedule-editor__grid">
            <label>
              날짜
              <select
                onChange={(event) => setDraft((current) => ({ ...current, scheduleDate: event.target.value }))}
                required
                value={selectedScheduleDate}
              >
                {retreatDates.map((date, index) => (
                  <option key={date} value={date}>{index + 1}일차 · {formatDateLabel(date)}</option>
                ))}
              </select>
            </label>
            <label>
              시작 시각 <span className="muted">(선택)</span>
              <input
                onChange={(event) => setDraft((current) => ({ ...current, startsTime: event.target.value }))}
                type="time"
                value={draft.startsTime}
              />
            </label>
            <label>
              종료 시각 <span className="muted">(선택)</span>
              <input
                onChange={(event) => setDraft((current) => ({ ...current, endsTime: event.target.value }))}
                type="time"
                value={draft.endsTime}
              />
            </label>
            <label className="schedule-editor__title">
              일정 이름
              <input
                maxLength={150}
                onChange={(event) => setDraft((current) => ({ ...current, title: event.target.value }))}
                placeholder="예: 저녁식사, 개회 집회"
                required
                value={draft.title}
              />
            </label>
            <label>
              종류
              <select
                onChange={(event) => {
                  const category = event.target.value as ScheduleItem["category"];
                  setDraft((current) => ({
                    ...current,
                    category,
                    collectParticipation: category === "MEAL" ? true : current.collectParticipation
                  }));
                }}
                value={draft.category}
              >
                {categories.map((category) => (
                  <option key={category.value} value={category.value}>{category.label}</option>
                ))}
              </select>
            </label>
            <label>
              장소 <span className="muted">(선택)</span>
              <input
                maxLength={150}
                onChange={(event) => setDraft((current) => ({ ...current, location: event.target.value }))}
                placeholder="예: 대강당"
                value={draft.location}
              />
            </label>
            <label>
              대상
              <select
                onChange={(event) => {
                  const targetAudience = event.target.value as ScheduleItem["targetAudience"];
                  setDraft((current) => ({
                    ...current,
                    targetAudience,
                    collectParticipation: targetAudience === "ALL" && current.collectParticipation
                  }));
                }}
                value={draft.targetAudience}
              >
                {audiences.map((audience) => (
                  <option key={audience.value} value={audience.value}>{audience.label}</option>
                ))}
              </select>
            </label>
            <label className="schedule-editor__description">
              설명 <span className="muted">(선택)</span>
              <textarea
                maxLength={10000}
                onChange={(event) => setDraft((current) => ({ ...current, description: event.target.value }))}
                rows={2}
                value={draft.description}
              />
            </label>
          </div>

          <div className="schedule-editor__checks">
            <label className="checkbox-row">
              <input
                checked={draft.collectParticipation}
                disabled={draft.targetAudience !== "ALL"}
                onChange={(event) => setDraft((current) => ({
                  ...current,
                  collectParticipation: event.target.checked
                }))}
                type="checkbox"
              />
              <span>
                <strong>신청서에서 참석 여부 받기</strong>
                <small>체크하면 참가자 신청·수정 화면에 이 일정이 나타납니다.</small>
              </span>
            </label>
            <label className="checkbox-row">
              <input
                checked={draft.active}
                onChange={(event) => setDraft((current) => ({ ...current, active: event.target.checked }))}
                type="checkbox"
              />
              <span><strong>운영 화면에 공개</strong></span>
            </label>
          </div>

          <div className="table-actions">
            <button className="button button--primary" disabled={saveMutation.isPending} type="submit">
              {editingId === null ? "시간표에 추가" : "수정 저장"}
            </button>
          </div>
        </form>
      ) : null}

      {!currentRetreat && !retreatsQuery.isLoading ? (
        <EmptyState title="운영할 수련회가 없습니다" message="수련회 관리에서 다음 수련회를 먼저 만들어 주세요." />
      ) : null}
      {currentRetreat && !canManage ? (
        <StatusMessage message="시간표 변경은 CHAIR 이상 권한이 필요합니다. 현재는 읽기 전용입니다." />
      ) : null}

      {currentRetreat ? (
        <div className="schedule-board">
          {retreatDates.map((date, index) => {
            const daySchedules = schedules.filter((item) => item.scheduleDate === date);
            return (
              <section className="schedule-day" key={date}>
                <header className="schedule-day__header">
                  <div>
                    <p className="eyebrow">Day {index + 1}</p>
                    <h2>{index + 1}일차</h2>
                  </div>
                  <span>{formatDateLabel(date)}</span>
                </header>
                <div className="schedule-day__items">
                  {daySchedules.map((item) => (
                    <article className={`schedule-card schedule-card--${item.category.toLowerCase()}`} key={item.id}>
                      <div className="schedule-card__time">{formatTimeRange(item)}</div>
                      <div className="schedule-card__body">
                        <div className="schedule-card__title-row">
                          <div>
                            <span className="schedule-card__category">{categoryLabel(item.category)}</span>
                            <h3>{item.title}</h3>
                          </div>
                          <span className={item.active ? "status-pill status-pill--success" : "status-pill status-pill--neutral"}>
                            {item.active ? "공개" : "비공개"}
                          </span>
                        </div>
                        {item.location ? <p className="schedule-card__meta">{item.location}</p> : null}
                        {item.description ? <p className="schedule-card__description">{item.description}</p> : null}
                        <div className="schedule-card__footer">
                          {item.collectParticipation ? (
                            <span className="mini-tag mini-tag--warning">신청서 노출 · {item.selectionCount}명 선택</span>
                          ) : item.participationOptionId ? (
                            <span className="mini-tag">신청서 비노출 · 기존 {item.selectionCount}명</span>
                          ) : (
                            <span className="muted">신청 인원 수집 안 함</span>
                          )}
                          {canManage ? (
                            <div className="table-actions">
                              <button className="table-action" onClick={() => edit(item)} type="button">수정</button>
                              <button
                                className={item.active ? "table-action table-action--warning" : "table-action"}
                                disabled={activeMutation.isPending}
                                onClick={() => activeMutation.mutate({ id: item.id, active: !item.active })}
                                type="button"
                              >
                                {item.active ? "비공개" : "공개"}
                              </button>
                            </div>
                          ) : null}
                        </div>
                      </div>
                    </article>
                  ))}
                  {!daySchedules.length ? <p className="schedule-day__empty">아직 등록된 일정이 없습니다.</p> : null}
                </div>
              </section>
            );
          })}
        </div>
      ) : null}
      {schedulesQuery.isLoading ? <EmptyState title="시간표를 불러오는 중입니다" message="잠시만 기다려 주세요." /> : null}
    </section>
  );
}

function datesBetween(startsOn: string, endsOn: string) {
  const dates: string[] = [];
  const cursor = new Date(`${startsOn}T12:00:00`);
  const end = new Date(`${endsOn}T12:00:00`);
  while (cursor <= end) {
    dates.push(cursor.toISOString().slice(0, 10));
    cursor.setDate(cursor.getDate() + 1);
  }
  return dates;
}

function toKoreaOffsetDateTime(date: string, time: string) {
  return `${date}T${time}:00+09:00`;
}

function timeToMinutes(time: string) {
  const [hours, minutes] = time.split(":").map(Number);
  return hours * 60 + minutes;
}

function timePart(value?: string | null) {
  if (!value) return "";
  return new Intl.DateTimeFormat("en-GB", {
    timeZone: "Asia/Seoul",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23"
  }).format(new Date(value));
}

function formatTimeRange(item: ScheduleItem) {
  const starts = timePart(item.startsAt);
  const ends = timePart(item.endsAt);
  return starts && ends ? `${starts}–${ends}` : "시간 미정";
}

function formatDateLabel(date: string) {
  return new Intl.DateTimeFormat("ko-KR", {
    month: "long",
    day: "numeric",
    weekday: "short"
  }).format(new Date(`${date}T12:00:00`));
}

function categoryLabel(value: ScheduleItem["category"]) {
  return categories.find((category) => category.value === value)?.label ?? value;
}
