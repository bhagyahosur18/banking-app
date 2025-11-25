package com.backendev.transactionservice.integration.service;

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
import com.backendev.transactionservice.service.AccountService;
import com.backendev.transactionservice.service.BalanceManager;
import com.backendev.transactionservice.service.TransactionHandler;
import com.backendev.transactionservice.service.TransactionService;
import com.backendev.transactionservice.service.UserInfoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceIntegrationTest {

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

    private TransactionService transactionService;

    private static final Long ACCOUNT_NUMBER = 1234567890L;
    private static final Long TO_ACCOUNT_NUMBER = 9876543210L;
    private static final String USER_ID = "user-123";
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(500);

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(accountService, userInfoService, balanceManager,
                transactionRepository, transactionMapper, accountBalanceRepository, accountBalanceMapper, transactionHandler);
    }

    private TransactionRequest createTransactionRequest() {
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber(ACCOUNT_NUMBER);
        request.setAmount(AMOUNT);
        request.setDescription("Test transaction");
        return request;
    }

    private TransferRequest createTransferRequest() {
        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber(ACCOUNT_NUMBER);
        request.setToAccountNumber(TO_ACCOUNT_NUMBER);
        request.setAmount(AMOUNT);
        request.setDescription("Test transfer");
        return request;
    }

    private TransactionResponse createTransactionResponse() {
        TransactionResponse response = new TransactionResponse();
        response.setTransactionId("TXN001");
        response.setAmount(AMOUNT);
        response.setStatus(TransactionStatus.SUCCESS);
        return response;
    }
    

    @Test
    void shouldDepositMoney() {
        TransactionRequest request = createTransactionRequest();
        TransactionResponse response = createTransactionResponse();

        when(transactionHandler.processTransaction(any(TransactionRequest.class), eq(TransactionType.DEPOSIT), any()))
                .thenReturn(response);

        TransactionResponse result = transactionService.deposit(request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        verify(transactionHandler).processTransaction(any(TransactionRequest.class), eq(TransactionType.DEPOSIT), any());
    }


    @Test
    void shouldWithdrawMoney() {
        TransactionRequest request = createTransactionRequest();
        TransactionResponse response = createTransactionResponse();

        when(transactionHandler.processTransaction(any(TransactionRequest.class), eq(TransactionType.WITHDRAWAL), any()))
                .thenReturn(response);

        TransactionResponse result = transactionService.withdraw(request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        verify(transactionHandler).processTransaction(any(TransactionRequest.class), eq(TransactionType.WITHDRAWAL), any());
    }

    @Test
    void shouldTransferMoney() {
        TransferRequest request = createTransferRequest();
        TransactionResponse response = createTransactionResponse();

        when(userInfoService.getCurrentUserId()).thenReturn(USER_ID);
        doNothing().when(accountService).validateTransferAccounts(ACCOUNT_NUMBER, TO_ACCOUNT_NUMBER, USER_ID);
        when(transactionHandler.processTransferTransaction(request)).thenReturn(response);

        TransactionResponse result = transactionService.transfer(request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        verify(userInfoService).getCurrentUserId();
        verify(accountService).validateTransferAccounts(ACCOUNT_NUMBER, TO_ACCOUNT_NUMBER, USER_ID);
        verify(transactionHandler).processTransferTransaction(request);
    }

    @Test
    void shouldFetchTransactionsByAccount() {
        Transaction txn = new Transaction();
        txn.setTransactionId("TXN001");
        txn.setFromAccountNumber(ACCOUNT_NUMBER);
        List<Transaction> transactions = List.of(txn);

        TransactionInfo txnInfo = new TransactionInfo();
        txnInfo.setTransactionId("TXN001");
        List<TransactionInfo> transactionInfos = List.of(txnInfo);

        when(transactionRepository.findAllByFromAccountNumberOrderByCreatedAtDesc(ACCOUNT_NUMBER))
                .thenReturn(transactions);
        when(transactionMapper.toTransactionInfoList(transactions))
                .thenReturn(transactionInfos);

        List<TransactionInfo> result = transactionService.fetchTransactionsByAccount(ACCOUNT_NUMBER);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTransactionId()).isEqualTo("TXN001");
        verify(transactionRepository).findAllByFromAccountNumberOrderByCreatedAtDesc(ACCOUNT_NUMBER);
    }

    @Test
    void shouldThrowExceptionWhenNoTransactionsFound() {
        when(transactionRepository.findAllByFromAccountNumberOrderByCreatedAtDesc(ACCOUNT_NUMBER))
                .thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> transactionService.fetchTransactionsByAccount(ACCOUNT_NUMBER))
                .isInstanceOf(InvalidAccountException.class)
                .hasMessage("Account number does not exists: " + ACCOUNT_NUMBER);
    }


    @Test
    void shouldFetchAccountBalance() {
        AccountBalance accountBalance = new AccountBalance();
        accountBalance.setAccountNumber(ACCOUNT_NUMBER);
        accountBalance.setBalance(AMOUNT);

        AccountBalanceInfo balanceInfo = new AccountBalanceInfo();
        balanceInfo.setAccountNumber(ACCOUNT_NUMBER);
        balanceInfo.setBalance(AMOUNT);

        when(accountBalanceRepository.findByAccountNumber(ACCOUNT_NUMBER))
                .thenReturn(Optional.of(accountBalance));
        when(accountBalanceMapper.toAccountBalanceInfo(accountBalance))
                .thenReturn(balanceInfo);

        AccountBalanceInfo result = transactionService.fetchAccountBalance(ACCOUNT_NUMBER);

        assertThat(result).isNotNull();
        assertThat(result.getAccountNumber()).isEqualTo(ACCOUNT_NUMBER);
        assertThat(result.getBalance()).isEqualTo(AMOUNT);
        verify(accountBalanceRepository).findByAccountNumber(ACCOUNT_NUMBER);
    }

    @Test
    void shouldThrowExceptionWhenAccountBalanceNotFound() {
        when(accountBalanceRepository.findByAccountNumber(ACCOUNT_NUMBER))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.fetchAccountBalance(ACCOUNT_NUMBER))
                .isInstanceOf(InvalidAccountException.class)
                .hasMessage("Account number does not exist: " + ACCOUNT_NUMBER);
    }
}
