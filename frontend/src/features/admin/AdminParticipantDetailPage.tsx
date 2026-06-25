import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, type ReactNode } from "react";
import { Link, useParams } from "react-router-dom";
import {
  getAdminRegistration,
  getAdminProfile,
  updateRegistrationFeePaid,
  getCheckInDetail,
  manuallyCheckIn,
  cancelCheckIn,
  getRegistrationHistories,
  getFeeEvents
} from "./adminApi";
import { StatusMessage } from "../../shared/ui/StatusMessage";

export function AdminParticipantDetailPage() {
  const params = useParams();
  const participantId = Number(params.participantId);
  const validParticipantId = Number.isInteger(participantId) && participantId > 0;
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: ["admin", "registrations", participantId],
    queryFn: () => getAdminRegistration(participantId),
    enabled: validParticipantId
  });

  const profileQuery = useQuery({
    queryKey: ["admin", "me"],
    queryFn: getAdminProfile
  });

  const checkInQuery = useQuery({
    queryKey: ["admin", "check-ins", participantId],
    queryFn: () => getCheckInDetail(participantId),
    enabled: validParticipantId
  });

  const feeEventsQuery = useQuery({
    queryKey: ["admin", "fees", participantId, "events"],
    queryFn: () => getFeeEvents(participantId),
    enabled: validParticipantId
  });

  const historiesQuery = useQuery({
    queryKey: ["admin", "registrations", participantId, "histories"],
    queryFn: () => getRegistrationHistories(participantId),
    enabled: validParticipantId
  });

  if (!validParticipantId) {
    return <StatusMessage message="올바르지 않은 참가자 ID입니다." tone="error" />;
  }

  const participant = query.data;
  const isChairPlus = profileQuery.data?.role !== undefined && profileQuery.data.role !== "STAFF";

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Participant Detail</p>
          <h1>{participant?.name ?? "참가자 상세"}</h1>
        </div>
        <Link className="button button--secondary" to="/admin/participants">
          목록으로
        </Link>
      </div>

      <StatusMessage message="이 화면은 민감한 참가자 상세 정보를 조회하므로 서버에 개인정보 접근 로그가 기록됩니다." />
      {query.isError ? <StatusMessage message={query.error.message} tone="error" /> : null}
      {query.isLoading ? <p className="table-empty">상세 정보를 불러오는 중...</p> : null}

      {participant ? (
        <>
          <section className="detail-grid">
            <DetailCard title="기본 정보">
              <DetailRow label="이름" value={participant.name} />
              <DetailRow label="성별" value={participant.gender === "FEMALE" ? "여성" : "남성"} />
              <DetailRow label="출생연도" value={`${participant.birthYear}`} />
              <DetailRow label="전화번호" value={participant.phoneNumber} />
            </DetailCard>
            <DetailCard title="운영 상태">
              <DetailRow label="등록 상태" value={participant.status === "REGISTERED" ? "등록 완료" : "취소"} />
              <DetailRow label="참가비" value={participant.feePaid ? "납부" : "미납"} />
              <DetailRow label="새가족" value={participant.newcomer ? "예" : "아니오"} />
              <DetailRow label="돌봄 대상" value={participant.careTarget ? "예" : "아니오"} />
            </DetailCard>
            <DetailCard title="소속/배정">
              <DetailRow label="자유 입력 셀" value={participant.churchCellDepartment ?? "-"} />
              <DetailRow label="중그룹" value={participant.middleGroupName ?? "-"} />
              <DetailRow label="교회 셀" value={participant.churchCellName ?? "-"} />
              <DetailRow
                label="수련회 조"
                value={`${participant.retreatGroupName ?? "-"}${participant.retreatGroupLeader ? " / 조장" : ""}`}
              />
            </DetailCard>
            <DetailCard title="참석 방식">
              <DetailRow label="참석 유형" value={participant.attendanceType === "FULL" ? "전체 참석" : "부분 참석"} />
              {participant.attendanceType === "PARTIAL" && (
                <>
                  <DetailRow label="예정 도착 시간" value={participant.plannedArrivalAt ? formatDateTime(participant.plannedArrivalAt) : "-"} />
                  <DetailRow label="예정 출발 시간" value={participant.plannedDepartureAt ? formatDateTime(participant.plannedDepartureAt) : "-"} />
                  <DetailRow label="부분참석 메모" value={participant.partialAttendanceNote ?? "-"} />
                </>
              )}
              <DetailRow label="1박" value={participant.lodgingNight1 ? "예" : "아니오"} />
              <DetailRow label="2박" value={participant.lodgingNight2 ? "예" : "아니오"} />
            </DetailCard>
            <DetailCard title="교통편 - 가는 길">
              <DetailRow label="교통편" value={formatTransportation(participant.inboundTransportationMethod)} />
              {(participant.inboundTransportationMethod === "CARPOOL_NEEDED" || participant.inboundCarpoolAvailable) && (
                <>
                  <DetailRow label="카풀 제공 가능" value={participant.inboundCarpoolAvailable ? "예" : "아니오"} />
                  <DetailRow label="카풀 인원" value={participant.inboundCarpoolSeats ? String(participant.inboundCarpoolSeats) : "-"} />
                  <DetailRow label="카풀 지역" value={participant.inboundCarpoolArea ?? "-"} />
                  <DetailRow label="카풀 루트 지역" value={participant.inboundCarpoolRouteArea ?? "-"} />
                  <DetailRow label="카풀 메모" value={participant.inboundCarpoolNote ?? "-"} />
                  <DetailRow label="선호 지역" value={participant.inboundCarpoolPreferredArea ?? "-"} />
                  <DetailRow label="선호 메모" value={participant.inboundCarpoolPreferredNote ?? "-"} />
                </>
              )}
              {(participant.inboundTransportationMethod === "WORSHIP_SHUTTLE" || participant.inboundTransportationMethod === "GROUP_BUS") && (
                <DetailRow label="경배 버스 슬롯" value={participant.inboundWorshipBusRideSlot ?? "-"} />
              )}
            </DetailCard>
            <DetailCard title="교통편 - 오는 길">
              <DetailRow label="교통편" value={formatTransportation(participant.outboundTransportationMethod)} />
              {(participant.outboundTransportationMethod === "CARPOOL_NEEDED" || participant.outboundCarpoolAvailable) && (
                <>
                  <DetailRow label="카풀 제공 가능" value={participant.outboundCarpoolAvailable ? "예" : "아니오"} />
                  <DetailRow label="카풀 인원" value={participant.outboundCarpoolSeats ? String(participant.outboundCarpoolSeats) : "-"} />
                  <DetailRow label="카풀 지역" value={participant.outboundCarpoolArea ?? "-"} />
                  <DetailRow label="카풀 루트 지역" value={participant.outboundCarpoolRouteArea ?? "-"} />
                  <DetailRow label="카풀 메모" value={participant.outboundCarpoolNote ?? "-"} />
                  <DetailRow label="선호 지역" value={participant.outboundCarpoolPreferredArea ?? "-"} />
                  <DetailRow label="선호 메모" value={participant.outboundCarpoolPreferredNote ?? "-"} />
                </>
              )}
              {(participant.outboundTransportationMethod === "WORSHIP_SHUTTLE" || participant.outboundTransportationMethod === "GROUP_BUS") && (
                <DetailRow label="경배 버스 슬롯" value={participant.outboundWorshipBusRideSlot ?? "-"} />
              )}
            </DetailCard>
          </section>

          <FeeManagementSection participantId={participantId} participant={participant} isChairPlus={isChairPlus} queryClient={queryClient} feeEventsQuery={feeEventsQuery} />

          <CheckInManagementSection
            participantId={participantId}
            checkInQuery={checkInQuery}
            isChairPlus={isChairPlus}
            queryClient={queryClient}
          />

          <HistoriesSection historiesQuery={historiesQuery} />

          <section className="panel">
            <h2>관리자 메모</h2>
            <p className="muted detail-memo">{participant.adminMemo?.trim() ? participant.adminMemo : "메모가 없습니다."}</p>
          </section>

          <section className="panel">
            <h2>기록</h2>
            <div className="detail-list">
              <DetailRow label="등록 시각" value={formatDateTime(participant.createdAt)} />
              <DetailRow label="수정 시각" value={formatDateTime(participant.updatedAt)} />
            </div>
          </section>
        </>
      ) : null}
    </section>
  );
}

function FeeManagementSection({
  participantId,
  participant,
  isChairPlus,
  queryClient,
  feeEventsQuery
}: {
  participantId: number;
  participant: any;
  isChairPlus: boolean;
  queryClient: any;
  feeEventsQuery: any;
}) {
  const [showReasonInput, setShowReasonInput] = useState(false);
  const [reason, setReason] = useState("");

  const mutation = useMutation({
    mutationFn: ({ feePaid, reason }: { feePaid: boolean; reason?: string }) =>
      updateRegistrationFeePaid(participantId, feePaid, reason),
    onSuccess: () => {
      setShowReasonInput(false);
      setReason("");
      void queryClient.invalidateQueries({ queryKey: ["admin", "registrations", participantId] });
      void queryClient.invalidateQueries({ queryKey: ["admin", "fees", participantId, "events"] });
    }
  });

  const handleMarkPaid = () => {
    mutation.mutate({ feePaid: true });
  };

  const handleMarkUnpaid = () => {
    if (!showReasonInput) {
      setShowReasonInput(true);
      return;
    }
    if (!reason.trim()) {
      alert("사유를 입력해주세요.");
      return;
    }
    mutation.mutate({ feePaid: false, reason });
  };

  return (
    <section className="panel">
      <h2>참가비 관리</h2>
      <div className="detail-list">
        <div className="detail-row">
          <span>현재 상태</span>
          <strong>
            <span className={participant.feePaid ? "status-pill status-pill--success" : "status-pill status-pill--warning"}>
              {participant.feePaid ? "납부" : "미납"}
            </span>
          </strong>
        </div>
      </div>

      {isChairPlus && (
        <div className="table-actions" style={{ marginTop: "1rem" }}>
          <button
            className="button button--primary"
            disabled={participant.feePaid || mutation.isPending}
            onClick={handleMarkPaid}
            type="button"
          >
            {mutation.isPending ? "처리 중..." : "납부 확인"}
          </button>
          <button
            className="button button--ghost"
            disabled={!participant.feePaid || mutation.isPending}
            onClick={handleMarkUnpaid}
            type="button"
          >
            {showReasonInput ? "미납 처리" : "미납 처리"}
          </button>
          {showReasonInput && (
            <button
              className="button button--ghost"
              onClick={() => {
                setShowReasonInput(false);
                setReason("");
              }}
              type="button"
            >
              취소
            </button>
          )}
        </div>
      )}

      {showReasonInput && isChairPlus && (
        <div style={{ marginTop: "1rem" }}>
          <label style={{ display: "block", marginBottom: "0.5rem" }}>
            미납 사유
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="미납 처리 사유를 입력해주세요"
              style={{ width: "100%", minHeight: "80px", padding: "0.5rem", marginTop: "0.5rem" }}
            />
          </label>
        </div>
      )}

      {mutation.isError && <StatusMessage message={mutation.error.message} tone="error" />}

      <div style={{ marginTop: "1.5rem" }}>
        <h3 style={{ fontSize: "0.9rem", color: "var(--color-muted)", marginBottom: "1rem" }}>변경 이력</h3>
        {feeEventsQuery.isLoading ? (
          <p className="muted">로드 중...</p>
        ) : !feeEventsQuery.data?.length ? (
          <p className="muted">변경 이력이 없습니다.</p>
        ) : (
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
              <tr style={{ borderBottom: "1px solid var(--color-border)" }}>
                <th style={{ textAlign: "left", padding: "0.5rem", fontSize: "0.85rem" }}>시간</th>
                <th style={{ textAlign: "left", padding: "0.5rem", fontSize: "0.85rem" }}>처리자</th>
                <th style={{ textAlign: "left", padding: "0.5rem", fontSize: "0.85rem" }}>사유</th>
              </tr>
            </thead>
            <tbody>
              {feeEventsQuery.data?.map((event: any) => (
                <tr key={event.id} style={{ borderBottom: "1px solid var(--color-border)" }}>
                  <td style={{ padding: "0.5rem", fontSize: "0.85rem" }}>{formatDateTime(event.createdAt)}</td>
                  <td style={{ padding: "0.5rem", fontSize: "0.85rem" }}>{event.changedBy?.name ?? "-"}</td>
                  <td style={{ padding: "0.5rem", fontSize: "0.85rem" }}>{event.reason ?? "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </section>
  );
}

function CheckInManagementSection({
  participantId,
  checkInQuery,
  isChairPlus,
  queryClient
}: {
  participantId: number;
  checkInQuery: any;
  isChairPlus: boolean;
  queryClient: any;
}) {
  const [showCancelReason, setShowCancelReason] = useState(false);
  const [cancelReason, setCancelReason] = useState("");

  const checkInMutation = useMutation({
    mutationFn: () => manuallyCheckIn(participantId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["admin", "check-ins", participantId] });
    }
  });

  const cancelMutation = useMutation({
    mutationFn: (reason: string) => cancelCheckIn(participantId, reason),
    onSuccess: () => {
      setShowCancelReason(false);
      setCancelReason("");
      void queryClient.invalidateQueries({ queryKey: ["admin", "check-ins", participantId] });
    }
  });

  const checkIn = checkInQuery.data;
  const isCheckedIn = checkIn?.checkedIn;

  const handleCheckIn = () => {
    checkInMutation.mutate();
  };

  const handleCancelCheckIn = () => {
    if (!showCancelReason) {
      setShowCancelReason(true);
      return;
    }
    if (!cancelReason.trim()) {
      alert("취소 사유를 입력해주세요.");
      return;
    }
    cancelMutation.mutate(cancelReason);
  };

  return (
    <section className="panel">
      <h2>체크인 관리</h2>
      {checkInQuery.isLoading ? (
        <p className="muted">로드 중...</p>
      ) : (
        <>
          <div className="detail-list">
            <div className="detail-row">
              <span>체크인 상태</span>
              <strong>
                <span className={isCheckedIn ? "status-pill status-pill--success" : "status-pill status-pill--neutral"}>
                  {isCheckedIn ? "완료" : "미완료"}
                </span>
              </strong>
            </div>
            {isCheckedIn && (
              <>
                <DetailRow label="체크인 시간" value={checkIn.checkedInAt ? formatDateTime(checkIn.checkedInAt) : "-"} />
                {checkIn.checkInMethod && <DetailRow label="처리 방법" value={checkIn.checkInMethod === "MANUAL" ? "수동" : "QR"} />}
                {checkIn.checkedInBy && <DetailRow label="처리자" value={checkIn.checkedInBy.name} />}
              </>
            )}
            {checkIn?.cancelledAt && (
              <>
                <DetailRow label="취소 시간" value={formatDateTime(checkIn.cancelledAt)} />
                {checkIn.cancelledBy && <DetailRow label="취소자" value={checkIn.cancelledBy.name} />}
              </>
            )}
          </div>

          <div className="table-actions" style={{ marginTop: "1rem" }}>
            {!isCheckedIn && (
              <button className="button button--primary" disabled={checkInMutation.isPending} onClick={handleCheckIn} type="button">
                {checkInMutation.isPending ? "처리 중..." : "체크인 완료"}
              </button>
            )}
            {isCheckedIn && isChairPlus && (
              <>
                <button className="button button--ghost" onClick={handleCancelCheckIn} disabled={cancelMutation.isPending} type="button">
                  {showCancelReason ? "취소 처리" : "체크인 취소"}
                </button>
                {showCancelReason && (
                  <button
                    className="button button--ghost"
                    onClick={() => {
                      setShowCancelReason(false);
                      setCancelReason("");
                    }}
                    type="button"
                  >
                    돌아가기
                  </button>
                )}
              </>
            )}
          </div>

          {showCancelReason && isChairPlus && (
            <div style={{ marginTop: "1rem" }}>
              <label style={{ display: "block", marginBottom: "0.5rem" }}>
                취소 사유
                <textarea
                  value={cancelReason}
                  onChange={(e) => setCancelReason(e.target.value)}
                  placeholder="체크인 취소 사유를 입력해주세요"
                  style={{ width: "100%", minHeight: "80px", padding: "0.5rem", marginTop: "0.5rem" }}
                />
              </label>
            </div>
          )}

          {(checkInMutation.isError || cancelMutation.isError) && (
            <StatusMessage
              message={(checkInMutation.error ?? cancelMutation.error)?.message ?? "오류가 발생했습니다."}
              tone="error"
            />
          )}
        </>
      )}
    </section>
  );
}

function HistoriesSection({ historiesQuery }: { historiesQuery: any }) {
  const changeTypeLabels: Record<string, string> = {
    FEE_PAYMENT_UPDATED: "참가비 변경",
    STATUS_UPDATED: "등록 상태 변경",
    ADMIN_MANAGEMENT_UPDATED: "관리 정보 변경",
    CHURCH_CELL_UPDATED: "교회 셀 변경"
  };

  return (
    <section className="panel">
      <h2>변경 이력</h2>
      {historiesQuery.isLoading ? (
        <p className="muted">로드 중...</p>
      ) : !historiesQuery.data?.length ? (
        <p className="muted">변경 이력이 없습니다.</p>
      ) : (
        <table style={{ width: "100%", borderCollapse: "collapse" }}>
          <thead>
            <tr style={{ borderBottom: "1px solid var(--color-border)" }}>
              <th style={{ textAlign: "left", padding: "0.5rem", fontSize: "0.85rem" }}>시간</th>
              <th style={{ textAlign: "left", padding: "0.5rem", fontSize: "0.85rem" }}>변경 유형</th>
              <th style={{ textAlign: "left", padding: "0.5rem", fontSize: "0.85rem" }}>처리자</th>
            </tr>
          </thead>
          <tbody>
            {historiesQuery.data?.map((history: any) => (
              <tr key={history.id} style={{ borderBottom: "1px solid var(--color-border)" }}>
                <td style={{ padding: "0.5rem", fontSize: "0.85rem" }}>{formatDateTime(history.createdAt)}</td>
                <td style={{ padding: "0.5rem", fontSize: "0.85rem" }}>{changeTypeLabels[history.changeType] ?? history.changeType}</td>
                <td style={{ padding: "0.5rem", fontSize: "0.85rem" }}>
                  {history.actorType === "PARTICIPANT" ? "참가자 본인" : "관리자"}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}

function DetailCard({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="panel detail-card">
      <h2>{title}</h2>
      <div className="detail-list">{children}</div>
    </section>
  );
}

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="detail-row">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString();
}

function formatTransportation(method: string | null): string {
  if (!method) return "-";
  switch (method) {
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
      return method;
  }
}
