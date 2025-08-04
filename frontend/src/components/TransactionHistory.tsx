'use client';

import { useState, useEffect, useCallback } from 'react';
import apiClient from '@/lib/apiClient';
import { Page, PaymentTransactionResponse } from '@/types';
import { useNotifications } from '@/context/NotificationContext';

// UI Components from shadcn/ui and lucide-react
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { CreditCard, Plus, ChevronLeft, ChevronRight, AlertCircle } from 'lucide-react';

// --- Helper Functions for Styling ---

/**
 * Returns a variant string for the Badge component based on transaction status.
 */
const getStatusBadgeVariant = (status: string): "default" | "secondary" | "destructive" | "outline" => {
  switch (status) {
    case 'COMPLETED':
      return 'default'; // Green in default themes
    case 'PENDING':
      return 'secondary'; // Yellowish/Gray
    case 'FAILED':
      return 'destructive'; // Red
    case 'REFUNDED':
    case 'DISPUTED':
      return 'outline'; // Blue/Orange border
    default:
      return 'secondary';
  }
};

/**
 * Formats an ISO date string to a locale-specific, readable format.
 */
const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleString('pt-PT', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

// --- Sub-components ---

const PaginationControls = ({ page, onPageChange }: { page: Page<any>, onPageChange: (newPage: number) => void }) => {
  if (page.totalPages <= 1) return null;

  return (
    <div className="flex items-center justify-between pt-4 mt-4 border-t">
      <Button
        variant="outline"
        size="sm"
        onClick={() => onPageChange(page.number - 1)}
        disabled={page.first}
      >
        <ChevronLeft className="w-4 h-4 mr-1" />
        Previous
      </Button>
      <span className="text-sm text-muted-foreground">
        Page {page.number + 1} of {page.totalPages}
      </span>
      <Button
        variant="outline"
        size="sm"
        onClick={() => onPageChange(page.number + 1)}
        disabled={page.last}
      >
        Next
        <ChevronRight className="w-4 h-4 ml-1" />
      </Button>
    </div>
  );
};

// --- Main Component ---

export default function TransactionHistory() {
  const [transactions, setTransactions] = useState<PaymentTransactionResponse[]>([]);
  const [pageData, setPageData] = useState<Page<PaymentTransactionResponse> | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { latestPaymentStatus } = useNotifications();

  const fetchTransactions = useCallback(async (page: number) => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await apiClient.get<Page<PaymentTransactionResponse>>('/transactions', {
        params: { page, size: 5 }, // Fetches 5 items per page
      });
      setTransactions(response.data.content);
      setPageData(response.data);
    } catch (err) {
      console.error("Failed to fetch transactions:", err);
      setError("Could not load transactions. Please try again.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchTransactions(currentPage);
  }, [currentPage, fetchTransactions]);

  // Re-fetch transactions on relevant notifications
  useEffect(() => {
    if (latestPaymentStatus) {
      console.log('Payment status changed, refetching transactions.');
      fetchTransactions(currentPage);
    }
  }, [latestPaymentStatus, currentPage, fetchTransactions]);

  const renderContent = () => {
    if (isLoading) {
      return (
        <div className="space-y-4 pt-2">
          {[...Array(5)].map((_, i) => (
            <Skeleton key={i} className="h-10 w-full" />
          ))}
        </div>
      );
    }

    if (error) {
      return (
        <div className="text-center space-y-4 py-8">
          <Alert variant="destructive">
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>{error}</AlertDescription>
          </Alert>
          <Button onClick={() => fetchTransactions(currentPage)} variant="outline" size="sm">
            <CreditCard className="h-4 w-4 mr-2" />
            Retry
          </Button>
        </div>
      );
    }

    if (transactions.length === 0) {
      return (
        <div className="text-center py-12 space-y-4">
          <CreditCard className="h-12 w-12 mx-auto text-muted-foreground" />
          <div className="space-y-2">
            <h3 className="text-lg font-medium text-foreground">No transactions yet</h3>
            <p className="text-sm text-muted-foreground">
              Your transaction history will appear here once you top up your balance.
            </p>
          </div>
        </div>
      );
    }

    return (
      <>
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Date</TableHead>
                <TableHead>Type</TableHead>
                <TableHead>Amount</TableHead>
                <TableHead>Status</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {transactions.map((tx) => (
                <TableRow key={tx.id}>
                  <TableCell className="font-medium">{formatDate(tx.createdAt)}</TableCell>
                  <TableCell>
                    <div className="flex items-center space-x-2">
                      <Plus className="h-4 w-4 text-muted-foreground" />
                      <span>Balance Top-up</span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <span className="font-medium text-green-500">
                      +${(tx.amountPaid / 100).toFixed(2)} {tx.currency.toUpperCase()}
                    </span>
                  </TableCell>
                  <TableCell>
                    <Badge variant={getStatusBadgeVariant(tx.status)}>
                      {tx.status.charAt(0).toUpperCase() + tx.status.slice(1).toLowerCase()}
                    </Badge>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
        {pageData && <PaginationControls page={pageData} onPageChange={setCurrentPage} />}
      </>
    );
  };
  
  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center text-primary">
          <CreditCard className="h-5 w-5 mr-2" />
          Transaction History
        </CardTitle>
        <CardDescription>
          View all your account transactions and their current status.
        </CardDescription>
      </CardHeader>
      <CardContent>
        {renderContent()}
      </CardContent>
    </Card>
  );
}