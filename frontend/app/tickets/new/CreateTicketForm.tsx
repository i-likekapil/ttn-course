"use client";

import {
  isNetworkApiError,
  isValidationApiError,
  ticketApi,
  type Priority,
} from "@/lib/api";
import { PRIORITIES } from "@/lib/labels";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";

export function CreateTicketForm() {
  const router = useRouter();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState<Priority>("MEDIUM");
  const [assignee, setAssignee] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setFormError(null);
    setFieldErrors({});

    const clientErrors: Record<string, string> = {};
    if (!title.trim()) {
      clientErrors.title = "Title is required";
    }
    if (description === null || description === undefined) {
      clientErrors.description = "Description is required";
    }
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      return;
    }

    setSubmitting(true);
    try {
      const created = await ticketApi.createTicket({
        title: title.trim(),
        description,
        priority,
        assignee: assignee.trim() ? assignee.trim() : undefined,
      });
      router.push(`/tickets/${created.id}`);
    } catch (err) {
      if (isValidationApiError(err)) {
        setFieldErrors(err.fieldErrorMap());
        if (!err.fieldErrors.length) {
          setFormError(err.detail);
        }
      } else if (isNetworkApiError(err)) {
        setFormError("Could not reach the server. Check that the API is running.");
      } else if (err instanceof Error) {
        setFormError(err.message);
      } else {
        setFormError("Failed to create ticket.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="form card" onSubmit={handleSubmit} noValidate>
      {formError ? (
        <div className="alert alert-error" role="alert">{formError}</div>
      ) : null}

      <div className="form-field">
        <label htmlFor="title">Title</label>
        <input
          id="title"
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          className={fieldErrors.title ? "input-error" : ""}
          aria-invalid={!!fieldErrors.title}
          aria-describedby={fieldErrors.title ? "title-error" : undefined}
        />
        {fieldErrors.title ? (
          <p id="title-error" className="field-error">{fieldErrors.title}</p>
        ) : null}
      </div>

      <div className="form-field">
        <label htmlFor="description">Description</label>
        <textarea
          id="description"
          rows={5}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          className={fieldErrors.description ? "input-error" : ""}
          aria-invalid={!!fieldErrors.description}
          aria-describedby={
            fieldErrors.description ? "description-error" : undefined
          }
        />
        {fieldErrors.description ? (
          <p id="description-error" className="field-error">
            {fieldErrors.description}
          </p>
        ) : null}
      </div>

      <div className="form-field">
        <label htmlFor="priority">Priority</label>
        <select
          id="priority"
          value={priority}
          onChange={(e) => setPriority(e.target.value as Priority)}
          className={fieldErrors.priority ? "input-error" : ""}
          aria-invalid={!!fieldErrors.priority}
        >
          {PRIORITIES.map((p) => (
            <option key={p} value={p}>{p}</option>
          ))}
        </select>
        {fieldErrors.priority ? (
          <p className="field-error">{fieldErrors.priority}</p>
        ) : null}
      </div>

      <div className="form-field">
        <label htmlFor="assignee">Assignee (optional)</label>
        <input
          id="assignee"
          type="text"
          value={assignee}
          onChange={(e) => setAssignee(e.target.value)}
          className={fieldErrors.assignee ? "input-error" : ""}
        />
        {fieldErrors.assignee ? (
          <p className="field-error">{fieldErrors.assignee}</p>
        ) : null}
      </div>

      <div className="form-actions">
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? "Creating…" : "Create ticket"}
        </button>
        <Link href="/tickets" className="btn">Cancel</Link>
      </div>
    </form>
  );
}
