import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import {
  assignParticipantToRetreatGroup,
  assignRetreatGroupLeader,
  createRetreatGroup,
  getAdminRegistrations,
  getRetreatGroupMembers,
  getRetreatGroups,
  removeParticipantFromRetreatGroup,
  removeRetreatGroupLeader,
  updateRetreatGroup,
  updateRetreatGroupActive,
  type RetreatGroup,
  type RetreatGroupMember,
  type RetreatGroupPayload
} from "./adminApi";
import { EmptyState } from "../../shared/ui/EmptyState";
import { StatusMessage } from "../../shared/ui/StatusMessage";

export function AdminRetreatGroupsPage() {
  const queryClient = useQueryClient();

  const groupsQuery = useQuery({
    queryKey: ["admin", "retreat-groups"],
    queryFn: getRetreatGroups
  });
  const groups = groupsQuery.data ?? [];

  function invalidateGroups() {
    void queryClient.invalidateQueries({ queryKey: ["admin", "retreat-groups"] });
  }

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Retreat Groups</p>
          <h1>수련회 조 편성</h1>
        </div>
        <span className="pill">CHAIR 이상 변경 가능</span>
      </div>

      <GroupPanel groups={groups} onChanged={invalidateGroups} query={groupsQuery} />
      <AssignmentPanel groups={groups} />
    </section>
  );
}

type GroupFormValues = {
  name: string;
  description: string;
  displayOrder: string;
};

function GroupPanel({
  groups,
  onChanged,
  query
}: {
  groups: RetreatGroup[];
  onChanged: () => void;
  query: ReturnType<typeof useQuery<RetreatGroup[]>>;
}) {
  const [editingId, setEditingId] = useState<number | null>(null);
  const { register, handleSubmit, reset } = useForm<GroupFormValues>({
    defaultValues: { name: "", description: "", displayOrder: "0" }
  });

  const saveMutation = useMutation({
    mutationFn: (values: GroupFormValues) => {
      const payload: RetreatGroupPayload = {
        name: values.name,
        description: values.description || undefined,
        displayOrder: Number(values.displayOrder)
      };
      return editingId ? updateRetreatGroup(editingId, payload) : createRetreatGroup(payload);
    },
    onSuccess: () => {
      onChanged();
      setEditingId(null);
      reset({ name: "", description: "", displayOrder: "0" });
    }
  });

  const activeMutation = useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) => updateRetreatGroupActive(id, active),
    onSuccess: onChanged
  });

  function startEdit(group: RetreatGroup) {
    setEditingId(group.id);
    reset({
      name: group.name,
      description: group.description ?? "",
      displayOrder: String(group.displayOrder)
    });
  }

  function cancelEdit() {
    setEditingId(null);
    reset({ name: "", description: "", displayOrder: "0" });
  }

  return (
    <section className="panel">
      <h2>조 목록</h2>

      {query.isError ? <StatusMessage message={query.error.message} tone="error" /> : null}
      {saveMutation.isError ? <StatusMessage message={saveMutation.error.message} tone="error" /> : null}
      {activeMutation.isError ? <StatusMessage message={activeMutation.error.message} tone="error" /> : null}

      <form className="form-grid" onSubmit={handleSubmit((values) => saveMutation.mutate(values))}>
        <label>
          이름
          <input {...register("name", { required: true })} placeholder="1조" />
        </label>
        <label>
          설명
          <input {...register("description")} placeholder="설명" />
        </label>
        <label>
          순서
          <input {...register("displayOrder", { required: true })} inputMode="numeric" />
        </label>
        <div className="table-actions">
          <button className="button button--primary" disabled={saveMutation.isPending} type="submit">
            {editingId ? "수정 저장" : "조 추가"}
          </button>
          {editingId ? (
            <button className="button button--secondary" onClick={cancelEdit} type="button">
              취소
            </button>
          ) : null}
        </div>
      </form>

      <div className="table-card">
        <table>
          <thead>
            <tr>
              <th>이름</th>
              <th>설명</th>
              <th>순서</th>
              <th>상태</th>
              <th>처리</th>
            </tr>
          </thead>
          <tbody>
            {groups.map((group) => (
              <tr key={group.id}>
                <td>
                  <strong>{group.name}</strong>
                </td>
                <td>{group.description ?? "-"}</td>
                <td>{group.displayOrder}</td>
                <td>
                  <span className={group.active ? "status-pill status-pill--success" : "status-pill status-pill--neutral"}>
                    {group.active ? "활성" : "비활성"}
                  </span>
                </td>
                <td>
                  <div className="table-actions">
                    <button className="table-action" onClick={() => startEdit(group)} type="button">
                      수정
                    </button>
                    <button
                      className={group.active ? "table-action table-action--warning" : "table-action"}
                      disabled={activeMutation.isPending}
                      onClick={() => activeMutation.mutate({ id: group.id, active: !group.active })}
                      type="button"
                    >
                      {group.active ? "비활성화" : "활성화"}
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {query.isLoading ? <EmptyState title="수련회 조 목록을 불러오는 중입니다" message="잠시만 기다려 주세요." /> : null}
        {!query.isLoading && !groups.length ? (
          <EmptyState title="등록된 수련회 조가 없습니다" message="위 양식으로 조를 추가해 주세요." />
        ) : null}
      </div>
    </section>
  );
}

function AssignmentPanel({ groups }: { groups: RetreatGroup[] }) {
  const queryClient = useQueryClient();
  const [groupId, setGroupId] = useState<number | "">("");
  const [participantKeyword, setParticipantKeyword] = useState("");
  const [selectedParticipantId, setSelectedParticipantId] = useState<number | "">("");

  const membersQuery = useQuery({
    queryKey: ["admin", "retreat-groups", groupId, "members"],
    queryFn: () => getRetreatGroupMembers(groupId as number),
    enabled: groupId !== ""
  });
  const members = membersQuery.data ?? [];

  const registrationsQuery = useQuery({
    queryKey: ["admin", "registrations", "for-assignment"],
    queryFn: () => getAdminRegistrations(100)
  });
  const registrations = registrationsQuery.data?.content;

  const unassignedParticipants = useMemo(() => {
    const keyword = participantKeyword.trim().toLowerCase();
    return (registrations ?? []).filter((item) => {
      if (item.status !== "REGISTERED" || item.retreatGroupId != null) {
        return false;
      }
      return keyword.length === 0 || item.name.toLowerCase().includes(keyword);
    });
  }, [participantKeyword, registrations]);

  function invalidateMembers() {
    void queryClient.invalidateQueries({ queryKey: ["admin", "retreat-groups", groupId, "members"] });
    void queryClient.invalidateQueries({ queryKey: ["admin", "registrations", "for-assignment"] });
  }

  const leaderMutation = useMutation({
    mutationFn: ({ participantId }: { participantId: number }) =>
      assignRetreatGroupLeader(groupId as number, participantId),
    onSuccess: invalidateMembers
  });

  const removeLeaderMutation = useMutation({
    mutationFn: () => removeRetreatGroupLeader(groupId as number),
    onSuccess: invalidateMembers
  });

  const removeMemberMutation = useMutation({
    mutationFn: (participantId: number) => removeParticipantFromRetreatGroup(participantId),
    onSuccess: invalidateMembers
  });

  const addMemberMutation = useMutation({
    mutationFn: (participantId: number) => assignParticipantToRetreatGroup(participantId, groupId as number),
    onSuccess: () => {
      invalidateMembers();
      setSelectedParticipantId("");
      setParticipantKeyword("");
    }
  });

  const mutationError =
    leaderMutation.error ?? removeLeaderMutation.error ?? removeMemberMutation.error ?? addMemberMutation.error;
  const actionPending =
    leaderMutation.isPending ||
    removeLeaderMutation.isPending ||
    removeMemberMutation.isPending ||
    addMemberMutation.isPending;

  return (
    <section className="panel">
      <h2>조원 배정</h2>

      {membersQuery.isError ? <StatusMessage message={membersQuery.error.message} tone="error" /> : null}
      {mutationError ? <StatusMessage message={mutationError.message} tone="error" /> : null}

      <section className="filter-panel" aria-label="조원 배정 대상 조 선택">
        <label>
          조 선택
          <select
            onChange={(event) => setGroupId(event.target.value ? Number(event.target.value) : "")}
            value={groupId}
          >
            <option value="">조를 선택하세요</option>
            {groups.map((group) => (
              <option key={group.id} value={group.id}>
                {group.name}
              </option>
            ))}
          </select>
        </label>
        <div className="filter-summary">
          <span>조원 수</span>
          <strong>{members.length}</strong>
        </div>
      </section>

      {groupId === "" ? (
        <EmptyState title="조를 선택해 주세요" message="위에서 조를 선택하면 조원 목록과 배정 도구가 표시됩니다." />
      ) : (
        <>
          <div className="table-card">
            <table>
              <thead>
                <tr>
                  <th>이름</th>
                  <th>성별</th>
                  <th>출생연도</th>
                  <th>교회 셀</th>
                  <th>조장</th>
                  <th>처리</th>
                </tr>
              </thead>
              <tbody>
                {members.map((member) => (
                  <MemberRow
                    actionPending={actionPending}
                    key={member.id}
                    member={member}
                    onAssignLeader={() => leaderMutation.mutate({ participantId: member.participantId })}
                    onRemove={() => removeMemberMutation.mutate(member.participantId)}
                    onRemoveLeader={() => removeLeaderMutation.mutate()}
                  />
                ))}
              </tbody>
            </table>
            {membersQuery.isLoading ? (
              <EmptyState title="조원 목록을 불러오는 중입니다" message="잠시만 기다려 주세요." />
            ) : null}
            {!membersQuery.isLoading && !members.length ? (
              <EmptyState title="아직 배정된 조원이 없습니다" message="아래에서 참가자를 검색해 추가해 주세요." />
            ) : null}
          </div>

          <form
            className="form-grid"
            onSubmit={(event) => {
              event.preventDefault();
              if (selectedParticipantId !== "") {
                addMemberMutation.mutate(selectedParticipantId);
              }
            }}
          >
            <label>
              참가자 검색
              <input
                onChange={(event) => setParticipantKeyword(event.target.value)}
                placeholder="이름으로 검색"
                type="search"
                value={participantKeyword}
              />
            </label>
            <label>
              미배정 참가자
              <select
                onChange={(event) =>
                  setSelectedParticipantId(event.target.value ? Number(event.target.value) : "")
                }
                value={selectedParticipantId}
              >
                <option value="">참가자를 선택하세요</option>
                {unassignedParticipants.map((participant) => (
                  <option key={participant.id} value={participant.id}>
                    {participant.name} ({participant.birthYear})
                  </option>
                ))}
              </select>
            </label>
            <button
              className="button button--primary"
              disabled={selectedParticipantId === "" || addMemberMutation.isPending}
              type="submit"
            >
              조에 추가
            </button>
          </form>
        </>
      )}
    </section>
  );
}

function MemberRow({
  actionPending,
  member,
  onAssignLeader,
  onRemove,
  onRemoveLeader
}: {
  actionPending: boolean;
  member: RetreatGroupMember;
  onAssignLeader: () => void;
  onRemove: () => void;
  onRemoveLeader: () => void;
}) {
  return (
    <tr>
      <td>
        <strong>{member.participantName}</strong>
      </td>
      <td>{member.gender === "FEMALE" ? "여성" : "남성"}</td>
      <td>{member.birthYear}</td>
      <td>{member.churchCellName ?? member.churchCellDepartment ?? "-"}</td>
      <td>
        <span className={member.leader ? "status-pill status-pill--success" : "status-pill status-pill--neutral"}>
          {member.leader ? "조장" : "조원"}
        </span>
      </td>
      <td>
        <div className="table-actions">
          {member.leader ? (
            <button className="table-action table-action--warning" disabled={actionPending} onClick={onRemoveLeader} type="button">
              조장 해제
            </button>
          ) : (
            <button className="table-action" disabled={actionPending} onClick={onAssignLeader} type="button">
              조장 지정
            </button>
          )}
          <button className="table-action table-action--warning" disabled={actionPending} onClick={onRemove} type="button">
            배정 해제
          </button>
        </div>
      </td>
    </tr>
  );
}
