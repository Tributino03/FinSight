package dev.Tributino.FinSight.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin_operation_lock")
public class AdminOperationLock {

    @Id
    private Long id;

    protected AdminOperationLock() {
    }

    public Long getId() {
        return id;
    }
}