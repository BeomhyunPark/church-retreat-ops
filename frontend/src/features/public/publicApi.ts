import { apiRequest } from "../../shared/api/client";

export type RegistrationCreatePayload = {
  name: string;
  gender: "MALE" | "FEMALE";
  birthYear: number;
  phoneNumber: string;
  churchCellDepartment?: string;
  privacyConsentAgreed: boolean;
  lookupKey: string;
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
