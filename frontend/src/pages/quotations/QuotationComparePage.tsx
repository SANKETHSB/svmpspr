import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { quotationAPI, rfqAPI } from "../../services/api";
import { Quotation, RFQ } from "../../types";
import {
  PageHeader,
  Spinner,
  StatusBadge,
  fmt,
} from "../../components/common/SharedComponents";
import { useAuth } from "../../context/AuthContext";

const QuotationComparePage: React.FC = () => {
  const { rfqId } = useParams<{ rfqId: string }>();
  const navigate = useNavigate();
  const { isManager } = useAuth();
  const [rfq, setRfq] = useState<RFQ | null>(null);
  const [quotations, setQuotations] = useState<Quotation[]>([]);
  const [loading, setLoading] = useState(true);
  const [evaluateModal, setEvaluateModal] = useState<{
    open: boolean;
    quotation: Quotation | null;
  }>({ open: false, quotation: null });
  const [evaluating, setEvaluating] = useState(false);
  const [docsModal, setDocsModal] = useState<{
    open: boolean;
    quotation: Quotation | null;
    docs: any[];
  }>({ open: false, quotation: null, docs: [] });
  const [loadingDocs, setLoadingDocs] = useState(false);
  const [exporting, setExporting] = useState(false);

  useEffect(() => {
    loadData();
  }, [rfqId]);

  const loadData = () => {
    setLoading(true);
    Promise.all([
      rfqAPI.getById(Number(rfqId)),
      quotationAPI.compare(Number(rfqId)),
    ])
      .then(([rRes, qRes]) => {
        setRfq(rRes.data.data);
        setQuotations(qRes.data.data || []);
      })
      .catch(() => toast.error("Failed to load comparison"))
      .finally(() => setLoading(false));
  };

  const handleEvaluate = (quotation: Quotation) => {
    setEvaluateModal({ open: true, quotation });
  };

  const submitEvaluation = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const score = Number(formData.get("score"));
    const comment = (formData.get("comment") as string).trim();

    if (!evaluateModal.quotation) return;
    if (score < 0 || score > 100) {
      toast.error("Score must be between 0 and 100");
      return;
    }
    if (!comment) {
      toast.error("Evaluation comment is required");
      return;
    }
    if (comment.length < 10) {
      toast.error("Evaluation comment must be at least 10 characters");
      return;
    }
    if (comment.length > 1000) {
      toast.error("Evaluation comment must not exceed 1000 characters");
      return;
    }

    setEvaluating(true);
    quotationAPI
      .evaluate(evaluateModal.quotation.id, { score, comment, award: false })
      .then(() => {
        toast.success("Quotation evaluated successfully");
        setEvaluateModal({ open: false, quotation: null });
        loadData();
      })
      .catch((err) => {
        const errorMsg =
          err.response?.data?.message || "Failed to evaluate quotation";
        toast.error(errorMsg);
      })
      .finally(() => setEvaluating(false));
  };

  const handleViewDocs = (quotation: Quotation) => {
    setLoadingDocs(true);
    setDocsModal({ open: true, quotation, docs: [] });
    quotationAPI
      .getDocuments(quotation.id)
      .then((res) =>
        setDocsModal((prev) => ({ ...prev, docs: res.data.data || [] })),
      )
      .catch(() => toast.error("Failed to load documents"))
      .finally(() => setLoadingDocs(false));
  };

  const handleDownloadDoc = (
    quotationId: number,
    docId: number,
    fileName: string,
  ) => {
    fetch(
      `http://localhost:8081/api/quotations/${quotationId}/documents/${docId}/download`,
      {
        headers: {
          Authorization: `Bearer ${localStorage.getItem("svpms-token")}`,
        },
      },
    )
      .then((res) => res.blob())
      .then((blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = fileName;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
      })
      .catch(() => toast.error("Failed to download document"));
  };

  const handleExportPdf = () => {
    setExporting(true);
    fetch(`http://localhost:8081/api/quotations/rfq/${rfqId}/export-pdf`, {
      headers: {
        Authorization: `Bearer ${localStorage.getItem("svpms-token")}`,
      },
    })
      .then((res) => res.blob())
      .then((blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = `quotation-comparison-${rfq?.rfqNumber || rfqId}.pdf`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        toast.success("PDF exported successfully");
      })
      .catch(() => toast.error("Failed to export PDF"))
      .finally(() => setExporting(false));
  };

  if (loading) return <Spinner />;
  if (!rfq) return <div>RFQ not found</div>;

  const lowest =
    quotations.length > 0
      ? Math.min(...quotations.map((q) => q.grandTotal))
      : 0;

  return (
    <div>
      <PageHeader
        title="Quotation Comparison"
        subtitle={`${rfq.rfqNumber} — ${quotations.length} quotations`}
        breadcrumb={[
          { label: "RFQs", href: "/rfqs" },
          { label: rfq.rfqNumber, href: `/rfqs/${rfq.id}` },
          { label: "Compare" },
        ]}
        actions={
          <>
            {quotations.length > 0 && (
              <button
                className="btn btn-primary btn-sm"
                onClick={handleExportPdf}
                disabled={exporting}
              >
                <i className="bi bi-file-earmark-pdf" />{" "}
                {exporting ? "Exporting..." : "Export PDF"}
              </button>
            )}
            <button
              className="btn btn-secondary btn-sm"
              onClick={() => navigate(-1)}
            >
              <i className="bi bi-arrow-left" /> Back
            </button>
          </>
        }
      />

      {quotations.length === 0 ? (
        <div
          className="card"
          style={{
            padding: 48,
            textAlign: "center",
            color: "var(--text-muted)",
          }}
        >
          <i
            className="bi bi-receipt"
            style={{ fontSize: "2.5rem", marginBottom: 12, display: "block" }}
          />
          <h6>No quotations submitted for this RFQ yet</h6>
        </div>
      ) : (
        <>
          {/* Summary cards */}
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
              gap: 14,
              marginBottom: 20,
            }}
          >
            {[
              {
                label: "Total Quotations",
                value: quotations.length,
                icon: "bi-receipt",
                color: "#3b82f6",
              },
              {
                label: "Lowest Bid",
                value: fmt.currency(lowest),
                icon: "bi-arrow-down-circle",
                color: "#22c55e",
              },
              {
                label: "Highest Bid",
                value: fmt.currency(
                  Math.max(...quotations.map((q) => q.grandTotal)),
                ),
                icon: "bi-arrow-up-circle",
                color: "#ef4444",
              },
              {
                label: "Avg. Grand Total",
                value: fmt.currency(
                  quotations.reduce((s, q) => s + q.grandTotal, 0) /
                    quotations.length,
                ),
                icon: "bi-calculator",
                color: "#f59e0b",
              },
            ].map((c) => (
              <div key={c.label} className="card" style={{ padding: 16 }}>
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <div>
                    <div
                      style={{
                        fontSize: "0.75rem",
                        color: "var(--text-muted)",
                      }}
                    >
                      {c.label}
                    </div>
                    <div
                      style={{
                        fontWeight: 700,
                        fontSize: "1rem",
                        marginTop: 2,
                      }}
                    >
                      {c.value}
                    </div>
                  </div>
                  <i
                    className={`bi ${c.icon}`}
                    style={{ fontSize: "1.4rem", color: c.color }}
                  />
                </div>
              </div>
            ))}
          </div>

          {/* Comparison Table */}
          <div className="card">
            <div className="card-header">
              Side-by-Side Comparison (sorted by Grand Total ↑)
            </div>
            <div style={{ overflowX: "auto" }}>
              <table className="table">
                <thead>
                  <tr>
                    <th>Rank</th>
                    <th>Vendor</th>
                    <th>Subtotal</th>
                    <th>Tax %</th>
                    <th>Tax Amount</th>
                    <th>Grand Total</th>
                    <th>Delivery</th>
                    <th>Score</th>
                    <th>Status</th>
                    <th>Documents</th>
                    <th>Evaluation</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {quotations.map((q, i) => (
                    <tr
                      key={q.id}
                      style={{
                        background:
                          q.grandTotal === lowest
                            ? "rgba(34,197,94,0.06)"
                            : undefined,
                        fontWeight: q.isAwarded ? 600 : undefined,
                      }}
                    >
                      <td>
                        <div
                          style={{
                            width: 28,
                            height: 28,
                            borderRadius: "50%",
                            background:
                              i === 0 ? "#22c55e20" : "var(--badge-bg)",
                            color: i === 0 ? "#16a34a" : "var(--text-muted)",
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            fontWeight: 700,
                            fontSize: "0.8rem",
                          }}
                        >
                          {i + 1}
                        </div>
                      </td>
                      <td>
                        <div style={{ fontWeight: 600 }}>{q.vendorName}</div>
                        {q.isAwarded && (
                          <span
                            className="badge badge-success"
                            style={{ fontSize: "0.65rem" }}
                          >
                            🏆 Awarded
                          </span>
                        )}
                        {q.grandTotal === lowest && !q.isAwarded && (
                          <span
                            className="badge badge-success"
                            style={{ fontSize: "0.65rem" }}
                          >
                            Lowest
                          </span>
                        )}
                      </td>
                      <td>{fmt.currency(q.totalAmount, q.currency)}</td>
                      <td>{q.taxPercentage}%</td>
                      <td>{fmt.currency(q.taxAmount, q.currency)}</td>
                      <td
                        style={{
                          fontWeight: 700,
                          color:
                            q.grandTotal === lowest ? "#22c55e" : "#3b82f6",
                        }}
                      >
                        {fmt.currency(q.grandTotal, q.currency)}
                      </td>
                      <td>{q.deliveryDays} days</td>
                      <td>
                        {q.weightedScore != null ? (
                          <span className="badge badge-info">
                            {q.weightedScore}/100
                          </span>
                        ) : (
                          <span style={{ color: "var(--text-muted)" }}>—</span>
                        )}
                      </td>
                      <td>
                        <StatusBadge status={q.status} />
                      </td>
                      <td>
                        <button
                          className="btn btn-secondary btn-sm"
                          onClick={() => handleViewDocs(q)}
                        >
                          <i className="bi bi-file-earmark-text" /> View
                        </button>
                      </td>
                      <td style={{ fontSize: "0.75rem" }}>
                        {q.evaluatedAt ? (
                          <div>
                            <div style={{ color: "var(--text-muted)" }}>
                              By: {q.evaluatedBy || "—"}
                            </div>
                            <div style={{ color: "var(--text-muted)" }}>
                              {fmt.datetime(q.evaluatedAt)}
                            </div>
                            {q.evaluationComment && (
                              <div
                                style={{
                                  marginTop: 4,
                                  fontStyle: "italic",
                                  maxWidth: 150,
                                  overflow: "hidden",
                                  textOverflow: "ellipsis",
                                  whiteSpace: "nowrap",
                                }}
                                title={q.evaluationComment}
                              >
                                "{q.evaluationComment}"
                              </div>
                            )}
                          </div>
                        ) : (
                          <span style={{ color: "var(--text-muted)" }}>
                            Not evaluated
                          </span>
                        )}
                      </td>
                      <td>
                        {isManager && (
                          <button
                            className="btn btn-primary btn-sm"
                            onClick={() => handleEvaluate(q)}
                          >
                            <i className="bi bi-star" />{" "}
                            {q.weightedScore != null
                              ? "Re-evaluate"
                              : "Evaluate"}
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

      {/* Evaluation Modal */}
      {evaluateModal.open && evaluateModal.quotation && (
        <div
          className="modal-overlay"
          onClick={() => setEvaluateModal({ open: false, quotation: null })}
        >
          <div
            className="modal-box"
            onClick={(e) => e.stopPropagation()}
            style={{ maxWidth: 500 }}
          >
            <div className="modal-header">
              <span>
                Evaluate Quotation - {evaluateModal.quotation.vendorName}
              </span>
              <button
                className="btn btn-secondary btn-sm btn-icon"
                onClick={() =>
                  setEvaluateModal({ open: false, quotation: null })
                }
              >
                <i className="bi bi-x-lg" />
              </button>
            </div>
            <form onSubmit={submitEvaluation}>
              <div className="modal-body">
                {rfq && rfq.status === "OPEN" && (
                  <div
                    style={{
                      padding: 12,
                      marginBottom: 16,
                      background: "#fef3c7",
                      border: "1px solid #fbbf24",
                      borderRadius: 6,
                      fontSize: "0.85rem",
                      color: "#92400e",
                    }}
                  >
                    <i
                      className="bi bi-exclamation-triangle"
                      style={{ marginRight: 6 }}
                    />
                    <strong>Note:</strong> This RFQ is still OPEN. You can
                    evaluate now for preliminary scoring, but more quotations
                    may be submitted before the deadline.
                  </div>
                )}
                <div style={{ marginBottom: 16 }}>
                  <div
                    style={{
                      fontSize: "0.85rem",
                      color: "var(--text-muted)",
                      marginBottom: 8,
                    }}
                  >
                    Grand Total:{" "}
                    <strong>
                      {fmt.currency(
                        evaluateModal.quotation.grandTotal,
                        evaluateModal.quotation.currency,
                      )}
                    </strong>
                  </div>
                  <div
                    style={{
                      fontSize: "0.85rem",
                      color: "var(--text-muted)",
                      marginBottom: 8,
                    }}
                  >
                    Delivery:{" "}
                    <strong>{evaluateModal.quotation.deliveryDays} days</strong>
                  </div>
                  {evaluateModal.quotation.weightedScore != null && (
                    <div
                      style={{
                        fontSize: "0.85rem",
                        color: "var(--text-muted)",
                        marginBottom: 8,
                      }}
                    >
                      Current Score:{" "}
                      <strong>
                        {evaluateModal.quotation.weightedScore}/100
                      </strong>
                    </div>
                  )}
                </div>
                <div className="form-group">
                  <label>
                    Weighted Score (0-100){" "}
                    <span style={{ color: "red" }}>*</span>
                  </label>
                  <input
                    type="number"
                    name="score"
                    className="form-control"
                    min="0"
                    max="100"
                    step="0.01"
                    defaultValue={evaluateModal.quotation.weightedScore || ""}
                    required
                    placeholder="Enter score between 0 and 100"
                  />
                </div>
                <div className="form-group">
                  <label>
                    Evaluation Comment <span style={{ color: "red" }}>*</span>
                  </label>
                  <textarea
                    name="comment"
                    className="form-control"
                    rows={4}
                    required
                    defaultValue={
                      evaluateModal.quotation.evaluationComment || ""
                    }
                    placeholder="Enter your evaluation comments (mandatory)"
                  />
                  <small style={{ color: "var(--text-muted)" }}>
                    Evaluation comments are mandatory before award (minimum 10
                    characters, maximum 1000)
                  </small>
                </div>
              </div>
              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() =>
                    setEvaluateModal({ open: false, quotation: null })
                  }
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={evaluating}
                >
                  {evaluating ? "Evaluating..." : "Submit Evaluation"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Documents Modal */}
      {docsModal.open && docsModal.quotation && (
        <div
          className="modal-overlay"
          onClick={() =>
            setDocsModal({ open: false, quotation: null, docs: [] })
          }
        >
          <div
            className="modal-box"
            onClick={(e) => e.stopPropagation()}
            style={{ maxWidth: 600 }}
          >
            <div className="modal-header">
              <span>Documents - {docsModal.quotation.vendorName}</span>
              <button
                className="btn btn-secondary btn-sm btn-icon"
                onClick={() =>
                  setDocsModal({ open: false, quotation: null, docs: [] })
                }
              >
                <i className="bi bi-x-lg" />
              </button>
            </div>
            <div className="modal-body">
              {loadingDocs ? (
                <div style={{ textAlign: "center", padding: 24 }}>
                  <div className="spinner-ring" style={{ margin: "0 auto" }} />
                  <p style={{ marginTop: 12, color: "var(--text-muted)" }}>
                    Loading documents...
                  </p>
                </div>
              ) : docsModal.docs.length === 0 ? (
                <div
                  style={{
                    textAlign: "center",
                    padding: 24,
                    color: "var(--text-muted)",
                  }}
                >
                  <i
                    className="bi bi-file-earmark-x"
                    style={{
                      fontSize: "2rem",
                      display: "block",
                      marginBottom: 8,
                    }}
                  />
                  <p>No documents uploaded for this quotation</p>
                </div>
              ) : (
                <div
                  style={{ display: "flex", flexDirection: "column", gap: 12 }}
                >
                  {docsModal.docs.map((doc: any) => (
                    <div
                      key={doc.id}
                      className="card"
                      style={{
                        padding: 12,
                        display: "flex",
                        flexDirection: "row",
                        alignItems: "center",
                        justifyContent: "space-between",
                      }}
                    >
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: 12,
                        }}
                      >
                        <i
                          className="bi bi-file-earmark-pdf"
                          style={{ fontSize: "1.5rem", color: "#ef4444" }}
                        />
                        <div>
                          <div
                            style={{ fontWeight: 600, fontSize: "0.875rem" }}
                          >
                            {doc.fileName}
                          </div>
                          <div
                            style={{
                              fontSize: "0.75rem",
                              color: "var(--text-muted)",
                            }}
                          >
                            {doc.fileSize
                              ? `${(doc.fileSize / 1024).toFixed(2)} KB`
                              : "—"}{" "}
                            • Uploaded {fmt.datetime(doc.uploadedAt)}
                          </div>
                          {doc.checksum && (
                            <div
                              style={{
                                fontSize: "0.7rem",
                                color: "var(--text-muted)",
                                fontFamily: "monospace",
                              }}
                            >
                              SHA-256: {doc.checksum.substring(0, 16)}...
                            </div>
                          )}
                        </div>
                      </div>
                      <button
                        className="btn btn-primary btn-sm"
                        onClick={() =>
                          handleDownloadDoc(
                            docsModal.quotation!.id,
                            doc.id,
                            doc.fileName,
                          )
                        }
                      >
                        <i className="bi bi-download" /> Download
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
            <div className="modal-footer">
              <button
                className="btn btn-secondary"
                onClick={() =>
                  setDocsModal({ open: false, quotation: null, docs: [] })
                }
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default QuotationComparePage;
