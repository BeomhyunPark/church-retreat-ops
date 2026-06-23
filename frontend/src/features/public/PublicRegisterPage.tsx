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
    <section className="panel">
      <p className="eyebrow">Registration</p>
      <h1>참가 등록</h1>
      <p className="muted">연락 가능한 정보로 입력해 주세요. 등록 완료 후 표시되는 조회 키는 다시 확인할 수 없습니다.</p>

      <form className="form-grid" onSubmit={handleSubmit(onSubmit)}>
        <label>
          이름
          <input {...register("name", { required: true })} autoComplete="name" placeholder="홍길동" />
        </label>
        <label>
          성별
          <select {...register("gender", { required: true })}>
            <option value="FEMALE">여성</option>
            <option value="MALE">남성</option>
          </select>
        </label>
        <label>
          출생연도
          <input {...register("birthYear", { required: true })} inputMode="numeric" placeholder="1991" />
        </label>
        <label>
          전화번호
          <input {...register("phoneNumber", { required: true })} inputMode="tel" placeholder="010-1234-5678" />
        </label>
        <label>
          교구/셀
          <input {...register("churchCellDepartment")} placeholder="드림공동체 1셀" />
        </label>
        <label className="check-row">
          <input {...register("privacyConsentAgreed", { required: true })} type="checkbox" />
          개인정보 수집 및 이용에 동의합니다.
        </label>

        <button className="button button--primary" disabled={mutation.isPending || formState.isSubmitting} type="submit">
          {mutation.isPending ? "등록 중..." : "등록하기"}
        </button>
      </form>

      {mutation.isError ? <StatusMessage message={mutation.error.message} tone="error" /> : null}
      {lookupKey ? (
        <div className="completion-card" role="status">
          <p className="eyebrow">Registered</p>
          <h2>등록이 완료되었습니다</h2>
          <p className="muted">아래 조회 키는 다시 표시되지 않습니다. 등록 조회와 현장 확인에 사용할 수 있습니다.</p>
          <div className="lookup-key-box">
            <span>내 조회 키</span>
            <strong>{lookupKey}</strong>
          </div>
          <div className="completion-actions">
            <Link className="button button--primary" to="/public/self-lookup">
              내 등록 조회하기
            </Link>
            <Link className="button button--secondary" to="/public">
              홈으로 돌아가기
            </Link>
          </div>
        </div>
      ) : null}
    </section>
  );
}
