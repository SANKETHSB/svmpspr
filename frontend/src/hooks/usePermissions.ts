import { useState, useEffect, useCallback } from 'react';
import { roleAPI } from '../services/api';
import { ModulePermission, SystemModule } from '../types';
import { useAuth } from '../context/AuthContext';

/**
 * AC #7: Hook that loads the current user's module-level permissions
 * and provides helpers to check CRUD access dynamically.
 *
 * System roles (ADMIN, PROCUREMENT_MANAGER, COMPLIANCE_OFFICER) get
 * full access to all modules — no DB lookup needed.
 * Custom roles load their permissions from the backend.
 *
 * Usage:
 *   const { can } = usePermissions();
 *   if (can('VENDORS', 'CREATE')) { ... }
 */

const SYSTEM_FULL_ACCESS_ROLES = ['ADMIN', 'PROCUREMENT_MANAGER', 'COMPLIANCE_OFFICER'];

interface PermissionMap {
  [module: string]: {
    canCreate: boolean;
    canRead: boolean;
    canUpdate: boolean;
    canDelete: boolean;
  };
}

export const usePermissions = () => {
  const { user, isAdmin, isManager, isCompliance } = useAuth();
  const [permissions, setPermissions] = useState<PermissionMap>({});
  const [loading, setLoading] = useState(false);

  const isSystemRole = isAdmin || isManager || isCompliance;

  const load = useCallback(async () => {
    if (!user || isSystemRole) return;
    setLoading(true);
    try {
      const res = await roleAPI.getMyPermissions();
      const perms: ModulePermission[] = res.data.data || [];
      const map: PermissionMap = {};
      perms.forEach(p => {
        map[p.module] = {
          canCreate: p.canCreate,
          canRead:   p.canRead,
          canUpdate: p.canUpdate,
          canDelete: p.canDelete,
        };
      });
      setPermissions(map);
    } catch {
      // Silently fail — system roles don't need this
    } finally {
      setLoading(false);
    }
  }, [user, isSystemRole]);

  useEffect(() => { load(); }, [load]);

  /**
   * AC #7: Check if current user can perform an action on a module.
   * System roles always return true.
   */
  const can = (module: SystemModule | string, action: 'CREATE' | 'READ' | 'UPDATE' | 'DELETE'): boolean => {
    // System roles have full access
    if (isSystemRole) return true;

    const perm = permissions[module];
    if (!perm) return false;

    switch (action) {
      case 'CREATE': return perm.canCreate;
      case 'READ':   return perm.canRead;
      case 'UPDATE': return perm.canUpdate;
      case 'DELETE': return perm.canDelete;
      default:       return false;
    }
  };

  /** True if user can read the module (most common check) */
  const canRead   = (module: SystemModule | string) => can(module, 'READ');
  const canCreate = (module: SystemModule | string) => can(module, 'CREATE');
  const canUpdate = (module: SystemModule | string) => can(module, 'UPDATE');
  const canDelete = (module: SystemModule | string) => can(module, 'DELETE');

  return { can, canRead, canCreate, canUpdate, canDelete, permissions, loading };
};
