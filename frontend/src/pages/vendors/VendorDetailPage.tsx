import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { vendorAPI } from "../../services/api";
import { Vendor, ComplianceDoc } from "../../types";
import {
  StatusBadge,
  Spinner,
  InfoRow,
  PageHeader,
  ConfirmModal,
  fmt,
} from "../../components/common/SharedComponents";
import { useAuth } from "../../context/AuthContext";

const VendorDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { isAdmin, isVendor, user, hasRole } = useAuth();
  const [vendor, setVendor] = useState<Vendor | null>(null);
  const [docs, setDocs] = useState<ComplianceDoc[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [rejectReason, setRejectReason] = useState("");
  const [showReject, setShowReject] = useState(false);
  const [confirmApprove, setConfirmApprove] = useState(false);

  useEffect(() => {
    const fetchAll = async () => {
      try {
        const [vRes, dRes] = await Promise.all([
          vendorAPI.getById(Number(id)),
          vendorAPI.getDocs(Number(id)),
        ]);
        setVendor(vRes.data.data);
        setDocs(dRes.data.data || []);
      } catch {
        toast.error("Failed to load vendor");
      } finally {
        setLoading(false);
      }
    };
    fetchAll();
  }, [id]);

  const doApprove = async () => {
    setActionLoading(true);
    try {
      const res = await vendorAPI.approve(Number(id));
      setVendor(res.data.data);
      toast.success("Vendor approved successfully!");
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Failed to approve");
    } finally {
      setActionLoading(false);
    }
  };

  const doReject = async () => {
    if (!rejectReason.trim()) {
      toast.error("Rejection reason is required");
      return;
    }
    setActionLoading(true);
    try {
      const res = await vendorAPI.reject(Number(id), rejectReason);
      setVendor(res.data.data);
      setShowReject(false);
      toast.success("Vendor rejected");
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Failed to reject");
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) return <Spinner />;
  if (!vendor) return <div>Vendor not found</div>;

  return (
    <div>
      <PageHeader
        title={vendor.companyName}
        breadcrumb={[
          { label: "Vendors", href: "/vendors" },
          { label: vendor.companyName },
        ]}
        actions={
          <>
            {hasRole("ADMIN", "PROCUREMENT_MANAGER") &&
              vendor.status === "APPROVED" && (
                <button
                  className="btn btn-primary btn-sm"
                  onClick={() => navigate(`/analytics/vendor/${id}`)}
                >
                  <i className="bi bi-graph-up-arrow" /> View Analytics
                </button>
              )}
            {isAdmin && vendor.status === "PENDING_APPROVAL" && (
              <>
                <button
                  className="btn btn-success btn-sm"
                  onClick={() => setConfirmApprove(true)}
                  disabled={actionLoading}
                >
                  <i className="bi bi-check-circle" /> Approve
                </button>
                <button
                  className="btn btn-danger btn-sm"
                  onClick={() => setShowReject(true)}
                >
                  <i className="bi bi-x-circle" /> Reject
                </button>
              </>
            )}
          </>
        }
      />

      <div
        style={{
          display: "flex",
          alignItems: "center",
          gap: 10,
          marginBottom: 20,
        }}
      >
        <StatusBadge status={vendor.status} />
        <span
          className={`badge ${vendor.isCompliant ? "badge-success" : "badge-danger"}`}
        >
          {vendor.isCompliant ? "✓ Compliant" : "✗ Non-Compliant"}
        </span>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "2fr 1fr", gap: 16 }}>
        {/* Main info */}
        <div>
          <div className="card" style={{ marginBottom: 16 }}>
            <div className="card-header">Company Details</div>
            <div className="card-body">
              <InfoRow label="Company Name" value={vendor.companyName} />
              <InfoRow
                label="GST Number"
                value={<span className="code-text">{vendor.gstNumber}</span>}
              />
              <InfoRow label="Registration ID" value={vendor.registrationId} />
              <InfoRow label="Email" value={vendor.email} />
              <InfoRow label="Phone" value={vendor.phone} />
              <InfoRow label="Contact Person" value={vendor.contactPerson} />
              <InfoRow label="Address" value={vendor.address} />
              <InfoRow
                label="Registered At"
                value={fmt.datetime(vendor.registeredAt)}
              />
              {vendor.approvedByName && (
                <InfoRow label="Approved By" value={vendor.approvedByName} />
              )}
              {vendor.approvedAt && (
                <InfoRow
                  label="Approved At"
                  value={fmt.datetime(vendor.approvedAt)}
                />
              )}
              {vendor.rejectionReason && (
                <InfoRow
                  label="Rejection Reason"
                  value={
                    <span style={{ color: "#ef4444" }}>
                      {vendor.rejectionReason}
                    </span>
                  }
                />
              )}
            </div>
          </div>

          {/* Compliance Docs */}
          <div className="card">
            <div className="card-header">
              Compliance Documents
              <span className="badge badge-secondary" style={{ marginLeft: 8 }}>{docs.length}</span>
              {isVendor && (
                <button
                  className="btn btn-primary btn-sm"
                  style={{ marginLeft: "auto" }}
                  onClick={() => navigate(`/vendors/${id}/compliance`)}
                >
                  <i className="bi bi-cloud-upload" /> Upload / Manage
                </button>
              )}
            </div>
            {docs.length === 0 ? (
              <div
                style={{
                  padding: "24px",
                  textAlign: "center",
                  color: "var(--text-muted)",
                  fontSize: "0.875rem",
                }}
              >
                No compliance documents uploaded yet
              </div>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table className="table">
                  <thead>
                    <tr>
                      <th>Type</th>
                      <th>File</th>
                      <th>Issue Date</th>
                      <th>Expiry Date</th>
                      <th>Validity</th>
                      <th>Version</th>
                    </tr>
                  </thead>
                  <tbody>
                    {docs.map((d) => {
                      const daysLeft = Math.ceil(
                        (new Date(d.expiryDate).getTime() - Date.now()) / 86400000
                      );
                      return (
                        <tr key={d.id}>
                          <td style={{ fontWeight: 600, fontSize: "0.85rem" }}>
                            {d.documentType}
                          </td>
                          <td style={{ fontSize: "0.82rem" }}>{d.fileName}</td>
                          <td style={{ fontSize: "0.82rem" }}>
                            {fmt.date(d.issueDate)}
                          </td>
                          <td
                            style={{
                              fontSize: "0.82rem",
                              color: d.isExpired ? "#ef4444" : "var(--text)",
                            }}
                          >
                            {fmt.date(d.expiryDate)}
                          </td>
                          <td>
                            {d.isExpired ? (
                              <span className="badge badge-danger">Expired</span>
                            ) : daysLeft <= 7 ? (
                              <span className="badge badge-danger">{daysLeft}d left</span>
                            ) : daysLeft <= 30 ? (
                              <span className="badge badge-warning">{daysLeft}d left</span>
                            ) : (
                              <span className="badge badge-success">Valid</span>
                            )}
                          </td>
                          <td style={{ fontSize: "0.82rem" }}>v{d.version}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>

        {/* Stats */}
        <div>
          <div className="card" style={{ marginBottom: 16 }}>
            <div className="card-header">Performance Stats</div>
            <div className="card-body">
              <InfoRow
                label="RFQs Participated"
                value={vendor.totalRfqsParticipated}
              />
              <InfoRow label="RFQs Won" value={vendor.totalRfqsWon} />
              <InfoRow
                label="Win Rate"
                value={
                  vendor.totalRfqsParticipated > 0
                    ? `${((vendor.totalRfqsWon / vendor.totalRfqsParticipated) * 100).toFixed(1)}%`
                    : "—"
                }
              />
              <InfoRow
                label="Performance Score"
                value={
                  <div
                    style={{ display: "flex", alignItems: "center", gap: 6 }}
                  >
                    <span style={{ fontWeight: 700, color: "#3b82f6" }}>
                      {vendor.performanceScore.toFixed(1)}
                    </span>
                    <span
                      style={{
                        color: "var(--text-muted)",
                        fontSize: "0.78rem",
                      }}
                    >
                      /100
                    </span>
                  </div>
                }
              />
            </div>
          </div>

          {/* Reject form */}
          {isAdmin && showReject && (
            <div className="card" style={{ border: "1px solid #ef4444" }}>
              <div className="card-header" style={{ color: "#ef4444" }}>
                Reject Vendor
              </div>
              <div className="card-body">
                <div className="form-group">
                  <label className="form-label">Rejection Reason *</label>
                  <textarea
                    className="form-control"
                    rows={3}
                    value={rejectReason}
                    onChange={(e) => setRejectReason(e.target.value)}
                    placeholder="Provide reason for rejection..."
                  />
                </div>
                <div style={{ display: "flex", gap: 8 }}>
                  <button
                    className="btn btn-danger btn-sm"
                    onClick={doReject}
                    disabled={actionLoading}
                  >
                    {actionLoading ? "Rejecting..." : "Confirm Reject"}
                  </button>
                  <button
                    className="btn btn-secondary btn-sm"
                    onClick={() => setShowReject(false)}
                  >
                    Cancel
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      <ConfirmModal
        open={confirmApprove}
        title="Approve Vendor"
        message={`Are you sure you want to approve ${vendor.companyName}? They will be able to participate in RFQs.`}
        confirmText="Approve"
        onConfirm={doApprove}
        onClose={() => setConfirmApprove(false)}
      />
    </div>
  );
};

export default VendorDetailPage;
