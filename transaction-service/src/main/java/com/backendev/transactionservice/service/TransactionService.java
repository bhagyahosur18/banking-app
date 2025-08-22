package com.backendev.transactionservice.service;

import com.backendev.transactionservice.dto.TransactionRequest;
import com.backendev.transactionservice.dto.TransactionResponse;
import com.backendev.transactionservice.dto.TransferRequest;
import com.backendev.transactionservice.entity.AccountBalance;
import com.backendev.transactionservice.entity.Transaction;
import com.backendev.transactionservice.enums.TransactionStatus;
import com.backendev.transactionservice.enums.TransactionType;
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
import java.time.Instant;
import java.util.Random;

@Service
@Slf4j
public class TransactionService {

    private static final Random random = new Random();

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final AccountBalanceRepository accountBalanceRepository;
    private final TransactionMapper transactionMapper;
    private final AccountBalanceMapper accountBalanceMapper;
    private final UserInfoService userInfoService;


    public TransactionService(TransactionRepository transactionRepository, AccountService accountService, AccountBalanceRepository accountBalanceRepository, TransactionMapper transactionMapper, AccountBalanceMapper accountBalanceMapper, UserInfoService userInfoService) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.accountBalanceRepository = accountBalanceRepository;
        this.transactionMapper = transactionMapper;
        this.accountBalanceMapper = accountBalanceMapper;
        this.userInfoService = userInfoService;
    }

    @Transactional(rollbackFor = Exception.class)
    public TransactionResponse deposit(@Valid TransactionRequest request) {
        return processTransaction(request, TransactionType.DEPOSIT,
                () -> {
                    updateAccountBalance(request.getAccountNumber(), request.getAmount());
            return getBalance(request.getAccountNumber());
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public TransactionResponse withdraw(TransactionRequest request) {
        return processTransaction(request, TransactionType.WITHDRAWAL, () -> {
            validateSufficientFunds(request.getAccountNumber(), request.getAmount());
            updateAccountBalance(request.getAccountNumber(), request.getAmount().negate());
            return getBalance(request.getAccountNumber());
        });
    }

    public TransactionResponse transfer(TransferRequest request) {
        String currentUserId = userInfoService.getCurrentUserId();
        accountService.validateAccountAndOwnership(request.getFromAccountNumber(), currentUserId);

        Transaction transaction = createAndSaveTransaction(request, TransactionType.TRANSFER);

        try {
            validateSufficientFunds(request.getFromAccountNumber(), request.getAmount());
            updateAccountBalance(request.getFromAccountNumber(), request.getAmount().negate());
            updateAccountBalance(request.getToAccountNumber(), request.getAmount());

            BigDecimal newBalance = getBalance(request.getFromAccountNumber());
            return completeTransaction(transaction, newBalance);

        } catch (InsufficientFundsException | InvalidAccountException | TransactionProcessingException e) {
            return failTransaction(transaction, "Transfer failed", e);
        }
    }

    private TransactionResponse processTransaction(TransactionRequest request,
                                                   TransactionType type,
                                                   BalanceOperation operation) {
        String currentUserId = userInfoService.getCurrentUserId();
        accountService.validateAccountAndOwnership(request.getAccountNumber(), currentUserId);

        Transaction transaction = createAndSaveTransaction(request, type);

        try {
            BigDecimal newBalance = operation.processBalanceChange();
            return completeTransaction(transaction, newBalance);
        } catch (Exception e) {
            String errorMessage = type == TransactionType.DEPOSIT ? "Deposit failed" : "Withdrawal failed";
            return failTransaction(transaction, errorMessage, e);
        }
    }

    private Transaction createAndSaveTransaction(Object request, TransactionType type) {
        Transaction transaction = mapRequestToTransaction(request, type);
        transaction.setTransactionId(generateTransactionId());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCreatedAt(Instant.now());
        return transactionRepository.save(transaction);
    }

    private Transaction mapRequestToTransaction(Object request, TransactionType type) {
        return switch (type) {
            case DEPOSIT -> transactionMapper.fromDepositRequest((TransactionRequest) request);
            case WITHDRAWAL -> transactionMapper.fromWithdrawalRequest((TransactionRequest) request);
            case TRANSFER -> transactionMapper.fromTransferRequest((TransferRequest) request);
        };
    }

    private TransactionResponse completeTransaction(Transaction transaction, BigDecimal newBalance) {
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setUpdatedAt(Instant.now());
        transaction = transactionRepository.save(transaction);
        accountService.syncBalanceWithAccountService(transaction.getFromAccountNumber(), newBalance);
        return transactionMapper.toResponseWithBalance(transaction, newBalance);
    }

    private TransactionResponse failTransaction(Transaction transaction, String errorMessage, Exception cause) {
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setUpdatedAt(Instant.now());
        transactionRepository.save(transaction);

        log.error("Transaction {} failed: {}", transaction.getTransactionId(), cause.getMessage(), cause);
        throw new TransactionProcessingException(errorMessage, cause);
    }

    public BigDecimal getBalance(Long accountNumber) {
        return accountBalanceRepository.findById(accountNumber)
                .map(AccountBalance::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() +
                String.format("%04d", random.nextInt(10000));
    }

    private void updateAccountBalance(Long accountNumber, BigDecimal amount) {
        AccountBalance accountBalance = accountBalanceRepository.findById(accountNumber)
                .orElse(accountBalanceMapper.createAccountBalance(accountNumber, BigDecimal.ZERO));

        accountBalance.setBalance(accountBalance.getBalance().add(amount));
        accountBalance.setLastUpdated(Instant.now());

        accountBalanceRepository.save(accountBalance);
    }

    private void validateSufficientFunds(Long accountNumber, BigDecimal amount) {
        BigDecimal currentBalance = getBalance(accountNumber);
        if (currentBalance.compareTo(amount) < 0) {
            log.warn("Insufficient funds for account {}: required={}, available={}",
                    accountNumber, amount, currentBalance);
            throw new InsufficientFundsException("Insufficient balance. Required: " + amount + ", Available: " + currentBalance);
        }
    }

    @FunctionalInterface
    private interface BalanceOperation {
        BigDecimal processBalanceChange() throws InsufficientFundsException, InvalidAccountException, TransactionProcessingException;
    }

}
