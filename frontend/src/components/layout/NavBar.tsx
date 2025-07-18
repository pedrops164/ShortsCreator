'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useSession, signIn, signOut } from 'next-auth/react';
import { LogIn, LogOut, LayoutGrid, FileText, PlusSquare, User } from 'lucide-react';
import { ThemeToggleButton } from '@/components/ThemeToggleButton';

const navItems = [
  { name: 'Create Video', path: '/create', icon: <PlusSquare size={18} /> },
  { name: 'Content', path: '/content', icon: <LayoutGrid size={18} /> },
  { name: 'Drafts', path: '/drafts', icon: <FileText size={18} /> },
  { name: 'Account', path: '/account', icon: <User size={18} /> },
];

export default function NavBar({ children }: Readonly<{children: React.ReactNode;}>) {
  const pathname = usePathname();
  const { data: session, status } = useSession(); // Get session status and data

  return (
    <div className="bg-background min-h-screen font-sans text-foreground flex flex-col lg:flex-row">
      {/* --- Sidebar Navigation --- */}
      <nav className="border-b border-accent lg:border-b-0 lg:border-r lg:w-64 flex flex-col">
        {/* --- Logo and Theme Toggle --- */}
        <div className="flex-shrink-0 flex h-16 items-center justify-between px-4">
          <div className="font-bold text-lg">Shorts Creator</div>
          <ThemeToggleButton />
        </div>
        
        {/* --- Navigation Links (Main Section) --- */}
        <div className="hidden lg:flex flex-col flex-1 p-2 mt-8">
          <div className="space-y-2">
            {navItems.map(item => (
              <Link
                key={item.name}
                href={item.path}
                className={`flex items-center gap-3 rounded-md px-3 py-2 text-sm transition-colors ${
                  pathname.startsWith(item.path)
                    ? 'bg-primary text-white font-semibold'
                    : 'text-foreground/70 hover:bg-accent hover:text-foreground/90'
                }`}
              >
                {item.icon}
                {item.name}
              </Link>
            ))}
          </div>
          
          {/* --- Login/Logout Button (Bottom Section) --- */}
          {/* mt-auto pushes this div to the bottom of the flex container */}
          <div className="mt-auto">
            {status === 'authenticated' && (
              <div className="p-2 space-y-2">
                 <p className="px-3 text-xs text-accent truncate">
                    Signed in as {session.user?.email}
                </p>
                <button
                  onClick={() => signOut()}
                  className="w-full flex items-center gap-3 rounded-md px-3 py-2 text-sm text-foreground/70 hover:bg-accent hover:text-foreground/90"
                >
                  <LogOut size={18} />
                  <span>Logout</span>
                </button>
              </div>
            )}
             {status === 'unauthenticated' && (
                <div className="p-2">
                    <button
                        onClick={() => signIn('keycloak')}
                        className="w-full flex items-center gap-3 rounded-md px-3 py-2 text-sm text-foreground/70 hover:bg-accent hover:text-foreground/90"
                    >
                        <LogIn size={18}/>
                        <span>Login</span>
                    </button>
                </div>
            )}
          </div>
        </div>
      </nav>

      {/* --- Main Content --- */}
      <main className="flex-1 p-4 sm:p-6 lg:p-8">
        {children} {/* Page components get rendered here */}
      </main>
    </div>
  );
}