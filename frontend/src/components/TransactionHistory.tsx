'use client';

import { useState, useEffect, useCallback } from 'react';
import { Banknote, CheckCircle, XCircle, Clock, AlertTriangle, ChevronLeft, ChevronRight } from 'lucide-react';
import apiClient from '@/lib/apiClient';
import { Page, PaymentTransactionResponse } from '@/types';

const StatusBadge = ({ status }: { status: string }) => {
  const statusStyles = {
    COMPLETED: 'bg-green-100 text-green-800 border-green-200',
    PENDING: 'bg-yellow-100 text-yellow-800 border-yellow-200',
    FAILED: 'bg-red-100 text-red-800 border-red-200',
    REFUNDED: 'bg-blue-100 text-blue-800 border-blue-200',
    DISPUTED: 'bg-orange-100 text-orange-800 border-orange-200',
  };
  const statusIcons = {
    COMPLETED: <CheckCircle className="w-4 h-4" />,
    PENDING: <Clock className="w-4 h-4" />,
    FAILED: <XCircle className="w-4 h-4" />,
    REFUNDED: <Banknote className="w-4 h-4" />,
    DISPUTED: <AlertTriangle className="w-4 h-4" />,
  };
  return (
    <span className={`inline-flex items-center gap-1.5 px-2 py-1 text-xs font-medium border rounded-full ${statusStyles[status] || 'bg-gray-100 text-gray-800'}`}>
      {statusIcons[status]}
      {status.charAt(0).toUpperCase() + status.slice(1).toLowerCase()}
    </span>
  );
};

const TransactionRow = ({ transaction }: { transaction: PaymentTransactionResponse }) => {
  const formattedDate = new Date(transaction.createdAt).toLocaleString('pt-PT', {
    year: 'numeric', month: 'long', day: 'numeric',
  });
  const amount = (transaction.amountPaid / 100).toFixed(2);

  return (
    <div className="flex items-center justify-between py-4 border-b border-accent/20">
      <div className="flex items-center gap-4">
        <div className="p-2 rounded-full bg-accent/10">
          <Banknote className="w-5 h-5 text-accent" />
        </div>
        <div>
          <p className="font-semibold text-foreground">Balance Top-up</p>
          <p className="text-sm text-accent">{formattedDate}</p>
        </div>
      </div>
      <div className="text-right">
        <p className="font-semibold text-green-400">+ ${amount} {transaction.currency}</p>
        <StatusBadge status={transaction.status} />
      </div>
    </div>
  );
};

const PaginationControls = ({ page, onPageChange }: { page: Page<any>, onPageChange: (newPage: number) => void }) => {
    if (page.totalPages <= 1) return null;
    return (
        <div className="flex items-center justify-between pt-4">
            <button
                onClick={() => onPageChange(page.number - 1)}
                disabled={page.first}
                className="flex items-center gap-1 px-3 py-2 text-sm font-medium rounded-md disabled:opacity-50 disabled:cursor-not-allowed bg-accent/20 hover:bg-accent/30"
            >
                <ChevronLeft className="w-4 h-4" /> Previous
            </button>
            <span className="text-sm text-accent">
                Page {page.number + 1} of {page.totalPages}
            </span>
            <button
                onClick={() => onPageChange(page.number + 1)}
                disabled={page.last}
                className="flex items-center gap-1 px-3 py-2 text-sm font-medium rounded-md disabled:opacity-50 disabled:cursor-not-allowed bg-accent/20 hover:bg-accent/30"
            >
                Next <ChevronRight className="w-4 h-4" />
            </button>
        </div>
    );
};

export default function TransactionHistory() {
  const [transactions, setTransactions] = useState<PaymentTransactionResponse[]>([]);
  const [pageData, setPageData] = useState<Page<any> | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchTransactions = useCallback(async (page: number) => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await apiClient.get<Page<PaymentTransactionResponse>>('/transactions', {
        params: { page, size: 5 },
      });
      setTransactions(response.data.content);
      setPageData(response.data);
    } catch (err) {
      console.error("Failed to fetch transactions:", err);
      setError("Could not load transactions.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchTransactions(currentPage);
  }, [currentPage, fetchTransactions]);

  return (
    <div className="p-6 border rounded-lg bg-background/20 border-accent/50">
      <h3 className="text-lg font-semibold text-foreground">Transaction History</h3>
      {isLoading ? (
        <div className="mt-4 space-y-2">
            {[...Array(3)].map((_, i) => (
                <div key={i} className="w-full h-16 rounded-md bg-accent/20 animate-pulse"></div>
            ))}
        </div>
      ) : error ? (
        <p className="mt-4 text-sm text-center text-red-400">{error}</p>
      ) : transactions.length > 0 ? (
        <div className="mt-2">
          {transactions.map(tx => <TransactionRow key={tx.id} transaction={tx} />)}
          {pageData && <PaginationControls page={pageData} onPageChange={setCurrentPage} />}
        </div>
      ) : (
        <p className="mt-4 text-sm text-center text-accent">You have no transactions yet.</p>
      )}
    </div>
  );
}