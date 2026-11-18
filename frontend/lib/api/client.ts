import { apiRequest } from "./http";
import type {
  CommentResponse,
  CreateCommentRequest,
  CreateTicketRequest,
  ListTicketsParams,
  PagedTicketResponse,
  TicketDetailResponse,
  TicketResponse,
  TicketStatus,
  TransitionStatusRequest,
  UpdateTicketRequest,
} from "./types";

function buildQuery(params: ListTicketsParams): string {
  const search = new URLSearchParams();
  if (params.status) {
    search.set("status", params.status);
  }
  if (params.q) {
    search.set("q", params.q);
  }
  if (params.page !== undefined) {
    search.set("page", String(params.page));
  }
  if (params.size !== undefined) {
    search.set("size", String(params.size));
  }
  const qs = search.toString();
  return qs ? `?${qs}` : "";
}

/** Typed client for spec/api-contract.md */
export const ticketApi = {
  createTicket(body: CreateTicketRequest): Promise<TicketResponse> {
    return apiRequest<TicketResponse>("/tickets", { method: "POST", body });
  },

  listTickets(params: ListTicketsParams = {}): Promise<PagedTicketResponse> {
    return apiRequest<PagedTicketResponse>(`/tickets${buildQuery(params)}`);
  },

  getTicket(id: number): Promise<TicketDetailResponse> {
    return apiRequest<TicketDetailResponse>(`/tickets/${id}`);
  },

  updateTicket(id: number, body: UpdateTicketRequest): Promise<TicketResponse> {
    return apiRequest<TicketResponse>(`/tickets/${id}`, {
      method: "PATCH",
      body,
    });
  },

  transitionStatus(
    id: number,
    status: TicketStatus,
  ): Promise<TicketResponse> {
    const body: TransitionStatusRequest = { status };
    return apiRequest<TicketResponse>(`/tickets/${id}/status`, {
      method: "POST",
      body,
    });
  },

  addComment(
    id: number,
    body: CreateCommentRequest,
  ): Promise<CommentResponse> {
    return apiRequest(`/tickets/${id}/comments`, { method: "POST", body });
  },
};

export default ticketApi;
