import { createBrowserRouter, Navigate } from "react-router-dom";
import { AdminDashboardPage } from "../features/admin/AdminDashboardPage";
import { AdminFeesPage } from "../features/admin/AdminFeesPage";
import { AdminLoginPage } from "../features/admin/AdminLoginPage";
import { AdminParticipantDetailPage } from "../features/admin/AdminParticipantDetailPage";
import { AdminParticipantsPage } from "../features/admin/AdminParticipantsPage";
import { PublicCheckInPage } from "../features/public/PublicCheckInPage";
import { PublicHomePage } from "../features/public/PublicHomePage";
import { PublicRegisterPage } from "../features/public/PublicRegisterPage";
import { PublicSelfLookupPage } from "../features/public/PublicSelfLookupPage";
import { AdminLayout } from "../shared/layout/AdminLayout";
import { PublicLayout } from "../shared/layout/PublicLayout";
import { PlaceholderPage } from "../shared/ui/PlaceholderPage";

export const router = createBrowserRouter([
  {
    path: "/",
    element: <Navigate to="/public" replace />
  },
  {
    path: "/public",
    element: <PublicLayout />,
    children: [
      { index: true, element: <PublicHomePage /> },
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
      { path: "community", element: <PlaceholderPage title="공동체 구조" /> },
      { path: "retreat-groups", element: <PlaceholderPage title="수련회 조 편성" /> },
      { path: "announcements", element: <PlaceholderPage title="공지 관리" /> },
      { path: "schedules", element: <PlaceholderPage title="일정 관리" /> },
      { path: "check-ins", element: <PlaceholderPage title="체크인 관리" /> }
    ]
  }
]);
