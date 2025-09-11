package com.content_storage_service.client;

import com.content_storage_service.config.AppProperties;
import com.content_storage_service.exception.InsufficientFundsClientException;
import com.content_storage_service.exception.PaymentServiceInternalErrorException;
import com.shortscreator.shared.dto.DebitRequestV1;
import com.shortscreator.shared.dto.ErrorResponse;
import com.shortscreator.shared.dto.RefundRequestV1;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class PaymentServiceClient {

    private final WebClient webClient;

    public PaymentServiceClient(WebClient.Builder webClientBuilder, AppProperties appProperties) {
        this.webClient = webClientBuilder.baseUrl(appProperties.getServices().getPaymentService().getUrl()).build();
    }

    public Mono<Void> debitBalance(DebitRequestV1 debitRequest) {
        return webClient.post()
                .uri("/api/v1/balance/debit")
                .bodyValue(debitRequest)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    // Translate specific, known client errors (4xx)
                    if (response.statusCode() == HttpStatus.CONFLICT) { // 409
                        return response.bodyToMono(ErrorResponse.class)
                                .flatMap(parsedError -> {
                                    // Use the message from the DTO to create the exception
                                    String cleanMessage = parsedError.message();
                                    return Mono.error(new InsufficientFundsClientException(cleanMessage));
                                });
                    }
                    
                    // Treat all other errors (e.g., 5xx) as service internal error
                    return response.bodyToMono(ErrorResponse.class)
                            .flatMap(parsedError -> {
                                String errorMessage = "Payment Service is unavailable: " + parsedError.message();
                                return Mono.error(new PaymentServiceInternalErrorException(errorMessage));
                            });
                })
                .bodyToMono(Void.class);
    }
    
    public Mono<Void> requestRefund(String contentId) {
        RefundRequestV1 refundRequest = new RefundRequestV1(contentId);
        return webClient.post()
                .uri("/api/v1/balance/refund")
                .bodyValue(refundRequest)
                .retrieve()
                //.onStatus(HttpStatusCode::isError, response -> /* ... handle errors ... */)
                .bodyToMono(Void.class);
    }
}