'use client';

import { useState, useEffect } from 'react';
import { DollarSign, Zap, Loader2, AlertCircle, Banknote } from 'lucide-react';
import apiClient from '@/lib/apiClient';
import { BalanceResponse, CreateCheckoutResponse } from '@/types';
import TransactionHistory from '@/components/TransactionHistory';
import { useNotifications } from '@/context/NotificationContext';

// --- Helper Components (can be in the same file or imported) ---

const StatCard = ({ title, value, icon: Icon, isLoading, error }) => (
  <div className="p-6 border rounded-lg bg-background/20 border-accent/50">
    <div className="flex items-center justify-between">
      <p className="text-sm font-medium text-accent">{title}</p>
      <Icon className="w-5 h-5 text-accent" />
    </div>
    <div className="mt-2">
      {isLoading ? (
        <div className="w-24 h-8 rounded-md bg-accent/20 animate-pulse"></div>
      ) : error ? (
        <p className="text-lg font-semibold text-red-400">{error}</p>
      ) : (
        <p className="text-3xl font-bold text-foreground">{value}</p>
      )}
    </div>
  </div>
);

const TopUpOptionCard = ({ amount, packageId, onTopUp, isRedirecting }) => {
  const isLoading = isRedirecting === packageId;
  return (
      <button
        onClick={() => onTopUp(packageId)}
        disabled={!!isRedirecting}
        className="flex items-center justify-center w-full px-4 py-2 font-semibold text-white transition-colors rounded-lg bg-primary hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {isLoading ? <Loader2 className="w-5 h-5 animate-spin" /> : `Top up $${amount}`}
      </button>
  );
};


// --- Main Account Page Component ---

export default function AccountPage() {
  const [balance, setBalance] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isRedirecting, setIsRedirecting] = useState<string | null>(null);
  const { latestPaymentStatus } = useNotifications();

  const fetchBalance = async () => {
    try {
      // Assume api.get returns a response like { data: { balance: 12.34 } }
      const response = await apiClient.get<BalanceResponse>('/balance');
      setBalance(response.data.balanceInCents / 100); // Convert cents to dollars
    } catch (err) {
      console.error("Failed to fetch balance:", err);
      setError('Could not load');
    } finally {
      setIsLoading(false);
    }
  };

  // Fetch balance on initial load
  useEffect(() => {
    fetchBalance();
  }, []);

  // Re-fetch balance when a payment is completed or refunded
  useEffect(() => {
    if (latestPaymentStatus && (latestPaymentStatus.status === 'COMPLETED' || latestPaymentStatus.status === 'REFUNDED' || latestPaymentStatus.status === 'DISPUTED')) {
      console.log('Payment status changed, refetching balance...');
      fetchBalance();
    }
  }, [latestPaymentStatus]);

  const handleTopUp = async (packageId: string) => {
    setIsRedirecting(packageId);
    try {
      // Assume api.post returns { data: { url: 'https://checkout.stripe.com/...' } }
      const response = await apiClient.post<CreateCheckoutResponse>('/payments/create-checkout-session', { packageId });
      console.log("Checkout session created:", response.data);
      if (response.data.redirectUrl) {
        window.location.href = response.data.redirectUrl;
      }
    } catch (err) {
      console.error(`Failed to create checkout session for ${packageId}:`, err);
      // Optionally show a toast notification for the error
      setIsRedirecting(null);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-8">
      <div>
        <h2 className="text-2xl font-bold text-primary">Account & Billing</h2>
        <p className="mt-1 text-accent">Manage your balance and top-up your credits.</p>
      </div>

      {/* --- Balance Display --- */}
      <div className="grid grid-cols-1">
        <StatCard 
          title="Current Balance"
          value={balance !== null ? `$${balance.toFixed(2)}` : ''}
          icon={DollarSign}
          isLoading={isLoading}
          error={error}
        />
      </div>

      {/* --- Top-up Section --- */}
      <div className="p-6 border rounded-lg bg-background/20 border-accent/50">
        <h3 className="text-lg font-semibold text-foreground">Top Up Your Balance</h3>
        <p className="mt-1 text-sm text-accent">Choose a package to add credits to your account.</p>
        <div className="grid grid-cols-1 gap-6 mt-6 md:grid-cols-2">
          <TopUpOptionCard 
            amount={5}
            packageId="topup_05_usd"
            onTopUp={handleTopUp}
            isRedirecting={isRedirecting}
          />
          <TopUpOptionCard 
            amount={10}
            packageId="topup_10_usd"
            onTopUp={handleTopUp}
            isRedirecting={isRedirecting}
          />
        </div>
      </div>
    
      {/* --- Render the self-contained TransactionHistory component --- */}
      <TransactionHistory />
    </div>
  );
}