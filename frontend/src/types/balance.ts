

export interface BalanceResponse {
    balanceInCents: number; // Balance in cents
    currency: string; // e.g., "USD"
}

export interface CreateCheckoutResponse {
    redirectUrl: string; // URL to redirect for payment
}

/**
 * Represents the possible statuses of a payment transaction.
 * This matches the TransactionStatus enum on the backend.
 */
export type TransactionStatus = 
  | 'COMPLETED'
  | 'PENDING'
  | 'FAILED'
  | 'REFUNDED'
  | 'DISPUTED';

export type TransactionType =
  | 'DEPOSIT'
  | 'CHARGE';

/**
 * Represents a unified view of a transaction,
 * combining deposits and charges.
 * This is used in the TransactionHistory component.
 */
export interface UnifiedTransaction {
  id: string;
  type: TransactionType;
  description: string;
  amount: number;
  currency: string;
  status: TransactionStatus;
  createdAt: string;
}


/*
  * Represents the structure of a payment status update event.
  * This matches the PaymentStatusUpdate DTO on the backend.
  */
export interface PaymentStatusUpdate {
  userId: string;
  transactionId: string;
  amount: number; // in cents
  status: TransactionStatus;
}

/*
  * Represents the price to generate a draft.
  */
export interface PriceResponse {
  finalPrice: number; // Price in cents
  currency: string;
}