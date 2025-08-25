package com.backendev.transactionservice.service;

import com.backendev.transactionservice.dto.TransactionRequest;
import com.backendev.transactionservice.dto.TransactionResponse;
import com.backendev.transactionservice.dto.TransferRequest;
import com.backendev.transactionservice.entity.Transaction;
import com.backendev.transactionservice.enums.TransactionType;
import com.backendev.transactionservice.exception.InsufficientFundsException;
import com.backendev.transactionservice.exception.InvalidAccountException;
import com.backendev.transactionservice.exception.TransactionProcessingException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Slf4j
public class TransactionService {

    private final AccountService accountService;
    private final UserInfoService userInfoService;
    private final TransactionProcessor transactionProcessor;
    private final BalanceManager balanceManager;

    public TransactionService(AccountService accountService, UserInfoService userInfoService, TransactionProcessor transactionProcessor, BalanceManager balanceManager) {
        this.accountService = accountService;
        this.userInfoService = userInfoService;
        this.transactionProcessor = transactionProcessor;
        this.balanceManager = balanceManager;
    }

    @Transactional(rollbackFor = Exception.class)
    public TransactionResponse deposit(@Valid TransactionRequest request) {
        return processTransaction(request, TransactionType.DEPOSIT,
                () -> {
                    balanceManager.updateAccountBalance(request.getAccountNumber(), request.getAmount());
            return balanceManager.getBalance(request.getAccountNumber());
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public TransactionResponse withdraw(TransactionRequest request) {
        return processTransaction(request, TransactionType.WITHDRAWAL, () -> {
            balanceManager.validateSufficientFunds(request.getAccountNumber(), request.getAmount());
            balanceManager.updateAccountBalance(request.getAccountNumber(), request.getAmount().negate());
            return balanceManager.getBalance(request.getAccountNumber());
        });
    }

    public TransactionResponse transfer(TransferRequest request) {
        String currentUserId = userInfoService.getCurrentUserId();
        accountService.validateAccountAndOwnership(request.getFromAccountNumber(), currentUserId);

        Transaction transaction = transactionProcessor.createAndSaveTransaction(request, TransactionType.TRANSFER);

        try {
            balanceManager.validateSufficientFunds(request.getFromAccountNumber(), request.getAmount());
            balanceManager.updateAccountBalance(request.getFromAccountNumber(), request.getAmount().negate());
            balanceManager.updateAccountBalance(request.getToAccountNumber(), request.getAmount());

            BigDecimal newBalance = balanceManager.getBalance(request.getFromAccountNumber());
            return transactionProcessor.completeTransaction(transaction, newBalance);

        } catch (InsufficientFundsException | InvalidAccountException | TransactionProcessingException e) {
            return transactionProcessor.failTransaction(transaction, "Transfer failed", e);
        }
    }

    private TransactionResponse processTransaction(TransactionRequest request,
                                                   TransactionType type,
                                                   BalanceOperation operation) {
        String currentUserId = userInfoService.getCurrentUserId();
        accountService.validateAccountAndOwnership(request.getAccountNumber(), currentUserId);

        Transaction transaction = transactionProcessor.createAndSaveTransaction(request, type);

        try {
            BigDecimal newBalance = operation.processBalanceChange();
            return transactionProcessor.completeTransaction(transaction, newBalance);
        } catch (Exception e) {
            String errorMessage = type == TransactionType.DEPOSIT ? "Deposit failed" : "Withdrawal failed";
            return transactionProcessor.failTransaction(transaction, errorMessage, e);
        }
    }

    @FunctionalInterface
    private interface BalanceOperation {
        BigDecimal processBalanceChange() throws InsufficientFundsException, InvalidAccountException, TransactionProcessingException;
    }
}