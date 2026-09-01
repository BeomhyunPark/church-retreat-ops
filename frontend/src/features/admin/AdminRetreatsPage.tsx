import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createRetreat,
  getAdminProfile,
  getRetreats,
  getSchedules,
  updateRetreat,
  updateRetreatRegistrationOpen,
  updateRetreatStatus,
  type AdminRoleValue,
  type RetreatPayload,
  type RetreatStatusValue
} from "./adminApi";
import { EmptyState } from "../../shared/ui/EmptyState";
import { StatusMessage } from "../../shared/ui/StatusMessage";

const roleLevel: Record<AdminRoleValue, number> = {
  STAFF: 1,
  CHAIR: 2,
  PASTOR: 3,
  SYSTEM_ADMIN: 4
};

const emptyForm: RetreatPayload = { name: "", startsOn: "", endsOn: "" };

export function AdminRetreatsPage() {
  const queryClient = useQueryClient();
  const [formChanges, setFormChanges] = useState<Partial<RetreatPayload>>({});
  const [scheduleRetreatId, setScheduleRetreatId] = useState<number | null>(null);
  const retreatsQuery = useQuery({ queryKey: ["admin", "retreats"], queryFn: getRetreats });
  const profileQuery = useQuery({ queryKey: ["admin", "me"], queryFn: getAdminProfile });
  const retreats = retreatsQuery.data ?? [];
  const current = retreats.find((retreat) => retreat.status !== "CLOSED");
  const scheduleRetreat = retreats.find((retreat) => retreat.id === scheduleRetreatId);
  const canManage = profileQuery.data ? roleLevel[profileQuery.data.role] >= roleLevel.CHAIR : false;
  const form = {
    ...(current
      ? { name: current.name, startsOn: current.startsOn, endsOn: current.endsOn }
      : emptyForm),
    ...formChanges
  };
  const schedulesQuery = useQuery({
    queryKey: ["admin", "schedules", "retreat", scheduleRetreatId],
    queryFn: () => getSchedules({ retreatId: scheduleRetreatId ?? undefined }),
    enabled: scheduleRetreatId !== null
  });

  function refreshRetreatState() {
    void queryClient.invalidateQueries({ queryKey: ["admin", "retreats"] });
    void queryClient.invalidateQueries({ queryKey: ["admin", "registrations"] });
    void queryClient.invalidateQueries({ queryKey: ["admin", "schedules"] });
    void queryClient.invalidateQueries({ queryKey: ["admin", "participation-options"] });
    void queryClient.invalidateQueries({ queryKey: ["public", "participation-options"] });
    void queryClient.invalidateQueries({ queryKey: ["admin", "retreat-groups"] });
    void queryClient.invalidateQueries({ queryKey: ["app", "identity"] });
  }

  const saveMutation = useMutation({
    mutationFn: (payload: RetreatPayload) => current
      ? updateRetreat(current.id, payload)
      : createRetreat(payload),
    onSuccess: () => {
      setFormChanges({});
      refreshRetreatState();
    }
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: RetreatStatusValue }) =>
      updateRetreatStatus(id, status),
    onSuccess: () => {
      setFormChanges({});
      refreshRetreatState();
    }
  });
  const registrationMutation = useMutation({
    mutationFn: ({ id, registrationOpen }: { id: number; registrationOpen: boolean }) =>
      updateRetreatRegistrationOpen(id, registrationOpen),
    onSuccess: refreshRetreatState
  });

  function changeField(field: keyof RetreatPayload, value: string) {
    setFormChanges((previous) => ({ ...previous, [field]: value }));
  }

  function closeCurrentRetreat() {
    if (!current || !window.confirm(
      "현재 수련회를 종료하시겠습니까? 참가자의 본인 수정도 함께 종료되고, 참가 인원은 요약으로만 남습니다."
    )) {
      return;
    }
    statusMutation.mutate({ id: current.id, status: "CLOSED" });
  }

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Retreat lifecycle</p>
          <h1>수련회 관리</h1>
        </div>
        <span className="pill">동시에 하나만 운영</span>
      </div>

      {retreatsQuery.isError ? <StatusMessage message={retreatsQuery.error.message} tone="error" /> : null}
      {saveMutation.isError ? <StatusMessage message={saveMutation.error.message} tone="error" /> : null}
      {statusMutation.isError ? <StatusMessage message={statusMutation.error.message} tone="error" /> : null}
      {registrationMutation.isError ? <StatusMessage message={registrationMutation.error.message} tone="error" /> : null}

      <section className="panel page-stack">
        <div>
          <p className="eyebrow">{current ? "Current retreat" : "Next retreat"}</p>
          <h2>{current ? "현재 수련회 설정" : "다음 수련회 만들기"}</h2>
        </div>
        {current ? (
          <span className={statusClassName(current.status)}>{statusLabel(current.status)}</span>
        ) : (
          <p className="muted">종료된 수련회만 있습니다. 다음 수련회를 만들면 준비 상태로 시작합니다.</p>
        )}
        <form
          className="form-grid"
          onSubmit={(event) => {
            event.preventDefault();
            saveMutation.mutate(form);
          }}
        >
          <label>
            수련회 이름
            <input
              disabled={!canManage}
              maxLength={150}
              onChange={(event) => changeField("name", event.target.value)}
              required
              value={form.name}
            />
          </label>
          <label>
            시작일
            <input
              disabled={!canManage}
              onChange={(event) => changeField("startsOn", event.target.value)}
              required
              type="date"
              value={form.startsOn}
            />
          </label>
          <label>
            종료일
            <input
              disabled={!canManage}
              min={form.startsOn}
              onChange={(event) => changeField("endsOn", event.target.value)}
              required
              type="date"
              value={form.endsOn}
            />
          </label>
          {current ? (
            <p className="muted">
              기간을 바꾸면 시간표도 일차 기준으로 함께 이동합니다. 기간 밖으로 밀린 일정은 기록을 보존하고 비공개 처리됩니다.
            </p>
          ) : null}
          {canManage ? (
            <div className="table-actions">
              <button className="button button--primary" disabled={saveMutation.isPending} type="submit">
                {current ? "설정 저장" : "준비 상태로 만들기"}
              </button>
              {current?.status === "DRAFT" ? (
                <button
                  className="button button--secondary"
                  disabled={statusMutation.isPending}
                  onClick={() => statusMutation.mutate({ id: current.id, status: "OPEN" })}
                  type="button"
                >
                  운영 시작·신규 신청 열기
                </button>
              ) : null}
              {current?.status === "OPEN" ? (
                <>
                  <button
                    className={current.registrationOpen ? "button button--outline" : "button button--secondary"}
                    disabled={registrationMutation.isPending}
                    onClick={() => registrationMutation.mutate({
                      id: current.id,
                      registrationOpen: !current.registrationOpen
                    })}
                    type="button"
                  >
                    {current.registrationOpen ? "신규 신청 마감" : "신규 신청 다시 열기"}
                  </button>
                  <button
                    className="button button--outline"
                    disabled={statusMutation.isPending}
                    onClick={closeCurrentRetreat}
                    type="button"
                  >
                    수련회 종료
                  </button>
                </>
              ) : null}
            </div>
          ) : (
            <p className="muted">수련회 설정 변경은 CHAIR 이상 권한이 필요합니다.</p>
          )}
        </form>
      </section>

      <div className="table-card">
        <table>
          <thead>
            <tr>
              <th>수련회</th>
              <th>기간</th>
              <th>상태</th>
              <th>신규 신청</th>
              <th>참가 인원</th>
              <th>기록</th>
            </tr>
          </thead>
          <tbody>
            {retreats.map((retreat) => (
              <tr key={retreat.id}>
                <td><strong>{retreat.name}</strong></td>
                <td>{retreat.startsOn} ~ {retreat.endsOn}</td>
                <td><span className={statusClassName(retreat.status)}>{statusLabel(retreat.status)}</span></td>
                <td>{retreat.status === "OPEN" ? (retreat.registrationOpen ? "접수 중" : "마감") : "-"}</td>
                <td>{retreat.participantCount == null ? "운영 중" : `${retreat.participantCount.toLocaleString()}명`}</td>
                <td>
                  <button
                    className="table-action"
                    onClick={() => setScheduleRetreatId(scheduleRetreatId === retreat.id ? null : retreat.id)}
                    type="button"
                  >
                    {scheduleRetreatId === retreat.id ? "시간표 닫기" : "시간표 보기"}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {retreatsQuery.isLoading ? <EmptyState title="수련회 기록을 불러오는 중입니다" message="잠시만 기다려 주세요." /> : null}
        {!retreatsQuery.isLoading && !retreats.length ? (
          <EmptyState title="저장된 수련회가 없습니다" message="다음 수련회를 준비 상태로 만들어 주세요." />
        ) : null}
      </div>

      {scheduleRetreatId !== null ? (
        <section className="panel page-stack">
          <div>
            <p className="eyebrow">Schedule archive</p>
            <h2>{scheduleRetreat?.name ?? "수련회"} 시간표</h2>
          </div>
          {schedulesQuery.isError ? <StatusMessage message={schedulesQuery.error.message} tone="error" /> : null}
          {schedulesQuery.isLoading ? <p className="muted">시간표를 불러오는 중입니다.</p> : null}
          {schedulesQuery.data?.length ? (
            <div className="stack">
              {schedulesQuery.data.map((schedule) => (
                <div className="result-card" key={schedule.id}>
                  <strong>{schedule.title}</strong>
                  <span className="muted">
                    {schedule.startsAt && schedule.endsAt
                      ? `${new Date(schedule.startsAt).toLocaleString("ko-KR", { timeZone: "Asia/Seoul" })} ~ ${new Date(schedule.endsAt).toLocaleTimeString("ko-KR", { timeZone: "Asia/Seoul" })}`
                      : `${schedule.scheduleDate} · 시간 미정`}
                    {schedule.location ? ` · ${schedule.location}` : ""}
                  </span>
                </div>
              ))}
            </div>
          ) : null}
          {!schedulesQuery.isLoading && !schedulesQuery.data?.length ? (
            <p className="muted">저장된 시간표가 없습니다.</p>
          ) : null}
        </section>
      ) : null}
    </section>
  );
}

function statusLabel(status: RetreatStatusValue) {
  return { DRAFT: "준비", OPEN: "운영 중", CLOSED: "종료" }[status];
}

function statusClassName(status: RetreatStatusValue) {
  if (status === "OPEN") return "status-pill status-pill--success";
  if (status === "DRAFT") return "status-pill status-pill--warning";
  return "status-pill status-pill--neutral";
}
