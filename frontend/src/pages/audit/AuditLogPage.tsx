import React, { useEffect, useState, useCallback } from "react";
import { toast } from "react-toastify";
import { auditAPI } from "../../services/api";
import { AuditLog } from "../../types";
import {
  Spinner,
  EmptyState,
  Pagination,
  PageHeader,
  fmt,
} from "../../components/common/SharedComponents";

const ACTION_COLOR: Record<string, string> = {
  LOGIN: "#3b82f6",
  LOGOUT: "#6b7280",
  LOGIN_FAILED: "#ef4444",
  LOGIN_FAILED_LOCKED: "#dc2626",
  VENDOR_REGISTERED: "#f59e0b",
  VENDOR_APPROVED: "#22c55e",
  VENDOR_REJECTED: "#ef4444",
  VENDOR_SUSPENDED: "#f97316",
  RFQ_CREATED: "#3b82f6",
  RFQ_UPDATED: "#8b5cf6",
  RFQ_AWARDED: "#22c55e",
  RFQ_CLOSED: "#6b7280",
  RFQ_AUTO_CLOSED: "#9ca3af",
  RFQ_FIELD_CHANGED: "#8b5cf6",
  RFQ_REOPENED_ADMIN_OVERRIDE: "#f59e0b",
  QUOTATION_SUBMITTED: "#06b6d4",
  QUOTATION_EVALUATED: "#8b5cf6",
  QUOTATION_RESUBMITTED: "#06b6d4",
  PO_GENERATED: "#22c55e",
  PO_STATUS_UPDATED: "#3b82f6",
  USER_CREATED: "#3b82f6",
  USER_UPDATED: "#8b5cf6",
  USER_DEACTIVATED: "#ef4444",
  USER_UNLOCKED: "#22c55e",
  COMPLIANCE_DOC_UPLOADED: "#06b6d4",
  COMPLIANCE_EXPIRED: "#ef4444",
  VENDORS_INVITED: "#f59e0b",
  AUDIT_VIEW: "#6b7280",
  AUDIT_EXPORT: "#6b7280",
  AUDIT_INTEGRITY_CHECK: "#6b7280",
  AUDIT_RETENTION_TRIGGERED: "#f97316",
  AUDIT_RETENTION_PURGE: "#ef4444",
  PASSWORD_RESET: "#8b5cf6",
  PASSWORD_RESET_REQUEST: "#8b5cf6",
};

interface IntegrityReport {
  intact: boolean;
  totalRecords: number;
  verifiedRecords: number;
  firstBrokenId: number | null;
  message: string;
}

const AuditLogPage: React.FC = () => {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [total, setTotal] = useState(0);
  const [expanded, setExpanded] = useState<number | null>(null);
  // AC #11: integrity check state
  const [integrityReport, setIntegrityReport] = useState<IntegrityReport | null>(null);
  const [checkingIntegrity, setCheckingIntegrity] = useState(false);
  const [filters, setFilters] = useState({
    actorId: "",   // AC #5: filter by user
    action: "",
    entityType: "",
    fromDate: "",
    toDate: "",
  });

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const params: any = { page, size: 20 };
      if (filters.actorId) params.actorId = filters.actorId;
      if (filters.action) params.action = filters.action;
      if (filters.entityType) params.entityType = filters.entityType;
      if (filters.fromDate)
        params.fromDate = new Date(filters.fromDate).toISOString();
      if (filters.toDate)
        params.toDate = new Date(filters.toDate).toISOString();
      const res = await auditAPI.getLogs(params);
      const d = res.data.data;
      setLogs(d.content);
      setTotalPages(d.totalPages);
      setTotal(d.totalElements);
    } catch {
      toast.error("Failed to load audit logs");
    } finally {
      setLoading(false);
    }
  }, [filters, page]);

  useEffect(() => {
    load();
  }, [load]);

  const exportCsv = async () => {
    try {
      // Build export params object matching API signature
      const exportParams: {
        entityType?: string;
        fromDate?: string;
        toDate?: string;
      } = {};
      if (filters.entityType) exportParams.entityType = filters.entityType;
      if (filters.fromDate)
        exportParams.fromDate = new Date(filters.fromDate).toISOString();
      if (filters.toDate)
        exportParams.toDate = new Date(filters.toDate).toISOString();

      const res = await auditAPI.exportCsv(exportParams);
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const a = document.createElement("a");
      a.href = url;
      a.download = "audit-logs.csv";
      a.click();
      window.URL.revokeObjectURL(url);
      toast.success("Audit logs exported");
    } catch {
      toast.error("Export failed");
    }
  };

  const setF = (k: string, v: string) => {
    setFilters((f) => ({ ...f, [k]: v }));
    setPage(0);
  };
  const clear = () => {
    setFilters({ actorId: "", action: "", entityType: "", fromDate: "", toDate: "" });
    setPage(0);
  };

  // AC #11: verify log integrity
  const checkIntegrity = async () => {
    setCheckingIntegrity(true);
    setIntegrityReport(null);
    try {
      const res = await auditAPI.verifyIntegrity();
      setIntegrityReport(res.data.data);
    } catch {
      toast.error("Integrity check failed");
    } finally {
      setCheckingIntegrity(false);
    }
  };

  return (
    <div>
      <PageHeader
        title="Audit Logs"
        subtitle={`${total} immutable log entries`}
        actions={
          <div style={{ display: "flex", gap: 8 }}>
            {/* AC #11: Log integrity check */}
            <button
              className="btn btn-secondary btn-sm"
              onClick={checkIntegrity}
              disabled={checkingIntegrity}
            >
              <i className="bi bi-shield-check" />{" "}
              {checkingIntegrity ? "Checking..." : "Verify Integrity"}
            </button>
            <button className="btn btn-secondary btn-sm" onClick={exportCsv}>
              <i className="bi bi-download" /> Export CSV
            </button>
          </div>
        }
      />

      {/* AC #11: Integrity report banner */}
      {integrityReport && (
        <div
          style={{
            marginBottom: 16,
            padding: "12px 18px",
            borderRadius: 8,
            border: `1px solid ${integrityReport.intact ? "#22c55e" : "#ef4444"}`,
            background: integrityReport.intact ? "rgba(34,197,94,0.07)" : "rgba(239,68,68,0.07)",
            display: "flex",
            alignItems: "center",
            gap: 12,
          }}
        >
          <i
            className={`bi ${integrityReport.intact ? "bi-shield-fill-check" : "bi-shield-fill-exclamation"}`}
            style={{ fontSize: "1.3rem", color: integrityReport.intact ? "#22c55e" : "#ef4444" }}
          />
          <div>
            <div style={{ fontWeight: 700, color: integrityReport.intact ? "#22c55e" : "#ef4444" }}>
              {integrityReport.intact ? "✅ Audit Log Integrity Verified" : "❌ Integrity Violation Detected"}
            </div>
            <div style={{ fontSize: "0.82rem", color: "var(--text-muted)", marginTop: 2 }}>
              {integrityReport.message} &nbsp;·&nbsp; Verified {integrityReport.verifiedRecords}/{integrityReport.totalRecords} records
              {integrityReport.firstBrokenId && ` · First broken ID: ${integrityReport.firstBrokenId}`}
            </div>
          </div>
          <button
            className="btn btn-sm"
            style={{ marginLeft: "auto" }}
            onClick={() => setIntegrityReport(null)}
          >
            <i className="bi bi-x" />
          </button>
        </div>
      )}

      {/* AC #5: Filters — action, entity, actor (user ID), date range */}
      <div className="filter-bar" style={{ marginBottom: 16 }}>
        {/* AC #5: Filter by user/actor ID */}
        <input
          type="number"
          className="form-control"
          style={{ width: 130 }}
          placeholder="Actor ID"
          value={filters.actorId}
          onChange={(e) => setF("actorId", e.target.value)}
        />
        <select
          className="form-control form-select"
          style={{ width: 200 }}
          value={filters.action}
          onChange={(e) => setF("action", e.target.value)}
        >
          <option value="">All Actions</option>
          {Object.keys(ACTION_COLOR).map((a) => (
            <option key={a} value={a}>
              {a.replace(/_/g, " ")}
            </option>
          ))}
        </select>
        <select
          className="form-control form-select"
          style={{ width: 180 }}
          value={filters.entityType}
          onChange={(e) => setF("entityType", e.target.value)}
        >
          <option value="">All Entities</option>
          {[
            "User",
            "Vendor",
            "RFQ",
            "Quotation",
            "PurchaseOrder",
            "ComplianceDocument",
            "AuditLog",
            "Auth",
          ].map((e) => (
            <option key={e} value={e}>
              {e}
            </option>
          ))}
        </select>
        <div className="form-group" style={{ marginBottom: 0 }}>
          <input
            type="datetime-local"
            className="form-control"
            placeholder="From date"
            value={filters.fromDate}
            onChange={(e) => setF("fromDate", e.target.value)}
          />
        </div>
        <div className="form-group" style={{ marginBottom: 0 }}>
          <input
            type="datetime-local"
            className="form-control"
            placeholder="To date"
            value={filters.toDate}
            onChange={(e) => setF("toDate", e.target.value)}
          />
        </div>
        <button className="btn btn-secondary btn-sm" onClick={clear}>
          <i className="bi bi-x-circle" /> Clear
        </button>
      </div>

      <div className="table-wrapper">
        {loading ? (
          <Spinner />
        ) : logs.length === 0 ? (
          <EmptyState
            icon="bi-shield-check"
            title="No audit logs found"
            desc="Adjust filters to find logs"
          />
        ) : (
          <>
            <div style={{ overflowX: "auto" }}>
              <table className="table">
                <thead>
                  <tr>
                    <th style={{ width: 32 }}></th>
                    <th>Action</th>
                    <th>Entity</th>
                    <th>Actor</th>
                    <th>Description</th>
                    <th>Timestamp</th>
                  </tr>
                </thead>
                <tbody>
                  {logs.map((log) => (
                    <React.Fragment key={log.id}>
                      <tr
                        style={{ cursor: "pointer" }}
                        onClick={() =>
                          setExpanded(expanded === log.id ? null : log.id)
                        }
                      >
                        <td style={{ color: "var(--text-muted)" }}>
                          <i
                            className={`bi bi-chevron-${expanded === log.id ? "down" : "right"}`}
                            style={{ fontSize: "0.75rem" }}
                          />
                        </td>
                        <td>
                          <span
                            style={{
                              display: "inline-flex",
                              alignItems: "center",
                              padding: "2px 8px",
                              borderRadius: 20,
                              background: `${ACTION_COLOR[log.action] || "#6b7280"}18`,
                              color: ACTION_COLOR[log.action] || "#6b7280",
                              fontSize: "0.72rem",
                              fontWeight: 700,
                            }}
                          >
                            {log.action.replace(/_/g, " ")}
                          </span>
                        </td>
                        <td>
                          <span
                            style={{ fontWeight: 600, fontSize: "0.85rem" }}
                          >
                            {log.entityType}
                          </span>
                          {log.entityId && (
                            <span
                              style={{
                                color: "var(--text-muted)",
                                fontSize: "0.78rem",
                              }}
                            >
                              {" "}
                              #{log.entityId}
                            </span>
                          )}
                        </td>
                        <td>
                          <div style={{ fontSize: "0.875rem" }}>
                            {log.actorName || "System"}
                          </div>
                          <div
                            style={{
                              fontSize: "0.72rem",
                              color: "var(--text-muted)",
                            }}
                          >
                            {log.actorType}
                          </div>
                        </td>
                        <td
                          style={{
                            maxWidth: 280,
                            fontSize: "0.82rem",
                            color: "var(--text-muted)",
                            overflow: "hidden",
                            textOverflow: "ellipsis",
                            whiteSpace: "nowrap",
                          }}
                        >
                          {log.description || "—"}
                        </td>
                        <td
                          style={{
                            fontSize: "0.78rem",
                            color: "var(--text-muted)",
                            whiteSpace: "nowrap",
                          }}
                        >
                          {fmt.datetime(log.timestamp)}
                        </td>
                      </tr>
                      {expanded === log.id && (
                        <tr>
                          <td colSpan={6}>
                            <div
                              style={{
                                background: "var(--surface2)",
                                borderRadius: 10,
                                padding: 14,
                                margin: "0 8px 8px",
                              }}
                            >
                              <div
                                style={{
                                  display: "grid",
                                  gridTemplateColumns: "1fr 1fr",
                                  gap: 12,
                                }}
                              >
                                {log.description && (
                                  <div>
                                    <div
                                      style={{
                                        fontSize: "0.72rem",
                                        color: "var(--text-muted)",
                                        fontWeight: 600,
                                        marginBottom: 4,
                                      }}
                                    >
                                      DESCRIPTION
                                    </div>
                                    <div style={{ fontSize: "0.82rem" }}>
                                      {log.description}
                                    </div>
                                  </div>
                                )}
                                {log.oldValue && (
                                  <div>
                                    <div
                                      style={{
                                        fontSize: "0.72rem",
                                        color: "#ef4444",
                                        fontWeight: 600,
                                        marginBottom: 4,
                                      }}
                                    >
                                      PREVIOUS VALUE
                                    </div>
                                    <pre
                                      style={{
                                        fontSize: "0.75rem",
                                        background: "var(--code-bg)",
                                        padding: "6px 10px",
                                        borderRadius: 6,
                                        overflow: "auto",
                                        maxHeight: 120,
                                        margin: 0,
                                        color: "var(--text)",
                                      }}
                                    >
                                      {log.oldValue}
                                    </pre>
                                  </div>
                                )}
                                {log.newValue && (
                                  <div>
                                    <div
                                      style={{
                                        fontSize: "0.72rem",
                                        color: "#22c55e",
                                        fontWeight: 600,
                                        marginBottom: 4,
                                      }}
                                    >
                                      NEW VALUE
                                    </div>
                                    <pre
                                      style={{
                                        fontSize: "0.75rem",
                                        background: "var(--code-bg)",
                                        padding: "6px 10px",
                                        borderRadius: 6,
                                        overflow: "auto",
                                        maxHeight: 120,
                                        margin: 0,
                                        color: "var(--text)",
                                      }}
                                    >
                                      {log.newValue}
                                    </pre>
                                  </div>
                                )}
                              </div>
                            </div>
                          </td>
                        </tr>
                      )}
                    </React.Fragment>
                  ))}
                </tbody>
              </table>
            </div>
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                padding: "12px 16px",
                borderTop: "1px solid var(--border)",
              }}
            >
              <span style={{ fontSize: "0.8rem", color: "var(--text-muted)" }}>
                Showing {page * 20 + 1}–{Math.min((page + 1) * 20, total)} of{" "}
                {total}
              </span>
              <Pagination page={page} total={totalPages} onChange={setPage} />
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default AuditLogPage;
