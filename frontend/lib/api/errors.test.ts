import { describe, expect, it } from "vitest";
import { problemToApiError } from "./parseProblem";
import {
  ConflictApiError,
  ValidationApiError,
  isConflictApiError,
  isValidationApiError,
} from "./errors";
import type { ProblemResponse } from "./types";

describe("problemToApiError", () => {
  it("maps 400 to ValidationApiError with field errors", () => {
    const problem: ProblemResponse = {
      status: 400,
      title: "Validation failed",
      detail: "One or more fields are invalid",
      errors: [{ field: "title", message: "must not be blank" }],
    };
    const err = problemToApiError(problem, 400);
    expect(err).toBeInstanceOf(ValidationApiError);
    expect(isValidationApiError(err)).toBe(true);
    expect(err.fieldErrorMap().title).toBe("must not be blank");
  });

  it("maps 409 to ConflictApiError with detail", () => {
    const problem: ProblemResponse = {
      status: 409,
      title: "Conflict",
      detail: "Cannot transition from CLOSED to OPEN",
    };
    const err = problemToApiError(problem, 409);
    expect(err).toBeInstanceOf(ConflictApiError);
    expect(isConflictApiError(err)).toBe(true);
    expect(err.detail).toContain("CLOSED");
  });
});
