import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { loginAdmin } from "./adminApi";
import { setAccessToken } from "../../shared/auth/tokenStore";
import { StatusMessage } from "../../shared/ui/StatusMessage";

type LoginForm = {
  email: string;
  password: string;
};

export function AdminLoginPage() {
  const navigate = useNavigate();
  const { register, handleSubmit } = useForm<LoginForm>();

  const mutation = useMutation({
    mutationFn: (values: LoginForm) => loginAdmin(values.email, values.password),
    onSuccess: (data) => {
      setAccessToken(data.accessToken);
      navigate("/admin/dashboard");
    }
  });

  return (
    <main className="login-shell">
      <section className="login-card">
        <span className="brand-mark">GMC</span>
        <p className="eyebrow">Retreat Ops</p>
        <h1>관리자 로그인</h1>
        <p className="muted">등록, 참가비, 현장 운영 정보를 확인하려면 관리자 계정으로 로그인하세요.</p>
        <form className="form-grid" onSubmit={handleSubmit((values) => mutation.mutate(values))}>
          <label>
            이메일
            <input {...register("email", { required: true })} autoComplete="username" placeholder="name@example.com" />
          </label>
          <label>
            비밀번호
            <input {...register("password", { required: true })} autoComplete="current-password" type="password" />
          </label>
          <button className="button button--primary" disabled={mutation.isPending} type="submit">
            {mutation.isPending ? "로그인 중..." : "로그인"}
          </button>
        </form>
        {mutation.isError ? <StatusMessage message={mutation.error.message} tone="error" /> : null}
      </section>
    </main>
  );
}
