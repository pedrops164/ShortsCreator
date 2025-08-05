'use client';

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Loader2, Wallet } from "lucide-react";

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  priceInCents: number | undefined;
  currency: string | undefined;
}

// Helper to format cents into a currency string (e.g., 150 -> $1.50)
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
}: Props) {
  const isPriceAvailable = typeof priceInCents === 'number' && currency;
  const formattedPrice = isPriceAvailable ? formatPrice(priceInCents, currency) : '...';

  return (
    <AlertDialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Confirm Generation</AlertDialogTitle>
          <AlertDialogDescription>
            Are you sure you want to submit this draft for generation? This action will deduct the cost from your account balance.
          </AlertDialogDescription>
        </AlertDialogHeader>
        
        <div className="my-4 p-4 bg-muted rounded-lg flex items-center justify-center space-x-3">
          <Wallet className="h-6 w-6 text-primary" />
          <div className="text-center">
            <p className="text-sm text-muted-foreground">Estimated Cost</p>
            <p className="text-2xl font-bold">
              {formattedPrice}
            </p>
          </div>
        </div>

        <AlertDialogFooter>
          <AlertDialogCancel >Cancel</AlertDialogCancel>
          <AlertDialogAction onClick={onConfirm} disabled={!isPriceAvailable}>
            Confirm & Generate
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}