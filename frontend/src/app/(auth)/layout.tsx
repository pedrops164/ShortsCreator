// This layout applies only to pages in the (auth) group, like /login
export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <>{children}</>; // Render the page directly without any wrappers
}