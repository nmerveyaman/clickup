import type { ReactNode } from "react";
import { BrowserRouter, Routes, Route, Navigate, useLocation } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import DashboardPage from "./pages/DashboardPage";
import WorkspaceMembersPage from './pages/WorkspaceMembersPage';

function ProtectedRoute({ children }: { children: ReactNode }) {
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);

  // 1. URL'de URL parametreleri var mı kontrol et
  const urlUserId = searchParams.get("userId");
  const urlUsername = searchParams.get("username");
  const urlRole = searchParams.get("role");
  const urlWorkspaceId = searchParams.get("workspaceId");

  // 2. URL'de userId varsa, "ProtectedRoute" render edilmeden ÖNCE senkronize olarak kaydediyoruz
  if (urlUserId) {
    localStorage.setItem("userId", urlUserId);
    localStorage.setItem("isLoggedIn", "true");
    if (urlUsername) localStorage.setItem("username", urlUsername);
    if (urlRole) localStorage.setItem("role", urlRole);
    if (urlWorkspaceId) localStorage.setItem("workspaceId", urlWorkspaceId);
  }

  // 3. LocalStorage durumunu kontrol et
  const isLoggedIn =
    localStorage.getItem("isLoggedIn") === "true" ||
    !!localStorage.getItem("userId") ||
    !!urlUserId;

  // 4. Oturum yoksa Login'e fırlat, varsa Dashboard'u göster
  if (!isLoggedIn) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/workspace-members" element={<WorkspaceMembersPage />} />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <DashboardPage />
            </ProtectedRoute>
          }
        />

        {/* Ana sayfa direkt login'e yönlendir */}
        <Route path="/" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;