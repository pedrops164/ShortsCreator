"use client"

import { useState } from "react"
import { useBalance } from "@/context/BalanceContext"
import { formatPriceFromCents } from "@/lib/pricingUtils"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Separator } from "@/components/ui/separator"
import { usePathname } from "next/navigation" 
import { Skeleton } from "@/components/ui/skeleton"
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip" // For collapsed view
import {
  CreditCard,
  HelpCircle,
  ChevronLeft,
  ChevronRight,
  Zap,
  PlusSquare,
  LayoutGrid,
  Wallet,
  LogOut,
  LogIn,
} from "lucide-react"
import { cn } from "@/lib/utils"
import { ThemeToggleButton } from "./ThemeToggleButton"
import { signOut, useSession } from "next-auth/react"
import Link from "next/link"

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
    badge: "12",
  },
  {
    title: "Account & Billing",
    icon: CreditCard,
    href: "/account",
    badge: null,
  },
]

const bottomItems = [
  {
    title: "Help & Support",
    icon: HelpCircle,
    href: "/help",
  },
]

export function Sidebar() {
  const { status } = useSession(); // Get auth status
  const [isCollapsed, setIsCollapsed] = useState(false)
  // 👇 Get the current path
  const pathname = usePathname()

  return (
    <div
      className={cn(
        "relative flex flex-col border-r bg-card transition-all duration-300",
        isCollapsed ? "w-16" : "w-64",
      )}
    >
      {/* Header */}
      <div className="flex h-16 items-center justify-between px-4 border-b">
        {!isCollapsed && (
          <div className="flex items-center space-x-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-yellow-primary">
              <Zap className="h-4 w-4 text-white" />
            </div>
            <span className="text-lg font-semibold text-primary">CreatorApp</span>
          </div>
        )}
        <Button variant="ghost" size="icon" onClick={() => setIsCollapsed(!isCollapsed)} className="h-8 w-8">
          {isCollapsed ? <ChevronRight className="h-4 w-4" /> : <ChevronLeft className="h-4 w-4" />}
        </Button>
      </div>

      {/* Navigation */}
      <div className="flex-1 overflow-auto py-4">
        <nav className="space-y-1 px-2">
          {navigationItems.map((item) => {
            const Icon = item.icon
            const isActive = pathname === item.href
            return (
              <Button
                key={item.title}
                variant={isActive ? "secondary" : "ghost"}
                className={cn(
                  "w-full justify-start h-10",
                  isCollapsed && "px-2",
                  isActive && "bg-yellow-100 text-yellow-900 dark:bg-yellow-900/20 dark:text-yellow-400",
                )}
                asChild
              >
                <a href={item.href}>
                  <Icon className={cn("h-4 w-4", !isCollapsed && "mr-3")} />
                  {!isCollapsed && (
                    <>
                      <span className="flex-1 text-left">{item.title}</span>
                      {item.badge && (
                        <Badge variant="secondary" className="ml-auto">
                          {item.badge}
                        </Badge>
                      )}
                    </>
                  )}
                </a>
              </Button>
            )
          })}
        </nav>
      </div>

      <div className="mt-auto space-y-2 p-2"></div>
        {/* Only render the balance section if the user is authenticated */}
        {status === 'authenticated' && (
          <>
            <Separator />
            <div className="p-1">
              <BalanceDisplay isCollapsed={isCollapsed} />
            </div>
          </>
        )}

        <Separator />

        {/* Theme Toggle */}
        <div className="p-1.5">
          <div className={cn("flex items-center", isCollapsed ? "justify-center" : "justify-between px-2")}>
            {!isCollapsed && <span className="text-sm font-medium text-primary">Theme</span>}
            <ThemeToggleButton />
          </div>
        </div>


        <Separator />

        {/* Bottom Navigation */}
        <nav className="space-y-1">
          {bottomItems.map((item) => {
            const Icon = item.icon
            return (
              <Button
                key={item.title}
                variant="ghost"
                className={cn("w-full justify-start h-10", isCollapsed && "px-2")}
                asChild
              >
                <a href={item.href}>
                  <Icon className={cn("h-4 w-4", !isCollapsed && "mr-3")} />
                  {!isCollapsed && <span>{item.title}</span>}
                </a>
              </Button>
            )
          })}
        </nav>

        {status !== 'loading' && (
          <>
            <Separator />
            <TooltipProvider delayDuration={0}>
              <Tooltip>
                <TooltipTrigger asChild>
                  {status === 'authenticated' ? (
                    // --- LOGOUT BUTTON ---
                    <Button
                      variant="ghost"
                      className={cn("w-full justify-start h-10", isCollapsed && "px-2 justify-center")}
                      onClick={() => signOut({ callbackUrl: '/login' })}
                    >
                      <LogOut className={cn("h-4 w-4", !isCollapsed && "mr-3")} />
                      {!isCollapsed && <span>Logout</span>}
                    </Button>
                  ) : (
                    // --- LOGIN BUTTON ---
                    <Button
                      variant="ghost"
                      className={cn("w-full justify-start h-10", isCollapsed && "px-2 justify-center")}
                      asChild
                    >
                      <Link href="/login">
                        <LogIn className={cn("h-4 w-4", !isCollapsed && "mr-3")} />
                        {!isCollapsed && <span>Login</span>}
                      </Link>
                    </Button>
                  )}
                </TooltipTrigger>
                {isCollapsed && (
                  <TooltipContent side="right">
                    {status === 'authenticated' ? 'Logout' : 'Login'}
                  </TooltipContent>
                )}
              </Tooltip>
            </TooltipProvider>
          </>
        )}
      </div>
  )
}

function BalanceDisplay({ isCollapsed }: { isCollapsed: boolean }) {
  const { balanceInCents, isLoading } = useBalance();

  if (isCollapsed) {
    return (
      <TooltipProvider delayDuration={0}>
        <Tooltip>
          <TooltipTrigger asChild>
            <Button variant="ghost" className="w-full justify-center h-10 px-2" asChild>
              <a href="/account">
                <Wallet className="h-5 w-5" />
              </a>
            </Button>
          </TooltipTrigger>
          <TooltipContent side="right" className="flex items-center gap-2">
            Balance: 
            <span className="font-semibold">
              {isLoading ? "..." : formatPriceFromCents(balanceInCents ?? 0)}
            </span>
          </TooltipContent>
        </Tooltip>
      </TooltipProvider>
    )
  }

  return (
    <div className="flex h-10 items-center justify-between px-2">
      <div className="flex items-baseline justify-items-center space-x-2">
        <span className="font-medium text-primary">Balance:</span>
        {isLoading ? (
          <Skeleton className="h-5 w-16" />
        ) : (
          <span className="font-bold text-primary">
            {formatPriceFromCents(balanceInCents ?? 0)}
          </span>
        )}
      </div>
      <Button variant="ghost" size="icon" className="h-7 w-7" asChild>
        <a href="/account"><PlusSquare className="h-4 w-4" /></a>
      </Button>
    </div>
  )
}