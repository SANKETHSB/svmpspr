import React, { createContext, useContext, useEffect, ReactNode } from 'react';
import { Theme } from '../types';

interface ThemeContextType {
  theme: Theme;
  toggleTheme: () => void;
  isDark: boolean;
}

const ThemeContext = createContext<ThemeContextType>({} as ThemeContextType);

// SVPMS uses a single "Cosmic Indigo" dark theme. The toggle is removed and
// the theme is locked. This provider is kept so existing consumers
// (useTheme / isDark) continue to compile.
export const ThemeProvider = ({ children }: { children: ReactNode }) => {
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', 'dark');
    document.body.className = 'theme-dark';
    localStorage.setItem('svpms-theme', 'dark');
  }, []);

  return (
    <ThemeContext.Provider value={{ theme: 'dark' as Theme, toggleTheme: () => {}, isDark: true }}>
      {children}
    </ThemeContext.Provider>
  );
};

export const useTheme = () => useContext(ThemeContext);
