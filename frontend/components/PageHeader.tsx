import Link from "next/link";

interface PageHeaderProps {
  title: string;
  action?: { href: string; label: string };
}

export function PageHeader({ title, action }: PageHeaderProps) {
  return (
    <header className="header">
      <h1>{title}</h1>
      {action ? (
        <Link href={action.href} className="btn btn-primary">
          {action.label}
        </Link>
      ) : null}
    </header>
  );
}
