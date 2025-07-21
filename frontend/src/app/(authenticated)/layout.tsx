import AuthenticatedLayout from '@/components/layout/AuthenticatedLayout';
import '../styles/globals.css';
import NavBar from "@/components/layout/NavBar";

// This layout applies to all pages inside the (main) group
export default function MainAppLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <AuthenticatedLayout>
      <NavBar>
        {children}
      </NavBar>
    </AuthenticatedLayout>
  );
}