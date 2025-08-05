/**
 * Contains frontend utilities for calculating content pricing.
 * This logic should mirror the backend implementation.
 */

const CENTS_PER_THOUSAND_CHARS = 7.0;

/**
 * Calculates the approximate generation price based on character count.
 * @param characterCount The total number of characters to be synthesized.
 * @returns The estimated price in cents, rounded up.
 */
export function calculateApproximatePrice(characterCount: number): number {
  if (characterCount === 0) {
    return 0;
  }

  // Perform the calculation using floating point numbers
  const price = (characterCount / 1000.0) * CENTS_PER_THOUSAND_CHARS;

  // Round up to the nearest whole cent and return as an integer
  return Math.ceil(price);
}

/**
 * Formats a price in cents into a user-friendly currency string.
 * @param cents The price in cents.
 * @param currency The currency code (e.g., 'USD').
 * @returns A formatted string (e.g., '$1.50').
 */
export function formatPriceFromCents(cents: number, currency: string = 'USD'): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
    }).format(cents / 100);
}