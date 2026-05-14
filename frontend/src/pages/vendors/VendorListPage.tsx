import React, { useEffect, useState, useCallback } from "react";
import { Link } from "react-router-dom";
import { toast } from "react-toastify";
import { vendorAPI } from "../../services/api";
import { Vendor, VendorStatus } from "../../types";
import {
  StatusBadge,
  Pagination,
  Spinner,
  EmptyState,
  PageHeader,
  fmt,
} from "../../components/common/SharedComponents";

/**
 * US 12: Advanced Filtering and Search for Vendors
 *
 * Features:
 * - AC #1: Search by name, GST, registration ID
 * - AC #4: Multi-criteria filtering (status, compliance)
 * - AC #5: Pagination support
 * - AC #6: Sorting (ascending/descending)
 * - AC #7: Case-insensitive search (backend)
 * - AC #8: Debounced search input (500ms delay)
 * - AC #9: No-result state with appropriate message
 * - AC #11: Filters resettable with single action
 */
const VendorListPage: React.FC = () => {
  const [vendors, setVendors] = useState<Vendor[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [status, setStatus] = useState<VendorStatus | "">("");
  const [compliant, setCompliant] = useState<string>(""); // '', 'true', 'false'
  const [sortField, setSortField] = useState("registeredAt");
  const [sortDirection, setSortDirection] = useState<"asc" | "desc">("desc");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [total, setTotal] = useState(0);

  // US 12 AC #8: Debounced search input (500ms delay)
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(search);
      setPage(0); // Reset to first page on search change
    }, 500);

    return () => clearTimeout(timer);
  }, [search]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      // US 12 AC #4: Multi-criteria filtering
      const params: any = {
        page,
        size: 10,
        sort: `${sortField},${sortDirection}`,
      };

      if (debouncedSearch) params.search = debouncedSearch;
      if (status) params.status = status;
      if (compliant) params.compliant = compliant === "true";

      const res = await vendorAPI.getAll(params);
      const d = res.data.data;
      setVendors(d.content);
      setTotalPages(d.totalPages);
      setTotal(d.totalElements);
    } catch {
      toast.error("Failed to load vendors");
    } finally {
      setLoading(false);
    }
  }, [debouncedSearch, status, compliant, sortField, sortDirection, page]);

  useEffect(() => {
    load();
  }, [load]);

  // US 12 AC #11: Reset all filters with single action
  const resetFilters = () => {
    setSearch("");
    setDebouncedSearch("");
    setStatus("");
    setCompliant("");
    setSortField("registeredAt");
    setSortDirection("desc");
    setPage(0);
  };

  // US 12 AC #6: Toggle sort direction
  const toggleSort = (field: string) => {
    if (sortField === field) {
      setSortDirection(sortDirection === "asc" ? "desc" : "asc");
    } else {
      setSortField(field);
      setSortDirection("asc");
    }
    setPage(0);
  };

  return (
    <div>
      <PageHeader
        title="Vendor Management"
        subtitle={`${total} vendor${total !== 1 ? "s" : ""} found`}
      />

      {/* US 12 AC #4: Multi-criteria filters */}
      <div
        className="filter-bar"
        style={{ marginBottom: 16, gap: 8, flexWrap: "wrap" }}
      >
        <div
          className="search-input-wrap"
          style={{ flex: "1 1 300px", minWidth: 200 }}
        >
          <i className="bi bi-search" />
          <input
            className="form-control"
            placeholder="Search by name, GST or registration ID..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          {search && (
            <i
              className="bi bi-x-circle"
              style={{
                position: "absolute",
                right: 10,
                cursor: "pointer",
                color: "var(--text-muted)",
              }}
              onClick={() => setSearch("")}
            />
          )}
        </div>

        <select
          className="form-control form-select"
          style={{ width: 180 }}
          value={status}
          onChange={(e) => {
            setStatus(e.target.value as any);
            setPage(0);
          }}
        >
          <option value="">All Statuses</option>
          <option value="PENDING_APPROVAL">Pending Approval</option>
          <option value="APPROVED">Approved</option>
          <option value="REJECTED">Rejected</option>
          <option value="SUSPENDED">Suspended</option>
        </select>

        <select
          className="form-control form-select"
          style={{ width: 160 }}
          value={compliant}
          onChange={(e) => {
            setCompliant(e.target.value);
            setPage(0);
          }}
        >
          <option value="">All Compliance</option>
          <option value="true">Compliant</option>
          <option value="false">Non-Compliant</option>
        </select>

        {/* US 12 AC #6: Sort selector */}
        <select
          className="form-control form-select"
          style={{ width: 180 }}
          value={`${sortField},${sortDirection}`}
          onChange={(e) => {
            const [field, dir] = e.target.value.split(",");
            setSortField(field);
            setSortDirection(dir as "asc" | "desc");
            setPage(0);
          }}
        >
          <option value="registeredAt,desc">Newest First</option>
          <option value="registeredAt,asc">Oldest First</option>
          <option value="companyName,asc">Name (A-Z)</option>
          <option value="companyName,desc">Name (Z-A)</option>
        </select>

        {/* US 12 AC #11: Reset filters button */}
        <button
          className="btn btn-secondary"
          onClick={resetFilters}
          title="Clear all filters"
        >
          <i className="bi bi-x-circle" /> Clear
        </button>
      </div>

      <div className="table-wrapper">
        {loading ? (
          <Spinner />
        ) : vendors.length === 0 ? (
          /* US 12 AC #9: No-result state with appropriate message */
          <EmptyState
            icon="bi-people"
            title="No vendors found"
            desc={
              debouncedSearch || status || compliant
                ? "No vendors match your search criteria. Try adjusting your filters."
                : "No vendors have registered yet."
            }
            action={
              debouncedSearch || status || compliant ? (
                <button
                  className="btn btn-primary btn-sm"
                  onClick={resetFilters}
                >
                  <i className="bi bi-x-circle" /> Clear Filters
                </button>
              ) : undefined
            }
          />
        ) : (
          <>
            <div style={{ overflowX: "auto" }}>
              <table className="table">
                <thead>
                  <tr>
                    <th>#</th>
                    <th
                      style={{ cursor: "pointer", userSelect: "none" }}
                      onClick={() => toggleSort("companyName")}
                    >
                      Company{" "}
                      {sortField === "companyName" && (
                        <i
                          className={`bi bi-arrow-${sortDirection === "asc" ? "up" : "down"}`}
                        />
                      )}
                    </th>
                    <th>GST Number</th>
                    <th>Registration ID</th>
                    <th>Email</th>
                    <th>Status</th>
                    <th>Compliance</th>
                    <th
                      style={{ cursor: "pointer", userSelect: "none" }}
                      onClick={() => toggleSort("registeredAt")}
                    >
                      Registered{" "}
                      {sortField === "registeredAt" && (
                        <i
                          className={`bi bi-arrow-${sortDirection === "asc" ? "up" : "down"}`}
                        />
                      )}
                    </th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {vendors.map((v, i) => (
                    <tr key={v.id}>
                      <td
                        style={{
                          color: "var(--text-muted)",
                          fontSize: "0.8rem",
                        }}
                      >
                        {page * 10 + i + 1}
                      </td>
                      <td>
                        <div style={{ fontWeight: 600 }}>{v.companyName}</div>
                        {v.contactPerson && (
                          <div
                            style={{
                              color: "var(--text-muted)",
                              fontSize: "0.78rem",
                            }}
                          >
                            {v.contactPerson}
                          </div>
                        )}
                      </td>
                      <td>
                        <span className="code-text">{v.gstNumber}</span>
                      </td>
                      <td style={{ fontSize: "0.82rem" }}>
                        {v.registrationId}
                      </td>
                      <td style={{ fontSize: "0.82rem" }}>{v.email}</td>
                      <td>
                        <StatusBadge status={v.status} />
                      </td>
                      <td>
                        <span
                          className={`badge ${
                            v.isCompliant ? "badge-success" : "badge-danger"
                          }`}
                        >
                          {v.isCompliant ? "✓ Compliant" : "✗ Non-Compliant"}
                        </span>
                      </td>
                      <td
                        style={{
                          fontSize: "0.8rem",
                          color: "var(--text-muted)",
                        }}
                      >
                        {fmt.date(v.registeredAt)}
                      </td>
                      <td>
                        <Link
                          to={`/vendors/${v.id}`}
                          className="btn btn-outline-primary btn-sm"
                        >
                          <i className="bi bi-eye" /> View
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {/* US 12 AC #5: Pagination support */}
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

export default VendorListPage;
