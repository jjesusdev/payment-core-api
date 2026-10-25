package com.jibanez.paymentcore.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jibanez.paymentcore.exception.BusinessExceptions;
import com.jibanez.paymentcore.user.ApplicationUser;
import com.jibanez.paymentcore.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyKeyRepository idempotencyKeyRepository, UserRepository userRepository, ObjectMapper objectMapper) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public record CachedResponse(int statusCode, String body) {
    }

    @Transactional(readOnly = true)
    public Optional<CachedResponse> getCached(String idempotencyKey, String username, Object request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        ApplicationUser user = userRepository.findByUsername(username).orElseThrow();
        String hash = hash(request);
        Optional<IdempotencyKey> existing = idempotencyKeyRepository.findByUserIdAndKey(user.getId(), idempotencyKey);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        IdempotencyKey saved = existing.get();
        if (!saved.getRequestHash().equals(hash)) {
            throw new BusinessExceptions.IdempotencyConflictException();
        }
        return Optional.of(new CachedResponse(saved.getStatusCode(), saved.getResponseBody()));
    }

    @Transactional
    public void save(String idempotencyKey, String username, Object request, Object response, int statusCode) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        ApplicationUser user = userRepository.findByUsername(username).orElseThrow();
        String hash = hash(request);
        String body;
        try {
            body = objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar la respuesta idempotente", e);
        }
        try {
            idempotencyKeyRepository.save(new IdempotencyKey(user.getId(), idempotencyKey, hash, body, statusCode));
        } catch (DataIntegrityViolationException ex) {
        }
    }

    private String hash(Object request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular hash", e);
        }
    }
}
