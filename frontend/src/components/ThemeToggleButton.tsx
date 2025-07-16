'use client';

import * as React from 'react';
import { Moon, Sun } from 'lucide-react';
import { useTheme } from 'next-themes';

export function ThemeToggleButton() {
  const { theme, setTheme } = useTheme();

  return (
    <button
      className="relative inline-flex items-center justify-center rounded-md p-2 hover:bg-zinc-200 dark:hover:bg-zinc-800 transition-colors"
      onClick={() => {
        console.log('Toggling theme', theme);
        setTheme(theme === 'light' ? 'dark' : 'light')}
      }
    >
      <Sun className="h-5 w-5 rotate-0 scale-100 transition-all dark:-rotate-90 dark:scale-0" />
      <Moon className="absolute h-5 w-5 rotate-90 scale-0 transition-all dark:rotate-0 dark:scale-100" />
      <span className="sr-only">Toggle theme</span>
    </button>
  );
}