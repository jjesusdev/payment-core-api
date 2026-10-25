package com.jibanez.paymentcore.idempotency;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
    Optional<IdempotencyKey> findByUserIdAndKey(Long userId, String key);
}
