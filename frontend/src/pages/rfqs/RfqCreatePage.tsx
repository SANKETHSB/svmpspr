import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import { rfqAPI, vendorAPI } from '../../services/api';
import { Vendor, RfqItem } from '../../types';
import { PageHeader, Spinner } from '../../components/common/SharedComponents';

const blank = (): RfqItem => ({ itemName: '', description: '', quantity: 1, unit: '', specifications: '' });

const RfqCreatePage: React.FC = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id?: string }>();
  const isEdit = !!id;
  const [loading, setLoading] = useState(false);
  const [vendors, setVendors] = useState<Vendor[]>([]);
  const [form, setForm] = useState({ title: '', description: '', terms: '', deadline: '' });
  const [items, setItems] = useState<RfqItem[]>([blank()]);
  const [selectedVendors, setSelectedVendors] = useState<number[]>([]);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [vendorSearch, setVendorSearch] = useState('');

  useEffect(() => {
    vendorAPI.getApproved().then(r => setVendors(r.data.data || [])).catch(() => {});
    if (isEdit) {
      rfqAPI.getById(Number(id)).then(r => {
        const rfq = r.data.data;
        setForm({ title: rfq.title, description: rfq.description || '', terms: rfq.terms, deadline: rfq.deadline?.slice(0, 16) });
        setItems(rfq.items.map((i: RfqItem) => ({ ...i })));
        setSelectedVendors(rfq.invitedVendors?.map((v: Vendor) => v.id) || []);
      }).catch(() => toast.error('Failed to load RFQ'));
    }
  }, [id, isEdit]);

  const validate = () => {
    const e: Record<string, string> = {};
    if (!form.title.trim()) e.title = 'Title required';
    if (!form.terms.trim()) e.terms = 'Terms required';
    if (!form.deadline) e.deadline = 'Deadline required';
    else if (new Date(form.deadline) <= new Date()) e.deadline = 'Deadline must be future';
    if (!isEdit && selectedVendors.length === 0) e.vendors = 'Select at least 1 vendor';
    items.forEach((item, i) => { if (!item.itemName.trim()) e[`item_${i}`] = 'Name required'; });
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;
    setLoading(true);
    try {
      const payload = { ...form, deadline: new Date(form.deadline).toISOString(), items, invitedVendorIds: selectedVendors };
      if (isEdit) await rfqAPI.update(Number(id), payload);
      else await rfqAPI.create(payload);
      toast.success(isEdit ? 'RFQ updated!' : 'RFQ created and vendors notified!');
      navigate('/rfqs');
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to save RFQ');
    } finally { setLoading(false); }
  };

  const setItem = (i: number, k: keyof RfqItem, v: any) => {
    const updated = [...items]; (updated[i] as any)[k] = v; setItems(updated);
    setErrors(e => { const ne = { ...e }; delete ne[`item_${i}`]; return ne; });
  };

  const filteredVendors = vendors.filter(v =>
    v.companyName.toLowerCase().includes(vendorSearch.toLowerCase()) ||
    v.gstNumber.includes(vendorSearch));

  const minDate = new Date(Date.now() + 60 * 60 * 1000).toISOString().slice(0, 16);

  return (
    <div>
      <PageHeader title={isEdit ? 'Edit RFQ' : 'Create RFQ'}
        subtitle={isEdit ? 'Update RFQ details' : 'Issue a new Request for Quotation'}
        breadcrumb={[{ label: 'RFQs', href: '/rfqs' }, { label: isEdit ? 'Edit' : 'Create' }]} />

      <form onSubmit={handleSubmit}>
        <div style={{ display: 'grid', gridTemplateColumns: '1.6fr 1fr', gap: 16, alignItems: 'start' }}>
          {/* Left */}
          <div>
            {/* Basic Info */}
            <div className="card" style={{ marginBottom: 16 }}>
              <div className="card-header">RFQ Information</div>
              <div className="card-body">
                <div className="form-group">
                  <label className="form-label">Title *</label>
                  <input className={`form-control${errors.title ? ' is-invalid' : ''}`} placeholder="e.g. Office Supplies Q2 2026"
                    value={form.title} onChange={e => { setForm({ ...form, title: e.target.value }); setErrors({ ...errors, title: '' }); }} />
                  {errors.title && <div className="invalid-feedback" style={{ display: 'block' }}>{errors.title}</div>}
                </div>
                <div className="form-group">
                  <label className="form-label">Description</label>
                  <textarea className="form-control" rows={3} placeholder="Detailed procurement requirements..."
                    value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} />
                </div>
                <div className="form-group">
                  <label className="form-label">Terms & Conditions *</label>
                  <textarea className={`form-control${errors.terms ? ' is-invalid' : ''}`} rows={3}
                    placeholder="Payment terms, delivery requirements, warranties..."
                    value={form.terms} onChange={e => { setForm({ ...form, terms: e.target.value }); setErrors({ ...errors, terms: '' }); }} />
                  {errors.terms && <div className="invalid-feedback" style={{ display: 'block' }}>{errors.terms}</div>}
                </div>
                <div className="form-group">
                  <label className="form-label">Submission Deadline *</label>
                  <input type="datetime-local" className={`form-control${errors.deadline ? ' is-invalid' : ''}`}
                    min={minDate} value={form.deadline}
                    onChange={e => { setForm({ ...form, deadline: e.target.value }); setErrors({ ...errors, deadline: '' }); }} />
                  {errors.deadline && <div className="invalid-feedback" style={{ display: 'block' }}>{errors.deadline}</div>}
                </div>
              </div>
            </div>

            {/* Items */}
            <div className="card">
              <div className="card-header">
                Line Items
                <button type="button" className="btn btn-outline-primary btn-sm" onClick={() => setItems([...items, blank()])}>
                  <i className="bi bi-plus-circle" /> Add Item
                </button>
              </div>
              <div className="card-body" style={{ padding: 16 }}>
                {items.map((item, i) => (
                  <div key={i} style={{ border: '1px solid var(--border)', borderRadius: 10, padding: 14, marginBottom: 12, background: 'var(--surface2)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 10 }}>
                      <span style={{ fontWeight: 600, fontSize: '0.85rem', color: '#3b82f6' }}>Item #{i + 1}</span>
                      {items.length > 1 && (
                        <button type="button" className="btn btn-outline-danger btn-sm btn-icon" onClick={() => setItems(items.filter((_, j) => j !== i))}>
                          <i className="bi bi-trash3" />
                        </button>
                      )}
                    </div>
                    <div className="row">
                      <div className="col-6">
                        <div className="form-group">
                          <label className="form-label" style={{ fontSize: '0.78rem' }}>Item Name *</label>
                          <input className={`form-control${errors[`item_${i}`] ? ' is-invalid' : ''}`} placeholder="Item name"
                            value={item.itemName} onChange={e => setItem(i, 'itemName', e.target.value)} />
                          {errors[`item_${i}`] && <div className="invalid-feedback" style={{ display: 'block' }}>{errors[`item_${i}`]}</div>}
                        </div>
                      </div>
                      <div className="col-3">
                        <div className="form-group">
                          <label className="form-label" style={{ fontSize: '0.78rem' }}>Quantity *</label>
                          <input type="number" className="form-control" min={1} value={item.quantity}
                            onChange={e => setItem(i, 'quantity', parseInt(e.target.value) || 1)} />
                        </div>
                      </div>
                      <div className="col-3">
                        <div className="form-group">
                          <label className="form-label" style={{ fontSize: '0.78rem' }}>Unit</label>
                          <input className="form-control" placeholder="pcs, kg..." value={item.unit}
                            onChange={e => setItem(i, 'unit', e.target.value)} />
                        </div>
                      </div>
                      <div className="col-6">
                        <div className="form-group">
                          <label className="form-label" style={{ fontSize: '0.78rem' }}>Description</label>
                          <input className="form-control" placeholder="Short description" value={item.description}
                            onChange={e => setItem(i, 'description', e.target.value)} />
                        </div>
                      </div>
                      <div className="col-6">
                        <div className="form-group">
                          <label className="form-label" style={{ fontSize: '0.78rem' }}>Specifications</label>
                          <input className="form-control" placeholder="Technical specs" value={item.specifications}
                            onChange={e => setItem(i, 'specifications', e.target.value)} />
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Right - Vendors */}
          {!isEdit && (
            <div className="card" style={{ position: 'sticky', top: 80 }}>
              <div className="card-header">
                Invite Vendors *
                <span className="badge badge-primary">{selectedVendors.length} selected</span>
              </div>
              {errors.vendors && <div className="alert alert-danger" style={{ margin: 12 }}>{errors.vendors}</div>}
              <div style={{ padding: '10px 12px', borderBottom: '1px solid var(--border)' }}>
                <div className="search-input-wrap">
                  <i className="bi bi-search" />
                  <input className="form-control" placeholder="Search vendors..." value={vendorSearch}
                    onChange={e => setVendorSearch(e.target.value)} />
                </div>
              </div>
              <div style={{ maxHeight: 360, overflowY: 'auto' }}>
                {filteredVendors.length === 0 ? (
                  <div style={{ padding: 20, textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.85rem' }}>No approved vendors</div>
                ) : filteredVendors.map(v => (
                  <div key={v.id} onClick={() => setSelectedVendors(prev =>
                    prev.includes(v.id) ? prev.filter(x => x !== v.id) : [...prev, v.id]
                  )} style={{
                    display: 'flex', alignItems: 'center', gap: 10, padding: '10px 14px',
                    borderBottom: '1px solid var(--border)', cursor: 'pointer',
                    background: selectedVendors.includes(v.id) ? 'rgba(59,130,246,0.08)' : 'transparent',
                    transition: 'background 0.15s',
                  }}>
                    <input type="checkbox" readOnly checked={selectedVendors.includes(v.id)} style={{ cursor: 'pointer' }} />
                    <div>
                      <div style={{ fontWeight: 600, fontSize: '0.875rem' }}>{v.companyName}</div>
                      <div style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>{v.gstNumber}</div>
                    </div>
                  </div>
                ))}
              </div>
              <div style={{ padding: 14, borderTop: '1px solid var(--border)' }}>
                <button type="submit" className="btn btn-primary w-100" disabled={loading}>
                  {loading ? 'Saving...' : isEdit ? 'Update RFQ' : 'Create RFQ & Notify Vendors'}
                </button>
              </div>
            </div>
          )}
          {isEdit && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? 'Updating...' : 'Update RFQ'}
              </button>
              <button type="button" className="btn btn-secondary" onClick={() => navigate(-1)}>Cancel</button>
            </div>
          )}
        </div>
      </form>
    </div>
  );
};

export default RfqCreatePage;
