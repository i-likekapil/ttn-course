import type { TicketStatus } from "@/lib/api";

/** Legal next statuses per spec/state-machine.md (UI hints only; API enforces). */
const NEXT_STATUS: Record<TicketStatus, TicketStatus[]> = {
  OPEN: ["IN_PROGRESS", "CANCELLED"],
  IN_PROGRESS: ["RESOLVED", "CANCELLED"],
  RESOLVED: ["CLOSED"],
  CLOSED: [],
  CANCELLED: [],
};

export function getLegalNextStatuses(current: TicketStatus): TicketStatus[] {
  return NEXT_STATUS[current] ?? [];
}

export function statusActionLabel(target: TicketStatus): string {
  switch (target) {
    case "IN_PROGRESS":
      return "Start progress";
    case "CANCELLED":
      return "Cancel ticket";
    case "RESOLVED":
      return "Mark resolved";
    case "CLOSED":
      return "Close ticket";
    default:
      return target.replace(/_/g, " ");
  }
}
