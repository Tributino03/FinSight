package dev.Tributino.FinSight.repository;

import dev.Tributino.FinSight.domain.Account;
import dev.Tributino.FinSight.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUserId(Long userId);

}
