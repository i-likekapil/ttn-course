import { TicketDetail } from "./TicketDetail";
import Link from "next/link";

interface PageProps {
  params: Promise<{ id: string }>;
}

export default async function TicketDetailPage({ params }: PageProps) {
  const { id } = await params;
  const ticketId = Number(id);

  if (!Number.isFinite(ticketId) || ticketId <= 0) {
    return (
      <div className="card">
        <p>Invalid ticket id.</p>
        <Link href="/tickets">Back to tickets</Link>
      </div>
    );
  }

  return (
    <>
      <p className="muted" style={{ marginBottom: "0.5rem" }}>
        <Link href="/tickets">← Back to tickets</Link>
      </p>
      <h1 style={{ marginTop: 0 }}>Ticket #{ticketId}</h1>
      <TicketDetail ticketId={ticketId} />
    </>
  );
}
