import { Sidebar } from '@/components/sidebar';
import '../styles/globals.css';

// This layout applies to all pages inside the (main) group
export default function MainAppLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <>
      <Sidebar />
      <main className="flex-1 overflow-auto">{children}</main>
    </>
  );
}