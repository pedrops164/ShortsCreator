
/**
 * A generic interface that matches the structure of a Page object
 * returned by a Spring Data JPA repository.
 */
export interface Page<T> {
  content: T[];
  totalPages: number;
  number: number; // The current page number (0-indexed)
  first: boolean;
  last: boolean;
}