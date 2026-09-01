import type { RegistrationCreatePayload, TransportationMethod, WorshipBusRideSlot } from "./publicApi";

export type AttendanceType = "FULL" | "PARTIAL" | "WORSHIP_ONLY";

export type RegisterFormValues = {
  name: string;
  gender: "MALE" | "FEMALE";
  birthYear: number;
  phoneNumber: string;
  middleGroupName?: string;
  cellName?: string;
  privacyConsentAgreed: boolean;
  lookupKey: string;
  attendanceType: AttendanceType;
  plannedArrivalAt?: string;
  plannedDepartureAt?: string;
  partialAttendanceNote?: string;
  lodgingNight1?: boolean;
  lodgingNight2?: boolean;
  selectedOptionIds: number[];
  inboundTransportationMethod: TransportationMethod;
  outboundTransportationMethod: TransportationMethod;
  inboundCarpoolAvailable?: boolean;
  inboundCarpoolSeats?: number;
  inboundCarpoolArea?: string;
  inboundCarpoolRouteArea?: string;
  inboundCarpoolNote?: string;
  inboundCarpoolPreferredArea?: string;
  inboundCarpoolPreferredNote?: string;
  inboundWorshipBusRideSlot?: WorshipBusRideSlot;
  outboundCarpoolArea?: string;
  outboundCarpoolRouteArea?: string;
  outboundCarpoolNote?: string;
  outboundCarpoolAvailable?: boolean;
  outboundCarpoolSeats?: number;
  outboundCarpoolPreferredArea?: string;
  outboundCarpoolPreferredNote?: string;
  outboundWorshipBusRideSlot?: WorshipBusRideSlot;
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
    return ["GROUP_BUS", "OWN_CAR", "PUBLIC_TRANSIT", "CARPOOL_NEEDED"];
  }
  if (attendanceType === "WORSHIP_ONLY") {
    return ["WORSHIP_SHUTTLE", "OWN_CAR", "PUBLIC_TRANSIT", "CARPOOL_NEEDED"];
  }
  return ["GROUP_BUS", "WORSHIP_SHUTTLE", "OWN_CAR", "PUBLIC_TRANSIT", "CARPOOL_NEEDED"];
}

export function getTransportationLabel(method: TransportationMethod): string {
  const labels: Record<TransportationMethod, string> = {
    OWN_CAR: "자차",
    GROUP_BUS: "단체 이동 차량",
    WORSHIP_SHUTTLE: "집회 차량",
    PUBLIC_TRANSIT: "대중교통",
    CARPOOL_NEEDED: "이동 지원 요청",
    NOT_DECIDED: "미정"
  };
  return labels[method];
}

export function getWorshipBusRideSlotLabel(slot: WorshipBusRideSlot): string {
  const labels: Record<WorshipBusRideSlot, string> = {
    DAY1_BEFORE_WORSHIP: "첫째 날 집회 전 교회 -> 수련회장",
    DAY1_AFTER_WORSHIP: "첫째 날 집회 후 수련회장 -> 교회",
    DAY2_BEFORE_WORSHIP: "둘째 날 집회 전 교회 -> 수련회장",
    DAY2_AFTER_WORSHIP: "둘째 날 집회 후 수련회장 -> 교회"
  };
  return labels[slot];
}

export const inboundWorshipBusRideSlots: WorshipBusRideSlot[] = ["DAY1_BEFORE_WORSHIP", "DAY2_BEFORE_WORSHIP"];
export const outboundWorshipBusRideSlots: WorshipBusRideSlot[] = ["DAY1_AFTER_WORSHIP", "DAY2_AFTER_WORSHIP"];

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
  outboundTransportation?: TransportationMethod,
  outboundCarpoolAvailable?: boolean
): RegisterStep[] {
  const steps: RegisterStep[] = [
    "name",
    "gender",
    "birthYear",
    "phoneNumber",
    "middleGroupName",
    "cellName",
    "attendanceType"
  ];

  if (attendanceType === "FULL") {
    steps.push("inboundTransportationMethod");
    if (inboundTransportation === "OWN_CAR") {
      steps.push("inboundCarpoolAvailable");
      if (inboundCarpoolAvailable === true) {
        steps.push("inboundCarpoolSeats", "inboundCarpoolArea", "inboundCarpoolRouteArea", "inboundCarpoolNote");
      }
      steps.push("outboundCarpoolAvailable");
      if (outboundCarpoolAvailable === true) {
        steps.push("outboundCarpoolSeats", "outboundCarpoolArea", "outboundCarpoolRouteArea", "outboundCarpoolNote");
      }
    } else if (inboundTransportation === "CARPOOL_NEEDED") {
      steps.push("inboundCarpoolPreferredArea", "inboundCarpoolPreferredNote", "outboundTransportationMethod");
    } else {
      steps.push("outboundTransportationMethod");
    }
    if (inboundTransportation !== "OWN_CAR" && outboundTransportation === "CARPOOL_NEEDED") {
      steps.push("outboundCarpoolPreferredArea", "outboundCarpoolPreferredNote");
    }
  } else if (attendanceType === "PARTIAL") {
    steps.push("plannedArrivalAt", "plannedDepartureAt", "partialAttendanceNote", "lodgingNight1", "selectedOptionIds");
    steps.push("inboundTransportationMethod");
    if (inboundTransportation === "OWN_CAR") {
      steps.push("inboundCarpoolAvailable");
      if (inboundCarpoolAvailable === true) {
        steps.push("inboundCarpoolSeats", "inboundCarpoolArea", "inboundCarpoolRouteArea", "inboundCarpoolNote");
      }
      steps.push("outboundCarpoolAvailable");
      if (outboundCarpoolAvailable === true) {
        steps.push("outboundCarpoolSeats", "outboundCarpoolArea", "outboundCarpoolRouteArea", "outboundCarpoolNote");
      }
    } else if (inboundTransportation === "CARPOOL_NEEDED") {
      steps.push("inboundCarpoolPreferredArea", "inboundCarpoolPreferredNote", "outboundTransportationMethod");
    } else {
      if (inboundTransportation === "WORSHIP_SHUTTLE") {
        steps.push("inboundWorshipBusRideSlot");
      }
      steps.push("outboundTransportationMethod");
    }
    if (inboundTransportation !== "OWN_CAR") {
      if (outboundTransportation === "WORSHIP_SHUTTLE") {
        steps.push("outboundWorshipBusRideSlot");
      }
      if (outboundTransportation === "CARPOOL_NEEDED") {
        steps.push("outboundCarpoolPreferredArea", "outboundCarpoolPreferredNote");
      }
    }
  } else if (attendanceType === "WORSHIP_ONLY") {
    steps.push("selectedOptionIds", "inboundTransportationMethod");
    if (inboundTransportation === "OWN_CAR") {
      steps.push("inboundCarpoolAvailable");
      if (inboundCarpoolAvailable === true) {
        steps.push("inboundCarpoolSeats", "inboundCarpoolArea", "inboundCarpoolRouteArea", "inboundCarpoolNote");
      }
      steps.push("outboundCarpoolAvailable");
      if (outboundCarpoolAvailable === true) {
        steps.push("outboundCarpoolSeats", "outboundCarpoolArea", "outboundCarpoolRouteArea", "outboundCarpoolNote");
      }
    } else if (inboundTransportation === "CARPOOL_NEEDED") {
      steps.push("inboundCarpoolPreferredArea", "inboundCarpoolPreferredNote", "outboundTransportationMethod");
    } else if (inboundTransportation) {
      if (inboundTransportation === "WORSHIP_SHUTTLE") {
        steps.push("inboundWorshipBusRideSlot");
      }
      steps.push("outboundTransportationMethod");
      if (outboundTransportation === "WORSHIP_SHUTTLE") {
        steps.push("outboundWorshipBusRideSlot");
      }
      if (outboundTransportation === "CARPOOL_NEEDED") {
        steps.push("outboundCarpoolPreferredArea", "outboundCarpoolPreferredNote");
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
    payload.outboundTransportationMethod = "OWN_CAR";
  }

  for (const direction of ["inbound", "outbound"] as const) {
    const method = payload[`${direction}TransportationMethod`];
    if (method !== "OWN_CAR") {
      delete payload[`${direction}CarpoolAvailable`];
      delete payload[`${direction}CarpoolSeats`];
      delete payload[`${direction}CarpoolArea`];
      delete payload[`${direction}CarpoolRouteArea`];
      delete payload[`${direction}CarpoolNote`];
    }
    if (method !== "CARPOOL_NEEDED") {
      delete payload[`${direction}CarpoolPreferredArea`];
      delete payload[`${direction}CarpoolPreferredNote`];
    }
    if (method !== "WORSHIP_SHUTTLE") {
      delete payload[`${direction}WorshipBusRideSlot`];
    }
  }

  if (values.attendanceType === "FULL") {
    delete payload.lodgingNight1;
    delete payload.lodgingNight2;
    payload.selectedOptionIds = [];
  }

  if (values.attendanceType !== "PARTIAL") {
    delete payload.plannedArrivalAt;
    delete payload.plannedDepartureAt;
    delete payload.partialAttendanceNote;
  } else {
    payload.plannedArrivalAt = toOffsetDateTime(values.plannedArrivalAt);
    payload.plannedDepartureAt = toOffsetDateTime(values.plannedDepartureAt);
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

function toOffsetDateTime(value?: string) {
  if (!value) {
    return value;
  }
  return new Date(value).toISOString();
}
