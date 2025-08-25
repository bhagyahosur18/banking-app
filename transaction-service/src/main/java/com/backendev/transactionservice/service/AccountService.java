package com.backendev.transactionservice.service;

import com.backendev.transactionservice.client.AccountServiceClient;
import com.backendev.transactionservice.dto.AccountResponse;
import com.backendev.transactionservice.dto.UpdateAccountBalanceRequest;
import com.backendev.transactionservice.exception.InvalidAccountException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class AccountService {

    private static final String ACCOUNT_ACTIVE = "ACTIVE";

    private final AccountServiceClient accountServiceClient;

    public AccountService(AccountServiceClient accountServiceClient) {
        this.accountServiceClient = accountServiceClient;
    }

    public void validateAccountAndOwnership(Long accountNumber, String currentUserId) {
        AccountResponse account = validateAccount(accountNumber);
        if (!currentUserId.equals(account.getUserId())) {
            log.warn("Access denied: User {} attempted to access account {} owned by {}",
                    currentUserId, account.getAccountNumber(), account.getUserId());
            throw new SecurityException("Access denied: Account does not belong to current user");
        }
    }

    public AccountResponse validateAccount(Long accountNumber) {
        try {
            ResponseEntity<AccountResponse> response = accountServiceClient.getAccount(accountNumber);
            AccountResponse account = response.getBody();

            if (account == null || !ACCOUNT_ACTIVE.equals(account.getStatus())) {
                throw new InvalidAccountException("Account not found or inactive");
            }
            return account;
        } catch (FeignException e) {
            throw new InvalidAccountException("Account validation failed", e);
        }
    }

    public void syncBalanceWithAccountService(Long accountNumber, BigDecimal newBalance) {
        try {
            log.info("Updating account {} with balance: {}", accountNumber, newBalance); // Add this

            if (newBalance == null) {
                throw new IllegalArgumentException("Balance cannot be null");
            }

            UpdateAccountBalanceRequest updateRequest = new UpdateAccountBalanceRequest(accountNumber, newBalance);

            ResponseEntity<AccountResponse> response = accountServiceClient.updateAccountBalance(accountNumber, updateRequest);
            AccountResponse account = response.getBody();

            if (account == null || !ACCOUNT_ACTIVE.equals(account.getStatus())) {
                throw new InvalidAccountException("Account not found or inactive");
            }
        } catch (FeignException e) {
            throw new InvalidAccountException("Account validation failed", e);
        }
    }
}
