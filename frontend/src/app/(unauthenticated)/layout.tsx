import { Sidebar } from '@/components/sidebar';
import '../styles/globals.css';
import { BalanceProvider } from '@/context/BalanceContext';
import { NotificationProvider } from '@/context/NotificationContext';

// This layout applies to all pages inside the (main) group
export default function MainAppLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <>
      <NotificationProvider>
        <BalanceProvider>
          <Sidebar />
          <main className="flex-1 overflow-auto">{children}</main>
        </BalanceProvider>
      </NotificationProvider>
    </>
  );
}