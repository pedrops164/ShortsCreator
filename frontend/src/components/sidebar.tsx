"use client"

import { useState } from "react"
import { useBalance } from "@/context/BalanceContext"
import { formatPriceFromCents } from "@/lib/pricingUtils"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import { usePathname } from "next/navigation" 
import { Skeleton } from "@/components/ui/skeleton"
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip" // For collapsed view
import {
  CreditCard,
  HelpCircle,
  ChevronLeft,
  ChevronRight,
  PlusSquare,
  LayoutGrid,
  Wallet,
  LogOut,
  LogIn,
  SunMoon,
} from "lucide-react"
import { cn } from "@/lib/utils"
import { signOut, useSession } from "next-auth/react"
import Link from "next/link"
import Image from "next/image"
import { useTheme } from "next-themes"
import React from "react"

const navigationItems = [
  {
    title: "Create Video",
    icon: PlusSquare,
    href: "/create",
    badge: null,
  },
  {
    title: "Content Library",
    icon: LayoutGrid,
    href: "/content",
    badge: null,
  },
  {
    title: "Account & Billing",
    icon: CreditCard,
    href: "/account",
    badge: null,
  },
]

export function Sidebar() {
  const { status } = useSession(); // Get auth status
  const [isCollapsed, setIsCollapsed] = useState(false);
  // Get the current path
  const pathname = usePathname();
  const { balanceInCents, isLoading } = useBalance();
  
  // ✨ Get theme context and logic directly in the Sidebar
  const { setTheme, resolvedTheme } = useTheme()
  const toggleTheme = () => setTheme(resolvedTheme === "dark" ? "light" : "dark")
  
  return (
    <div
      className={cn(
        "relative flex h-screen flex-col border-r bg-card transition-all duration-300", // Added h-screen for full height
        isCollapsed ? "w-20" : "w-64", // Adjusted collapsed width for better centering
      )}
    >
      {/* Header */}
      <div className={cn("flex h-16 items-center border-b", isCollapsed ? "justify-center" : "px-4")}>
        <Link href="/create" className="flex items-center gap-3">
          <Image src="/logo.png" width={32} height={32} alt="Mad Shorts Logo" className="rounded-md" />
          <span
            className={cn(
              "text-lg font-bold text-primary whitespace-nowrap transition-opacity duration-300",
              isCollapsed && "sr-only", // Use sr-only for accessibility
            )}
          >
            Mad Shorts
          </span>
        </Link>
      </div>

      {/* Navigation */}
      <div className="flex-1 overflow-auto py-4">
        <nav className="space-y-1 px-2">
          {navigationItems.map((item) => {
            const Icon = item.icon
            const isActive = pathname === item.href
            return (
            <TooltipProvider key={item.title} delayDuration={0}>
                <Tooltip>
                <TooltipTrigger asChild>
                    <Button
                    variant={isActive ? "secondary" : "ghost"}
                    className={cn(
                        "w-full justify-start h-10",
                        // ✨ FIX: Removed px-2 from the collapsed state to allow perfect centering
                        isCollapsed && "justify-center",
                        isActive && "bg-yellow-100 text-yellow-900 dark:bg-yellow-900/20 dark:text-yellow-400",
                    )}
                    asChild
                    >
                    <Link href={item.href}>
                        <Icon className={cn("h-4 w-4", !isCollapsed && "mr-3")} />
                        <span className={cn(isCollapsed && "sr-only")}>{item.title}</span>
                    </Link>
                    </Button>
                </TooltipTrigger>
                <TooltipContent side="right">{item.title}</TooltipContent>
                </Tooltip>
            </TooltipProvider>
            )
          })}
        </nav>
      </div>

      {/* Unified Sidebar Footer */}
      <div className="mt-auto border-t p-2 space-y-1">
        <TooltipProvider delayDuration={0}>
          {/* Balance Section */}
          {status === 'authenticated' && (
            <Tooltip>
              <TooltipTrigger asChild>
                <Link href="/account" className="block">
                  <div className="flex h-10 items-center justify-between rounded-lg px-2 hover:bg-muted">
                    {isCollapsed ? (
                      <Wallet className="h-5 w-5 mx-auto" />
                    ) : (
                      <>
                        <div className="flex items-center gap-3">
                          <Wallet className="h-4 w-4" />
                          <span className="text-sm font-medium">Balance</span>
                        </div>
                        {isLoading ? (
                          <Skeleton className="h-5 w-14" />
                        ) : (
                          <span className="text-sm font-bold">
                            {formatPriceFromCents(balanceInCents ?? 0)}
                          </span>
                        )}
                      </>
                    )}
                  </div>
                </Link>
              </TooltipTrigger>
              <TooltipContent side="right">
                Balance: {isLoading ? "..." : formatPriceFromCents(balanceInCents ?? 0)}
              </TooltipContent>
            </Tooltip>
          )}

          {/* Theme Toggle Section */}
          <Tooltip>
            <TooltipTrigger asChild>
              <Button variant="ghost" className="w-full justify-start h-10 px-2" onClick={toggleTheme}>
                {isCollapsed ? (
                  <div className="flex justify-center w-full">
                      <SunMoon className="h-5 w-5" />
                  </div>
                ) : (
                  <div className="flex items-center justify-between w-full">
                    <div className="flex items-center gap-3">
                      <SunMoon className="h-5 w-5" />
                      <span className="text-sm font-medium">Theme</span>
                    </div>
                  </div>
                )}
              </Button>
            </TooltipTrigger>
            <TooltipContent side="right">Toggle Theme</TooltipContent>
          </Tooltip>

          {/* Help & Support Section */}
          <Tooltip>
            <TooltipTrigger asChild>
              <Button variant="ghost" className="w-full justify-start h-10 px-2" asChild>
                <Link href="/help">
                  <HelpCircle className={cn("h-4 w-4", isCollapsed ? "mx-auto" : "mr-3")} />
                  <span className={cn(isCollapsed && "sr-only")}>Help & Support</span>
                </Link>
              </Button>
            </TooltipTrigger>
            <TooltipContent side="right">Help & Support</TooltipContent>
          </Tooltip>

          {/* Login/Logout Section */}
          {status !== 'loading' && (
            <Tooltip>
              <TooltipTrigger asChild>
                {status === 'authenticated' ? (
                  <Button variant="ghost" className="w-full justify-start h-10 px-2" onClick={() => signOut({ callbackUrl: '/login' })}>
                    <LogOut className={cn("h-4 w-4", isCollapsed ? "mx-auto" : "mr-3")} />
                    <span className={cn(isCollapsed && "sr-only")}>Logout</span>
                  </Button>
                ) : (
                  <Button variant="ghost" className="w-full justify-start h-10 px-2" asChild>
                    <Link href="/login">
                      <LogIn className={cn("h-4 w-4", isCollapsed ? "mx-auto" : "mr-3")} />
                      <span className={cn(isCollapsed && "sr-only")}>Login</span>
                    </Link>
                  </Button>
                )}
              </TooltipTrigger>
              <TooltipContent side="right">
                {status === 'authenticated' ? 'Logout' : 'Login'}
              </TooltipContent>
            </Tooltip>
          )}
        </TooltipProvider>

        <Separator />
        <div className="p-1">
          <Button onClick={() => setIsCollapsed(!isCollapsed)} variant="ghost" className="w-full justify-center h-10">
            {isCollapsed ? (
              <ChevronRight className="h-5 w-5" />
            ) : (
              <ChevronLeft className="h-5 w-5" />
            )}
            <span className="sr-only">Toggle Sidebar</span>
          </Button>
        </div>
      </div>
    </div>
  )
}