import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { rfqAPI, quotationAPI, poAPI } from '../../services/api';
import { RFQ, Quotation } from '../../types';
import { PageHeader, Spinner, fmt } from '../../components/common/SharedComponents';

const POCreatePage: React.FC = () => {
  const { rfqId } = useParams<{ rfqId: string }>();
  const navigate = useNavigate();
  const [rfq, setRfq] = useState<RFQ | null>(null);
  const [winnerQ, setWinnerQ] = useState<Quotation | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState({ deliveryDate: '', shippingAddress: '', paymentTerms: '', specialInstructions: '' });
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    const load = async () => {
      try {
        const [rRes, qRes] = await Promise.all([rfqAPI.getById(Number(rfqId)), quotationAPI.getByRfq(Number(rfqId))]);
        setRfq(rRes.data.data);
        const winner = (qRes.data.data || []).find((q: Quotation) => q.isAwarded);
        setWinnerQ(winner || null);
        if (winner) setForm(f => ({ ...f, paymentTerms: '' }));
      } catch { toast.error('Failed to load data'); }
      finally { setLoading(false); }
    };
    load();
  }, [rfqId]);

  const validate = () => {
    const e: Record<string, string> = {};
    if (!form.deliveryDate) e.deliveryDate = 'Required';
    else if (new Date(form.deliveryDate) <= new Date()) e.deliveryDate = 'Must be future date';
    if (!form.shippingAddress.trim()) e.shippingAddress = 'Required';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = async (ev: React.FormEvent) => {
    ev.preventDefault();
    if (!validate()) return;
    setSubmitting(true);
    try {
      const res = await poAPI.generate(Number(rfqId), form);
      toast.success(`Purchase Order ${res.data.data.poNumber} generated!`);
      navigate(`/purchase-orders/${res.data.data.id}`);
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to generate PO');
    } finally { setSubmitting(false); }
  };

  const ch = (k: string, v: string) => { setForm(f => ({ ...f, [k]: v })); setErrors(e => { const ne = { ...e }; delete ne[k]; return ne; }); };

  if (loading) return <Spinner />;
  if (!rfq) return <div>RFQ not found</div>;
  if (rfq.status !== 'AWARDED') return (
    <div className="alert alert-warning" style={{ margin: 24 }}>
      RFQ must be AWARDED before generating a PO. Current status: {rfq.status}
    </div>
  );

  return (
    <div>
      <PageHeader title="Generate Purchase Order"
        subtitle={`For: ${rfq.title} (${rfq.rfqNumber})`}
        breadcrumb={[{ label: 'RFQs', href: '/rfqs' }, { label: rfq.rfqNumber, href: `/rfqs/${rfq.id}` }, { label: 'Generate PO' }]} />

      <form onSubmit={handleSubmit}>
        <div style={{ display: 'grid', gridTemplateColumns: '1.6fr 1fr', gap: 16, alignItems: 'start' }}>
          <div>
            <div className="card" style={{ marginBottom: 16 }}>
              <div className="card-header">Delivery & Shipping Details</div>
              <div className="card-body">
                <div className="form-group">
                  <label className="form-label">Delivery Date *</label>
                  <input type="date" className={`form-control${errors.deliveryDate ? ' is-invalid' : ''}`}
                    min={new Date(Date.now() + 86400000).toISOString().split('T')[0]}
                    value={form.deliveryDate} onChange={e => ch('deliveryDate', e.target.value)} />
                  {errors.deliveryDate && <div className="invalid-feedback" style={{ display: 'block' }}>{errors.deliveryDate}</div>}
                </div>
                <div className="form-group">
                  <label className="form-label">Shipping Address *</label>
                  <textarea className={`form-control${errors.shippingAddress ? ' is-invalid' : ''}`} rows={3}
                    placeholder="Full delivery address..."
                    value={form.shippingAddress} onChange={e => ch('shippingAddress', e.target.value)} />
                  {errors.shippingAddress && <div className="invalid-feedback" style={{ display: 'block' }}>{errors.shippingAddress}</div>}
                </div>
                <div className="form-group">
                  <label className="form-label">Payment Terms</label>
                  <input className="form-control" placeholder="e.g. Net 30 days after invoice"
                    value={form.paymentTerms} onChange={e => ch('paymentTerms', e.target.value)} />
                </div>
                <div className="form-group">
                  <label className="form-label">Special Instructions</label>
                  <textarea className="form-control" rows={2}
                    placeholder="Any special handling or delivery instructions..."
                    value={form.specialInstructions} onChange={e => ch('specialInstructions', e.target.value)} />
                </div>
              </div>
            </div>
            <div style={{ display: 'flex', gap: 10 }}>
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? 'Generating...' : <><i className="bi bi-file-earmark-plus" /> Generate Purchase Order</>}
              </button>
              <button type="button" className="btn btn-secondary" onClick={() => navigate(-1)}>Cancel</button>
            </div>
          </div>

          {/* Summary */}
          <div>
            {winnerQ && (
              <div className="card" style={{ marginBottom: 16, border: '1px solid #22c55e' }}>
                <div className="card-header" style={{ color: '#16a34a' }}>
                  <i className="bi bi-trophy-fill" style={{ marginRight: 6 }} />Winning Quotation
                </div>
                <div className="card-body">
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                    {[
                      { label: 'Vendor', value: winnerQ.vendorName },
                      { label: 'Subtotal', value: fmt.currency(winnerQ.totalAmount, winnerQ.currency) },
                      { label: `Tax (${winnerQ.taxPercentage}%)`, value: fmt.currency(winnerQ.taxAmount, winnerQ.currency) },
                      { label: 'Grand Total', value: <strong style={{ color: '#3b82f6', fontSize: '1.1rem' }}>{fmt.currency(winnerQ.grandTotal, winnerQ.currency)}</strong> },
                      { label: 'Delivery', value: `${winnerQ.deliveryDays} working days` },
                    ].map(({ label, value }) => (
                      <div key={label} style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: 6, borderBottom: '1px solid var(--border)', fontSize: '0.875rem' }}>
                        <span style={{ color: 'var(--text-muted)' }}>{label}</span>
                        <span>{value}</span>
                      </div>
                    ))}
                    {winnerQ.notes && (
                      <div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: 4 }}>Notes</div>
                        <div style={{ fontSize: '0.82rem' }}>{winnerQ.notes}</div>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            )}
            <div className="card">
              <div className="card-header">Award Info</div>
              <div className="card-body">
                <div style={{ fontSize: '0.875rem' }}>
                  <div style={{ marginBottom: 8 }}><strong>Awarded Vendor:</strong> {rfq.awardedVendorName}</div>
                  {rfq.awardReason && <div><strong>Award Reason:</strong> {rfq.awardReason}</div>}
                </div>
              </div>
            </div>
          </div>
        </div>
      </form>
    </div>
  );
};

export default POCreatePage;
