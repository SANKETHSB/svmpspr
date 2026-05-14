import React, { useEffect, useState, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { vendorAPI } from "../../services/api";
import { ComplianceDoc } from "../../types";
import { PageHeader, Spinner, fmt } from "../../components/common/SharedComponents";

const ALLOWED_TYPES = ["application/pdf", "image/png", "image/jpeg"];
const ALLOWED_EXT   = ["pdf", "png", "jpg", "jpeg"];
const MAX_MB        = 10;

const MANDATORY_DOCS = [
  "GST Certificate",
  "Business Registration",
  "ISO Certification",
  "Tax Compliance Certificate",
  "Insurance Certificate",
];

// ─── Days badge ───────────────────────────────────────────────────────────────
const DaysBadge: React.FC<{ days: number; expired: boolean }> = ({ days, expired }) => {
  if (expired)    return <span className="badge badge-danger">Expired</span>;
  if (days <= 7)  return <span className="badge badge-danger">{days}d left</span>;
  if (days <= 30) return <span className="badge badge-warning">{days}d left</span>;
  return <span className="badge badge-success">{days}d left</span>;
};

const VendorComplianceUploadPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const vendorId = Number(id);

  const [docs, setDocs]               = useState<ComplianceDoc[]>([]);
  const [loading, setLoading]         = useState(true);
  const [uploading, setUploading]     = useState(false);
  const [historyDoc, setHistoryDoc]   = useState<string | null>(null);
  const [history, setHistory]         = useState<ComplianceDoc[]>([]);
  const [historyLoading, setHistoryLoading] = useState(false);

  // Form state
  const [docType, setDocType]     = useState(MANDATORY_DOCS[0]);
  const [customType, setCustomType] = useState("");
  const [issueDate, setIssueDate] = useState("");
  const [expiryDate, setExpiryDate] = useState("");
  const [file, setFile]           = useState<File | null>(null);
  const [fileError, setFileError] = useState("");

  const loadDocs = useCallback(async () => {
    setLoading(true);
    try {
      const res = await vendorAPI.getDocs(vendorId);
      setDocs(res.data.data || []);
    } catch {
      toast.error("Failed to load compliance documents");
    } finally {
      setLoading(false);
    }
  }, [vendorId]);

  useEffect(() => { loadDocs(); }, [loadDocs]);

  // AC #7: Client-side file validation
  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    setFileError("");
    if (!f) { setFile(null); return; }

    const ext = f.name.split(".").pop()?.toLowerCase() || "";
    if (!ALLOWED_EXT.includes(ext)) {
      setFileError(`Invalid file type ".${ext}". Allowed: PDF, PNG, JPG/JPEG.`);
      setFile(null);
      e.target.value = "";
      return;
    }
    if (!ALLOWED_TYPES.includes(f.type)) {
      setFileError(`Invalid MIME type "${f.type}". Allowed: PDF, PNG, JPG/JPEG.`);
      setFile(null);
      e.target.value = "";
      return;
    }
    if (f.size > MAX_MB * 1024 * 1024) {
      setFileError(`File too large (${(f.size / 1024 / 1024).toFixed(1)} MB). Max: ${MAX_MB} MB.`);
      setFile(null);
      e.target.value = "";
      return;
    }
    setFile(f);
  };

  const handleUpload = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!file) { toast.error("Please select a file"); return; }
    if (!issueDate || !expiryDate) { toast.error("Issue date and expiry date are required"); return; }

    const finalType = docType === "__custom__" ? customType.trim() : docType;
    if (!finalType) { toast.error("Document type is required"); return; }

    const formData = new FormData();
    formData.append("documentType", finalType);
    formData.append("issueDate", issueDate);
    formData.append("expiryDate", expiryDate);
    formData.append("file", file);

    setUploading(true);
    try {
      await vendorAPI.uploadDoc(vendorId, formData);
      toast.success("Document uploaded successfully!");
      // Reset form
      setFile(null);
      setIssueDate("");
      setExpiryDate("");
      setFileError("");
      (document.getElementById("compliance-file-input") as HTMLInputElement).value = "";
      await loadDocs();
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Upload failed");
    } finally {
      setUploading(false);
    }
  };

  // AC #8: Load version history
  const loadHistory = async (documentType: string) => {
    setHistoryDoc(documentType);
    setHistoryLoading(true);
    try {
      const res = await vendorAPI.getDocVersionHistory(vendorId, documentType);
      setHistory(res.data.data || []);
    } catch {
      toast.error("Failed to load version history");
    } finally {
      setHistoryLoading(false);
    }
  };

  const uploadedTypes = new Set(docs.map((d) => d.documentType));
  const missingDocs   = MANDATORY_DOCS.filter((t) => !uploadedTypes.has(t));

  if (loading) return <Spinner />;

  return (
    <div>
      <PageHeader
        title="Compliance Documents"
        subtitle="Upload and manage your regulatory compliance documents"
        breadcrumb={[
          { label: "My Profile", href: `/vendors/${vendorId}` },
          { label: "Compliance Documents" },
        ]}
      />

      {/* Missing mandatory docs warning */}
      {missingDocs.length > 0 && (
        <div className="alert alert-warning" style={{ marginBottom: 16, display: "flex", gap: 10, alignItems: "flex-start" }}>
          <i className="bi bi-exclamation-triangle-fill" style={{ marginTop: 2 }} />
          <div>
            <strong>Missing mandatory documents:</strong>{" "}
            {missingDocs.join(", ")}. Upload them to maintain compliance.
          </div>
        </div>
      )}

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1.4fr", gap: 16 }}>

        {/* ── Upload Form ── */}
        <div className="card">
          <div className="card-header">
            <i className="bi bi-cloud-upload" style={{ marginRight: 8 }} />
            Upload Document
          </div>
          <div className="card-body">
            <form onSubmit={handleUpload}>
              {/* Document Type */}
              <div className="form-group">
                <label className="form-label">Document Type *</label>
                <select
                  className="form-control"
                  value={docType}
                  onChange={(e) => setDocType(e.target.value)}
                >
                  {MANDATORY_DOCS.map((t) => (
                    <option key={t} value={t}>{t}</option>
                  ))}
                  <option value="__custom__">Other (specify below)</option>
                </select>
              </div>
              {docType === "__custom__" && (
                <div className="form-group">
                  <label className="form-label">Custom Document Type *</label>
                  <input
                    className="form-control"
                    value={customType}
                    onChange={(e) => setCustomType(e.target.value)}
                    placeholder="e.g. Environmental Clearance"
                  />
                </div>
              )}

              {/* Issue Date */}
              <div className="form-group">
                <label className="form-label">Issue Date *</label>
                <input
                  type="date"
                  className="form-control"
                  value={issueDate}
                  max={new Date().toISOString().split("T")[0]}
                  onChange={(e) => setIssueDate(e.target.value)}
                  required
                />
              </div>

              {/* Expiry Date */}
              <div className="form-group">
                <label className="form-label">Expiry Date *</label>
                <input
                  type="date"
                  className="form-control"
                  value={expiryDate}
                  min={new Date().toISOString().split("T")[0]}
                  onChange={(e) => setExpiryDate(e.target.value)}
                  required
                />
              </div>

              {/* File Upload — AC #7 */}
              <div className="form-group">
                <label className="form-label">
                  File * <span style={{ color: "var(--text-muted)", fontSize: "0.78rem" }}>
                    (PDF, PNG, JPG/JPEG — max {MAX_MB} MB)
                  </span>
                </label>
                <input
                  id="compliance-file-input"
                  type="file"
                  className="form-control"
                  accept=".pdf,.png,.jpg,.jpeg"
                  onChange={handleFileChange}
                  required
                />
                {fileError && (
                  <div style={{ color: "#ef4444", fontSize: "0.8rem", marginTop: 4 }}>
                    <i className="bi bi-exclamation-circle" style={{ marginRight: 4 }} />
                    {fileError}
                  </div>
                )}
                {file && !fileError && (
                  <div style={{ color: "#22c55e", fontSize: "0.8rem", marginTop: 4 }}>
                    <i className="bi bi-check-circle" style={{ marginRight: 4 }} />
                    {file.name} ({(file.size / 1024).toFixed(1)} KB)
                  </div>
                )}
              </div>

              <button
                type="submit"
                className="btn btn-primary"
                disabled={uploading || !!fileError}
                style={{ width: "100%" }}
              >
                {uploading
                  ? <><i className="bi bi-hourglass-split" /> Uploading...</>
                  : <><i className="bi bi-cloud-upload" /> Upload Document</>}
              </button>
            </form>
          </div>
        </div>

        {/* ── Current Documents ── */}
        <div>
          <div className="card" style={{ marginBottom: 16 }}>
            <div className="card-header">
              Current Documents
              <span className="badge badge-secondary" style={{ marginLeft: 8 }}>{docs.length}</span>
            </div>
            {docs.length === 0 ? (
              <div style={{ padding: 24, textAlign: "center", color: "var(--text-muted)", fontSize: "0.875rem" }}>
                No compliance documents uploaded yet.
              </div>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table className="table" style={{ fontSize: "0.82rem" }}>
                  <thead>
                    <tr>
                      <th>Type</th>
                      <th>File</th>
                      <th>Expiry</th>
                      <th>Validity</th>
                      <th>Ver.</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    {docs.map((d) => (
                      <tr key={d.id}>
                        <td style={{ fontWeight: 600 }}>{d.documentType}</td>
                        <td style={{ color: "var(--text-muted)", maxWidth: 120, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                          {d.fileName}
                        </td>
                        <td style={{ color: d.isExpired ? "#ef4444" : "var(--text)" }}>
                          {fmt.date(d.expiryDate)}
                        </td>
                        <td>
                          <DaysBadge days={d.daysUntilExpiry} expired={d.isExpired} />
                        </td>
                        <td>v{d.version}</td>
                        <td>
                          <button
                            className="btn btn-secondary btn-sm"
                            onClick={() => loadHistory(d.documentType)}
                            title="View version history"
                          >
                            <i className="bi bi-clock-history" />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* ── Version History Panel — AC #8 ── */}
          {historyDoc && (
            <div className="card">
              <div className="card-header" style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <span>
                  <i className="bi bi-clock-history" style={{ marginRight: 8 }} />
                  Version History — {historyDoc}
                </span>
                <button className="btn btn-secondary btn-sm" onClick={() => setHistoryDoc(null)}>
                  <i className="bi bi-x" />
                </button>
              </div>
              {historyLoading ? (
                <div style={{ padding: 16 }}><Spinner /></div>
              ) : (
                <div style={{ overflowX: "auto" }}>
                  <table className="table" style={{ fontSize: "0.82rem" }}>
                    <thead>
                      <tr>
                        <th>Version</th>
                        <th>File</th>
                        <th>Issue Date</th>
                        <th>Expiry Date</th>
                        <th>Uploaded</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {history.map((h) => (
                        <tr key={h.id} style={{ opacity: h.isLatest ? 1 : 0.6 }}>
                          <td>
                            <span style={{ fontWeight: 700 }}>v{h.version}</span>
                            {h.isLatest && (
                              <span className="badge badge-primary" style={{ marginLeft: 6, fontSize: "0.65rem" }}>
                                Latest
                              </span>
                            )}
                          </td>
                          <td style={{ color: "var(--text-muted)" }}>{h.fileName}</td>
                          <td>{fmt.date(h.issueDate)}</td>
                          <td style={{ color: h.isExpired ? "#ef4444" : "var(--text)" }}>
                            {fmt.date(h.expiryDate)}
                          </td>
                          <td>{fmt.datetime(h.uploadedAt)}</td>
                          <td>
                            {h.isExpired
                              ? <span className="badge badge-danger">Expired</span>
                              : h.isLatest
                                ? <span className="badge badge-success">Active</span>
                                : <span className="badge badge-secondary">Superseded</span>}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default VendorComplianceUploadPage;
