import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { lookupRegistration, type RegistrationSelfLookupPayload } from "./publicApi";
import { StatusMessage } from "../../shared/ui/StatusMessage";

export function PublicSelfLookupPage() {
  const { register, handleSubmit } = useForm<RegistrationSelfLookupPayload>();
  const mutation = useMutation({ mutationFn: lookupRegistration });
  const registrationStatus = mutation.data?.status === "REGISTERED" ? "등록 완료" : mutation.data?.status;

  return (
    <section className="panel">
      <div className="section-heading">
        <p className="eyebrow">My Registration</p>
        <h1>내 등록 조회</h1>
        <p className="muted">등록할 때 받은 조회 키와 전화번호 끝 4자리로 본인 등록 정보를 확인합니다.</p>
      </div>
      <form className="form-grid" onSubmit={handleSubmit((values) => mutation.mutate(values))}>
        <label>
          이름
          <input {...register("name", { required: true })} autoComplete="name" placeholder="홍길동" />
        </label>
        <label>
          전화번호 끝 4자리
          <input
            {...register("phoneLastFour", { required: true, minLength: 4, maxLength: 4 })}
            inputMode="numeric"
            placeholder="5678"
          />
        </label>
        <label>
          조회 키
          <input {...register("lookupKey", { required: true })} autoComplete="off" placeholder="등록 완료 시 받은 조회 키" />
        </label>
        <button className="button button--primary" disabled={mutation.isPending} type="submit">
          조회하기
        </button>
      </form>

      {mutation.isError ? <StatusMessage message={mutation.error.message} tone="error" /> : null}
      {mutation.data ? (
        <div className="result-card">
          <div className="result-card__header">
            <span>
              <small>참가자</small>
              <strong>{mutation.data.name}</strong>
            </span>
            <span className={mutation.data.feePaid ? "status-pill status-pill--success" : "status-pill status-pill--warning"}>
              {mutation.data.feePaid ? "참가비 납부 완료" : "참가비 확인 필요"}
            </span>
          </div>
          <dl className="summary-list">
            <div>
              <dt>전화번호</dt>
              <dd>{mutation.data.phoneNumber}</dd>
            </div>
            <div>
              <dt>등록 상태</dt>
              <dd>{registrationStatus}</dd>
            </div>
            <div>
              <dt>셀</dt>
              <dd>{mutation.data.churchCellDepartment ?? "미입력"}</dd>
            </div>
            <div>
              <dt>참석</dt>
              <dd>
                {mutation.data.attendanceType === "FULL"
                  ? "전체참석"
                  : `부분참석 · ${mutation.data.attendanceSlots.map(attendanceSlotLabel).join(", ") || "시간 미입력"}`}
              </dd>
            </div>
            <div>
              <dt>이동</dt>
              <dd>
                {transportationLabel(mutation.data.transportationType)}
                {mutation.data.carpoolNeeded ? " · 카풀 희망" : ""}
                {mutation.data.carpoolOffer ? ` · 카풀 가능 ${mutation.data.carpoolSeats ?? 0}명` : ""}
              </dd>
            </div>
          </dl>
        </div>
      ) : null}
    </section>
  );
}

function transportationLabel(value: "OWN_CAR" | "PUBLIC_TRANSPORT" | "UNDECIDED") {
  if (value === "OWN_CAR") return "자차";
  if (value === "PUBLIC_TRANSPORT") return "대중교통";
  return "미정";
}

function attendanceSlotLabel(value: string) {
  const labels: Record<string, string> = {
    DAY1_MORNING: "첫째날 오전",
    DAY1_AFTERNOON: "첫째날 오후",
    DAY1_GATHERING: "첫째날 집회",
    DAY2_MORNING: "둘째날 오전",
    DAY2_AFTERNOON: "둘째날 오후",
    DAY2_GATHERING: "둘째날 집회",
    DAY3_MORNING: "셋째날 오전",
    DAY3_AFTERNOON: "셋째날 오후",
    DAY3_GATHERING: "셋째날 집회"
  };

  return labels[value] ?? value;
}
