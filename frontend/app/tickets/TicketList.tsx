"use client";

import {
  isNetworkApiError,
  ticketApi,
  type TicketResponse,
  type TicketStatus,
} from "@/lib/api";
import { formatDate, formatStatus, TICKET_STATUSES } from "@/lib/labels";
import { useDebouncedValue } from "@/lib/useDebouncedValue";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useCallback, useEffect, useState } from "react";

const PAGE_SIZE = 20;

type StatusFilter = TicketStatus | "ALL";

export function TicketList() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const initialQ = searchParams.get("q") ?? "";
  const statusParam = searchParams.get("status");
  const initialStatus: StatusFilter =
    statusParam && TICKET_STATUSES.includes(statusParam as TicketStatus)
      ? (statusParam as TicketStatus)
      : "ALL";
  const initialPage = Number(searchParams.get("page") ?? "0") || 0;

  const [keyword, setKeyword] = useState(initialQ);
  const debouncedQ = useDebouncedValue(keyword, 300);
  const [statusFilter, setStatusFilter] = useState<StatusFilter>(initialStatus);
  const [page, setPage] = useState(initialPage);

  const [tickets, setTickets] = useState<TicketResponse[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const syncUrl = useCallback(
    (q: string, status: StatusFilter, p: number) => {
      const params = new URLSearchParams();
      if (q.trim()) {
        params.set("q", q.trim());
      }
      if (status !== "ALL") {
        params.set("status", status);
      }
      if (p > 0) {
        params.set("page", String(p));
      }
      const qs = params.toString();
      router.replace(qs ? `/tickets?${qs}` : "/tickets", { scroll: false });
    },
    [router],
  );

  useEffect(() => {
    syncUrl(debouncedQ, statusFilter, page);
  }, [debouncedQ, statusFilter, page, syncUrl]);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const result = await ticketApi.listTickets({
          q: debouncedQ.trim() || undefined,
          status: statusFilter === "ALL" ? undefined : statusFilter,
          page,
          size: PAGE_SIZE,
        });
        if (!cancelled) {
          setTickets(result.content);
          setTotalPages(result.totalPages);
          setTotalElements(result.totalElements);
        }
      } catch (e) {
        if (!cancelled) {
          setTickets([]);
          if (isNetworkApiError(e)) {
            setError("Could not reach the server. Check that the API is running.");
          } else if (e instanceof Error) {
            setError(e.message);
          } else {
            setError("Failed to load tickets.");
          }
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [debouncedQ, statusFilter, page]);

  function onStatusChange(value: string) {
    setStatusFilter(value as StatusFilter);
    setPage(0);
  }

  function onSearchChange(value: string) {
    setKeyword(value);
    setPage(0);
  }

  return (
    <>
      <div className="toolbar card">
        <input
          type="search"
          placeholder="Search title or description…"
          value={keyword}
          onChange={(e) => onSearchChange(e.target.value)}
          aria-label="Search tickets"
        />
        <select
          value={statusFilter}
          onChange={(e) => onStatusChange(e.target.value)}
          aria-label="Filter by status"
        >
          <option value="ALL">All statuses</option>
          {TICKET_STATUSES.map((s) => (
            <option key={s} value={s}>
              {formatStatus(s)}
            </option>
          ))}
        </select>
      </div>

      {error ? <div className="alert alert-error" role="alert">{error}</div> : null}

      <div className="card">
        {loading ? (
          <p className="muted">Loading tickets…</p>
        ) : tickets.length === 0 ? (
          <p className="muted">No tickets found.</p>
        ) : (
          <div className="table-wrap">
            <table className="tickets">
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Status</th>
                  <th>Priority</th>
                  <th>Assignee</th>
                  <th>Updated</th>
                </tr>
              </thead>
              <tbody>
                {tickets.map((t) => (
                  <tr key={t.id}>
                    <td>
                      <Link href={`/tickets/${t.id}`} className="row-link">
                        {t.title}
                      </Link>
                    </td>
                    <td>
                      <span className="badge">{formatStatus(t.status)}</span>
                    </td>
                    <td>{t.priority}</td>
                    <td>{t.assignee ?? "—"}</td>
                    <td className="muted">{formatDate(t.updatedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {!loading && totalPages > 1 ? (
          <div className="pagination">
            <button
              type="button"
              className="btn"
              disabled={page <= 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Previous
            </button>
            <span className="muted">
              Page {page + 1} of {totalPages} ({totalElements} tickets)
            </span>
            <button
              type="button"
              className="btn"
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </button>
          </div>
        ) : null}
      </div>
    </>
  );
}
