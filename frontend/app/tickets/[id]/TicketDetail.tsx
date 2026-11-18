"use client";

import {
  isConflictApiError,
  isNetworkApiError,
  isNotFoundApiError,
  isValidationApiError,
  ticketApi,
  type CommentResponse,
  type Priority,
  type TicketDetailResponse,
  type TicketStatus,
} from "@/lib/api";
import { formatDate, formatStatus, PRIORITIES } from "@/lib/labels";
import { getLegalNextStatuses, statusActionLabel } from "@/lib/transitions";
import Link from "next/link";
import { useCallback, useEffect, useState } from "react";

interface TicketDetailProps {
  ticketId: number;
}

export function TicketDetail({ ticketId }: TicketDetailProps) {
  const [ticket, setTicket] = useState<TicketDetailResponse | null>(null);
  const [comments, setComments] = useState<CommentResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState<Priority>("MEDIUM");
  const [assignee, setAssignee] = useState("");

  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [saveError, setSaveError] = useState<string | null>(null);
  const [statusError, setStatusError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [transitioning, setTransitioning] = useState<TicketStatus | null>(null);

  const [commentBody, setCommentBody] = useState("");
  const [commentAuthor, setCommentAuthor] = useState("");
  const [commentFieldErrors, setCommentFieldErrors] = useState<
    Record<string, string>
  >({});
  const [commentError, setCommentError] = useState<string | null>(null);
  const [commentSubmitting, setCommentSubmitting] = useState(false);

  const applyTicket = useCallback((data: TicketDetailResponse) => {
    setTicket(data);
    setComments(data.comments);
    setTitle(data.title);
    setDescription(data.description);
    setPriority(data.priority);
    setAssignee(data.assignee ?? "");
  }, []);

  const loadTicket = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    setNotFound(false);
    try {
      const data = await ticketApi.getTicket(ticketId);
      applyTicket(data);
    } catch (e) {
      setTicket(null);
      if (isNotFoundApiError(e)) {
        setNotFound(true);
      } else if (isNetworkApiError(e)) {
        setLoadError("Could not reach the server. Check that the API is running.");
      } else if (e instanceof Error) {
        setLoadError(e.message);
      } else {
        setLoadError("Failed to load ticket.");
      }
    } finally {
      setLoading(false);
    }
  }, [ticketId, applyTicket]);

  useEffect(() => {
    loadTicket();
  }, [loadTicket]);

  async function handleSave(e: React.FormEvent) {
    e.preventDefault();
    setSaveError(null);
    setFieldErrors({});
    if (!title.trim()) {
      setFieldErrors({ title: "Title is required" });
      return;
    }
    setSaving(true);
    try {
      const updated = await ticketApi.updateTicket(ticketId, {
        title: title.trim(),
        description,
        priority,
        assignee: assignee.trim() ? assignee.trim() : null,
      });
      setTicket((prev) =>
        prev ? { ...prev, ...updated, comments: prev.comments } : null,
      );
    } catch (err) {
      if (isValidationApiError(err)) {
        setFieldErrors(err.fieldErrorMap());
      } else if (isNetworkApiError(err)) {
        setSaveError("Could not reach the server.");
      } else if (err instanceof Error) {
        setSaveError(err.message);
      }
    } finally {
      setSaving(false);
    }
  }

  async function handleTransition(target: TicketStatus) {
    setStatusError(null);
    setTransitioning(target);
    try {
      const updated = await ticketApi.transitionStatus(ticketId, target);
      setTicket((prev) =>
        prev ? { ...prev, ...updated, comments: prev.comments } : null,
      );
    } catch (err) {
      if (isConflictApiError(err)) {
        setStatusError(err.detail);
      } else if (isNetworkApiError(err)) {
        setStatusError("Could not reach the server.");
      } else if (err instanceof Error) {
        setStatusError(err.message);
      } else {
        setStatusError("Status change failed.");
      }
    } finally {
      setTransitioning(null);
    }
  }

  async function handleAddComment(e: React.FormEvent) {
    e.preventDefault();
    setCommentError(null);
    setCommentFieldErrors({});
    const clientErrors: Record<string, string> = {};
    if (!commentBody.trim()) {
      clientErrors.body = "Comment is required";
    }
    if (!commentAuthor.trim()) {
      clientErrors.author = "Author is required";
    }
    if (Object.keys(clientErrors).length > 0) {
      setCommentFieldErrors(clientErrors);
      return;
    }
    setCommentSubmitting(true);
    try {
      const created = await ticketApi.addComment(ticketId, {
        body: commentBody.trim(),
        author: commentAuthor.trim(),
      });
      setComments((prev) => [...prev, created]);
      setCommentBody("");
    } catch (err) {
      if (isValidationApiError(err)) {
        setCommentFieldErrors(err.fieldErrorMap());
      } else if (isNetworkApiError(err)) {
        setCommentError("Could not reach the server.");
      } else if (err instanceof Error) {
        setCommentError(err.message);
      }
    } finally {
      setCommentSubmitting(false);
    }
  }

  if (loading) {
    return <p className="muted">Loading ticket…</p>;
  }

  if (notFound) {
    return (
      <div className="card">
        <p>Ticket not found.</p>
        <p>
          <Link href="/tickets">Back to tickets</Link>
        </p>
      </div>
    );
  }

  if (loadError || !ticket) {
    return (
      <div className="alert alert-error" role="alert">
        {loadError ?? "Unknown error"}
      </div>
    );
  }

  const nextStatuses = getLegalNextStatuses(ticket.status);

  return (
    <div className="detail-layout">
      <section className="card detail-section">
        <div className="detail-meta">
          <span className="badge">{formatStatus(ticket.status)}</span>
          <span className="muted">Updated {formatDate(ticket.updatedAt)}</span>
        </div>

        {statusError ? (
          <div className="alert alert-error" role="alert">{statusError}</div>
        ) : null}

        {nextStatuses.length > 0 ? (
          <div className="status-actions">
            {nextStatuses.map((target) => (
              <button
                key={target}
                type="button"
                className="btn"
                disabled={transitioning !== null}
                onClick={() => handleTransition(target)}
              >
                {transitioning === target
                  ? "Updating…"
                  : statusActionLabel(target)}
              </button>
            ))}
          </div>
        ) : (
          <p className="muted">No further status actions for this ticket.</p>
        )}

        <form className="form" onSubmit={handleSave} style={{ marginTop: "1rem" }}>
          {saveError ? (
            <div className="alert alert-error" role="alert">{saveError}</div>
          ) : null}

          <div className="form-field">
            <label htmlFor="title">Title</label>
            <input
              id="title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className={fieldErrors.title ? "input-error" : ""}
            />
            {fieldErrors.title ? (
              <p className="field-error">{fieldErrors.title}</p>
            ) : null}
          </div>

          <div className="form-field">
            <label htmlFor="description">Description</label>
            <textarea
              id="description"
              rows={6}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className={fieldErrors.description ? "input-error" : ""}
            />
            {fieldErrors.description ? (
              <p className="field-error">{fieldErrors.description}</p>
            ) : null}
          </div>

          <div className="form-field">
            <label htmlFor="priority">Priority</label>
            <select
              id="priority"
              value={priority}
              onChange={(e) => setPriority(e.target.value as Priority)}
            >
              {PRIORITIES.map((p) => (
                <option key={p} value={p}>{p}</option>
              ))}
            </select>
          </div>

          <div className="form-field">
            <label htmlFor="assignee">Assignee</label>
            <input
              id="assignee"
              value={assignee}
              onChange={(e) => setAssignee(e.target.value)}
              placeholder="Leave empty to clear"
            />
            <p className="muted" style={{ marginTop: "0.25rem" }}>
              Save with an empty assignee to clear.
            </p>
          </div>

          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? "Saving…" : "Save changes"}
            </button>
          </div>
        </form>
      </section>

      <section className="card detail-section">
        <h2 className="section-title">Comments</h2>
        {comments.length === 0 ? (
          <p className="muted">No comments yet.</p>
        ) : (
          <ul className="comment-list">
            {comments.map((c) => (
              <li key={c.id} className="comment-item">
                <div className="comment-meta">
                  <strong>{c.author}</strong>
                  <span className="muted">{formatDate(c.createdAt)}</span>
                </div>
                <p className="comment-body">{c.body}</p>
              </li>
            ))}
          </ul>
        )}

        <form className="form" onSubmit={handleAddComment} style={{ marginTop: "1rem" }}>
          {commentError ? (
            <div className="alert alert-error" role="alert">{commentError}</div>
          ) : null}
          <div className="form-field">
            <label htmlFor="comment-body">Comment</label>
            <textarea
              id="comment-body"
              rows={3}
              value={commentBody}
              onChange={(e) => setCommentBody(e.target.value)}
              className={commentFieldErrors.body ? "input-error" : ""}
            />
            {commentFieldErrors.body ? (
              <p className="field-error">{commentFieldErrors.body}</p>
            ) : null}
          </div>
          <div className="form-field">
            <label htmlFor="comment-author">Author</label>
            <input
              id="comment-author"
              value={commentAuthor}
              onChange={(e) => setCommentAuthor(e.target.value)}
              className={commentFieldErrors.author ? "input-error" : ""}
            />
            {commentFieldErrors.author ? (
              <p className="field-error">{commentFieldErrors.author}</p>
            ) : null}
          </div>
          <button
            type="submit"
            className="btn btn-primary"
            disabled={commentSubmitting}
          >
            {commentSubmitting ? "Adding…" : "Add comment"}
          </button>
        </form>
      </section>
    </div>
  );
}
