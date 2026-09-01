import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  assignParticipantToRetreatGroup,
  assignRetreatGroupLeader,
  createRetreatGroup,
  deleteRetreatGroup,
  getAdminRegistrations,
  getParticipationOptions,
  getRetreatGroups,
  getRetreatGroupTree,
  removeParticipantFromRetreatGroup,
  removeRetreatGroupLeader,
  updateRetreatGroup,
  updateRetreatGroupMemberOrder,
  type AdminRegistration,
  type ParticipationOption,
  type RetreatGroup
} from "./adminApi";
import { EmptyState } from "../../shared/ui/EmptyState";
import { StatusMessage } from "../../shared/ui/StatusMessage";

// ─── Types & Constants ────────────────────────────────────────────────────────

type DragDropState = { [participantId: number]: number | null };
type MemberOrderState = { [groupId: number]: number[] };

type BoardGroup = RetreatGroup & { isNew?: boolean };

function renumberBoardGroups(orderedGroups: BoardGroup[]) {
  return orderedGroups.map((group, index) => ({
    ...group,
    name: `${index + 1}조`,
    displayOrder: index + 1
  }));
}

function createEmptyBoardGroup(id: number): BoardGroup {
  return {
    id,
    name: "1조",
    description: "",
    displayOrder: 1,
    active: true,
    createdAt: "",
    updatedAt: "",
    isNew: true
  };
}

type AttendanceSlot = number;

const COLOR_FULL = "#22c55e";
const COLOR_PARTIAL = "#f97316";
const COLOR_EMPTY = "#e5e7eb";

function getAttendanceSlots(reg: AdminRegistration): AttendanceSlot[] {
  return reg.selectedOptionIds;
}

function getSegments(attended: AttendanceSlot[], slots: ParticipationOption[]): { start: number; end: number }[] {
  const segs: { start: number; end: number }[] = [];
  let cur: number | null = null;
  slots.forEach(({ id }, i) => {
    if (attended.includes(id)) {
      if (cur === null) cur = i;
    } else if (cur !== null) {
      segs.push({ start: cur, end: i - 1 });
      cur = null;
    }
  });
  if (cur !== null) segs.push({ start: cur, end: slots.length - 1 });
  return segs;
}

// ─── Page ────────────────────────────────────────────────────────────────────

export function AdminRetreatGroupsPage() {
  const queryClient = useQueryClient();
  const groupsQuery = useQuery({ queryKey: ["admin", "retreat-groups"], queryFn: getRetreatGroups });
  const participationOptionsQuery = useQuery({
    queryKey: ["admin", "participation-options"],
    queryFn: getParticipationOptions
  });
  const groups = groupsQuery.data ?? [];
  const slots = (participationOptionsQuery.data ?? []).filter((option) => option.active);
  const groupsKey = groups
    .map((group) => `${group.id}:${group.name}:${group.displayOrder}:${group.active}`)
    .join("|");

  return (
    <section className="page-stack">
      <RetreatGroupBoard
        groups={groups}
        key={groupsKey}
        onChanged={() => queryClient.invalidateQueries({ queryKey: ["admin", "retreat-groups"] })}
        slots={slots}
      />
    </section>
  );
}

// ─── Board ────────────────────────────────────────────────────────────────────

function RetreatGroupBoard({
  groups,
  onChanged,
  slots
}: {
  groups: RetreatGroup[];
  onChanged: () => Promise<void>;
  slots: ParticipationOption[];
}) {
  const queryClient = useQueryClient();

  const nextTempGroupIdRef = useRef(-2);
  const ensureMinimumGroup = useCallback((orderedGroups: BoardGroup[]) => {
    const renumbered = renumberBoardGroups(orderedGroups);
    return renumbered.length > 0
      ? renumbered
      : [createEmptyBoardGroup(nextTempGroupIdRef.current--)];
  }, []);
  const [boardGroups, setBoardGroups] = useState<BoardGroup[]>(() => {
    const initialGroups = renumberBoardGroups([...groups]
      .filter(group => group.active)
      .sort((a, b) => a.displayOrder - b.displayOrder || a.id - b.id));
    return initialGroups.length > 0 ? initialGroups : [createEmptyBoardGroup(-1)];
  });
  const [deletedGroupIds, setDeletedGroupIds] = useState<number[]>([]);
  const [draft, setDraft] = useState<DragDropState>({});
  const [draftLeaders, setDraftLeaders] = useState<{ [groupId: number]: number | null }>({});
  const [memberOrder, setMemberOrder] = useState<MemberOrderState>({});
  const [hasChanges, setHasChanges] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [expandedGroupId, setExpandedGroupId] = useState<number | null>(null);

  const containerRef = useRef<HTMLDivElement>(null);
  const [scrollOffset, setScrollOffset] = useState(0);
  const [containerWidth, setContainerWidth] = useState(Infinity);

  // filters
  const [keyword, setKeyword] = useState("");
  const [fGender, setFGender] = useState<"" | "MALE" | "FEMALE">("");
  const [fAttendance, setFAttendance] = useState<"" | "FULL" | "PARTIAL" | "WORSHIP_ONLY">("");
  const [fNewcomer, setFNewcomer] = useState<"" | "true" | "false">("");

  const registrationsQuery = useQuery({
    queryKey: ["admin", "registrations", "for-board"],
    queryFn: () => getAdminRegistrations({ size: 500 })
  });
  const groupTreeQuery = useQuery({
    queryKey: ["admin", "retreat-groups", "tree"],
    queryFn: getRetreatGroupTree
  });
  const allRegs = useMemo(() => registrationsQuery.data?.content ?? [], [registrationsQuery.data]);
  const activeRegs = useMemo(() => allRegs.filter(r => r.status === "REGISTERED"), [allRegs]);

  const activeGroups = useMemo(() =>
    [...groups]
      .filter(g => g.active)
      .sort((a, b) => a.displayOrder - b.displayOrder || a.id - b.id),
    [groups]
  );

  const persistedMemberOrder = useMemo<MemberOrderState>(() =>
    Object.fromEntries((groupTreeQuery.data?.groups ?? []).map(group => [
      group.id,
      group.members.map(member => member.participantId)
    ])),
    [groupTreeQuery.data]);

  const sortedGroups = boardGroups;

  const addGroupMutation = useMutation({
    mutationFn: async () => {
      setBoardGroups(prev => ensureMinimumGroup([
        ...prev,
        {
          id: nextTempGroupIdRef.current--,
          name: "",
          description: "",
          displayOrder: prev.length + 1,
          active: true,
          createdAt: "",
          updatedAt: "",
          isNew: true
        }
      ]));
      setHasChanges(true);
    },
  });

  const deactivateGroupMutation = useMutation({
    mutationFn: async (group: RetreatGroup) => {
      const targetIndex = sortedGroups.findIndex(g => g.id === group.id);
      if (targetIndex < 0) return;

      if (group.id > 0) {
        setDeletedGroupIds(prev => prev.includes(group.id) ? prev : [...prev, group.id]);
      }
      setBoardGroups(prev => ensureMinimumGroup(prev.filter(g => g.id !== group.id)));
      setDraft(prev => {
        const next = { ...prev };
        activeRegs.forEach(reg => {
          const assignedGroupId = next[reg.id] !== undefined ? next[reg.id] : reg.retreatGroupId;
          if (assignedGroupId === group.id) {
            next[reg.id] = null;
          }
        });
        return next;
      });
      setDraftLeaders(prev => {
        const next = { ...prev };
        delete next[group.id];
        return next;
      });
      setHasChanges(true);
    }
  });

  const reorderGroupMutation = useMutation({
    mutationFn: async ({ draggedId, targetId }: { draggedId: number; targetId: number }) => {
      const draggedIndex = sortedGroups.findIndex(g => g.id === draggedId);
      const targetIndex = sortedGroups.findIndex(g => g.id === targetId);
      if (draggedIndex < 0 || targetIndex < 0 || draggedIndex === targetIndex) return;

      const nextGroups = [...sortedGroups];
      const [draggedGroup] = nextGroups.splice(draggedIndex, 1);
      nextGroups.splice(targetIndex, 0, draggedGroup);
      setBoardGroups(ensureMinimumGroup(nextGroups));
      setHasChanges(true);
    },
  });

  const getAssignedGroupId = useCallback((pid: number): number | null => {
    if (draft[pid] !== undefined) return draft[pid];
    return activeRegs.find(r => r.id === pid)?.retreatGroupId ?? null;
  }, [draft, activeRegs]);

  const getOrderedParticipantIds = useCallback((groupId: number, orderState = memberOrder) => {
    const assignedIds = activeRegs
      .filter(reg => getAssignedGroupId(reg.id) === groupId)
      .map(reg => reg.id);
    const assignedIdSet = new Set(assignedIds);
    const preferredIds = orderState[groupId] ?? persistedMemberOrder[groupId] ?? [];
    return [
      ...preferredIds.filter(id => assignedIdSet.delete(id)),
      ...assignedIds.filter(id => assignedIdSet.has(id))
    ];
  }, [activeRegs, getAssignedGroupId, memberOrder, persistedMemberOrder]);

  // 특정 조의 현재 조장 participantId (draft 반영)
  const getGroupLeaderId = useCallback((groupId: number): number | null => {
    if (draftLeaders[groupId] !== undefined) return draftLeaders[groupId];
    return activeRegs.find(r => {
      return draft[r.id] === undefined && r.retreatGroupId === groupId && r.retreatGroupLeader;
    })?.id ?? null;
  }, [draftLeaders, activeRegs, draft]);

  const assignedCount = useMemo(
    () => activeRegs.filter(r => getAssignedGroupId(r.id) !== null).length,
    [activeRegs, getAssignedGroupId]
  );

  const filteredRegs = useMemo(() => {
    return activeRegs.filter(reg => {
      if (getAssignedGroupId(reg.id) !== null) return false;
      if (keyword.trim()) {
        const k = keyword.toLowerCase();
        if (!reg.name.toLowerCase().includes(k) &&
          !reg.phoneNumber.slice(-4).includes(k) &&
          !(reg.cellName ?? "").toLowerCase().includes(k)) return false;
      }
      if (fGender && reg.gender !== fGender) return false;
      if (fAttendance && reg.attendanceType !== fAttendance) return false;
      if (fNewcomer === "true" && !reg.newcomer) return false;
      if (fNewcomer === "false" && reg.newcomer) return false;
      return true;
    });
  }, [activeRegs, keyword, fGender, fAttendance, fNewcomer, getAssignedGroupId]);

  const saveMutation = useMutation({
    mutationFn: async () => {
      const runId = Date.now();
      const groupIdMap = new Map<number, number>();
      const keptGroups = sortedGroups.filter(group => group.id > 0);
      const newGroups = sortedGroups.filter(group => group.id < 0);

      for (const group of keptGroups) {
        groupIdMap.set(group.id, group.id);
        await updateRetreatGroup(group.id, {
          name: `임시-${group.id}-${runId}`,
          description: group.description ?? "",
          displayOrder: group.displayOrder
        });
      }

      for (const group of newGroups) {
        const created = await createRetreatGroup({
          name: `임시-new-${Math.abs(group.id)}-${runId}`,
          description: group.description ?? "",
          displayOrder: group.displayOrder
        });
        groupIdMap.set(group.id, created.id);
      }

      // 1) 조 배정 저장
      for (const [pidStr, gid] of Object.entries(draft)) {
        const pid = Number(pidStr);
        const reg = activeRegs.find(r => r.id === pid);
        if (!reg) continue;
        if (gid === null) {
          if (reg.retreatGroupId) await removeParticipantFromRetreatGroup(pid);
        } else {
          const realGroupId = groupIdMap.get(gid);
          if (realGroupId !== undefined && reg.retreatGroupId !== realGroupId) {
            await assignParticipantToRetreatGroup(pid, realGroupId);
          }
        }
      }

      for (const groupId of deletedGroupIds) {
        await deleteRetreatGroup(groupId);
      }

      for (let i = 0; i < sortedGroups.length; i += 1) {
        const group = sortedGroups[i];
        const realGroupId = groupIdMap.get(group.id);
        if (realGroupId === undefined) continue;
        const displayOrder = i + 1;
        await updateRetreatGroup(realGroupId, {
          name: `${displayOrder}조`,
          description: group.description ?? "",
          displayOrder
        });
      }

      // 2) 조장 저장
      for (const [groupIdStr, leaderId] of Object.entries(draftLeaders)) {
        const groupId = Number(groupIdStr);
        const realGroupId = groupIdMap.get(groupId);
        if (realGroupId === undefined) continue;
        if (leaderId === null) {
          await removeRetreatGroupLeader(realGroupId);
        } else {
          await assignRetreatGroupLeader(realGroupId, leaderId);
        }
      }

      // 3) 조원 표시 순서 저장
      for (const group of sortedGroups) {
        const realGroupId = groupIdMap.get(group.id);
        if (realGroupId === undefined) continue;
        await updateRetreatGroupMemberOrder(realGroupId, getOrderedParticipantIds(group.id));
      }
    },
    onSuccess: async () => {
      setDeletedGroupIds([]);
      setDraft({});
      setDraftLeaders({});
      setShowModal(false);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["admin", "registrations"] }),
        queryClient.invalidateQueries({ queryKey: ["admin", "retreat-groups", "tree"] }),
        onChanged()
      ]);
      setHasChanges(false);
    }
  });

  const handleDrop = useCallback((targetGid: number | null, pid: number) => {
    setDraft(prev => ({ ...prev, [pid]: targetGid }));
    setMemberOrder(prev => {
      const next = { ...prev };
      sortedGroups.forEach(group => {
        next[group.id] = getOrderedParticipantIds(group.id, prev).filter(id => id !== pid);
      });
      if (targetGid !== null) next[targetGid] = [...(next[targetGid] ?? []), pid];
      return next;
    });
    setHasChanges(true);
  }, [getOrderedParticipantIds, sortedGroups]);

  const handleMemberDrop = useCallback((targetGid: number, pid: number, targetPid: number) => {
    if (pid === targetPid) return;
    setDraft(prev => ({ ...prev, [pid]: targetGid }));
    setMemberOrder(prev => {
      const next = { ...prev };
      sortedGroups.forEach(group => {
        next[group.id] = getOrderedParticipantIds(group.id, prev).filter(id => id !== pid);
      });
      const targetOrder = next[targetGid] ?? [];
      const targetIndex = targetOrder.indexOf(targetPid);
      targetOrder.splice(targetIndex < 0 ? targetOrder.length : targetIndex, 0, pid);
      next[targetGid] = targetOrder;
      return next;
    });
    setHasChanges(true);
  }, [getOrderedParticipantIds, sortedGroups]);

  const handleToggleLeader = useCallback((groupId: number, pid: number) => {
    setDraftLeaders(prev => {
      const curLeaderId = prev[groupId] !== undefined
        ? prev[groupId]
        : (activeRegs.find(r => {
          const gid = draft[r.id] !== undefined ? draft[r.id] : r.retreatGroupId;
          return gid === groupId && r.retreatGroupLeader;
        })?.id ?? null);
      return { ...prev, [groupId]: curLeaderId === pid ? null : pid };
    });
    setMemberOrder(prev => ({
      ...prev,
      [groupId]: [pid, ...getOrderedParticipantIds(groupId, prev).filter(id => id !== pid)]
    }));
    setHasChanges(true);
  }, [activeRegs, draft, getOrderedParticipantIds]);

  const CARD_WIDTH = 310;
  const CARD_GAP = 14;
  const ADD_WIDTH = 96;
  const CARD_W = CARD_WIDTH + CARD_GAP;
  const totalInnerWidth = sortedGroups.length * CARD_W + ADD_WIDTH;
  const maxOffset = Math.max(0, totalInnerWidth - containerWidth);
  const visibleScrollOffset = Math.min(scrollOffset, maxOffset);
  const canScrollLeft = visibleScrollOffset > 0;
  const canScrollRight = visibleScrollOffset < maxOffset;

  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;
    setContainerWidth(el.clientWidth);
    const ro = new ResizeObserver(([e]) => setContainerWidth(e.contentRect.width));
    ro.observe(el);
    return () => ro.disconnect();
  }, [registrationsQuery.isLoading]);

  const scrollGroups = useCallback((dir: "left" | "right") => {
    const step = containerWidth * 0.85 || 900;
    setScrollOffset(prev =>
      Math.max(0, Math.min(maxOffset, Math.min(prev, maxOffset) + (dir === "left" ? -step : step)))
    );
  }, [containerWidth, maxOffset]);

  const handleCancel = () => {
    setBoardGroups(ensureMinimumGroup(activeGroups));
    setDeletedGroupIds([]);
    setDraft({});
    setDraftLeaders({});
    setMemberOrder(persistedMemberOrder);
    setHasChanges(false);
  };

  const handleDeactivateAllGroups = () => {
    setBoardGroups(ensureMinimumGroup([]));
    setDeletedGroupIds(activeGroups.map(g => g.id));
    setDraft(prev => {
      const next = { ...prev };
      activeRegs.forEach(reg => {
        const assignedGroupId = next[reg.id] !== undefined ? next[reg.id] : reg.retreatGroupId;
        if (assignedGroupId !== null) {
          next[reg.id] = null;
        }
      });
      return next;
    });
    setHasChanges(true);
  };

  const handleReset = () => {
    if (window.confirm("저장되지 않은 변경사항을 모두 취소하시겠습니까?")) handleCancel();
  };

  const handleTruncate = () => {
    if (window.confirm("모든 조 편성을 초기화하고 참가자들의 조 배정을 해제하시겠습니까? 이 작업은 되돌릴 수 없습니다.")) handleDeactivateAllGroups();
  };

  if (registrationsQuery.isLoading) {
    return <EmptyState title="참가자 정보를 불러오는 중입니다" message="잠시만 기다려 주세요." />;
  }

  return (
    <>
      {/* ── Page heading ─────────────────────────────────────── */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: "16px", flex: "1 1 0%" }}>
        <div>
          <div className="page-heading-title-row">
            <h1>수련회 조 편성</h1>
            <span className="masking-badge">🔒 CHAIR 이상 변경 가능</span>
          </div>
          <p className="muted" style={{ marginTop: "4px", fontSize: "0.88rem" }}>
            아래 후보 목록의 참가자를 드래그하여 각 조에 배정해주세요.
          </p>
        </div>
        {/* ── Unsaved changes banner ────────────────────────────── */}
        {hasChanges && (
          <div style={{
            display: "flex", justifyContent: "space-between", alignItems: "center", gap: "12px",
            background: "#fff3cd", border: "1px solid #ffc107", borderRadius: "8px",
            padding: "10px 16px", fontSize: "0.88rem", fontWeight: 600, color: "#856404"
          }}>
            <span>저장되지 않은 변경사항이 있어요.</span>
            <button className="button button--ghost button--sm" onClick={handleCancel}>변경 취소</button>
          </div>
        )}
        <div style={{ display: "flex", flexDirection: "column", alignItems: "flex-end", gap: "8px", flexShrink: 0, width: "340px" }}>
          <div style={{ fontSize: "0.82rem", color: "var(--color-muted)", fontWeight: 600, whiteSpace: "nowrap" }}>
            전체 {activeRegs.length}명 &nbsp;·&nbsp; 배정 {assignedCount}명 &nbsp;·&nbsp; 미배정 {activeRegs.length - assignedCount}명
          </div>
          <div style={{ display: "flex", gap: "8px" }}>
            <button className="button button--outline button--sm" onClick={handleReset}>변경 취소</button>
            <button
              className="button button--primary button--sm"
              disabled={!hasChanges || saveMutation.isPending}
              onClick={() => setShowModal(true)}
            >
              {saveMutation.isPending ? "저장 중..." : "🗂 편성 저장"}
            </button>
          </div>
        </div>
      </div>



      {/* ── Search + Filter card ──────────────────────────────── */}
      <div className="search-card">
        <div className="search-card-top">
          <div className="search-input-wrap" style={{ flex: 1, maxWidth: "none" }}>
            <svg className="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
            </svg>
            <input
              type="search"
              placeholder="이름, 연락처 끝 4자리, 셀 검색"
              value={keyword}
              onChange={e => setKeyword(e.target.value)}
            />
          </div>
        </div>
        <div className="search-card-divider" />
        <div style={{ display: "flex", flexWrap: "wrap", gap: "12px 16px", alignItems: "center" }}>
          {/* 참석유형 */}
          <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
            <span className="filter-label">참석유형</span>
            {([["FULL", "전체참석"], ["PARTIAL", "부분참석"], ["WORSHIP_ONLY", "집회만"]] as const).map(([val, lbl]) => (
              <label key={val} style={{ display: "flex", alignItems: "center", gap: "4px", cursor: "pointer", fontSize: "0.82rem", fontWeight: fAttendance === val ? 700 : 400, color: fAttendance === val ? "var(--color-primary-dark)" : "var(--color-muted)", userSelect: "none" }}>
                <input
                  type="radio"
                  name="fAttendance"
                  value={val}
                  checked={fAttendance === val}
                  onChange={() => setFAttendance(fAttendance === val ? "" : val)}
                  onClick={() => { if (fAttendance === val) setFAttendance(""); }}
                  style={{ accentColor: "var(--color-primary)", cursor: "pointer" }}
                />
                {lbl}
              </label>
            ))}
          </div>

          <div style={{ width: "1px", height: "18px", background: "var(--color-border)" }} />

          {/* 성별 */}
          <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
            <span className="filter-label">성별</span>
            {([["MALE", "남"], ["FEMALE", "여"]] as const).map(([val, lbl]) => (
              <label key={val} style={{ display: "flex", alignItems: "center", gap: "4px", cursor: "pointer", fontSize: "0.82rem", fontWeight: fGender === val ? 700 : 400, color: fGender === val ? "var(--color-primary-dark)" : "var(--color-muted)", userSelect: "none" }}>
                <input
                  type="radio"
                  name="fGender"
                  value={val}
                  checked={fGender === val}
                  onChange={() => setFGender(fGender === val ? "" : val)}
                  onClick={() => { if (fGender === val) setFGender(""); }}
                  style={{ accentColor: "var(--color-primary)", cursor: "pointer" }}
                />
                {lbl}
              </label>
            ))}
          </div>

          <div style={{ width: "1px", height: "18px", background: "var(--color-border)" }} />

          {/* 새가족 */}
          <label style={{ display: "flex", alignItems: "center", gap: "4px", cursor: "pointer", fontSize: "0.82rem", fontWeight: fNewcomer === "true" ? 700 : 400, color: fNewcomer === "true" ? "var(--color-primary-dark)" : "var(--color-muted)", userSelect: "none" }}>
            <input
              type="checkbox"
              checked={fNewcomer === "true"}
              onChange={e => setFNewcomer(e.target.checked ? "true" : "")}
              style={{ accentColor: "var(--color-primary)", cursor: "pointer" }}
            />
            새가족만
          </label>

          <button
            className="button button--ghost button--sm"
            onClick={() => { setKeyword(""); setFGender(""); setFAttendance(""); setFNewcomer(""); }}
          >
            초기화
          </button>
        </div>
      </div>

      {/* ── Group boards ─────────────────────────────────────── */}
      <div style={{ position: "relative", maxWidth: "100%", overflow: "hidden", padding: "0 54px" }}>
        {canScrollLeft && (
          <button onClick={() => scrollGroups("left")} style={{
            position: "absolute", left: "4px", top: "50%", transform: "translateY(-50%)", zIndex: 10,
            width: "44px", height: "72px", borderRadius: "12px", background: "var(--color-surface)",
            border: "1px solid var(--color-border)", boxShadow: "0 8px 22px rgba(15,23,42,0.16)",
            cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center",
            fontSize: "2rem", color: "var(--color-primary-dark)", fontWeight: 800, lineHeight: 1
          }}>‹</button>
        )}
        {canScrollRight && (
          <button onClick={() => scrollGroups("right")} style={{
            position: "absolute", right: "4px", top: "50%", transform: "translateY(-50%)", zIndex: 10,
            width: "44px", height: "72px", borderRadius: "12px", background: "var(--color-surface)",
            border: "1px solid var(--color-border)", boxShadow: "0 8px 22px rgba(15,23,42,0.16)",
            cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center",
            fontSize: "2rem", color: "var(--color-primary-dark)", fontWeight: 800, lineHeight: 1
          }}>›</button>
        )}

        {/* overflow:hidden으로 페이지 가로 확장 방지, transform으로 슬라이드 */}
        <div ref={containerRef} style={{ overflow: "hidden" }}>
          <div style={{
            display: "flex", gap: `${CARD_GAP}px`,
            transform: `translateX(-${visibleScrollOffset}px)`,
            transition: "transform 0.3s ease",
            willChange: "transform"
          }}>
            {sortedGroups.map(group => (
              <GroupBoard
                key={group.id}
                group={group}
                activeRegs={activeRegs}
                draft={draft}
                width={CARD_WIDTH}
                leaderId={getGroupLeaderId(group.id)}
                slots={slots}
                orderedParticipantIds={getOrderedParticipantIds(group.id)}
                onDrop={pid => handleDrop(group.id, pid)}
                onMemberDrop={(pid, targetPid) => handleMemberDrop(group.id, pid, targetPid)}
                onGroupDrop={draggedId => reorderGroupMutation.mutate({ draggedId, targetId: group.id })}
                onToggleLeader={pid => handleToggleLeader(group.id, pid)}
                onExpand={() => setExpandedGroupId(group.id)}
                onDeactivate={() => {
                  if (window.confirm(`"${group.name}"을 제거하시겠습니까? 배정된 조원은 미배정 상태가 됩니다.`))
                    deactivateGroupMutation.mutate(group);
                }}
              />
            ))}

            {/* 조 추가 */}
            <button
              onClick={() => addGroupMutation.mutate()}
              disabled={addGroupMutation.isPending}
              style={{
                flexShrink: 0, width: `${ADD_WIDTH}px`, background: "transparent",
                border: "2px dashed var(--color-border)", borderRadius: "12px",
                cursor: addGroupMutation.isPending ? "default" : "pointer",
                color: "var(--color-muted)", fontSize: "1.6rem",
                fontWeight: 300, transition: "border-color 0.15s, color 0.15s",
                display: "flex", flexDirection: "column", alignItems: "center",
                justifyContent: "center", gap: "4px", minHeight: "120px"
              }}
              onMouseEnter={e => {
                if (addGroupMutation.isPending) return;
                (e.currentTarget as HTMLButtonElement).style.borderColor = "var(--color-primary)";
                (e.currentTarget as HTMLButtonElement).style.color = "var(--color-primary)";
              }}
              onMouseLeave={e => {
                (e.currentTarget as HTMLButtonElement).style.borderColor = "var(--color-border)";
                (e.currentTarget as HTMLButtonElement).style.color = "var(--color-muted)";
              }}
              title="조 추가"
            >
              <span>{addGroupMutation.isPending ? "…" : "+"}</span>
            </button>
          </div>  {/* flex inner */}
        </div>  {/* overflow:hidden container */}
      </div>  {/* position:relative wrapper */}

      {/* ── Candidate list ───────────────────────────────────── */}
      <CandidateSection
        registrations={filteredRegs}
        totalCount={activeRegs.length}
        sortedGroups={sortedGroups}
        getAssignedGroupId={getAssignedGroupId}
        slots={slots}
        onUnassign={pid => handleDrop(null, pid)}
      />

      {/* ── Legend ───────────────────────────────────────────── */}
      <div style={{ display: "flex", gap: "18px", alignItems: "center", justifyContent: "space-between", padding: "4px 0", fontSize: "0.78rem", fontWeight: 600, color: "var(--color-muted)" }}>
        <div style={{ display: "flex", gap: "12px", alignItems: "center" }}>
          <LegendItem color={COLOR_FULL} label="전체참석 구간" />
          <LegendItem color={COLOR_PARTIAL} label="부분참석 구간" />
          <LegendItem color={COLOR_EMPTY} label="미참석 구간" />
          <span style={{ borderLeft: "1px solid var(--color-border)", paddingLeft: "18px" }}>
            ♥ = 조장 &nbsp;·&nbsp; 각 조 타임라인은 1일차 오전 → 오후 → 집회 → 2일차 오전 → … → 3일차 오후 순
          </span>
        </div>
        <button className="button button--outline button--sm" onClick={handleTruncate} style={{ color: "#dc2626", borderColor: "#dc2626" }}>전체 초기화</button>
      </div>

      {/* ── Save modal ───────────────────────────────────────── */}
      {showModal && (
        <SaveModal
          activeRegs={activeRegs}
          sortedGroups={sortedGroups}
          getAssignedGroupId={getAssignedGroupId}
          onConfirm={() => saveMutation.mutate()}
          onCancel={() => setShowModal(false)}
          isPending={saveMutation.isPending}
        />
      )}

      {saveMutation.isError && <StatusMessage message={saveMutation.error.message} tone="error" />}

      {/* ── 조 확대 팝업 ─────────────────────────────────────── */}
      {expandedGroupId !== null && (() => {
        const group = sortedGroups.find(g => g.id === expandedGroupId);
        if (!group) return null;
        const memberById = new Map(activeRegs.map(reg => [reg.id, reg] as const));
        const members = getOrderedParticipantIds(group.id).flatMap(id => {
          const reg = memberById.get(id);
          return reg ? [reg] : [];
        }).filter(reg => {
          const gid = draft[reg.id] !== undefined ? draft[reg.id] : reg.retreatGroupId;
          return gid === group.id;
        });
        return (
          <GroupDetailModal
            group={group}
            members={members}
            leaderId={getGroupLeaderId(group.id)}
            onClose={() => setExpandedGroupId(null)}
            slots={slots}
          />
        );
      })()}
    </>
  );
}

// ─── Group Board ──────────────────────────────────────────────────────────────

const NAME_COL_W = 108;

function GroupBoard({
  group, activeRegs, draft, width, leaderId, slots, orderedParticipantIds, onDrop, onMemberDrop,
  onGroupDrop, onToggleLeader, onExpand, onDeactivate
}: {
  group: RetreatGroup;
  activeRegs: AdminRegistration[];
  draft: DragDropState;
  width: number;
  leaderId: number | null;
  slots: ParticipationOption[];
  orderedParticipantIds: number[];
  onDrop: (pid: number) => void;
  onMemberDrop: (pid: number, targetPid: number) => void;
  onGroupDrop: (draggedGroupId: number) => void;
  onToggleLeader: (pid: number) => void;
  onExpand: () => void;
  onDeactivate: () => void;
}) {
  const [dragOver, setDragOver] = useState(false);

  const members = useMemo(() => {
    const memberById = new Map(activeRegs.filter(reg => {
      const gid = draft[reg.id] !== undefined ? draft[reg.id] : reg.retreatGroupId;
      return gid === group.id;
    }).map(reg => [reg.id, reg] as const));
    return orderedParticipantIds.flatMap(id => {
      const member = memberById.get(id);
      return member ? [member] : [];
    });
  }, [activeRegs, draft, group.id, orderedParticipantIds]);

  const maleCount = members.filter(m => m.gender === "MALE").length;
  const femaleCount = members.filter(m => m.gender === "FEMALE").length;
  const fullCount = members.filter(m => m.attendanceType === "FULL").length;
  const partialCount = members.length - fullCount;

  const slotCounts = slots.map(({ id }) =>
    members.filter(reg => getAttendanceSlots(reg).includes(id)).length
  );
  const dayGroups = [...new Set(slots.map((slot) => slot.eventDate))].map((eventDate) => ({
    label: eventDate.slice(5).replace("-", "/"),
    cols: slots.filter((slot) => slot.eventDate === eventDate).length
  }));
  const slotCount = Math.max(slots.length, 1);

  return (
    <div
      onDragOver={e => { e.preventDefault(); setDragOver(true); }}
      onDragLeave={e => { if (!e.currentTarget.contains(e.relatedTarget as Node)) setDragOver(false); }}
      onDragEnd={() => setDragOver(false)}
      onDrop={e => {
        e.preventDefault();
        setDragOver(false);
        const pid = parseInt(e.dataTransfer.getData("participantId"));
        const gid = parseInt(e.dataTransfer.getData("groupId"));
        if (!isNaN(pid)) onDrop(pid);
        else if (!isNaN(gid) && gid !== group.id) onGroupDrop(gid);
      }}
      style={{
        flexShrink: 0, width: `${width}px`,
        background: dragOver ? "#edf7f5" : "var(--color-surface)",
        border: `2px solid ${dragOver ? "var(--color-primary)" : "var(--color-border)"}`,
        borderRadius: "12px", overflow: "hidden",
        transition: "border-color 0.15s, background 0.15s",
        height: "fit-content"
      }}
    >
      {/* ── 조 헤더 */}
      <div style={{ padding: "10px 14px 8px" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "4px", gap: "6px" }}>
          {/* 드래그 핸들 */}
          <div
            draggable
            onDragStart={e => {
              e.stopPropagation();
              e.dataTransfer.effectAllowed = "move";
              e.dataTransfer.setData("groupId", String(group.id));
            }}
            title="드래그하여 순서 변경"
            style={{ cursor: "grab", color: "var(--color-muted)", fontSize: "14px", lineHeight: 1, flexShrink: 0, padding: "2px 4px", userSelect: "none" }}
          >⠿</div>

          {/* 조 이름 (클릭 시 확대) */}
          <h2
            onClick={onExpand}
            style={{ fontSize: "1.15rem", fontWeight: 800, margin: 0, cursor: "pointer", flex: 1, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}
            title={`${group.name} 확대보기`}
          >{group.name}</h2>

          {/* 확대 버튼 */}
          <button onClick={onExpand} title="확대보기" style={{
            background: "none", border: "none", cursor: "pointer", flexShrink: 0,
            color: "var(--color-muted)", padding: "2px 4px", borderRadius: "4px",
            display: "flex", alignItems: "center"
          }}>
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
              <path d="M15 3h6v6M9 21H3v-6M21 3l-7 7M3 21l7-7" />
            </svg>
          </button>

          {/* 조 삭제 */}
          <button onClick={e => { e.stopPropagation(); onDeactivate(); }} title="조 삭제" style={{
            background: "none", border: "1px solid #fca5a5", cursor: "pointer", flexShrink: 0,
            color: "#dc2626", fontSize: "0.68rem", fontWeight: 700,
            padding: "2px 6px", borderRadius: "6px", lineHeight: 1,
            display: "flex", alignItems: "center", gap: "3px",
            transition: "background 0.15s"
          }}
            onMouseEnter={e => (e.currentTarget.style.background = "#fef2f2")}
            onMouseLeave={e => (e.currentTarget.style.background = "none")}
          >
            <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="3 6 5 6 21 6" /><path d="M19 6l-1 14H6L5 6" /><path d="M10 11v6M14 11v6" /><path d="M9 6V4h6v2" />
            </svg>
            삭제
          </button>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: "6px", flexWrap: "wrap" }}>
          <span style={{ fontSize: "0.8rem", color: "var(--color-muted)", fontWeight: 600 }}>
            {members.length}명 · 남 {maleCount} · 여 {femaleCount}
          </span>
          <span className="status-pill status-pill--success" style={{ fontSize: "0.7rem", padding: "2px 8px" }}>전체 {fullCount}</span>
          <span className="status-pill status-pill--warning" style={{ fontSize: "0.7rem", padding: "2px 8px" }}>부분 {partialCount}</span>
        </div>
      </div>

      {/* ── 타임라인 영역 */}
      <div style={{ padding: "0 14px" }}>
        {/* 헤더 row 1: 일차 그룹 */}
        <div style={{ display: "grid", gridTemplateColumns: `${NAME_COL_W}px 1fr`, marginBottom: "1px" }}>
          <div />
          <div style={{ display: "grid", gridTemplateColumns: dayGroups.map((day) => `${day.cols}fr`).join(" "), borderBottom: "1px solid var(--color-border)" }}>
            {dayGroups.map((d, i) => (
              <div key={i} style={{
                textAlign: "center", fontSize: "8px", fontWeight: 800,
                color: "var(--color-primary-dark)", paddingBottom: "2px",
                borderRight: i < dayGroups.length - 1 ? "1px solid var(--color-border)" : "none"
              }}>
                {d.label}
              </div>
            ))}
          </div>
        </div>
        {/* 헤더 row 2: 슬롯 */}
        <div style={{ display: "grid", gridTemplateColumns: `${NAME_COL_W}px 1fr`, marginBottom: "6px" }}>
          <div />
          <div style={{ display: "grid", gridTemplateColumns: `repeat(${slotCount}, 1fr)` }}>
            {slots.map((slot, i) => (
              <div key={i} style={{
                textAlign: "center", fontSize: "8px", fontWeight: 600,
                color: "var(--color-muted)", paddingTop: "2px",
                borderRight: i < slots.length - 1 ? "1px solid color-mix(in srgb, var(--color-border) 60%, transparent)" : "none"
              }}>
                {slot.label}
              </div>
            ))}
          </div>
        </div>

        {/* 조원 행 */}
        <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
          {members.map(member => (
            <BoardMemberRow
              key={member.id}
              participant={member}
              slots={slots}
              isLeader={leaderId === member.id}
              onToggleLeader={() => onToggleLeader(member.id)}
              onDropMember={pid => {
                setDragOver(false);
                onMemberDrop(pid, member.id);
              }}
            />
          ))}
        </div>
      </div>

      {/* ── 드롭존 */}
      <div style={{
        margin: "10px 14px", padding: "9px",
        border: `1.5px dashed ${dragOver ? "var(--color-primary)" : "var(--color-border)"}`,
        borderRadius: "8px", textAlign: "center", fontSize: "0.78rem",
        color: dragOver ? "var(--color-primary)" : "var(--color-muted)",
        fontWeight: 600, background: dragOver ? "rgb(23 107 91 / 0.04)" : "transparent",
        transition: "all 0.15s", userSelect: "none"
      }}>
        + 여기로 드래그하여 추가
      </div>

      {/* ── 시간대별 인원 footer */}
      <div style={{ padding: "8px 14px", borderTop: "1px solid var(--color-border)", background: "var(--color-background)" }}>
        <div style={{ display: "grid", gridTemplateColumns: `${NAME_COL_W}px 1fr`, marginLeft: "4px" }}>
          <span style={{ fontSize: "0.68rem", fontWeight: 700, color: "var(--color-muted)", alignSelf: "center", lineHeight: 1.3 }}>
            시간대별 인원
          </span>
          <div style={{ display: "grid", gridTemplateColumns: `repeat(${slotCount}, 1fr)` }}>
            {slotCounts.map((n, i) => (
              <div key={i} style={{
                textAlign: "center", fontSize: "0.78rem", fontWeight: 700,
                borderRight: i < slotCounts.length - 1 ? "1px solid color-mix(in srgb, var(--color-border) 60%, transparent)" : "none"
              }}>{n}</div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

// ─── Board Member Row ─────────────────────────────────────────────────────────

function BoardMemberRow({
  participant, isLeader, onToggleLeader, slots, onDropMember
}: {
  participant: AdminRegistration;
  isLeader: boolean;
  onToggleLeader: () => void;
  slots: ParticipationOption[];
  onDropMember: (pid: number) => void;
}) {
  const isFull = participant.attendanceType === "FULL";
  const color = isFull ? COLOR_FULL : COLOR_PARTIAL;
  const segments = getSegments(getAttendanceSlots(participant), slots);

  return (
    <div
      draggable
      onDragStart={e => {
        e.dataTransfer.effectAllowed = "move";
        e.dataTransfer.setData("participantId", String(participant.id));
      }}
      onDragOver={e => {
        e.preventDefault();
      }}
      onDrop={e => {
        const pid = parseInt(e.dataTransfer.getData("participantId"));
        if (Number.isNaN(pid)) return;
        e.preventDefault();
        e.stopPropagation();
        onDropMember(pid);
      }}
      style={{
        display: "grid",
        gridTemplateColumns: "93px 1fr",
        gap: "8px",
        alignItems: "center",
        cursor: "grab",
        userSelect: "none",
        padding: "3px 0",
        paddingLeft: "4px",
        borderRadius: "6px",
        transition: "background 0.1s",
      }}
    >
      {/* Name column */}
      <div style={{
        display: "flex", alignItems: "center", gap: "5px", minWidth: 0,
        background: participant.gender === "MALE" ? "rgba(59,130,246,0.1)" : "rgba(236,72,153,0.1)",
        borderRadius: "999px", padding: "2px 4px",
      }}>
        {/* 조장 토글 버튼 */}
        <button
          onClick={e => { e.stopPropagation(); onToggleLeader(); }}
          title={isLeader ? "조장 해제" : "조장으로 지정"}
          style={{
            width: "18px", height: "18px", borderRadius: "50%", flexShrink: 0,
            border: isLeader ? "1.5px solid #f50b0b" : "1.5px solid var(--color-border)",
            background: isLeader ? "#fed9c7" : "transparent",
            cursor: "pointer", display: "flex",
            justifyContent: "center", fontSize: "10px", lineHeight: 1.6,
            color: isLeader ? "#f50b0b" : "var(--color-muted)",
            transition: "all 0.15s", padding: 0
          }}
        >♥</button>
        <div style={{ minWidth: 0 }}>
          <div style={{
            fontWeight: isLeader ? 800 : 700, fontSize: "0.82rem",
            whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis",
            color: isLeader ? "#b45309" : "inherit"
          }}>
            {String(participant.birthYear).slice(2)} {participant.name}
          </div>
        </div>
      </div>

      {/* Timeline bar */}
      <TimelineBar segments={segments} color={color} slotCount={slots.length} />
    </div>
  );
}

// ─── Timeline Bar ─────────────────────────────────────────────────────────────

function TimelineBar({
  segments,
  color,
  slotCount
}: {
  segments: { start: number; end: number }[];
  color: string;
  slotCount: number;
}) {
  const safeSlotCount = Math.max(slotCount, 1);
  const gridWidth = 100 / safeSlotCount;
  return (
    <div style={{ position: "relative", height: "9px", background: COLOR_EMPTY, borderRadius: "5px", overflow: "hidden" }}>
      {segments.map((seg, i) => (
        <div
          key={i}
          style={{
            position: "absolute",
            top: 0, bottom: 0,
            left: `${(seg.start / safeSlotCount) * 100}%`,
            width: `${((seg.end - seg.start + 1) / safeSlotCount) * 100}%`,
            background: color,
            borderRadius: "5px"
          }}
        />
      ))}
      {/* 슬롯 구분 그리드 오버레이 */}
      <div style={{
        position: "absolute", inset: 0,
        backgroundImage: `repeating-linear-gradient(90deg, transparent, transparent calc(${gridWidth}% - 1px), rgba(255,255,255,0.55) calc(${gridWidth}% - 1px), rgba(255,255,255,0.55) ${gridWidth}%)`,
        borderRadius: "5px",
        pointerEvents: "none"
      }} />
    </div>
  );
}

// ─── Candidate Section ────────────────────────────────────────────────────────

function CandidateSection({
  registrations, totalCount, sortedGroups, getAssignedGroupId, slots, onUnassign
}: {
  registrations: AdminRegistration[];
  totalCount: number;
  sortedGroups: RetreatGroup[];
  getAssignedGroupId: (pid: number) => number | null;
  slots: ParticipationOption[];
  onUnassign: (pid: number) => void;
}) {
  const [dragOver, setDragOver] = useState(false);

  return (
    <div
      onDragOver={e => {
        e.preventDefault();
        setDragOver(true);
      }}
      onDragLeave={e => {
        if (!e.currentTarget.contains(e.relatedTarget as Node)) setDragOver(false);
      }}
      onDrop={e => {
        e.preventDefault();
        setDragOver(false);
        const pid = parseInt(e.dataTransfer.getData("participantId"));
        if (!Number.isNaN(pid)) onUnassign(pid);
      }}
      style={{
        border: `2px dashed ${dragOver ? "var(--color-primary)" : "transparent"}`,
        borderRadius: "12px",
        padding: "10px",
        margin: "-10px",
        background: dragOver ? "rgb(23 107 91 / 0.04)" : "transparent",
        transition: "border-color 0.15s, background 0.15s"
      }}
    >
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "12px" }}>
        <div style={{ display: "flex", alignItems: "baseline", gap: "8px" }}>
          <h2 style={{ fontSize: "1.1rem" }}>참가자 목록</h2>
          <strong style={{ color: "var(--color-primary-dark)" }}>
            {registrations.length}명<span style={{ fontWeight: 400, color: "var(--color-muted)", fontSize: "0.8rem" }}> / 전체 {totalCount}명</span>
          </strong>
          <span style={{ fontSize: "0.8rem", color: "var(--color-muted)" }}>조원을 이 영역으로 드래그하면 미배정으로 이동합니다.</span>
        </div>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(148px, 1fr))", gap: "10px" }}>
        {registrations.map(reg => (
          <CandidateCard
            key={reg.id}
            participant={reg}
            assignedGroup={sortedGroups.find(g => g.id === getAssignedGroupId(reg.id))}
            slots={slots}
          />
        ))}
      </div>
    </div>
  );
}

function CandidateCard({
  participant, assignedGroup, slots
}: {
  participant: AdminRegistration;
  assignedGroup: RetreatGroup | undefined;
  slots: ParticipationOption[];
}) {
  const isFull = participant.attendanceType === "FULL";
  const color = isFull ? COLOR_FULL : COLOR_PARTIAL;
  const segments = getSegments(getAttendanceSlots(participant), slots);

  return (
    <div
      draggable
      onDragStart={e => {
        e.dataTransfer.effectAllowed = "move";
        e.dataTransfer.setData("participantId", String(participant.id));
      }}
      style={{
        background: "var(--color-surface)",
        border: "1px solid var(--color-border)",
        borderRadius: "8px",
        padding: "10px",
        cursor: "grab",
        userSelect: "none",
        transition: "box-shadow 0.1s",
        display: "flex",
        flexDirection: "column",
        gap: "9px"
      }}
    >
      {/* Name + attendance type badge */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: "4px" }}>
        <span style={{
          fontWeight: 700, fontSize: "0.82rem", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap", background: participant.gender === "MALE" ? "rgba(59,130,246,0.1)" : "rgba(236,72,153,0.1)",
          borderRadius: "999px", padding: "2px 9px",
        }}>
          {String(participant.birthYear).slice(2)} {participant.name}
        </span>
        <span style={{
          fontSize: "0.68rem", fontWeight: 700, flexShrink: 0,
          color: isFull ? "var(--color-primary-dark)" : "var(--color-warning)",
          background: isFull ? "#e8f3ea" : "#fff4df",
          borderRadius: "999px", padding: "1px 7px"
        }}>
          {isFull ? "전체" : "부분"}
        </span>
      </div>

      {/* Timeline bar */}
      <TimelineBar segments={segments} color={color} slotCount={slots.length} />

      {/* Assignment status */}
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", fontSize: "0.7rem", fontWeight: 600, color: assignedGroup ? "var(--color-primary-dark)" : "var(--color-muted)" }}>
        <span>
          {participant.middleGroupName ?? "-"} · {participant.cellName ?? "-"}
        </span>
        {participant.newcomer && (
          <span style={{ marginLeft: "4px", color: "var(--color-primary-dark)", fontWeight: 700 }}>새가족</span>
        )}
      </div>
    </div>
  );
}

// ─── Save Modal ───────────────────────────────────────────────────────────────

function SaveModal({
  activeRegs, sortedGroups, getAssignedGroupId, onConfirm, onCancel, isPending
}: {
  activeRegs: AdminRegistration[];
  sortedGroups: RetreatGroup[];
  getAssignedGroupId: (pid: number) => number | null;
  onConfirm: () => void;
  onCancel: () => void;
  isPending: boolean;
}) {
  const unassignedCount = activeRegs.filter(r => getAssignedGroupId(r.id) === null).length;
  const assignedCount = activeRegs.length - unassignedCount;
  const groupCounts = sortedGroups.map(g => ({
    name: g.name,
    count: activeRegs.filter(r => getAssignedGroupId(r.id) === g.id).length
  }));

  return (
    <div style={{
      position: "fixed", inset: 0, background: "rgba(0,0,0,0.45)",
      display: "flex", alignItems: "center", justifyContent: "center", zIndex: 50
    }}>
      <div style={{
        background: "var(--color-surface)", borderRadius: "14px", padding: "28px",
        maxWidth: "380px", width: "90%", boxShadow: "0 24px 64px rgba(0,0,0,0.28)"
      }}>
        <h2 style={{ marginBottom: "20px" }}>조 편성 저장 확인</h2>

        <div style={{ display: "grid", gap: "0" }}>
          {([
            { label: "전체 인원", value: `${activeRegs.length}명`, highlight: false },
            { label: "배정 인원", value: `${assignedCount}명`, highlight: "success" as const },
            { label: "미배정 인원", value: `${unassignedCount}명`, highlight: unassignedCount > 0 ? "danger" as const : false },
          ] as const).map(row => (
            <div key={row.label} style={{
              display: "flex", justifyContent: "space-between",
              padding: "8px 0", borderBottom: "1px solid var(--color-border)", fontSize: "0.9rem"
            }}>
              <span style={{ color: "var(--color-muted)" }}>{row.label}</span>
              <strong style={{ color: row.highlight === "success" ? "var(--color-primary-dark)" : row.highlight === "danger" ? "var(--color-danger)" : undefined }}>
                {row.value}
              </strong>
            </div>
          ))}
          {groupCounts.map(({ name, count }) => (
            <div key={name} style={{
              display: "flex", justifyContent: "space-between",
              padding: "8px 0", borderBottom: "1px solid var(--color-border)", fontSize: "0.9rem"
            }}>
              <span style={{ color: "var(--color-muted)" }}>{name}</span>
              <strong>{count}명</strong>
            </div>
          ))}
        </div>

        {unassignedCount > 0 && (
          <div style={{
            background: "#fff4df", border: "1px solid #ffc107", borderRadius: "8px",
            padding: "10px 14px", marginTop: "16px", fontSize: "0.84rem",
            color: "var(--color-warning)", fontWeight: 600
          }}>
            ⚠️ 미배정 참가자가 {unassignedCount}명 있습니다. 그래도 저장하시겠습니까?
          </div>
        )}

        <div style={{ display: "flex", gap: "8px", justifyContent: "flex-end", marginTop: "20px" }}>
          <button className="button button--outline button--sm" onClick={onCancel} disabled={isPending}>취소</button>
          <button className="button button--primary button--sm" onClick={onConfirm} disabled={isPending}>
            {isPending ? "저장 중..." : "저장"}
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Legend Item ──────────────────────────────────────────────────────────────

function LegendItem({ color, label }: { color: string; label: string }) {
  return (
    <span style={{ display: "flex", alignItems: "center", gap: "6px" }}>
      <span style={{ display: "inline-block", width: "24px", height: "8px", background: color, borderRadius: "4px" }} />
      {label}
    </span>
  );
}

// ─── Group Detail Modal ───────────────────────────────────────────────────────

const MODAL_NAME_COL_W = 160;

function GroupDetailModal({
  group, members, leaderId, onClose, slots
}: {
  group: RetreatGroup;
  members: AdminRegistration[];
  leaderId: number | null;
  onClose: () => void;
  slots: ParticipationOption[];
}) {
  const maleCount = members.filter(m => m.gender === "MALE").length;
  const femaleCount = members.filter(m => m.gender === "FEMALE").length;
  const fullCount = members.filter(m => m.attendanceType === "FULL").length;
  const partialCount = members.length - fullCount;
  const slotCounts = slots.map(({ id }) =>
    members.filter(reg => getAttendanceSlots(reg).includes(id)).length
  );
  const dayGroups = [...new Set(slots.map((slot) => slot.eventDate))].map((eventDate) => ({
    label: eventDate.slice(5).replace("-", "/"),
    cols: slots.filter((slot) => slot.eventDate === eventDate).length
  }));
  const slotCount = Math.max(slots.length, 1);
  const gridWidth = 100 / slotCount;

  return (
    <div
      style={{
        position: "fixed", inset: 0, background: "rgba(0,0,0,0.5)",
        display: "flex", alignItems: "center", justifyContent: "center", zIndex: 60
      }}
      onClick={onClose}
    >
      <div
        onClick={e => e.stopPropagation()}
        style={{
          background: "var(--color-surface)", borderRadius: "16px",
          width: "min(760px, 96vw)", maxHeight: "85vh",
          display: "flex", flexDirection: "column",
          boxShadow: "0 32px 80px rgba(0,0,0,0.32)", overflow: "hidden"
        }}
      >
        {/* 헤더 */}
        <div style={{ padding: "20px 24px 12px", borderBottom: "1px solid var(--color-border)", flexShrink: 0 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
            <div>
              <h2 style={{ fontSize: "1.6rem", fontWeight: 900, margin: "0 0 6px" }}>{group.name}</h2>
              <div style={{ display: "flex", alignItems: "center", gap: "8px", flexWrap: "wrap" }}>
                <span style={{ fontSize: "0.9rem", color: "var(--color-muted)", fontWeight: 600 }}>
                  {members.length}명 &nbsp;·&nbsp; 남 {maleCount} &nbsp;·&nbsp; 여 {femaleCount}
                </span>
                <span className="status-pill status-pill--success" style={{ fontSize: "0.78rem", padding: "2px 10px" }}>전체 {fullCount}</span>
                <span className="status-pill status-pill--warning" style={{ fontSize: "0.78rem", padding: "2px 10px" }}>부분 {partialCount}</span>
              </div>
            </div>
            <button
              onClick={onClose}
              style={{ background: "none", border: "none", cursor: "pointer", fontSize: "1.4rem", color: "var(--color-muted)", lineHeight: 1, padding: "4px 8px" }}
            >✕</button>
          </div>
        </div>

        {/* 스크롤 가능한 내용 */}
        <div style={{ overflowY: "auto", flex: 1, padding: "12px 24px 20px" }}>
          {/* 타임라인 헤더 row 1 */}
          <div style={{ display: "grid", gridTemplateColumns: `${MODAL_NAME_COL_W}px 1fr`, marginBottom: "1px" }}>
            <div />
            <div style={{ display: "grid", gridTemplateColumns: dayGroups.map((day) => `${day.cols}fr`).join(" "), borderBottom: "1px solid var(--color-border)" }}>
              {dayGroups.map((d, i) => (
                <div key={i} style={{
                  textAlign: "center", fontSize: "10px", fontWeight: 800,
                  color: "var(--color-primary-dark)", paddingBottom: "2px",
                  borderRight: i < dayGroups.length - 1 ? "1px solid var(--color-border)" : "none"
                }}>{d.label}</div>
              ))}
            </div>
          </div>
          {/* 타임라인 헤더 row 2 */}
          <div style={{ display: "grid", gridTemplateColumns: `${MODAL_NAME_COL_W}px 1fr`, marginBottom: "10px" }}>
            <div />
            <div style={{ display: "grid", gridTemplateColumns: `repeat(${slotCount}, 1fr)` }}>
              {slots.map((slot, i) => (
                <div key={i} style={{
                  textAlign: "center", fontSize: "10px", fontWeight: 600,
                  color: "var(--color-muted)", paddingTop: "2px",
                  borderRight: i < slots.length - 1 ? "1px solid color-mix(in srgb, var(--color-border) 60%, transparent)" : "none"
                }}>{slot.label}</div>
              ))}
            </div>
          </div>

          {/* 조원 행 */}
          <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
            {members.length === 0 ? (
              <p style={{ color: "var(--color-muted)", fontSize: "0.88rem", textAlign: "center", padding: "24px 0" }}>
                배정된 조원이 없습니다.
              </p>
            ) : members.map(member => {
              const isLeader = leaderId === member.id;
              const isFull = member.attendanceType === "FULL";
              const color = isFull ? COLOR_FULL : COLOR_PARTIAL;
              const segments = getSegments(getAttendanceSlots(member), slots);
              return (
                <div key={member.id} style={{
                  display: "grid", gridTemplateColumns: `${MODAL_NAME_COL_W}px 1fr`,
                  gap: "12px", alignItems: "center", padding: "5px 8px", borderRadius: "8px",
                  borderLeft: isLeader ? "3px solid #f59e0b" : "3px solid transparent",
                  background: isLeader ? "rgba(245,158,11,0.07)" : "transparent"
                }}>
                  <div style={{ display: "flex", alignItems: "center", gap: "8px", minWidth: 0 }}>
                    {isLeader && (
                      <span style={{ fontSize: "13px", color: "#d97706", flexShrink: 0 }}>★</span>
                    )}
                    <div style={{ minWidth: 0 }}>
                      <div style={{
                        fontWeight: isLeader ? 800 : 700, fontSize: "0.95rem",
                        whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis",
                        color: isLeader ? "#b45309" : "inherit"
                      }}>
                        {member.name}
                      </div>
                      <div style={{ fontSize: "0.78rem", color: "var(--color-muted)", whiteSpace: "nowrap" }}>
                        {member.gender === "MALE" ? "남" : "여"} · {String(member.birthYear).slice(2)} · {member.cellName ?? "-"}
                        {member.newcomer && <span style={{ marginLeft: "4px", color: "var(--color-primary-dark)", fontWeight: 700 }}>새가족</span>}
                      </div>
                    </div>
                  </div>
                  {/* 더 큰 타임라인 바 */}
                  <div style={{ position: "relative", height: "16px", background: COLOR_EMPTY, borderRadius: "8px", overflow: "hidden" }}>
                    {segments.map((seg, i) => (
                      <div key={i} style={{
                        position: "absolute", top: 0, bottom: 0,
                        left: `${(seg.start / slotCount) * 100}%`,
                        width: `${((seg.end - seg.start + 1) / slotCount) * 100}%`,
                        background: color, borderRadius: "8px"
                      }} />
                    ))}
                    <div style={{
                      position: "absolute", inset: 0,
                      backgroundImage: `repeating-linear-gradient(90deg, transparent, transparent calc(${gridWidth}% - 1px), rgba(255,255,255,0.55) calc(${gridWidth}% - 1px), rgba(255,255,255,0.55) ${gridWidth}%)`,
                      pointerEvents: "none"
                    }} />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* 시간대별 인원 footer */}
        <div style={{ padding: "10px 24px 14px", borderTop: "1px solid var(--color-border)", background: "var(--color-background)", flexShrink: 0 }}>
          <div style={{ display: "grid", gridTemplateColumns: `${MODAL_NAME_COL_W}px 1fr` }}>
            <span style={{ fontSize: "0.75rem", fontWeight: 700, color: "var(--color-muted)", alignSelf: "center" }}>시간대별 인원</span>
            <div style={{ display: "grid", gridTemplateColumns: `repeat(${slotCount}, 1fr)` }}>
              {slotCounts.map((n, i) => (
                <div key={i} style={{
                  textAlign: "center", fontSize: "0.9rem", fontWeight: 700,
                  borderRight: i < slotCounts.length - 1 ? "1px solid color-mix(in srgb, var(--color-border) 60%, transparent)" : "none"
                }}>{n}</div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
