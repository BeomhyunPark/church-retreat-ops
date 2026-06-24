import { useState, type KeyboardEvent } from "react";
import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { Link } from "react-router-dom";
import { createRegistration, type TransportationMethod } from "./publicApi";
import { StatusMessage } from "../../shared/ui/StatusMessage";

// Validation helper functions
const ValidationHelpers = {
  // Filter non-digits from input
  filterNumeric: (value: string): string => value.replace(/\D/g, ""),

  // Filter to Korean and English only - remove digits, spaces, hyphen, and dangerous chars
  filterTextOnly: (value: string): string => {
    // Remove digits, spaces, hyphen, and dangerous characters
    return value.replace(/[0-9!@#$%^&*()+=\[\]{};:'"<>,.?/\\|`~\s\-]/g, "");
  },

  // Normalize whitespace (remove leading spaces, collapse multiple spaces)
  normalizeWhitespace: (value: string): string => {
    return value.replace(/\s+/g, " ").trim();
  }
};

type AttendanceType = "FULL" | "PARTIAL" | "WORSHIP_ONLY";

type FormValues = {
  name: string;
  gender: "MALE" | "FEMALE";
  birthYear: number;
  phoneNumber: string;
  churchCellDepartment?: string;
  privacyConsentAgreed: boolean;
  lookupKey: string;
  attendanceType: AttendanceType;
  inboundTransportationMethod: TransportationMethod;
  outboundTransportationMethod: TransportationMethod;
  inboundCarpoolAvailable?: boolean;
  inboundCarpoolSeats?: number;
  outboundCarpoolAvailable?: boolean;
  outboundCarpoolSeats?: number;
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
    return ["OWN_CAR", "GROUP_BUS", "WORSHIP_SHUTTLE"];
  }
  return ["OWN_CAR", "GROUP_BUS", "WORSHIP_SHUTTLE", "PUBLIC_TRANSIT", "CARPOOL_NEEDED", "NOT_DECIDED"];
}

function getTransportationLabel(method: TransportationMethod): string {
  const labels: Record<TransportationMethod, string> = {
    OWN_CAR: "자차",
    GROUP_BUS: "단체버스",
    WORSHIP_SHUTTLE: "예배셔틀",
    PUBLIC_TRANSIT: "대중교통",
    CARPOOL_NEEDED: "차량 필요",
    NOT_DECIDED: "미정"
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

function buildSteps(attendanceType?: AttendanceType): Array<keyof FormValues> {
  const baseSteps: Array<keyof FormValues> = [
    "name",
    "gender",
    "birthYear",
    "phoneNumber",
    "churchCellDepartment",
    "attendanceType"
  ];

  if (!attendanceType) {
    return baseSteps;
  }

  // All attendance types have both inbound and outbound transportation
  const steps: Array<keyof FormValues> = [
    ...baseSteps,
    "inboundTransportationMethod",
    "inboundCarpoolAvailable",
    "inboundCarpoolSeats",
    "outboundTransportationMethod",
    "outboundCarpoolAvailable",
    "outboundCarpoolSeats"
  ];

  if (attendanceType === "FULL") {
    // FULL: inbound and outbound transportation only
    steps.push("lookupKey");
    steps.push("privacyConsentAgreed");
  } else if (attendanceType === "PARTIAL") {
    // PARTIAL: inbound/outbound, lodging, checklist
    steps.push("lodgingNight1");
    steps.push("attendDay1Morning");
    steps.push("lookupKey");
    steps.push("privacyConsentAgreed");
  } else {
    // WORSHIP_ONLY: inbound/outbound, checklist (no lodging)
    steps.push("attendDay1Morning");
    steps.push("lookupKey");
    steps.push("privacyConsentAgreed");
  }

  return steps;
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
    formState,
    getValues
  } = useForm<FormValues>({
    mode: "onBlur",
    defaultValues: {
      privacyConsentAgreed: false,
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
      inboundCarpoolAvailable: false,
      outboundCarpoolAvailable: false
    }
  });

  const mutation = useMutation({
    mutationFn: createRegistration,
    onSuccess: () => setRegistered(true)
  });

  const attendanceType = watch("attendanceType");
  const inboundTransportation = watch("inboundTransportationMethod");
  const outboundTransportation = watch("outboundTransportationMethod");
  const inboundCarpoolAvailable = watch("inboundCarpoolAvailable");
  const outboundCarpoolAvailable = watch("outboundCarpoolAvailable");
  const gender = watch("gender");

  const currentSteps = buildSteps(attendanceType);
  const isLastStep = step === currentSteps.length - 1;

  function onSubmit(values: FormValues) {
    const payload: any = {
      ...values,
      privacyConsentAgreed: values.privacyConsentAgreed
    };

    // For FULL attendance, remove survey-related fields
    if (values.attendanceType === "FULL") {
      delete payload.lodgingNight1;
      delete payload.lodgingNight2;
      delete payload.attendDay1Morning;
      delete payload.attendDay1Afternoon;
      delete payload.attendDay1Worship;
      delete payload.attendDay2Morning;
      delete payload.attendDay2Afternoon;
      delete payload.attendDay2Worship;
      delete payload.attendDay3Morning;
      delete payload.attendDay3Afternoon;
    }

    mutation.mutate(payload);
  }

  async function goNext() {
    const currentField = currentSteps[step];
    const valid = await trigger(currentField);
    if (!valid) {
      setShake(true);
      return;
    }
    setStep((current) => Math.min(current + 1, currentSteps.length - 1));
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
            {currentSteps.map((field, index) => (
              <span key={field} className={index <= step ? "wizard__dot wizard__dot--active" : "wizard__dot"} />
            ))}
          </div>

          <div
            className={shake ? "wizard__step wizard__step--shake" : "wizard__step"}
            onKeyDown={handleStepKeyDown}
          >
            {/* Step: name */}
            {currentSteps[step] === "name" ? (
              <label className="flow-field flow-field--lg">
                <span>이름</span>
                <input
                  {...register("name", {
                    required: "이름을 입력해주세요.",
                    minLength: { value: 2, message: "2자 이상 입력해주세요." },
                    maxLength: { value: 50, message: "50자 이하로 입력해주세요." },
                    validate: (value) => {
                      const filtered = ValidationHelpers.filterTextOnly(value);
                      return filtered.length > 0 || "한글 또는 영문만 입력 가능합니다.";
                    }
                  })}
                  onChange={(e) => {
                    const filtered = ValidationHelpers.filterTextOnly(e.target.value);
                    e.target.value = filtered;
                    const event = new Event("change", { bubbles: true });
                    e.target.dispatchEvent(event);
                  }}
                  autoComplete="name"
                  placeholder="홍길동"
                  autoFocus
                />
                {formState.errors.name && <span className="field-error">{formState.errors.name.message}</span>}
              </label>
            ) : null}

            {/* Step: gender */}
            {currentSteps[step] === "gender" ? (
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

            {/* Step: birthYear */}
            {currentSteps[step] === "birthYear" ? (
              <label className="flow-field flow-field--lg">
                <span>또래</span>
                <input
                  {...register("birthYear", {
                    required: "태어난 연도를 입력해주세요.",
                    validate: (value) => {
                      const numValue = parseInt(value as any, 10);
                      if (isNaN(numValue)) return "숫자만 입력 가능합니다.";
                      if (String(value).length !== 2) return "2자리로 입력해주세요.";
                      return true;
                    }
                  })}
                  onChange={(e) => {
                    const filtered = ValidationHelpers.filterNumeric(e.target.value).slice(0, 2);
                    e.target.value = filtered;
                  }}
                  inputMode="numeric"
                  placeholder="90"
                  maxLength={2}
                  autoFocus
                />
                {formState.errors.birthYear && <span className="field-error">{formState.errors.birthYear.message}</span>}
              </label>
            ) : null}

            {/* Step: phoneNumber */}
            {currentSteps[step] === "phoneNumber" ? (
              <label className="flow-field flow-field--lg">
                <span>전화번호</span>
                <input
                  {...phoneField}
                  onChange={(event) => {
                    const filtered = ValidationHelpers.filterNumeric(event.target.value);
                    event.target.value = formatPhoneNumber(filtered);
                    onPhoneChange(event);
                  }}
                  inputMode="tel"
                  placeholder="01012345678"
                  autoFocus
                />
                {formState.errors.phoneNumber && <span className="field-error">010-1234-5678 형식으로 입력해주세요.</span>}
              </label>
            ) : null}

            {/* Step: churchCellDepartment */}
            {currentSteps[step] === "churchCellDepartment" ? (
              <label className="flow-field flow-field--lg">
                <span>셀</span>
                <div className="suffix-input">
                  <input
                    {...register("churchCellDepartment", {
                      maxLength: { value: 100, message: "100자 이하로 입력해주세요." },
                      validate: (value) => {
                        if (!value) return true; // Optional field
                        const filtered = ValidationHelpers.filterTextOnly(value);
                        return filtered.length > 0 || "한글 또는 영문만 입력 가능합니다.";
                      }
                    })}
                    onChange={(e) => {
                      const filtered = ValidationHelpers.filterTextOnly(e.target.value);
                      e.target.value = filtered;
                    }}
                    placeholder="유성현"
                    autoFocus
                  />
                  <span className="suffix-input__suffix">셀</span>
                </div>
                {formState.errors.churchCellDepartment && <span className="field-error">{formState.errors.churchCellDepartment.message}</span>}
              </label>
            ) : null}

            {/* Step: attendanceType */}
            {currentSteps[step] === "attendanceType" ? (
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
                        setValue("inboundTransportationMethod", undefined as any);
                        setValue("outboundTransportationMethod", undefined as any);
                        setValue("inboundCarpoolAvailable", false);
                        setValue("inboundCarpoolSeats", undefined);
                        setValue("outboundCarpoolAvailable", false);
                        setValue("outboundCarpoolSeats", undefined);
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

            {/* Step: inboundTransportationMethod */}
            {currentSteps[step] === "inboundTransportationMethod" && attendanceType ? (
              <div className="flow-field flow-field--lg">
                <span>어떻게 가세요? (인바운드)</span>
                <span className="direction-label">올 때</span>
                <div className="option-cards" role="radiogroup" aria-label="인바운드 이동 방식">
                  {getTransportationOptions(attendanceType).map((method) => (
                    <button
                      key={method}
                      type="button"
                      className={
                        inboundTransportation === method
                          ? "option-card option-card--active"
                          : "option-card"
                      }
                      onClick={() => {
                        setValue("inboundTransportationMethod", method, { shouldValidate: true });
                        if (method !== "OWN_CAR") {
                          setValue("inboundCarpoolAvailable", false);
                          setValue("inboundCarpoolSeats", undefined);
                        }
                      }}
                    >
                      {getTransportationLabel(method)}
                    </button>
                  ))}
                </div>
                <input type="hidden" {...register("inboundTransportationMethod", { required: true })} />
              </div>
            ) : null}

            {/* Step: inboundCarpoolAvailable */}
            {currentSteps[step] === "inboundCarpoolAvailable" && inboundTransportation === "OWN_CAR" ? (
              <div className="flow-field flow-field--lg">
                <span>동승자 탈 수 있어요? (인바운드)</span>
                <span className="direction-label">올 때</span>
                <div className="segmented" role="radiogroup" aria-label="인바운드 동승자 여부">
                  <span
                    className={inboundCarpoolAvailable !== undefined ? "segmented__pill segmented__pill--active" : "segmented__pill"}
                    style={
                      inboundCarpoolAvailable === true
                        ? { left: "50%", width: "calc(50% - 0.25rem)" }
                        : inboundCarpoolAvailable === false
                          ? { left: "0.25rem", width: "calc(50% - 0.25rem)" }
                          : { left: "50%", width: "2px", transform: "translateX(-1px)" }
                    }
                  />
                  <button
                    type="button"
                    className={inboundCarpoolAvailable === false ? "segmented__option segmented__option--active" : "segmented__option"}
                    onClick={() => {
                      setValue("inboundCarpoolAvailable", false, { shouldValidate: true });
                      setValue("inboundCarpoolSeats", undefined);
                    }}
                  >
                    아니오
                  </button>
                  <button
                    type="button"
                    className={inboundCarpoolAvailable === true ? "segmented__option segmented__option--active" : "segmented__option"}
                    onClick={() => setValue("inboundCarpoolAvailable", true, { shouldValidate: true })}
                  >
                    네
                  </button>
                </div>
                <input type="hidden" {...register("inboundCarpoolAvailable")} />
              </div>
            ) : null}

            {/* Step: inboundCarpoolSeats */}
            {currentSteps[step] === "inboundCarpoolSeats" ? (
              inboundTransportation === "OWN_CAR" && inboundCarpoolAvailable ? (
                <label className="flow-field flow-field--lg">
                  <span>몇 명까지 가능해요? (인바운드)</span>
                  <span className="direction-label">올 때</span>
                  <input
                    {...register("inboundCarpoolSeats", {
                      required: "동승자 수를 입력해주세요.",
                      min: { value: 1, message: "최소 1명 이상이어야 합니다." },
                      max: { value: 10, message: "최대 10명까지 입력 가능합니다." },
                      validate: (value) => {
                        const numValue = parseInt(value as any, 10);
                        if (isNaN(numValue)) return "숫자만 입력 가능합니다.";
                        return true;
                      }
                    })}
                    onChange={(e) => {
                      const filtered = ValidationHelpers.filterNumeric(e.target.value).slice(0, 2);
                      e.target.value = filtered;
                    }}
                    inputMode="numeric"
                    placeholder="1"
                    autoFocus
                  />
                  {formState.errors.inboundCarpoolSeats && <span className="field-error">{formState.errors.inboundCarpoolSeats.message}</span>}
                </label>
              ) : null
            ) : null}

            {/* Step: outboundTransportationMethod */}
            {currentSteps[step] === "outboundTransportationMethod" && attendanceType ? (
              <div className="flow-field flow-field--lg">
                <span>어떻게 돌아가세요? (아웃바운드)</span>
                <span className="direction-label">돌아갈 때</span>
                <div className="option-cards" role="radiogroup" aria-label="아웃바운드 이동 방식">
                  {getTransportationOptions(attendanceType).map((method) => (
                    <button
                      key={method}
                      type="button"
                      className={
                        outboundTransportation === method
                          ? "option-card option-card--active"
                          : "option-card"
                      }
                      onClick={() => {
                        setValue("outboundTransportationMethod", method, { shouldValidate: true });
                        if (method !== "OWN_CAR") {
                          setValue("outboundCarpoolAvailable", false);
                          setValue("outboundCarpoolSeats", undefined);
                        }
                      }}
                    >
                      {getTransportationLabel(method)}
                    </button>
                  ))}
                </div>
                <input type="hidden" {...register("outboundTransportationMethod", { required: true })} />
              </div>
            ) : null}

            {/* Step: outboundCarpoolAvailable */}
            {currentSteps[step] === "outboundCarpoolAvailable" && outboundTransportation === "OWN_CAR" ? (
              <div className="flow-field flow-field--lg">
                <span>동승자 탈 수 있어요? (아웃바운드)</span>
                <span className="direction-label">돌아갈 때</span>
                <div className="segmented" role="radiogroup" aria-label="아웃바운드 동승자 여부">
                  <span
                    className={outboundCarpoolAvailable !== undefined ? "segmented__pill segmented__pill--active" : "segmented__pill"}
                    style={
                      outboundCarpoolAvailable === true
                        ? { left: "50%", width: "calc(50% - 0.25rem)" }
                        : outboundCarpoolAvailable === false
                          ? { left: "0.25rem", width: "calc(50% - 0.25rem)" }
                          : { left: "50%", width: "2px", transform: "translateX(-1px)" }
                    }
                  />
                  <button
                    type="button"
                    className={outboundCarpoolAvailable === false ? "segmented__option segmented__option--active" : "segmented__option"}
                    onClick={() => {
                      setValue("outboundCarpoolAvailable", false, { shouldValidate: true });
                      setValue("outboundCarpoolSeats", undefined);
                    }}
                  >
                    아니오
                  </button>
                  <button
                    type="button"
                    className={outboundCarpoolAvailable === true ? "segmented__option segmented__option--active" : "segmented__option"}
                    onClick={() => setValue("outboundCarpoolAvailable", true, { shouldValidate: true })}
                  >
                    네
                  </button>
                </div>
                <input type="hidden" {...register("outboundCarpoolAvailable")} />
              </div>
            ) : null}

            {/* Step: outboundCarpoolSeats */}
            {currentSteps[step] === "outboundCarpoolSeats" ? (
              outboundTransportation === "OWN_CAR" && outboundCarpoolAvailable ? (
                <label className="flow-field flow-field--lg">
                  <span>몇 명까지 가능해요? (아웃바운드)</span>
                  <span className="direction-label">돌아갈 때</span>
                  <input
                    {...register("outboundCarpoolSeats", {
                      required: "동승자 수를 입력해주세요.",
                      min: { value: 1, message: "최소 1명 이상이어야 합니다." },
                      max: { value: 10, message: "최대 10명까지 입력 가능합니다." },
                      validate: (value) => {
                        const numValue = parseInt(value as any, 10);
                        if (isNaN(numValue)) return "숫자만 입력 가능합니다.";
                        return true;
                      }
                    })}
                    onChange={(e) => {
                      const filtered = ValidationHelpers.filterNumeric(e.target.value).slice(0, 2);
                      e.target.value = filtered;
                    }}
                    inputMode="numeric"
                    placeholder="1"
                    autoFocus
                  />
                  {formState.errors.outboundCarpoolSeats && <span className="field-error">{formState.errors.outboundCarpoolSeats.message}</span>}
                </label>
              ) : null
            ) : null}

            {/* Step: lodgingNight1 and lodgingNight2 */}
            {currentSteps[step] === "lodgingNight1" && attendanceType === "PARTIAL" ? (
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
            {currentSteps[step] === "attendDay1Morning" && (attendanceType === "PARTIAL" || attendanceType === "WORSHIP_ONLY") ? (
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

            {/* Step: lookupKey (always needed, before privacy) */}
            {currentSteps[step] === "lookupKey" ? (
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

            {/* Step: privacyConsentAgreed */}
            {currentSteps[step] === "privacyConsentAgreed" ? (
              <div>
                <label className="check-row">
                  <input {...register("privacyConsentAgreed", { required: true })} type="checkbox" />
                  <span>등록 확인을 위해 개인정보 수집 및 이용에 동의합니다.</span>
                </label>
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
