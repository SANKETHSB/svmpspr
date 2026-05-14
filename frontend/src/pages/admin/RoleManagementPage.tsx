import React, { useEffect, useState, useCallback } from 'react';
import { toast } from 'react-toastify';
import { roleAPI, userAPI } from '../../services/api';
import { Role, ModulePermission, RoleAssignmentHistory, SYSTEM_MODULES } from '../../types';
import { PageHeader, Spinner, ConfirmModal, fmt } from '../../components/common/SharedComponents';

// ─── helpers ─────────────────────────────────────────────────────────────────
const emptyPerms = (): ModulePermission[] =>
  SYSTEM_MODULES.map(m => ({ module: m, canCreate: false, canRead: false, canUpdate: false, canDelete: false }));

const CrudBadge: React.FC<{ label: string; active: boolean }> = ({ label, active }) => (
  <span className={`badge ${active ? 'badge-success' : 'badge-secondary'}`}
    style={{ fontSize: '0.65rem', marginRight: 2 }}>
    {label}
  </span>
);

// ─── Permission Matrix Editor ─────────────────────────────────────────────────
const PermissionMatrix: React.FC<{
  perms: ModulePermission[];
  onChange: (perms: ModulePermission[]) => void;
  readOnly?: boolean;
}> = ({ perms, onChange, readOnly }) => {
  const toggle = (module: string, field: keyof ModulePermission) => {
    if (readOnly) return;
    onChange(perms.map(p =>
      p.module === module ? { ...p, [field]: !(p[field] as boolean) } : p
    ));
  };

  const toggleAll = (module: string, value: boolean) => {
    if (readOnly) return;
    onChange(perms.map(p =>
      p.module === module
        ? { ...p, canCreate: value, canRead: value, canUpdate: value, canDelete: value }
        : p
    ));
  };

  return (
    <div style={{ overflowX: 'auto' }}>
      <table className="table" style={{ fontSize: '0.82rem' }}>
        <thead>
          <tr>
            <th style={{ width: 160 }}>Module</th>
            <th style={{ textAlign: 'center' }}>Create</th>
            <th style={{ textAlign: 'center' }}>Read</th>
            <th style={{ textAlign: 'center' }}>Update</th>
            <th style={{ textAlign: 'center' }}>Delete</th>
            {!readOnly && <th style={{ textAlign: 'center' }}>All</th>}
          </tr>
        </thead>
        <tbody>
          {perms.map(p => {
            const allOn = p.canCreate && p.canRead && p.canUpdate && p.canDelete;
            return (
              <tr key={p.module}>
                <td style={{ fontWeight: 600 }}>{p.module.replace(/_/g, ' ')}</td>
                {(['canCreate', 'canRead', 'canUpdate', 'canDelete'] as const).map(f => (
                  <td key={f} style={{ textAlign: 'center' }}>
                    <input
                      type="checkbox"
                      checked={!!p[f]}
                      onChange={() => toggle(p.module, f)}
                      disabled={readOnly}
                      style={{ cursor: readOnly ? 'default' : 'pointer', width: 16, height: 16 }}
                    />
                  </td>
                ))}
                {!readOnly && (
                  <td style={{ textAlign: 'center' }}>
                    <input
                      type="checkbox"
                      checked={allOn}
                      onChange={() => toggleAll(p.module, !allOn)}
                      style={{ cursor: 'pointer', width: 16, height: 16 }}
                      title="Toggle all"
                    />
                  </td>
                )}
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
};

// ─── Main Page ────────────────────────────────────────────────────────────────
const RoleManagementPage: React.FC = () => {
  const [roles, setRoles]           = useState<Role[]>([]);
  const [loading, setLoading]       = useState(true);
  const [selected, setSelected]     = useState<Role | null>(null);
  const [showForm, setShowForm]     = useState(false);
  const [editRole, setEditRole]     = useState<Role | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Role | null>(null);
  const [activeTab, setActiveTab]   = useState<'roles' | 'history'>('roles');
  const [history, setHistory]       = useState<RoleAssignmentHistory[]>([]);
  const [histLoading, setHistLoading] = useState(false);
  const [saving, setSaving]         = useState(false);

  // Form state
  const [formName, setFormName]     = useState('');
  const [formDesc, setFormDesc]     = useState('');
  const [formPerms, setFormPerms]   = useState<ModulePermission[]>(emptyPerms());

  // Assign role state
  const [showAssign, setShowAssign] = useState(false);
  const [assignUserId, setAssignUserId] = useState('');
  const [assignRole, setAssignRole] = useState('');
  const [assignReason, setAssignReason] = useState('');
  const [users, setUsers]           = useState<any[]>([]);

  const loadRoles = useCallback(async () => {
    setLoading(true);
    try {
      const res = await roleAPI.getAll();
      setRoles(res.data.data || []);
    } catch { toast.error('Failed to load roles'); }
    finally { setLoading(false); }
  }, []);

  const loadHistory = useCallback(async () => {
    setHistLoading(true);
    try {
      const res = await roleAPI.getAssignmentHistory();
      setHistory(res.data.data || []);
    } catch { toast.error('Failed to load history'); }
    finally { setHistLoading(false); }
  }, []);

  const loadUsers = useCallback(async () => {
    try {
      const res = await userAPI.getAll({ size: 100 });
      setUsers(res.data.data?.content || []);
    } catch {}
  }, []);

  useEffect(() => { loadRoles(); }, [loadRoles]);
  useEffect(() => { if (activeTab === 'history') loadHistory(); }, [activeTab, loadHistory]);

  const openCreate = () => {
    setEditRole(null);
    setFormName(''); setFormDesc('');
    setFormPerms(emptyPerms());
    setShowForm(true);
  };

  const openEdit = (role: Role) => {
    setEditRole(role);
    setFormName(role.name);
    setFormDesc(role.description || '');
    // Merge existing perms with all modules
    const merged = SYSTEM_MODULES.map(m => {
      const existing = role.permissions?.find(p => p.module === m);
      return existing || { module: m, canCreate: false, canRead: false, canUpdate: false, canDelete: false };
    });
    setFormPerms(merged);
    setShowForm(true);
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formName.trim()) { toast.error('Role name is required'); return; }
    setSaving(true);
    try {
      const payload = { name: formName.trim(), description: formDesc, permissions: formPerms };
      if (editRole) {
        await roleAPI.update(editRole.id, payload);
        toast.success('Role updated');
      } else {
        await roleAPI.create(payload);
        toast.success('Role created');
      }
      setShowForm(false);
      await loadRoles();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Save failed');
    } finally { setSaving(false); }
  };

  // AC #5: Delete with confirmation
  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await roleAPI.delete(deleteTarget.id, true);
      toast.success(`Role "${deleteTarget.name}" deleted`);
      setDeleteTarget(null);
      if (selected?.id === deleteTarget.id) setSelected(null);
      await loadRoles();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Delete failed');
      setDeleteTarget(null);
    }
  };

  const openAssign = async () => {
    await loadUsers();
    setAssignUserId(''); setAssignRole(''); setAssignReason('');
    setShowAssign(true);
  };

  const handleAssign = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!assignUserId || !assignRole) { toast.error('User and role are required'); return; }
    try {
      await roleAPI.assignRole(Number(assignUserId), assignRole, assignReason);
      toast.success('Role assigned successfully');
      setShowAssign(false);
      await loadHistory();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Assignment failed');
    }
  };

  return (
    <div>
      <PageHeader
        title="Role & Permission Management"
        subtitle="Configure roles and granular module-level permissions"
        breadcrumb={[{ label: 'Role Management' }]}
        actions={
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn btn-secondary btn-sm" onClick={openAssign}>
              <i className="bi bi-person-badge" /> Assign Role
            </button>
            <button className="btn btn-primary btn-sm" onClick={openCreate}>
              <i className="bi bi-plus-circle" /> New Role
            </button>
          </div>
        }
      />

      {/* Tabs */}
      <div style={{ display: 'flex', gap: 0, marginBottom: 16, borderBottom: '2px solid var(--border)' }}>
        {(['roles', 'history'] as const).map(t => (
          <button key={t} onClick={() => setActiveTab(t)}
            className="btn btn-sm"
            style={{
              borderRadius: 0, borderBottom: activeTab === t ? '2px solid #3b82f6' : 'none',
              color: activeTab === t ? '#3b82f6' : 'var(--text-muted)',
              fontWeight: activeTab === t ? 700 : 400, background: 'none', marginBottom: -2,
            }}>
            {t === 'roles' ? 'Roles & Permissions' : 'Assignment History'}
          </button>
        ))}
      </div>

      {/* ── ROLES TAB ── */}
      {activeTab === 'roles' && (
        <>
          {/* Create/Edit Form */}
          {showForm && (
            <div className="card" style={{ marginBottom: 16, border: '1px solid #3b82f6' }}>
              <div className="card-header" style={{ color: '#3b82f6', display: 'flex', justifyContent: 'space-between' }}>
                <span><i className="bi bi-shield-lock" style={{ marginRight: 6 }} />
                  {editRole ? `Edit Role: ${editRole.name}` : 'Create New Role'}
                </span>
                <button className="btn btn-secondary btn-sm btn-icon" onClick={() => setShowForm(false)}>
                  <i className="bi bi-x-lg" />
                </button>
              </div>
              <div className="card-body">
                <form onSubmit={handleSave}>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 16 }}>
                    <div className="form-group">
                      <label className="form-label">Role Name *</label>
                      <input className="form-control" value={formName}
                        onChange={e => setFormName(e.target.value)}
                        placeholder="e.g. FINANCE_REVIEWER"
                        disabled={editRole?.isSystem}
                        required />
                      {editRole?.isSystem && (
                        <small style={{ color: '#f59e0b' }}>
                          <i className="bi bi-shield-fill" /> System role — name is protected
                        </small>
                      )}
                    </div>
                    <div className="form-group">
                      <label className="form-label">Description</label>
                      <input className="form-control" value={formDesc}
                        onChange={e => setFormDesc(e.target.value)}
                        placeholder="Brief description of this role" />
                    </div>
                  </div>

                  <div style={{ marginBottom: 12 }}>
                    <label className="form-label" style={{ fontWeight: 600 }}>
                      Module Permissions (CRUD)
                    </label>
                    <PermissionMatrix perms={formPerms} onChange={setFormPerms} />
                  </div>

                  <div style={{ display: 'flex', gap: 8 }}>
                    <button type="submit" className="btn btn-primary" disabled={saving}>
                      {saving ? 'Saving...' : editRole ? 'Update Role' : 'Create Role'}
                    </button>
                    <button type="button" className="btn btn-secondary" onClick={() => setShowForm(false)}>
                      Cancel
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}

          {/* Roles List */}
          {loading ? <Spinner /> : (
            <div style={{ display: 'grid', gridTemplateColumns: selected ? '1fr 1.5fr' : '1fr', gap: 16 }}>
              {/* Role cards */}
              <div className="card">
                <div className="card-header">
                  All Roles
                  <span className="badge badge-secondary" style={{ marginLeft: 8 }}>{roles.length}</span>
                </div>
                <div style={{ overflowX: 'auto' }}>
                  <table className="table">
                    <thead>
                      <tr><th>Name</th><th>Type</th><th>Modules</th><th>Actions</th></tr>
                    </thead>
                    <tbody>
                      {roles.map(r => (
                        <tr key={r.id}
                          style={{ cursor: 'pointer', background: selected?.id === r.id ? 'var(--hover-bg)' : undefined }}
                          onClick={() => setSelected(selected?.id === r.id ? null : r)}>
                          <td style={{ fontWeight: 600 }}>
                            {r.name.replace(/_/g, ' ')}
                            {r.isSystem && (
                              <span className="badge badge-warning" style={{ marginLeft: 6, fontSize: '0.65rem' }}>
                                System
                              </span>
                            )}
                          </td>
                          <td>
                            <span className={`badge ${r.isSystem ? 'badge-warning' : 'badge-primary'}`}>
                              {r.isSystem ? 'Protected' : 'Custom'}
                            </span>
                          </td>
                          <td style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                            {r.permissions?.length || 0} modules
                          </td>
                          <td onClick={e => e.stopPropagation()}>
                            <div style={{ display: 'flex', gap: 4 }}>
                              <button className="btn btn-secondary btn-sm btn-icon" title="Edit"
                                onClick={() => openEdit(r)}>
                                <i className="bi bi-pencil" />
                              </button>
                              {!r.isSystem && (
                                <button className="btn btn-outline-danger btn-sm btn-icon" title="Delete"
                                  onClick={() => setDeleteTarget(r)}>
                                  <i className="bi bi-trash" />
                                </button>
                              )}
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Permission detail panel */}
              {selected && (
                <div className="card">
                  <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span>
                      <i className="bi bi-shield-lock" style={{ marginRight: 8 }} />
                      {selected.name.replace(/_/g, ' ')} — Permissions
                    </span>
                    <button className="btn btn-secondary btn-sm btn-icon" onClick={() => setSelected(null)}>
                      <i className="bi bi-x" />
                    </button>
                  </div>
                  <div className="card-body" style={{ padding: '8px 0' }}>
                    {selected.description && (
                      <p style={{ padding: '0 16px 8px', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                        {selected.description}
                      </p>
                    )}
                    {selected.permissions && selected.permissions.length > 0 ? (
                      <PermissionMatrix
                        perms={SYSTEM_MODULES.map(m => {
                          const ex = selected.permissions?.find(p => p.module === m);
                          return ex || { module: m, canCreate: false, canRead: false, canUpdate: false, canDelete: false };
                        })}
                        onChange={() => {}}
                        readOnly
                      />
                    ) : (
                      <p style={{ padding: '16px', color: 'var(--text-muted)', textAlign: 'center' }}>
                        No permissions configured for this role.
                      </p>
                    )}
                  </div>
                </div>
              )}
            </div>
          )}
        </>
      )}

      {/* ── HISTORY TAB (AC #10) ── */}
      {activeTab === 'history' && (
        <div className="card">
          <div className="card-header">
            Role Assignment History
            <span className="badge badge-secondary" style={{ marginLeft: 8 }}>{history.length}</span>
          </div>
          {histLoading ? <Spinner /> : history.length === 0 ? (
            <div style={{ padding: 32, textAlign: 'center', color: 'var(--text-muted)' }}>
              No role assignment history yet.
            </div>
          ) : (
            <div style={{ overflowX: 'auto' }}>
              <table className="table" style={{ fontSize: '0.82rem' }}>
                <thead>
                  <tr><th>User</th><th>Previous Role</th><th>New Role</th><th>Changed By</th><th>Reason</th><th>Date</th></tr>
                </thead>
                <tbody>
                  {history.map(h => (
                    <tr key={h.id}>
                      <td style={{ fontWeight: 600 }}>{h.userName}</td>
                      <td>
                        <span className="badge badge-secondary">{h.previousRole || '—'}</span>
                      </td>
                      <td>
                        <span className="badge badge-primary">{h.newRole}</span>
                      </td>
                      <td>{h.actorName}</td>
                      <td style={{ color: 'var(--text-muted)' }}>{h.reason || '—'}</td>
                      <td>{fmt.datetime(h.changedAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* ── Assign Role Modal ── */}
      {showAssign && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)',
          zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center'
        }}>
          <div className="card" style={{ width: 480, maxWidth: '95vw' }}>
            <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span><i className="bi bi-person-badge" style={{ marginRight: 8 }} />Assign Role to User</span>
              <button className="btn btn-secondary btn-sm btn-icon" onClick={() => setShowAssign(false)}>
                <i className="bi bi-x-lg" />
              </button>
            </div>
            <div className="card-body">
              <form onSubmit={handleAssign}>
                <div className="form-group">
                  <label className="form-label">User *</label>
                  <select className="form-control form-select" value={assignUserId}
                    onChange={e => setAssignUserId(e.target.value)} required>
                    <option value="">Select user...</option>
                    {users.map((u: any) => (
                      <option key={u.id} value={u.id}>
                        {u.name} ({u.email}) — {u.effectiveRole || u.role}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label className="form-label">New Role *</label>
                  <select className="form-control form-select" value={assignRole}
                    onChange={e => setAssignRole(e.target.value)} required>
                    <option value="">Select role...</option>
                    {roles.map(r => (
                      <option key={r.id} value={r.name}>{r.name.replace(/_/g, ' ')}</option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label className="form-label">Reason (optional)</label>
                  <input className="form-control" value={assignReason}
                    onChange={e => setAssignReason(e.target.value)}
                    placeholder="Reason for role change..." />
                </div>
                <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
                  <button type="submit" className="btn btn-primary">Assign Role</button>
                  <button type="button" className="btn btn-secondary" onClick={() => setShowAssign(false)}>Cancel</button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* AC #5: Delete confirmation modal */}
      <ConfirmModal
        open={!!deleteTarget}
        title="Delete Role"
        message={
          deleteTarget
            ? `Are you sure you want to delete the role "${deleteTarget.name}"? ` +
              `This action cannot be undone. Ensure no users are assigned this role before deleting.`
            : ''
        }
        confirmText="Delete Role"
        danger
        onConfirm={handleDelete}
        onClose={() => setDeleteTarget(null)}
      />
    </div>
  );
};

export default RoleManagementPage;
