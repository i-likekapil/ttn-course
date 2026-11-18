import { ApiConfigError, ApiError } from "./errors";
import { responseToApiError, wrapFetchError } from "./parseProblem";

export function getApiBaseUrl(): string {
  const base = process.env.NEXT_PUBLIC_API_URL;
  if (!base) {
    throw new ApiConfigError();
  }
  return base.replace(/\/$/, "");
}

type HttpMethod = "GET" | "POST" | "PATCH";

export interface RequestOptions {
  method?: HttpMethod;
  body?: unknown;
  headers?: Record<string, string>;
}

export async function apiRequest<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const base = getApiBaseUrl();
  const url = path.startsWith("/") ? `${base}${path}` : `${base}/${path}`;
  const { method = "GET", body, headers = {} } = options;

  const init: RequestInit = {
    method,
    headers: {
      Accept: "application/json",
      ...headers,
    },
  };

  if (body !== undefined) {
    init.headers = {
      ...init.headers,
      "Content-Type": "application/json",
    };
    init.body = JSON.stringify(body);
  }

  try {
    const response = await fetch(url, init);
    if (!response.ok) {
      throw await responseToApiError(response);
    }
    if (response.status === 204) {
      return undefined as T;
    }
    return (await response.json()) as T;
  } catch (error) {
    if (error instanceof ApiConfigError) {
      throw error;
    }
    if (error instanceof ApiError) {
      throw error;
    }
    throw wrapFetchError(error);
  }
}
