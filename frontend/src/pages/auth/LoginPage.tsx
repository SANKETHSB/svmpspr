import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { useAuth } from '../../context/AuthContext';
import { authAPI } from '../../services/api';

const LoginPage: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: '', password: '' });
  const [showPwd, setShowPwd] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const validate = () => {
    const e: Record<string, string> = {};
    if (!form.email) e.email = 'Email is required';
    else if (!/\S+@\S+\.\S+/.test(form.email)) e.email = 'Invalid email format';
    if (!form.password) e.password = 'Password is required';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = async (ev: React.FormEvent) => {
    ev.preventDefault();
    if (!validate()) return;
    setLoading(true);
    try {
      const res = await authAPI.login(form);
      const data = res.data.data;
      login({ ...data, accessToken: data.accessToken });
      toast.success(`Welcome back, ${data.name}!`);
      navigate('/dashboard');
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Login failed. Check credentials.');
    } finally {
      setLoading(false);
    }
  };

  const fillDemo = (email: string, password: string) => setForm({ email, password });

  return (
    <div className="login-page">
      <div className="login-card">
        {/* Logo */}
        <div style={{ textAlign: 'center', marginBottom: 28 }}>
          <div className="login-logo"><i className="bi bi-building-check" /></div>
          <h2 style={{ fontWeight: 800, fontSize: '1.4rem', marginBottom: 4, color: 'var(--text)' }}>
            Smart Vendor PMS
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', margin: 0 }}>
            Infosys Limited — Procurement Platform
          </p>
        </div>

        <form onSubmit={handleSubmit}>
          {/* Email */}
          <div className="form-group">
            <label className="form-label">Email Address</label>
            <div className="input-group">
              <span className="input-group-text"><i className="bi bi-envelope" /></span>
              <input type="email" className={`form-control${errors.email ? ' is-invalid' : ''}`}
                placeholder="you@company.com" value={form.email}
                onChange={e => { setForm({ ...form, email: e.target.value }); setErrors({ ...errors, email: '' }); }} />
            </div>
            {errors.email && <div className="invalid-feedback" style={{ display: 'block' }}>{errors.email}</div>}
          </div>

          {/* Password */}
          <div className="form-group">
            <label className="form-label">Password</label>
            <div className="input-group">
              <span className="input-group-text"><i className="bi bi-lock" /></span>
              <input type={showPwd ? 'text' : 'password'}
                className={`form-control${errors.password ? ' is-invalid' : ''}`}
                style={{ borderRight: 'none' }}
                placeholder="Enter password" value={form.password}
                onChange={e => { setForm({ ...form, password: e.target.value }); setErrors({ ...errors, password: '' }); }} />
              <span className="input-group-text" style={{ cursor: 'pointer', borderLeft: '1px solid var(--input-border)' }}
                onClick={() => setShowPwd(!showPwd)}>
                <i className={`bi bi-eye${showPwd ? '-slash' : ''}`} />
              </span>
            </div>
            {errors.password && <div className="invalid-feedback" style={{ display: 'block' }}>{errors.password}</div>}
          </div>

          {/* Submit */}
          <button type="submit" className="btn btn-primary w-100" style={{ marginTop: 8, padding: '11px' }} disabled={loading}>
            {loading ? <><span style={{ width: 16, height: 16, border: '2px solid rgba(255,255,255,0.3)', borderTopColor: '#fff', borderRadius: '50%', display: 'inline-block', animation: 'spin 0.7s linear infinite', marginRight: 8 }} />Signing in...</> : 'Sign In'}
          </button>
        </form>

        {/* Register link */}
        <p style={{ textAlign: 'center', marginTop: 20, fontSize: '0.875rem', color: 'var(--text-muted)' }}>
          New vendor? <Link to="/register" style={{ color: '#3b82f6', fontWeight: 600, textDecoration: 'none' }}>Register here</Link>
        </p>

        {/* Demo credentials */}
        <div style={{ marginTop: 20, padding: 14, background: 'var(--surface2)', borderRadius: 10, border: '1px solid var(--border)' }}>
          <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600, marginBottom: 8 }}>
            <i className="bi bi-info-circle" style={{ marginRight: 4 }} />DEMO CREDENTIALS
          </p>
          {[
            { label: '🔑 Admin', email: 'admin@svpms.com', pwd: 'Admin@123456' },
            { label: '📋 Manager', email: 'manager@svpms.com', pwd: 'Manager@123456' },
            { label: '🏭 Vendor', email: 'vendor@techsupply.com', pwd: 'Vendor@123456' },
          ].map(d => (
            <button key={d.email} onClick={() => fillDemo(d.email, d.pwd)} style={{
              display: 'block', width: '100%', textAlign: 'left', padding: '5px 8px',
              background: 'transparent', border: 'none', cursor: 'pointer',
              color: 'var(--text-muted)', fontSize: '0.78rem', borderRadius: 6,
              transition: 'background 0.15s',
            }}
              onMouseEnter={e => (e.currentTarget.style.background = 'var(--hover-bg)')}
              onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}>
              <span style={{ fontWeight: 600 }}>{d.label}</span> — {d.email}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
