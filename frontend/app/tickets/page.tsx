import { PageHeader } from "@/components/PageHeader";
import { Suspense } from "react";
import { TicketList } from "./TicketList";

export default function TicketsPage() {
  return (
    <>
      <PageHeader
        title="Tickets"
        action={{ href: "/tickets/new", label: "Create ticket" }}
      />
      <Suspense fallback={<p className="muted">Loading…</p>}>
        <TicketList />
      </Suspense>
    </>
  );
}
