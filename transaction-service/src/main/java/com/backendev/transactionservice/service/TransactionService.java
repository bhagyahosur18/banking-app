package com.backendev.transactionservice.service;

import com.backendev.transactionservice.dto.AccountBalanceInfo;
import com.backendev.transactionservice.dto.TransactionInfo;
import com.backendev.transactionservice.dto.TransactionRequest;
import com.backendev.transactionservice.dto.TransactionResponse;
import com.backendev.transactionservice.dto.TransferRequest;
import com.backendev.transactionservice.entity.AccountBalance;
import com.backendev.transactionservice.entity.Transaction;
import com.backendev.transactionservice.enums.TransactionType;
import com.backendev.transactionservice.exception.AccountNumberNotFoundException;
import com.backendev.transactionservice.exception.InsufficientFundsException;
import com.backendev.transactionservice.exception.InvalidAccountException;
import com.backendev.transactionservice.exception.TransactionProcessingException;
import com.backendev.transactionservice.mapper.AccountBalanceMapper;
import com.backendev.transactionservice.mapper.TransactionMapper;
import com.backendev.transactionservice.repository.AccountBalanceRepository;
import com.backendev.transactionservice.repository.TransactionRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class TransactionService {

    private final AccountService accountService;
    private final UserInfoService userInfoService;
    private final TransactionProcessor transactionProcessor;
    private final BalanceManager balanceManager;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountBalanceMapper accountBalanceMapper;

    public TransactionService(AccountService accountService, UserInfoService userInfoService, TransactionProcessor transactionProcessor, BalanceManager balanceManager, TransactionRepository transactionRepository, TransactionMapper transactionMapper, AccountBalanceRepository accountBalanceRepository, AccountBalanceMapper accountBalanceMapper) {
        this.accountService = accountService;
        this.userInfoService = userInfoService;
        this.transactionProcessor = transactionProcessor;
        this.balanceManager = balanceManager;
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.accountBalanceRepository = accountBalanceRepository;
        this.accountBalanceMapper = accountBalanceMapper;
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

    public List<TransactionInfo> fetchTransactionsByAccount(Long accountNumber) {
        if(!transactionRepository.existsByFromAccountNumber(accountNumber)){
            throw new AccountNumberNotFoundException("Account number does not exists: "+ accountNumber);
        }        List<Transaction>  transactions = transactionRepository.findAllByFromAccountNumberOrderByCreatedAtDesc(accountNumber);
        log.info("Found {} transactions for account: {}", transactions.size(), accountNumber);
        return transactionMapper.toTransactionInfoList(transactions);
    }

    public AccountBalanceInfo fetchAccountBalance(Long accountNumber) {
        if(!accountBalanceRepository.existsByAccountNumber(accountNumber)){
            throw new AccountNumberNotFoundException("Account number does not exists: "+ accountNumber);
        }
        AccountBalance accountBalance = accountBalanceRepository.findByAccountNumber(accountNumber);
        return accountBalanceMapper.toAccountBalanceInfo(accountBalance);

    }

    @FunctionalInterface
    private interface BalanceOperation {
        BigDecimal processBalanceChange() throws InsufficientFundsException, InvalidAccountException, TransactionProcessingException;
    }
}