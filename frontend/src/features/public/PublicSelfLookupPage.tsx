import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { lookupRegistration, type RegistrationSelfLookupPayload } from "./publicApi";
import { StatusMessage } from "../../shared/ui/StatusMessage";

export function PublicSelfLookupPage() {
  const { register, handleSubmit } = useForm<RegistrationSelfLookupPayload>();
  const mutation = useMutation({ mutationFn: lookupRegistration });

  return (
    <section className="panel">
      <p className="eyebrow">Public</p>
      <h1>내 등록 조회</h1>
      <form className="form-grid" onSubmit={handleSubmit((values) => mutation.mutate(values))}>
        <label>
          이름
          <input {...register("name", { required: true })} autoComplete="name" />
        </label>
        <label>
          전화번호 끝 4자리
          <input {...register("phoneLastFour", { required: true, minLength: 4, maxLength: 4 })} inputMode="numeric" />
        </label>
        <label>
          조회 키
          <input {...register("lookupKey", { required: true })} />
        </label>
        <button className="button button--primary" disabled={mutation.isPending} type="submit">
          조회하기
        </button>
      </form>

      {mutation.isError ? <StatusMessage message={mutation.error.message} tone="error" /> : null}
      {mutation.data ? (
        <div className="result-card">
          <strong>{mutation.data.name}</strong>
          <span>{mutation.data.phoneNumber}</span>
          <span>{mutation.data.feePaid ? "참가비 납부 완료" : "참가비 미납"}</span>
          <span>상태: {mutation.data.status}</span>
        </div>
      ) : null}
    </section>
  );
}
