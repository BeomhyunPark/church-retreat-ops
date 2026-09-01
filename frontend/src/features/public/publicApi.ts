import { apiRequest } from "../../shared/api/client";

export type TransportationMethod = "OWN_CAR" | "GROUP_BUS" | "WORSHIP_SHUTTLE" | "PUBLIC_TRANSIT" | "CARPOOL_NEEDED" | "NOT_DECIDED";
export type WorshipBusRideSlot = "DAY1_BEFORE_WORSHIP" | "DAY1_AFTER_WORSHIP" | "DAY2_BEFORE_WORSHIP" | "DAY2_AFTER_WORSHIP";
export type ParticipationOption = {
  id: number;
  optionType: "PROGRAM" | "MEAL";
  label: string;
  eventDate: string;
  displayOrder: number;
};

export type RegistrationCreatePayload = {
  name: string;
  gender: "MALE" | "FEMALE";
  birthYear: number;
  phoneNumber: string;
  middleGroupName?: string;
  cellName?: string;
  privacyConsentAgreed: boolean;
  lookupKey: string;
  attendanceType: "FULL" | "PARTIAL" | "WORSHIP_ONLY";
  plannedArrivalAt?: string;
  plannedDepartureAt?: string;
  partialAttendanceNote?: string;
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
  outboundCarpoolAvailable?: boolean;
  outboundCarpoolSeats?: number;
  outboundCarpoolArea?: string;
  outboundCarpoolRouteArea?: string;
  outboundCarpoolNote?: string;
  outboundCarpoolPreferredArea?: string;
  outboundCarpoolPreferredNote?: string;
  outboundWorshipBusRideSlot?: WorshipBusRideSlot;
  lodgingNight1?: boolean;
  lodgingNight2?: boolean;
  selectedOptionIds: number[];
};

export type RegistrationResponse = {
  id: number;
  name: string;
  gender: "MALE" | "FEMALE";
  birthYear: number;
  phoneNumber: string;
  middleGroupName?: string | null;
  cellName?: string | null;
  feePaid: boolean;
  status: string;
  attendanceType: "FULL" | "PARTIAL" | "WORSHIP_ONLY";
  plannedArrivalAt?: string | null;
  plannedDepartureAt?: string | null;
  partialAttendanceNote?: string | null;
  selectedOptionIds: number[];
  inboundTransportationMethod: TransportationMethod;
  outboundTransportationMethod: TransportationMethod;
  inboundCarpoolAvailable?: boolean | null;
  inboundCarpoolSeats?: number | null;
  inboundCarpoolArea?: string | null;
  inboundCarpoolRouteArea?: string | null;
  inboundCarpoolNote?: string | null;
  inboundCarpoolPreferredArea?: string | null;
  inboundCarpoolPreferredNote?: string | null;
  inboundWorshipBusRideSlot?: WorshipBusRideSlot | null;
  outboundCarpoolAvailable?: boolean | null;
  outboundCarpoolSeats?: number | null;
  outboundCarpoolArea?: string | null;
  outboundCarpoolRouteArea?: string | null;
  outboundCarpoolNote?: string | null;
  outboundCarpoolPreferredArea?: string | null;
  outboundCarpoolPreferredNote?: string | null;
  outboundWorshipBusRideSlot?: WorshipBusRideSlot | null;
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
    middleGroupName?: string;
    cellName?: string;
    attendanceType: "FULL" | "PARTIAL" | "WORSHIP_ONLY";
    plannedArrivalAt?: string;
    plannedDepartureAt?: string;
    partialAttendanceNote?: string;
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
    outboundCarpoolAvailable?: boolean;
    outboundCarpoolSeats?: number;
    outboundCarpoolArea?: string;
    outboundCarpoolRouteArea?: string;
    outboundCarpoolNote?: string;
    outboundCarpoolPreferredArea?: string;
    outboundCarpoolPreferredNote?: string;
    outboundWorshipBusRideSlot?: WorshipBusRideSlot;
    lodgingNight1?: boolean;
    lodgingNight2?: boolean;
    selectedOptionIds: number[];
  };
};

export function createRegistration(payload: RegistrationCreatePayload) {
  return apiRequest<RegistrationCreateResponse>("/registrations", {
    method: "POST",
    body: payload
  });
}

export function getParticipationOptions() {
  return apiRequest<ParticipationOption[]>("/participation-options");
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
