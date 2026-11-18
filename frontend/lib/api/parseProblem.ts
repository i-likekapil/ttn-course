import {
  ApiError,
  ClientApiError,
  ConflictApiError,
  NetworkApiError,
  NotFoundApiError,
  ValidationApiError,
} from "./errors";
import type { ProblemResponse } from "./types";

export async function parseProblemResponse(
  response: Response,
): Promise<ProblemResponse | null> {
  const contentType = response.headers.get("content-type") ?? "";
  if (!contentType.includes("application/json")) {
    return null;
  }
  try {
    const body = (await response.json()) as ProblemResponse;
    if (
      typeof body.status === "number" &&
      typeof body.title === "string" &&
      typeof body.detail === "string"
    ) {
      return body;
    }
  } catch {
    return null;
  }
  return null;
}

export function problemToApiError(
  problem: ProblemResponse,
  httpStatus: number,
): ApiError {
  const normalized: ProblemResponse = {
    ...problem,
    status: problem.status || httpStatus,
  };

  switch (httpStatus) {
    case 400:
      return new ValidationApiError(normalized);
    case 404:
      return new NotFoundApiError(normalized);
    case 409:
      return new ConflictApiError(normalized);
    default:
      if (httpStatus >= 400 && httpStatus < 500) {
        return new ClientApiError(normalized);
      }
      return new ClientApiError({
        ...normalized,
        detail: normalized.detail || `Request failed with status ${httpStatus}`,
      });
  }
}

export async function responseToApiError(response: Response): Promise<ApiError> {
  const problem = await parseProblemResponse(response);
  if (problem) {
    return problemToApiError(problem, response.status);
  }
  return new ClientApiError({
    status: response.status,
    title: "Error",
    detail: response.statusText || `Request failed with status ${response.status}`,
  });
}

export function wrapFetchError(cause: unknown): NetworkApiError {
  return new NetworkApiError(cause);
}
