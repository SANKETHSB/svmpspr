/**
 * US 12: Advanced Filtering and Search Component
 *
 * This component provides:
 * - US 12 AC #8: Debounced frontend search input
 * - US 12 AC #9: No-result state with appropriate message
 * - US 12 AC #11: Resettable filters with single action
 */

import React, { useState, useEffect, useCallback } from "react";
import { useDebounce } from "../../hooks/useDebounce";

/**
 * US 12 AC #9: No-result state component
 */
interface EmptySearchStateProps {
  search?: string;
  hasFilters: boolean;
  entityName: string;
  onClear?: () => void;
}

export const EmptySearchState: React.FC<EmptySearchStateProps> = ({
  search,
  hasFilters,
  entityName,
  onClear,
}) => {
  const message = search
    ? `No ${entityName} found matching "${search}"`
    : hasFilters
      ? `No ${entityName} match the selected filters`
      : `No ${entityName} found`;

  return (
    <div
      className="empty-state"
      style={{ padding: "40px 20px", textAlign: "center" }}
    >
      <i
        className="bi bi-search"
        style={{
          fontSize: "3rem",
          color: "var(--text-muted)",
          marginBottom: 16,
        }}
      />
      <h3 style={{ margin: "0 0 8px", color: "var(--text)" }}>{message}</h3>
      <p style={{ color: "var(--text-muted)", margin: "0 0 16px" }}>
        {search
          ? "Try adjusting your search term or filters"
          : hasFilters
            ? "Try clearing some filters to see more results"
            : `Create your first ${entityName}`}
      </p>
      {(search || hasFilters) && onClear && (
        <button onClick={onClear} className="btn btn-outline-primary btn-sm">
          <i className="bi bi-x-circle" style={{ marginRight: 6 }} />
          Clear All Filters
        </button>
      )}
    </div>
  );
};

/**
 * US 12 AC #8: Debounced Search Input Component
 */
interface SearchInputProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  delay?: number;
  autoFocus?: boolean;
  onClear?: () => void;
}

export const SearchInput: React.FC<SearchInputProps> = ({
  value,
  onChange,
  placeholder = "Search...",
  delay = 300,
  autoFocus = false,
  onClear,
}) => {
  const [localValue, setLocalValue] = useState(value);

  // Sync local value with prop changes
  useEffect(() => {
    setLocalValue(value);
  }, [value]);

  // Debounce the value change
  const [debouncedValue] = useDebounce(localValue, delay);

  // Call onChange when debounced value changes
  useEffect(() => {
    if (debouncedValue !== value) {
      onChange(debouncedValue);
    }
  }, [debouncedValue, onChange, value]);

  const handleClear = () => {
    setLocalValue("");
    onChange("");
    onClear?.();
  };

  return (
    <div className="search-input-wrap" style={{ flex: 1 }}>
      <i className="bi bi-search" />
      <input
        type="text"
        className="form-control"
        placeholder={placeholder}
        value={localValue}
        onChange={(e) => setLocalValue(e.target.value)}
        autoFocus={autoFocus}
      />
      {localValue && (
        <button
          className="search-clear-btn"
          onClick={handleClear}
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
        >
          <i className="bi bi-x-circle" />
        </button>
      )}
    </div>
  );
};

/**
 * US 12 AC #11: Resettable Filters Component
 */
interface FilterBarProps {
  search: string;
  onSearchChange: (value: string) => void;
  searchPlaceholder?: string;
  filters: React.ReactNode;
  onReset: () => void;
  hasActiveFilters: boolean;
}

export const FilterBar: React.FC<FilterBarProps> = ({
  search,
  onSearchChange,
  searchPlaceholder = "Search...",
  filters,
  onReset,
  hasActiveFilters,
}) => {
  return (
    <div className="filter-bar" style={{ marginBottom: 16 }}>
      <SearchInput
        value={search}
        onChange={onSearchChange}
        placeholder={searchPlaceholder}
      />
      {filters}
      {hasActiveFilters && (
        <button
          onClick={onReset}
          className="btn btn-outline-secondary btn-sm"
          style={{ marginLeft: 8 }}
          title="Clear all filters"
        >
          <i className="bi bi-x-circle" style={{ marginRight: 4 }} />
          Clear Filters
        </button>
      )}
    </div>
  );
};

/**
 * US 12 AC #6: Sort Selector Component
 */
interface SortSelectorProps {
  sortBy: string;
  sortOrder: "asc" | "desc";
  onChange: (sortBy: string, sortOrder: "asc" | "desc") => void;
  options: { value: string; label: string }[];
}

export const SortSelector: React.FC<SortSelectorProps> = ({
  sortBy,
  sortOrder,
  onChange,
  options,
}) => {
  const handleChange = (value: string) => {
    if (value === sortBy) {
      // Toggle sort order if same field
      onChange(sortBy, sortOrder === "asc" ? "desc" : "asc");
    } else {
      // Reset to descending for new field
      onChange(value, "desc");
    }
  };

  return (
    <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
      <select
        className="form-control form-select"
        style={{ width: "auto", minWidth: 150 }}
        value={sortBy}
        onChange={(e) => handleChange(e.target.value)}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      <button
        className="btn btn-outline-secondary btn-sm"
        onClick={() => onChange(sortBy, sortOrder === "asc" ? "desc" : "asc")}
        title={`Sort ${sortOrder === "asc" ? "Descending" : "Ascending"}`}
      >
        <i className={`bi bi-sort-${sortOrder === "asc" ? "up" : "down"}`} />
      </button>
    </div>
  );
};

/**
 * US 12 AC #5: Pagination Info Component
 */
interface PaginationInfoProps {
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
}

export const PaginationInfo: React.FC<PaginationInfoProps> = ({
  page,
  pageSize,
  totalElements,
  totalPages,
}) => {
  const start = totalElements === 0 ? 0 : page * pageSize + 1;
  const end = Math.min((page + 1) * pageSize, totalElements);

  return (
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
        Showing {start}–{end} of {totalElements} results
        {totalPages > 1 && ` (Page ${page + 1} of ${totalPages})`}
      </span>
    </div>
  );
};

/**
 * US 12 AC #12: Performance indicator component
 * Shows search execution time
 */
interface PerformanceIndicatorProps {
  executionTime?: number; // in milliseconds
  threshold?: number; // warning threshold in milliseconds
}

export const PerformanceIndicator: React.FC<PerformanceIndicatorProps> = ({
  executionTime,
  threshold = 1000, // 1 second default threshold
}) => {
  if (!executionTime) return null;

  const isSlow = executionTime > threshold;
  const color = isSlow ? "#ef4444" : "#22c55e";

  return (
    <small
      style={{
        fontSize: "0.7rem",
        color: "var(--text-muted)",
        marginLeft: 8,
      }}
      title={`Search completed in ${executionTime}ms`}
    >
      {isSlow ? "⚠️" : "✓"} {executionTime}ms
    </small>
  );
};

export default FilterBar;
