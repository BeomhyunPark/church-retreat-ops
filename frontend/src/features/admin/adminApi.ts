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
  adminMemo?: string | null;
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

export type FeeRosterFilters = {
  feePaid?: boolean;
  keyword?: string;
};

export type FeeDetailResponse = {
  participant: FeeRosterItem;
  events: unknown[];
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

export function getAdminRegistration(id: number) {
  return apiRequest<AdminRegistration>(`/admin/registrations/${id}`, { auth: true });
}

export function getFeeRoster(filters: FeeRosterFilters = {}) {
  const params = new URLSearchParams({
    page: "0",
    size: "20"
  });

  if (filters.feePaid !== undefined) {
    params.set("feePaid", String(filters.feePaid));
  }

  if (filters.keyword?.trim()) {
    params.set("keyword", filters.keyword.trim());
  }

  return apiRequest<PageResponse<FeeRosterItem>>(`/admin/fees?${params.toString()}`, { auth: true });
}

export function updateFeeStatus(participantId: number, feePaid: boolean, reason?: string) {
  return apiRequest<FeeDetailResponse>(`/admin/fees/${participantId}`, {
    auth: true,
    method: "PATCH",
    body: {
      feePaid,
      reason
    }
  });
}
