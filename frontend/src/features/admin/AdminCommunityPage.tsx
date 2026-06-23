import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import {
  createCell,
  createMiddleGroup,
  getCells,
  getMiddleGroups,
  updateCell,
  updateCellActive,
  updateMiddleGroup,
  updateMiddleGroupActive,
  type ChurchCell,
  type ChurchCellPayload,
  type ChurchMiddleGroup,
  type ChurchMiddleGroupPayload
} from "./adminApi";
import { EmptyState } from "../../shared/ui/EmptyState";
import { StatusMessage } from "../../shared/ui/StatusMessage";

export function AdminCommunityPage() {
  const queryClient = useQueryClient();
  const [cellFilter, setCellFilter] = useState("");

  const middleGroupsQuery = useQuery({
    queryKey: ["admin", "community", "middle-groups"],
    queryFn: getMiddleGroups
  });
  const cellsQuery = useQuery({
    queryKey: ["admin", "community", "cells", cellFilter],
    queryFn: () => getCells({ middleGroupId: cellFilter ? Number(cellFilter) : undefined })
  });

  const middleGroups = middleGroupsQuery.data ?? [];
  const cells = cellsQuery.data ?? [];

  function invalidateMiddleGroups() {
    void queryClient.invalidateQueries({ queryKey: ["admin", "community", "middle-groups"] });
  }

  function invalidateCells() {
    void queryClient.invalidateQueries({ queryKey: ["admin", "community", "cells"] });
  }

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Community</p>
          <h1>공동체 구조</h1>
        </div>
        <span className="pill">CHAIR 이상 변경 가능</span>
      </div>

      <MiddleGroupPanel
        middleGroups={middleGroups}
        onChanged={invalidateMiddleGroups}
        query={middleGroupsQuery}
      />

      <CellPanel
        cellFilter={cellFilter}
        cells={cells}
        middleGroups={middleGroups}
        onChanged={invalidateCells}
        onFilterChange={setCellFilter}
        query={cellsQuery}
      />
    </section>
  );
}

type MiddleGroupFormValues = {
  name: string;
  elderName: string;
  description: string;
  displayOrder: string;
};

function MiddleGroupPanel({
  middleGroups,
  onChanged,
  query
}: {
  middleGroups: ChurchMiddleGroup[];
  onChanged: () => void;
  query: ReturnType<typeof useQuery<ChurchMiddleGroup[]>>;
}) {
  const [editingId, setEditingId] = useState<number | null>(null);
  const { register, handleSubmit, reset } = useForm<MiddleGroupFormValues>({
    defaultValues: { name: "", elderName: "", description: "", displayOrder: "0" }
  });

  const saveMutation = useMutation({
    mutationFn: (values: MiddleGroupFormValues) => {
      const payload: ChurchMiddleGroupPayload = {
        name: values.name,
        elderName: values.elderName || undefined,
        description: values.description || undefined,
        displayOrder: Number(values.displayOrder)
      };
      return editingId ? updateMiddleGroup(editingId, payload) : createMiddleGroup(payload);
    },
    onSuccess: () => {
      onChanged();
      setEditingId(null);
      reset({ name: "", elderName: "", description: "", displayOrder: "0" });
    }
  });

  const activeMutation = useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) => updateMiddleGroupActive(id, active),
    onSuccess: onChanged
  });

  function startEdit(middleGroup: ChurchMiddleGroup) {
    setEditingId(middleGroup.id);
    reset({
      name: middleGroup.name,
      elderName: middleGroup.elderName ?? "",
      description: middleGroup.description ?? "",
      displayOrder: String(middleGroup.displayOrder)
    });
  }

  function cancelEdit() {
    setEditingId(null);
    reset({ name: "", elderName: "", description: "", displayOrder: "0" });
  }

  return (
    <section className="panel">
      <h2>중그룹</h2>

      {query.isError ? <StatusMessage message={query.error.message} tone="error" /> : null}
      {saveMutation.isError ? <StatusMessage message={saveMutation.error.message} tone="error" /> : null}
      {activeMutation.isError ? <StatusMessage message={activeMutation.error.message} tone="error" /> : null}

      <form className="form-grid" onSubmit={handleSubmit((values) => saveMutation.mutate(values))}>
        <label>
          이름
          <input {...register("name", { required: true })} placeholder="1중그룹" />
        </label>
        <label>
          장로
          <input {...register("elderName")} placeholder="홍길동 장로" />
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
            {editingId ? "수정 저장" : "중그룹 추가"}
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
              <th>장로</th>
              <th>설명</th>
              <th>순서</th>
              <th>상태</th>
              <th>처리</th>
            </tr>
          </thead>
          <tbody>
            {middleGroups.map((middleGroup) => (
              <tr key={middleGroup.id}>
                <td>
                  <strong>{middleGroup.name}</strong>
                </td>
                <td>{middleGroup.elderName ?? "-"}</td>
                <td>{middleGroup.description ?? "-"}</td>
                <td>{middleGroup.displayOrder}</td>
                <td>
                  <span
                    className={
                      middleGroup.active ? "status-pill status-pill--success" : "status-pill status-pill--neutral"
                    }
                  >
                    {middleGroup.active ? "활성" : "비활성"}
                  </span>
                </td>
                <td>
                  <div className="table-actions">
                    <button className="table-action" onClick={() => startEdit(middleGroup)} type="button">
                      수정
                    </button>
                    <button
                      className={middleGroup.active ? "table-action table-action--warning" : "table-action"}
                      disabled={activeMutation.isPending}
                      onClick={() => activeMutation.mutate({ id: middleGroup.id, active: !middleGroup.active })}
                      type="button"
                    >
                      {middleGroup.active ? "비활성화" : "활성화"}
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {query.isLoading ? <EmptyState title="중그룹 목록을 불러오는 중입니다" message="잠시만 기다려 주세요." /> : null}
        {!query.isLoading && !middleGroups.length ? (
          <EmptyState title="등록된 중그룹이 없습니다" message="위 양식으로 중그룹을 추가해 주세요." />
        ) : null}
      </div>
    </section>
  );
}

type CellFormValues = {
  middleGroupId: string;
  name: string;
  cellLeaderName: string;
  description: string;
  displayOrder: string;
};

function CellPanel({
  cellFilter,
  cells,
  middleGroups,
  onChanged,
  onFilterChange,
  query
}: {
  cellFilter: string;
  cells: ChurchCell[];
  middleGroups: ChurchMiddleGroup[];
  onChanged: () => void;
  onFilterChange: (value: string) => void;
  query: ReturnType<typeof useQuery<ChurchCell[]>>;
}) {
  const [editingId, setEditingId] = useState<number | null>(null);
  const { register, handleSubmit, reset } = useForm<CellFormValues>({
    defaultValues: { middleGroupId: "", name: "", cellLeaderName: "", description: "", displayOrder: "0" }
  });

  useEffect(() => {
    if (!editingId && middleGroups.length) {
      reset((current) => ({ ...current, middleGroupId: current.middleGroupId || String(middleGroups[0].id) }));
    }
  }, [editingId, middleGroups, reset]);

  const saveMutation = useMutation({
    mutationFn: (values: CellFormValues) => {
      const payload: ChurchCellPayload = {
        middleGroupId: Number(values.middleGroupId),
        name: values.name,
        cellLeaderName: values.cellLeaderName || undefined,
        description: values.description || undefined,
        displayOrder: Number(values.displayOrder)
      };
      return editingId ? updateCell(editingId, payload) : createCell(payload);
    },
    onSuccess: () => {
      onChanged();
      setEditingId(null);
      reset({ middleGroupId: cellFilter, name: "", cellLeaderName: "", description: "", displayOrder: "0" });
    }
  });

  const activeMutation = useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) => updateCellActive(id, active),
    onSuccess: onChanged
  });

  function startEdit(cell: ChurchCell) {
    setEditingId(cell.id);
    reset({
      middleGroupId: String(cell.middleGroupId),
      name: cell.name,
      cellLeaderName: cell.cellLeaderName ?? "",
      description: cell.description ?? "",
      displayOrder: String(cell.displayOrder)
    });
  }

  function cancelEdit() {
    setEditingId(null);
    reset({ middleGroupId: cellFilter, name: "", cellLeaderName: "", description: "", displayOrder: "0" });
  }

  return (
    <section className="panel">
      <h2>교회 셀</h2>

      {query.isError ? <StatusMessage message={query.error.message} tone="error" /> : null}
      {saveMutation.isError ? <StatusMessage message={saveMutation.error.message} tone="error" /> : null}
      {activeMutation.isError ? <StatusMessage message={activeMutation.error.message} tone="error" /> : null}

      <section className="filter-panel" aria-label="셀 목록 필터">
        <label>
          중그룹
          <select onChange={(event) => onFilterChange(event.target.value)} value={cellFilter}>
            <option value="">전체</option>
            {middleGroups.map((middleGroup) => (
              <option key={middleGroup.id} value={middleGroup.id}>
                {middleGroup.name}
              </option>
            ))}
          </select>
        </label>
        <div className="filter-summary">
          <span>검색 결과</span>
          <strong>{cells.length}</strong>
        </div>
      </section>

      <form className="form-grid" onSubmit={handleSubmit((values) => saveMutation.mutate(values))}>
        <label>
          중그룹
          <select {...register("middleGroupId", { required: true })}>
            {middleGroups.map((middleGroup) => (
              <option key={middleGroup.id} value={middleGroup.id}>
                {middleGroup.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          이름
          <input {...register("name", { required: true })} placeholder="1셀" />
        </label>
        <label>
          셀리더
          <input {...register("cellLeaderName")} placeholder="홍길동" />
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
          <button className="button button--primary" disabled={saveMutation.isPending || !middleGroups.length} type="submit">
            {editingId ? "수정 저장" : "셀 추가"}
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
              <th>중그룹</th>
              <th>이름</th>
              <th>셀리더</th>
              <th>순서</th>
              <th>상태</th>
              <th>처리</th>
            </tr>
          </thead>
          <tbody>
            {cells.map((cell) => (
              <tr key={cell.id}>
                <td>{cell.middleGroupName}</td>
                <td>
                  <strong>{cell.name}</strong>
                </td>
                <td>{cell.cellLeaderName ?? "-"}</td>
                <td>{cell.displayOrder}</td>
                <td>
                  <span className={cell.active ? "status-pill status-pill--success" : "status-pill status-pill--neutral"}>
                    {cell.active ? "활성" : "비활성"}
                  </span>
                </td>
                <td>
                  <div className="table-actions">
                    <button className="table-action" onClick={() => startEdit(cell)} type="button">
                      수정
                    </button>
                    <button
                      className={cell.active ? "table-action table-action--warning" : "table-action"}
                      disabled={activeMutation.isPending}
                      onClick={() => activeMutation.mutate({ id: cell.id, active: !cell.active })}
                      type="button"
                    >
                      {cell.active ? "비활성화" : "활성화"}
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {query.isLoading ? <EmptyState title="셀 목록을 불러오는 중입니다" message="잠시만 기다려 주세요." /> : null}
        {!query.isLoading && !cells.length ? (
          <EmptyState title="조건에 맞는 셀이 없습니다" message="중그룹을 먼저 등록한 뒤 셀을 추가해 주세요." />
        ) : null}
      </div>
    </section>
  );
}
