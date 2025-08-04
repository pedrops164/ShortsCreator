import AuthenticatedLayout from '@/components/layout/AuthenticatedLayout';
import '../styles/globals.css';
import { Sidebar } from '@/components/sidebar';

// This layout applies to all pages inside the (main) group
export default function MainAppLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <AuthenticatedLayout>
      <Sidebar />
      <main className="flex-1 overflow-auto">{children}</main>
    </AuthenticatedLayout>
  );
}