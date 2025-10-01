package com.backendev.transactionservice.service;

import com.backendev.transactionservice.dto.TransactionRequest;
import com.backendev.transactionservice.dto.TransactionResponse;
import com.backendev.transactionservice.dto.TransferRequest;
import com.backendev.transactionservice.entity.Transaction;
import com.backendev.transactionservice.enums.TransactionStatus;
import com.backendev.transactionservice.enums.TransactionType;
import com.backendev.transactionservice.exception.InsufficientFundsException;
import com.backendev.transactionservice.exception.InvalidAccountException;
import com.backendev.transactionservice.exception.TransactionProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionHandlerTest {
    @Mock
    private UserInfoService userInfoService;

    @Mock
    private TransactionProcessor transactionProcessor;

    @Mock
    private AccountService accountService;

    @Mock
    private BalanceManager balanceManager;

    @InjectMocks
    private TransactionHandler transactionHandler;

    private static final String USER_ID = "user123";
    private static final Long ACCOUNT_NUMBER = 12345L;
    private static final Long TO_ACCOUNT_NUMBER = 67890L;
    private static final BigDecimal AMOUNT = new BigDecimal("500.00");
    private static final BigDecimal NEW_BALANCE = new BigDecimal("1500.00");
    private static final String TRANSACTION_ID = "TXN123456";

    private TransactionRequest transactionRequest;
    private TransferRequest transferRequest;
    private Transaction transaction;
    private TransactionResponse transactionResponse;

    @BeforeEach
    void setUp() {
        transactionRequest = new TransactionRequest();
        transactionRequest.setAccountNumber(ACCOUNT_NUMBER);
        transactionRequest.setAmount(AMOUNT);

        transferRequest = new TransferRequest();
        transferRequest.setFromAccountNumber(ACCOUNT_NUMBER);
        transferRequest.setToAccountNumber(TO_ACCOUNT_NUMBER);
        transferRequest.setAmount(AMOUNT);

        transaction = new Transaction();
        transaction.setTransactionId(TRANSACTION_ID);
        transaction.setFromAccountNumber(ACCOUNT_NUMBER);
        transaction.setAmount(AMOUNT);
        transaction.setStatus(TransactionStatus.PENDING);

        transactionResponse = new TransactionResponse();
        transactionResponse.setTransactionId(TRANSACTION_ID);
        transactionResponse.setStatus(TransactionStatus.COMPLETED);
        transactionResponse.setAccountBalance(NEW_BALANCE);
    }

    @Test
    void processTransaction_Deposit_Success() {
        TransactionHandler.BalanceOperation operation = () -> NEW_BALANCE;

        when(userInfoService.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionProcessor.createAndSaveTransaction(transactionRequest, TransactionType.DEPOSIT))
                .thenReturn(transaction);
        when(transactionProcessor.completeTransaction(transaction, NEW_BALANCE))
                .thenReturn(transactionResponse);

        TransactionResponse result = transactionHandler.processTransaction(
                transactionRequest, TransactionType.DEPOSIT, operation);

        assertNotNull(result);
        assertEquals(TransactionStatus.COMPLETED, result.getStatus());
        verify(userInfoService).getCurrentUserId();
        verify(accountService).validateAccountAndOwnership(ACCOUNT_NUMBER, USER_ID);
        verify(transactionProcessor).createAndSaveTransaction(transactionRequest, TransactionType.DEPOSIT);
        verify(transactionProcessor).completeTransaction(transaction, NEW_BALANCE);
    }

    @Test
    void processTransaction_Withdrawal_Success() {
        TransactionHandler.BalanceOperation operation = () -> NEW_BALANCE;

        when(userInfoService.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionProcessor.createAndSaveTransaction(transactionRequest, TransactionType.WITHDRAWAL))
                .thenReturn(transaction);
        when(transactionProcessor.completeTransaction(transaction, NEW_BALANCE))
                .thenReturn(transactionResponse);

        TransactionResponse result = transactionHandler.processTransaction(
                transactionRequest, TransactionType.WITHDRAWAL, operation);

        assertNotNull(result);
        verify(accountService).validateAccountAndOwnership(ACCOUNT_NUMBER, USER_ID);
        verify(transactionProcessor).createAndSaveTransaction(transactionRequest, TransactionType.WITHDRAWAL);
        verify(transactionProcessor).completeTransaction(transaction, NEW_BALANCE);
    }

    @Test
    void processTransaction_DepositFails_ReturnsFailedResponse() {
        TransactionProcessingException exception = new TransactionProcessingException("Processing error");
        TransactionHandler.BalanceOperation operation = () -> {
            throw exception;
        };

        when(userInfoService.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionProcessor.createAndSaveTransaction(transactionRequest, TransactionType.DEPOSIT))
                .thenReturn(transaction);
        when(transactionProcessor.failTransaction(eq(transaction), eq("Deposit failed"), any(Exception.class)))
                .thenReturn(transactionResponse);

        transactionHandler.processTransaction(
                transactionRequest, TransactionType.DEPOSIT, operation);

        verify(transactionProcessor).failTransaction(eq(transaction), eq("Deposit failed"), any(Exception.class));
        verify(transactionProcessor, never()).completeTransaction(any(), any());
    }

    @Test
    void processTransaction_WithdrawalFails_ReturnsFailedResponse() {
        InsufficientFundsException exception = new InsufficientFundsException("Insufficient funds");
        TransactionHandler.BalanceOperation operation = () -> {
            throw exception;
        };

        when(userInfoService.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionProcessor.createAndSaveTransaction(transactionRequest, TransactionType.WITHDRAWAL))
                .thenReturn(transaction);
        when(transactionProcessor.failTransaction(eq(transaction), eq("Withdrawal failed"), any(Exception.class)))
                .thenReturn(transactionResponse);

        transactionHandler.processTransaction(
                transactionRequest, TransactionType.WITHDRAWAL, operation);

        verify(transactionProcessor).failTransaction(eq(transaction), eq("Withdrawal failed"), any(Exception.class));
    }

    @Test
    void processTransferTransaction_Success() {
        when(transactionProcessor.createAndSaveTransaction(transferRequest, TransactionType.TRANSFER))
                .thenReturn(transaction);
        when(balanceManager.getBalance(ACCOUNT_NUMBER)).thenReturn(NEW_BALANCE);
        when(transactionProcessor.completeTransaction(transaction, NEW_BALANCE))
                .thenReturn(transactionResponse);

        TransactionResponse result = transactionHandler.processTransferTransaction(transferRequest);

        assertNotNull(result);
        assertEquals(TransactionStatus.COMPLETED, result.getStatus());
        verify(balanceManager).validateSufficientFunds(ACCOUNT_NUMBER, AMOUNT);
        verify(balanceManager).updateAccountBalance(ACCOUNT_NUMBER, AMOUNT.negate());
        verify(balanceManager).updateAccountBalance(TO_ACCOUNT_NUMBER, AMOUNT);
        verify(balanceManager).getBalance(ACCOUNT_NUMBER);
        verify(transactionProcessor).completeTransaction(transaction, NEW_BALANCE);
    }

    @Test
    void processTransferTransaction_InsufficientFunds_FailsTransaction() {
        InsufficientFundsException exception = new InsufficientFundsException("Insufficient funds");

        when(transactionProcessor.createAndSaveTransaction(transferRequest, TransactionType.TRANSFER))
                .thenReturn(transaction);
        doThrow(exception).when(balanceManager).validateSufficientFunds(ACCOUNT_NUMBER, AMOUNT);
        when(transactionProcessor.failTransaction(eq(transaction), eq("Transfer failed"), any(InsufficientFundsException.class)))
                .thenReturn(transactionResponse);

        transactionHandler.processTransferTransaction(transferRequest);

        verify(balanceManager).validateSufficientFunds(ACCOUNT_NUMBER, AMOUNT);
        verify(balanceManager, never()).updateAccountBalance(any(), any());
        verify(transactionProcessor).failTransaction(eq(transaction), eq("Transfer failed"), any(InsufficientFundsException.class));
    }


    @Test
    void processTransferTransaction_InvalidAccount_FailsTransaction() {
        InvalidAccountException exception = new InvalidAccountException("Invalid account");

        when(transactionProcessor.createAndSaveTransaction(transferRequest, TransactionType.TRANSFER))
                .thenReturn(transaction);
        doThrow(exception).when(balanceManager).updateAccountBalance(ACCOUNT_NUMBER, AMOUNT.negate());
        when(transactionProcessor.failTransaction(eq(transaction), eq("Transfer failed"), any(InvalidAccountException.class)))
                .thenReturn(transactionResponse);

        transactionHandler.processTransferTransaction(transferRequest);

        verify(balanceManager).validateSufficientFunds(ACCOUNT_NUMBER, AMOUNT);
        verify(transactionProcessor).failTransaction(eq(transaction), eq("Transfer failed"), any(InvalidAccountException.class));
    }


    @Test
    void processTransferTransaction_TransactionProcessingException_FailsTransaction() {
        TransactionProcessingException exception = new TransactionProcessingException("Processing error");

        when(transactionProcessor.createAndSaveTransaction(transferRequest, TransactionType.TRANSFER))
                .thenReturn(transaction);
        when(balanceManager.getBalance(ACCOUNT_NUMBER)).thenThrow(exception);
        when(transactionProcessor.failTransaction(eq(transaction), eq("Transfer failed"), any(TransactionProcessingException.class)))
                .thenReturn(transactionResponse);

        transactionHandler.processTransferTransaction(transferRequest);

        verify(transactionProcessor).failTransaction(eq(transaction), eq("Transfer failed"), any(TransactionProcessingException.class));
    }
}