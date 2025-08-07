package com.shortscreator.shared.dto;

import com.shortscreator.shared.enums.ContentType;

/**
 * DTO for requesting a debit from the Payment Service.
 * @param userId The ID of the user to charge.
 * @param priceDetails The calculated price and currency.
 * @param contentType The type of content being generated, for transaction logging.
 */
public record DebitRequestV1(
    String userId,
    String contentId,
    ContentPriceV1 priceDetails,
    ContentType contentType
) {}
