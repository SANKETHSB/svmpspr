import React, { ReactNode } from 'react';

// ── Status Badge ─────────────────────────────────────────────────────────────
const BADGE_MAP: Record<string, string> = {
  PENDING_APPROVAL: 'badge-warning', APPROVED: 'badge-success',
  REJECTED: 'badge-danger', SUSPENDED: 'badge-secondary',
  OPEN: 'badge-success', CLOSED: 'badge-secondary',
  AWARDED: 'badge-primary', ARCHIVED: 'badge-secondary',
  SUBMITTED: 'badge-info', UNDER_EVALUATION: 'badge-warning',
  GENERATED: 'badge-primary', SENT: 'badge-info',
  RECEIVED: 'badge-warning', CLOSED_PO: 'badge-secondary',
  ADMIN: 'badge-purple', PROCUREMENT_MANAGER: 'badge-primary',
  COMPLIANCE_OFFICER: 'badge-info', VENDOR: 'badge-success',
};

export const StatusBadge: React.FC<{ status: string; label?: string }> = ({ status, label }) => (
  <span className={`badge ${BADGE_MAP[status] || 'badge-secondary'}`}>
    {(label || status).replace(/_/g, ' ')}
  </span>
);

// ── Loading Spinner ────────────────────────────────────────────────────────────
export const Spinner: React.FC<{ msg?: string }> = ({ msg }) => (
  <div className="spinner-center" style={{ flexDirection: 'column', gap: 12 }}>
    <div className="spinner-ring" />
    {msg && <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>{msg}</p>}
  </div>
);

// ── Empty State ────────────────────────────────────────────────────────────────
export const EmptyState: React.FC<{ icon?: string; title: string; desc?: string; action?: ReactNode }> = ({ icon = 'bi-inbox', title, desc, action }) => (
  <div className="empty-state">
    <i className={`bi ${icon}`} />
    <h6>{title}</h6>
    {desc && <p style={{ fontSize: '0.85rem' }}>{desc}</p>}
    {action && <div style={{ marginTop: 16 }}>{action}</div>}
  </div>
);

// ── Pagination ────────────────────────────────────────────────────────────────
export const Pagination: React.FC<{ page: number; total: number; onChange: (p: number) => void }> = ({ page, total, onChange }) => {
  if (total <= 1) return null;
  const pages = Array.from({ length: Math.min(total, 5) }, (_, i) => Math.max(0, Math.min(page - 2, total - 5)) + i);
  return (
    <div className="pagination">
      <button className="page-btn" disabled={page === 0} onClick={() => onChange(page - 1)}><i className="bi bi-chevron-left" /></button>
      {pages.map(p => <button key={p} className={`page-btn ${p === page ? 'active' : ''}`} onClick={() => onChange(p)}>{p + 1}</button>)}
      <button className="page-btn" disabled={page >= total - 1} onClick={() => onChange(page + 1)}><i className="bi bi-chevron-right" /></button>
    </div>
  );
};

// ── Confirm Modal ─────────────────────────────────────────────────────────────
export const ConfirmModal: React.FC<{
  open: boolean; title: string; message: string;
  confirmText?: string; danger?: boolean;
  onConfirm: () => void; onClose: () => void;
}> = ({ open, title, message, confirmText = 'Confirm', danger = false, onConfirm, onClose }) => {
  if (!open) return null;
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-box" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <span>{title}</span>
          <button className="btn btn-secondary btn-sm btn-icon" onClick={onClose}><i className="bi bi-x-lg" /></button>
        </div>
        <div className="modal-body"><p style={{ color: 'var(--text-muted)' }}>{message}</p></div>
        <div className="modal-footer">
          <button className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button className={`btn ${danger ? 'btn-danger' : 'btn-primary'}`} onClick={() => { onConfirm(); onClose(); }}>{confirmText}</button>
        </div>
      </div>
    </div>
  );
};

// ── Format Helpers ────────────────────────────────────────────────────────────
export const fmt = {
  date: (s: string) => s ? new Date(s).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '—',
  datetime: (s: string) => s ? new Date(s).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '—',
  currency: (n: number, c = 'INR') => new Intl.NumberFormat('en-IN', { style: 'currency', currency: c }).format(n),
  number: (n: number) => new Intl.NumberFormat('en-IN').format(n),
};

// ── Avatar ────────────────────────────────────────────────────────────────────
export const Avatar: React.FC<{ name: string; size?: number }> = ({ name, size = 34 }) => (
  <div style={{
    width: size, height: size, borderRadius: '50%',
    background: 'linear-gradient(135deg, #3b82f6, #8b5cf6)',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    color: '#fff', fontWeight: 700, fontSize: size * 0.35, flexShrink: 0,
  }}>
    {name?.charAt(0).toUpperCase() || '?'}
  </div>
);

// ── Info Row ──────────────────────────────────────────────────────────────────
export const InfoRow: React.FC<{ label: string; value: ReactNode }> = ({ label, value }) => (
  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 0', borderBottom: '1px solid var(--border)' }}>
    <span style={{ color: 'var(--text-muted)', fontSize: '0.82rem' }}>{label}</span>
    <span style={{ fontWeight: 600, fontSize: '0.875rem', textAlign: 'right', maxWidth: '60%' }}>{value || '—'}</span>
  </div>
);

// ── Page Header ───────────────────────────────────────────────────────────────
export const PageHeader: React.FC<{
  title: string; subtitle?: string;
  breadcrumb?: { label: string; href?: string }[];
  actions?: ReactNode;
}> = ({ title, subtitle, breadcrumb, actions }) => (
  <div className="page-header d-flex align-items-center justify-content-between">
    <div>
      {breadcrumb && (
        <div className="breadcrumb" style={{ marginBottom: 6 }}>
          {breadcrumb.map((b, i) => (
            <span key={i}>
              {b.href ? <a href={b.href}>{b.label}</a> : <span>{b.label}</span>}
              {i < breadcrumb.length - 1 && <i className="bi bi-chevron-right" style={{ fontSize: '0.65rem' }} />}
            </span>
          ))}
        </div>
      )}
      <h1 className="page-title">{title}</h1>
      {subtitle && <p className="page-subtitle">{subtitle}</p>}
    </div>
    {actions && <div className="d-flex gap-2">{actions}</div>}
  </div>
);
