package com.backendev.transactionservice.service;

import com.backendev.transactionservice.client.AccountServiceClient;
import com.backendev.transactionservice.dto.AccountResponse;
import com.backendev.transactionservice.dto.TransactionRequest;
import com.backendev.transactionservice.dto.TransactionResponse;
import com.backendev.transactionservice.dto.TransferRequest;
import com.backendev.transactionservice.dto.UpdateAccountBalanceRequest;
import com.backendev.transactionservice.entity.AccountBalance;
import com.backendev.transactionservice.entity.Transaction;
import com.backendev.transactionservice.enums.TransactionStatus;
import com.backendev.transactionservice.exception.InsufficientFundsException;
import com.backendev.transactionservice.exception.InvalidAccountException;
import com.backendev.transactionservice.exception.TransactionProcessingException;
import com.backendev.transactionservice.mapper.AccountBalanceMapper;
import com.backendev.transactionservice.mapper.TransactionMapper;
import com.backendev.transactionservice.repository.AccountBalanceRepository;
import com.backendev.transactionservice.repository.TransactionRepository;
import feign.FeignException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
    private final AccountServiceClient accountServiceClient;
    private final AccountBalanceRepository accountBalanceRepository;
    private final TransactionMapper transactionMapper;
    private final AccountBalanceMapper accountBalanceMapper;
    private final UserInfoService userInfoService;

    public TransactionService(TransactionRepository transactionRepository, AccountServiceClient accountServiceClient, AccountBalanceRepository accountBalanceRepository, TransactionMapper transactionMapper, AccountBalanceMapper accountBalanceMapper, UserInfoService userInfoService) {
        this.transactionRepository = transactionRepository;
        this.accountServiceClient = accountServiceClient;
        this.accountBalanceRepository = accountBalanceRepository;
        this.transactionMapper = transactionMapper;
        this.accountBalanceMapper = accountBalanceMapper;
        this.userInfoService = userInfoService;
    }

    @Transactional(rollbackFor = Exception.class)
    public TransactionResponse deposit(@Valid TransactionRequest request) {
        String currentUserId = userInfoService.getCurrentUserId();

        AccountResponse account = validateAccount(request.getAccountNumber());
        validateAccountOwnership(account, currentUserId);

        Transaction transaction = transactionMapper.fromDepositRequest(request);
        transaction.setTransactionId(generateTransactionId());
        transactionRepository.save(transaction);

        try {
            updateBalance(request.getAccountNumber(), request.getAmount());

            BigDecimal newBalance = getBalance(request.getAccountNumber());
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setUpdatedAt(Instant.now());
            transactionRepository.save(transaction);

            updateAccountBalance(request.getAccountNumber(), newBalance);
            return transactionMapper.toResponseWithBalance(transaction, newBalance);

        } catch (Exception e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new TransactionProcessingException("Deposit failed");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public TransactionResponse withdraw(TransactionRequest request) {
        String currentUserId = userInfoService.getCurrentUserId();
        AccountResponse account = validateAccount(request.getAccountNumber());
        validateAccountOwnership(account, currentUserId);

        BigDecimal currentBalance = getBalance(request.getAccountNumber());
        if (currentBalance.compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient balance");
        }
        Transaction transaction = transactionMapper.fromWithdrawalRequest(request);
        transaction.setTransactionId(generateTransactionId());
        transaction = transactionRepository.save(transaction);
        try {
            updateBalance(request.getAccountNumber(), request.getAmount().negate());
            BigDecimal newBalance = getBalance(request.getAccountNumber());
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setUpdatedAt(Instant.now());
            transaction = transactionRepository.save(transaction);
            return transactionMapper.toResponseWithBalance(transaction, newBalance);

        } catch (Exception e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new TransactionProcessingException("Withdrawal failed: " + e.getMessage());
        }
    }

    public TransactionResponse transfer(TransferRequest request) {
        String currentUserId = userInfoService.getCurrentUserId();
        AccountResponse fromAccount = validateAccount(request.getFromAccountNumber());
        validateAccountOwnership(fromAccount, currentUserId);

        BigDecimal currentBalance = getBalance(request.getFromAccountNumber());
        if (currentBalance.compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient balance");
        }
        Transaction transaction = transactionMapper.fromTransferRequest(request);
        transaction.setTransactionId(generateTransactionId());
        transactionRepository.save(transaction);
        try {
            updateBalance(request.getFromAccountNumber(), request.getAmount().negate());
            updateBalance(request.getToAccountNumber(), request.getAmount());
            BigDecimal userAccountNewBalance = getBalance(request.getFromAccountNumber());
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setUpdatedAt(Instant.now());
            transactionRepository.save(transaction);
            return transactionMapper.toResponseWithBalance(transaction, userAccountNewBalance);

        } catch (Exception e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new TransactionProcessingException("Transfer failed");
        }
    }

    private AccountResponse validateAccount(Long accountNumber) {
        try {
            ResponseEntity<AccountResponse> response = accountServiceClient.getAccount(accountNumber);
            AccountResponse account = response.getBody();

            if (account == null || !"ACTIVE".equals(account.getStatus())) {
                throw new InvalidAccountException("Account not found or inactive");
            }
            return account;
        } catch (FeignException e) {
            throw new InvalidAccountException("Account validation failed", e);
        }
    }

    private void validateAccountOwnership(AccountResponse account, String currentUserId) {
        if (!currentUserId.equals(account.getUserId())) {
            throw new SecurityException("Access denied: Account does not belong to current user");
        }
    }

    public BigDecimal getBalance(Long accountNumber) {
        validateAccount(accountNumber);
        return accountBalanceRepository.findById(accountNumber)
                .map(AccountBalance::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() +
                String.format("%04d", random.nextInt(10000));
    }
    private void updateBalance(Long accountNumber, BigDecimal amount) {
        AccountBalance accountBalance = accountBalanceRepository.findById(accountNumber)
                .orElse(accountBalanceMapper.createAccountBalance(accountNumber, BigDecimal.ZERO));

        accountBalance.setBalance(accountBalance.getBalance().add(amount));
        accountBalance.setLastUpdated(Instant.now());

        accountBalanceRepository.save(accountBalance);
    }

    private void updateAccountBalance(Long accountNumber, BigDecimal newBalance) {
        try {
            log.info("Updating account {} with balance: {}", accountNumber, newBalance); // Add this

            if (newBalance == null) {
                throw new IllegalArgumentException("Balance cannot be null");
            }

            UpdateAccountBalanceRequest updateRequest = new UpdateAccountBalanceRequest(accountNumber, newBalance);

            ResponseEntity<AccountResponse> response = accountServiceClient.updateAccountBalance(accountNumber, updateRequest);
            AccountResponse account = response.getBody();

            if (account == null || !"ACTIVE".equals(account.getStatus())) {
                throw new InvalidAccountException("Account not found or inactive");
            }
        } catch (FeignException e) {
            throw new InvalidAccountException("Account validation failed", e);
        }
    }

}
