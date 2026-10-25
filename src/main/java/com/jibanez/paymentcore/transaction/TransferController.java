package com.jibanez.paymentcore.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jibanez.paymentcore.auth.SecurityUtils;
import com.jibanez.paymentcore.idempotency.IdempotencyService;
import com.jibanez.paymentcore.transaction.dto.TransactionResponse;
import com.jibanez.paymentcore.transaction.dto.TransferRequest;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransactionService transactionService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public TransferController(TransactionService transactionService, IdempotencyService idempotencyService, ObjectMapper objectMapper) {
        this.transactionService = transactionService;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String username = SecurityUtils.currentUsername();
        Optional<IdempotencyService.CachedResponse> cached = idempotencyService.getCached(idempotencyKey, username, request);
        if (cached.isPresent()) {
            try {
                TransactionResponse body = objectMapper.readValue(cached.get().body(), TransactionResponse.class);
                return ResponseEntity.status(cached.get().statusCode())
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(body);
            } catch (Exception ex) {
                throw new IllegalStateException("No se pudo deserializar la respuesta idempotente", ex);
            }
        }
        TransactionResponse response = transactionService.transfer(request, username);
        idempotencyService.save(idempotencyKey, username, request, response, HttpStatus.OK.value());
        return ResponseEntity.ok(response);
    }
}