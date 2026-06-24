import { apiRequest } from "../../shared/api/client";

export type RegistrationCreatePayload = {
  name: string;
  gender: "MALE" | "FEMALE";
  birthYear: number;
  phoneNumber: string;
  churchCellDepartment?: string;
  privacyConsentAgreed: boolean;
  lookupKey: string;
  attendanceType: "FULL" | "PARTIAL" | "WORSHIP_ONLY";
  transportation: "OWN_CAR" | "BUS" | "PUBLIC_TRANSIT" | "RIDE_NEEDED";
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

export type RegistrationResponse = {
  id: number;
  name: string;
  gender: "MALE" | "FEMALE";
  birthYear: number;
  phoneNumber: string;
  churchCellDepartment?: string | null;
  feePaid: boolean;
  status: string;
};

export type RegistrationCreateResponse = {
  resultType: "CREATED" | "OVERWRITTEN";
  registration: RegistrationResponse;
};

export type RegistrationSelfLookupPayload = {
  name: string;
  lookupKey: string;
};

export type RegistrationSelfUpdatePayload = {
  name: string;
  phoneLastFour: string;
  lookupKey: string;
  update: {
    gender: "MALE" | "FEMALE";
    birthYear: number;
    phoneNumber: string;
    churchCellDepartment?: string;
    attendanceType: "FULL" | "PARTIAL" | "WORSHIP_ONLY";
    transportation: "OWN_CAR" | "BUS" | "PUBLIC_TRANSIT" | "RIDE_NEEDED";
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

export function selfUpdateRegistration(payload: RegistrationSelfUpdatePayload) {
  return apiRequest<RegistrationResponse>("/registrations/self", {
    method: "PUT",
    body: payload
  });
}
