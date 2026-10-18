package dev.Tributino.FinSight.repository;

import dev.Tributino.FinSight.domain.AdminOperationLock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdminOperationLockRepository
        extends JpaRepository<AdminOperationLock, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT lockRow
            FROM AdminOperationLock lockRow
            WHERE lockRow.id = :id
            """)
    Optional<AdminOperationLock> findByIdForUpdate(
            @Param("id") Long id
    );
}