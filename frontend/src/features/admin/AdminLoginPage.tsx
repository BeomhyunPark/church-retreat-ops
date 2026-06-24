import { useEffect, useState, type KeyboardEvent } from "react";
import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { Link, useNavigate } from "react-router-dom";
import { loginAdmin, roleLabel } from "./adminApi";
import { setAccessToken } from "../../shared/auth/tokenStore";
import { useAppIdentity } from "../../shared/identity/appIdentity";
import { BrandHeader } from "../../shared/layout/BrandHeader";
import { StatusMessage } from "../../shared/ui/StatusMessage";
import { CheckBadgeIcon, HomeIcon } from "../../shared/ui/icons";
import { useSwipeStep } from "../../shared/hooks/useSwipeStep";

type LoginForm = {
  email: string;
  password: string;
};

const STEP_FIELDS: Array<keyof LoginForm> = ["email", "password"];

export function AdminLoginPage() {
  const navigate = useNavigate();
  const { identity } = useAppIdentity();
  const [step, setStep] = useState(0);
  const [shake, setShake] = useState(false);
  const [welcome, setWelcome] = useState<{ name: string; role: string } | null>(null);
  const [dismissDirection, setDismissDirection] = useState<"left" | "right" | "tap" | null>(null);
  const { register, handleSubmit, trigger, resetField, setFocus } = useForm<LoginForm>();

  const isLastStep = step === STEP_FIELDS.length - 1;

  const mutation = useMutation({
    mutationFn: (values: LoginForm) => loginAdmin(values.email, values.password),
    onSuccess: (data) => {
      setAccessToken(data.accessToken);
      setWelcome({ name: data.admin.name, role: roleLabel(data.admin.role) });
    },
    onError: () => {
      resetField("password");
      setStep(1);
      setFocus("password");
    }
  });

  useEffect(() => {
    setFocus("email");
  }, [setFocus]);

  async function goNext() {
    const valid = await trigger(STEP_FIELDS[step]);
    if (!valid) {
      setShake(true);
      return;
    }
    setStep((current) => Math.min(current + 1, STEP_FIELDS.length - 1));
  }

  function goBack() {
    setStep((current) => Math.max(current - 1, 0));
  }

  function handleStepKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === "Enter" && !isLastStep) {
      event.preventDefault();
      goNext();
    }
  }

  function dismissWelcome(direction: "left" | "right" | "tap") {
    setDismissDirection(direction);
    setTimeout(() => navigate("/admin/dashboard"), 280);
  }

  const welcomeSwipe = useSwipeStep(
    () => dismissWelcome("left"),
    () => dismissWelcome("right")
  );

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
            <button
              type="button"
              className={
                dismissDirection
                  ? `welcome-card welcome-card--swipe-${dismissDirection}`
                  : "welcome-card"
              }
              onClick={() => dismissWelcome("tap")}
              onTouchStart={welcomeSwipe.onTouchStart}
              onTouchEnd={welcomeSwipe.onTouchEnd}
              aria-label={`환영해요, ${welcome.name}님. 탭하거나 스와이프해서 운영 화면으로 이동합니다.`}
            >
              <span className="welcome-card__icon">
                <CheckBadgeIcon />
              </span>
              <span className="welcome-card__badge">{welcome.role}</span>
              <h2>환영해요, {welcome.name}님</h2>
              <p>권한 확인이 끝났어요.</p>
              <p className="welcome-card__hint">탭하거나 스와이프해서 이동</p>
            </button>
          ) : (
            <form
              className="form-grid wizard"
              onSubmit={handleSubmit((values) => mutation.mutate(values), () => setShake(true))}
            >
              <div className="wizard__progress">
                {STEP_FIELDS.map((field, index) => (
                  <span key={field} className={index <= step ? "wizard__dot wizard__dot--active" : "wizard__dot"} />
                ))}
              </div>

              <div
                className={shake ? "wizard__step wizard__step--shake" : "wizard__step"}
                onKeyDown={handleStepKeyDown}
                onAnimationEnd={() => setShake(false)}
              >
                {step === 0 ? (
                  <label className="flow-field flow-field--lg">
                    <span>이메일</span>
                    <input
                      {...register("email", { required: true })}
                      autoComplete="username"
                      placeholder="name@example.com"
                      autoFocus
                    />
                  </label>
                ) : null}

                {step === 1 ? (
                  <label className="flow-field flow-field--lg">
                    <span>비밀번호</span>
                    <input
                      {...register("password", { required: true })}
                      autoComplete="current-password"
                      type="password"
                      autoFocus
                    />
                  </label>
                ) : null}
              </div>

              <div className="wizard__actions">
                {step > 0 ? (
                  <button type="button" className="button button--ghost" onClick={goBack}>
                    이전
                  </button>
                ) : null}
                {!isLastStep ? (
                  <button type="button" className="button button--primary" onClick={goNext}>
                    다음
                  </button>
                ) : (
                  <button className="button button--primary" disabled={mutation.isPending} type="submit">
                    {mutation.isPending ? "로그인 중..." : "로그인"}
                  </button>
                )}
              </div>
            </form>
          )}

          {mutation.isError ? <StatusMessage message={mutation.error.message} tone="error" /> : null}
        </section>
      </main>
    </div>
  );
}
