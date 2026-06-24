import { useState, type KeyboardEvent } from "react";
import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { lookupRegistration, type RegistrationSelfLookupPayload } from "./publicApi";
import { StatusMessage } from "../../shared/ui/StatusMessage";

const STEP_FIELDS: Array<keyof RegistrationSelfLookupPayload> = ["name", "lookupKey"];

export function PublicSelfLookupPage() {
  const [step, setStep] = useState(0);
  const [shake, setShake] = useState(false);
  const { register, handleSubmit, trigger, formState } = useForm<RegistrationSelfLookupPayload>();
  const mutation = useMutation({ mutationFn: lookupRegistration });

  const isLastStep = step === STEP_FIELDS.length - 1;

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

  function goHome() {
    window.location.href = "/";
  }

  function handleStepKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === "Enter" && !isLastStep) {
      event.preventDefault();
      goNext();
    }
  }

  return (
    <section className="register-flow">
      <div className="register-flow__header">
        <p className="eyebrow">My Registration</p>
        <h1>내 등록 조회</h1>
        <p className="muted">등록할 때 정한 비밀번호로 본인 등록 정보를 확인합니다.</p>
      </div>

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
              <span>이름</span>
              <input {...register("name", { required: true })} autoComplete="name" placeholder="홍길동" autoFocus />
            </label>
          ) : null}

          {step === 1 ? (
            <label className="flow-field flow-field--lg">
              <span>비밀번호 (숫자 6자리)</span>
              <input
                {...register("lookupKey", { required: true, pattern: /^[0-9]{6}$/ })}
                inputMode="numeric"
                maxLength={6}
                placeholder="123456"
                autoFocus
              />
              {formState.errors.lookupKey ? <span className="field-error">숫자 6자리로 입력해주세요.</span> : null}
            </label>
          ) : null}
        </div>

        <div className="wizard__actions">
          {step === 0 ? (
              <button type="button" className="button button--ghost" onClick={goHome}>
                취소
              </button>
            ) : null}
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
              {mutation.isPending ? "조회 중..." : "조회하기"}
            </button>
          )}
        </div>
      </form>

      {mutation.isError ? <StatusMessage message={mutation.error.message} tone="error" /> : null}
      {mutation.data ? (
        <div className="result-pass">
          <span className="result-pass__label">My Pass</span>
          <strong>{mutation.data.name}</strong>
          <div className="result-pass__info">
            <span className="result-pass__phone">{mutation.data.phoneNumber}</span>
            <div className="result-pass__status-group">
              <span className={mutation.data.feePaid ? "status-pill status-pill--success" : "status-pill status-pill--warning"}>
                {mutation.data.feePaid ? "참가비 완료" : "참가비 확인"}
              </span>
              <span className="status-pill status-pill--info">
                {mutation.data.status === "REGISTERED" ? "✓ 등록됨" : mutation.data.status}
              </span>
            </div>
          </div>
          <div className="result-pass__code">
            <span className="result-pass__bar result-pass__bar--filled" />
            <span className="result-pass__bar result-pass__bar--filled" />
            <span className="result-pass__bar result-pass__bar--filled" />
            <span className="result-pass__bar result-pass__bar--filled" />
          </div>
          <div className="result-pass__meta">
            <span>COMPLETE</span>
            <span>2026</span>
          </div>
        </div>
      ) : null}
    </section>
  );
}
