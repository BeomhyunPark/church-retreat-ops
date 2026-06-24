import { useState, type KeyboardEvent } from "react";
import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { Link } from "react-router-dom";
import { createRegistration, type RegistrationCreatePayload } from "./publicApi";
import { StatusMessage } from "../../shared/ui/StatusMessage";

type FormValues = Omit<RegistrationCreatePayload, "birthYear" | "privacyConsentAgreed"> & {
  ageGroup: string;
  privacyConsentAgreed: boolean;
};

const STEP_FIELDS: Array<keyof FormValues> = [
  "name",
  "phoneNumber",
  "gender",
  "ageGroup",
  "churchCellDepartment",
  "lookupKey"
];

function formatPhoneNumber(value: string) {
  const digits = value.replace(/\D/g, "").slice(0, 11);
  if (digits.length <= 3) {
    return digits;
  }
  if (digits.length <= 7) {
    return `${digits.slice(0, 3)}-${digits.slice(3)}`;
  }
  return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`;
}

function ageGroupToBirthYear(ageGroup: string) {
  const yy = Number(ageGroup);
  const currentYy = new Date().getFullYear() % 100;
  return yy <= currentYy ? 2000 + yy : 1900 + yy;
}

export function PublicRegisterPage() {
  const [registered, setRegistered] = useState(false);
  const [step, setStep] = useState(0);
  const [shake, setShake] = useState(false);
  const {
    register,
    handleSubmit,
    trigger,
    setValue,
    watch,
    formState
  } = useForm<FormValues>({
    defaultValues: {
      privacyConsentAgreed: false
    }
  });

  const mutation = useMutation({
    mutationFn: createRegistration,
    onSuccess: () => setRegistered(true)
  });

  function onSubmit(values: FormValues) {
    mutation.mutate({
      ...values,
      birthYear: ageGroupToBirthYear(values.ageGroup),
      privacyConsentAgreed: values.privacyConsentAgreed
    });
  }

  const isLastStep = step === STEP_FIELDS.length - 1;
  const gender = watch("gender");

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

  const { onChange: onPhoneChange, ...phoneField } = register("phoneNumber", {
    required: true,
    pattern: /^[0-9]{3}-[0-9]{3,4}-[0-9]{4}$/
  });

  return (
    <section className="register-flow">
      <div className="register-flow__header">
        <p className="eyebrow">Registration</p>
        <p className="muted">필요한 것만, 한 번에 하나씩 물어볼게요.</p>
      </div>

      {!registered ? (
        <form
          className="form-grid wizard"
          onSubmit={handleSubmit(onSubmit, () => setShake(true))}
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
                <span>전화번호</span>
                <input
                  {...phoneField}
                  onChange={(event) => {
                    event.target.value = formatPhoneNumber(event.target.value);
                    onPhoneChange(event);
                  }}
                  inputMode="tel"
                  placeholder="01012345678"
                  autoFocus
                />
              </label>
            ) : null}

            {step === 2 ? (
              <div className="flow-field flow-field--lg">
                <span>성별</span>
                <div className="segmented" role="radiogroup" aria-label="성별">
                  <span
                    className={gender ? "segmented__pill segmented__pill--active" : "segmented__pill"}
                    style={
                      gender === "MALE"
                        ? { left: "50%", width: "calc(50% - 0.25rem)" }
                        : gender === "FEMALE"
                          ? { left: "0.25rem", width: "calc(50% - 0.25rem)" }
                          : { left: "50%", width: "2px", transform: "translateX(-1px)" }
                    }
                  />
                  <button
                    type="button"
                    className={gender === "FEMALE" ? "segmented__option segmented__option--active" : "segmented__option"}
                    onClick={() => setValue("gender", "FEMALE", { shouldValidate: true })}
                  >
                    여성
                  </button>
                  <button
                    type="button"
                    className={gender === "MALE" ? "segmented__option segmented__option--active" : "segmented__option"}
                    onClick={() => setValue("gender", "MALE", { shouldValidate: true })}
                  >
                    남성
                  </button>
                </div>
                <input type="hidden" {...register("gender", { required: true })} />
              </div>
            ) : null}

            {step === 3 ? (
              <label className="flow-field flow-field--lg">
                <span>또래</span>
                <input
                  {...register("ageGroup", { required: true, pattern: /^[0-9]{2}$/ })}
                  inputMode="numeric"
                  maxLength={2}
                  placeholder="00"
                  autoFocus
                />
              </label>
            ) : null}

            {step === 4 ? (
              <label className="flow-field flow-field--lg">
                <span>셀</span>
                <div className="suffix-input">
                  <input {...register("churchCellDepartment")} placeholder="유성현" autoFocus />
                  <span className="suffix-input__suffix">셀</span>
                </div>
              </label>
            ) : null}

            {step === 5 ? (
              <label className="flow-field flow-field--lg">
                <span>비밀번호 (숫자 6자리)</span>
                <input
                  {...register("lookupKey", { required: true, pattern: /^[0-9]{6}$/ })}
                  inputMode="numeric"
                  maxLength={6}
                  placeholder="예: 123456"
                  autoFocus
                />
                {formState.errors.lookupKey ? <span className="field-error">숫자 6자리로 정해주세요.</span> : null}
              </label>
            ) : null}
          </div>

          {isLastStep ? (
            <label className="check-row">
              <input {...register("privacyConsentAgreed", { required: true })} type="checkbox" />
              <span>등록 확인을 위해 개인정보 수집 및 이용에 동의합니다.</span>
            </label>
          ) : null}

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
              <button className="button button--primary" disabled={mutation.isPending || formState.isSubmitting} type="submit">
                {mutation.isPending ? "등록 중..." : "이대로 등록"}
              </button>
            )}
          </div>
        </form>
      ) : null}

      {mutation.isError ? <StatusMessage message={mutation.error.message} tone="error" /> : null}
      {registered ? (
        <div className="completion-card" role="status">
          <p className="eyebrow">Registered</p>
          <h2>등록 끝났어요</h2>
          <p className="muted">방금 정한 비밀번호로 내 등록을 확인할 수 있어요.</p>
          <div className="completion-actions">
            <Link className="button button--primary" to="/public/self-lookup">
              내 등록 확인
            </Link>
            <Link className="button button--secondary" to="/">
              처음으로
            </Link>
          </div>
        </div>
      ) : null}
    </section>
  );
}
