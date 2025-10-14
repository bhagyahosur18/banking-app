package com.backendev.transactionservice.service;

import com.backendev.transactionservice.dto.AccountBalanceInfo;
import com.backendev.transactionservice.dto.TransactionInfo;
import com.backendev.transactionservice.dto.TransactionRequest;
import com.backendev.transactionservice.dto.TransactionResponse;
import com.backendev.transactionservice.dto.TransferRequest;
import com.backendev.transactionservice.entity.Transaction;
import com.backendev.transactionservice.enums.TransactionType;
import com.backendev.transactionservice.exception.InvalidAccountException;
import com.backendev.transactionservice.mapper.AccountBalanceMapper;
import com.backendev.transactionservice.mapper.TransactionMapper;
import com.backendev.transactionservice.repository.AccountBalanceRepository;
import com.backendev.transactionservice.repository.TransactionRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class TransactionService {

    private final AccountService accountService;
    private final UserInfoService userInfoService;
    private final BalanceManager balanceManager;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountBalanceMapper accountBalanceMapper;
    private final TransactionHandler transactionHandler;

    public TransactionService(AccountService accountService, UserInfoService userInfoService, BalanceManager balanceManager, TransactionRepository transactionRepository, TransactionMapper transactionMapper, AccountBalanceRepository accountBalanceRepository, AccountBalanceMapper accountBalanceMapper, TransactionHandler transactionHandler) {
        this.accountService = accountService;
        this.userInfoService = userInfoService;
        this.balanceManager = balanceManager;
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.accountBalanceRepository = accountBalanceRepository;
        this.accountBalanceMapper = accountBalanceMapper;
        this.transactionHandler = transactionHandler;
    }

    public TransactionResponse deposit(@Valid TransactionRequest request) {
        return transactionHandler.processTransaction(request, TransactionType.DEPOSIT,
                () -> {
                    balanceManager.updateAccountBalance(request.getAccountNumber(), request.getAmount());
                    return balanceManager.getBalance(request.getAccountNumber());
                });
    }

    public TransactionResponse withdraw(TransactionRequest request) {
        return transactionHandler.processTransaction(request, TransactionType.WITHDRAWAL, () -> {
            balanceManager.validateSufficientFunds(request.getAccountNumber(), request.getAmount());
            balanceManager.updateAccountBalance(request.getAccountNumber(), request.getAmount().negate());
            return balanceManager.getBalance(request.getAccountNumber());
        });
    }

    public TransactionResponse transfer(TransferRequest request) {
        String currentUserId = userInfoService.getCurrentUserId();
        accountService.validateTransferAccounts(request.getFromAccountNumber(), request.getToAccountNumber(), currentUserId);
        return transactionHandler.processTransferTransaction(request);
    }

    @Transactional(readOnly = true)
    public List<TransactionInfo> fetchTransactionsByAccount(Long accountNumber) {
        log.debug("Fetching transactions for account: {}", accountNumber);

        List<Transaction> transactions = transactionRepository.findAllByFromAccountNumberOrderByCreatedAtDesc(accountNumber);

        if (transactions.isEmpty()) {
            throw new InvalidAccountException("Account number does not exists: " + accountNumber);
        }
        log.info("Found {} transactions for account: {}", transactions.size(), accountNumber);
        return transactionMapper.toTransactionInfoList(transactions);
    }

    @Transactional(readOnly = true)
    public AccountBalanceInfo fetchAccountBalance(Long accountNumber) {
        log.debug("Fetching account balance for account: {}", accountNumber);

        return accountBalanceRepository.findByAccountNumber(accountNumber)
                .map(accountBalanceMapper::toAccountBalanceInfo)
                .orElseThrow(() -> new InvalidAccountException(
                        "Account number does not exist: " + accountNumber));
    }
}