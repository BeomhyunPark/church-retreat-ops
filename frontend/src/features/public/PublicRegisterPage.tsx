import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { Link } from "react-router-dom";
import { createRegistration, type RegistrationCreatePayload } from "./publicApi";
import { StatusMessage } from "../../shared/ui/StatusMessage";

type FormValues = Omit<RegistrationCreatePayload, "birthYear" | "privacyConsentAgreed"> & {
  birthYear: string;
  privacyConsentAgreed: boolean;
};

export function PublicRegisterPage() {
  const [lookupKey, setLookupKey] = useState<string | null>(null);
  const { register, handleSubmit, formState } = useForm<FormValues>({
    defaultValues: {
      gender: "FEMALE",
      privacyConsentAgreed: false
    }
  });

  const mutation = useMutation({
    mutationFn: createRegistration,
    onSuccess: (data) => setLookupKey(data.lookupKey)
  });

  function onSubmit(values: FormValues) {
    mutation.mutate({
      ...values,
      birthYear: Number(values.birthYear),
      privacyConsentAgreed: values.privacyConsentAgreed
    });
  }

  return (
    <section className="register-flow">
      <div className="register-flow__header">
        <p className="eyebrow">Registration</p>
        <h1>하나씩 적으면 끝</h1>
        <p className="muted">필요한 것만 물어볼게요. 마지막에 조회 키가 나옵니다.</p>
      </div>

      <form className="form-grid" onSubmit={handleSubmit(onSubmit)}>
        <label className="flow-field">
          <span>이름</span>
          <input {...register("name", { required: true })} autoComplete="name" placeholder="홍길동" />
        </label>
        <label className="flow-field">
          <span>전화번호</span>
          <input {...register("phoneNumber", { required: true })} inputMode="tel" placeholder="010-1234-5678" />
        </label>
        <label className="flow-field">
          <span>성별</span>
          <select {...register("gender", { required: true })}>
            <option value="FEMALE">여성</option>
            <option value="MALE">남성</option>
          </select>
        </label>
        <label className="flow-field">
          <span>출생연도</span>
          <input {...register("birthYear", { required: true })} inputMode="numeric" placeholder="1991" />
        </label>
        <label className="flow-field">
          <span>또래 / 셀</span>
          <input {...register("churchCellDepartment")} placeholder="예: 청년부 1셀" />
        </label>
        <label className="check-row">
          <input {...register("privacyConsentAgreed", { required: true })} type="checkbox" />
          <span>등록 확인을 위해 개인정보 수집 및 이용에 동의합니다.</span>
        </label>

        <button className="button button--primary" disabled={mutation.isPending || formState.isSubmitting} type="submit">
          {mutation.isPending ? "등록 중..." : "이대로 등록"}
        </button>
      </form>

      {mutation.isError ? <StatusMessage message={mutation.error.message} tone="error" /> : null}
      {lookupKey ? (
        <div className="completion-card" role="status">
          <p className="eyebrow">Registered</p>
          <h2>등록 끝났어요</h2>
          <p className="muted">이 키는 지금만 보여요. 내 등록 확인할 때 필요합니다.</p>
          <div className="lookup-key-box">
            <span>내 조회 키</span>
            <strong>{lookupKey}</strong>
          </div>
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
