'use client';

import { useState, useEffect } from 'react';
import apiClient from '@/lib/apiClient'; // Assumes apiClient is configured
import { useNotifications } from '@/context/NotificationContext'; // Assumes context is set up
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Plus, RefreshCw, AlertCircle, TrendingUp } from "lucide-react";
import TransactionHistory from '@/components/TransactionHistory';

// --- API & Data Type Definitions ---

interface BalanceResponse {
  balanceInCents: number;
}

interface CreateCheckoutResponse {
  redirectUrl: string;
}

// Top-up packages with IDs that can be sent to the backend
const topUpPackages = [
  {
    id: "topup_01_usd", // Example: Stripe Price ID
    name: "Starter Pack",
    amount: 1.0,
    description: "Perfect for trying out our services.",
    badge: null,
  },
  {
    id: "topup_05_usd", // Example: Stripe Price ID
    name: "Creator Pack",
    amount: 5.0,
    description: "Most popular choice for regular users.",
    badge: null,
  },
  {
    id: "topup_10_usd",
    name: "Pro Pack",
    amount: 10.0,
    description: "...",
    badge: "Most Popular",
  }
];

export default function BillingDashboard() {
  // --- State Management ---
  const [balance, setBalance] = useState<number | null>(null);
  const [isLoadingBalance, setIsLoadingBalance] = useState(true);
  const [balanceError, setBalanceError] = useState<string | null>(null);
  const [isRedirecting, setIsRedirecting] = useState<string | null>(null);
  const { latestPaymentStatus } = useNotifications();

  // --- Data Fetching & API Calls ---

  const fetchBalance = async () => {
    setIsLoadingBalance(true);
    setBalanceError(null);
    try {
      const response = await apiClient.get<BalanceResponse>('/balance');
      setBalance(response.data.balanceInCents / 100);
    } catch (err) {
      console.error("Failed to fetch balance:", err);
      setBalanceError('Failed to load balance. Please try again.');
    } finally {
      setIsLoadingBalance(false);
    }
  };
  
  const handleTopUp = async (packageId: string) => {
    setIsRedirecting(packageId);
    try {
      const response = await apiClient.post<CreateCheckoutResponse>('/payments/create-checkout-session', { packageId });
      if (response.data.redirectUrl) {
        window.location.href = response.data.redirectUrl;
      } else {
        console.error("No redirect URL received.");
        // TODO: Optionally show a toast notification for this error
        setIsRedirecting(null);
      }
    } catch (err) {
      console.error(`Failed to create checkout session for ${packageId}:`, err);
      // TODO: Optionally show a toast notification for the error
      setIsRedirecting(null);
    }
  };

  // --- Effects ---

  useEffect(() => {
    fetchBalance();
  }, []);

  useEffect(() => {
    if (latestPaymentStatus && ['COMPLETED', 'REFUNDED', 'DISPUTED'].includes(latestPaymentStatus.status)) {
      console.log('Payment status changed, refetching data...');
      fetchBalance();
    }
  }, [latestPaymentStatus]);
  
  // --- Render ---

  return (
    <div className="p-4 md:p-6 lg:p-8 bg-gradient-main min-h-full">
      <div className="max-w-6xl mx-auto space-y-8">
        <header className="text-center space-y-2">
          <h1 className="text-3xl md:text-4xl font-bold text-primary">Account & Billing</h1>
          <p className="text-secondary">Manage your account balance and view transaction history.</p>
        </header>

        <Card className="border-2 border-balance bg-gradient-balance">
          <CardHeader className="text-center pb-2">
            <CardTitle className="text-lg text-secondary">Available Balance</CardTitle>
          </CardHeader>
          <CardContent className="text-center">
            {isLoadingBalance ? (
              <Skeleton className="h-16 w-32 mx-auto" />
            ) : balanceError ? (
              <div className="space-y-4">
                <Alert className="border-red-200 bg-red-50 dark:border-red-800 dark:bg-red-900/20">
                  <AlertCircle className="h-4 w-4 text-red-600" />
                  <AlertDescription className="text-red-700 dark:text-red-400">
                    {balanceError}
                  </AlertDescription>
                </Alert>
                <Button onClick={fetchBalance} variant="outline" size="sm">
                  <RefreshCw className="h-4 w-4 mr-2" />
                  Retry
                </Button>
              </div>
            ) : (
              <div className="space-y-2">
                <div className="text-5xl md:text-6xl font-bold text-yellow-primary">${balance?.toFixed(2) ?? '0.00'}</div>
                <div className="flex items-center justify-center text-sm text-secondary">
                  <TrendingUp className="h-4 w-4 mr-1" />
                  Ready to use for content creation
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        <div className="space-y-6">
          <div className="text-center">
            <h2 className="text-2xl font-bold text-primary mb-2">Add Funds</h2>
            <p className="text-secondary">Choose a package to top up your account.</p>
          </div>
          <div className="grid md:grid-cols-3 gap-6">
            {topUpPackages.map((pkg) => (
              <Card key={pkg.id} className="relative border-2 hover:border-yellow transition-colors">
                {pkg.badge && (
                  <div className="absolute -top-3 left-1/2 transform -translate-x-1/2">
                    <Badge className="bg-red-500 hover:bg-red-600 text-white">{pkg.badge}</Badge>
                  </div>
                )}
                <CardHeader className="text-center">
                  <CardTitle className="text-xl text-primary">{pkg.name}</CardTitle>
                  <CardDescription className="text-muted">{pkg.description}</CardDescription>
                </CardHeader>
                <CardContent className="text-center space-y-4">
                  <div className="text-3xl font-bold text-yellow-primary">${pkg.amount.toFixed(2)}</div>
                  <Button
                    onClick={() => handleTopUp(pkg.id)}
                    disabled={!!isRedirecting}
                    className="w-full bg-yellow-primary hover:bg-yellow-secondary text-white"
                  >
                    {isRedirecting === pkg.id ? (
                      <>
                        <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                        Processing...
                      </>
                    ) : (
                      <>
                        <Plus className="h-4 w-4 mr-2" />
                        Top up ${pkg.amount.toFixed(2)}
                      </>
                    )}
                  </Button>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>

        <TransactionHistory />
      </div>
    </div>
  );
}