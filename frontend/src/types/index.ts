export type UserRole = 'ADMIN' | 'PROCUREMENT_MANAGER' | 'COMPLIANCE_OFFICER' | 'VENDOR';

export interface AuthUser {
  id: number;
  name: string;
  email: string;
  role: string;
  roleType: UserRole | string;
  accessToken: string;
}

export interface LoginRequest { email: string; password: string; }

export type VendorStatus = 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'SUSPENDED';

export interface Vendor {
  id: number; companyName: string; email: string; gstNumber: string;
  registrationId: string; phone: string; address: string; contactPerson: string;
  status: VendorStatus; isEmailVerified: boolean; isCompliant: boolean;
  performanceScore: number; totalRfqsWon: number; totalRfqsParticipated: number;
  rejectionReason: string; approvedByName: string; approvedAt: string; registeredAt: string;
}

export interface VendorRegisterRequest {
  companyName: string; email: string; password: string; gstNumber: string;
  registrationId: string; phone: string; address: string; contactPerson: string;
}

export type RFQStatus = 'OPEN' | 'CLOSED' | 'AWARDED' | 'ARCHIVED';

export interface RfqItem {
  id?: number; itemName: string; description: string;
  quantity: number; unit: string; specifications: string;
}

export interface RFQ {
  id: number; rfqNumber: string; title: string; description: string;
  terms: string; deadline: string; status: RFQStatus; revisionNumber: number;
  createdByName: string; createdById: number; awardedVendorName: string;
  awardedVendorId: number; awardReason: string; awardedAt: string;
  items: RfqItem[]; invitedVendors: Vendor[]; quotationCount: number;
  createdAt: string; updatedAt: string;
}

export interface QuotationItemReq { rfqItemId: number; unitPrice: number; }
export interface QuotationRequest {
  rfqId: number; totalAmount: number; taxPercentage: number; currency: string;
  deliveryDays: number; notes: string; items: QuotationItemReq[];
}

export type QuotationStatus = 'SUBMITTED' | 'UNDER_EVALUATION' | 'AWARDED' | 'REJECTED';

export interface QuotationItemResp {
  id: number; rfqItemId: number; itemName: string; quantity: number;
  unit: string; unitPrice: number; totalPrice: number;
}

export interface Quotation {
  id: number; rfqId: number; rfqNumber: string; vendorId: number; vendorName: string;
  totalAmount: number; taxAmount: number; grandTotal: number; taxPercentage: number;
  currency: string; deliveryDays: number; notes: string; status: QuotationStatus;
  weightedScore: number; evaluationComment: string; evaluatedBy: string;
  evaluatedAt: string; isAwarded: boolean; items: QuotationItemResp[]; submittedAt: string;
}

export type POStatus = 'GENERATED' | 'SENT' | 'RECEIVED' | 'CLOSED';

export interface PurchaseOrder {
  id: number; poNumber: string; rfqId: number; rfqNumber: string; vendorId: number;
  vendorName: string; quotationId: number; totalAmount: number; currency: string;
  deliveryDate: string; shippingAddress: string; paymentTerms: string;
  specialInstructions: string; status: POStatus; generatedByName: string; generatedAt: string;
}

export interface ComplianceDoc {
  id: number; vendorId: number; vendorName: string; documentType: string;
  fileName: string; fileSize: number; fileType: string; issueDate: string;
  expiryDate: string; isExpired: boolean; isLatest: boolean;
  version: number; uploadedAt: string; daysUntilExpiry: number;
}

export interface ComplianceSummary {
  totalDocuments: number;
  expiredDocuments: number;
  expiringIn30Days: number;
  nonCompliantVendors: number;
  compliantVendors: number;
  totalVendors: number;
  complianceRate: number;
}

export interface VendorComplianceStatus {
  vendorId: number;
  vendorName: string;
  isCompliant: boolean;
  totalDocs: number;
  expiredDocs: number;
  expiringDocs: number;
  documents: ComplianceDoc[];
}

export interface Notification {
  id: number; title: string; message: string; type: string;
  isRead: boolean; referenceId: number; referenceType: string;
  createdAt: string; readAt: string | null;
}

// US 13 AC #10: Notification Preference types
export interface NotificationPreference {
  notificationType: string;
  inAppEnabled: boolean;
  emailEnabled: boolean;
}

export interface AuditLog {
  id: number; actorId: number; actorType: string; actorName: string;
  action: string; entityType: string; entityId: number;
  oldValue: string; newValue: string; description: string; timestamp: string;
}

// ── Role & Permission Management (US-RBAC) ────────────────────────────────
export interface ModulePermission {
  id?: number;
  module: string;
  canCreate: boolean;
  canRead: boolean;
  canUpdate: boolean;
  canDelete: boolean;
}

export interface Role {
  id: number;
  name: string;
  description: string;
  isSystem: boolean;
  isActive: boolean;
  permissions: ModulePermission[];
  createdAt: string;
  updatedAt: string;
}

export interface RoleAssignmentHistory {
  id: number;
  userId: number;
  userName: string;
  previousRole: string;
  newRole: string;
  actorId: number;
  actorName: string;
  reason: string;
  changedAt: string;
}

export const SYSTEM_MODULES = [
  'VENDORS', 'RFQS', 'QUOTATIONS', 'PURCHASE_ORDERS',
  'USERS', 'AUDIT_LOGS', 'NOTIFICATIONS', 'COMPLIANCE',
  'ANALYTICS', 'DASHBOARD', 'ROLES',
] as const;

export type SystemModule = typeof SYSTEM_MODULES[number];

export interface DashboardStats {
  totalVendors: number; pendingVendors: number; approvedVendors: number;
  rejectedVendors: number; suspendedVendors: number;
  totalRfqs: number; openRfqs: number; closedRfqs: number; awardedRfqs: number;
  totalQuotations: number; totalPOs: number;
  expiringDocuments: number; expiredDocuments: number;
  vendorsByStatus: { status: string; count: number }[];
  rfqTrend: { status: string; count: number }[];
  topVendors: { name: string; won: number; score: number }[];
}

export interface Page<T> {
  content: T[]; totalElements: number; totalPages: number; number: number; size: number;
}

export interface ApiResponse<T> {
  success: boolean; message: string; data: T;
}

export type Theme = 'light' | 'dark';
