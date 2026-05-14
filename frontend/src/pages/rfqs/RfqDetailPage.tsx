import React, { useEffect, useState } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import { toast } from "react-toastify";
import { rfqAPI, quotationAPI } from "../../services/api";
import { RFQ, Quotation } from "../../types";
import {
  StatusBadge,
  Spinner,
  PageHeader,
  InfoRow,
  fmt,
  ConfirmModal,
} from "../../components/common/SharedComponents";
import { useAuth } from "../../context/AuthContext";

const RfqDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { isManager, isAdmin, isVendor } = useAuth();
  const [rfq, setRfq] = useState<RFQ | null>(null);
  const [quotations, setQuotations] = useState<Quotation[]>([]);
  const [loading, setLoading] = useState(true);
  const [awardQuotationId, setAwardQuotationId] = useState<number | null>(null);
  const [awardReason, setAwardReason] = useState("");
  const [confirmClose, setConfirmClose] = useState(false);
  const [confirmAward, setConfirmAward] = useState(false);
  const [awarding, setAwarding] = useState(false);

  useEffect(() => {
    const load = async () => {
      try {
        const [rRes] = await Promise.all([rfqAPI.getById(Number(id))]);
        setRfq(rRes.data.data);
        if (!isVendor) {
          const qRes = await quotationAPI.getByRfq(Number(id));
          setQuotations(qRes.data.data || []);
        }
      } catch {
        toast.error("Failed to load RFQ");
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [id, isVendor]);

  const doClose = async () => {
    try {
      const res = await rfqAPI.close(Number(id));
      setRfq(res.data.data);
      toast.success("RFQ closed");
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Failed to close");
    }
  };

  const doAward = async () => {
    if (!awardQuotationId) {
      toast.error("Select a quotation to award");
      return;
    }
    if (!awardReason.trim()) {
      toast.error("Award reason is required");
      return;
    }
    if (awardReason.trim().length < 10) {
      toast.error("Award reason must be at least 10 characters");
      return;
    }
    // US 08 AC #11: Award must require confirmation dialog
    setConfirmAward(true);
  };

  const confirmAwardAction = async () => {
    if (!awardQuotationId) return;
    setAwarding(true);
    try {
      const res = await rfqAPI.award(Number(id), awardQuotationId, awardReason);
      setRfq(res.data.data);
      setQuotations((prev) =>
        prev.map((q) => ({
          ...q,
          status: q.id === awardQuotationId ? "AWARDED" : "REJECTED",
          isAwarded: q.id === awardQuotationId,
        })),
      );
      toast.success("🏆 RFQ awarded successfully!");
      setConfirmAward(false);
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Failed to award RFQ");
    } finally {
      setAwarding(false);
    }
  };

  if (loading) return <Spinner />;
  if (!rfq) return <div>RFQ not found</div>;

  const isPast = new Date(rfq.deadline) < new Date();
  const canAward =
    isManager &&
    (rfq.status === "OPEN" || rfq.status === "CLOSED") &&
    quotations.length > 0;

  return (
    <div>
      <PageHeader
        title={rfq.title}
        breadcrumb={[
          { label: "RFQs", href: "/rfqs" },
          { label: rfq.rfqNumber },
        ]}
        actions={
          <div style={{ display: "flex", gap: 8 }}>
            {isVendor && rfq.status === "OPEN" && !isPast && (
              <Link
                to={`/quotations/submit/${rfq.id}`}
                className="btn btn-success"
              >
                <i className="bi bi-send" /> Submit Quotation
              </Link>
            )}
            {isManager && rfq.status === "OPEN" && (
              <>
                <Link
                  to={`/rfqs/${rfq.id}/edit`}
                  className="btn btn-secondary btn-sm"
                >
                  <i className="bi bi-pencil" /> Edit
                </Link>
                <button
                  className="btn btn-warning btn-sm"
                  onClick={() => setConfirmClose(true)}
                >
                  <i className="bi bi-x-circle" /> Close RFQ
                </button>
              </>
            )}
            {rfq.status === "AWARDED" && isManager && (
              <Link
                to={`/purchase-orders/create/${rfq.id}`}
                className="btn btn-primary"
              >
                <i className="bi bi-file-earmark-plus" /> Generate PO
              </Link>
            )}
            {!isVendor && (
              <Link
                to={`/quotations/compare/${rfq.id}`}
                className="btn btn-info btn-sm"
              >
                <i className="bi bi-bar-chart" /> Compare Quotes
              </Link>
            )}
          </div>
        }
      />

      <div
        style={{ display: "flex", gap: 10, marginBottom: 20, flexWrap: "wrap" }}
      >
        <span className="code-text">{rfq.rfqNumber}</span>
        <StatusBadge status={rfq.status} />
        {rfq.revisionNumber > 1 && (
          <span className="badge badge-secondary">
            Rev. {rfq.revisionNumber}
          </span>
        )}
        {isPast && rfq.status === "OPEN" && (
          <span className="badge badge-danger">Deadline Passed</span>
        )}
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "2fr 1fr", gap: 16 }}>
        {/* Left */}
        <div>
          {/* Details */}
          <div className="card" style={{ marginBottom: 16 }}>
            <div className="card-header">RFQ Details</div>
            <div className="card-body">
              <InfoRow
                label="Deadline"
                value={
                  <span
                    style={{
                      color:
                        isPast && rfq.status === "OPEN"
                          ? "#ef4444"
                          : "var(--text)",
                    }}
                  >
                    {fmt.datetime(rfq.deadline)}
                  </span>
                }
              />
              <InfoRow label="Created By" value={rfq.createdByName} />
              <InfoRow label="Created At" value={fmt.datetime(rfq.createdAt)} />
              {rfq.awardedVendorName && (
                <InfoRow
                  label="Awarded To"
                  value={
                    <span style={{ color: "#22c55e", fontWeight: 700 }}>
                      🏆 {rfq.awardedVendorName}
                    </span>
                  }
                />
              )}
              {rfq.awardReason && (
                <InfoRow label="Award Reason" value={rfq.awardReason} />
              )}
              {rfq.awardedAt && (
                <InfoRow
                  label="Awarded At"
                  value={fmt.datetime(rfq.awardedAt)}
                />
              )}
              {rfq.description && (
                <div style={{ marginTop: 12 }}>
                  <div
                    style={{
                      color: "var(--text-muted)",
                      fontSize: "0.82rem",
                      marginBottom: 4,
                    }}
                  >
                    Description
                  </div>
                  <p style={{ fontSize: "0.875rem", lineHeight: 1.6 }}>
                    {rfq.description}
                  </p>
                </div>
              )}
              <div style={{ marginTop: 12 }}>
                <div
                  style={{
                    color: "var(--text-muted)",
                    fontSize: "0.82rem",
                    marginBottom: 4,
                  }}
                >
                  Terms & Conditions
                </div>
                <div
                  style={{
                    background: "var(--surface2)",
                    borderRadius: 8,
                    padding: "10px 14px",
                    fontSize: "0.875rem",
                    lineHeight: 1.6,
                  }}
                >
                  {rfq.terms}
                </div>
              </div>
            </div>
          </div>

          {/* Items */}
          <div className="card" style={{ marginBottom: 16 }}>
            <div className="card-header">
              Line Items{" "}
              <span className="badge badge-secondary">
                {rfq.items?.length || 0}
              </span>
            </div>
            <div style={{ overflowX: "auto" }}>
              <table className="table">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Item Name</th>
                    <th>Qty</th>
                    <th>Unit</th>
                    <th>Specs</th>
                  </tr>
                </thead>
                <tbody>
                  {rfq.items?.map((item, i) => (
                    <tr key={item.id || i}>
                      <td
                        style={{
                          color: "var(--text-muted)",
                          fontSize: "0.8rem",
                        }}
                      >
                        {i + 1}
                      </td>
                      <td>
                        <div style={{ fontWeight: 600, fontSize: "0.875rem" }}>
                          {item.itemName}
                        </div>
                        {item.description && (
                          <div
                            style={{
                              color: "var(--text-muted)",
                              fontSize: "0.75rem",
                            }}
                          >
                            {item.description}
                          </div>
                        )}
                      </td>
                      <td style={{ fontWeight: 700 }}>{item.quantity}</td>
                      <td
                        style={{
                          fontSize: "0.82rem",
                          color: "var(--text-muted)",
                        }}
                      >
                        {item.unit || "—"}
                      </td>
                      <td
                        style={{
                          fontSize: "0.82rem",
                          color: "var(--text-muted)",
                        }}
                      >
                        {item.specifications || "—"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Quotations */}
          {!isVendor && (
            <div className="card" style={{ marginBottom: 16 }}>
              <div className="card-header">
                Submitted Quotations{" "}
                <span className="badge badge-info">{quotations.length}</span>
              </div>
              {quotations.length === 0 ? (
                <div
                  style={{
                    padding: 24,
                    textAlign: "center",
                    color: "var(--text-muted)",
                    fontSize: "0.875rem",
                  }}
                >
                  No quotations submitted yet
                </div>
              ) : (
                <div style={{ overflowX: "auto" }}>
                  <table className="table">
                    <thead>
                      <tr>
                        {canAward && <th style={{ width: 40 }}></th>}
                        <th>Vendor</th>
                        <th>Total</th>
                        <th>Tax</th>
                        <th>Grand Total</th>
                        <th>Delivery</th>
                        <th>Score</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {quotations.map((q) => (
                        <tr
                          key={q.id}
                          style={{
                            background: q.isAwarded
                              ? "rgba(34,197,94,0.05)"
                              : undefined,
                          }}
                        >
                          {canAward && (
                            <td>
                              <input
                                type="radio"
                                name="awardQ"
                                checked={awardQuotationId === q.id}
                                onChange={() => setAwardQuotationId(q.id)}
                              />
                            </td>
                          )}
                          <td>
                            <div
                              style={{ fontWeight: 600, fontSize: "0.875rem" }}
                            >
                              {q.vendorName}
                            </div>
                            {q.isAwarded && (
                              <span
                                className="badge badge-success"
                                style={{ fontSize: "0.65rem" }}
                              >
                                🏆 Winner
                              </span>
                            )}
                          </td>
                          <td style={{ fontWeight: 600 }}>
                            {fmt.currency(q.totalAmount, q.currency)}
                          </td>
                          <td
                            style={{
                              fontSize: "0.82rem",
                              color: "var(--text-muted)",
                            }}
                          >
                            {q.taxPercentage}%
                          </td>
                          <td style={{ fontWeight: 700, color: "#3b82f6" }}>
                            {fmt.currency(q.grandTotal, q.currency)}
                          </td>
                          <td style={{ fontSize: "0.82rem" }}>
                            {q.deliveryDays} days
                          </td>
                          <td>
                            {q.weightedScore != null ? (
                              <span className="badge badge-info">
                                {q.weightedScore}/100
                              </span>
                            ) : (
                              <span
                                style={{
                                  color: "var(--text-muted)",
                                  fontSize: "0.8rem",
                                }}
                              >
                                —
                              </span>
                            )}
                          </td>
                          <td>
                            <StatusBadge status={q.status} />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {/* Award Panel */}
          {canAward && (
            <div className="card" style={{ border: "1px solid #f59e0b" }}>
              <div className="card-header" style={{ color: "#b45309" }}>
                <i className="bi bi-trophy" style={{ marginRight: 6 }} /> Award
                RFQ
              </div>
              <div className="card-body">
                <div className="form-group">
                  <label className="form-label">Award Reason *</label>
                  <textarea
                    className="form-control"
                    rows={2}
                    value={awardReason}
                    onChange={(e) => setAwardReason(e.target.value)}
                    placeholder="Reason for selecting this vendor..."
                  />
                </div>
                <button
                  className="btn btn-warning"
                  onClick={doAward}
                  disabled={awarding || !awardQuotationId}
                >
                  {awarding ? "Awarding..." : "🏆 Award RFQ to Selected Vendor"}
                </button>
                {!awardQuotationId && (
                  <p
                    style={{
                      color: "var(--text-muted)",
                      fontSize: "0.8rem",
                      marginTop: 6,
                    }}
                  >
                    Select a quotation from the table above first
                  </p>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Right - Invited Vendors */}
        <div>
          <div className="card">
            <div className="card-header">
              Invited Vendors{" "}
              <span className="badge badge-secondary">
                {rfq.invitedVendors?.length || 0}
              </span>
            </div>
            <div>
              {rfq.invitedVendors?.map((v) => (
                <div
                  key={v.id}
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    padding: "10px 16px",
                    borderBottom: "1px solid var(--border)",
                  }}
                >
                  <div>
                    <div style={{ fontWeight: 600, fontSize: "0.875rem" }}>
                      {v.companyName}
                    </div>
                    <div
                      style={{
                        color: "var(--text-muted)",
                        fontSize: "0.75rem",
                      }}
                    >
                      {v.gstNumber}
                    </div>
                  </div>
                  <StatusBadge status={v.status} />
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      <ConfirmModal
        open={confirmClose}
        title="Close RFQ"
        message="Are you sure you want to close this RFQ? No more quotations will be accepted."
        confirmText="Close RFQ"
        danger
        onConfirm={doClose}
        onClose={() => setConfirmClose(false)}
      />

      <ConfirmModal
        open={confirmAward}
        title="🏆 Award RFQ"
        message={`Are you sure you want to award this RFQ to ${quotations.find((q) => q.id === awardQuotationId)?.vendorName}? This action cannot be undone. All other vendors will be notified of rejection.`}
        confirmText="Award RFQ"
        danger={false}
        onConfirm={confirmAwardAction}
        onClose={() => setConfirmAward(false)}
      />
    </div>
  );
};

export default RfqDetailPage;
