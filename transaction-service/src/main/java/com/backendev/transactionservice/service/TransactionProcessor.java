package com.backendev.transactionservice.service;

import com.backendev.transactionservice.dto.TransactionRequest;
import com.backendev.transactionservice.dto.TransactionResponse;
import com.backendev.transactionservice.dto.TransferRequest;
import com.backendev.transactionservice.entity.Transaction;
import com.backendev.transactionservice.enums.TransactionStatus;
import com.backendev.transactionservice.enums.TransactionType;
import com.backendev.transactionservice.exception.TransactionProcessingException;
import com.backendev.transactionservice.mapper.TransactionMapper;
import com.backendev.transactionservice.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Random;

@Component
@Slf4j
public class TransactionProcessor {

    private static final Random random = new Random();

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final AccountService accountService;

    public TransactionProcessor(TransactionRepository transactionRepository, TransactionMapper transactionMapper, AccountService accountService) {
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.accountService = accountService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Transaction createAndSaveTransaction(Object request, TransactionType type) {
        Transaction transaction = mapRequestToTransaction(request, type);
        transaction.setTransactionId(generateTransactionId());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCreatedAt(Instant.now());
        return transactionRepository.save(transaction);
    }

    @Transactional(rollbackFor = Exception.class)
    public TransactionResponse completeTransaction(Transaction transaction, BigDecimal newBalance) {
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setUpdatedAt(Instant.now());
        transaction = transactionRepository.save(transaction);
        accountService.syncBalanceWithAccountService(transaction.getFromAccountNumber(), newBalance);
        return transactionMapper.toResponseWithBalance(transaction, newBalance);
    }

    public TransactionResponse failTransaction(Transaction transaction, String errorMessage, Exception cause) {
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setUpdatedAt(Instant.now());
        transactionRepository.save(transaction);

        log.error("Transaction {} failed: {}", transaction.getTransactionId(), cause.getMessage(), cause);
        throw new TransactionProcessingException(errorMessage, cause);
    }

    private Transaction mapRequestToTransaction(Object request, TransactionType type) {
        return switch (type) {
            case DEPOSIT -> transactionMapper.fromDepositRequest((TransactionRequest) request);
            case WITHDRAWAL -> transactionMapper.fromWithdrawalRequest((TransactionRequest) request);
            case TRANSFER -> transactionMapper.fromTransferRequest((TransferRequest) request);
        };
    }

    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() +
                String.format("%04d", random.nextInt(10000));
    }
}
