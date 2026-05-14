import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  LineElement,
  PointElement,
} from "chart.js";
import { Doughnut, Bar, Line } from "react-chartjs-2";
import { dashboardAPI } from "../../services/api";
import { DashboardStats } from "../../types";
import { useAuth } from "../../context/AuthContext";
import {
  PageHeader,
  Spinner,
  fmt,
} from "../../components/common/SharedComponents";

ChartJS.register(
  ArcElement,
  Tooltip,
  Legend,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  LineElement,
  PointElement,
);

const StatCard: React.FC<{
  label: string;
  value: number | string;
  icon: string;
  color: string;
  to?: string;
  sub?: string;
}> = ({ label, value, icon, color, to, sub }) => {
  const card = (
    <div className="card stat-card" style={{ height: "100%" }}>
      <div className="card-body" style={{ padding: 20 }}>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "flex-start",
          }}
        >
          <div>
            <p
              style={{
                color: "var(--text-muted)",
                fontSize: "0.8rem",
                fontWeight: 500,
                marginBottom: 6,
              }}
            >
              {label}
            </p>
            <h3
              style={{
                fontWeight: 800,
                fontSize: "1.8rem",
                color: "var(--text)",
                marginBottom: 2,
              }}
            >
              {value}
            </h3>
            {sub && (
              <p style={{ color: "var(--text-muted)", fontSize: "0.75rem" }}>
                {sub}
              </p>
            )}
          </div>
          <div
            className="stat-icon"
            style={{ background: `${color}45`, color }}
          >
            <i className={`bi ${icon}`} />
          </div>
        </div>
      </div>
    </div>
  );
  return to ? (
    <Link
      to={to}
      style={{ textDecoration: "none", display: "block", height: "100%" }}
    >
      {card}
    </Link>
  ) : (
    card
  );
};

const DashboardPage: React.FC = () => {
  const { isAdmin, isManager, isCompliance, isVendor, user } = useAuth();
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [vendorChartType, setVendorChartType] = useState<"doughnut" | "bar">("doughnut");
  const [rfqChartType, setRfqChartType] = useState<"bar" | "line">("bar");

  useEffect(() => {
    if (!isVendor) {
      dashboardAPI
        .getKpis()
        .then((r) => setStats(r.data.data))
        .catch(() => {})
        .finally(() => setLoading(false));
    } else setLoading(false);
  }, [isVendor]);

  if (loading) return <Spinner msg="Loading dashboard..." />;

  const vendorInsights = {
    responseRate: "76%",
    avgQuoteTime: "2.4 days",
    activeRfqs: "3",
    winRate: "18%",
  };

  if (isVendor)
    return (
      <div>
        <div className="hero-banner fade-in" style={{ marginBottom: 20 }}>
          <h2>Welcome back 👋</h2>
          <p>
            Track your RFQs, quotations and purchase orders all in one place.
          </p>
        </div>
        <PageHeader
          title="Vendor Dashboard"
          subtitle="Your procurement workspace"
        />
        <div
          className="dashboard-grid"
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))",
            gap: 16,
          }}
        >
          <StatCard
            label="My RFQs"
            value="View All"
            icon="bi-file-earmark-text-fill"
            color="#3b82f6"
            to="/rfqs"
          />
          <StatCard
            label="My Quotations"
            value="View All"
            icon="bi-receipt"
            color="#22c55e"
            to="/quotations"
          />
          <StatCard
            label="Purchase Orders"
            value="View All"
            icon="bi-bag-check-fill"
            color="#06b6d4"
            to="/purchase-orders"
          />
          <StatCard
            label="Notifications"
            value="Check"
            icon="bi-bell-fill"
            color="#f59e0b"
            to="/notifications"
          />
          <StatCard
            label="Compliance Docs"
            value="Manage"
            icon="bi-shield-fill-check"
            color="#a855f7"
            to={`/vendors/${user?.id}/compliance`}
          />
        </div>
        <div className="card" style={{ marginTop: 16 }}>
          <div className="card-header">
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <span className="header-icon" style={{ background: "rgba(14,165,233,0.25)" }}>
                <i className="bi bi-stars" />
              </span>
              Vendor Insights
            </div>
          </div>
          <div className="card-body">
            <div className="analysis-grid">
              <div className="analysis-item">
                <div className="analysis-label">
                  <i className="bi bi-activity" />
                  Response Rate
                </div>
                <div className="analysis-value">{vendorInsights.responseRate}</div>
                <div className="analysis-meta">Last 30 days</div>
              </div>
              <div className="analysis-item">
                <div className="analysis-label">
                  <i className="bi bi-clock-history" />
                  Avg Quote Time
                </div>
                <div className="analysis-value">{vendorInsights.avgQuoteTime}</div>
                <div className="analysis-meta">Submission speed</div>
              </div>
              <div className="analysis-item">
                <div className="analysis-label">
                  <i className="bi bi-folder2-open" />
                  Active RFQs
                </div>
                <div className="analysis-value">{vendorInsights.activeRfqs}</div>
                <div className="analysis-meta">Needs action</div>
              </div>
              <div className="analysis-item">
                <div className="analysis-label">
                  <i className="bi bi-trophy-fill" />
                  Win Rate
                </div>
                <div className="analysis-value">{vendorInsights.winRate}</div>
                <div className="analysis-meta">Rolling 90 days</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    );

  if (!stats)
    return (
      <div className="empty-state">
        <i className="bi bi-exclamation-triangle" />
        <h6>Unable to load dashboard</h6>
      </div>
    );

  const vendorStatusTotal =
    stats.vendorsByStatus?.reduce((sum, v) => sum + v.count, 0) || 0;
  const approvedCount =
    stats.vendorsByStatus?.find((v) => v.status === "APPROVED")?.count || 0;
  const pendingCount =
    stats.vendorsByStatus?.find((v) => v.status === "PENDING_APPROVAL")
      ?.count || 0;
  const approvedPct = vendorStatusTotal
    ? Math.round((approvedCount / vendorStatusTotal) * 100)
    : 0;
  const pendingPct = vendorStatusTotal
    ? Math.round((pendingCount / vendorStatusTotal) * 100)
    : 0;

  const rfqTotal =
    stats.totalRfqs || stats.openRfqs + stats.awardedRfqs + stats.closedRfqs;
  const awardedPct = rfqTotal
    ? Math.round((stats.awardedRfqs / rfqTotal) * 100)
    : 0;
  const completionPct = rfqTotal
    ? Math.round(((stats.closedRfqs + stats.awardedRfqs) / rfqTotal) * 100)
    : 0;
  const approvalRate = vendorStatusTotal
    ? Math.round((approvedCount / vendorStatusTotal) * 100)
    : 0;
  const avgQuotesPerRfq = stats.totalRfqs
    ? (stats.totalQuotations / stats.totalRfqs).toFixed(1)
    : "0.0";
  const pipelineHealth =
    completionPct >= 70 ? "Strong" : completionPct >= 45 ? "Moderate" : "Needs Attention";
  const complianceRisk =
    stats.expiredDocuments > 0
      ? "High"
      : stats.expiringDocuments > 0
        ? "Medium"
        : "Low";

  const topRfq = (stats.rfqTrend || []).reduce(
    (best, item) => (item.count > best.count ? item : best),
    { status: "N/A", count: 0 },
  );

  const rfqSegments = [
    { label: "Open", value: stats.openRfqs, color: "#3b82f6" },
    { label: "Awarded", value: stats.awardedRfqs, color: "#22c55e" },
    { label: "Closed", value: stats.closedRfqs, color: "#f59e0b" },
  ];
  const rfqPipelineTotal =
    rfqSegments.reduce((sum, seg) => sum + seg.value, 0) || 1;

  const doughnutData = {
    labels: stats.vendorsByStatus?.map((v) => v.status.replace("_", " ")) || [],
    datasets: [
      {
        data: stats.vendorsByStatus?.map((v) => v.count) || [],
        backgroundColor: ["#f59e0b", "#22c55e", "#ef4444", "#6b7280"],
        borderWidth: 0,
        hoverOffset: 6,
      },
    ],
  };

  const barData = {
    labels: stats.rfqTrend?.map((r) => r.status) || [],
    datasets: [
      {
        label: "RFQs",
        data: stats.rfqTrend?.map((r) => r.count) || [],
        backgroundColor: ["#00a4ef", "#3b82f6", "#22c55e", "#a855f7"],
        borderRadius: 8,
        borderSkipped: false,
      },
    ],
  };

  const lineData = {
    labels: stats.rfqTrend?.map((r) => r.status) || [],
    datasets: [
      {
        label: "RFQ Trend",
        data: stats.rfqTrend?.map((r) => r.count) || [],
        borderColor: "#00a4ef",
        backgroundColor: "rgba(0, 164, 239, 0.1)",
        borderWidth: 3,
        fill: true,
        tension: 0.4,
        pointRadius: 6,
        pointBackgroundColor: "#00a4ef",
        pointBorderColor: "#ffffff",
        pointBorderWidth: 2,
      },
    ],
  };

  return (
    <div>
      <PageHeader title="Dashboard" subtitle="Procurement KPIs at a glance" />

      {/* KPI Cards */}
      <div
        className="dashboard-grid"
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(160px, 1fr))",
          gap: 14,
          marginBottom: 24,
        }}
      >
        <StatCard
          label="Total Vendors"
          value={stats.totalVendors}
          icon="bi-people-fill"
          color="#3b82f6"
          to="/vendors"
          sub={`${stats.approvedVendors} approved`}
        />
        <StatCard
          label="Pending Approval"
          value={stats.pendingVendors}
          icon="bi-hourglass-split"
          color="#f59e0b"
          to="/vendors?status=PENDING_APPROVAL"
        />
        <StatCard
          label="Open RFQs"
          value={stats.openRfqs}
          icon="bi-file-earmark-text-fill"
          color="#22c55e"
          to="/rfqs?status=OPEN"
        />
        <StatCard
          label="Awarded RFQs"
          value={stats.awardedRfqs}
          icon="bi-trophy-fill"
          color="#a855f7"
          to="/rfqs?status=AWARDED"
        />
        <StatCard
          label="Total Quotations"
          value={stats.totalQuotations}
          icon="bi-receipt"
          color="#06b6d4"
          to="/quotations"
        />
        <StatCard
          label="Purchase Orders"
          value={stats.totalPOs}
          icon="bi-bag-check-fill"
          color="#10b981"
          to="/purchase-orders"
        />
        {(isAdmin || isCompliance) && (
          <StatCard
            label="Expiring Docs"
            value={stats.expiringDocuments}
            icon="bi-exclamation-triangle-fill"
            color="#ef4444"
            sub={`${stats.expiredDocuments} expired`}
          />
        )}
      </div>

      {/* Charts */}
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))",
          gap: 16,
          marginBottom: 24,
        }}
      >
        <div className="card chart-card">
          <div className="card-header">
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8 }}>
              <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <span
                  className="header-icon"
                  style={{ background: "rgba(0,164,239,0.25)" }}
                >
                  <i className="bi bi-pie-chart-fill" />
                </span>
                Vendor Status Distribution
              </div>
              <button
                onClick={() => setVendorChartType(vendorChartType === "doughnut" ? "bar" : "doughnut")}
                style={{
                  background: "rgba(0,164,239,0.2)",
                  border: "1px solid rgba(0,164,239,0.4)",
                  color: "#00a4ef",
                  padding: "6px 12px",
                  borderRadius: 6,
                  cursor: "pointer",
                  fontSize: "0.8rem",
                  fontWeight: 600,
                  transition: "all 0.25s ease",
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = "rgba(0,164,239,0.3)";
                  e.currentTarget.style.boxShadow = "0 4px 12px rgba(0,164,239,0.25)";
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = "rgba(0,164,239,0.2)";
                  e.currentTarget.style.boxShadow = "none";
                }}
              >
                <i className={`bi ${vendorChartType === "doughnut" ? "bi-bar-chart-fill" : "bi-pie-chart-fill"}`} />
                {" "}{vendorChartType === "doughnut" ? "Bar" : "Doughnut"}
              </button>
            </div>
          </div>
          <div className="card-body">
            <div
              style={{
                height: 220,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
              }}
            >
              {vendorChartType === "doughnut" ? (
                <Doughnut
                  data={doughnutData}
                  options={{
                    cutout: "65%",
                    plugins: {
                      legend: { position: "bottom", labels: { color: "#fff" } },
                    },
                    maintainAspectRatio: false,
                  }}
                />
              ) : (
                <Bar
                  data={{
                    labels: stats.vendorsByStatus?.map((v) => v.status.replace("_", " ")) || [],
                    datasets: [
                      {
                        label: "Vendors",
                        data: stats.vendorsByStatus?.map((v) => v.count) || [],
                        backgroundColor: ["#f59e0b", "#22c55e", "#ef4444", "#6b7280"],
                        borderRadius: 8,
                        borderSkipped: false,
                      },
                    ],
                  }}
                  options={{
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { legend: { display: false } },
                    scales: {
                      y: {
                        beginAtZero: true,
                        grid: { color: "rgba(255,255,255,0.14)" },
                        ticks: { color: "#fff" },
                      },
                      x: { grid: { display: false }, ticks: { color: "#fff" } },
                    },
                  }}
                />
              )}
            </div>
            <div className="chart-analysis">
              <span className="analysis-icon">
                <i className="bi bi-stars" />
              </span>
              <span>
                Approved vendors make up {approvedPct}% of the base;{" "}
                {pendingPct}% are pending review.
              </span>
            </div>
            <div style={{
              display: "grid",
              gridTemplateColumns: "repeat(2, 1fr)",
              gap: 12,
              marginTop: 16,
              paddingTop: 16,
              borderTop: "1px solid rgba(0,164,239,0.2)"
            }}>
              <div>
                <div style={{ fontSize: "0.75rem", color: "var(--text-muted)" }}>Approved</div>
                <div style={{ fontSize: "1.4rem", fontWeight: 700, color: "#22c55e" }}>{approvedCount}</div>
              </div>
              <div>
                <div style={{ fontSize: "0.75rem", color: "var(--text-muted)" }}>Pending</div>
                <div style={{ fontSize: "1.4rem", fontWeight: 700, color: "#f59e0b" }}>{pendingCount}</div>
              </div>
            </div>
          </div>
        </div>
        <div className="card chart-card">
          <div className="card-header">
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8 }}>
              <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <span
                  className="header-icon"
                  style={{ background: "rgba(0,164,239,0.25)" }}
                >
                  <i className={`bi ${rfqChartType === "bar" ? "bi-bar-chart-fill" : "bi-graph-up"}`} />
                </span>
                RFQ Status Overview
              </div>
              <button
                onClick={() => setRfqChartType(rfqChartType === "bar" ? "line" : "bar")}
                style={{
                  background: "rgba(0,164,239,0.2)",
                  border: "1px solid rgba(0,164,239,0.4)",
                  color: "#00a4ef",
                  padding: "6px 12px",
                  borderRadius: 6,
                  cursor: "pointer",
                  fontSize: "0.8rem",
                  fontWeight: 600,
                  transition: "all 0.25s ease",
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = "rgba(0,164,239,0.3)";
                  e.currentTarget.style.boxShadow = "0 4px 12px rgba(0,164,239,0.25)";
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = "rgba(0,164,239,0.2)";
                  e.currentTarget.style.boxShadow = "none";
                }}
              >
                <i className={`bi ${rfqChartType === "bar" ? "bi-graph-up" : "bi-bar-chart-fill"}`} />
                {" "}{rfqChartType === "bar" ? "Line" : "Bar"}
              </button>
            </div>
          </div>
          <div className="card-body">
            <div style={{ height: 220 }}>
              {rfqChartType === "bar" ? (
                <Bar
                  data={barData}
                  options={{
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { legend: { display: false } },
                    scales: {
                      y: {
                        beginAtZero: true,
                        grid: { color: "rgba(255,255,255,0.14)" },
                        ticks: { color: "#fff" },
                      },
                      x: { grid: { display: false }, ticks: { color: "#fff" } },
                    },
                  }}
                />
              ) : (
                <Line
                  data={lineData}
                  options={{
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { legend: { display: false } },
                    scales: {
                      y: {
                        beginAtZero: true,
                        grid: { color: "rgba(255,255,255,0.14)" },
                        ticks: { color: "#fff" },
                      },
                      x: { grid: { display: false }, ticks: { color: "#fff" } },
                    },
                  }}
                />
              )}
            </div>
            <div className="chart-analysis">
              <span className="analysis-icon">
                <i className="bi bi-activity" />
              </span>
              <span>
                {topRfq.count
                  ? `${topRfq.status.replace("_", " ")} leads with ${topRfq.count} RFQs.`
                  : "RFQ trend data is still building."}{" "}
                Award rate is {awardedPct}% of total RFQs.
              </span>
            </div>
            <div style={{
              display: "grid",
              gridTemplateColumns: "repeat(3, 1fr)",
              gap: 12,
              marginTop: 16,
              paddingTop: 16,
              borderTop: "1px solid rgba(0,164,239,0.2)"
            }}>
              <div>
                <div style={{ fontSize: "0.75rem", color: "var(--text-muted)" }}>Open</div>
                <div style={{ fontSize: "1.4rem", fontWeight: 700, color: "#3b82f6" }}>{stats.openRfqs}</div>
              </div>
              <div>
                <div style={{ fontSize: "0.75rem", color: "var(--text-muted)" }}>Awarded</div>
                <div style={{ fontSize: "1.4rem", fontWeight: 700, color: "#22c55e" }}>{stats.awardedRfqs}</div>
              </div>
              <div>
                <div style={{ fontSize: "0.75rem", color: "var(--text-muted)" }}>Closed</div>
                <div style={{ fontSize: "1.4rem", fontWeight: 700, color: "#f59e0b" }}>{stats.closedRfqs}</div>
              </div>
            </div>
          </div>
        </div>
        <div className="card chart-card">
          <div className="card-header">
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <span
                className="header-icon"
                style={{ background: "rgba(0,164,239,0.25)" }}
              >
                <i className="bi bi-diagram-3-fill" />
              </span>
              RFQ Pipeline
            </div>
          </div>
          <div className="card-body">
            <div className="rfq-pipeline-bar">
              {rfqSegments.map((seg) => (
                <div
                  key={seg.label}
                  className="rfq-pipeline-seg"
                  style={{
                    width: `${(seg.value / rfqPipelineTotal) * 100}%`,
                    background: seg.color,
                    transition: "all 0.3s ease",
                  }}
                />
              ))}
            </div>
            <div className="rfq-pipeline-legend">
              {rfqSegments.map((seg) => (
                <div key={seg.label} className="rfq-pipeline-item">
                  <span
                    className="rfq-pipeline-dot"
                    style={{ background: seg.color }}
                  />
                  <span>{seg.label}</span>
                  <strong>{seg.value}</strong>
                </div>
              ))}
            </div>
            <div className="chart-analysis">
              <span className="analysis-icon">
                <i className="bi bi-flag-fill" />
              </span>
              <span>
                Pipeline completion rate is {completionPct}% (awarded + closed). Health: <strong>{pipelineHealth}</strong>
              </span>
            </div>
            <div style={{
              display: "grid",
              gridTemplateColumns: "repeat(2, 1fr)",
              gap: 12,
              marginTop: 16,
              paddingTop: 16,
              borderTop: "1px solid rgba(0,164,239,0.2)"
            }}>
              <div>
                <div style={{ fontSize: "0.75rem", color: "var(--text-muted)" }}>Completion Rate</div>
                <div style={{ fontSize: "1.4rem", fontWeight: 700, color: "#00a4ef" }}>{completionPct}%</div>
              </div>
              <div>
                <div style={{ fontSize: "0.75rem", color: "var(--text-muted)" }}>Total RFQs</div>
                <div style={{ fontSize: "1.4rem", fontWeight: 700, color: "#3b82f6" }}>{rfqTotal}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 24 }}>
        <div className="card-header">
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <span className="header-icon" style={{ background: "rgba(0,164,239,0.25)" }}>
              <i className="bi bi-clipboard-data-fill" />
            </span>
            Executive Insights
          </div>
        </div>
        <div className="card-body">
          <div className="analysis-grid">
            <div className="analysis-item">
              <div className="analysis-label">
                <i className="bi bi-check-circle-fill" />
                Vendor Approval Rate
              </div>
              <div className="analysis-value">{approvalRate}%</div>
              <div className="analysis-meta">{approvedCount} approved vendors</div>
            </div>
            <div className="analysis-item">
              <div className="analysis-label">
                <i className="bi bi-receipt-cutoff" />
                Avg Quotes / RFQ
              </div>
              <div className="analysis-value">{avgQuotesPerRfq}</div>
              <div className="analysis-meta">{stats.totalQuotations} total quotations</div>
            </div>
            <div className="analysis-item">
              <div className="analysis-label">
                <i className="bi bi-diagram-3-fill" />
                Pipeline Health
              </div>
              <div className="analysis-value">{pipelineHealth}</div>
              <div className="analysis-meta">{completionPct}% completion rate</div>
            </div>
            <div className="analysis-item">
              <div className="analysis-label">
                <i className="bi bi-shield-exclamation" />
                Compliance Risk
              </div>
              <div className="analysis-value">{complianceRisk}</div>
              <div className="analysis-meta">{stats.expiredDocuments} expired documents</div>
            </div>
          </div>
        </div>
      </div>

      {/* Top Vendors + Quick Actions */}
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16 }}>
        {stats.topVendors && stats.topVendors.length > 0 && (
          <div className="card">
            <div className="card-header">Top Performing Vendors</div>
            <div style={{ padding: "0 0 8px" }}>
              {stats.topVendors.map((v, i) => (
                <div
                  key={i}
                  style={{
                    display: "flex",
                    alignItems: "center",
                    padding: "10px 20px",
                    borderBottom: "1px solid var(--border)",
                    gap: 12,
                  }}
                >
                  <div
                    style={{
                      width: 28,
                      height: 28,
                      borderRadius: "50%",
                      background: "#3b82f620",
                      color: "#3b82f6",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      fontSize: "0.8rem",
                      fontWeight: 700,
                    }}
                  >
                    {i + 1}
                  </div>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontWeight: 600, fontSize: "0.875rem" }}>
                      {v.name}
                    </div>
                    <div
                      style={{
                        color: "var(--text-muted)",
                        fontSize: "0.75rem",
                      }}
                    >
                      {v.won} RFQs won
                    </div>
                  </div>
                  <div
                    style={{
                      fontSize: "0.8rem",
                      fontWeight: 600,
                      color: "#22c55e",
                    }}
                  >
                    ★ {v.score.toFixed(1)}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        <div className="card">
          <div className="card-header">Quick Actions</div>
          <div
            className="card-body"
            style={{ display: "flex", flexDirection: "column", gap: 10 }}
          >
            {(isAdmin || isManager) && (
              <Link
                to="/vendors?status=PENDING_APPROVAL"
                className="btn btn-warning"
                style={{ justifyContent: "flex-start" }}
              >
                <i className="bi bi-person-check" /> Review Pending Vendors (
                {stats.pendingVendors})
              </Link>
            )}
            {isManager && (
              <Link
                to="/rfqs/create"
                className="btn btn-primary"
                style={{ justifyContent: "flex-start" }}
              >
                <i className="bi bi-plus-circle" /> Create New RFQ
              </Link>
            )}
            <Link
              to="/rfqs?status=CLOSED"
              className="btn btn-secondary"
              style={{ justifyContent: "flex-start" }}
            >
              <i className="bi bi-file-earmark-check" /> View Closed RFQs (
              {stats.closedRfqs})
            </Link>
            <Link
              to="/purchase-orders"
              className="btn btn-info"
              style={{ justifyContent: "flex-start" }}
            >
              <i className="bi bi-bag" /> View All Purchase Orders (
              {stats.totalPOs})
            </Link>
            {(isAdmin || isCompliance) && (
              <Link
                to="/audit-logs"
                className="btn btn-secondary"
                style={{ justifyContent: "flex-start" }}
              >
                <i className="bi bi-shield-check" /> View Audit Logs
              </Link>
            )}
            {(isAdmin || isCompliance) && (
              <Link
                to="/compliance"
                className="btn btn-danger"
                style={{ justifyContent: "flex-start" }}
              >
                <i className="bi bi-shield-fill-check" /> Compliance Monitor
                {stats.expiredDocuments > 0 && (
                  <span
                    className="badge badge-danger"
                    style={{ marginLeft: 8 }}
                  >
                    {stats.expiredDocuments} expired
                  </span>
                )}
              </Link>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default DashboardPage;
