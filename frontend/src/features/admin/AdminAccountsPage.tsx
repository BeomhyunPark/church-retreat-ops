import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import {
  createAdminAccount,
  getAdminAccounts,
  updateAdminAccount,
  updateAdminAccountStatus,
  resetAdminAccountPassword,
  roleLabel,
  type AdminAccount,
  type AdminRoleValue,
  type AdminStatusValue
} from "./adminApi";
import { EmptyState } from "../../shared/ui/EmptyState";
import { StatusMessage } from "../../shared/ui/StatusMessage";

const roles: AdminRoleValue[] = ["STAFF", "CHAIR", "PASTOR", "SYSTEM_ADMIN"];

type FormValues = {
  email: string;
  name: string;
  password: string;
  role: AdminRoleValue;
};

export function AdminAccountsPage() {
  const queryClient = useQueryClient();
  const [editingId, setEditingId] = useState<number | null>(null);
  const [resetTargetId, setResetTargetId] = useState<number | null>(null);
  const [resetPassword, setResetPassword] = useState("");

  const query = useQuery({
    queryKey: ["admin", "users"],
    queryFn: getAdminAccounts
  });
  const accounts = query.data ?? [];

  const { register, handleSubmit, reset } = useForm<FormValues>({
    defaultValues: { email: "", name: "", password: "", role: "STAFF" }
  });

  function invalidate() {
    void queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
  }

  const saveMutation = useMutation({
    mutationFn: (values: FormValues) =>
      editingId
        ? updateAdminAccount(editingId, { name: values.name, role: values.role })
        : createAdminAccount(values),
    onSuccess: () => {
      invalidate();
      setEditingId(null);
      reset({ email: "", name: "", password: "", role: "STAFF" });
    }
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: AdminStatusValue }) => updateAdminAccountStatus(id, status),
    onSuccess: invalidate
  });

  const resetMutation = useMutation({
    mutationFn: ({ id, newPassword }: { id: number; newPassword: string }) =>
      resetAdminAccountPassword(id, newPassword),
    onSuccess: () => {
      setResetTargetId(null);
      setResetPassword("");
    }
  });

  function startEdit(account: AdminAccount) {
    setEditingId(account.id);
    reset({ email: account.email, name: account.name, password: "", role: account.role });
  }

  function cancelEdit() {
    setEditingId(null);
    reset({ email: "", name: "", password: "", role: "STAFF" });
  }

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Accounts</p>
          <h1>관리자 계정</h1>
        </div>
        <span className="pill">SYSTEM_ADMIN 전용</span>
      </div>

      {query.isError ? <StatusMessage message={query.error.message} tone="error" /> : null}
      {saveMutation.isError ? <StatusMessage message={saveMutation.error.message} tone="error" /> : null}
      {statusMutation.isError ? <StatusMessage message={statusMutation.error.message} tone="error" /> : null}
      {resetMutation.isError ? <StatusMessage message={resetMutation.error.message} tone="error" /> : null}

      <form className="form-grid" onSubmit={handleSubmit((values) => saveMutation.mutate(values))}>
        <label>
          이메일
          <input {...register("email", { required: true })} disabled={Boolean(editingId)} placeholder="staff@example.local" type="email" />
        </label>
        <label>
          이름
          <input {...register("name", { required: true })} placeholder="홍길동" />
        </label>
        {editingId ? null : (
          <label>
            초기 비밀번호
            <input {...register("password", { required: !editingId, minLength: 8 })} type="password" />
          </label>
        )}
        <label>
          권한
          <select {...register("role", { required: true })}>
            {roles.map((role) => (
              <option key={role} value={role}>
                {roleLabel(role)}
              </option>
            ))}
          </select>
        </label>
        <div className="table-actions">
          <button className="button button--primary" disabled={saveMutation.isPending} type="submit">
            {editingId ? "수정 저장" : "계정 추가"}
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
              <th>이메일</th>
              <th>이름</th>
              <th>권한</th>
              <th>상태</th>
              <th>마지막 로그인</th>
              <th>처리</th>
            </tr>
          </thead>
          <tbody>
            {accounts.map((account) => (
              <tr key={account.id}>
                <td>{account.email}</td>
                <td>
                  <strong>{account.name}</strong>
                </td>
                <td>{roleLabel(account.role)}</td>
                <td>
                  <span className={statusClassName(account.status)}>{statusLabel(account.status)}</span>
                </td>
                <td>{account.lastLoginAt ? new Date(account.lastLoginAt).toLocaleString() : "-"}</td>
                <td>
                  <div className="table-actions">
                    <button className="table-action" onClick={() => startEdit(account)} type="button">
                      수정
                    </button>
                    {account.status === "ACTIVE" ? (
                      <button
                        className="table-action table-action--warning"
                        disabled={statusMutation.isPending}
                        onClick={() => statusMutation.mutate({ id: account.id, status: "INACTIVE" })}
                        type="button"
                      >
                        비활성화
                      </button>
                    ) : (
                      <button
                        className="table-action"
                        disabled={statusMutation.isPending}
                        onClick={() => statusMutation.mutate({ id: account.id, status: "ACTIVE" })}
                        type="button"
                      >
                        활성화
                      </button>
                    )}
                    <button
                      className="table-action table-action--warning"
                      disabled={statusMutation.isPending || account.status === "LOCKED"}
                      onClick={() => statusMutation.mutate({ id: account.id, status: "LOCKED" })}
                      type="button"
                    >
                      잠금
                    </button>
                    {resetTargetId === account.id ? (
                      <form
                        className="table-actions"
                        onSubmit={(event) => {
                          event.preventDefault();
                          resetMutation.mutate({ id: account.id, newPassword: resetPassword });
                        }}
                      >
                        <input
                          minLength={8}
                          onChange={(event) => setResetPassword(event.target.value)}
                          placeholder="새 비밀번호"
                          required
                          type="password"
                          value={resetPassword}
                        />
                        <button className="table-action" disabled={resetMutation.isPending} type="submit">
                          저장
                        </button>
                        <button
                          className="table-action"
                          onClick={() => {
                            setResetTargetId(null);
                            setResetPassword("");
                          }}
                          type="button"
                        >
                          취소
                        </button>
                      </form>
                    ) : (
                      <button className="table-action" onClick={() => setResetTargetId(account.id)} type="button">
                        비밀번호 재설정
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {query.isLoading ? <EmptyState title="관리자 계정을 불러오는 중입니다" message="잠시만 기다려 주세요." /> : null}
        {!query.isLoading && !accounts.length ? (
          <EmptyState title="등록된 관리자 계정이 없습니다" message="위 양식으로 계정을 추가해 주세요." />
        ) : null}
      </div>
    </section>
  );
}

function statusLabel(status: AdminStatusValue) {
  const labels: Record<AdminStatusValue, string> = {
    ACTIVE: "활성",
    INACTIVE: "비활성",
    LOCKED: "잠김"
  };

  return labels[status];
}

function statusClassName(status: AdminStatusValue) {
  if (status === "ACTIVE") {
    return "status-pill status-pill--success";
  }

  if (status === "LOCKED") {
    return "status-pill status-pill--danger";
  }

  return "status-pill status-pill--neutral";
}
