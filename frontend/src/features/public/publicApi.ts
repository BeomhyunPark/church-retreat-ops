import { apiRequest } from "../../shared/api/client";

export type AttendanceType = "FULL" | "PARTIAL";
export type AttendanceSlot =
  | "DAY1_MORNING"
  | "DAY1_AFTERNOON"
  | "DAY1_GATHERING"
  | "DAY2_MORNING"
  | "DAY2_AFTERNOON"
  | "DAY2_GATHERING"
  | "DAY3_MORNING"
  | "DAY3_AFTERNOON"
  | "DAY3_GATHERING";
export type TransportationType = "OWN_CAR" | "PUBLIC_TRANSPORT" | "UNDECIDED";

export type RegistrationCreatePayload = {
  name: string;
  gender: "MALE" | "FEMALE";
  birthYear: number;
  phoneNumber: string;
  churchCellDepartment?: string;
  attendanceType: AttendanceType;
  attendanceSlots: AttendanceSlot[];
  transportationType: TransportationType;
  carpoolNeeded: boolean;
  carpoolOffer: boolean;
  carpoolSeats?: number | null;
  transportationNote?: string;
  privacyConsentAgreed: boolean;
};

export type RegistrationCreateResponse = {
  registrationId: number;
  lookupKey: string;
};

export type RegistrationSelfLookupPayload = {
  name: string;
  phoneLastFour: string;
  lookupKey: string;
};

export type RegistrationResponse = {
  id: number;
  name: string;
  gender: "MALE" | "FEMALE";
  birthYear: number;
  phoneNumber: string;
  churchCellDepartment?: string | null;
  attendanceType: AttendanceType;
  attendanceSlots: AttendanceSlot[];
  transportationType: TransportationType;
  carpoolNeeded: boolean;
  carpoolOffer: boolean;
  carpoolSeats?: number | null;
  transportationNote?: string | null;
  feePaid: boolean;
  status: string;
};

export function createRegistration(payload: RegistrationCreatePayload) {
  return apiRequest<RegistrationCreateResponse>("/registrations", {
    method: "POST",
    body: payload
  });
}

export function lookupRegistration(payload: RegistrationSelfLookupPayload) {
  return apiRequest<RegistrationResponse>("/registrations/self/lookup", {
    method: "POST",
    body: payload
  });
}
