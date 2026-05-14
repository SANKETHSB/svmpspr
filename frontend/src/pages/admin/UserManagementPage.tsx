import React, { useEffect, useState, useCallback } from 'react';
import { toast } from 'react-toastify';
import { userAPI } from '../../services/api';
import { usePermissions } from '../../hooks/usePermissions';
import { PageHeader, Spinner, EmptyState, Pagination, StatusBadge, ConfirmModal, fmt } from '../../components/common/SharedComponents';

interface User { id: number; name: string; email: string; role: string; isActive: boolean; isLocked: boolean; lastLoginAt: string; createdAt: string; }

const ROLES = ['ADMIN', 'PROCUREMENT_MANAGER', 'COMPLIANCE_OFFICER'];

const UserManagementPage: React.FC = () => {
  const { canCreate, canUpdate, canDelete } = usePermissions();
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [total, setTotal] = useState(0);
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [editUser, setEditUser] = useState<User | null>(null);
  const [confirmDeactivate, setConfirmDeactivate] = useState<User | null>(null);
  const [form, setForm] = useState({ name: '', email: '', password: '', role: 'PROCUREMENT_MANAGER' });
  const [formLoading, setFormLoading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await userAPI.getAll({ search: search || undefined, role: roleFilter || undefined, page, size: 10 });
      const d = res.data.data;
      setUsers(d.content); setTotalPages(d.totalPages); setTotal(d.totalElements);
    } catch { toast.error('Failed to load users'); }
    finally { setLoading(false); }
  }, [search, roleFilter, page]);

  useEffect(() => { load(); }, [load]);

  const validate = () => {
    const e: Record<string, string> = {};
    if (!form.name.trim()) e.name = 'Name required';
    if (!form.email || !/\S+@\S+\.\S+/.test(form.email)) e.email = 'Valid email required';
    if (!editUser && (!form.password || form.password.length < 8)) e.password = 'Min 8 characters';
    if (!form.role) e.role = 'Role required';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = async (ev: React.FormEvent) => {
    ev.preventDefault();
    if (!validate()) return;
    setFormLoading(true);
    try {
      if (editUser) {
        const res = await userAPI.update(editUser.id, form);
        setUsers(prev => prev.map(u => u.id === editUser.id ? res.data.data : u));
        toast.success('User updated');
      } else {
        await userAPI.create(form);
        toast.success('User created');
        load();
      }
      setShowForm(false); setEditUser(null); setForm({ name: '', email: '', password: '', role: 'PROCUREMENT_MANAGER' });
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Operation failed');
    } finally { setFormLoading(false); }
  };

  const handleDeactivate = async (u: User) => {
    try { await userAPI.deactivate(u.id); setUsers(prev => prev.map(x => x.id === u.id ? { ...x, isActive: false } : x)); toast.success('User deactivated'); }
    catch (err: any) { toast.error(err.response?.data?.message || 'Failed'); }
  };

  const handleUnlock = async (id: number) => {
    try { await userAPI.unlock(id); setUsers(prev => prev.map(u => u.id === id ? { ...u, isLocked: false } : u)); toast.success('User unlocked'); }
    catch { toast.error('Failed to unlock'); }
  };

  const openEdit = (u: User) => { setEditUser(u); setForm({ name: u.name, email: u.email, password: '', role: u.role }); setShowForm(true); };

  const F = ({ k, label, type = 'text' }: { k: string; label: string; type?: string }) => (
    <div className="form-group">
      <label className="form-label">{label}</label>
      <input type={type} className={`form-control${errors[k] ? ' is-invalid' : ''}`}
        value={(form as any)[k]} onChange={e => { setForm(f => ({ ...f, [k]: e.target.value })); setErrors(er => { const ne = { ...er }; delete ne[k]; return ne; }); }} />
      {errors[k] && <div className="invalid-feedback" style={{ display: 'block' }}>{errors[k]}</div>}
    </div>
  );

  return (
    <div>
      <PageHeader title="User Management" subtitle={`${total} internal users`}
        actions={
          // AC #7: Only show Add User button if user has CREATE permission on USERS module
          canCreate('USERS') ? (
            <button className="btn btn-primary" onClick={() => { setEditUser(null); setForm({ name: '', email: '', password: '', role: 'PROCUREMENT_MANAGER' }); setErrors({}); setShowForm(true); }}>
              <i className="bi bi-person-plus" /> Add User
            </button>
          ) : undefined
        } />

      {/* Create/Edit Form */}
      {showForm && (
        <div className="card" style={{ marginBottom: 16, border: '1px solid #3b82f6' }}>
          <div className="card-header" style={{ color: '#3b82f6' }}>
            <i className="bi bi-person-gear" style={{ marginRight: 6 }} />
            {editUser ? 'Edit User' : 'Create New User'}
            <button className="btn btn-secondary btn-sm btn-icon" style={{ marginLeft: 'auto' }} onClick={() => setShowForm(false)}>
              <i className="bi bi-x-lg" />
            </button>
          </div>
          <div className="card-body">
            <form onSubmit={handleSubmit}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <F k="name" label="Full Name *" />
                <F k="email" label="Email Address *" type="email" />
                <F k="password" label={editUser ? 'Password (leave blank to keep)' : 'Password * (min 8 chars)'} type="password" />
                <div className="form-group">
                  <label className="form-label">Role *</label>
                  <select className={`form-control form-select${errors.role ? ' is-invalid' : ''}`}
                    value={form.role} onChange={e => setForm(f => ({ ...f, role: e.target.value }))}>
                    {ROLES.map(r => <option key={r} value={r}>{r.replace(/_/g, ' ')}</option>)}
                  </select>
                  {errors.role && <div className="invalid-feedback" style={{ display: 'block' }}>{errors.role}</div>}
                </div>
              </div>
              <div style={{ display: 'flex', gap: 8, marginTop: 4 }}>
                <button type="submit" className="btn btn-primary" disabled={formLoading}>
                  {formLoading ? 'Saving...' : (editUser ? 'Update User' : 'Create User')}
                </button>
                <button type="button" className="btn btn-secondary" onClick={() => setShowForm(false)}>Cancel</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Filters */}
      <div className="filter-bar">
        <div className="search-input-wrap" style={{ flex: 1 }}>
          <i className="bi bi-search" />
          <input className="form-control" placeholder="Search by name or email..."
            value={search} onChange={e => { setSearch(e.target.value); setPage(0); }} />
        </div>
        <select className="form-control form-select" style={{ width: 220 }} value={roleFilter}
          onChange={e => { setRoleFilter(e.target.value); setPage(0); }}>
          <option value="">All Roles</option>
          {ROLES.map(r => <option key={r} value={r}>{r.replace(/_/g, ' ')}</option>)}
        </select>
      </div>

      <div className="table-wrapper">
        {loading ? <Spinner /> : users.length === 0 ? (
          <EmptyState icon="bi-people" title="No users found" />
        ) : (
          <>
            <div style={{ overflowX: 'auto' }}>
              <table className="table">
                <thead>
                  <tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th>Last Login</th><th>Created</th><th>Actions</th></tr>
                </thead>
                <tbody>
                  {users.map(u => (
                    <tr key={u.id}>
                      <td style={{ fontWeight: 600 }}>{u.name}</td>
                      <td style={{ fontSize: '0.875rem' }}>{u.email}</td>
                      <td><StatusBadge status={u.role} label={u.role.replace(/_/g, ' ')} /></td>
                      <td>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                          <span className={`badge ${u.isActive ? 'badge-success' : 'badge-danger'}`}>
                            {u.isActive ? 'Active' : 'Deactivated'}
                          </span>
                          {u.isLocked && <span className="badge badge-warning">Locked</span>}
                        </div>
                      </td>
                      <td style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>{u.lastLoginAt ? fmt.datetime(u.lastLoginAt) : 'Never'}</td>
                      <td style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>{fmt.date(u.createdAt)}</td>
                      <td>
                        <div style={{ display: 'flex', gap: 4 }}>
                          {/* AC #7: Hide edit/delete buttons based on permissions */}
                          {canUpdate('USERS') && (
                            <button className="btn btn-secondary btn-sm btn-icon" title="Edit" onClick={() => openEdit(u)}>
                              <i className="bi bi-pencil" />
                            </button>
                          )}
                          {u.isLocked && (
                            <button className="btn btn-warning btn-sm btn-icon" title="Unlock" onClick={() => handleUnlock(u.id)}>
                              <i className="bi bi-unlock" />
                            </button>
                          )}
                          {u.isActive && canDelete('USERS') && (
                            <button className="btn btn-outline-danger btn-sm btn-icon" title="Deactivate" onClick={() => setConfirmDeactivate(u)}>
                              <i className="bi bi-person-x" />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 16px', borderTop: '1px solid var(--border)' }}>
              <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Showing {page * 10 + 1}–{Math.min((page + 1) * 10, total)} of {total}</span>
              <Pagination page={page} total={totalPages} onChange={setPage} />
            </div>
          </>
        )}
      </div>

      <ConfirmModal open={!!confirmDeactivate} title="Deactivate User"
        message={`Are you sure you want to deactivate ${confirmDeactivate?.name}? They will no longer be able to login.`}
        confirmText="Deactivate" danger
        onConfirm={() => confirmDeactivate && handleDeactivate(confirmDeactivate)}
        onClose={() => setConfirmDeactivate(null)} />
    </div>
  );
};

export default UserManagementPage;
