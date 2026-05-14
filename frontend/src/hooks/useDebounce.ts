/**
 * US 12 AC #8: Debounced frontend search input required
 * 
 * This custom hook provides debounced search functionality to prevent
 * excessive API calls while the user is typing.
 * 
 * USAGE:
 * const [searchTerm, debouncedSearch, setSearchTerm] = useDebounce('', 500);
 * 
 * useEffect(() => {
 *   // API call with debouncedSearch
 *   fetchData(debouncedSearch);
 * }, [debouncedSearch]);
 */

import { useState, useEffect } from 'react';

/**
 * Custom hook for debounced search input
 * 
 * @param initialValue - Initial search value
 * @param delay - Debounce delay in milliseconds (default: 300ms)
 * @returns [value, debouncedValue, setValue] - Current value, debounced value, setter
 * 
 * @example
 * const [search, debouncedSearch, setSearch] = useDebounce('', 500);
 * 
 * // In your input:
 * <input value={search} onChange={(e) => setSearch(e.target.value)} />
 * 
 * // In your useEffect:
 * useEffect(() => {
 *   // This only triggers 500ms after user stops typing
 *   searchAPI(debouncedSearch);
 * }, [debouncedSearch]);
 */
export function useDebounce<T>(
  initialValue: T,
  delay: number = 300
): [T, T, (value: T | ((prev: T) => T)) => void] {
  const [value, setValue] = useState<T>(initialValue);
  const [debouncedValue, setDebouncedValue] = useState<T>(initialValue);

  useEffect(() => {
    // Set up a timer to update debounced value after delay
    const timer = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    // Clean up the timer if value changes or component unmounts
    // This resets the timer on each keystroke
    return () => {
      clearTimeout(timer);
    };
  }, [value, delay]);

  return [value, debouncedValue, setValue];
}

/**
 * Alternative hook for debounced callback functions
 * 
 * @param callback - Function to debounce
 * @param delay - Debounce delay in milliseconds
 * @returns Debounced function
 * 
 * @example
 * const debouncedSearch = useDebouncedCallback((term) => {
 *   searchAPI(term);
 * }, 500);
 * 
 * <input onChange={(e) => debouncedSearch(e.target.value)} />
 */
export function useDebouncedCallback<T extends (...args: any[]) => any>(
  callback: T,
  delay: number = 300
): (...args: Parameters<T>) => void {
  const [timeoutId, setTimeoutId] = useState<NodeJS.Timeout | null>(null);

  useEffect(() => {
    // Clean up timeout on unmount
    return () => {
      if (timeoutId) {
        clearTimeout(timeoutId);
      }
    };
  }, [timeoutId]);

  return (...args: Parameters<T>) => {
    // Clear previous timeout
    if (timeoutId) {
      clearTimeout(timeoutId);
    }

    // Set new timeout
    const newTimeoutId = setTimeout(() => {
      callback(...args);
    }, delay);

    setTimeoutId(newTimeoutId);
  };
}

/**
 * Hook for debounced search with loading state
 * 
 * @param searchFunction - Async function to call with search term
 * @param delay - Debounce delay in milliseconds
 * @returns { search, setSearch, results, loading, error }
 * 
 * @example
 * const { search, setSearch, results, loading } = useDebouncedSearch(
 *   async (term) => {
 *     const res = await api.get(`/vendors?search=${term}`);
 *     return res.data;
 *   },
 *   500
 * );
 */
export function useDebouncedSearch<T>(
  searchFunction: (term: string) => Promise<T[]>,
  delay: number = 300
): {
  search: string;
  setSearch: (value: string) => void;
  results: T[];
  loading: boolean;
  error: string | null;
  clearSearch: () => void;
} {
  const [search, setSearch] = useState('');
  const [debouncedSearch] = useDebounce(search, delay);
  const [results, setResults] = useState<T[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!debouncedSearch.trim()) {
      setResults([]);
      return;
    }

    const performSearch = async () => {
      setLoading(true);
      setError(null);
      
      try {
        const searchResults = await searchFunction(debouncedSearch);
        setResults(searchResults);
      } catch (err: any) {
        setError(err.message || 'Search failed');
        setResults([]);
      } finally {
        setLoading(false);
      }
    };

    performSearch();
  }, [debouncedSearch, searchFunction]);

  const clearSearch = () => {
    setSearch('');
    setResults([]);
    setError(null);
  };

  return { search, setSearch, results, loading, error, clearSearch };
}

export default useDebounce;
