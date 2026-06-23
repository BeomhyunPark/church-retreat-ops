import { useMemo, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import {
  createRegistration,
  type AttendanceSlot,
  type AttendanceType,
  type RegistrationCreatePayload,
  type TransportationType
} from "./publicApi";
import { StatusMessage } from "../../shared/ui/StatusMessage";

type FormValues = Omit<RegistrationCreatePayload, "birthYear" | "carpoolSeats" | "privacyConsentAgreed"> & {
  birthYear: string;
  carpoolSeats: string;
  privacyConsentAgreed: boolean;
};

type StepId =
  | "name"
  | "gender"
  | "birthYear"
  | "phoneNumber"
  | "churchCellDepartment"
  | "attendanceType"
  | "attendanceSlots"
  | "transportationType"
  | "carpoolNeeded"
  | "carpoolOffer"
  | "carpoolSeats"
  | "transportationNote"
  | "privacyConsentAgreed";

const initialValues: FormValues = {
  name: "",
  gender: "FEMALE",
  birthYear: "",
  phoneNumber: "",
  churchCellDepartment: "",
  attendanceType: "FULL",
  attendanceSlots: [],
  transportationType: "UNDECIDED",
  carpoolNeeded: false,
  carpoolOffer: false,
  carpoolSeats: "",
  transportationNote: "",
  privacyConsentAgreed: false
};

const attendanceSlotOptions: Array<{ value: AttendanceSlot; label: string }> = [
  { value: "DAY1_MORNING", label: "첫째날 오전" },
  { value: "DAY1_AFTERNOON", label: "첫째날 오후" },
  { value: "DAY1_GATHERING", label: "첫째날 집회" },
  { value: "DAY2_MORNING", label: "둘째날 오전" },
  { value: "DAY2_AFTERNOON", label: "둘째날 오후" },
  { value: "DAY2_GATHERING", label: "둘째날 집회" },
  { value: "DAY3_MORNING", label: "셋째날 오전" },
  { value: "DAY3_AFTERNOON", label: "셋째날 오후" },
  { value: "DAY3_GATHERING", label: "셋째날 집회" }
];

const attendanceTypeLabels: Record<AttendanceType, string> = {
  FULL: "전체참석",
  PARTIAL: "부분참석"
};

const transportationTypeLabels: Record<TransportationType, string> = {
  OWN_CAR: "자차",
  PUBLIC_TRANSPORT: "대중교통",
  UNDECIDED: "아직 미정"
};

export function PublicRegisterPage() {
  const [lookupKey, setLookupKey] = useState<string | null>(null);
  const [stepIndex, setStepIndex] = useState(0);
  const [values, setValues] = useState<FormValues>(initialValues);
  const steps = useMemo(() => buildSteps(values), [values]);
  const currentStep = steps[Math.min(stepIndex, steps.length - 1)];
  const isLastStep = stepIndex === steps.length - 1;
  const progress = `${stepIndex + 1}/${steps.length}`;
  const answeredSteps = steps.slice(0, stepIndex);

  const mutation = useMutation({
    mutationFn: createRegistration,
    onSuccess: (data) => setLookupKey(data.lookupKey)
  });

  function updateValue<Key extends keyof FormValues>(key: Key, value: FormValues[Key]) {
    setValues((current) => ({ ...current, [key]: value }));
  }

  function toggleAttendanceSlot(slot: AttendanceSlot) {
    setValues((current) => ({
      ...current,
      attendanceSlots: current.attendanceSlots.includes(slot)
        ? current.attendanceSlots.filter((item) => item !== slot)
        : [...current.attendanceSlots, slot]
    }));
  }

  function isStepReady(step: StepId) {
    if (step === "churchCellDepartment" || step === "transportationNote") {
      return true;
    }

    if (step === "privacyConsentAgreed") {
      return values.privacyConsentAgreed;
    }

    if (step === "birthYear") {
      const digits = values.birthYear.replace(/\D/g, "");
      return digits.length === 2 || digits.length === 4;
    }

    if (step === "phoneNumber") {
      return values.phoneNumber.replace(/\D/g, "").length >= 10;
    }

    if (step === "attendanceSlots") {
      return values.attendanceSlots.length > 0;
    }

    if (step === "carpoolSeats") {
      return Number(values.carpoolSeats) > 0;
    }

    return String(values[step]).trim().length > 0;
  }

  function goNext() {
    if (!isStepReady(currentStep)) {
      return;
    }

    setStepIndex((current) => Math.min(current + 1, steps.length - 1));
  }

  function goBack() {
    setStepIndex((current) => Math.max(current - 1, 0));
  }

  function onSubmit() {
    mutation.mutate({
      ...values,
      birthYear: normalizeBirthYear(values.birthYear),
      attendanceSlots: values.attendanceType === "PARTIAL" ? values.attendanceSlots : [],
      carpoolOffer: values.transportationType === "OWN_CAR" && values.carpoolOffer,
      carpoolNeeded: values.transportationType !== "OWN_CAR" && values.carpoolNeeded,
      carpoolSeats: values.transportationType === "OWN_CAR" && values.carpoolOffer ? Number(values.carpoolSeats) : null,
      privacyConsentAgreed: values.privacyConsentAgreed
    });
  }

  function handlePrimaryAction() {
    if (isLastStep) {
      onSubmit();
      return;
    }

    goNext();
  }

  return (
    <section className="register-flow">
      {lookupKey ? (
        <div className="register-complete" role="status">
          <p className="eyebrow">Done</p>
          <h1>신청 완료</h1>
          <p>이 키는 다시 안 보여요. 지금 저장해두세요.</p>
          <div className="lookup-key-box">
            <span>조회 키</span>
            <strong>{lookupKey}</strong>
          </div>
          <div className="completion-actions">
            <Link className="button button--primary" to="/public/self-lookup">
              내 정보 보기
            </Link>
            <Link className="button button--secondary" to="/public">
              홈
            </Link>
          </div>
        </div>
      ) : (
        <>
          <div className="register-flow__top">
            <button className="register-flow__back" disabled={stepIndex === 0 || mutation.isPending} onClick={goBack} type="button">
              이전
            </button>
            <span>{progress}</span>
          </div>

          <div className="register-progress" aria-hidden="true">
            <span style={{ width: `${((stepIndex + 1) / steps.length) * 100}%` }} />
          </div>

          {answeredSteps.length ? (
            <div className="answer-stack" aria-label="입력한 내용">
              {answeredSteps.map((step) => (
                <button className="answer-chip" key={step} onClick={() => setStepIndex(steps.indexOf(step))} type="button">
                  <span>{getStepLabel(step)}</span>
                  <strong>{formatStepValue(step, values)}</strong>
                </button>
              ))}
            </div>
          ) : null}

          <div className="register-step" key={currentStep}>
            {currentStep === "name" ? (
              <>
                <p className="eyebrow">Start</p>
                <h1>이름</h1>
                <input
                  autoComplete="name"
                  autoFocus
                  className="register-step__input"
                  enterKeyHint="next"
                  onChange={(event) => updateValue("name", event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter") goNext();
                  }}
                  placeholder="유성현"
                  value={values.name}
                />
              </>
            ) : null}

            {currentStep === "gender" ? (
              <>
                <p className="eyebrow">Gender</p>
                <h1>성별</h1>
                <div className="choice-grid">
                  <ChoiceButton selected={values.gender === "FEMALE"} onClick={() => updateValue("gender", "FEMALE")}>
                    여성
                  </ChoiceButton>
                  <ChoiceButton selected={values.gender === "MALE"} onClick={() => updateValue("gender", "MALE")}>
                    남성
                  </ChoiceButton>
                </div>
              </>
            ) : null}

            {currentStep === "birthYear" ? (
              <>
                <p className="eyebrow">Age</p>
                <h1>또래</h1>
                <input
                  autoFocus
                  className="register-step__input"
                  enterKeyHint="next"
                  inputMode="numeric"
                  maxLength={4}
                  onChange={(event) => updateValue("birthYear", event.target.value.replace(/\D/g, ""))}
                  onKeyDown={(event) => {
                    if (event.key === "Enter") goNext();
                  }}
                  placeholder="99"
                  value={values.birthYear}
                />
              </>
            ) : null}

            {currentStep === "phoneNumber" ? (
              <>
                <p className="eyebrow">Phone</p>
                <h1>연락처</h1>
                <input
                  autoFocus
                  autoComplete="tel"
                  className="register-step__input"
                  enterKeyHint="next"
                  inputMode="tel"
                  onChange={(event) => updateValue("phoneNumber", event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter") goNext();
                  }}
                  placeholder="010-1234-5678"
                  value={values.phoneNumber}
                />
              </>
            ) : null}

            {currentStep === "churchCellDepartment" ? (
              <>
                <p className="eyebrow">Cell</p>
                <h1>셀</h1>
                <p className="register-step__hint">모르면 비워두고 넘어가도 돼요.</p>
                <input
                  autoFocus
                  className="register-step__input"
                  enterKeyHint="next"
                  onChange={(event) => updateValue("churchCellDepartment", event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter") goNext();
                  }}
                  placeholder="OOO"
                  value={values.churchCellDepartment}
                />
              </>
            ) : null}

            {currentStep === "attendanceType" ? (
              <>
                <p className="eyebrow">Stay</p>
                <h1>참석 형태</h1>
                <div className="choice-grid">
                  <ChoiceButton selected={values.attendanceType === "FULL"} onClick={() => updateValue("attendanceType", "FULL")}>
                    전체참석
                  </ChoiceButton>
                  <ChoiceButton selected={values.attendanceType === "PARTIAL"} onClick={() => updateValue("attendanceType", "PARTIAL")}>
                    부분참석
                  </ChoiceButton>
                </div>
              </>
            ) : null}

            {currentStep === "attendanceSlots" ? (
              <>
                <p className="eyebrow">When</p>
                <h1>언제 참석해요?</h1>
                <div className="slot-grid">
                  {attendanceSlotOptions.map((option) => (
                    <button
                      className={values.attendanceSlots.includes(option.value) ? "slot-chip slot-chip--selected" : "slot-chip"}
                      key={option.value}
                      onClick={() => toggleAttendanceSlot(option.value)}
                      type="button"
                    >
                      {option.label}
                    </button>
                  ))}
                </div>
              </>
            ) : null}

            {currentStep === "transportationType" ? (
              <>
                <p className="eyebrow">Move</p>
                <h1>어떻게 와요?</h1>
                <div className="choice-grid choice-grid--stack">
                  <ChoiceButton selected={values.transportationType === "OWN_CAR"} onClick={() => updateValue("transportationType", "OWN_CAR")}>
                    자차
                  </ChoiceButton>
                  <ChoiceButton
                    selected={values.transportationType === "PUBLIC_TRANSPORT"}
                    onClick={() => updateValue("transportationType", "PUBLIC_TRANSPORT")}
                  >
                    대중교통
                  </ChoiceButton>
                  <ChoiceButton selected={values.transportationType === "UNDECIDED"} onClick={() => updateValue("transportationType", "UNDECIDED")}>
                    아직 미정
                  </ChoiceButton>
                </div>
              </>
            ) : null}

            {currentStep === "carpoolNeeded" ? (
              <>
                <p className="eyebrow">Carpool</p>
                <h1>카풀 필요해요?</h1>
                <div className="choice-grid">
                  <ChoiceButton selected={values.carpoolNeeded} onClick={() => updateValue("carpoolNeeded", true)}>
                    필요해요
                  </ChoiceButton>
                  <ChoiceButton selected={!values.carpoolNeeded} onClick={() => updateValue("carpoolNeeded", false)}>
                    괜찮아요
                  </ChoiceButton>
                </div>
              </>
            ) : null}

            {currentStep === "carpoolOffer" ? (
              <>
                <p className="eyebrow">Carpool</p>
                <h1>태워줄 수 있어요?</h1>
                <div className="choice-grid">
                  <ChoiceButton selected={values.carpoolOffer} onClick={() => updateValue("carpoolOffer", true)}>
                    가능해요
                  </ChoiceButton>
                  <ChoiceButton selected={!values.carpoolOffer} onClick={() => updateValue("carpoolOffer", false)}>
                    어려워요
                  </ChoiceButton>
                </div>
              </>
            ) : null}

            {currentStep === "carpoolSeats" ? (
              <>
                <p className="eyebrow">Seats</p>
                <h1>몇 명 가능해요?</h1>
                <input
                  autoFocus
                  className="register-step__input"
                  enterKeyHint="next"
                  inputMode="numeric"
                  maxLength={2}
                  onChange={(event) => updateValue("carpoolSeats", event.target.value.replace(/\D/g, ""))}
                  onKeyDown={(event) => {
                    if (event.key === "Enter") goNext();
                  }}
                  placeholder="3"
                  value={values.carpoolSeats}
                />
              </>
            ) : null}

            {currentStep === "transportationNote" ? (
              <>
                <p className="eyebrow">Memo</p>
                <h1>출발 위치</h1>
                <p className="register-step__hint">카풀이나 픽업 참고용이에요. 비워도 돼요.</p>
                <input
                  autoFocus
                  className="register-step__input"
                  enterKeyHint="next"
                  onChange={(event) => updateValue("transportationNote", event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter") goNext();
                  }}
                  placeholder="교회, OO역 근처"
                  value={values.transportationNote}
                />
              </>
            ) : null}

            {currentStep === "privacyConsentAgreed" ? (
              <>
                <p className="eyebrow">Last</p>
                <h1>마지막이에요</h1>
                <button
                  className={values.privacyConsentAgreed ? "consent-card consent-card--checked" : "consent-card"}
                  onClick={() => updateValue("privacyConsentAgreed", !values.privacyConsentAgreed)}
                  type="button"
                >
                  <strong>개인정보 수집 및 이용 동의</strong>
                  <span>수련회 등록과 연락을 위해 사용할게요.</span>
                </button>
              </>
            ) : null}
          </div>

          {mutation.isError ? <StatusMessage message={mutation.error.message} tone="error" /> : null}

          <button
            className="register-flow__primary"
            disabled={!isStepReady(currentStep) || mutation.isPending}
            onClick={handlePrimaryAction}
            type="button"
          >
            {mutation.isPending ? "신청 중..." : isLastStep ? "신청 완료하기" : "다음"}
          </button>
        </>
      )}
    </section>
  );
}

function ChoiceButton({ children, selected, onClick }: { children: string; selected: boolean; onClick: () => void }) {
  return (
    <button className={selected ? "choice-card choice-card--selected" : "choice-card"} onClick={onClick} type="button">
      {children}
    </button>
  );
}

function buildSteps(values: FormValues): StepId[] {
  const steps: StepId[] = ["name", "gender", "birthYear", "phoneNumber", "churchCellDepartment", "attendanceType"];

  if (values.attendanceType === "PARTIAL") {
    steps.push("attendanceSlots");
  }

  steps.push("transportationType");

  if (values.transportationType === "OWN_CAR") {
    steps.push("carpoolOffer");
    if (values.carpoolOffer) {
      steps.push("carpoolSeats");
    }
  } else {
    steps.push("carpoolNeeded");
  }

  steps.push("transportationNote", "privacyConsentAgreed");
  return steps;
}

function formatStepValue(step: StepId, values: FormValues) {
  if (step === "gender") {
    return values.gender === "FEMALE" ? "여성" : "남성";
  }

  if (step === "attendanceType") {
    return attendanceTypeLabels[values.attendanceType];
  }

  if (step === "attendanceSlots") {
    return values.attendanceSlots.map((slot) => attendanceSlotOptions.find((option) => option.value === slot)?.label).join(", ");
  }

  if (step === "transportationType") {
    return transportationTypeLabels[values.transportationType];
  }

  if (step === "carpoolNeeded") {
    return values.carpoolNeeded ? "희망" : "필요 없음";
  }

  if (step === "carpoolOffer") {
    return values.carpoolOffer ? "가능" : "어려움";
  }

  if (step === "carpoolSeats") {
    return `${values.carpoolSeats}명`;
  }

  if (step === "privacyConsentAgreed") {
    return values.privacyConsentAgreed ? "동의 완료" : "";
  }

  if (step === "churchCellDepartment") {
    return values.churchCellDepartment?.trim() || "나중에 확인";
  }

  if (step === "transportationNote") {
    return values.transportationNote?.trim() || "없음";
  }

  return String(values[step]).trim();
}

function getStepLabel(step: StepId) {
  const labels: Record<StepId, string> = {
    name: "이름",
    gender: "성별",
    birthYear: "또래",
    phoneNumber: "연락처",
    churchCellDepartment: "셀",
    attendanceType: "참석",
    attendanceSlots: "시간",
    transportationType: "이동",
    carpoolNeeded: "카풀",
    carpoolOffer: "카풀",
    carpoolSeats: "좌석",
    transportationNote: "출발",
    privacyConsentAgreed: "동의"
  };

  return labels[step];
}

function normalizeBirthYear(value: string) {
  const digits = value.replace(/\D/g, "");

  if (digits.length === 2) {
    const year = Number(digits);
    return year <= 15 ? 2000 + year : 1900 + year;
  }

  return Number(digits);
}
