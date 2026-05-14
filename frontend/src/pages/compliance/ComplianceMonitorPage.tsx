import React, { useEffect, useState, useCallback } from "react";
import { Link } from "react-router-dom";
import { toast } from "react-toastify";
import { vendorAPI } from "../../services/api";
import {
  ComplianceSummary,
  VendorComplianceStatus,
  ComplianceDoc,
} from "../../types";
import {
  PageHeader,
  Spinner,
  fmt,
} from "../../components/common/SharedComponents";

// ─── Stat Card ───────────────────────────────────────────────────────────────
const StatCard: React.FC<{
  label: string;
  value: number | string;
  icon: string;
  color: string;
  sub?: string;
}> = ({ label, value, icon, color, sub }) => (
  <div className="card stat-card" style={{ height: "100%" }}>
    <div className="card-body" style={{ padding: 20 }}>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "flex-start",
        }}
      >
        <div>
          <p
            style={{
              color: "var(--text-muted)",
              fontSize: "0.8rem",
              fontWeight: 500,
              marginBottom: 6,
            }}
          >
            {label}
          </p>
          <h3
            style={{
              fontWeight: 800,
              fontSize: "1.8rem",
              color: "var(--text)",
              marginBottom: 2,
            }}
          >
            {value}
          </h3>
          {sub && (
            <p style={{ color: "var(--text-muted)", fontSize: "0.75rem" }}>
              {sub}
            </p>
          )}
        </div>
        <div className="stat-icon" style={{ background: `${color}45`, color }}>
          <i className={`bi ${icon}`} />
        </div>
      </div>
    </div>
  </div>
);

// ─── Days badge ───────────────────────────────────────────────────────────────
const DaysBadge: React.FC<{ days: number; expired: boolean }> = ({
  days,
  expired,
}) => {
  if (expired) return <span className="badge badge-danger">Expired</span>;
  if (days <= 7)
    return <span className="badge badge-danger">{days}d left</span>;
  if (days <= 30)
    return <span className="badge badge-warning">{days}d left</span>;
  return <span className="badge badge-success">{days}d left</span>;
};

// ─── Main Page ────────────────────────────────────────────────────────────────
const ComplianceMonitorPage: React.FC = () => {
  const [summary, setSummary] = useState<ComplianceSummary | null>(null);
  const [vendors, setVendors] = useState<VendorComplianceStatus[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<"all" | "non-compliant" | "expiring">(
    "all",
  );
  const [search, setSearch] = useState("");
  const [expanded, setExpanded] = useState<number | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [sumRes, statusRes] = await Promise.all([
        vendorAPI.getComplianceSummary(),
        vendorAPI.getAllComplianceStatus(),
      ]);
      setSummary(sumRes.data.data);
      setVendors(statusRes.data.data || []);
    } catch {
      toast.error("Failed to load compliance data");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const filtered = vendors.filter((v) => {
    const matchSearch = v.vendorName
      .toLowerCase()
      .includes(search.toLowerCase());
    if (!matchSearch) return false;
    if (filter === "non-compliant") return !v.isCompliant;
    if (filter === "expiring") return v.expiringDocs > 0;
    return true;
  });

  const avgDocsPerVendor =
    summary && summary.totalVendors
      ? (summary.totalDocuments / summary.totalVendors).toFixed(1)
      : "0.0";
  const complianceTarget = 85;
  const riskLevel =
    summary && summary.expiredDocuments > 0
      ? "High"
      : summary && summary.expiringIn30Days > 0
        ? "Medium"
        : "Low";

  if (loading) return <Spinner msg="Loading compliance data..." />;

  return (
    <div>
      <PageHeader
        title="Compliance Monitor"
        subtitle="Track vendor document validity and regulatory compliance"
        breadcrumb={[{ label: "Compliance Monitor" }]}
      />

      {/* ── Summary KPIs ── */}
      {summary && (
        <>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(160px, 1fr))",
              gap: 14,
              marginBottom: 16,
            }}
          >
            <StatCard
              label="Compliance Rate"
              value={`${summary.complianceRate}%`}
              icon="bi-shield-fill-check"
              color={
                summary.complianceRate >= 80
                  ? "#22c55e"
                  : summary.complianceRate >= 60
                    ? "#f59e0b"
                    : "#ef4444"
              }
              sub={`${summary.compliantVendors} of ${summary.totalVendors} vendors`}
            />
            <StatCard
              label="Total Documents"
              value={summary.totalDocuments}
              icon="bi-file-earmark-text-fill"
              color="#3b82f6"
            />
            <StatCard
              label="Expired Documents"
              value={summary.expiredDocuments}
              icon="bi-x-circle-fill"
              color="#ef4444"
              sub="Require immediate action"
            />
            <StatCard
              label="Expiring (30 days)"
              value={summary.expiringIn30Days}
              icon="bi-exclamation-triangle-fill"
              color="#f59e0b"
              sub="Renewal needed soon"
            />
            <StatCard
              label="Non-Compliant Vendors"
              value={summary.nonCompliantVendors}
              icon="bi-person-x-fill"
              color="#ef4444"
              sub="Blocked from new RFQs"
            />
          </div>
          <div className="card" style={{ marginBottom: 24 }}>
            <div className="card-header">
              <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <span className="header-icon" style={{ background: "rgba(34,197,94,0.25)" }}>
                  <i className="bi bi-shield-check" />
                </span>
                Compliance Insights
              </div>
            </div>
            <div className="card-body">
              <div className="analysis-grid">
                <div className="analysis-item">
                  <div className="analysis-label">
                    <i className="bi bi-bullseye" />
                    Target Rate
                  </div>
                  <div className="analysis-value">{complianceTarget}%</div>
                  <div className="analysis-meta">Policy benchmark</div>
                </div>
                <div className="analysis-item">
                  <div className="analysis-label">
                    <i className="bi bi-graph-up" />
                    Risk Level
                  </div>
                  <div className="analysis-value">{riskLevel}</div>
                  <div className="analysis-meta">{summary.expiredDocuments} expired documents</div>
                </div>
                <div className="analysis-item">
                  <div className="analysis-label">
                    <i className="bi bi-calendar-event" />
                    Docs Expiring Soon
                  </div>
                  <div className="analysis-value">{summary.expiringIn30Days}</div>
                  <div className="analysis-meta">Next 30 days</div>
                </div>
                <div className="analysis-item">
                  <div className="analysis-label">
                    <i className="bi bi-stack" />
                    Avg Docs / Vendor
                  </div>
                  <div className="analysis-value">{avgDocsPerVendor}</div>
                  <div className="analysis-meta">Portfolio coverage</div>
                </div>
              </div>
            </div>
          </div>
        </>
      )}

      {/* ── Filters ── */}
      <div className="card" style={{ marginBottom: 16 }}>
        <div
          className="card-body"
          style={{
            display: "flex",
            gap: 12,
            flexWrap: "wrap",
            alignItems: "center",
          }}
        >
          <input
            className="form-control"
            style={{ maxWidth: 260 }}
            placeholder="Search vendor..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <div style={{ display: "flex", gap: 8 }}>
            {(["all", "non-compliant", "expiring"] as const).map((f) => (
              <button
                key={f}
                className={`btn btn-sm ${filter === f ? "btn-primary" : "btn-secondary"}`}
                onClick={() => setFilter(f)}
              >
                {f === "all"
                  ? "All Vendors"
                  : f === "non-compliant"
                    ? "Non-Compliant"
                    : "Expiring Soon"}
              </button>
            ))}
          </div>
          <button
            className="btn btn-secondary btn-sm"
            style={{ marginLeft: "auto" }}
            onClick={load}
          >
            <i className="bi bi-arrow-clockwise" /> Refresh
          </button>
        </div>
      </div>

      {/* ── Vendor Compliance Table ── */}
      <div className="card">
        <div className="card-header">
          Vendor Compliance Status
          <span className="badge badge-secondary" style={{ marginLeft: 8 }}>
            {filtered.length}
          </span>
        </div>

        {filtered.length === 0 ? (
          <div
            style={{
              padding: 32,
              textAlign: "center",
              color: "var(--text-muted)",
            }}
          >
            <i
              className="bi bi-shield-check"
              style={{ fontSize: "2rem", display: "block", marginBottom: 8 }}
            />
            No vendors match the current filter.
          </div>
        ) : (
          <div style={{ overflowX: "auto" }}>
            <table className="table">
              <thead>
                <tr>
                  <th>Vendor</th>
                  <th>Status</th>
                  <th>Documents</th>
                  <th>Expired</th>
                  <th>Expiring Soon</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((v) => (
                  <React.Fragment key={v.vendorId}>
                    <tr
                      style={{
                        cursor: "pointer",
                        background:
                          expanded === v.vendorId
                            ? "var(--hover-bg)"
                            : undefined,
                      }}
                      onClick={() =>
                        setExpanded(expanded === v.vendorId ? null : v.vendorId)
                      }
                    >
                      <td style={{ fontWeight: 600 }}>
                        <i
                          className={`bi bi-chevron-${expanded === v.vendorId ? "down" : "right"}`}
                          style={{
                            marginRight: 8,
                            fontSize: "0.75rem",
                            color: "var(--text-muted)",
                          }}
                        />
                        {v.vendorName}
                      </td>
                      <td>
                        <span
                          className={`badge ${v.isCompliant ? "badge-success" : "badge-danger"}`}
                        >
                          {v.isCompliant ? "✓ Compliant" : "✗ Non-Compliant"}
                        </span>
                      </td>
                      <td>{v.totalDocs}</td>
                      <td>
                        {v.expiredDocs > 0 ? (
                          <span className="badge badge-danger">
                            {v.expiredDocs}
                          </span>
                        ) : (
                          <span style={{ color: "var(--text-muted)" }}>—</span>
                        )}
                      </td>
                      <td>
                        {v.expiringDocs > 0 ? (
                          <span className="badge badge-warning">
                            {v.expiringDocs}
                          </span>
                        ) : (
                          <span style={{ color: "var(--text-muted)" }}>—</span>
                        )}
                      </td>
                      <td>
                        <Link
                          to={`/vendors/${v.vendorId}`}
                          className="btn btn-secondary btn-sm"
                          onClick={(e) => e.stopPropagation()}
                        >
                          <i className="bi bi-eye" /> View
                        </Link>
                      </td>
                    </tr>

                    {/* Expanded document rows */}
                    {expanded === v.vendorId && (
                      <tr>
                        <td
                          colSpan={6}
                          style={{ padding: 0, background: "var(--hover-bg)" }}
                        >
                          <div style={{ padding: "12px 24px" }}>
                            {v.documents.length === 0 ? (
                              <p
                                style={{
                                  color: "var(--text-muted)",
                                  margin: 0,
                                  fontSize: "0.85rem",
                                }}
                              >
                                No compliance documents uploaded.
                              </p>
                            ) : (
                              <table
                                className="table"
                                style={{ marginBottom: 0, fontSize: "0.82rem" }}
                              >
                                <thead>
                                  <tr>
                                    <th>Document Type</th>
                                    <th>File</th>
                                    <th>Issue Date</th>
                                    <th>Expiry Date</th>
                                    <th>Validity</th>
                                    <th>Version</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  {v.documents.map((d: ComplianceDoc) => (
                                    <tr key={d.id}>
                                      <td style={{ fontWeight: 600 }}>
                                        {d.documentType}
                                      </td>
                                      <td
                                        style={{ color: "var(--text-muted)" }}
                                      >
                                        {d.fileName}
                                      </td>
                                      <td>{fmt.date(d.issueDate)}</td>
                                      <td
                                        style={{
                                          color: d.isExpired
                                            ? "#ef4444"
                                            : "var(--text)",
                                        }}
                                      >
                                        {fmt.date(d.expiryDate)}
                                      </td>
                                      <td>
                                        <DaysBadge
                                          days={d.daysUntilExpiry}
                                          expired={d.isExpired}
                                        />
                                      </td>
                                      <td>v{d.version}</td>
                                    </tr>
                                  ))}
                                </tbody>
                              </table>
                            )}
                          </div>
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default ComplianceMonitorPage;
