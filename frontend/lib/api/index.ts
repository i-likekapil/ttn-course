export { ticketApi, default as ticketApiDefault } from "./client";
export { getApiBaseUrl, apiRequest } from "./http";
export {
  parseProblemResponse,
  problemToApiError,
  responseToApiError,
  wrapFetchError,
} from "./parseProblem";
export type * from "./types";
export {
  ApiError,
  ValidationApiError,
  NotFoundApiError,
  ConflictApiError,
  ClientApiError,
  NetworkApiError,
  ApiConfigError,
  isApiError,
  isValidationApiError,
  isConflictApiError,
  isNotFoundApiError,
  isNetworkApiError,
} from "./errors";
export type { ApiErrorKind } from "./errors";
