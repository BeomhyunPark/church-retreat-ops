import { useQuery } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { Link, useParams } from "react-router-dom";
import { getAdminRegistration } from "./adminApi";
import { StatusMessage } from "../../shared/ui/StatusMessage";

export function AdminParticipantDetailPage() {
  const params = useParams();
  const participantId = Number(params.participantId);
  const validParticipantId = Number.isInteger(participantId) && participantId > 0;

  const query = useQuery({
    queryKey: ["admin", "registrations", participantId],
    queryFn: () => getAdminRegistration(participantId),
    enabled: validParticipantId
  });

  if (!validParticipantId) {
    return <StatusMessage message="올바르지 않은 참가자 ID입니다." tone="error" />;
  }

  const participant = query.data;

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
              <DetailRow label="참석 형태" value={participant.attendanceType === "FULL" ? "전체참석" : "부분참석"} />
              <DetailRow label="참석 시간" value={formatAttendanceSlots(participant.attendanceSlots)} />
              <DetailRow label="이동 수단" value={transportationLabel(participant.transportationType)} />
              <DetailRow label="카풀" value={formatCarpool(participant)} />
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
          </section>

          <section className="panel">
            <h2>이동 메모</h2>
            <p className="muted detail-memo">
              {participant.transportationNote?.trim() ? participant.transportationNote : "메모가 없습니다."}
            </p>
          </section>

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

function transportationLabel(value: "OWN_CAR" | "PUBLIC_TRANSPORT" | "UNDECIDED") {
  if (value === "OWN_CAR") return "자차";
  if (value === "PUBLIC_TRANSPORT") return "대중교통";
  return "미정";
}

function formatCarpool(participant: {
  carpoolNeeded: boolean;
  carpoolOffer: boolean;
  carpoolSeats?: number | null;
}) {
  if (participant.carpoolNeeded) {
    return "카풀 희망";
  }
  if (participant.carpoolOffer) {
    return `카풀 가능 ${participant.carpoolSeats ?? 0}명`;
  }
  return "-";
}

function formatAttendanceSlots(values: string[]) {
  if (!values.length) {
    return "-";
  }

  return values.map(attendanceSlotLabel).join(", ");
}

function attendanceSlotLabel(value: string) {
  const labels: Record<string, string> = {
    DAY1_MORNING: "첫째날 오전",
    DAY1_AFTERNOON: "첫째날 오후",
    DAY1_GATHERING: "첫째날 집회",
    DAY2_MORNING: "둘째날 오전",
    DAY2_AFTERNOON: "둘째날 오후",
    DAY2_GATHERING: "둘째날 집회",
    DAY3_MORNING: "셋째날 오전",
    DAY3_AFTERNOON: "셋째날 오후",
    DAY3_GATHERING: "셋째날 집회"
  };

  return labels[value] ?? value;
}
