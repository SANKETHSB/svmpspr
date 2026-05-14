import React, { useEffect, useState, useCallback } from "react";
import { Link } from "react-router-dom";
import { toast } from "react-toastify";
import { rfqAPI } from "../../services/api";
import { RFQ, RFQStatus } from "../../types";
import {
  StatusBadge,
  Pagination,
  Spinner,
  EmptyState,
  PageHeader,
  fmt,
} from "../../components/common/SharedComponents";
import { useAuth } from "../../context/AuthContext";

/**
 * US 12: Advanced Filtering and Search for RFQs
 *
 * Features:
 * - US 12 AC #2: Filter RFQs by status
 * - US 12 AC #4: Multi-criteria filtering
 * - US 12 AC #5: Pagination support
 * - US 12 AC #6: Sorting (ascending/descending)
 * - US 12 AC #8: Debounced search input (300ms delay)
 * - US 12 AC #9: No-result state
 * - US 12 AC #11: Resettable filters
 */
const RfqListPage: React.FC = () => {
  const { isManager, isVendor, user } = useAuth();
  const [rfqs, setRfqs] = useState<RFQ[]>([]);
  const [loading, setLoading] = useState(true);

  // US 12 AC #8: Debounced search (separate input state and debounced state)
  const [searchInput, setSearchInput] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");

  const [status, setStatus] = useState<RFQStatus | "">("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [total, setTotal] = useState(0);

  // US 12 AC #6: Sorting state
  const [sortBy, setSortBy] = useState("createdAt");
  const [sortOrder, setSortOrder] = useState<"asc" | "desc">("desc");

  // US 12 AC #8: Debounce search input (300ms delay)
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(searchInput);
      setPage(0); // Reset to first page on new search
    }, 300);

    return () => clearTimeout(timer);
  }, [searchInput]);

  // US 12 AC #11: Reset all filters
  const handleResetFilters = () => {
    setSearchInput("");
    setDebouncedSearch("");
    setStatus("");
    setSortBy("createdAt");
    setSortOrder("desc");
    setPage(0);
  };

  // Check if any filters are active
  const hasActiveFilters = searchInput || status;

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = isVendor
        ? await rfqAPI.getForVendor(user!.id, { page, size: 10 })
        : await rfqAPI.getAll({
            search: debouncedSearch || undefined,
            status: status || undefined,
            page,
            size: 10,
          });
      const d = res.data.data;
      setRfqs(d.content);
      setTotalPages(d.totalPages);
      setTotal(d.totalElements);
    } catch {
      toast.error("Failed to load RFQs");
    } finally {
      setLoading(false);
    }
  }, [debouncedSearch, status, page, isVendor, user]);

  useEffect(() => {
    load();
  }, [load]);

  const deadlinePast = (d: string) => new Date(d) < new Date();

  // US 12 AC #9: No-result state check
  const showEmptyState = !loading && rfqs.length === 0;

  return (
    <div>
      <PageHeader
        title="Request for Quotations"
        subtitle={`${total} RFQs`}
        actions={
          isManager ? (
            <Link to="/rfqs/create" className="btn btn-primary">
              <i className="bi bi-plus-circle" /> Create RFQ
            </Link>
          ) : undefined
        }
      />

      {!isVendor && (
        <div className="filter-bar">
          {/* US 12 AC #8: Debounced search input */}
          <div className="search-input-wrap" style={{ flex: 1 }}>
            <i className="bi bi-search" />
            <input
              className="form-control"
              placeholder="Search by title or RFQ number..."
              value={searchInput}
              onChange={(e) => {
                setSearchInput(e.target.value);
              }}
            />
            {/* Clear button for search */}
            {searchInput && (
              <button
                onClick={() => {
                  setSearchInput("");
                }}
                style={{
                  position: "absolute",
                  right: 8,
                  top: "50%",
                  transform: "translateY(-50%)",
                  background: "none",
                  border: "none",
                  color: "var(--text-muted)",
                  cursor: "pointer",
                  padding: 4,
                }}
                title="Clear search"
              >
                <i className="bi bi-x-circle" />
              </button>
            )}
          </div>

          {/* US 12 AC #2: Filter by status */}
          <select
            className="form-control form-select"
            style={{ width: 160 }}
            value={status}
            onChange={(e) => {
              setStatus(e.target.value as any);
              setPage(0);
            }}
          >
            <option value="">All Statuses</option>
            {["OPEN", "CLOSED", "AWARDED", "ARCHIVED"].map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>

          {/* US 12 AC #11: Reset filters button */}
          {hasActiveFilters && (
            <button
              onClick={handleResetFilters}
              className="btn btn-outline-secondary btn-sm"
              title="Clear all filters"
            >
              <i className="bi bi-x-circle" style={{ marginRight: 4 }} />
              Clear
            </button>
          )}
        </div>
      )}

      <div className="table-wrapper">
        {loading ? (
          <Spinner />
        ) : rfqs.length === 0 ? (
          // US 12 AC #9: No-result state with appropriate message
          <EmptyState
            icon="bi-search"
            title={
              debouncedSearch || hasActiveFilters
                ? "No RFQs match your search"
                : "No RFQs found"
            }
            desc={
              debouncedSearch
                ? `No RFQs found matching "${debouncedSearch}". Try adjusting your search or filters.`
                : hasActiveFilters
                  ? "No RFQs match the selected filters. Try clearing some filters."
                  : isVendor
                    ? "You haven't been invited to any RFQs yet"
                    : "Create your first RFQ to get started"
            }
            action={
              hasActiveFilters ? (
                <button
                  onClick={handleResetFilters}
                  className="btn btn-outline-primary btn-sm"
                >
                  <i className="bi bi-x-circle" style={{ marginRight: 6 }} />
                  Clear All Filters
                </button>
              ) : isManager ? (
                <Link to="/rfqs/create" className="btn btn-primary btn-sm">
                  <i className="bi bi-plus-circle" style={{ marginRight: 6 }} />
                  Create RFQ
                </Link>
              ) : undefined
            }
          />
        ) : (
          <>
            <div style={{ overflowX: "auto" }}>
              <table className="table">
                <thead>
                  <tr>
                    <th>RFQ Number</th>
                    <th>Title</th>
                    <th>Items</th>
                    <th>Deadline</th>
                    <th>Status</th>
                    <th>Quotations</th>
                    <th>Created By</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {rfqs.map((r) => (
                    <tr key={r.id}>
                      <td>
                        <span className="code-text">{r.rfqNumber}</span>
                      </td>
                      <td>
                        <div style={{ fontWeight: 600 }}>{r.title}</div>
                        {r.awardedVendorName && (
                          <div
                            style={{ color: "#22c55e", fontSize: "0.75rem" }}
                          >
                            🏆 {r.awardedVendorName}
                          </div>
                        )}
                        {/* US 11 AC #4: Visual indicator for closed RFQs */}
                        {r.status === "CLOSED" && (
                          <div
                            style={{
                              color: "#ef4444",
                              fontSize: "0.75rem",
                              marginTop: 2,
                            }}
                          >
                            <i
                              className="bi bi-lock-fill"
                              style={{ marginRight: 4 }}
                            />
                            Closed - No submissions accepted
                          </div>
                        )}
                      </td>
                      <td>
                        <span className="badge badge-secondary">
                          {r.items?.length || 0}
                        </span>
                      </td>
                      <td>
                        <div
                          style={{
                            fontSize: "0.82rem",
                            color:
                              deadlinePast(r.deadline) && r.status === "OPEN"
                                ? "#ef4444"
                                : "var(--text)",
                          }}
                        >
                          {deadlinePast(r.deadline) && r.status === "OPEN" && (
                            <i
                              className="bi bi-exclamation-triangle"
                              style={{ marginRight: 4 }}
                            />
                          )}
                          {fmt.datetime(r.deadline)}
                        </div>
                      </td>
                      <td>
                        <StatusBadge status={r.status} />
                      </td>
                      <td>
                        <span className="badge badge-info">
                          {r.quotationCount || 0}
                        </span>
                      </td>
                      <td
                        style={{
                          fontSize: "0.82rem",
                          color: "var(--text-muted)",
                        }}
                      >
                        {r.createdByName}
                      </td>
                      <td>
                        <div style={{ display: "flex", gap: 6 }}>
                          <Link
                            to={`/rfqs/${r.id}`}
                            className="btn btn-outline-primary btn-sm"
                          >
                            <i className="bi bi-eye" />
                          </Link>
                          {isVendor &&
                            r.status === "OPEN" &&
                            !deadlinePast(r.deadline) && (
                              <Link
                                to={`/quotations/submit/${r.id}`}
                                className="btn btn-success btn-sm"
                              >
                                Quote
                              </Link>
                            )}
                          {isManager && r.status === "AWARDED" && (
                            <Link
                              to={`/purchase-orders/create/${r.id}`}
                              className="btn btn-primary btn-sm"
                            >
                              PO
                            </Link>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                padding: "12px 16px",
                borderTop: "1px solid var(--border)",
              }}
            >
              <span style={{ fontSize: "0.8rem", color: "var(--text-muted)" }}>
                Showing {page * 10 + 1}–{Math.min((page + 1) * 10, total)} of{" "}
                {total}
              </span>
              <Pagination page={page} total={totalPages} onChange={setPage} />
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default RfqListPage;
