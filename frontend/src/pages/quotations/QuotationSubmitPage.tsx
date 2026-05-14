import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { rfqAPI, quotationAPI } from "../../services/api";
import { RFQ } from "../../types";
import {
  PageHeader,
  Spinner,
  fmt,
} from "../../components/common/SharedComponents";

const QuotationSubmitPage: React.FC = () => {
  const { rfqId } = useParams<{ rfqId: string }>();
  const navigate = useNavigate();
  const [rfq, setRfq] = useState<RFQ | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [quotationId, setQuotationId] = useState<number | null>(null);
  const [documents, setDocuments] = useState<any[]>([]);
  const [uploadingDoc, setUploadingDoc] = useState(false);
  const [finalizing, setFinalizing] = useState(false);
  const [form, setForm] = useState({
    totalAmount: "",
    taxPercentage: "18",
    currency: "INR",
    deliveryDays: "",
    notes: "",
  });
  const [itemPrices, setItemPrices] = useState<Record<number, string>>({});
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    rfqAPI
      .getById(Number(rfqId))
      .then((r) => {
        setRfq(r.data.data);
        const init: Record<number, string> = {};
        r.data.data.items?.forEach((item: any) => {
          if (item.id) init[item.id] = "";
        });
        setItemPrices(init);
      })
      .catch(() => toast.error("Failed to load RFQ"))
      .finally(() => setLoading(false));
  }, [rfqId]);

  const taxAmt =
    (parseFloat(form.totalAmount || "0") *
      parseFloat(form.taxPercentage || "0")) /
    100;
  const grandTotal = parseFloat(form.totalAmount || "0") + taxAmt;

  const validate = () => {
    const e: Record<string, string> = {};
    if (!form.totalAmount || parseFloat(form.totalAmount) <= 0)
      e.totalAmount = "Valid amount required";
    if (!form.deliveryDays || parseInt(form.deliveryDays) < 1)
      e.deliveryDays = "Min 1 day";
    if (
      parseFloat(form.taxPercentage) < 0 ||
      parseFloat(form.taxPercentage) > 100
    )
      e.taxPercentage = "0–100 only";
    rfq?.items?.forEach((item) => {
      if (!itemPrices[item.id!] || parseFloat(itemPrices[item.id!]) <= 0)
        e[`price_${item.id}`] = "Required";
    });
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = async (ev: React.FormEvent) => {
    ev.preventDefault();
    if (!validate()) {
      toast.error("Fix validation errors");
      return;
    }
    setSubmitting(true);
    try {
      const items = rfq!.items.map((item) => ({
        rfqItemId: item.id!,
        unitPrice: parseFloat(itemPrices[item.id!]),
      }));
      const response = await quotationAPI.submit({
        rfqId: Number(rfqId),
        totalAmount: parseFloat(form.totalAmount),
        taxPercentage: parseFloat(form.taxPercentage),
        currency: form.currency,
        deliveryDays: parseInt(form.deliveryDays),
        notes: form.notes,
        items,
      });
      const newQuotationId = response.data.data.id;
      setQuotationId(newQuotationId);
      toast.success(
        "Quotation created! Now upload supporting documents (required).",
      );
      // Load existing documents if any
      const docsResponse = await quotationAPI.getDocuments(newQuotationId);
      setDocuments(docsResponse.data.data);
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Submission failed");
    } finally {
      setSubmitting(false);
    }
  };

  const handleFileUpload = async (ev: React.ChangeEvent<HTMLInputElement>) => {
    if (!ev.target.files || ev.target.files.length === 0 || !quotationId)
      return;
    const file = ev.target.files[0];

    // Validate file size (max 10MB)
    if (file.size > 10 * 1024 * 1024) {
      toast.error("File size must be less than 10MB");
      return;
    }

    setUploadingDoc(true);
    try {
      await quotationAPI.uploadDocument(quotationId, file);
      toast.success("Document uploaded with integrity verification!");
      // Refresh documents list
      const docsResponse = await quotationAPI.getDocuments(quotationId);
      setDocuments(docsResponse.data.data);
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Upload failed");
    } finally {
      setUploadingDoc(false);
      ev.target.value = ""; // Reset file input
    }
  };

  const handleFinalize = async () => {
    if (!quotationId) return;
    if (documents.length === 0) {
      toast.error(
        "Please upload at least one supporting document before finalizing.",
      );
      return;
    }
    setFinalizing(true);
    try {
      await quotationAPI.finalize(quotationId);
      toast.success("Quotation finalized and submitted successfully!");
      navigate("/quotations");
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Finalization failed");
    } finally {
      setFinalizing(false);
    }
  };

  const ch = (k: string, v: string) => {
    setForm((f) => ({ ...f, [k]: v }));
    setErrors((e) => {
      const ne = { ...e };
      delete ne[k];
      return ne;
    });
  };

  if (loading) return <Spinner />;
  if (!rfq) return <div>RFQ not found</div>;
  if (rfq.status !== "OPEN")
    return (
      <div className="alert alert-warning" style={{ margin: 24 }}>
        This RFQ is not open for quotation submission. Status: {rfq.status}
      </div>
    );

  return (
    <div>
      <PageHeader
        title="Submit Quotation"
        subtitle={`For: ${rfq.title} (${rfq.rfqNumber})`}
        breadcrumb={[
          { label: "RFQs", href: "/rfqs" },
          { label: rfq.rfqNumber, href: `/rfqs/${rfq.id}` },
          { label: "Submit Quotation" },
        ]}
      />

      {/* US 06 Implementation Notice */}
      {!quotationId && (
        <div className="alert alert-info" style={{ marginBottom: 16 }}>
          <i className="bi bi-info-circle" style={{ marginRight: 8 }} />
          <strong>US 06: Secure Quotation Submission</strong> - Supporting
          documents are required before final submission. After creating your
          quotation, you'll upload documents with integrity verification
          (SHA-256).
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div
          style={{
            display: "grid",
            gridTemplateColumns: quotationId ? "1fr 1fr 1fr" : "1.6fr 1fr",
            gap: 16,
            alignItems: "start",
          }}
        >
          {/* Left */}
          <div>
            {/* Pricing Summary */}
            <div className="card" style={{ marginBottom: 16 }}>
              <div className="card-header">Pricing Details</div>
              <div className="card-body">
                <div
                  style={{
                    display: "grid",
                    gridTemplateColumns: "1fr 1fr 1fr 1fr",
                    gap: 12,
                  }}
                >
                  <div className="form-group">
                    <label className="form-label">Total Amount *</label>
                    <div className="input-group">
                      <span className="input-group-text">₹</span>
                      <input
                        type="number"
                        min="0.01"
                        step="0.01"
                        className={`form-control${errors.totalAmount ? " is-invalid" : ""}`}
                        placeholder="0.00"
                        value={form.totalAmount}
                        onChange={(e) => ch("totalAmount", e.target.value)}
                        disabled={!!quotationId}
                      />
                    </div>
                    {errors.totalAmount && (
                      <div
                        className="invalid-feedback"
                        style={{ display: "block" }}
                      >
                        {errors.totalAmount}
                      </div>
                    )}
                  </div>
                  <div className="form-group">
                    <label className="form-label">Tax %</label>
                    <input
                      type="number"
                      min="0"
                      max="100"
                      step="0.01"
                      className={`form-control${errors.taxPercentage ? " is-invalid" : ""}`}
                      value={form.taxPercentage}
                      onChange={(e) => ch("taxPercentage", e.target.value)}
                      disabled={!!quotationId}
                    />
                    {errors.taxPercentage && (
                      <div
                        className="invalid-feedback"
                        style={{ display: "block" }}
                      >
                        {errors.taxPercentage}
                      </div>
                    )}
                  </div>
                  <div className="form-group">
                    <label className="form-label">Currency</label>
                    <select
                      className="form-control form-select"
                      value={form.currency}
                      onChange={(e) => ch("currency", e.target.value)}
                      disabled={!!quotationId}
                    >
                      <option>INR</option>
                      <option>USD</option>
                      <option>EUR</option>
                      <option>GBP</option>
                    </select>
                  </div>
                  <div className="form-group">
                    <label className="form-label">Delivery (Days) *</label>
                    <input
                      type="number"
                      min="1"
                      className={`form-control${errors.deliveryDays ? " is-invalid" : ""}`}
                      placeholder="e.g. 14"
                      value={form.deliveryDays}
                      onChange={(e) => ch("deliveryDays", e.target.value)}
                      disabled={!!quotationId}
                    />
                    {errors.deliveryDays && (
                      <div
                        className="invalid-feedback"
                        style={{ display: "block" }}
                      >
                        {errors.deliveryDays}
                      </div>
                    )}
                  </div>
                </div>

                {/* Grand Total Box */}
                <div
                  style={{
                    background:
                      "linear-gradient(135deg,rgba(59,130,246,0.1),rgba(99,102,241,0.1))",
                    borderRadius: 12,
                    padding: 16,
                    marginTop: 8,
                    display: "flex",
                    justifyContent: "space-around",
                  }}
                >
                  {[
                    {
                      label: "Subtotal",
                      value: fmt.currency(
                        parseFloat(form.totalAmount || "0"),
                        form.currency,
                      ),
                    },
                    {
                      label: `Tax (${form.taxPercentage}%)`,
                      value: fmt.currency(taxAmt, form.currency),
                    },
                    {
                      label: "Grand Total",
                      value: fmt.currency(grandTotal, form.currency),
                      big: true,
                    },
                  ].map(({ label, value, big }) => (
                    <div key={label} style={{ textAlign: "center" }}>
                      <div
                        style={{
                          fontSize: "0.75rem",
                          color: "var(--text-muted)",
                          marginBottom: 4,
                        }}
                      >
                        {label}
                      </div>
                      <div
                        style={{
                          fontWeight: big ? 800 : 600,
                          fontSize: big ? "1.3rem" : "1rem",
                          color: big ? "#3b82f6" : "var(--text)",
                        }}
                      >
                        {value}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* Item-level pricing */}
            <div className="card" style={{ marginBottom: 16 }}>
              <div className="card-header">Item-level Unit Prices *</div>
              <div className="card-body">
                {rfq.items?.map((item) => (
                  <div
                    key={item.id}
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: 12,
                      padding: "8px 0",
                      borderBottom: "1px solid var(--border)",
                    }}
                  >
                    <div style={{ flex: 1 }}>
                      <div style={{ fontWeight: 600, fontSize: "0.875rem" }}>
                        {item.itemName}
                      </div>
                      <div
                        style={{
                          color: "var(--text-muted)",
                          fontSize: "0.75rem",
                        }}
                      >
                        {item.quantity} {item.unit}
                      </div>
                    </div>
                    <div style={{ width: 160 }}>
                      <div className="input-group">
                        <span className="input-group-text">₹</span>
                        <input
                          type="number"
                          min="0.01"
                          step="0.01"
                          className={`form-control${errors[`price_${item.id}`] ? " is-invalid" : ""}`}
                          placeholder="Unit price"
                          value={itemPrices[item.id!] || ""}
                          onChange={(e) => {
                            setItemPrices((prev) => ({
                              ...prev,
                              [item.id!]: e.target.value,
                            }));
                            setErrors((prev) => {
                              const ne = { ...prev };
                              delete ne[`price_${item.id}`];
                              return ne;
                            });
                          }}
                          disabled={!!quotationId}
                        />
                      </div>
                      {errors[`price_${item.id}`] && (
                        <div
                          style={{
                            color: "#ef4444",
                            fontSize: "0.75rem",
                            marginTop: 2,
                          }}
                        >
                          {errors[`price_${item.id}`]}
                        </div>
                      )}
                    </div>
                    <div
                      style={{
                        width: 120,
                        textAlign: "right",
                        fontSize: "0.875rem",
                        fontWeight: 600,
                      }}
                    >
                      {itemPrices[item.id!]
                        ? fmt.currency(
                            parseFloat(itemPrices[item.id!]) * item.quantity,
                            form.currency,
                          )
                        : "—"}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Notes */}
            <div className="card" style={{ marginBottom: 16 }}>
              <div className="card-header">Additional Notes</div>
              <div className="card-body">
                <textarea
                  className="form-control"
                  rows={3}
                  placeholder="Warranty terms, delivery conditions, special offers..."
                  value={form.notes}
                  onChange={(e) => ch("notes", e.target.value)}
                  disabled={!!quotationId}
                />
              </div>
            </div>

            {!quotationId && (
              <div style={{ display: "flex", gap: 10 }}>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={submitting}
                >
                  {submitting ? (
                    "Creating..."
                  ) : (
                    <>
                      <i className="bi bi-arrow-right-circle" /> Create
                      Quotation
                    </>
                  )}
                </button>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => navigate(-1)}
                >
                  Cancel
                </button>
              </div>
            )}
          </div>

          {/* Middle - Document Upload (shown after quotation created) */}
          {quotationId && (
            <div className="card" style={{ position: "sticky", top: 80 }}>
              <div
                className="card-header"
                style={{ background: "#10b981", color: "white" }}
              >
                <i className="bi bi-file-earmark-arrow-up" /> Upload Supporting
                Documents *
              </div>
              <div className="card-body">
                <div
                  className="alert alert-warning"
                  style={{ fontSize: "0.85rem", padding: "8px 12px" }}
                >
                  <i className="bi bi-exclamation-triangle" /> At least one
                  document is required before finalizing submission.
                </div>

                <div style={{ marginBottom: 16 }}>
                  <label
                    className="btn btn-outline-primary"
                    style={{ width: "100%", cursor: "pointer" }}
                  >
                    <i className="bi bi-cloud-upload" /> Choose File
                    <input
                      type="file"
                      style={{ display: "none" }}
                      onChange={handleFileUpload}
                      disabled={uploadingDoc}
                      accept=".pdf,.doc,.docx,.xls,.xlsx,.jpg,.jpeg,.png"
                    />
                  </label>
                  <div
                    style={{
                      fontSize: "0.75rem",
                      color: "var(--text-muted)",
                      marginTop: 4,
                    }}
                  >
                    Accepted: PDF, DOC, DOCX, XLS, XLSX, JPG, PNG (Max 10MB)
                  </div>
                </div>

                {uploadingDoc && (
                  <div style={{ textAlign: "center", padding: 16 }}>
                    <div className="spinner-border spinner-border-sm text-primary" />
                    <div style={{ fontSize: "0.85rem", marginTop: 8 }}>
                      Uploading & verifying integrity...
                    </div>
                  </div>
                )}

                <div style={{ marginTop: 16 }}>
                  <div
                    style={{
                      fontWeight: 600,
                      fontSize: "0.875rem",
                      marginBottom: 8,
                    }}
                  >
                    Uploaded Documents ({documents.length})
                  </div>
                  {documents.length === 0 ? (
                    <div
                      style={{
                        textAlign: "center",
                        padding: 16,
                        color: "var(--text-muted)",
                        fontSize: "0.85rem",
                      }}
                    >
                      No documents uploaded yet
                    </div>
                  ) : (
                    <div
                      style={{
                        display: "flex",
                        flexDirection: "column",
                        gap: 8,
                      }}
                    >
                      {documents.map((doc: any) => (
                        <div
                          key={doc.id}
                          style={{
                            padding: "8px 12px",
                            background: "var(--surface2)",
                            borderRadius: 8,
                            fontSize: "0.85rem",
                          }}
                        >
                          <div style={{ fontWeight: 600, marginBottom: 4 }}>
                            <i className="bi bi-file-earmark-check text-success" />{" "}
                            {doc.fileName}
                          </div>
                          <div
                            style={{
                              fontSize: "0.75rem",
                              color: "var(--text-muted)",
                            }}
                          >
                            {(doc.fileSize / 1024).toFixed(1)} KB •{" "}
                            {new Date(doc.uploadedAt).toLocaleString()}
                          </div>
                          <div
                            style={{
                              fontSize: "0.7rem",
                              color: "#10b981",
                              marginTop: 4,
                            }}
                          >
                            <i className="bi bi-shield-check" /> Integrity
                            verified (SHA-256)
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                <button
                  type="button"
                  className="btn btn-success"
                  style={{ width: "100%", marginTop: 16 }}
                  onClick={handleFinalize}
                  disabled={finalizing || documents.length === 0}
                >
                  {finalizing ? (
                    "Finalizing..."
                  ) : (
                    <>
                      <i className="bi bi-check-circle" /> Finalize & Submit
                    </>
                  )}
                </button>
                {documents.length === 0 && (
                  <div
                    style={{
                      fontSize: "0.75rem",
                      color: "#ef4444",
                      marginTop: 8,
                      textAlign: "center",
                    }}
                  >
                    Upload at least one document to enable finalization
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Right - RFQ Summary */}
          <div className="card" style={{ position: "sticky", top: 80 }}>
            <div className="card-header">RFQ Summary</div>
            <div className="card-body">
              <div style={{ display: "flex", flexDirection: "column", gap: 2 }}>
                <div style={{ fontSize: "0.8rem", color: "var(--text-muted)" }}>
                  RFQ Number
                </div>
                <span className="code-text" style={{ marginBottom: 12 }}>
                  {rfq.rfqNumber}
                </span>
                <div style={{ fontSize: "0.8rem", color: "var(--text-muted)" }}>
                  Deadline
                </div>
                <div
                  style={{
                    fontWeight: 600,
                    marginBottom: 12,
                    color:
                      new Date(rfq.deadline) < new Date()
                        ? "#ef4444"
                        : "var(--text)",
                  }}
                >
                  {fmt.datetime(rfq.deadline)}
                </div>
                <div
                  style={{
                    fontSize: "0.8rem",
                    color: "var(--text-muted)",
                    marginBottom: 4,
                  }}
                >
                  Items Required
                </div>
                {rfq.items?.map((item, i) => (
                  <div
                    key={i}
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      padding: "4px 0",
                      borderBottom: "1px solid var(--border)",
                      fontSize: "0.82rem",
                    }}
                  >
                    <span>{item.itemName}</span>
                    <span style={{ fontWeight: 600 }}>
                      {item.quantity} {item.unit}
                    </span>
                  </div>
                ))}
                <div style={{ marginTop: 12 }}>
                  <div
                    style={{
                      fontSize: "0.8rem",
                      color: "var(--text-muted)",
                      marginBottom: 4,
                    }}
                  >
                    Terms
                  </div>
                  <div
                    style={{
                      fontSize: "0.82rem",
                      background: "var(--surface2)",
                      borderRadius: 8,
                      padding: "8px 10px",
                      lineHeight: 1.5,
                    }}
                  >
                    {rfq.terms}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </form>
    </div>
  );
};

export default QuotationSubmitPage;
