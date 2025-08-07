package com.shortscreator.shared.dto;

import java.util.List;

/**
 * A custom, stable DTO for paginated responses.
 * This creates a stable API contract, independent of Spring's internal PageImpl class.
 * @param <T> The type of the content in the page.
 */
public record CustomPage<T>(
    List<T> content,
    int number,         // The current page number (0-indexed)
    int totalPages,
    boolean first,
    boolean last
) {}