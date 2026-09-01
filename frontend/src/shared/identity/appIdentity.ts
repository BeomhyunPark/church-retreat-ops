import { useQuery } from "@tanstack/react-query";
import { apiRequest } from "../api/client";

export type AppIdentity = {
  appName: string;
  organizationName: string;
  eventName: string;
  registrationOpen: boolean;
};

export const fallbackAppIdentity: AppIdentity = {
  appName: "청년2부 수련회",
  organizationName: "지구촌교회 드림공동체 청년2부",
  eventName: "청년2부 수련회",
  registrationOpen: false
};

export function getAppIdentity() {
  return apiRequest<AppIdentity>("/app/identity");
}

export function useAppIdentity() {
  const query = useQuery({
    queryKey: ["app", "identity"],
    queryFn: getAppIdentity
  });

  return {
    ...query,
    identity: query.data ?? fallbackAppIdentity
  };
}

export function brandInitials(value: string) {
  const trimmed = value.trim();
  if (!trimmed) {
    return "RO";
  }

  const words = trimmed.split(/\s+/).filter(Boolean);
  if (words.length > 1) {
    return words
      .slice(0, 2)
      .map((word) => word[0])
      .join("")
      .toUpperCase();
  }

  return trimmed.slice(0, 2).toUpperCase();
}
