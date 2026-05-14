import React from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { ToastContainer } from "react-toastify";
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap-icons/font/bootstrap-icons.css";
import "react-toastify/dist/ReactToastify.css";
import "./App.css";

import { AuthProvider, useAuth } from "./context/AuthContext";
import { ThemeProvider } from "./context/ThemeContext";
import { VoiceAssistantProvider } from "./context/VoiceAssistantContext";
import Layout from "./components/layout/Layout";

// Pages
import AuthPage from "./pages/auth/AuthPage";
import DashboardPage from "./pages/dashboard/DashboardPage";
import VendorListPage from "./pages/vendors/VendorListPage";
import VendorDetailPage from "./pages/vendors/VendorDetailPage";
import RfqListPage from "./pages/rfqs/RfqListPage";
import RfqCreatePage from "./pages/rfqs/RfqCreatePage";
import RfqDetailPage from "./pages/rfqs/RfqDetailPage";
import QuotationListPage from "./pages/quotations/QuotationListPage";
import QuotationSubmitPage from "./pages/quotations/QuotationSubmitPage";
import QuotationComparePage from "./pages/quotations/QuotationComparePage";
import POListPage from "./pages/purchase-orders/POListPage";
import POCreatePage from "./pages/purchase-orders/POCreatePage";
import PODetailPage from "./pages/purchase-orders/PODetailPage";
import NotificationsPage from "./pages/notifications/NotificationsPage";
import NotificationPreferencesPage from "./pages/notifications/NotificationPreferencesPage";
import AuditLogPage from "./pages/audit/AuditLogPage";
import UserManagementPage from "./pages/admin/UserManagementPage";
import VendorAnalyticsDashboard from "./pages/analytics/VendorAnalyticsDashboard";
import ComplianceMonitorPage from "./pages/compliance/ComplianceMonitorPage";
import VendorComplianceUploadPage from "./pages/compliance/VendorComplianceUploadPage";
import RoleManagementPage from "./pages/admin/RoleManagementPage";
import ReportsPage from "./pages/reports/ReportsPage";
import CurtainDemoPage from "./pages/demo/CurtainDemoPage";
import CurtainLoginDemo from "./components/demo/CurtainLoginDemo";

const Guard: React.FC<{ children: React.ReactNode; roles?: string[] }> = ({
  children,
  roles,
}) => {
  const { isAuthenticated, hasRole } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (roles && !hasRole(...roles)) return <Navigate to="/dashboard" replace />;
  return <>{children}</>;
};

const AppRoutes: React.FC = () => {
  const { isAuthenticated } = useAuth();
  return (
    <Routes>
      <Route
        path="/login"
        element={
          isAuthenticated ? <Navigate to="/dashboard" /> : <CurtainLoginDemo />
        }
      />
      <Route
        path="/register"
        element={
          isAuthenticated ? <Navigate to="/dashboard" /> : <CurtainLoginDemo />
        }
      />
      {/* Demo Route - Standalone Curtain Animation */}
      <Route path="/demo/curtain" element={<CurtainDemoPage />} />
      <Route
        path="/"
        element={
          <Guard>
            <Layout />
          </Guard>
        }
      >
        <Route index element={<Navigate to="/dashboard" />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route
          path="vendors"
          element={
            <Guard
              roles={["ADMIN", "PROCUREMENT_MANAGER", "COMPLIANCE_OFFICER"]}
            >
              <VendorListPage />
            </Guard>
          }
        />
        <Route path="vendors/:id" element={<VendorDetailPage />} />
        <Route
          path="vendors/:id/compliance"
          element={
            <Guard roles={["VENDOR"]}>
              <VendorComplianceUploadPage />
            </Guard>
          }
        />
        <Route
          path="compliance"
          element={
            <Guard roles={["ADMIN", "COMPLIANCE_OFFICER"]}>
              <ComplianceMonitorPage />
            </Guard>
          }
        />
        <Route
          path="analytics/vendor/:vendorId"
          element={
            <Guard roles={["ADMIN", "PROCUREMENT_MANAGER"]}>
              <VendorAnalyticsDashboard />
            </Guard>
          }
        />
        <Route path="rfqs" element={<RfqListPage />} />
        <Route
          path="rfqs/create"
          element={
            <Guard roles={["PROCUREMENT_MANAGER"]}>
              <RfqCreatePage />
            </Guard>
          }
        />
        <Route path="rfqs/:id" element={<RfqDetailPage />} />
        <Route
          path="rfqs/:id/edit"
          element={
            <Guard roles={["PROCUREMENT_MANAGER"]}>
              <RfqCreatePage />
            </Guard>
          }
        />
        <Route path="quotations" element={<QuotationListPage />} />
        <Route
          path="quotations/submit/:rfqId"
          element={
            <Guard roles={["VENDOR"]}>
              <QuotationSubmitPage />
            </Guard>
          }
        />
        <Route
          path="quotations/compare/:rfqId"
          element={
            <Guard roles={["ADMIN", "PROCUREMENT_MANAGER"]}>
              <QuotationComparePage />
            </Guard>
          }
        />
        <Route path="purchase-orders" element={<POListPage />} />
        <Route
          path="purchase-orders/create/:rfqId"
          element={
            <Guard roles={["PROCUREMENT_MANAGER"]}>
              <POCreatePage />
            </Guard>
          }
        />
        <Route path="purchase-orders/:id" element={<PODetailPage />} />
        <Route path="notifications" element={<NotificationsPage />} />
        <Route path="notifications/preferences" element={<NotificationPreferencesPage />} />
        <Route
          path="audit-logs"
          element={
            <Guard roles={["ADMIN", "COMPLIANCE_OFFICER"]}>
              <AuditLogPage />
            </Guard>
          }
        />
        <Route
          path="users"
          element={
            <Guard roles={["ADMIN"]}>
              <UserManagementPage />
            </Guard>
          }
        />
        <Route
          path="roles"
          element={
            <Guard roles={["ADMIN"]}>
              <RoleManagementPage />
            </Guard>
          }
        />
        <Route
          path="reports"
          element={
            <Guard roles={["ADMIN", "PROCUREMENT_MANAGER", "COMPLIANCE_OFFICER"]}>
              <ReportsPage />
            </Guard>
          }
        />
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" />} />
    </Routes>
  );
};

const App: React.FC = () => (
  <ThemeProvider>
    <AuthProvider>
      <VoiceAssistantProvider>
        <BrowserRouter>
          <AppRoutes />
          <ToastContainer position="top-right" autoClose={3500} theme="colored" />
        </BrowserRouter>
      </VoiceAssistantProvider>
    </AuthProvider>
  </ThemeProvider>
);

export default App;
