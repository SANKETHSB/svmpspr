import React, { useState, useEffect } from "react";
import { Outlet, NavLink, useNavigate, useLocation } from "react-router-dom";
import { AnimatePresence, motion } from "framer-motion";
import { useAuth } from "../../context/AuthContext";
import { notifAPI } from "../../services/api";
import { Avatar } from "../common/SharedComponents";
import { VoiceAssistantWidget } from "../voice/VoiceAssistantWidget";

interface NavItem {
  to: string;
  icon: string;
  label: string;
  roles?: string[];
}

const NAV: NavItem[] = [
  { to: "/dashboard", icon: "bi-grid-1x2-fill", label: "Dashboard" },
  {
    to: "/vendors",
    icon: "bi-people-fill",
    label: "Vendors",
    roles: ["ADMIN", "PROCUREMENT_MANAGER", "COMPLIANCE_OFFICER"],
  },
  {
    to: "/compliance",
    icon: "bi-shield-fill-check",
    label: "Compliance Monitor",
    roles: ["ADMIN", "COMPLIANCE_OFFICER"],
  },
  { to: "/rfqs", icon: "bi-file-earmark-text-fill", label: "RFQs" },
  { to: "/quotations", icon: "bi-receipt", label: "Quotations" },
  {
    to: "/purchase-orders",
    icon: "bi-bag-check-fill",
    label: "Purchase Orders",
  },
  { to: "/notifications", icon: "bi-bell-fill", label: "Notifications" },
  { to: "/notifications/preferences", icon: "bi-gear-fill", label: "Notification Settings" },
  {
    to: "/audit-logs",
    icon: "bi-shield-check",
    label: "Audit Logs",
    roles: ["ADMIN", "COMPLIANCE_OFFICER"],
  },
  {
    to: "/users",
    icon: "bi-person-gear",
    label: "User Management",
    roles: ["ADMIN"],
  },
  {
    to: "/roles",
    icon: "bi-shield-lock-fill",
    label: "Role Management",
    roles: ["ADMIN"],
  },
  {
    to: "/reports",
    icon: "bi-file-earmark-bar-graph-fill",
    label: "Reports & Exports",
    roles: ["ADMIN", "PROCUREMENT_MANAGER", "COMPLIANCE_OFFICER"],
  },
];

const Layout: React.FC = () => {
  const { user, logout, hasRole } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [unread, setUnread] = useState(0);
  const [sidebarOpen, setSidebarOpen] = useState(false);

  useEffect(() => {
    // Only fetch unread count if user is authenticated
    if (!user) return;

    const fetch = async () => {
      try {
        const res = await notifAPI.getUnreadCount();
        setUnread(res.data.data?.count || 0);
      } catch {
        /* silent */
      }
    };
    fetch();
    const t = setInterval(fetch, 30000);
    return () => clearInterval(t);
  }, [user]);

  const handleLogout = async () => {
    try {
      await notifAPI.markAllRead();
    } catch {
      /* silent */
    }
    logout();
    navigate("/login");
  };

  const visibleNav = NAV.filter((n) => !n.roles || hasRole(...n.roles));

  return (
    <div style={{ display: "flex" }}>
      {/* Mobile overlay */}
      {sidebarOpen && (
        <div
          onClick={() => setSidebarOpen(false)}
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(0,0,0,0.5)",
            zIndex: 999,
          }}
        />
      )}

      {/* Sidebar */}
      <nav
        className="sidebar"
        style={sidebarOpen ? { transform: "translateX(0)" } : undefined}
      >
        {/* Brand */}
        <div className="sidebar-brand">
          <div className="sidebar-brand-icon">
            <i className="bi bi-building-check" />
          </div>
          <div>
            <div className="sidebar-brand-text">SVPMS</div>
            <div className="sidebar-brand-sub">Infosys Limited</div>
          </div>
        </div>

        {/* Nav */}
        <div className="sidebar-nav">
          <div className="sidebar-section-title">Main Menu</div>
          {visibleNav.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `sidebar-link${isActive ? " active" : ""}`
              }
              onClick={() => setSidebarOpen(false)}
            >
              <i className={`bi ${item.icon}`} />
              <span>{item.label}</span>
              {item.to === "/notifications" && unread > 0 && (
                <span
                  className="notif-badge"
                  style={{
                    marginLeft: "auto",
                    background: "linear-gradient(135deg,#f59e0b 0%,#ec4899 100%)",
                    color: "#fff",
                    borderRadius: 999,
                    padding: "1px 8px",
                    fontSize: "0.7rem",
                    fontWeight: 700,
                    boxShadow: "0 0 0 2px rgba(236,72,153,0.18), 0 0 12px rgba(245,158,11,0.55)",
                  }}
                >
                  {unread > 99 ? "99+" : unread}
                </span>
              )}
            </NavLink>
          ))}
        </div>

        {/* User info */}
        <div className="sidebar-user">
          <Avatar name={user?.name || "?"} />
          <div style={{ overflow: "hidden", flex: 1 }}>
            <div
              className="sidebar-user-name"
              style={{
                overflow: "hidden",
                textOverflow: "ellipsis",
                whiteSpace: "nowrap",
              }}
            >
              {user?.name}
            </div>
            <div className="sidebar-user-role">
              {user?.roleType?.replace(/_/g, " ")}
            </div>
          </div>
        </div>
      </nav>

      {/* Main */}
      <div className="main-wrapper" style={{ flex: 1 }}>
        {/* Topbar */}
        <header className="topbar">
          {/* Mobile toggle */}
          <button
            className="btn btn-secondary btn-sm btn-icon"
            style={{ marginRight: 12, display: "none" }}
            id="sidebar-toggle"
            onClick={() => setSidebarOpen(true)}
          >
            <i className="bi bi-list" />
          </button>

          <div style={{ flex: 1 }} />

          <div className="d-flex align-items-center gap-3">
            {/* Notifications */}
            <NavLink
              to="/notifications"
              style={{
                position: "relative",
                color: "var(--text-muted)",
                fontSize: "1.1rem",
                lineHeight: 1,
              }}
            >
              <i className="bi bi-bell" />
              {unread > 0 && (
                <span
                  className="notif-badge"
                  style={{
                    position: "absolute",
                    top: -5,
                    right: -6,
                    background: "linear-gradient(135deg,#f59e0b 0%,#ec4899 100%)",
                    color: "#fff",
                    borderRadius: 999,
                    padding: "0 6px",
                    fontSize: "0.62rem",
                    fontWeight: 700,
                    minWidth: 16,
                    textAlign: "center",
                    boxShadow: "0 0 0 2px rgba(10,11,30,0.6), 0 0 14px rgba(236,72,153,0.7)",
                  }}
                >
                  {unread > 99 ? "99+" : unread}
                </span>
              )}
            </NavLink>

            {/* User dropdown */}
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: 8,
                cursor: "pointer",
                padding: "4px 8px",
                borderRadius: 8,
                transition: "var(--transition)",
              }}
              onClick={handleLogout}
              title="Logout"
              onMouseEnter={(e) =>
                (e.currentTarget.style.background = "var(--hover-bg)")
              }
              onMouseLeave={(e) =>
                (e.currentTarget.style.background = "transparent")
              }
            >
              <Avatar name={user?.name || "?"} size={28} />
              <span
                style={{
                  fontSize: "0.875rem",
                  fontWeight: 500,
                  maxWidth: 120,
                  overflow: "hidden",
                  textOverflow: "ellipsis",
                  whiteSpace: "nowrap",
                }}
              >
                {user?.name}
              </span>
              <i
                className="bi bi-box-arrow-right"
                style={{ color: "var(--text-muted)", fontSize: "0.875rem" }}
              />
            </div>
          </div>
        </header>

        {/* Content with smooth route transitions */}
        <main className="page-content">
          <AnimatePresence mode="wait">
            <motion.div
              key={location.pathname}
              initial={{ opacity: 0, y: 12, filter: "blur(4px)" }}
              animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
              exit={{ opacity: 0, y: -8, filter: "blur(4px)" }}
              transition={{ duration: 0.32, ease: [0.25, 1, 0.5, 1] }}
            >
              <Outlet />
            </motion.div>
          </AnimatePresence>
        </main>
      </div>
      <VoiceAssistantWidget />
    </div>
  );
};

export default Layout;
