import { PageHeader } from "@/components/PageHeader";
import Link from "next/link";
import { CreateTicketForm } from "./CreateTicketForm";

export default function NewTicketPage() {
  return (
    <>
      <p className="muted" style={{ marginBottom: "0.5rem" }}>
        <Link href="/tickets">← Back to tickets</Link>
      </p>
      <PageHeader title="Create ticket" />
      <CreateTicketForm />
    </>
  );
}
