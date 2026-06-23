import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
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
      <p className="eyebrow">Public</p>
      <h1>참가 등록</h1>
      <p className="muted">등록 후 표시되는 조회 키는 다시 보여주지 않으니 따로 보관해야 합니다.</p>

      <form className="form-grid" onSubmit={handleSubmit(onSubmit)}>
        <label>
          이름
          <input {...register("name", { required: true })} autoComplete="name" />
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
          <input {...register("churchCellDepartment")} placeholder="청년부 A셀" />
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
        <StatusMessage message={`등록 완료. 내 조회 키: ${lookupKey}`} tone="success" />
      ) : null}
    </section>
  );
}
