import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { vendorAnalyticsAPI, vendorAPI } from "../../services/api";
import {
  PageHeader,
  Spinner,
  fmt,
} from "../../components/common/SharedComponents";

interface VendorAnalytics {
  vendorId: number;
  vendorName: string;
  totalRfqsParticipated: number;
  totalRfqsWon: number;
  winRatio: number;
  averageBidAmount: number;
  currency: string;
  totalPOs: number;
  onTimePOs: number;
  onTimeDeliveryRate: number;
  performanceScore: number;
  performanceGrade: string;
  quotationTrends: Array<{ period: string; count: number }>;
  winTrends: Array<{ period: string; count: number }>;
  historicalComparison: {
    comparisonPeriod: string;
    winRatioChange: number;
    performanceScoreChange: number;
    avgBidChange: number;
    trend: string;
  };
  totalQuotationsSubmitted: number;
  rejectedQuotations: number;
  totalValueWon: number;
  lowestBid: number;
  highestBid: number;
}

const VendorAnalyticsDashboard: React.FC = () => {
  const { vendorId } = useParams<{ vendorId: string }>();
  const navigate = useNavigate();
  const [analytics, setAnalytics] = useState<VendorAnalytics | null>(null);
  const [loading, setLoading] = useState(true);
  const [chartType, setChartType] = useState<"bar" | "line" | "donut">("bar");
  const [dateRange, setDateRange] = useState({
    startDate: new Date(Date.now() - 365 * 24 * 60 * 60 * 1000)
      .toISOString()
      .split("T")[0],
    endDate: new Date().toISOString().split("T")[0],
  });
  const [exporting, setExporting] = useState(false);

  useEffect(() => {
    loadAnalytics();
  }, [vendorId, dateRange]);

  const loadAnalytics = async () => {
    setLoading(true);
    try {
      const res = await vendorAnalyticsAPI.getVendorAnalytics(
        Number(vendorId),
        dateRange.startDate,
        dateRange.endDate,
      );
      setAnalytics(res.data.data);
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Failed to load analytics");
    } finally {
      setLoading(false);
    }
  };

  const handleExport = async () => {
    setExporting(true);
    try {
      const res = await vendorAnalyticsAPI.exportAnalytics(
        Number(vendorId),
        dateRange.startDate,
        dateRange.endDate,
      );
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const a = document.createElement("a");
      a.href = url;
      a.download = `vendor-analytics-${vendorId}-${Date.now()}.csv`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      toast.success("Analytics exported successfully");
    } catch {
      toast.error("Failed to export analytics");
    } finally {
      setExporting(false);
    }
  };

  if (loading) return <Spinner />;
  if (!analytics) return <div>No analytics data available</div>;

  const totalQuotes = analytics.totalQuotationsSubmitted;
  const winRate = totalQuotes
    ? Math.round((analytics.totalRfqsWon / totalQuotes) * 100)
    : 0;
  const rejectRate = totalQuotes
    ? Math.round((analytics.rejectedQuotations / totalQuotes) * 100)
    : 0;
  const activeQuotes = Math.max(
    totalQuotes - analytics.totalRfqsWon - analytics.rejectedQuotations,
    0,
  );

  const peakTrend = analytics.quotationTrends.reduce(
    (best, item) => (item.count > best.count ? item : best),
    { period: "N/A", count: 0 },
  );

  const bidRange = Math.max(analytics.highestBid - analytics.lowestBid, 1);
  const avgBidPos = Math.min(
    100,
    Math.max(
      0,
      ((analytics.averageBidAmount - analytics.lowestBid) / bidRange) * 100,
    ),
  );
  const bidSpread = analytics.highestBid - analytics.lowestBid;
  const onTimeRate = Math.min(100, Math.max(0, analytics.onTimeDeliveryRate));

  const getGradeColor = (grade: string) => {
    switch (grade) {
      case "A":
        return "#22c55e";
      case "B":
        return "#3b82f6";
      case "C":
        return "#f59e0b";
      case "D":
        return "#ef4444";
      case "F":
        return "#991b1b";
      default:
        return "#6b7280";
    }
  };

  const getTrendIcon = (trend: string) => {
    switch (trend) {
      case "IMPROVING":
        return { icon: "📈", color: "#22c55e", text: "Improving" };
      case "DECLINING":
        return { icon: "📉", color: "#ef4444", text: "Declining" };
      default:
        return { icon: "➡️", color: "#6b7280", text: "Stable" };
    }
  };

  const trendInfo = getTrendIcon(
    analytics.historicalComparison?.trend || "STABLE",
  );

  return (
    <div>
      <PageHeader
        title={`Vendor Analytics: ${analytics.vendorName}`}
        subtitle={`Performance Period: ${dateRange.startDate} to ${dateRange.endDate}`}
        breadcrumb={[
          { label: "Vendors", href: "/vendors" },
          { label: analytics.vendorName, href: `/vendors/${vendorId}` },
          { label: "Analytics" },
        ]}
        actions={
          <>
            <button
              className="btn btn-success btn-sm"
              onClick={handleExport}
              disabled={exporting}
            >
              <i className="bi bi-download" />{" "}
              {exporting ? "Exporting..." : "Export CSV"}
            </button>
            <button
              className="btn btn-secondary btn-sm"
              onClick={() => navigate(-1)}
            >
              <i className="bi bi-arrow-left" /> Back
            </button>
          </>
        }
      />

      {/* Date Range Filter */}
      <div className="card chart-card" style={{ marginBottom: 20 }}>
        <div className="card-body" style={{ padding: 16 }}>
          <div
            style={{
              display: "flex",
              gap: 16,
              alignItems: "end",
              flexWrap: "wrap",
            }}
          >
            <div style={{ flex: 1, minWidth: 200 }}>
              <label
                className="form-label"
                style={{
                  fontSize: "0.875rem",
                  marginBottom: 4,
                  fontWeight: 600,
                }}
              >
                Start Date
              </label>
              <input
                type="date"
                className="form-control"
                value={dateRange.startDate}
                max={dateRange.endDate}
                onChange={(e) =>
                  setDateRange((prev) => ({
                    ...prev,
                    startDate: e.target.value,
                  }))
                }
                style={{ fontSize: "0.95rem" }}
              />
              <small
                style={{ color: "var(--text-muted)", fontSize: "0.75rem" }}
              >
                Current:{" "}
                {dateRange.startDate &&
                !isNaN(new Date(dateRange.startDate).getTime())
                  ? new Date(dateRange.startDate).toLocaleDateString("en-US", {
                      year: "numeric",
                      month: "short",
                      day: "numeric",
                    })
                  : "Not selected"}
              </small>
            </div>
            <div style={{ flex: 1, minWidth: 200 }}>
              <label
                className="form-label"
                style={{
                  fontSize: "0.875rem",
                  marginBottom: 4,
                  fontWeight: 600,
                }}
              >
                End Date
              </label>
              <input
                type="date"
                className="form-control"
                value={dateRange.endDate}
                min={dateRange.startDate}
                max={new Date().toISOString().split("T")[0]}
                onChange={(e) =>
                  setDateRange((prev) => ({ ...prev, endDate: e.target.value }))
                }
                style={{ fontSize: "0.95rem" }}
              />
              <small
                style={{ color: "var(--text-muted)", fontSize: "0.75rem" }}
              >
                Current:{" "}
                {dateRange.endDate &&
                !isNaN(new Date(dateRange.endDate).getTime())
                  ? new Date(dateRange.endDate).toLocaleDateString("en-US", {
                      year: "numeric",
                      month: "short",
                      day: "numeric",
                    })
                  : "Not selected"}
              </small>
            </div>
            <button className="btn btn-primary" onClick={loadAnalytics}>
              <i className="bi bi-funnel" /> Apply Filter
            </button>
            <button
              className="btn btn-secondary"
              onClick={() =>
                setDateRange({
                  startDate: new Date(Date.now() - 365 * 24 * 60 * 60 * 1000)
                    .toISOString()
                    .split("T")[0],
                  endDate: new Date().toISOString().split("T")[0],
                })
              }
            >
              <i className="bi bi-arrow-clockwise" /> Reset to Last Year
            </button>
          </div>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 20 }}>
        <div className="card-header">
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <span className="header-icon" style={{ background: "rgba(59,130,246,0.25)" }}>
              <i className="bi bi-lightning-charge-fill" />
            </span>
            Analytics Highlights
          </div>
        </div>
        <div className="card-body">
          <div className="analysis-grid">
            <div className="analysis-item">
              <div className="analysis-label">
                <i className="bi bi-trophy-fill" />
                Win Rate
              </div>
              <div className="analysis-value">{winRate}%</div>
              <div className="analysis-meta">{analytics.totalRfqsWon} wins</div>
            </div>
            <div className="analysis-item">
              <div className="analysis-label">
                <i className="bi bi-x-circle-fill" />
                Rejection Rate
              </div>
              <div className="analysis-value">{rejectRate}%</div>
              <div className="analysis-meta">{analytics.rejectedQuotations} rejected</div>
            </div>
            <div className="analysis-item">
              <div className="analysis-label">
                <i className="bi bi-hourglass-split" />
                Active Quotes
              </div>
              <div className="analysis-value">{activeQuotes}</div>
              <div className="analysis-meta">Open evaluations</div>
            </div>
            <div className="analysis-item">
              <div className="analysis-label">
                <i className="bi bi-cash-stack" />
                Bid Spread
              </div>
              <div className="analysis-value">
                {fmt.currency(bidSpread, analytics.currency)}
              </div>
              <div className="analysis-meta">High vs low bids</div>
            </div>
          </div>
        </div>
      </div>

      {/* Performance Score Card */}
      <div
        className="card"
        style={{
          marginBottom: 20,
          border: `2px solid ${getGradeColor(analytics.performanceGrade)}`,
        }}
      >
        <div className="card-body" style={{ textAlign: "center", padding: 32 }}>
          <div
            style={{
              fontSize: "0.875rem",
              color: "var(--text-muted)",
              marginBottom: 8,
            }}
          >
            Overall Performance Score
          </div>
          <div
            style={{
              fontSize: "4rem",
              fontWeight: 700,
              color: getGradeColor(analytics.performanceGrade),
              lineHeight: 1,
            }}
          >
            {analytics.performanceScore}
          </div>
          <div
            style={{
              fontSize: "2rem",
              fontWeight: 700,
              color: getGradeColor(analytics.performanceGrade),
              marginTop: 8,
            }}
          >
            Grade: {analytics.performanceGrade}
          </div>
          <div
            style={{
              marginTop: 16,
              fontSize: "0.875rem",
              color: "var(--text-muted)",
            }}
          >
            {trendInfo.icon}{" "}
            <span style={{ color: trendInfo.color, fontWeight: 600 }}>
              {trendInfo.text}
            </span>{" "}
            compared to previous period
          </div>
        </div>
      </div>

      {/* Key Metrics Grid */}
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))",
          gap: 16,
          marginBottom: 20,
        }}
      >
        {[
          {
            label: "RFQs Participated",
            value: analytics.totalRfqsParticipated,
            icon: "bi-clipboard-check",
            color: "#3b82f6",
          },
          {
            label: "RFQs Won",
            value: analytics.totalRfqsWon,
            icon: "bi-trophy-fill",
            color: "#22c55e",
          },
          {
            label: "Win Ratio",
            value: `${analytics.winRatio}%`,
            icon: "bi-percent",
            color: "#8b5cf6",
          },
          {
            label: "Avg Bid Amount",
            value: fmt.currency(analytics.averageBidAmount, analytics.currency),
            icon: "bi-currency-dollar",
            color: "#f59e0b",
          },
          {
            label: "Total POs",
            value: analytics.totalPOs,
            icon: "bi-file-earmark-text",
            color: "#06b6d4",
          },
          {
            label: "On-Time Deliveries",
            value: `${analytics.onTimePOs}/${analytics.totalPOs}`,
            icon: "bi-clock-history",
            color: "#10b981",
          },
          {
            label: "On-Time Rate",
            value: `${analytics.onTimeDeliveryRate}%`,
            icon: "bi-speedometer2",
            color: "#14b8a6",
          },
          {
            label: "Total Value Won",
            value: fmt.currency(analytics.totalValueWon, analytics.currency),
            icon: "bi-cash-stack",
            color: "#22c55e",
          },
        ].map((metric) => (
          <div key={metric.label} className="card">
            <div className="card-body" style={{ padding: 16 }}>
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                }}
              >
                <div>
                  <div
                    style={{
                      fontSize: "0.75rem",
                      color: "var(--text-muted)",
                      marginBottom: 4,
                    }}
                  >
                    {metric.label}
                  </div>
                  <div style={{ fontSize: "1.5rem", fontWeight: 700 }}>
                    {metric.value}
                  </div>
                </div>
                <i
                  className={`bi ${metric.icon}`}
                  style={{
                    fontSize: "2rem",
                    color: metric.color,
                    opacity: 0.7,
                  }}
                />
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Historical Comparison */}
      {analytics.historicalComparison && (
        <div className="card" style={{ marginBottom: 20 }}>
          <div className="card-header">
            <i className="bi bi-graph-up-arrow" style={{ marginRight: 8 }} />
            Historical Comparison
            <span
              style={{
                fontSize: "0.75rem",
                color: "var(--text-muted)",
                marginLeft: 8,
              }}
            >
              vs {analytics.historicalComparison.comparisonPeriod}
            </span>
          </div>
          <div className="card-body">
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))",
                gap: 16,
              }}
            >
              {[
                {
                  label: "Win Ratio Change",
                  value: analytics.historicalComparison.winRatioChange,
                  suffix: "%",
                  isPositive: analytics.historicalComparison.winRatioChange > 0,
                },
                {
                  label: "Performance Score Change",
                  value: analytics.historicalComparison.performanceScoreChange,
                  suffix: " pts",
                  isPositive:
                    analytics.historicalComparison.performanceScoreChange > 0,
                },
                {
                  label: "Avg Bid Change",
                  value: analytics.historicalComparison.avgBidChange,
                  suffix: ` ${analytics.currency}`,
                  isPositive: analytics.historicalComparison.avgBidChange < 0, // Lower is better for bids
                },
              ].map((item) => (
                <div
                  key={item.label}
                  style={{
                    padding: 16,
                    borderRadius: 8,
                    background: item.isPositive
                      ? "rgba(34,197,94,0.1)"
                      : "rgba(239,68,68,0.1)",
                    border: `1px solid ${item.isPositive ? "#22c55e" : "#ef4444"}`,
                  }}
                >
                  <div
                    style={{
                      fontSize: "0.75rem",
                      color: "var(--text-muted)",
                      marginBottom: 4,
                    }}
                  >
                    {item.label}
                  </div>
                  <div
                    style={{
                      fontSize: "1.25rem",
                      fontWeight: 700,
                      color: item.isPositive ? "#22c55e" : "#ef4444",
                      display: "flex",
                      alignItems: "center",
                      gap: 6,
                    }}
                  >
                    <i
                      className={`bi ${item.isPositive ? "bi-arrow-up" : "bi-arrow-down"}`}
                    />
                    {Math.abs(Number(item.value))}
                    {item.suffix}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Trends Chart */}
      <div className="card chart-card" style={{ marginBottom: 20 }}>
        <div
          className="card-header"
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <span className="header-icon" style={{ background: "rgba(59,130,246,0.25)" }}>
              <i className="bi bi-graph-up" />
            </span>
            Monthly Trends
          </div>
          <div style={{ display: "flex", gap: 8 }}>
            <button
              className={`btn btn-sm ${chartType === "bar" ? "btn-primary" : "btn-outline-primary"}`}
              onClick={() => setChartType("bar")}
              title="Bar Chart"
            >
              <i className="bi bi-bar-chart-fill" /> Bar
            </button>
            <button
              className={`btn btn-sm ${chartType === "line" ? "btn-primary" : "btn-outline-primary"}`}
              onClick={() => setChartType("line")}
              title="Line Chart"
            >
              <i className="bi bi-graph-up" /> Line
            </button>
            <button
              className={`btn btn-sm ${chartType === "donut" ? "btn-primary" : "btn-outline-primary"}`}
              onClick={() => setChartType("donut")}
              title="Donut Chart"
            >
              <i className="bi bi-pie-chart-fill" /> Donut
            </button>
          </div>
        </div>
        <div className="card-body">
          {analytics.quotationTrends.length > 0 ? (
            <div style={{ overflowX: "auto" }}>
              {/* BAR CHART */}
              {chartType === "bar" && (
                <div
                  style={{
                    display: "flex",
                    gap: 8,
                    minWidth: 600,
                    padding: "20px 0",
                  }}
                >
                  {analytics.quotationTrends.map((trend) => {
                    const wins =
                      analytics.winTrends.find((w) => w.period === trend.period)
                        ?.count || 0;
                    const maxCount = Math.max(
                      ...analytics.quotationTrends.map((t) => t.count),
                      1,
                    );
                    const height = (trend.count / maxCount) * 150;
                    const winHeight = (wins / maxCount) * 150;

                    return (
                      <div
                        key={trend.period}
                        style={{
                          flex: 1,
                          display: "flex",
                          flexDirection: "column",
                          alignItems: "center",
                          gap: 8,
                        }}
                      >
                        <div
                          style={{
                            fontSize: "0.7rem",
                            color: "var(--text-muted)",
                          }}
                        >
                          {trend.period}
                        </div>
                        <div
                          style={{
                            display: "flex",
                            gap: 4,
                            alignItems: "flex-end",
                            height: 150,
                          }}
                        >
                          <div
                            style={{
                              width: 24,
                              height: height || 5,
                              background: "#3b82f6",
                              borderRadius: "4px 4px 0 0",
                              position: "relative",
                            }}
                          >
                            <div
                              style={{
                                position: "absolute",
                                top: -20,
                                left: "50%",
                                transform: "translateX(-50%)",
                                fontSize: "0.7rem",
                                fontWeight: 600,
                              }}
                            >
                              {trend.count}
                            </div>
                          </div>
                          <div
                            style={{
                              width: 24,
                              height: winHeight || 5,
                              background: "#22c55e",
                              borderRadius: "4px 4px 0 0",
                              position: "relative",
                            }}
                          >
                            <div
                              style={{
                                position: "absolute",
                                top: -20,
                                left: "50%",
                                transform: "translateX(-50%)",
                                fontSize: "0.7rem",
                                fontWeight: 600,
                                color: "#22c55e",
                              }}
                            >
                              {wins}
                            </div>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}

              {/* LINE CHART */}
              {chartType === "line" && (
                <div style={{ padding: "20px 0", minWidth: 600 }}>
                  <svg
                    width="100%"
                    height="200"
                    style={{ overflow: "visible" }}
                  >
                    {/* Grid lines */}
                    {[0, 25, 50, 75, 100].map((y) => (
                      <line
                        key={y}
                        x1="40"
                        y1={180 - y * 1.5}
                        x2="95%"
                        y2={180 - y * 1.5}
                        stroke="rgba(255,255,255,0.18)"
                        strokeWidth="1"
                      />
                    ))}

                    {(() => {
                      const maxCount = Math.max(
                        ...analytics.quotationTrends.map((t) => t.count),
                        1,
                      );
                      const width =
                        typeof window !== "undefined"
                          ? window.innerWidth * 0.8
                          : 800;
                      const stepX =
                        (width - 100) /
                        Math.max(analytics.quotationTrends.length - 1, 1);

                      // Quotations line
                      const quotationPoints = analytics.quotationTrends
                        .map((trend, i) => {
                          const x = 60 + i * stepX;
                          const y = 180 - (trend.count / maxCount) * 150;
                          return `${x},${y}`;
                        })
                        .join(" ");

                      // Wins line
                      const winPoints = analytics.quotationTrends
                        .map((trend, i) => {
                          const wins =
                            analytics.winTrends.find(
                              (w) => w.period === trend.period,
                            )?.count || 0;
                          const x = 60 + i * stepX;
                          const y = 180 - (wins / maxCount) * 150;
                          return `${x},${y}`;
                        })
                        .join(" ");

                      return (
                        <>
                          {/* Quotations line */}
                          <polyline
                            points={quotationPoints}
                            fill="none"
                            stroke="#3b82f6"
                            strokeWidth="3"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                          />
                          {/* Wins line */}
                          <polyline
                            points={winPoints}
                            fill="none"
                            stroke="#22c55e"
                            strokeWidth="3"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                          />
                          {/* Data points */}
                          {analytics.quotationTrends.map((trend, i) => {
                            const x = 60 + i * stepX;
                            const y = 180 - (trend.count / maxCount) * 150;
                            const wins =
                              analytics.winTrends.find(
                                (w) => w.period === trend.period,
                              )?.count || 0;
                            const yWin = 180 - (wins / maxCount) * 150;
                            return (
                              <g key={trend.period}>
                                <circle cx={x} cy={y} r="5" fill="#3b82f6" />
                                <circle cx={x} cy={yWin} r="5" fill="#22c55e" />
                                <text
                                  x={x}
                                  y="195"
                                  textAnchor="middle"
                                  fontSize="10"
                                  fill="var(--text-muted)"
                                >
                                  {trend.period}
                                </text>
                              </g>
                            );
                          })}
                        </>
                      );
                    })()}
                  </svg>
                </div>
              )}

              {/* DONUT CHART */}
              {chartType === "donut" && (
                <div
                  style={{
                    display: "flex",
                    justifyContent: "center",
                    alignItems: "center",
                    padding: "40px 0",
                    gap: 40,
                  }}
                >
                  <svg width="250" height="250" viewBox="0 0 250 250">
                    {(() => {
                      const total = analytics.totalQuotationsSubmitted;
                      const won = analytics.totalRfqsWon;
                      const rejected = analytics.rejectedQuotations;
                      const pending = total - won - rejected;

                      if (total === 0) {
                        return (
                          <>
                            <circle
                              cx="125"
                              cy="125"
                              r="80"
                              fill="none"
                              stroke="#e5e7eb"
                              strokeWidth="40"
                            />
                            <text
                              x="125"
                              y="125"
                              textAnchor="middle"
                              fontSize="16"
                              fill="var(--text-muted)"
                            >
                              No Data
                            </text>
                          </>
                        );
                      }

                      const wonPercent = (won / total) * 100;
                      const rejectedPercent = (rejected / total) * 100;
                      const pendingPercent = (pending / total) * 100;

                      const circumference = 2 * Math.PI * 80;
                      const wonDash = (wonPercent / 100) * circumference;
                      const rejectedDash =
                        (rejectedPercent / 100) * circumference;
                      const pendingDash =
                        (pendingPercent / 100) * circumference;

                      let offset = 0;

                      return (
                        <>
                          {/* Won segment */}
                          <circle
                            cx="125"
                            cy="125"
                            r="80"
                            fill="none"
                            stroke="#22c55e"
                            strokeWidth="40"
                            strokeDasharray={`${wonDash} ${circumference - wonDash}`}
                            strokeDashoffset={-offset}
                            transform="rotate(-90 125 125)"
                          />
                          {/* Rejected segment */}
                          {rejected > 0 && (
                            <circle
                              cx="125"
                              cy="125"
                              r="80"
                              fill="none"
                              stroke="#ef4444"
                              strokeWidth="40"
                              strokeDasharray={`${rejectedDash} ${circumference - rejectedDash}`}
                              strokeDashoffset={-(offset + wonDash)}
                              transform="rotate(-90 125 125)"
                            />
                          )}
                          {/* Pending segment */}
                          {pending > 0 && (
                            <circle
                              cx="125"
                              cy="125"
                              r="80"
                              fill="none"
                              stroke="#f59e0b"
                              strokeWidth="40"
                              strokeDasharray={`${pendingDash} ${circumference - pendingDash}`}
                              strokeDashoffset={
                                -(offset + wonDash + rejectedDash)
                              }
                              transform="rotate(-90 125 125)"
                            />
                          )}
                          {/* Center text */}
                          <text
                            x="125"
                            y="115"
                            textAnchor="middle"
                            fontSize="32"
                            fontWeight="700"
                            fill="var(--text)"
                          >
                            {total}
                          </text>
                          <text
                            x="125"
                            y="140"
                            textAnchor="middle"
                            fontSize="14"
                            fill="var(--text-muted)"
                          >
                            Total
                          </text>
                        </>
                      );
                    })()}
                  </svg>

                  {/* Legend */}
                  <div
                    style={{
                      display: "flex",
                      flexDirection: "column",
                      gap: 16,
                    }}
                  >
                    <div
                      style={{ display: "flex", alignItems: "center", gap: 12 }}
                    >
                      <div
                        style={{
                          width: 20,
                          height: 20,
                          background: "#22c55e",
                          borderRadius: 4,
                        }}
                      />
                      <div>
                        <div style={{ fontSize: "0.875rem", fontWeight: 600 }}>
                          Won: {analytics.totalRfqsWon}
                        </div>
                        <div
                          style={{
                            fontSize: "0.75rem",
                            color: "var(--text-muted)",
                          }}
                        >
                          {analytics.totalQuotationsSubmitted > 0
                            ? (
                                (analytics.totalRfqsWon /
                                  analytics.totalQuotationsSubmitted) *
                                100
                              ).toFixed(1)
                            : 0}
                          %
                        </div>
                      </div>
                    </div>
                    <div
                      style={{ display: "flex", alignItems: "center", gap: 12 }}
                    >
                      <div
                        style={{
                          width: 20,
                          height: 20,
                          background: "#ef4444",
                          borderRadius: 4,
                        }}
                      />
                      <div>
                        <div style={{ fontSize: "0.875rem", fontWeight: 600 }}>
                          Rejected: {analytics.rejectedQuotations}
                        </div>
                        <div
                          style={{
                            fontSize: "0.75rem",
                            color: "var(--text-muted)",
                          }}
                        >
                          {analytics.totalQuotationsSubmitted > 0
                            ? (
                                (analytics.rejectedQuotations /
                                  analytics.totalQuotationsSubmitted) *
                                100
                              ).toFixed(1)
                            : 0}
                          %
                        </div>
                      </div>
                    </div>
                    <div
                      style={{ display: "flex", alignItems: "center", gap: 12 }}
                    >
                      <div
                        style={{
                          width: 20,
                          height: 20,
                          background: "#f59e0b",
                          borderRadius: 4,
                        }}
                      />
                      <div>
                        <div style={{ fontSize: "0.875rem", fontWeight: 600 }}>
                          Pending:{" "}
                          {analytics.totalQuotationsSubmitted -
                            analytics.totalRfqsWon -
                            analytics.rejectedQuotations}
                        </div>
                        <div
                          style={{
                            fontSize: "0.75rem",
                            color: "var(--text-muted)",
                          }}
                        >
                          {analytics.totalQuotationsSubmitted > 0
                            ? (
                                ((analytics.totalQuotationsSubmitted -
                                  analytics.totalRfqsWon -
                                  analytics.rejectedQuotations) /
                                  analytics.totalQuotationsSubmitted) *
                                100
                              ).toFixed(1)
                            : 0}
                          %
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* Legend for Bar and Line charts */}
              {(chartType === "bar" || chartType === "line") && (
                <div
                  style={{
                    display: "flex",
                    justifyContent: "center",
                    gap: 24,
                    marginTop: 16,
                    fontSize: "0.875rem",
                  }}
                >
                  <div>
                    <span
                      style={{
                        display: "inline-block",
                        width: 12,
                        height: 12,
                        background: "#3b82f6",
                        borderRadius: 2,
                        marginRight: 6,
                      }}
                    />
                    Quotations
                  </div>
                  <div>
                    <span
                      style={{
                        display: "inline-block",
                        width: 12,
                        height: 12,
                        background: "#22c55e",
                        borderRadius: 2,
                        marginRight: 6,
                      }}
                    />
                    Wins
                  </div>
                </div>
              )}
              <div className="chart-analysis" style={{ marginTop: 16 }}>
                <span className="analysis-icon">
                  <i className="bi bi-lightning-charge-fill" />
                </span>
                <span>
                  Peak activity: {peakTrend.period} with {peakTrend.count} quotations. Win rate is {winRate}% ({analytics.totalRfqsWon} wins), with {activeQuotes} still active.
                </span>
              </div>
            </div>
          ) : (
            <div
              style={{
                textAlign: "center",
                padding: 32,
                color: "var(--text-muted)",
              }}
            >
              No trend data available for this period
            </div>
          )}
        </div>
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))",
          gap: 16,
          marginBottom: 20,
        }}
      >
        <div className="card chart-card">
          <div className="card-header">
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <span className="header-icon" style={{ background: "rgba(14,165,233,0.25)" }}>
                <i className="bi bi-currency-dollar" />
              </span>
              Bid Range Spectrum
            </div>
          </div>
          <div className="card-body">
            <div className="range-track">
              <div className="range-fill" />
              <div className="range-marker" style={{ left: `${avgBidPos}%` }}>
                <span className="range-marker-label">Avg</span>
              </div>
            </div>
            <div className="range-labels">
              <span>Low: {fmt.currency(analytics.lowestBid, analytics.currency)}</span>
              <span>High: {fmt.currency(analytics.highestBid, analytics.currency)}</span>
            </div>
            <div className="chart-analysis">
              <span className="analysis-icon">
                <i className="bi bi-cash-stack" />
              </span>
              <span>
                Spread is {fmt.currency(bidSpread, analytics.currency)}; average bid sits at {Math.round(avgBidPos)}% of the range.
              </span>
            </div>
          </div>
        </div>
        <div className="card chart-card">
          <div className="card-header">
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <span className="header-icon" style={{ background: "rgba(34,197,94,0.25)" }}>
                <i className="bi bi-speedometer2" />
              </span>
              Delivery Pace Gauge
            </div>
          </div>
          <div className="card-body">
            <div
              className="delivery-gauge"
              style={{
                background: `conic-gradient(#22c55e ${onTimeRate}%, rgba(255,255,255,0.12) ${onTimeRate}% 100%)`,
              }}
            >
              <div className="delivery-gauge-inner">
                <div className="delivery-gauge-value">{onTimeRate}%</div>
                <div className="delivery-gauge-label">On-time</div>
              </div>
            </div>
            <div className="chart-analysis">
              <span className="analysis-icon">
                <i className="bi bi-truck" />
              </span>
              <span>
                {analytics.onTimePOs} of {analytics.totalPOs} orders delivered on time. Target: 80%+.
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Detailed Statistics */}
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16 }}>
        <div className="card">
          <div className="card-header">
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <span className="header-icon" style={{ background: "rgba(99,102,241,0.25)" }}>
                <i className="bi bi-clipboard-data" />
              </span>
              Quotation Statistics
            </div>
          </div>
          <div className="card-body">
            <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
              {[
                {
                  label: "Total Submitted",
                  value: analytics.totalQuotationsSubmitted,
                },
                {
                  label: "Awarded",
                  value: analytics.totalRfqsWon,
                  color: "#22c55e",
                },
                {
                  label: "Rejected",
                  value: analytics.rejectedQuotations,
                  color: "#ef4444",
                },
                {
                  label: "Lowest Bid",
                  value: fmt.currency(analytics.lowestBid, analytics.currency),
                },
                {
                  label: "Highest Bid",
                  value: fmt.currency(analytics.highestBid, analytics.currency),
                },
                {
                  label: "Average Bid",
                  value: fmt.currency(
                    analytics.averageBidAmount,
                    analytics.currency,
                  ),
                },
              ].map((stat) => (
                <div
                  key={stat.label}
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    padding: "8px 0",
                    borderBottom: "1px solid var(--border)",
                  }}
                >
                  <span
                    style={{ fontSize: "0.875rem", color: "var(--text-muted)" }}
                  >
                    {stat.label}
                  </span>
                  <span
                    style={{ fontWeight: 600, color: stat.color || "inherit" }}
                  >
                    {stat.value}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card-header">
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <span className="header-icon" style={{ background: "rgba(34,197,94,0.25)" }}>
                <i className="bi bi-truck" />
              </span>
              Delivery Performance
            </div>
          </div>
          <div className="card-body">
            <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
              {[
                { label: "Total Purchase Orders", value: analytics.totalPOs },
                {
                  label: "On-Time Deliveries",
                  value: analytics.onTimePOs,
                  color: "#22c55e",
                },
                {
                  label: "Late Deliveries",
                  value: analytics.totalPOs - analytics.onTimePOs,
                  color: "#ef4444",
                },
                {
                  label: "On-Time Rate",
                  value: `${analytics.onTimeDeliveryRate}%`,
                  color:
                    analytics.onTimeDeliveryRate >= 80 ? "#22c55e" : "#f59e0b",
                },
              ].map((stat) => (
                <div
                  key={stat.label}
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    padding: "8px 0",
                    borderBottom: "1px solid var(--border)",
                  }}
                >
                  <span
                    style={{ fontSize: "0.875rem", color: "var(--text-muted)" }}
                  >
                    {stat.label}
                  </span>
                  <span
                    style={{ fontWeight: 600, color: stat.color || "inherit" }}
                  >
                    {stat.value}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default VendorAnalyticsDashboard;
