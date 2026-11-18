import type { FieldError, ProblemResponse } from "./types";

export type ApiErrorKind =
  | "validation"
  | "not_found"
  | "conflict"
  | "client"
  | "network"
  | "unknown";

/**
 * Base API failure with parsed ProblemResponse when available.
 */
export class ApiError extends Error {
  readonly kind: ApiErrorKind;
  readonly status: number;
  readonly title: string;
  readonly detail: string;
  readonly fieldErrors: FieldError[];
  readonly raw?: ProblemResponse;

  constructor(
    message: string,
    options: {
      kind: ApiErrorKind;
      status: number;
      title: string;
      detail: string;
      fieldErrors?: FieldError[];
      raw?: ProblemResponse;
    },
  ) {
    super(message);
    this.name = "ApiError";
    this.kind = options.kind;
    this.status = options.status;
    this.title = options.title;
    this.detail = options.detail;
    this.fieldErrors = options.fieldErrors ?? [];
    this.raw = options.raw;
  }

  /** Map field name → message (first error per field). */
  fieldErrorMap(): Record<string, string> {
    const map: Record<string, string> = {};
    for (const err of this.fieldErrors) {
      if (!map[err.field]) {
        map[err.field] = err.message;
      }
    }
    return map;
  }

  messageForField(field: string): string | undefined {
    return this.fieldErrors.find((e) => e.field === field)?.message;
  }
}

/** HTTP 400 — validation / bad input */
export class ValidationApiError extends ApiError {
  constructor(problem: ProblemResponse) {
    super(problem.detail || "Validation failed", {
      kind: "validation",
      status: problem.status,
      title: problem.title,
      detail: problem.detail,
      fieldErrors: problem.errors ?? [],
      raw: problem,
    });
    this.name = "ValidationApiError";
  }
}

/** HTTP 404 */
export class NotFoundApiError extends ApiError {
  constructor(problem: ProblemResponse) {
    super(problem.detail || "Not found", {
      kind: "not_found",
      status: problem.status,
      title: problem.title,
      detail: problem.detail,
      fieldErrors: problem.errors ?? [],
      raw: problem,
    });
    this.name = "NotFoundApiError";
  }
}

/** HTTP 409 — invalid status transition */
export class ConflictApiError extends ApiError {
  constructor(problem: ProblemResponse) {
    super(problem.detail || "Conflict", {
      kind: "conflict",
      status: problem.status,
      title: problem.title,
      detail: problem.detail,
      fieldErrors: problem.errors ?? [],
      raw: problem,
    });
    this.name = "ConflictApiError";
  }
}

/** Other 4xx/5xx with problem body */
export class ClientApiError extends ApiError {
  constructor(problem: ProblemResponse) {
    super(problem.detail || problem.title || "Request failed", {
      kind: "client",
      status: problem.status,
      title: problem.title,
      detail: problem.detail,
      fieldErrors: problem.errors ?? [],
      raw: problem,
    });
    this.name = "ClientApiError";
  }
}

/** Fetch failed (offline, CORS, DNS, etc.) */
export class NetworkApiError extends ApiError {
  constructor(cause?: unknown) {
    const detail =
      cause instanceof Error
        ? cause.message
        : "Could not reach the server";
    super(detail, {
      kind: "network",
      status: 0,
      title: "Network error",
      detail,
    });
    this.name = "NetworkApiError";
  }
}

export class ApiConfigError extends Error {
  constructor() {
    super(
      "NEXT_PUBLIC_API_URL is not set. Copy frontend/.env.example to .env.local",
    );
    this.name = "ApiConfigError";
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}

export function isValidationApiError(error: unknown): error is ValidationApiError {
  return error instanceof ValidationApiError;
}

export function isConflictApiError(error: unknown): error is ConflictApiError {
  return error instanceof ConflictApiError;
}

export function isNotFoundApiError(error: unknown): error is NotFoundApiError {
  return error instanceof NotFoundApiError;
}

export function isNetworkApiError(error: unknown): error is NetworkApiError {
  return error instanceof NetworkApiError;
}
