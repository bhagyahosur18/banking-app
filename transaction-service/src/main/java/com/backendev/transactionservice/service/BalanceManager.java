package com.backendev.transactionservice.service;

import com.backendev.transactionservice.entity.AccountBalance;
import com.backendev.transactionservice.exception.InsufficientFundsException;
import com.backendev.transactionservice.mapper.AccountBalanceMapper;
import com.backendev.transactionservice.repository.AccountBalanceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
@Slf4j
public class BalanceManager {

    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountBalanceMapper accountBalanceMapper;

    public BalanceManager(AccountBalanceRepository accountBalanceRepository, AccountBalanceMapper accountBalanceMapper) {
        this.accountBalanceRepository = accountBalanceRepository;
        this.accountBalanceMapper = accountBalanceMapper;
    }

    public BigDecimal getBalance(Long accountNumber) {
        return accountBalanceRepository.findById(accountNumber)
                .map(AccountBalance::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    public void updateAccountBalance(Long accountNumber, BigDecimal amount) {
        AccountBalance accountBalance = accountBalanceRepository.findById(accountNumber)
                .orElse(accountBalanceMapper.createAccountBalance(accountNumber, BigDecimal.ZERO));

        accountBalance.setBalance(accountBalance.getBalance().add(amount));
        accountBalance.setLastUpdated(Instant.now());

        accountBalanceRepository.save(accountBalance);
    }

    public void validateSufficientFunds(Long accountNumber, BigDecimal amount) {
        BigDecimal currentBalance = getBalance(accountNumber);
        if (currentBalance.compareTo(amount) < 0) {
            log.warn("Insufficient funds for account {}: required={}, available={}",
                    accountNumber, amount, currentBalance);
            throw new InsufficientFundsException("Insufficient balance. Required: " + amount + ", Available: " + currentBalance);
        }
    }
}
