import { apiRequest } from "../../shared/api/client";

export type AdminRoleValue = "STAFF" | "CHAIR" | "PASTOR" | "SYSTEM_ADMIN";
export type AdminStatusValue = "ACTIVE" | "INACTIVE" | "LOCKED";

export type AdminProfile = {
  id: number;
  email: string;
  name: string;
  role: AdminRoleValue;
  status: AdminStatusValue;
};

export type AdminAccount = {
  id: number;
  email: string;
  name: string;
  role: AdminRoleValue;
  status: AdminStatusValue;
  lastLoginAt?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type AdminAccountPayload = {
  name: string;
  role: AdminRoleValue;
};

export type AdminCreatePayload = AdminAccountPayload & {
  email: string;
  password: string;
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
  retreatGroupId?: number | null;
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

export type CheckInRosterItem = {
  participantId: number;
  name: string;
  gender: "MALE" | "FEMALE";
  birthYear: number;
  phoneLast4: string;
  churchCellId?: number | null;
  churchCellName?: string | null;
  middleGroupId?: number | null;
  middleGroupName?: string | null;
  retreatGroupId?: number | null;
  retreatGroupName?: string | null;
  retreatGroupLeader: boolean;
  checkedIn: boolean;
  checkedInAt?: string | null;
  checkInMethod?: "MANUAL" | "QR" | null;
  checkedInBy?: { id: number; name: string } | null;
  cancelledAt?: string | null;
  cancelledBy?: { id: number; name: string } | null;
};

export type CheckInRosterFilters = {
  checkedIn?: boolean;
  keyword?: string;
};

export type Announcement = {
  id: number;
  title: string;
  content: string;
  pinned: boolean;
  active: boolean;
  visibleFrom?: string | null;
  visibleUntil?: string | null;
  targets: Array<{
    id: number;
    targetType: string;
    targetValue?: string | null;
    createdAt: string;
  }>;
  createdBy?: { id: number; email: string; name: string; role: string } | null;
  updatedBy?: { id: number; email: string; name: string; role: string } | null;
  createdAt: string;
  updatedAt: string;
};

export type ScheduleItem = {
  id: number;
  title: string;
  description?: string | null;
  scheduleDate: string;
  startsAt: string;
  endsAt: string;
  location?: string | null;
  category:
    | "WORSHIP"
    | "PRAYER"
    | "MEAL"
    | "GROUP_ACTIVITY"
    | "LECTURE"
    | "BREAK"
    | "MOVE"
    | "CHECK_IN"
    | "CHECK_OUT"
    | "NOTICE"
    | "ETC";
  targetAudience: "ALL" | "STAFF_ONLY" | "LEADERS_ONLY" | "NEWCOMERS" | "CARE_TARGETS";
  active: boolean;
  displayOrder: number;
  createdBy?: { id: number; email: string; name: string; role: string } | null;
  updatedBy?: { id: number; email: string; name: string; role: string } | null;
  createdAt: string;
  updatedAt: string;
};

export type ScheduleFilters = {
  active?: boolean;
  category?: string;
  date?: string;
};

export type ChurchMiddleGroup = {
  id: number;
  name: string;
  elderName?: string | null;
  description?: string | null;
  displayOrder: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type ChurchCell = {
  id: number;
  middleGroupId: number;
  middleGroupName: string;
  name: string;
  cellLeaderName?: string | null;
  description?: string | null;
  displayOrder: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type ChurchMiddleGroupPayload = {
  name: string;
  elderName?: string;
  description?: string;
  displayOrder: number;
};

export type ChurchCellPayload = {
  middleGroupId: number;
  name: string;
  cellLeaderName?: string;
  description?: string;
  displayOrder: number;
};

export type RetreatGroup = {
  id: number;
  name: string;
  description?: string | null;
  displayOrder: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type RetreatGroupPayload = {
  name: string;
  description?: string;
  displayOrder: number;
};

export type RetreatGroupMember = {
  id: number;
  retreatGroupId: number;
  retreatGroupName: string;
  participantId: number;
  participantName: string;
  gender: "MALE" | "FEMALE";
  birthYear: number;
  churchCellDepartment?: string | null;
  churchCellId?: number | null;
  churchCellName?: string | null;
  middleGroupId?: number | null;
  middleGroupName?: string | null;
  leader: boolean;
  createdAt: string;
  updatedAt: string;
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

export function getAdminRegistrations(size = 20) {
  return apiRequest<PageResponse<AdminRegistration>>(`/admin/registrations?page=0&size=${size}`, { auth: true });
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

export function getCheckInRoster(filters: CheckInRosterFilters = {}) {
  const params = new URLSearchParams({
    page: "0",
    size: "20"
  });

  if (filters.checkedIn !== undefined) {
    params.set("checkedIn", String(filters.checkedIn));
  }

  if (filters.keyword?.trim()) {
    params.set("keyword", filters.keyword.trim());
  }

  return apiRequest<PageResponse<CheckInRosterItem>>(`/admin/check-ins?${params.toString()}`, { auth: true });
}

export function manuallyCheckIn(participantId: number) {
  return apiRequest<CheckInRosterItem>(`/admin/check-ins/${participantId}`, {
    auth: true,
    method: "POST"
  });
}

export function cancelCheckIn(participantId: number, reason: string) {
  return apiRequest<CheckInRosterItem>(`/admin/check-ins/${participantId}/cancel`, {
    auth: true,
    method: "PATCH",
    body: { reason }
  });
}

export function getAnnouncements() {
  return apiRequest<Announcement[]>("/admin/announcements", { auth: true });
}

export function updateAnnouncementActive(id: number, active: boolean) {
  return apiRequest<Announcement>(`/admin/announcements/${id}/active`, {
    auth: true,
    method: "PATCH",
    body: { active }
  });
}

export function updateAnnouncementPinned(id: number, pinned: boolean) {
  return apiRequest<Announcement>(`/admin/announcements/${id}/pinned`, {
    auth: true,
    method: "PATCH",
    body: { pinned }
  });
}

export function getSchedules(filters: ScheduleFilters = {}) {
  const params = new URLSearchParams();

  if (filters.date) {
    params.set("date", filters.date);
  }

  if (filters.category) {
    params.set("category", filters.category);
  }

  if (filters.active !== undefined) {
    params.set("active", String(filters.active));
  }

  const query = params.toString();
  return apiRequest<ScheduleItem[]>(`/admin/schedules${query ? `?${query}` : ""}`, { auth: true });
}

export function updateScheduleActive(id: number, active: boolean) {
  return apiRequest<ScheduleItem>(`/admin/schedules/${id}/active`, {
    auth: true,
    method: "PATCH",
    body: { active }
  });
}

export function getMiddleGroups() {
  return apiRequest<ChurchMiddleGroup[]>("/admin/community/middle-groups", { auth: true });
}

export function createMiddleGroup(payload: ChurchMiddleGroupPayload) {
  return apiRequest<ChurchMiddleGroup>("/admin/community/middle-groups", {
    auth: true,
    method: "POST",
    body: payload
  });
}

export function updateMiddleGroup(id: number, payload: ChurchMiddleGroupPayload) {
  return apiRequest<ChurchMiddleGroup>(`/admin/community/middle-groups/${id}`, {
    auth: true,
    method: "PATCH",
    body: payload
  });
}

export function updateMiddleGroupActive(id: number, active: boolean) {
  return apiRequest<ChurchMiddleGroup>(`/admin/community/middle-groups/${id}/active`, {
    auth: true,
    method: "PATCH",
    body: { active }
  });
}

export function getCells(filters: { middleGroupId?: number; active?: boolean } = {}) {
  const params = new URLSearchParams();

  if (filters.middleGroupId !== undefined) {
    params.set("middleGroupId", String(filters.middleGroupId));
  }

  if (filters.active !== undefined) {
    params.set("active", String(filters.active));
  }

  const query = params.toString();
  return apiRequest<ChurchCell[]>(`/admin/community/cells${query ? `?${query}` : ""}`, { auth: true });
}

export function createCell(payload: ChurchCellPayload) {
  return apiRequest<ChurchCell>("/admin/community/cells", {
    auth: true,
    method: "POST",
    body: payload
  });
}

export function updateCell(id: number, payload: ChurchCellPayload) {
  return apiRequest<ChurchCell>(`/admin/community/cells/${id}`, {
    auth: true,
    method: "PATCH",
    body: payload
  });
}

export function updateCellActive(id: number, active: boolean) {
  return apiRequest<ChurchCell>(`/admin/community/cells/${id}/active`, {
    auth: true,
    method: "PATCH",
    body: { active }
  });
}

export function getAdminAccounts() {
  return apiRequest<AdminAccount[]>("/admin/users", { auth: true });
}

export function createAdminAccount(payload: AdminCreatePayload) {
  return apiRequest<AdminAccount>("/admin/users", {
    auth: true,
    method: "POST",
    body: payload
  });
}

export function updateAdminAccount(id: number, payload: AdminAccountPayload) {
  return apiRequest<AdminAccount>(`/admin/users/${id}`, {
    auth: true,
    method: "PATCH",
    body: payload
  });
}

export function updateAdminAccountStatus(id: number, status: AdminStatusValue) {
  return apiRequest<AdminAccount>(`/admin/users/${id}/status`, {
    auth: true,
    method: "PATCH",
    body: { status }
  });
}

export function resetAdminAccountPassword(id: number, newPassword: string) {
  return apiRequest<void>(`/admin/users/${id}/password`, {
    auth: true,
    method: "PATCH",
    body: { newPassword }
  });
}

export function changeOwnPassword(currentPassword: string, newPassword: string) {
  return apiRequest<void>("/admin/auth/password", {
    auth: true,
    method: "PATCH",
    body: { currentPassword, newPassword }
  });
}

export function getRetreatGroups() {
  return apiRequest<RetreatGroup[]>("/admin/retreat-groups", { auth: true });
}

export function createRetreatGroup(payload: RetreatGroupPayload) {
  return apiRequest<RetreatGroup>("/admin/retreat-groups", {
    auth: true,
    method: "POST",
    body: payload
  });
}

export function updateRetreatGroup(id: number, payload: RetreatGroupPayload) {
  return apiRequest<RetreatGroup>(`/admin/retreat-groups/${id}`, {
    auth: true,
    method: "PATCH",
    body: payload
  });
}

export function updateRetreatGroupActive(id: number, active: boolean) {
  return apiRequest<RetreatGroup>(`/admin/retreat-groups/${id}/active`, {
    auth: true,
    method: "PATCH",
    body: { active }
  });
}

export function getRetreatGroupMembers(groupId: number) {
  return apiRequest<RetreatGroupMember[]>(`/admin/retreat-groups/${groupId}/members`, { auth: true });
}

export function assignRetreatGroupLeader(groupId: number, participantId: number) {
  return apiRequest<void>(`/admin/retreat-groups/${groupId}/leader`, {
    auth: true,
    method: "PATCH",
    body: { participantId }
  });
}

export function removeRetreatGroupLeader(groupId: number) {
  return apiRequest<void>(`/admin/retreat-groups/${groupId}/leader`, {
    auth: true,
    method: "DELETE",
    body: { confirmText: "DELETE" }
  });
}

export function assignParticipantToRetreatGroup(participantId: number, retreatGroupId: number) {
  return apiRequest<void>(`/admin/participants/${participantId}/retreat-group`, {
    auth: true,
    method: "PATCH",
    body: { retreatGroupId }
  });
}

export function removeParticipantFromRetreatGroup(participantId: number) {
  return apiRequest<void>(`/admin/participants/${participantId}/retreat-group`, {
    auth: true,
    method: "DELETE",
    body: { confirmText: "DELETE" }
  });
}
