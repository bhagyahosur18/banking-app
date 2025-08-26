package com.backendev.transactionservice.repository;

import com.backendev.transactionservice.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    boolean existsByFromAccountNumber(Long accountNumber);
    List<Transaction> findAllByFromAccountNumberOrderByCreatedAtDesc(Long accountNumber);
}
