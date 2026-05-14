import axios, { AxiosInstance } from 'axios';

const api: AxiosInstance = axios.create({ baseURL: 'http://localhost:8081/api' });

api.interceptors.request.use(cfg => {
  const token = localStorage.getItem('svpms-token');
  if (token) cfg.headers.Authorization = `Bearer ${token}`;
  return cfg;
});

api.interceptors.response.use(
  r => r,
  err => {
    if (err.response?.status === 401 || err.response?.status === 403) {
      localStorage.clear();
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

// AUTH
export const authAPI = {
  login: (data: any) => api.post('/auth/login', data),
  logout: () => api.post('/auth/logout'),
  forgotPassword: (email: string) => api.post(`/auth/forgot-password?email=${encodeURIComponent(email)}`),
  verifyOtp: (email: string, otp: string) =>
    api.post(`/auth/verify-otp?email=${encodeURIComponent(email)}&otp=${encodeURIComponent(otp)}`),
  resetPassword: (email: string, otp: string, newPassword: string) =>
    api.post(`/auth/reset-password?email=${encodeURIComponent(email)}&otp=${encodeURIComponent(otp)}&newPassword=${encodeURIComponent(newPassword)}`),
};

// USERS
export const userAPI = {
  getAll: (p?: any) => api.get('/users', { params: p }),
  create: (d: any) => api.post('/users', d),
  getById: (id: number) => api.get(`/users/${id}`),
  update: (id: number, d: any) => api.put(`/users/${id}`, d),
  deactivate: (id: number) => api.delete(`/users/${id}`),
  unlock: (id: number) => api.patch(`/users/${id}/unlock`),
};

// VENDORS
export const vendorAPI = {
  register: (d: any) => api.post('/vendors/register', d),
  getAll: (p?: any) => api.get('/vendors', { params: p }),
  getApproved: () => api.get('/vendors/approved'),
  getMe: () => api.get('/vendors/me'),
  getById: (id: number) => api.get(`/vendors/${id}`),
  approve: (id: number) => api.patch(`/vendors/${id}/approve`),
  reject: (id: number, reason: string) => api.patch(`/vendors/${id}/reject`, { reason }),
  suspend: (id: number, reason: string) => api.patch(`/vendors/${id}/suspend`, { reason }),
  // Compliance Documents
  getDocs: (id: number) => api.get(`/vendors/${id}/compliance-documents`),
  uploadDoc: (id: number, formData: FormData) =>
    api.post(`/vendors/${id}/compliance-documents`, formData, { headers: { 'Content-Type': 'multipart/form-data' } }),
  getDocVersionHistory: (id: number, documentType: string) =>
    api.get(`/vendors/${id}/compliance-documents/history/${encodeURIComponent(documentType)}`),
  downloadDoc: (vendorId: number, docId: number) =>
    api.get(`/vendors/${vendorId}/compliance-documents/${docId}/download`, { responseType: 'blob' }),
  // Compliance summary (Admin/Compliance Officer)
  getComplianceSummary: () => api.get('/vendors/compliance-summary'),
  getAllComplianceStatus: () => api.get('/vendors/compliance-status'),
};

// RFQs
export const rfqAPI = {
  create: (d: any) => api.post('/rfqs', d),
  getAll: (p?: any) => api.get('/rfqs', { params: p }),
  getForVendor: (vendorId: number, p?: any) => api.get(`/rfqs/vendor/${vendorId}`, { params: p }),
  getById: (id: number) => api.get(`/rfqs/${id}`),
  update: (id: number, d: any) => api.put(`/rfqs/${id}`, d),
  inviteVendors: (id: number, vendorIds: number[]) => api.post(`/rfqs/${id}/invite-vendors`, { vendorIds }),
  close: (id: number) => api.patch(`/rfqs/${id}/close`),
  award: (id: number, quotationId: number, awardReason: string) =>
    api.patch(`/rfqs/${id}/award`, { quotationId, awardReason }),
};

// QUOTATIONS
export const quotationAPI = {
  submit: (d: any) => api.post('/quotations', d),
  resubmit: (id: number, d: any) => api.put(`/quotations/${id}`, d),
  getById: (id: number) => api.get(`/quotations/${id}`),
  getByRfq: (rfqId: number) => api.get(`/quotations/rfq/${rfqId}`),
  compare: (rfqId: number) => api.get(`/quotations/rfq/${rfqId}/compare`),
  getByVendor: (vendorId: number) => api.get(`/quotations/vendor/${vendorId}`),
  evaluate: (id: number, d: any) => api.post(`/quotations/${id}/evaluate`, d),
  uploadDocument: (id: number, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post(`/quotations/${id}/documents`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  },
  getDocuments: (id: number) => api.get(`/quotations/${id}/documents`),
  finalize: (id: number) => api.post(`/quotations/${id}/finalize`),
};

// PURCHASE ORDERS
export const poAPI = {
  generate: (rfqId: number, d: any) => api.post(`/purchase-orders/rfq/${rfqId}`, d),
  getAll: (p?: any) => api.get('/purchase-orders', { params: p }),
  getById: (id: number) => api.get(`/purchase-orders/${id}`),
  exportPdf: (id: number) => api.get(`/purchase-orders/${id}/export`, { responseType: 'blob' }),
  updateStatus: (id: number, status: string) => api.patch(`/purchase-orders/${id}/status`, { status }),
};

// NOTIFICATIONS
export const notifAPI = {
  getAll: (p?: any) => api.get('/notifications', { params: p }),
  getUnreadCount: () => api.get('/notifications/unread-count'),
  markRead: (id: number) => api.patch(`/notifications/${id}/read`),
  markUnread: (id: number) => api.patch(`/notifications/${id}/unread`), // US 13 AC #7
  markAllRead: () => api.patch('/notifications/read-all'),
  // US 13 AC #10: Notification preferences
  getPreferences: () => api.get('/notifications/preferences'),
  updatePreferences: (preferences: any[]) => api.put('/notifications/preferences', preferences),
  resetPreferences: () => api.post('/notifications/preferences/reset'),
};

// AUDIT
export const auditAPI = {
  getLogs: (p?: any) => api.get('/audit-logs', { params: p }),
  exportCsv: (params?: { entityType?: string; fromDate?: string; toDate?: string }) =>
    api.get('/audit-logs/export', { params, responseType: 'blob' }),
  verifyIntegrity: () => api.get('/audit-logs/integrity'),
  runRetention: (retentionDays = 365) =>
    api.post('/audit-logs/retention/run', null, { params: { retentionDays } }),
};

// EXPORTS (Procurement Reports)
export const exportAPI = {
  // AC #1: Vendor list
  vendorsCsv:   (p?: any) => api.get('/exports/vendors/csv',   { params: p, responseType: 'blob' }),
  vendorsExcel: (p?: any) => api.get('/exports/vendors/excel', { params: p, responseType: 'blob' }),
  // AC #2: RFQ comparison
  rfqComparisonPdf: (rfqId: number) => api.get(`/exports/rfqs/${rfqId}/comparison/pdf`, { responseType: 'blob' }),
  // AC #3: PO history
  poHistoryCsv:   (p?: any) => api.get('/exports/purchase-orders/csv',   { params: p, responseType: 'blob' }),
  poHistoryExcel: (p?: any) => api.get('/exports/purchase-orders/excel', { params: p, responseType: 'blob' }),
  // AC #3: RFQ list
  rfqsCsv: (p?: any) => api.get('/exports/rfqs/csv', { params: p, responseType: 'blob' }),
};
export const roleAPI = {
  getAll: () => api.get('/roles'),
  getById: (id: number) => api.get(`/roles/${id}`),
  create: (d: any) => api.post('/roles', d),
  update: (id: number, d: any) => api.put(`/roles/${id}`, d),
  delete: (id: number, confirmed: boolean) => api.delete(`/roles/${id}?confirmed=${confirmed}`),
  updateModulePermissions: (id: number, d: any) => api.patch(`/roles/${id}/permissions`, d),
  assignRole: (userId: number, roleName: string, reason?: string) =>
    api.patch(`/roles/assign/${userId}`, { roleName, reason }),
  getAssignmentHistory: () => api.get('/roles/assignment-history'),
  getUserAssignmentHistory: (userId: number) => api.get(`/roles/assignment-history/user/${userId}`),
  getMyPermissions: () => api.get('/roles/my-permissions'),
};

// DASHBOARD
export const dashboardAPI = {
  getKpis: () => api.get('/dashboard/kpis'),
};

// VENDOR ANALYTICS
export const vendorAnalyticsAPI = {
  getVendorAnalytics: (vendorId: number, startDate?: string, endDate?: string) =>
    api.get(`/vendor-analytics/${vendorId}`, { params: { startDate, endDate } }),
  getAllVendorsAnalytics: (startDate?: string, endDate?: string) =>
    api.get('/vendor-analytics', { params: { startDate, endDate } }),
  exportAnalytics: (vendorId: number, startDate?: string, endDate?: string) =>
    api.get(`/vendor-analytics/${vendorId}/export`, {
      params: { startDate, endDate },
      responseType: 'blob'
    }),
  updateMetrics: (vendorId: number) => api.post(`/vendor-analytics/${vendorId}/update-metrics`),
};

export default api;
