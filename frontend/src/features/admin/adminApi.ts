import { apiRequest } from "../../shared/api/client";

export type AdminProfile = {
  id: number;
  email: string;
  name: string;
  role: "STAFF" | "CHAIR" | "PASTOR" | "SYSTEM_ADMIN";
  status: "ACTIVE" | "INACTIVE" | "LOCKED";
};

export type AdminLoginResponse = {
  accessToken: string;
  admin: AdminProfile;
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type AdminRegistration = {
  id: number;
  name: string;
  gender: "MALE" | "FEMALE";
  birthYear: number;
  phoneNumber: string;
  churchCellDepartment?: string | null;
  middleGroupName?: string | null;
  churchCellName?: string | null;
  retreatGroupName?: string | null;
  retreatGroupLeader: boolean;
  feePaid: boolean;
  status: "REGISTERED" | "CANCELLED";
  newcomer: boolean;
  careTarget: boolean;
  createdAt: string;
  updatedAt: string;
};

export type FeeRosterItem = {
  participantId: number;
  name: string;
  gender: "MALE" | "FEMALE";
  birthYear: number;
  phoneLast4: string;
  feePaid: boolean;
  retreatGroupName?: string | null;
  churchCellName?: string | null;
  feeStatusUpdatedAt?: string | null;
};

export function loginAdmin(email: string, password: string) {
  return apiRequest<AdminLoginResponse>("/admin/auth/login", {
    method: "POST",
    body: { email, password }
  });
}

export function getAdminProfile() {
  return apiRequest<AdminProfile>("/admin/auth/me", { auth: true });
}

export function getAdminRegistrations() {
  return apiRequest<PageResponse<AdminRegistration>>("/admin/registrations?page=0&size=20", { auth: true });
}

export function getFeeRoster() {
  return apiRequest<PageResponse<FeeRosterItem>>("/admin/fees?page=0&size=20", { auth: true });
}
