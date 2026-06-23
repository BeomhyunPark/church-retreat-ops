import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  getAnnouncements,
  updateAnnouncementActive,
  updateAnnouncementPinned,
  type Announcement
} from "./adminApi";
import { EmptyState } from "../../shared/ui/EmptyState";
import { StatusMessage } from "../../shared/ui/StatusMessage";

export function AdminAnnouncementsPage() {
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: ["admin", "announcements"],
    queryFn: getAnnouncements
  });
  const activeMutation = useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) => updateAnnouncementActive(id, active),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["admin", "announcements"] });
    }
  });
  const pinnedMutation = useMutation({
    mutationFn: ({ id, pinned }: { id: number; pinned: boolean }) => updateAnnouncementPinned(id, pinned),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["admin", "announcements"] });
    }
  });
  const announcements = query.data ?? [];
  const mutationError = activeMutation.error ?? pinnedMutation.error;
  const actionPending = activeMutation.isPending || pinnedMutation.isPending;

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Announcements</p>
          <h1>공지 관리</h1>
        </div>
        <span className="pill">CHAIR 이상 변경 가능</span>
      </div>

      {query.isError ? <StatusMessage message={query.error.message} tone="error" /> : null}
      {mutationError ? <StatusMessage message={mutationError.message} tone="error" /> : null}

      <div className="table-card">
        <table>
          <thead>
            <tr>
              <th>공지</th>
              <th>상태</th>
              <th>노출 기간</th>
              <th>대상</th>
              <th>수정자</th>
              <th>처리</th>
            </tr>
          </thead>
          <tbody>
            {announcements.map((item) => (
              <AnnouncementRow
                actionPending={actionPending}
                announcement={item}
                key={item.id}
                onActiveChange={(active) => activeMutation.mutate({ id: item.id, active })}
                onPinnedChange={(pinned) => pinnedMutation.mutate({ id: item.id, pinned })}
              />
            ))}
          </tbody>
        </table>
        {query.isLoading ? <EmptyState title="공지 목록을 불러오는 중입니다" message="잠시만 기다려 주세요." /> : null}
        {!query.isLoading && !announcements.length ? (
          <EmptyState title="등록된 공지가 없습니다" message="공지 작성 기능을 연결하면 이곳에서 운영 공지를 관리할 수 있습니다." />
        ) : null}
      </div>
    </section>
  );
}

function AnnouncementRow({
  actionPending,
  announcement,
  onActiveChange,
  onPinnedChange
}: {
  actionPending: boolean;
  announcement: Announcement;
  onActiveChange: (active: boolean) => void;
  onPinnedChange: (pinned: boolean) => void;
}) {
  return (
    <tr>
      <td>
        <strong>{announcement.title}</strong>
        <span className="table-note">{announcement.content}</span>
      </td>
      <td>
        <span className={announcement.active ? "status-pill status-pill--success" : "status-pill status-pill--neutral"}>
          {announcement.active ? "공개" : "비공개"}
        </span>
        {announcement.pinned ? <span className="table-note">상단 고정</span> : null}
      </td>
      <td>{visibleRange(announcement)}</td>
      <td>{targetLabel(announcement)}</td>
      <td>{announcement.updatedBy?.name ?? "-"}</td>
      <td>
        <div className="table-actions">
          <button
            className={announcement.active ? "table-action table-action--warning" : "table-action"}
            disabled={actionPending}
            onClick={() => onActiveChange(!announcement.active)}
            type="button"
          >
            {announcement.active ? "비공개" : "공개"}
          </button>
          <button
            className={announcement.pinned ? "table-action table-action--warning" : "table-action"}
            disabled={actionPending}
            onClick={() => onPinnedChange(!announcement.pinned)}
            type="button"
          >
            {announcement.pinned ? "고정 해제" : "고정"}
          </button>
        </div>
      </td>
    </tr>
  );
}

function visibleRange(announcement: Announcement) {
  if (!announcement.visibleFrom && !announcement.visibleUntil) {
    return "항상";
  }

  return `${formatDate(announcement.visibleFrom)} - ${formatDate(announcement.visibleUntil)}`;
}

function formatDate(value?: string | null) {
  return value ? new Date(value).toLocaleDateString() : "제한 없음";
}

function targetLabel(announcement: Announcement) {
  if (!announcement.targets.length) {
    return "전체";
  }

  return announcement.targets.map((target) => target.targetValue ?? target.targetType).join(", ");
}
