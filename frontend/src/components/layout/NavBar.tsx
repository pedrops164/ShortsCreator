'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { ThemeToggleButton } from '@/components/ThemeToggleButton';

const navItems = [
  { name: 'Content', path: '/content' },
  { name: 'Drafts', path: '/drafts' },
  { name: 'Create Video', path: '/create' },
  { name: 'Account', path: '/account' },
];

export default function NavBar({ children }: Readonly<{children: React.ReactNode;}>) {
  const pathname = usePathname();

  return (
    <div className="bg-background min-h-screen font-sans text-foreground flex flex-col lg:flex-row">
      {/* --- Sidebar Navigation --- */}
      <nav className="border-b border-accent lg:border-b-0 lg:border-r lg:w-64">
        {/* --- Logo and Theme Toggle --- */}
        <div className="flex h-16 items-center justify-between px-4">
          <div className="font-bold text-lg">Shorts Creator</div>
          <ThemeToggleButton />
        </div>
        
        {/* --- Navigation Links --- */}
        <div className="hidden lg:flex flex-col gap-2 mt-8 p-2">
          {navItems.map(item => (
            <Link
              key={item.name}
              href={item.path}
              className={`flex items-center rounded-md px-3 py-2 text-sm transition-colors ${
                pathname.startsWith(item.path)
                  // + Active: Use `primary` for background and a contrasting text color
                  ? 'bg-primary text-white font-semibold'
                  // + Inactive: Use a muted foreground color and `accent` for hover
                  : 'hover:bg-accent hover:text-foreground/80 text-foreground/70'
              }`}
            >
              {item.name}
            </Link>
          ))}
        </div>
      </nav>

      {/* --- Main Content --- */}
      <main className="flex-1 p-4 sm:p-6 lg:p-8">
        {children} {/* Page components get rendered here */}
      </main>
    </div>
  );
}