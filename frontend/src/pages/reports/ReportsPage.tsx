import React, { useState } from 'react';
import { exportAPI } from '../../services/api';
import { PageHeader } from '../../components/common/SharedComponents';
import ExportButton, { triggerDownload } from '../../components/common/ExportButton';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';

// AC #7: Standard file naming helper
const fname = (prefix: string, ext: string) => {
  const ts = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
  return `${prefix}_${ts}.${ext}`;
};

// ─── Section Card ─────────────────────────────────────────────────────────────
const Section: React.FC<{ title: string; icon: string; children: React.ReactNode }> = ({ title, icon, children }) => (
  <div className="card" style={{ marginBottom: 16 }}>
    <div className="card-header">
      <i className={`bi ${icon}`} style={{ marginRight: 8 }} />{title}
    </div>
    <div className="card-body">{children}</div>
  </div>
);

// ─── Filter Row ───────────────────────────────────────────────────────────────
const FilterRow: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', alignItems: 'flex-end', marginBottom: 12 }}>
    {children}
  </div>
);

const Field: React.FC<{ label: string; children: React.ReactNode }> = ({ label, children }) => (
  <div className="form-group" style={{ marginBottom: 0, minWidth: 160 }}>
    <label className="form-label" style={{ fontSize: '0.78rem' }}>{label}</label>
    {children}
  </div>
);

// ─── Main Page ────────────────────────────────────────────────────────────────
const ReportsPage: React.FC = () => {
  const { isAdmin, isManager, isCompliance } = useAuth();

  // Vendor filters — AC #4
  const [vSearch, setVSearch]     = useState('');
  const [vStatus, setVStatus]     = useState('');
  const [vCompliant, setVCompliant] = useState('');

  // RFQ filters — AC #4
  const [rSearch, setRSearch]     = useState('');
  const [rStatus, setRStatus]     = useState('');
  const [rfqId, setRfqId]         = useState('');

  // PO filters — AC #4
  const [pSearch, setPSearch]     = useState('');
  const [pVendorId, setPVendorId] = useState('');
  const [pStatus, setPStatus]     = useState('');
  const [pFrom, setPFrom]         = useState('');
  const [pTo, setPTo]             = useState('');

  // ── Vendor exports ──────────────────────────────────────────────────────────
  const vendorParams = () => ({
    search:    vSearch   || undefined,
    status:    vStatus   || undefined,
    compliant: vCompliant !== '' ? vCompliant : undefined,
  });

  // ── RFQ exports ─────────────────────────────────────────────────────────────
  const rfqParams = () => ({
    search: rSearch || undefined,
    status: rStatus || undefined,
  });

  // ── PO exports ──────────────────────────────────────────────────────────────
  const poParams = () => ({
    search:   pSearch   || undefined,
    vendorId: pVendorId || undefined,
    status:   pStatus   || undefined,
    dateFrom: pFrom     || undefined,
    dateTo:   pTo       || undefined,
  });

  return (
    <div>
      <PageHeader
        title="Procurement Reports"
        subtitle="Export vendor, RFQ, and purchase order data with active filters"
        breadcrumb={[{ label: 'Reports' }]}
      />

      {/* ── AC #1: Vendor List Export ── */}
      {(isAdmin || isManager || isCompliance) && (
        <Section title="Vendor List Export" icon="bi-people-fill">
          <FilterRow>
            <Field label="Search">
              <input className="form-control" style={{ width: 180 }} placeholder="Name / GST / Reg ID"
                value={vSearch} onChange={e => setVSearch(e.target.value)} />
            </Field>
            <Field label="Status">
              <select className="form-control form-select" style={{ width: 180 }}
                value={vStatus} onChange={e => setVStatus(e.target.value)}>
                <option value="">All Statuses</option>
                <option value="PENDING_APPROVAL">Pending Approval</option>
                <option value="APPROVED">Approved</option>
                <option value="REJECTED">Rejected</option>
                <option value="SUSPENDED">Suspended</option>
              </select>
            </Field>
            <Field label="Compliance">
              <select className="form-control form-select" style={{ width: 160 }}
                value={vCompliant} onChange={e => setVCompliant(e.target.value)}>
                <option value="">All</option>
                <option value="true">Compliant</option>
                <option value="false">Non-Compliant</option>
              </select>
            </Field>
          </FilterRow>
          <div style={{ display: 'flex', gap: 8 }}>
            <ExportButton
              label="Export CSV"
              icon="bi-filetype-csv"
              filename={fname('vendors', 'csv')}
              onExport={() => exportAPI.vendorsCsv(vendorParams())}
            />
            <ExportButton
              label="Export Excel"
              icon="bi-file-earmark-excel"
              filename={fname('vendors', 'xlsx')}
              onExport={() => exportAPI.vendorsExcel(vendorParams())}
              variant="success"
            />
          </div>
          <p style={{ marginTop: 8, fontSize: '0.78rem', color: 'var(--text-muted)' }}>
            Exports reflect active filters. Restricted fields (passwords, internal paths) are excluded.
          </p>
        </Section>
      )}

      {/* ── AC #2: RFQ Comparison Export ── */}
      {(isAdmin || isManager) && (
        <Section title="RFQ Quotation Comparison Report" icon="bi-file-earmark-bar-graph">
          <FilterRow>
            <Field label="RFQ ID *">
              <input className="form-control" style={{ width: 160 }} placeholder="Enter RFQ ID"
                type="number" value={rfqId} onChange={e => setRfqId(e.target.value)} />
            </Field>
          </FilterRow>
          <ExportButton
            label="Export Comparison PDF"
            icon="bi-filetype-pdf"
            filename={fname(`rfq_comparison_${rfqId || 'X'}`, 'pdf')}
            onExport={() => {
              if (!rfqId) { toast.error('Please enter an RFQ ID'); return Promise.reject(); }
              return exportAPI.rfqComparisonPdf(Number(rfqId));
            }}
            variant="primary"
          />
          <p style={{ marginTop: 8, fontSize: '0.78rem', color: 'var(--text-muted)' }}>
            Generates a PDF with all quotations ranked by price. Lowest bidder is highlighted.
          </p>
        </Section>
      )}

      {/* ── AC #3: RFQ List Export ── */}
      {(isAdmin || isManager || isCompliance) && (
        <Section title="RFQ List Export" icon="bi-file-earmark-text-fill">
          <FilterRow>
            <Field label="Search">
              <input className="form-control" style={{ width: 180 }} placeholder="Title / RFQ Number"
                value={rSearch} onChange={e => setRSearch(e.target.value)} />
            </Field>
            <Field label="Status">
              <select className="form-control form-select" style={{ width: 160 }}
                value={rStatus} onChange={e => setRStatus(e.target.value)}>
                <option value="">All Statuses</option>
                <option value="OPEN">Open</option>
                <option value="CLOSED">Closed</option>
                <option value="AWARDED">Awarded</option>
                <option value="ARCHIVED">Archived</option>
              </select>
            </Field>
          </FilterRow>
          <ExportButton
            label="Export RFQs CSV"
            icon="bi-filetype-csv"
            filename={fname('rfqs', 'csv')}
            onExport={() => exportAPI.rfqsCsv(rfqParams())}
          />
        </Section>
      )}

      {/* ── AC #3: PO History Export ── */}
      {(isAdmin || isManager) && (
        <Section title="Purchase Order History Export" icon="bi-bag-check-fill">
          <FilterRow>
            <Field label="Search">
              <input className="form-control" style={{ width: 180 }} placeholder="PO / RFQ Number"
                value={pSearch} onChange={e => setPSearch(e.target.value)} />
            </Field>
            <Field label="Vendor ID">
              <input className="form-control" style={{ width: 130 }} placeholder="Vendor ID"
                type="number" value={pVendorId} onChange={e => setPVendorId(e.target.value)} />
            </Field>
            <Field label="Status">
              <select className="form-control form-select" style={{ width: 160 }}
                value={pStatus} onChange={e => setPStatus(e.target.value)}>
                <option value="">All Statuses</option>
                <option value="GENERATED">Generated</option>
                <option value="SENT">Sent</option>
                <option value="RECEIVED">Received</option>
                <option value="CLOSED">Closed</option>
              </select>
            </Field>
            <Field label="From Date">
              <input type="datetime-local" className="form-control" style={{ width: 190 }}
                value={pFrom} onChange={e => setPFrom(e.target.value)} />
            </Field>
            <Field label="To Date">
              <input type="datetime-local" className="form-control" style={{ width: 190 }}
                value={pTo} onChange={e => setPTo(e.target.value)} />
            </Field>
          </FilterRow>
          <div style={{ display: 'flex', gap: 8 }}>
            <ExportButton
              label="Export CSV"
              icon="bi-filetype-csv"
              filename={fname('po_history', 'csv')}
              onExport={() => exportAPI.poHistoryCsv(poParams())}
            />
            <ExportButton
              label="Export Excel"
              icon="bi-file-earmark-excel"
              filename={fname('po_history', 'xlsx')}
              onExport={() => exportAPI.poHistoryExcel(poParams())}
              variant="success"
            />
          </div>
          <p style={{ marginTop: 8, fontSize: '0.78rem', color: 'var(--text-muted)' }}>
            All exports include timestamp metadata. Export actions are logged to the audit trail.
          </p>
        </Section>
      )}
    </div>
  );
};

export default ReportsPage;
