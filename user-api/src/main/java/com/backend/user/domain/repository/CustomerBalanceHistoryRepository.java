package com.backend.user.domain.repository;

import com.backend.user.domain.model.CustomerBalanceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

public interface CustomerBalanceHistoryRepository extends JpaRepository<CustomerBalanceHistory, Long> {
    Optional<CustomerBalanceHistory> findFirstByCustomer_IdOrderByIdDesc(
            @RequestParam("customer_id") Long customerId
    );
}
