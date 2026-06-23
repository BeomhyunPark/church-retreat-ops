import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { changeOwnPassword } from "./adminApi";
import { StatusMessage } from "../../shared/ui/StatusMessage";

type FormValues = {
  currentPassword: string;
  newPassword: string;
};

export function AdminProfilePage() {
  const [done, setDone] = useState(false);
  const { register, handleSubmit, reset } = useForm<FormValues>();

  const mutation = useMutation({
    mutationFn: (values: FormValues) => changeOwnPassword(values.currentPassword, values.newPassword),
    onSuccess: () => {
      setDone(true);
      reset();
    }
  });

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Profile</p>
          <h1>비밀번호 변경</h1>
        </div>
      </div>

      {mutation.isError ? <StatusMessage message={mutation.error.message} tone="error" /> : null}
      {done ? <StatusMessage message="비밀번호가 변경되었습니다." tone="success" /> : null}

      <form
        className="form-grid panel"
        onSubmit={handleSubmit((values) => {
          setDone(false);
          mutation.mutate(values);
        })}
      >
        <label>
          현재 비밀번호
          <input {...register("currentPassword", { required: true })} autoComplete="current-password" type="password" />
        </label>
        <label>
          새 비밀번호
          <input
            {...register("newPassword", { required: true, minLength: 8 })}
            autoComplete="new-password"
            type="password"
          />
        </label>
        <button className="button button--primary" disabled={mutation.isPending} type="submit">
          {mutation.isPending ? "변경 중..." : "비밀번호 변경"}
        </button>
      </form>
    </section>
  );
}
