import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { vendorAPI } from '../../services/api';

const RegisterVendorPage: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState({
    companyName: '', email: '', password: '', confirmPassword: '',
    gstNumber: '', registrationId: '', phone: '', address: '', contactPerson: '',
  });
  const [errors, setErrors] = useState<Record<string, string>>({});

  const set = (k: string, v: string) => { setForm(f => ({ ...f, [k]: v })); setErrors(e => ({ ...e, [k]: '' })); };

  const validate = () => {
    const e: Record<string, string> = {};
    if (!form.companyName) e.companyName = 'Required';
    if (!form.email || !/\S+@\S+\.\S+/.test(form.email)) e.email = 'Valid email required';
    if (!form.password || form.password.length < 8) e.password = 'Min 8 characters';
    if (form.password !== form.confirmPassword) e.confirmPassword = 'Passwords do not match';
    if (!form.gstNumber || form.gstNumber.length !== 15) e.gstNumber = 'GST must be exactly 15 characters';
    if (!form.registrationId) e.registrationId = 'Required';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = async (ev: React.FormEvent) => {
    ev.preventDefault();
    if (!validate()) return;
    setLoading(true);
    try {
      const { confirmPassword, ...payload } = form;
      await vendorAPI.register(payload);
      toast.success('Registration submitted! Await admin approval to login.');
      navigate('/login');
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Registration failed.');
    } finally { setLoading(false); }
  };

  const F = ({ name, label, type = 'text', placeholder = '' }: { name: string; label: string; type?: string; placeholder?: string }) => (
    <div className="form-group">
      <label className="form-label">{label}</label>
      <input type={type} className={`form-control${errors[name] ? ' is-invalid' : ''}`}
        placeholder={placeholder || label} value={(form as any)[name]}
        onChange={e => set(name, e.target.value)} />
      {errors[name] && <div className="invalid-feedback" style={{ display: 'block' }}>{errors[name]}</div>}
    </div>
  );

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg)', padding: '32px 16px' }}>
      <div style={{ maxWidth: 720, margin: '0 auto' }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <div style={{ width: 52, height: 52, background: 'linear-gradient(135deg,#3b82f6,#6366f1)', borderRadius: 14, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '1.4rem', color: '#fff', margin: '0 auto 12px' }}>
            <i className="bi bi-building-add" />
          </div>
          <h2 style={{ fontWeight: 800, color: 'var(--text)' }}>Vendor Registration</h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>Register to participate in procurement processes</p>
        </div>

        <div className="card">
          <form onSubmit={handleSubmit}>
            <div className="card-header">Company Information</div>
            <div className="card-body">
              <div className="row">
                <div className="col-6"><F name="companyName" label="Company Name *" /></div>
                <div className="col-6"><F name="contactPerson" label="Contact Person" /></div>
                <div className="col-6"><F name="gstNumber" label="GST Number * (15 chars)" placeholder="22AAAAA0000A1Z5" /></div>
                <div className="col-6"><F name="registrationId" label="Registration ID *" /></div>
                <div className="col-6"><F name="phone" label="Phone Number" type="tel" /></div>
                <div className="col-6"><F name="email" label="Email Address *" type="email" /></div>
                <div className="col-12">
                  <div className="form-group">
                    <label className="form-label">Address</label>
                    <textarea className="form-control" rows={2} placeholder="Full business address"
                      value={form.address} onChange={e => set('address', e.target.value)} />
                  </div>
                </div>
              </div>
            </div>

            <div className="card-header">Login Credentials</div>
            <div className="card-body">
              <div className="row">
                <div className="col-6"><F name="password" label="Password * (min 8 chars)" type="password" /></div>
                <div className="col-6"><F name="confirmPassword" label="Confirm Password *" type="password" /></div>
              </div>
              <div style={{ display: 'flex', gap: 12, marginTop: 8 }}>
                <button type="submit" className="btn btn-primary" disabled={loading}>
                  {loading ? 'Registering...' : 'Register Vendor'}
                </button>
                <Link to="/login" className="btn btn-secondary">Back to Login</Link>
              </div>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default RegisterVendorPage;
