package com.backendev.transactionservice.service;

import com.backendev.transactionservice.dto.AccountBalanceInfo;
import com.backendev.transactionservice.dto.TransactionInfo;
import com.backendev.transactionservice.dto.TransactionRequest;
import com.backendev.transactionservice.dto.TransactionResponse;
import com.backendev.transactionservice.dto.TransferRequest;
import com.backendev.transactionservice.entity.AccountBalance;
import com.backendev.transactionservice.entity.Transaction;
import com.backendev.transactionservice.enums.TransactionStatus;
import com.backendev.transactionservice.enums.TransactionType;
import com.backendev.transactionservice.exception.InvalidAccountException;
import com.backendev.transactionservice.mapper.AccountBalanceMapper;
import com.backendev.transactionservice.mapper.TransactionMapper;
import com.backendev.transactionservice.repository.AccountBalanceRepository;
import com.backendev.transactionservice.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock
    private AccountService accountService;

    @Mock
    private UserInfoService userInfoService;

    @Mock
    private BalanceManager balanceManager;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private AccountBalanceRepository accountBalanceRepository;

    @Mock
    private AccountBalanceMapper accountBalanceMapper;

    @Mock
    private TransactionHandler transactionHandler;

    @InjectMocks
    private TransactionService transactionService;

    @Captor
    private ArgumentCaptor<TransactionHandler.BalanceOperation> balanceOperationCaptor;

    private static final String USER_ID = "user123";
    private static final Long ACCOUNT_NUMBER = 12345L;
    private static final Long TO_ACCOUNT_NUMBER = 67890L;
    private static final BigDecimal AMOUNT = new BigDecimal("500.00");
    private static final BigDecimal BALANCE = new BigDecimal("1500.00");

    private TransactionRequest transactionRequest;
    private TransferRequest transferRequest;
    private TransactionResponse transactionResponse;
    private Transaction transaction;
    private AccountBalance accountBalance;
    private AccountBalanceInfo accountBalanceInfo;
    private TransactionInfo transactionInfo;

    @BeforeEach
    void setUp() {
        transactionRequest = new TransactionRequest();
        transactionRequest.setAccountNumber(ACCOUNT_NUMBER);
        transactionRequest.setAmount(AMOUNT);

        transferRequest = new TransferRequest();
        transferRequest.setFromAccountNumber(ACCOUNT_NUMBER);
        transferRequest.setToAccountNumber(TO_ACCOUNT_NUMBER);
        transferRequest.setAmount(AMOUNT);

        transactionResponse = new TransactionResponse();
        transactionResponse.setTransactionId("TXN123456");
        transactionResponse.setStatus(TransactionStatus.COMPLETED);
        transactionResponse.setAccountBalance(BALANCE);

        transaction = new Transaction();
        transaction.setTransactionId("TXN123456");
        transaction.setFromAccountNumber(ACCOUNT_NUMBER);
        transaction.setAmount(AMOUNT);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCreatedAt(Instant.now());

        accountBalance = new AccountBalance(ACCOUNT_NUMBER, BALANCE, Instant.now());

        accountBalanceInfo = new AccountBalanceInfo();
        accountBalanceInfo.setAccountNumber(ACCOUNT_NUMBER);
        accountBalanceInfo.setBalance(BALANCE);
        accountBalanceInfo.setLastUpdated("01 Oct 2025, 10:30 AM");

        transactionInfo = new TransactionInfo();
        transactionInfo.setTransactionId("TXN123456");
        transactionInfo.setFromAccountNumber(ACCOUNT_NUMBER);
        transactionInfo.setAmount(AMOUNT);
        transactionInfo.setType(TransactionType.DEPOSIT);
        transactionInfo.setStatus(TransactionStatus.COMPLETED);
        transactionInfo.setCreatedAt("01 Oct 2025, 10:30 AM");
    }

    @Test
    void deposit_Success() {
        when(transactionHandler.processTransaction(eq(transactionRequest), eq(TransactionType.DEPOSIT), any()))
                .thenReturn(transactionResponse);

        TransactionResponse result = transactionService.deposit(transactionRequest);

        assertNotNull(result);
        assertEquals(TransactionStatus.COMPLETED, result.getStatus());
        verify(transactionHandler).processTransaction(eq(transactionRequest), eq(TransactionType.DEPOSIT), any());
    }


    @Test
    void deposit_UpdatesBalanceCorrectly() {
        when(transactionHandler.processTransaction(eq(transactionRequest), eq(TransactionType.DEPOSIT), any()))
                .thenAnswer(invocation -> {
                    TransactionHandler.BalanceOperation operation = invocation.getArgument(2);
                    operation.processBalanceChange();
                    return transactionResponse;
                });
        when(balanceManager.getBalance(ACCOUNT_NUMBER)).thenReturn(BALANCE);

        transactionService.deposit(transactionRequest);

        verify(balanceManager).updateAccountBalance(ACCOUNT_NUMBER, AMOUNT);
        verify(balanceManager).getBalance(ACCOUNT_NUMBER);
    }

    @Test
    void withdraw_Success() {
        when(transactionHandler.processTransaction(eq(transactionRequest), eq(TransactionType.WITHDRAWAL), any()))
                .thenReturn(transactionResponse);

        TransactionResponse result = transactionService.withdraw(transactionRequest);

        assertNotNull(result);
        assertEquals(TransactionStatus.COMPLETED, result.getStatus());
        verify(transactionHandler).processTransaction(eq(transactionRequest), eq(TransactionType.WITHDRAWAL), any());
    }

    @Test
    void withdraw_ValidatesAndUpdatesBalanceCorrectly() {
        when(transactionHandler.processTransaction(eq(transactionRequest), eq(TransactionType.WITHDRAWAL), any()))
                .thenAnswer(invocation -> {
                    TransactionHandler.BalanceOperation operation = invocation.getArgument(2);
                    operation.processBalanceChange();
                    return transactionResponse;
                });
        when(balanceManager.getBalance(ACCOUNT_NUMBER)).thenReturn(BALANCE);

        transactionService.withdraw(transactionRequest);

        verify(balanceManager).validateSufficientFunds(ACCOUNT_NUMBER, AMOUNT);
        verify(balanceManager).updateAccountBalance(ACCOUNT_NUMBER, AMOUNT.negate());
        verify(balanceManager).getBalance(ACCOUNT_NUMBER);
    }

    @Test
    void transfer_Success() {
        when(userInfoService.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionHandler.processTransferTransaction(transferRequest)).thenReturn(transactionResponse);

        TransactionResponse result = transactionService.transfer(transferRequest);

        assertNotNull(result);
        assertEquals(TransactionStatus.COMPLETED, result.getStatus());
        verify(userInfoService).getCurrentUserId();
        verify(accountService).validateAccountAndOwnership(ACCOUNT_NUMBER, USER_ID);
        verify(transactionHandler).processTransferTransaction(transferRequest);
    }

    @Test
    void fetchTransactionsByAccount_Success() {
        List<Transaction> transactions = Arrays.asList(transaction);
        List<TransactionInfo> transactionInfoList = Arrays.asList(transactionInfo);

        when(transactionRepository.findAllByFromAccountNumberOrderByCreatedAtDesc(ACCOUNT_NUMBER))
                .thenReturn(transactions);
        when(transactionMapper.toTransactionInfoList(transactions)).thenReturn(transactionInfoList);

        List<TransactionInfo> result = transactionService.fetchTransactionsByAccount(ACCOUNT_NUMBER);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("TXN123456", result.get(0).getTransactionId());
        assertEquals(ACCOUNT_NUMBER, result.get(0).getFromAccountNumber());
        verify(transactionRepository).findAllByFromAccountNumberOrderByCreatedAtDesc(ACCOUNT_NUMBER);
        verify(transactionMapper).toTransactionInfoList(transactions);
    }

    @Test
    void fetchTransactionsByAccount_NoTransactions_ThrowsException() {
        when(transactionRepository.findAllByFromAccountNumberOrderByCreatedAtDesc(ACCOUNT_NUMBER))
                .thenReturn(Collections.emptyList());

        assertThrows(InvalidAccountException.class,
                () -> transactionService.fetchTransactionsByAccount(ACCOUNT_NUMBER));

        verify(transactionRepository).findAllByFromAccountNumberOrderByCreatedAtDesc(ACCOUNT_NUMBER);
        verify(transactionMapper, never()).toTransactionInfoList(any());
    }

    @Test
    void fetchTransactionsByAccount_MultipleTransactions_ReturnsAll() {
        Transaction transaction2 = new Transaction();
        transaction2.setTransactionId("TXN789012");
        transaction2.setFromAccountNumber(ACCOUNT_NUMBER);

        TransactionInfo transactionInfo2 = new TransactionInfo();
        transactionInfo2.setTransactionId("TXN789012");
        transactionInfo2.setFromAccountNumber(ACCOUNT_NUMBER);

        List<Transaction> transactions = Arrays.asList(transaction, transaction2);
        List<TransactionInfo> transactionInfoList = Arrays.asList(transactionInfo, transactionInfo2);

        when(transactionRepository.findAllByFromAccountNumberOrderByCreatedAtDesc(ACCOUNT_NUMBER))
                .thenReturn(transactions);
        when(transactionMapper.toTransactionInfoList(transactions)).thenReturn(transactionInfoList);

        List<TransactionInfo> result = transactionService.fetchTransactionsByAccount(ACCOUNT_NUMBER);

        assertEquals(2, result.size());
        verify(transactionMapper).toTransactionInfoList(transactions);
    }

    @Test
    void fetchAccountBalance_Success() {
        when(accountBalanceRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(accountBalance);
        when(accountBalanceMapper.toAccountBalanceInfo(accountBalance)).thenReturn(accountBalanceInfo);

        AccountBalanceInfo result = transactionService.fetchAccountBalance(ACCOUNT_NUMBER);

        assertNotNull(result);
        assertEquals(ACCOUNT_NUMBER, result.getAccountNumber());
        assertEquals(BALANCE, result.getBalance());
        assertEquals("01 Oct 2025, 10:30 AM", result.getLastUpdated());
        verify(accountBalanceRepository).findByAccountNumber(ACCOUNT_NUMBER);
        verify(accountBalanceMapper).toAccountBalanceInfo(accountBalance);
    }

    @Test
    void fetchAccountBalance_AccountNotFound_ThrowsException() {
        when(accountBalanceRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(null);

        assertThrows(InvalidAccountException.class,
                () -> transactionService.fetchAccountBalance(ACCOUNT_NUMBER));

        verify(accountBalanceRepository).findByAccountNumber(ACCOUNT_NUMBER);
        verify(accountBalanceMapper, never()).toAccountBalanceInfo(any());
    }

}