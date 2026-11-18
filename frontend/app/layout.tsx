import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Support Tickets",
  description: "Support ticket management",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <div className="container">{children}</div>
      </body>
    </html>
  );
}
