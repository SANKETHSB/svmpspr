/**
 * ============================================================================
 * US 13 AC #10: NOTIFICATION PREFERENCES PAGE
 * ============================================================================
 *
 * User Story: As a user, I want to configure my notification preferences
 * so that I can control which notifications I receive and through which channels.
 *
 * Priority: Must Have
 * Status: ✅ COMPLETED
 *
 * ============================================================================
 * ACCEPTANCE CRITERIA IMPLEMENTATION
 * ============================================================================
 *
 * ✅ AC #10: Users must be able to configure notification preferences
 *    - Implementation: Preferences page with toggle switches
 *    - Separate controls for in-app and email notifications
 *    - Save and reset functionality
 *    - Preferences persist in database
 *    - Location: This entire component
 *
 * ============================================================================
 * KEY FEATURES
 * ============================================================================
 *
 * - Display all notification types with descriptions
 * - Toggle switches for in-app notifications
 * - Toggle switches for email notifications
 * - Save preferences button
 * - Reset to defaults button
 * - Loading states with spinner
 * - Success/error toast notifications
 * - Responsive design
 * - Icon indicators for each notification type
 *
 * ============================================================================
 * NOTIFICATION TYPES SUPPORTED
 * ============================================================================
 *
 * 1. VENDOR_APPROVED - Vendor registration approved
 * 2. VENDOR_REJECTED - Vendor registration rejected
 * 3. RFQ_ASSIGNED - New RFQ invitation
 * 4. RFQ_AWARDED - RFQ award notification
 * 5. RFQ_CLOSED - RFQ closed automatically
 * 6. PO_ISSUED - Purchase Order issued
 * 7. COMPLIANCE_EXPIRY - Compliance document expiring
 * 8. ACCOUNT_LOCKED - Account locked due to failed logins
 * 9. GENERAL - General notifications
 *
 * ============================================================================
 * IMPLEMENTATION DATE: May 13, 2026
 * IMPLEMENTED BY: Development Team
 * STATUS: ✅ PRODUCTION READY
 * ============================================================================
 */

import React, { useEffect, useState } from "react";
import { toast } from "react-toastify";
import { notifAPI } from "../../services/api";
import { NotificationPreference } from "../../types";
import {
  Spinner,
  PageHeader,
} from "../../components/common/SharedComponents";

// Notification type metadata with icons, colors, and descriptions
const NOTIFICATION_TYPES = [
  {
    type: "VENDOR_APPROVED",
    label: "Vendor Approved",
    description: "When a vendor registration is approved",
    icon: "bi-person-check-fill",
    color: "#22c55e",
  },
  {
    type: "VENDOR_REJECTED",
    label: "Vendor Rejected",
    description: "When a vendor registration is rejected",
    icon: "bi-person-x-fill",
    color: "#ef4444",
  },
  {
    type: "RFQ_ASSIGNED",
    label: "RFQ Assigned",
    description: "When you are invited to participate in an RFQ",
    icon: "bi-file-earmark-text-fill",
    color: "#3b82f6",
  },
  {
    type: "RFQ_AWARDED",
    label: "RFQ Awarded",
    description: "When your quotation wins an RFQ",
    icon: "bi-trophy-fill",
    color: "#f59e0b",
  },
  {
    type: "RFQ_CLOSED",
    label: "RFQ Closed",
    description: "When an RFQ is automatically closed",
    icon: "bi-file-earmark-x-fill",
    color: "#6b7280",
  },
  {
    type: "PO_ISSUED",
    label: "Purchase Order Issued",
    description: "When a Purchase Order is issued to you",
    icon: "bi-bag-check-fill",
    color: "#06b6d4",
  },
  {
    type: "COMPLIANCE_EXPIRY",
    label: "Compliance Expiry",
    description: "When a compliance document is expiring soon",
    icon: "bi-exclamation-triangle-fill",
    color: "#ef4444",
  },
  {
    type: "ACCOUNT_LOCKED",
    label: "Account Locked",
    description: "When your account is locked due to security reasons",
    icon: "bi-lock-fill",
    color: "#ef4444",
  },
  {
    type: "GENERAL",
    label: "General Notifications",
    description: "General system notifications and announcements",
    icon: "bi-bell-fill",
    color: "#6b7280",
  },
];

const NotificationPreferencesPage: React.FC = () => {
  const [preferences, setPreferences] = useState<NotificationPreference[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [resetting, setResetting] = useState(false);

  // Load preferences on mount
  useEffect(() => {
    loadPreferences();
  }, []);

  const loadPreferences = async () => {
    setLoading(true);
    try {
      const res = await notifAPI.getPreferences();
      setPreferences(res.data.data || []);
    } catch (error) {
      toast.error("Failed to load notification preferences");
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  // Toggle in-app notification for a specific type
  const toggleInApp = (type: string) => {
    setPreferences((prev) =>
      prev.map((pref) =>
        pref.notificationType === type
          ? { ...pref, inAppEnabled: !pref.inAppEnabled }
          : pref
      )
    );
  };

  // Toggle email notification for a specific type
  const toggleEmail = (type: string) => {
    setPreferences((prev) =>
      prev.map((pref) =>
        pref.notificationType === type
          ? { ...pref, emailEnabled: !pref.emailEnabled }
          : pref
      )
    );
  };

  // Save preferences to backend
  const savePreferences = async () => {
    setSaving(true);
    try {
      await notifAPI.updatePreferences(preferences);
      toast.success("Notification preferences saved successfully");
    } catch (error) {
      toast.error("Failed to save preferences");
      console.error(error);
    } finally {
      setSaving(false);
    }
  };

  // Reset preferences to defaults (all enabled)
  const resetPreferences = async () => {
    if (!window.confirm("Reset all notification preferences to defaults (all enabled)?")) {
      return;
    }

    setResetting(true);
    try {
      await notifAPI.resetPreferences();
      toast.success("Preferences reset to defaults");
      await loadPreferences(); // Reload from server
    } catch (error) {
      toast.error("Failed to reset preferences");
      console.error(error);
    } finally {
      setResetting(false);
    }
  };

  // Get preference for a specific notification type
  const getPreference = (type: string): NotificationPreference | undefined => {
    return preferences.find((p) => p.notificationType === type);
  };

  return (
    <div>
      <PageHeader
        title="Notification Preferences"
        subtitle="Configure which notifications you want to receive and through which channels"
        actions={
          <div style={{ display: "flex", gap: 10 }}>
            <button
              className="btn btn-secondary btn-sm"
              onClick={resetPreferences}
              disabled={loading || resetting}
            >
              <i className="bi bi-arrow-counterclockwise" />{" "}
              {resetting ? "Resetting..." : "Reset to Defaults"}
            </button>
            <button
              className="btn btn-primary btn-sm"
              onClick={savePreferences}
              disabled={loading || saving}
            >
              <i className="bi bi-check2" />{" "}
              {saving ? "Saving..." : "Save Preferences"}
            </button>
          </div>
        }
      />

      <div className="card">
        {loading ? (
          <Spinner />
        ) : (
          <div style={{ padding: "20px" }}>
            {/* Header Row */}
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "1fr 150px 150px",
                gap: 20,
                padding: "0 20px 15px 20px",
                borderBottom: "2px solid var(--border)",
                fontWeight: 600,
                fontSize: "0.85rem",
                color: "var(--text-muted)",
              }}
            >
              <div>NOTIFICATION TYPE</div>
              <div style={{ textAlign: "center" }}>
                <i className="bi bi-app-indicator" /> IN-APP
              </div>
              <div style={{ textAlign: "center" }}>
                <i className="bi bi-envelope" /> EMAIL
              </div>
            </div>

            {/* Preference Rows */}
            {NOTIFICATION_TYPES.map((notifType) => {
              const pref = getPreference(notifType.type);
              const inAppEnabled = pref?.inAppEnabled ?? true;
              const emailEnabled = pref?.emailEnabled ?? true;

              return (
                <div
                  key={notifType.type}
                  style={{
                    display: "grid",
                    gridTemplateColumns: "1fr 150px 150px",
                    gap: 20,
                    padding: "20px",
                    borderBottom: "1px solid var(--border)",
                    alignItems: "center",
                  }}
                >
                  {/* Notification Type Info */}
                  <div style={{ display: "flex", alignItems: "center", gap: 15 }}>
                    <div
                      style={{
                        width: 40,
                        height: 40,
                        borderRadius: "50%",
                        background: `${notifType.color}18`,
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        fontSize: "1rem",
                        color: notifType.color,
                        flexShrink: 0,
                      }}
                    >
                      <i className={`bi ${notifType.icon}`} />
                    </div>
                    <div>
                      <div style={{ fontWeight: 600, fontSize: "0.95rem" }}>
                        {notifType.label}
                      </div>
                      <div
                        style={{
                          fontSize: "0.8rem",
                          color: "var(--text-muted)",
                          marginTop: 2,
                        }}
                      >
                        {notifType.description}
                      </div>
                    </div>
                  </div>

                  {/* In-App Toggle */}
                  <div style={{ display: "flex", justifyContent: "center" }}>
                    <label className="switch">
                      <input
                        type="checkbox"
                        checked={inAppEnabled}
                        onChange={() => toggleInApp(notifType.type)}
                      />
                      <span className="slider round"></span>
                    </label>
                  </div>

                  {/* Email Toggle */}
                  <div style={{ display: "flex", justifyContent: "center" }}>
                    <label className="switch">
                      <input
                        type="checkbox"
                        checked={emailEnabled}
                        onChange={() => toggleEmail(notifType.type)}
                      />
                      <span className="slider round"></span>
                    </label>
                  </div>
                </div>
              );
            })}

            {/* Info Box */}
            <div
              style={{
                marginTop: 20,
                padding: 15,
                background: "rgba(59, 130, 246, 0.05)",
                border: "1px solid rgba(59, 130, 246, 0.2)",
                borderRadius: 8,
                fontSize: "0.85rem",
                color: "var(--text-muted)",
              }}
            >
              <i className="bi bi-info-circle" style={{ marginRight: 8 }} />
              <strong>Note:</strong> Changes take effect immediately after saving.
              Disabling both in-app and email for a notification type means you won't
              receive that notification at all.
            </div>
          </div>
        )}
      </div>

      {/* CSS for Toggle Switch */}
      <style>{`
        .switch {
          position: relative;
          display: inline-block;
          width: 50px;
          height: 26px;
        }

        .switch input {
          opacity: 0;
          width: 0;
          height: 0;
        }

        .slider {
          position: absolute;
          cursor: pointer;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background-color: #ccc;
          transition: 0.3s;
        }

        .slider:before {
          position: absolute;
          content: "";
          height: 20px;
          width: 20px;
          left: 3px;
          bottom: 3px;
          background-color: white;
          transition: 0.3s;
        }

        input:checked + .slider {
          background-color: #3b82f6;
        }

        input:checked + .slider:before {
          transform: translateX(24px);
        }

        .slider.round {
          border-radius: 26px;
        }

        .slider.round:before {
          border-radius: 50%;
        }
      `}</style>
    </div>
  );
};

export default NotificationPreferencesPage;
