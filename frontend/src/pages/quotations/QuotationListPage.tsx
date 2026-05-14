import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import { quotationAPI } from '../../services/api';
import { Quotation } from '../../types';
import { StatusBadge, Spinner, EmptyState, PageHeader, fmt } from '../../components/common/SharedComponents';
import { useAuth } from '../../context/AuthContext';

const QuotationListPage: React.FC = () => {
  const { isVendor, isManager, user } = useAuth();
  const [quotations, setQuotations] = useState<Quotation[]>([]);
  const [loading, setLoading] = useState(true);
  const [evalId, setEvalId] = useState<number | null>(null);
  const [evalForm, setEvalForm] = useState({ score: '', comment: '' });

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await quotationAPI.getByVendor(user!.id);
      setQuotations(res.data.data || []);
    } catch { toast.error('Failed to load quotations'); }
    finally { setLoading(false); }
  }, [user]);

  useEffect(() => { load(); }, [load]);

  const handleEvaluate = async (id: number) => {
    if (!evalForm.score || !evalForm.comment) { toast.error('Score and comment required'); return; }
    try {
      const res = await quotationAPI.evaluate(id, { score: parseFloat(evalForm.score), comment: evalForm.comment, award: false });
      setQuotations(prev => prev.map(q => q.id === id ? res.data.data : q));
      setEvalId(null);
      setEvalForm({ score: '', comment: '' });
      toast.success('Evaluation saved');
    } catch (err: any) { toast.error(err.response?.data?.message || 'Evaluation failed'); }
  };

  return (
    <div>
      <PageHeader title="Quotations" subtitle={`${quotations.length} quotations`} />
      <div className="table-wrapper">
        {loading ? <Spinner /> : quotations.length === 0 ? (
          <EmptyState icon="bi-receipt" title="No quotations yet"
            desc={isVendor ? 'Submit your first quotation from an RFQ' : 'No quotations received'}
            action={isVendor ? <Link to="/rfqs" className="btn btn-primary btn-sm">Browse RFQs</Link> : undefined} />
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table className="table">
              <thead>
                <tr><th>RFQ</th><th>Vendor</th><th>Total</th><th>Grand Total</th><th>Tax</th><th>Delivery</th><th>Score</th><th>Status</th><th>Submitted</th><th>Actions</th></tr>
              </thead>
              <tbody>
                {quotations.map(q => (
                  <React.Fragment key={q.id}>
                    <tr style={{ background: q.isAwarded ? 'rgba(34,197,94,0.05)' : undefined }}>
                      <td>
                        <Link to={`/rfqs/${q.rfqId}`} style={{ textDecoration: 'none' }}>
                          <span className="code-text">{q.rfqNumber}</span>
                        </Link>
                      </td>
                      <td style={{ fontWeight: 600, fontSize: '0.875rem' }}>
                        {q.vendorName}
                        {q.isAwarded && <div><span className="badge badge-success" style={{ fontSize: '0.65rem' }}>🏆 Winner</span></div>}
                      </td>
                      <td>{fmt.currency(q.totalAmount, q.currency)}</td>
                      <td style={{ fontWeight: 700, color: '#3b82f6' }}>{fmt.currency(q.grandTotal, q.currency)}</td>
                      <td style={{ fontSize: '0.82rem' }}>{q.taxPercentage}%</td>
                      <td style={{ fontSize: '0.82rem' }}>{q.deliveryDays} days</td>
                      <td>
                        {q.weightedScore != null
                          ? <span className="badge badge-info">{q.weightedScore}/100</span>
                          : <span style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>—</span>}
                      </td>
                      <td><StatusBadge status={q.status} /></td>
                      <td style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>{fmt.datetime(q.submittedAt)}</td>
                      <td>
                        <div style={{ display: 'flex', gap: 4 }}>
                          <Link to={`/rfqs/${q.rfqId}`} className="btn btn-outline-primary btn-sm btn-icon" title="View RFQ">
                            <i className="bi bi-eye" />
                          </Link>
                          {isVendor && q.status === 'SUBMITTED' && (
                            <Link to={`/quotations/submit/${q.rfqId}`} className="btn btn-secondary btn-sm btn-icon" title="Edit">
                              <i className="bi bi-pencil" />
                            </Link>
                          )}
                          {isManager && q.status === 'SUBMITTED' && (
                            <button className="btn btn-info btn-sm btn-icon" title="Evaluate"
                              onClick={() => { setEvalId(evalId === q.id ? null : q.id); setEvalForm({ score: '', comment: '' }); }}>
                              <i className="bi bi-star" />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                    {evalId === q.id && (
                      <tr>
                        <td colSpan={10}>
                          <div style={{ background: 'rgba(6,182,212,0.08)', borderRadius: 8, padding: 14 }}>
                            <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end', flexWrap: 'wrap' }}>
                              <div className="form-group" style={{ marginBottom: 0, minWidth: 120 }}>
                                <label className="form-label" style={{ fontSize: '0.78rem' }}>Score (0-100) *</label>
                                <input type="number" className="form-control" min={0} max={100} placeholder="85"
                                  value={evalForm.score} onChange={e => setEvalForm({ ...evalForm, score: e.target.value })} />
                              </div>
                              <div className="form-group" style={{ marginBottom: 0, flex: 1, minWidth: 200 }}>
                                <label className="form-label" style={{ fontSize: '0.78rem' }}>Evaluation Comment *</label>
                                <input type="text" className="form-control" placeholder="Evaluation notes..."
                                  value={evalForm.comment} onChange={e => setEvalForm({ ...evalForm, comment: e.target.value })} />
                              </div>
                              <button className="btn btn-info btn-sm" style={{ color: '#fff' }} onClick={() => handleEvaluate(q.id)}>Save Evaluation</button>
                              <button className="btn btn-secondary btn-sm" onClick={() => setEvalId(null)}>Cancel</button>
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
        )}
      </div>
    </div>
  );
};

export default QuotationListPage;
