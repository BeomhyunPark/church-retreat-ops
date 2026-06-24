import { useState, type KeyboardEvent } from "react";
import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { lookupRegistration, selfUpdateRegistration, type RegistrationResponse, type RegistrationSelfUpdatePayload } from "./publicApi";
import { StatusMessage } from "../../shared/ui/StatusMessage";

type AttendanceType = "FULL" | "PARTIAL" | "WORSHIP_ONLY";
type TransportationMethod = "OWN_CAR" | "BUS" | "PUBLIC_TRANSIT" | "RIDE_NEEDED";

type FormValues = {
  name: string;
  phoneLastFour: string;
  lookupKey: string;
  gender: "MALE" | "FEMALE";
  birthYear: number;
  phoneNumber: string;
  churchCellDepartment?: string;
  attendanceType: AttendanceType;
  transportation: TransportationMethod;
  carpoolAvailable?: boolean;
  carpoolSeats?: number;
  lodgingNight1?: boolean;
  lodgingNight2?: boolean;
  attendDay1Morning?: boolean;
  attendDay1Afternoon?: boolean;
  attendDay1Worship?: boolean;
  attendDay2Morning?: boolean;
  attendDay2Afternoon?: boolean;
  attendDay2Worship?: boolean;
  attendDay3Morning?: boolean;
  attendDay3Afternoon?: boolean;
};

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

function getTransportationOptions(attendanceType: AttendanceType): TransportationMethod[] {
  if (attendanceType === "FULL") {
    return ["OWN_CAR", "BUS"];
  }
  return ["OWN_CAR", "PUBLIC_TRANSIT", "RIDE_NEEDED"];
}

function getTransportationLabel(method: TransportationMethod): string {
  const labels: Record<TransportationMethod, string> = {
    OWN_CAR: "자차",
    BUS: "버스",
    PUBLIC_TRANSIT: "대중교통",
    RIDE_NEEDED: "차량 필요"
  };
  return labels[method];
}

function getAttendanceLabel(type: AttendanceType): string {
  const labels: Record<AttendanceType, string> = {
    FULL: "전체 참석",
    PARTIAL: "부분 참석",
    WORSHIP_ONLY: "예배만"
  };
  return labels[type];
}

function buildSteps(isLoaded: boolean, attendanceType?: AttendanceType): Array<keyof FormValues> {
  // Initial lookup steps
  const lookupSteps: Array<keyof FormValues> = ["name", "phoneLastFour", "lookupKey"];

  if (!isLoaded) {
    return lookupSteps;
  }

  // After loading, include edit steps
  const editSteps: Array<keyof FormValues> = ["attendanceType"];

  if (!attendanceType) {
    return [...lookupSteps, ...editSteps];
  }

  const steps: Array<keyof FormValues> = [...lookupSteps, ...editSteps, "transportation"];

  if (attendanceType === "FULL") {
    // FULL: no carpool, lodging, or checklist
    // steps already has transportation
  } else if (attendanceType === "PARTIAL") {
    // PARTIAL: carpool (if OWN_CAR), lodging, checklist
    steps.push("carpoolAvailable");
    steps.push("lodgingNight1");
    steps.push("attendDay1Morning");
  } else {
    // WORSHIP_ONLY: carpool (if OWN_CAR), no lodging, checklist
    steps.push("carpoolAvailable");
    steps.push("attendDay1Morning");
  }

  return steps;
}

export function PublicSelfEditPage() {
  const [updated, setUpdated] = useState(false);
  const [step, setStep] = useState(0);
  const [shake, setShake] = useState(false);
  const [isLoaded, setIsLoaded] = useState(false);
  const [loadedData, setLoadedData] = useState<RegistrationResponse | null>(null);

  const {
    register,
    handleSubmit,
    trigger,
    setValue,
    watch,
    formState,
    getValues
  } = useForm<FormValues>({
    mode: "onBlur",
    defaultValues: {
      lodgingNight1: false,
      lodgingNight2: false,
      attendDay1Morning: false,
      attendDay1Afternoon: false,
      attendDay1Worship: false,
      attendDay2Morning: false,
      attendDay2Afternoon: false,
      attendDay2Worship: false,
      attendDay3Morning: false,
      attendDay3Afternoon: false,
      carpoolAvailable: false
    }
  });

  const lookupMutation = useMutation({
    mutationFn: async () => {
      const { name, lookupKey } = getValues();
      return lookupRegistration({ name, lookupKey });
    },
    onSuccess: (data) => {
      setLoadedData(data);
      setIsLoaded(true);
      // Prefill form with existing data
      prefillFormWithData(data);
    }
  });

  const updateMutation = useMutation({
    mutationFn: selfUpdateRegistration,
    onSuccess: () => setUpdated(true)
  });

  function prefillFormWithData(data: RegistrationResponse) {
    // We'll need to parse phoneNumber to get the last 4 digits
    const phoneLastFour = data.phoneNumber.slice(-4);

    // Prefill only the editable fields - others remain as is
    setValue("phoneNumber", data.phoneNumber);
    setValue("phoneLastFour", phoneLastFour);
  }

  const attendanceType = watch("attendanceType");
  const transportation = watch("transportation");
  const carpoolAvailable = watch("carpoolAvailable");
  const gender = watch("gender");

  const currentSteps = buildSteps(isLoaded, attendanceType);
  const isLastStep = step === currentSteps.length - 1;

  function onSubmit(values: FormValues) {
    const payload: RegistrationSelfUpdatePayload = {
      name: values.name,
      phoneLastFour: values.phoneLastFour,
      lookupKey: values.lookupKey,
      update: {
        gender: values.gender,
        birthYear: values.birthYear,
        phoneNumber: values.phoneNumber,
        churchCellDepartment: values.churchCellDepartment,
        attendanceType: values.attendanceType,
        transportation: values.transportation,
        carpoolAvailable: values.carpoolAvailable,
        carpoolSeats: values.carpoolSeats,
        lodgingNight1: values.lodgingNight1,
        lodgingNight2: values.lodgingNight2,
        attendDay1Morning: values.attendDay1Morning,
        attendDay1Afternoon: values.attendDay1Afternoon,
        attendDay1Worship: values.attendDay1Worship,
        attendDay2Morning: values.attendDay2Morning,
        attendDay2Afternoon: values.attendDay2Afternoon,
        attendDay2Worship: values.attendDay2Worship,
        attendDay3Morning: values.attendDay3Morning,
        attendDay3Afternoon: values.attendDay3Afternoon
      }
    };

    // For FULL attendance, remove survey-related fields
    if (values.attendanceType === "FULL") {
      delete payload.update.carpoolAvailable;
      delete payload.update.carpoolSeats;
      delete payload.update.lodgingNight1;
      delete payload.update.lodgingNight2;
      delete payload.update.attendDay1Morning;
      delete payload.update.attendDay1Afternoon;
      delete payload.update.attendDay1Worship;
      delete payload.update.attendDay2Morning;
      delete payload.update.attendDay2Afternoon;
      delete payload.update.attendDay2Worship;
      delete payload.update.attendDay3Morning;
      delete payload.update.attendDay3Afternoon;
    }

    updateMutation.mutate(payload);
  }

  async function goNext() {
    const currentField = currentSteps[step];

    // If we're at the lookup step, trigger lookup
    if (currentField === "lookupKey" && !isLoaded) {
      const valid = await trigger(["name", "phoneLastFour", "lookupKey"]);
      if (!valid) {
        setShake(true);
        return;
      }
      await lookupMutation.mutateAsync();
      setStep((current) => Math.min(current + 1, currentSteps.length - 1));
    } else {
      const valid = await trigger(currentField);
      if (!valid) {
        setShake(true);
        return;
      }
      setStep((current) => Math.min(current + 1, currentSteps.length - 1));
    }
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
        <p className="eyebrow">Update Registration</p>
        <h1>내 등록 수정</h1>
        <p className="muted">기존 정보를 확인하고 필요한 항목을 수정할 수 있습니다.</p>
      </div>

      {!updated ? (
        <form
          className="form-grid wizard"
          onSubmit={handleSubmit(onSubmit, () => setShake(true))}
        >
          <div className="wizard__progress">
            {currentSteps.map((field, index) => (
              <span key={field} className={index <= step ? "wizard__dot wizard__dot--active" : "wizard__dot"} />
            ))}
          </div>

          <div
            className={shake ? "wizard__step wizard__step--shake" : "wizard__step"}
            onKeyDown={handleStepKeyDown}
            onAnimationEnd={() => setShake(false)}
          >
            {/* Step: name */}
            {currentSteps[step] === "name" ? (
              <label className="flow-field flow-field--lg">
                <span>이름</span>
                <input {...register("name", { required: true })} autoComplete="name" placeholder="홍길동" autoFocus />
              </label>
            ) : null}

            {/* Step: phoneLastFour */}
            {currentSteps[step] === "phoneLastFour" ? (
              <label className="flow-field flow-field--lg">
                <span>전화번호 끝 4자리</span>
                <input
                  {...register("phoneLastFour", { required: true, pattern: /^[0-9]{4}$/ })}
                  inputMode="numeric"
                  maxLength={4}
                  placeholder="1234"
                  autoFocus
                />
                {formState.errors.phoneLastFour ? <span className="field-error">숫자 4자리로 입력해주세요.</span> : null}
              </label>
            ) : null}

            {/* Step: lookupKey */}
            {currentSteps[step] === "lookupKey" && !isLoaded ? (
              <label className="flow-field flow-field--lg">
                <span>비밀번호 (숫자 6자리)</span>
                <input
                  {...register("lookupKey", { required: true, pattern: /^[0-9]{6}$/ })}
                  inputMode="numeric"
                  maxLength={6}
                  placeholder="예: 123456"
                  autoFocus
                />
                {formState.errors.lookupKey ? <span className="field-error">숫자 6자리로 입력해주세요.</span> : null}
              </label>
            ) : null}

            {/* Step: attendanceType */}
            {currentSteps[step] === "attendanceType" && isLoaded ? (
              <div className="flow-field flow-field--lg">
                <span>참석 방식</span>
                <div className="option-cards" role="radiogroup" aria-label="참석 방식">
                  {(["FULL", "PARTIAL", "WORSHIP_ONLY"] as AttendanceType[]).map((type) => (
                    <button
                      key={type}
                      type="button"
                      className={
                        attendanceType === type
                          ? "option-card option-card--active"
                          : "option-card"
                      }
                      onClick={() => {
                        setValue("attendanceType", type, { shouldValidate: true });
                        // Reset dependent fields when changing attendance type
                        setValue("transportation", undefined as any);
                        setValue("carpoolAvailable", false);
                        setValue("lodgingNight1", false);
                        setValue("lodgingNight2", false);
                      }}
                    >
                      {getAttendanceLabel(type)}
                    </button>
                  ))}
                </div>
                <input type="hidden" {...register("attendanceType", { required: true })} />
              </div>
            ) : null}

            {/* Step: transportation */}
            {currentSteps[step] === "transportation" && attendanceType && isLoaded ? (
              <div className="flow-field flow-field--lg">
                <span>이동 방식</span>
                <div className="option-cards" role="radiogroup" aria-label="이동 방식">
                  {getTransportationOptions(attendanceType).map((method) => (
                    <button
                      key={method}
                      type="button"
                      className={
                        transportation === method
                          ? "option-card option-card--active"
                          : "option-card"
                      }
                      onClick={() => {
                        setValue("transportation", method, { shouldValidate: true });
                        if (method !== "OWN_CAR") {
                          setValue("carpoolAvailable", false);
                          setValue("carpoolSeats", undefined);
                        }
                      }}
                    >
                      {getTransportationLabel(method)}
                    </button>
                  ))}
                </div>
                <input type="hidden" {...register("transportation", { required: true })} />
              </div>
            ) : null}

            {/* Step: carpoolAvailable */}
            {currentSteps[step] === "carpoolAvailable" && transportation === "OWN_CAR" && isLoaded ? (
              <div className="flow-field flow-field--lg">
                <span>동승자 탈 수 있어요?</span>
                <div className="segmented" role="radiogroup" aria-label="동승자 여부">
                  <span
                    className={carpoolAvailable !== undefined ? "segmented__pill segmented__pill--active" : "segmented__pill"}
                    style={
                      carpoolAvailable === true
                        ? { left: "50%", width: "calc(50% - 0.25rem)" }
                        : carpoolAvailable === false
                          ? { left: "0.25rem", width: "calc(50% - 0.25rem)" }
                          : { left: "50%", width: "2px", transform: "translateX(-1px)" }
                    }
                  />
                  <button
                    type="button"
                    className={carpoolAvailable === false ? "segmented__option segmented__option--active" : "segmented__option"}
                    onClick={() => {
                      setValue("carpoolAvailable", false, { shouldValidate: true });
                      setValue("carpoolSeats", undefined);
                    }}
                  >
                    아니오
                  </button>
                  <button
                    type="button"
                    className={carpoolAvailable === true ? "segmented__option segmented__option--active" : "segmented__option"}
                    onClick={() => setValue("carpoolAvailable", true, { shouldValidate: true })}
                  >
                    네
                  </button>
                </div>
                <input type="hidden" {...register("carpoolAvailable")} />
              </div>
            ) : null}

            {/* Step: carpoolSeats */}
            {currentSteps[step] === "carpoolAvailable" && transportation === "OWN_CAR" && carpoolAvailable && isLoaded ? (
              <label className="flow-field flow-field--lg">
                <span>몇 명까지 가능해요?</span>
                <input
                  {...register("carpoolSeats", { min: 1, max: 10 })}
                  inputMode="numeric"
                  placeholder="1"
                  autoFocus
                />
              </label>
            ) : null}

            {/* Step: lodgingNight1 and lodgingNight2 */}
            {currentSteps[step] === "lodgingNight1" && attendanceType === "PARTIAL" && isLoaded ? (
              <div className="flow-field flow-field--lg">
                <span>숙박</span>
                <div>
                  <label className="check-row">
                    <input {...register("lodgingNight1")} type="checkbox" />
                    <span>첫째 밤 (금요일)</span>
                  </label>
                  <label className="check-row">
                    <input {...register("lodgingNight2")} type="checkbox" />
                    <span>둘째 밤 (토요일)</span>
                  </label>
                </div>
              </div>
            ) : null}

            {/* Step: attendance checklist */}
            {currentSteps[step] === "attendDay1Morning" && (attendanceType === "PARTIAL" || attendanceType === "WORSHIP_ONLY") && isLoaded ? (
              <div className="flow-field flow-field--lg">
                <span>참석 시간</span>
                <div className="check-grid">
                  <div>
                    <h3 className="checklist-day-label">금요일 (Day 1)</h3>
                    <label className="check-row">
                      <input {...register("attendDay1Morning")} type="checkbox" />
                      <span>아침</span>
                    </label>
                    <label className="check-row">
                      <input {...register("attendDay1Afternoon")} type="checkbox" />
                      <span>오후</span>
                    </label>
                    <label className="check-row">
                      <input {...register("attendDay1Worship")} type="checkbox" />
                      <span>예배</span>
                    </label>
                  </div>
                  <div>
                    <h3 className="checklist-day-label">토요일 (Day 2)</h3>
                    <label className="check-row">
                      <input {...register("attendDay2Morning")} type="checkbox" />
                      <span>아침</span>
                    </label>
                    <label className="check-row">
                      <input {...register("attendDay2Afternoon")} type="checkbox" />
                      <span>오후</span>
                    </label>
                    <label className="check-row">
                      <input {...register("attendDay2Worship")} type="checkbox" />
                      <span>예배</span>
                    </label>
                  </div>
                  <div>
                    <h3 className="checklist-day-label">일요일 (Day 3)</h3>
                    <label className="check-row">
                      <input {...register("attendDay3Morning")} type="checkbox" />
                      <span>아침</span>
                    </label>
                    <label className="check-row">
                      <input {...register("attendDay3Afternoon")} type="checkbox" />
                      <span>오후</span>
                    </label>
                  </div>
                </div>
              </div>
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
              <button type="button" className="button button--primary" onClick={goNext} disabled={lookupMutation.isPending}>
                {lookupMutation.isPending ? "조회 중..." : "다음"}
              </button>
            ) : (
              <button className="button button--primary" disabled={updateMutation.isPending || formState.isSubmitting} type="submit">
                {updateMutation.isPending ? "저장 중..." : "저장하기"}
              </button>
            )}
          </div>
        </form>
      ) : null}

      {lookupMutation.isError ? <StatusMessage message={lookupMutation.error.message} tone="error" /> : null}
      {updateMutation.isError ? <StatusMessage message={updateMutation.error.message} tone="error" /> : null}
      {updated ? (
        <div className="completion-card" role="status">
          <p className="eyebrow">Updated</p>
          <h2>수정 완료했어요</h2>
          <p className="muted">등록 정보가 저장되었습니다.</p>
          <div className="completion-actions">
            <button className="button button--primary" onClick={goHome}>
              처음으로
            </button>
          </div>
        </div>
      ) : null}
    </section>
  );
}
