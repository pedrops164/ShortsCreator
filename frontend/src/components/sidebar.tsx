"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Separator } from "@/components/ui/separator"
import { usePathname } from "next/navigation" 
import {
  CreditCard,
  HelpCircle,
  ChevronLeft,
  ChevronRight,
  Zap,
  PlusSquare,
  LayoutGrid,
} from "lucide-react"
import { cn } from "@/lib/utils"
import { ThemeToggleButton } from "./ThemeToggleButton"

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

      <Separator />

      {/* Theme Toggle */}
      <div className="p-2">
        <div className={cn("flex items-center", isCollapsed ? "justify-center" : "justify-between px-2")}>
          {!isCollapsed && <span className="text-sm font-medium text-primary">Theme</span>}
          <ThemeToggleButton />
        </div>
      </div>

      <Separator />

      {/* Bottom Navigation */}
      <div className="p-2">
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
      </div>
    </div>
  )
}
