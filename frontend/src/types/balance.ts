

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

/**
 * Defines the structure of a single payment transaction record
 * as returned by the backend API.
 */
export interface PaymentTransactionResponse {
  id: string; // UUID
  amountPaid: number; // in cents
  currency: string;
  status: TransactionStatus;
  createdAt: string; // ISO 8601 date string
  paymentIntentId: string;
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