'use client';

import { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import apiClient from '@/lib/apiClient';
import { useNotifications } from '@/context/NotificationContext';
import { useSession } from 'next-auth/react';

interface BalanceResponse {
  balanceInCents: number;
}

interface BalanceContextType {
  balanceInCents: number | null;
  isLoading: boolean;
  error: string | null;
  fetchBalance: () => Promise<void>;
}

const BalanceContext = createContext<BalanceContextType | undefined>(undefined);

export function BalanceProvider({ children }: { children: ReactNode }) {
  const { status } = useSession(); // Get the authentication status

  // If the user is not authenticated, we provide a default, "empty" context.
  // This prevents crashes because the hooks inside AuthenticatedBalanceLogic are never called.
  if (status !== 'authenticated') {
    const default_value = { balanceInCents: null, isLoading: false, error: null, fetchBalance: async () => {} };
    return (
      <BalanceContext.Provider value={default_value}>
        {children}
      </BalanceContext.Provider>
    );
  }

  return <AuthenticatedBalanceLogic>{children}</AuthenticatedBalanceLogic>;
}

function AuthenticatedBalanceLogic({ children }: { children: ReactNode }) {
  const [balanceInCents, setBalanceInCents] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { latestPaymentStatus } = useNotifications();

  const fetchBalance = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await apiClient.get<BalanceResponse>('/balance');
      setBalanceInCents(response.data.balanceInCents);
    } catch (err) {
      console.error("BalanceContext: Failed to fetch balance:", err);
      setError('Failed to load balance.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchBalance();
  }, [fetchBalance]);
  
  // Refetch balance whenever a relevant payment notification comes in
  useEffect(() => {
    if (latestPaymentStatus && ['COMPLETED', 'REFUNDED', 'DISPUTED'].includes(latestPaymentStatus.status)) {
      console.log('BalanceContext: Payment status changed, refetching balance...');
      fetchBalance();
    }
  }, [latestPaymentStatus, fetchBalance]);

  const value = { balanceInCents, isLoading, error, fetchBalance };

  return (
    <BalanceContext.Provider value={value}>
      {children}
    </BalanceContext.Provider>
  );
}


// Custom hook for easy consumption
export function useBalance() {
  const context = useContext(BalanceContext);
  if (context === undefined) {
    throw new Error('useBalance must be used within a BalanceProvider');
  }
  return context;
}