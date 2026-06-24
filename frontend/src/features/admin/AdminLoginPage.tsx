import { useEffect, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { Link, useNavigate } from "react-router-dom";
import { loginAdmin, roleLabel } from "./adminApi";
import { setAccessToken } from "../../shared/auth/tokenStore";
import { useAppIdentity } from "../../shared/identity/appIdentity";
import { BrandHeader } from "../../shared/layout/BrandHeader";
import { StatusMessage } from "../../shared/ui/StatusMessage";
import { HomeIcon } from "../../shared/ui/icons";

type LoginForm = {
  email: string;
  password: string;
};

export function AdminLoginPage() {
  const navigate = useNavigate();
  const { identity } = useAppIdentity();
  const [welcome, setWelcome] = useState<{ name: string; role: string } | null>(null);
  const { register, handleSubmit, resetField, setFocus } = useForm<LoginForm>();

  const mutation = useMutation({
    mutationFn: (values: LoginForm) => loginAdmin(values.email, values.password),
    onSuccess: (data) => {
      setAccessToken(data.accessToken);
      setWelcome({ name: data.admin.name, role: roleLabel(data.admin.role) });
      setTimeout(() => navigate("/admin/dashboard"), 1100);
    },
    onError: () => {
      resetField("password");
      setFocus("password");
    }
  });

  useEffect(() => {
    setFocus("email");
  }, [setFocus]);

  return (
    <div className="public-shell">
      <header className="public-header">
        <BrandHeader />
        <Link className="icon-link" to="/" aria-label="홈으로">
          <HomeIcon />
        </Link>
      </header>

      <main className="public-main">
        <section className="register-flow">
          <div className="register-flow__header">
            <p className="eyebrow">Admin</p>
            <h1>관리자 로그인</h1>
            <p className="muted">{identity.appName} 운영 정보를 확인하려면 로그인하세요.</p>
          </div>

          {welcome ? (
            <div className="completion-card" role="status">
              <p className="eyebrow">Welcome</p>
              <h2>환영해요 {welcome.name}님</h2>
              <p className="muted">{welcome.role} 권한 확인됐습니다. 운영 화면으로 이동할게요.</p>
            </div>
          ) : (
            <form className="form-grid" onSubmit={handleSubmit((values) => mutation.mutate(values))}>
              <label className="flow-field flow-field--lg">
                <span>이메일</span>
                <input {...register("email", { required: true })} autoComplete="username" placeholder="name@example.com" />
              </label>
              <label className="flow-field flow-field--lg">
                <span>비밀번호</span>
                <input {...register("password", { required: true })} autoComplete="current-password" type="password" />
              </label>
              <button className="button button--primary" disabled={mutation.isPending} type="submit">
                {mutation.isPending ? "로그인 중..." : "로그인"}
              </button>
            </form>
          )}

          {mutation.isError ? <StatusMessage message={mutation.error.message} tone="error" /> : null}
        </section>
      </main>
    </div>
  );
}
