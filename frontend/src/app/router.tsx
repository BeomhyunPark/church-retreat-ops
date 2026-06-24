import { createBrowserRouter, Navigate } from "react-router-dom";
import { AdminAccountsPage } from "../features/admin/AdminAccountsPage";
import { AdminAnnouncementsPage } from "../features/admin/AdminAnnouncementsPage";
import { AdminCheckInsPage } from "../features/admin/AdminCheckInsPage";
import { AdminCommunityPage } from "../features/admin/AdminCommunityPage";
import { AdminDashboardPage } from "../features/admin/AdminDashboardPage";
import { AdminFeesPage } from "../features/admin/AdminFeesPage";
import { AdminLoginPage } from "../features/admin/AdminLoginPage";
import { AdminParticipantDetailPage } from "../features/admin/AdminParticipantDetailPage";
import { AdminParticipantsPage } from "../features/admin/AdminParticipantsPage";
import { AdminProfilePage } from "../features/admin/AdminProfilePage";
import { AdminRetreatGroupsPage } from "../features/admin/AdminRetreatGroupsPage";
import { AdminSchedulesPage } from "../features/admin/AdminSchedulesPage";
import { AppHomePage } from "../features/home/AppHomePage";
import { PublicCheckInPage } from "../features/public/PublicCheckInPage";
import { PublicRegisterPage } from "../features/public/PublicRegisterPage";
import { PublicSelfLookupPage } from "../features/public/PublicSelfLookupPage";
import { AdminLayout } from "../shared/layout/AdminLayout";
import { PublicLayout } from "../shared/layout/PublicLayout";

export const router = createBrowserRouter([
  {
    path: "/",
    element: <AppHomePage />
  },
  {
    path: "/public",
    element: <PublicLayout />,
    children: [
      { index: true, element: <Navigate to="/public/register" replace /> },
      { path: "register", element: <PublicRegisterPage /> },
      { path: "self-lookup", element: <PublicSelfLookupPage /> },
      { path: "check-in", element: <PublicCheckInPage /> }
    ]
  },
  {
    path: "/admin/login",
    element: <AdminLoginPage />
  },
  {
    path: "/admin",
    element: <AdminLayout />,
    children: [
      { index: true, element: <Navigate to="/admin/dashboard" replace /> },
      { path: "dashboard", element: <AdminDashboardPage /> },
      { path: "participants", element: <AdminParticipantsPage /> },
      { path: "participants/:participantId", element: <AdminParticipantDetailPage /> },
      { path: "fees", element: <AdminFeesPage /> },
      { path: "community", element: <AdminCommunityPage /> },
      { path: "retreat-groups", element: <AdminRetreatGroupsPage /> },
      { path: "announcements", element: <AdminAnnouncementsPage /> },
      { path: "schedules", element: <AdminSchedulesPage /> },
      { path: "check-ins", element: <AdminCheckInsPage /> },
      { path: "accounts", element: <AdminAccountsPage /> },
      { path: "profile", element: <AdminProfilePage /> }
    ]
  }
]);
