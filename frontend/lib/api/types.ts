/** Mirrors spec/api-contract.md and backend enums */

export type TicketStatus =
  | "OPEN"
  | "IN_PROGRESS"
  | "RESOLVED"
  | "CLOSED"
  | "CANCELLED";

export type Priority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";

export interface TicketResponse {
  id: number;
  title: string;
  description: string;
  priority: Priority;
  status: TicketStatus;
  assignee: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CommentResponse {
  id: number;
  ticketId: number;
  body: string;
  author: string;
  createdAt: string;
}

export interface TicketDetailResponse extends TicketResponse {
  comments: CommentResponse[];
}

export interface PagedTicketResponse {
  content: TicketResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateTicketRequest {
  title: string;
  description: string;
  priority: Priority;
  assignee?: string | null;
}

export interface UpdateTicketRequest {
  title?: string;
  description?: string;
  priority?: Priority;
  assignee?: string | null;
}

export interface TransitionStatusRequest {
  status: TicketStatus;
}

export interface CreateCommentRequest {
  body: string;
  author: string;
}

export interface ListTicketsParams {
  status?: TicketStatus;
  q?: string;
  page?: number;
  size?: number;
}

export interface ProblemResponse {
  status: number;
  title: string;
  detail: string;
  errors?: FieldError[];
}

export interface FieldError {
  field: string;
  message: string;
}
