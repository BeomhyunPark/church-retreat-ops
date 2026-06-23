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
