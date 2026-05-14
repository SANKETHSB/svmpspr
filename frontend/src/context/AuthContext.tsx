import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { AuthUser } from '../types';

interface AuthContextType {
  user: AuthUser | null;
  isAuthenticated: boolean;
  login: (user: AuthUser) => void;
  logout: () => void;
  hasRole: (...roles: string[]) => boolean;
  isAdmin: boolean;
  isManager: boolean;
  isVendor: boolean;
  isCompliance: boolean;
}

const AuthContext = createContext<AuthContextType>({} as AuthContextType);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<AuthUser | null>(() => {
    const s = localStorage.getItem('svpms-user');
    return s ? JSON.parse(s) : null;
  });

  const login = (u: AuthUser) => {
    localStorage.setItem('svpms-user', JSON.stringify(u));
    localStorage.setItem('svpms-token', u.accessToken);
    setUser(u);
  };

  const logout = () => {
    localStorage.removeItem('svpms-user');
    localStorage.removeItem('svpms-token');
    setUser(null);
  };

  const hasRole = (...roles: string[]) =>
    user ? roles.includes(user.roleType) : false;

  return (
    <AuthContext.Provider value={{
      user,
      isAuthenticated: !!user,
      login, logout, hasRole,
      isAdmin: user?.roleType === 'ADMIN',
      isManager: user?.roleType === 'PROCUREMENT_MANAGER',
      isVendor: user?.role === 'VENDOR',
      isCompliance: user?.roleType === 'COMPLIANCE_OFFICER',
    }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
