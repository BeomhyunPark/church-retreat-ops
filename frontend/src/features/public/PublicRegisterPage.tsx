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
  inboundTransportationMethod: TransportationMethod;
  outboundTransportationMethod: TransportationMethod;
  inboundCarpoolAvailable?: boolean;
  inboundCarpoolSeats?: number;
  inboundCarpoolArea?: string;
  inboundCarpoolPreferredArea?: string;
  outboundCarpoolPreferredArea?: string;
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
    return ["GROUP_BUS", "OWN_CAR"];
  }
  return ["GROUP_BUS", "WORSHIP_SHUTTLE", "OWN_CAR", "PUBLIC_TRANSIT", "CARPOOL_NEEDED", "NOT_DECIDED"];
}

function getTransportationLabel(method: TransportationMethod): string {
  const labels: Record<TransportationMethod, string> = {
    OWN_CAR: "자차",
    GROUP_BUS: "함께 이동해요",
    WORSHIP_SHUTTLE: "집회 차량",
    PUBLIC_TRANSIT: "대중교통",
    CARPOOL_NEEDED: "카풀 희망",
    NOT_DECIDED: "미정"
  };
  return labels[method];
}

function getAttendanceLabel(type: AttendanceType): string {
  const labels: Record<AttendanceType, string> = {
    FULL: "전체 참석",
    PARTIAL: "부분 참석",
    WORSHIP_ONLY: "집회만"
  };
  return labels[type];
}

function buildSteps(
  attendanceType?: AttendanceType,
  inboundTransportation?: TransportationMethod,
  inboundCarpoolAvailable?: boolean,
  outboundTransportation?: TransportationMethod
): Array<keyof FormValues> {
  const steps: Array<keyof FormValues> = [
    "name",
    "gender",
    "birthYear",
    "phoneNumber",
    "churchCellDepartment",
    "attendanceType"
  ];

  // 참석방식 선택 후 일정 확인
  if (attendanceType === "FULL") {
    // 전체참석: 자차 또는 함께 이동
    steps.push("inboundTransportationMethod");
    if (inboundTransportation === "OWN_CAR") {
      steps.push("inboundCarpoolAvailable");
      if (inboundCarpoolAvailable === true) {
        steps.push("inboundCarpoolSeats", "inboundCarpoolArea");
      }
    } else {
      // Driving yourself there means driving yourself back - outbound is only
      // asked when you didn't bring your own car inbound
      steps.push("outboundTransportationMethod");
    }
  } else if (attendanceType === "PARTIAL") {
    // 부분참석: 숙박 + 일정 선택
    steps.push("lodgingNight1", "attendDay1Morning");
    steps.push("inboundTransportationMethod");
    if (inboundTransportation === "OWN_CAR") {
      steps.push("inboundCarpoolAvailable");
      if (inboundCarpoolAvailable === true) {
        steps.push("inboundCarpoolSeats", "inboundCarpoolArea");
      }
    } else if (inboundTransportation === "CARPOOL_NEEDED") {
      steps.push("inboundCarpoolPreferredArea");
    }
    if (inboundTransportation !== "OWN_CAR") {
      steps.push("outboundTransportationMethod");
      if (outboundTransportation === "CARPOOL_NEEDED") {
        steps.push("outboundCarpoolPreferredArea");
      }
    }
  } else if (attendanceType === "WORSHIP_ONLY") {
    // 집회만: 집회 차량 또는 자차
    steps.push("inboundTransportationMethod");
    if (inboundTransportation === "OWN_CAR") {
      steps.push("inboundCarpoolAvailable");
      if (inboundCarpoolAvailable === true) {
        steps.push("inboundCarpoolSeats", "inboundCarpoolArea");
      }
    } else if (inboundTransportation === "CARPOOL_NEEDED") {
      steps.push("inboundCarpoolPreferredArea");
    }
    if (inboundTransportation !== "OWN_CAR") {
      steps.push("outboundTransportationMethod");
      if (outboundTransportation === "CARPOOL_NEEDED") {
        steps.push("outboundCarpoolPreferredArea");
      }
    }
  }

  // 마지막 단계
  steps.push("lookupKey", "privacyConsentAgreed");
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
      attendDay3Afternoon: false
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
  const gender = watch("gender");

  const currentSteps = buildSteps(attendanceType, inboundTransportation, inboundCarpoolAvailable, outboundTransportation);
  const isLastStep = step === currentSteps.length - 1;


  function onSubmit(values: FormValues) {
    const payload: any = {
      ...values,
      privacyConsentAgreed: values.privacyConsentAgreed
    };

    // The form only collects a 2-digit birth year (e.g. "92") - expand it to the
    // full 4-digit year the backend expects, using the current year as the pivot
    const currentTwoDigitYear = new Date().getFullYear() % 100;
    const enteredTwoDigitYear = Number(values.birthYear);
    payload.birthYear = enteredTwoDigitYear <= currentTwoDigitYear
      ? 2000 + enteredTwoDigitYear
      : 1900 + enteredTwoDigitYear;

    // Outbound carpool mirrors inbound: it's the same car making the same trip back
    if (values.inboundTransportationMethod === "OWN_CAR") {
      payload.outboundCarpoolAvailable = values.inboundCarpoolAvailable;
      payload.outboundCarpoolSeats = values.inboundCarpoolSeats;
      payload.outboundCarpoolArea = values.inboundCarpoolArea;
    }

    // Strip carpool fields that don't apply to the final transportation choice for
    // each direction - a leftover value from an earlier choice (e.g. "아니오" from
    // a previous OWN_CAR pick) would otherwise get sent and rejected by the backend
    for (const direction of ["inbound", "outbound"] as const) {
      const method = payload[`${direction}TransportationMethod`];
      if (method !== "OWN_CAR") {
        delete payload[`${direction}CarpoolAvailable`];
        delete payload[`${direction}CarpoolSeats`];
        delete payload[`${direction}CarpoolArea`];
      }
      if (method !== "CARPOOL_NEEDED") {
        delete payload[`${direction}CarpoolPreferredArea`];
      }
    }

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

  function handleInputKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === "Enter" && !isLastStep) {
      e.preventDefault();
      goNext();
    }
  }

  return (
    <section className="register-flow">
      <div className="register-flow__header">
        <p className="eyebrow">Registration</p>
        <p className="muted">필요한 것만, 한 번에 하나씩 물어볼게요.</p>
      </div>

      {!registered ? (
          <form
          className="form-grid wizard"
          onSubmit={(e) => {
            // Allow form submission only on last step
            if (!isLastStep) {
              e.preventDefault();
              goNext();
            } else {
              handleSubmit(onSubmit, () => setShake(true))(e);
            }
          }}
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
                        setValue("lodgingNight1", false);
                        setValue("lodgingNight2", false);
                        // 집회만 선택시 다른 일정은 자동으로 false
                        if (type === "WORSHIP_ONLY") {
                          setValue("attendDay1Morning", false);
                          setValue("attendDay1Afternoon", false);
                          setValue("attendDay1Worship", true);
                          setValue("attendDay2Morning", false);
                          setValue("attendDay2Afternoon", false);
                          setValue("attendDay2Worship", true);
                          setValue("attendDay3Morning", false);
                          setValue("attendDay3Afternoon", false);
                        } else {
                          setValue("attendDay1Morning", false);
                          setValue("attendDay1Afternoon", false);
                          setValue("attendDay1Worship", false);
                          setValue("attendDay2Morning", false);
                          setValue("attendDay2Afternoon", false);
                          setValue("attendDay2Worship", false);
                          setValue("attendDay3Morning", false);
                          setValue("attendDay3Afternoon", false);
                        }
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
                <span>어떻게 가세요?</span>
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
                        if (method === "OWN_CAR") {
                          // Same car goes back - outbound is decided, no need to ask again
                          setValue("outboundTransportationMethod", "OWN_CAR", { shouldValidate: true });
                        } else {
                          setValue("inboundCarpoolAvailable", false);
                          setValue("inboundCarpoolSeats", undefined);
                          // Car isn't there for the return trip - clear a stale OWN_CAR pick
                          if (outboundTransportation === "OWN_CAR") {
                            setValue("outboundTransportationMethod", undefined as any);
                          }
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
                <span>동승자 태울 수 있어요?</span>
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
                <input type="hidden" {...register("inboundCarpoolAvailable", { validate: (value) => value !== undefined })} />
              </div>
            ) : null}

            {/* Step: inboundCarpoolSeats */}
            {currentSteps[step] === "inboundCarpoolSeats" ? (
              inboundTransportation === "OWN_CAR" && inboundCarpoolAvailable ? (
                <label className="flow-field flow-field--lg">
                  <span>몇 명까지 가능해요?</span>
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

            {/* Step: inboundCarpoolArea */}
            {currentSteps[step] === "inboundCarpoolArea" ? (
              inboundTransportation === "OWN_CAR" && inboundCarpoolAvailable ? (
                <label className="flow-field flow-field--lg">
                  <span>어느 동네에서 태울 수 있어요?</span>
                  <input
                    {...register("inboundCarpoolArea", {
                      required: "지역을 입력해주세요.",
                      maxLength: { value: 100, message: "100자 이하로 입력해주세요." }
                    })}
                    placeholder="예: 강남역, 삼성역 근처"
                    autoFocus
                  />
                  {formState.errors.inboundCarpoolArea && <span className="field-error">{formState.errors.inboundCarpoolArea.message}</span>}
                </label>
              ) : null
            ) : null}

            {/* Step: inboundCarpoolPreferredArea */}
            {currentSteps[step] === "inboundCarpoolPreferredArea" ? (
              inboundTransportation === "CARPOOL_NEEDED" ? (
                <label className="flow-field flow-field--lg">
                  <span>어느 동네가 편해요?</span>
                  <input
                    {...register("inboundCarpoolPreferredArea", {
                      required: "지역을 입력해주세요.",
                      maxLength: { value: 100, message: "100자 이하로 입력해주세요." }
                    })}
                    placeholder="예: 강남역, 삼성역 근처"
                    autoFocus
                  />
                  {formState.errors.inboundCarpoolPreferredArea && <span className="field-error">{formState.errors.inboundCarpoolPreferredArea.message}</span>}
                </label>
              ) : null
            ) : null}

            {/* Step: outboundTransportationMethod */}
            {currentSteps[step] === "outboundTransportationMethod" && attendanceType ? (
              <div className="flow-field flow-field--lg">
                <span>어떻게 돌아가세요?</span>
                <div className="option-cards" role="radiogroup" aria-label="아웃바운드 이동 방식">
                  {/* OWN_CAR isn't offered here: if your car isn't there, you can't drive it back */}
                  {getTransportationOptions(attendanceType)
                    .filter((method) => method !== "OWN_CAR")
                    .map((method) => (
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
                        }}
                      >
                        {getTransportationLabel(method)}
                      </button>
                    ))}
                </div>
                <input type="hidden" {...register("outboundTransportationMethod", { required: true })} />
              </div>
            ) : null}

            {/* Step: outboundCarpoolPreferredArea */}
            {currentSteps[step] === "outboundCarpoolPreferredArea" ? (
              outboundTransportation === "CARPOOL_NEEDED" ? (
                <label className="flow-field flow-field--lg">
                  <span>어느 동네가 편해요?</span>
                  <input
                    {...register("outboundCarpoolPreferredArea", {
                      required: "지역을 입력해주세요.",
                      maxLength: { value: 100, message: "100자 이하로 입력해주세요." }
                    })}
                    placeholder="예: 강남역, 삼성역 근처"
                    autoFocus
                  />
                  {formState.errors.outboundCarpoolPreferredArea && <span className="field-error">{formState.errors.outboundCarpoolPreferredArea.message}</span>}
                </label>
              ) : null
            ) : null}

            {/* Step: lodgingNight1 and lodgingNight2 */}
            {currentSteps[step] === "lodgingNight1" && attendanceType === "PARTIAL" ? (
              <div className="flow-field flow-field--lg">
                <span>숙박</span>
                <div className="check-chip-row">
                  <label className="check-chip">
                    <input {...register("lodgingNight1")} type="checkbox" />
                    <span>첫째 밤</span>
                  </label>
                  <label className="check-chip">
                    <input {...register("lodgingNight2")} type="checkbox" />
                    <span>둘째 밤</span>
                  </label>
                </div>
              </div>
            ) : null}

            {/* Step: attendance checklist */}
            {currentSteps[step] === "attendDay1Morning" && (attendanceType === "PARTIAL" || attendanceType === "WORSHIP_ONLY") ? (
              <div className="flow-field flow-field--lg">
                <span>참석 시간</span>
                {attendanceType === "WORSHIP_ONLY" ? (
                  <div className="checklist-worship-only">
                    <p className="checklist-note">집회 시간만 참석합니다</p>
                    <div className="check-grid">
                      <div>
                        <h3 className="checklist-day-label">Day 1</h3>
                        <div className="check-chip-row">
                          <label className="check-chip">
                            <input {...register("attendDay1Worship")} type="checkbox" disabled={true} checked={true} readOnly={true} />
                            <span className="disabled-text">집회</span>
                          </label>
                        </div>
                      </div>
                      <div>
                        <h3 className="checklist-day-label">Day 2</h3>
                        <div className="check-chip-row">
                          <label className="check-chip">
                            <input {...register("attendDay2Worship")} type="checkbox" disabled={true} checked={true} readOnly={true} />
                            <span className="disabled-text">집회</span>
                          </label>
                        </div>
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="check-grid">
                    <div>
                      <h3 className="checklist-day-label">Day 1</h3>
                      <div className="check-chip-row">
                        <label className="check-chip">
                          <input {...register("attendDay1Morning")} type="checkbox" />
                          <span>오전</span>
                        </label>
                        <label className="check-chip">
                          <input {...register("attendDay1Afternoon")} type="checkbox" />
                          <span>오후</span>
                        </label>
                        <label className="check-chip">
                          <input {...register("attendDay1Worship")} type="checkbox" />
                          <span>집회</span>
                        </label>
                      </div>
                    </div>
                    <div>
                      <h3 className="checklist-day-label">Day 2</h3>
                      <div className="check-chip-row">
                        <label className="check-chip">
                          <input {...register("attendDay2Morning")} type="checkbox" />
                          <span>오전</span>
                        </label>
                        <label className="check-chip">
                          <input {...register("attendDay2Afternoon")} type="checkbox" />
                          <span>오후</span>
                        </label>
                        <label className="check-chip">
                          <input {...register("attendDay2Worship")} type="checkbox" />
                          <span>집회</span>
                        </label>
                      </div>
                    </div>
                    <div>
                      <h3 className="checklist-day-label">Day 3</h3>
                      <div className="check-chip-row">
                        <label className="check-chip">
                          <input {...register("attendDay3Morning")} type="checkbox" />
                          <span>오전</span>
                        </label>
                        <label className="check-chip">
                          <input {...register("attendDay3Afternoon")} type="checkbox" />
                          <span>오후</span>
                        </label>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            ) : null}

            {/* Step: lookupKey (always needed, before privacy) */}
            {currentSteps[step] === "lookupKey" ? (
              <label className="flow-field flow-field--lg">
                <span>비밀번호 (숫자 6자리)</span>
                <input
                  {...register("lookupKey", { required: true, pattern: /^[0-9]{6}$/ })}
                  onChange={(e) => {
                    e.target.value = ValidationHelpers.filterNumeric(e.target.value).slice(0, 6);
                  }}
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
