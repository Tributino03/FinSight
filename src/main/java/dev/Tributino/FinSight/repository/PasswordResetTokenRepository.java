package dev.Tributino.FinSight.repository;

import dev.Tributino.FinSight.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE PasswordResetToken t
               SET t.usedAt = :now
             WHERE t.tokenHash = :tokenHash
               AND t.usedAt IS NULL
               AND t.expiresAt > :now
            """)
    int consume(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE PasswordResetToken t
               SET t.usedAt = :now
             WHERE t.user.id = :userId
               AND t.usedAt IS NULL
            """)
    int invalidateActiveByUser(@Param("userId") Long userId, @Param("now") Instant now);
}