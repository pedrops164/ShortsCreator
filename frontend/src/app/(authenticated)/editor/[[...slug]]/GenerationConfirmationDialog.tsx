'use client';

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button"; // Import regular Button
import { ApiError } from "@/lib/apiClient";
import { AlertCircle, Wallet } from "lucide-react";

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  priceInCents: number | undefined;
  currency: string | undefined;
  error: ApiError | null;
}

const formatPrice = (cents: number, currency: string) => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency,
  }).format(cents / 100);
};

export function GenerationConfirmationDialog({
  isOpen,
  onClose,
  onConfirm,
  priceInCents,
  currency,
  error
}: Props) {
  const isPriceAvailable = typeof priceInCents === 'number' && currency;
  const formattedPrice = isPriceAvailable ? formatPrice(priceInCents, currency) : '...';

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{error ? 'Generation Failed' : 'Confirm Generation'}</DialogTitle>
          {!error && (
            <DialogDescription>
              Are you sure you want to submit this draft for generation? This action will deduct the cost from your account balance.
            </DialogDescription>
          )}
        </DialogHeader>

        {error ? (
          <Alert variant="destructive" className="my-4">
            <AlertCircle className="h-4 w-4" />
            <AlertTitle>{error.errorCode.replace(/_/g, ' ')}</AlertTitle>
            <AlertDescription>{error.message}</AlertDescription>
          </Alert>
        ) : (
          <div className="my-4 p-4 bg-muted rounded-lg flex items-center justify-center space-x-3">
            <Wallet className="h-6 w-6 text-primary" />
            <div className="text-center">
              <p className="text-sm text-muted-foreground">Exact Cost</p>
              <p className="text-2xl font-bold">{formattedPrice}</p>
            </div>
          </div>
        )}

        <DialogFooter>
          {/* Use regular Buttons which don't auto-close */}
          <Button variant="outline" onClick={onClose}>
            {error ? 'Close' : 'Cancel'}
          </Button>
          <Button onClick={onConfirm} disabled={!isPriceAvailable}>
            {error ? 'Try Again' : 'Confirm & Generate'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}