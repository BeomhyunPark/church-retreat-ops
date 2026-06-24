import { useQuery } from "@tanstack/react-query";
import { apiRequest } from "../api/client";

export type AppIdentity = {
  appName: string;
  organizationName: string;
  eventName: string;
};

export const fallbackAppIdentity: AppIdentity = {
  appName: "Retreat Ops",
  organizationName: "Your Church",
  eventName: "Your Retreat"
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
