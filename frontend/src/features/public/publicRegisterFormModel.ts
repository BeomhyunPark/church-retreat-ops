import type { RegistrationCreatePayload, TransportationMethod } from "./publicApi";

export type AttendanceType = "FULL" | "PARTIAL" | "WORSHIP_ONLY";

export type RegisterFormValues = {
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
  outboundCarpoolArea?: string;
  outboundCarpoolPreferredArea?: string;
};

export type RegisterStep = keyof RegisterFormValues;

const textOnlyDisallowedPattern = new RegExp("[\\d!@#$%^&*()+=\\[\\]{};:'\"<>,.?/\\\\|`~\\s-]", "g");

export const validationHelpers = {
  filterNumeric: (value: string): string => value.replace(/\D/g, ""),

  filterTextOnly: (value: string): string => value.replace(textOnlyDisallowedPattern, ""),

  normalizeWhitespace: (value: string): string => value.replace(/\s+/g, " ").trim()
};

export function formatPhoneNumber(value: string) {
  const digits = value.replace(/\D/g, "").slice(0, 11);
  if (digits.length <= 3) {
    return digits;
  }
  if (digits.length <= 7) {
    return `${digits.slice(0, 3)}-${digits.slice(3)}`;
  }
  return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`;
}

export function getTransportationOptions(attendanceType: AttendanceType): TransportationMethod[] {
  if (attendanceType === "FULL") {
    return ["GROUP_BUS", "OWN_CAR"];
  }
  return ["GROUP_BUS", "WORSHIP_SHUTTLE", "OWN_CAR", "PUBLIC_TRANSIT", "CARPOOL_NEEDED", "NOT_DECIDED"];
}

export function getTransportationLabel(method: TransportationMethod): string {
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

export function getAttendanceLabel(type: AttendanceType): string {
  const labels: Record<AttendanceType, string> = {
    FULL: "전체 참석",
    PARTIAL: "부분 참석",
    WORSHIP_ONLY: "집회만"
  };
  return labels[type];
}

export function buildRegisterSteps(
  attendanceType?: AttendanceType,
  inboundTransportation?: TransportationMethod,
  inboundCarpoolAvailable?: boolean,
  outboundTransportation?: TransportationMethod
): RegisterStep[] {
  const steps: RegisterStep[] = [
    "name",
    "gender",
    "birthYear",
    "phoneNumber",
    "churchCellDepartment",
    "attendanceType"
  ];

  if (attendanceType === "FULL") {
    steps.push("inboundTransportationMethod");
    if (inboundTransportation === "OWN_CAR") {
      steps.push("inboundCarpoolAvailable");
      if (inboundCarpoolAvailable === true) {
        steps.push("inboundCarpoolSeats", "inboundCarpoolArea");
      }
    } else {
      steps.push("outboundTransportationMethod");
    }
  } else if (attendanceType === "PARTIAL") {
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

  steps.push("lookupKey", "privacyConsentAgreed");
  return steps;
}

export function buildRegistrationPayload(values: RegisterFormValues): RegistrationCreatePayload {
  const payload: RegistrationCreatePayload = {
    ...values,
    birthYear: expandTwoDigitBirthYear(values.birthYear),
    privacyConsentAgreed: values.privacyConsentAgreed
  };

  if (values.inboundTransportationMethod === "OWN_CAR") {
    payload.outboundCarpoolAvailable = values.inboundCarpoolAvailable;
    payload.outboundCarpoolSeats = values.inboundCarpoolSeats;
    payload.outboundCarpoolArea = values.inboundCarpoolArea;
  }

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

  return payload;
}

function expandTwoDigitBirthYear(birthYear: number) {
  const currentTwoDigitYear = new Date().getFullYear() % 100;
  const enteredTwoDigitYear = Number(birthYear);
  return enteredTwoDigitYear <= currentTwoDigitYear
    ? 2000 + enteredTwoDigitYear
    : 1900 + enteredTwoDigitYear;
}
