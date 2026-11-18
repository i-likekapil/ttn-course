import { describe, expect, it } from "vitest";
import { getLegalNextStatuses } from "./transitions";

describe("getLegalNextStatuses", () => {
  it("matches state machine edges", () => {
    expect(getLegalNextStatuses("OPEN")).toEqual(["IN_PROGRESS", "CANCELLED"]);
    expect(getLegalNextStatuses("IN_PROGRESS")).toEqual([
      "RESOLVED",
      "CANCELLED",
    ]);
    expect(getLegalNextStatuses("RESOLVED")).toEqual(["CLOSED"]);
    expect(getLegalNextStatuses("CLOSED")).toEqual([]);
    expect(getLegalNextStatuses("CANCELLED")).toEqual([]);
  });
});
