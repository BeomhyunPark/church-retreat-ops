import { apiRequest } from "../../shared/api/client";

export type TransportationMethod = "OWN_CAR" | "GROUP_BUS" | "WORSHIP_SHUTTLE" | "PUBLIC_TRANSIT" | "CARPOOL_NEEDED" | "NOT_DECIDED";

export type RegistrationCreatePayload = {
  name: string;
  gender: "MALE" | "FEMALE";
  birthYear: number;
  phoneNumber: string;
  churchCellDepartment?: string;
  privacyConsentAgreed: boolean;
  lookupKey: string;
  attendanceType: "FULL" | "PARTIAL" | "WORSHIP_ONLY";
  inboundTransportationMethod: TransportationMethod;
  outboundTransportationMethod: TransportationMethod;
  inboundCarpoolAvailable?: boolean;
  inboundCarpoolSeats?: number;
  inboundCarpoolArea?: string;
  inboundCarpoolPreferredArea?: string;
  outboundCarpoolAvailable?: boolean;
  outboundCarpoolSeats?: number;
  outboundCarpoolArea?: string;
  outboundCarpoolPreferredArea?: string;
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
    inboundTransportationMethod: TransportationMethod;
    outboundTransportationMethod: TransportationMethod;
    inboundCarpoolAvailable?: boolean;
    inboundCarpoolSeats?: number;
    inboundCarpoolArea?: string;
    inboundCarpoolPreferredArea?: string;
    outboundCarpoolAvailable?: boolean;
    outboundCarpoolSeats?: number;
    outboundCarpoolArea?: string;
    outboundCarpoolPreferredArea?: string;
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
