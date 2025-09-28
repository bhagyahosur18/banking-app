package com.backendev.accountservice.service;

import com.backendev.accountservice.dto.AccountDetailsDto;
import com.backendev.accountservice.dto.AccountDto;
import com.backendev.accountservice.dto.AccountResponse;
import com.backendev.accountservice.dto.CreateAccountRequest;
import com.backendev.accountservice.dto.UpdateAccountBalanceRequest;
import com.backendev.accountservice.entity.Account;
import com.backendev.accountservice.enums.AccountStatus;
import com.backendev.accountservice.enums.AccountType;
import com.backendev.accountservice.exception.AccountLimitExceededException;
import com.backendev.accountservice.exception.AccountNotFoundException;
import com.backendev.accountservice.mapper.AccountMapper;
import com.backendev.accountservice.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountService accountService;

    private CreateAccountRequest createAccountRequest;
    private Account account;
    private AccountDto accountDto;
    private final String userId = "user123";
    private final Long accountNumber = 1234567890L;

    @BeforeEach
    void setUp() {
        createAccountRequest = new CreateAccountRequest();
        createAccountRequest.setAccountType(AccountType.SAVINGS);

        account = new Account();
        account.setAccountNumber(accountNumber);
        account.setUserId(userId);
        account.setBalance(BigDecimal.valueOf(1000));

        accountDto = new AccountDto();
        accountDto.setAccountNumber(accountNumber);
    }

    @Nested
    class CreateAccountTests {

        @Test
        void shouldCreateAccount_whenValidRequest() {
            when(accountRepository.countByUserIdAndType(userId, AccountType.SAVINGS)).thenReturn(0L);
            when(accountRepository.existsByAccountNumber(anyLong())).thenReturn(false);
            when(accountMapper.createNewAccount(any(), eq(userId), anyLong())).thenReturn(account);
            when(accountRepository.save(account)).thenReturn(account);
            when(accountMapper.toAccountDto(account)).thenReturn(accountDto);

            AccountDto result = accountService.createAccount(userId, createAccountRequest);

            assertNotNull(result);
            assertEquals(accountNumber, result.getAccountNumber());
            verify(accountRepository).save(account);
        }

        @Test
        void shouldThrowException_whenAccountLimitExceeded() {
            when(accountRepository.countByUserIdAndType(userId, AccountType.SAVINGS)).thenReturn(5L);

            assertThrows(AccountLimitExceededException.class,
                    () -> accountService.createAccount(userId, createAccountRequest));
        }

        @Test
        void shouldGenerateUniqueAccountNumber_whenDuplicateExists() {
            when(accountRepository.countByUserIdAndType(userId, AccountType.SAVINGS)).thenReturn(0L);
            when(accountRepository.existsByAccountNumber(anyLong()))
                    .thenReturn(true)  // First call - duplicate exists
                    .thenReturn(false); // Second call - unique number
            when(accountMapper.createNewAccount(any(), eq(userId), anyLong())).thenReturn(account);
            when(accountRepository.save(account)).thenReturn(account);
            when(accountMapper.toAccountDto(account)).thenReturn(accountDto);

            AccountDto result = accountService.createAccount(userId, createAccountRequest);

            assertNotNull(result);
            verify(accountRepository, atLeast(2)).existsByAccountNumber(anyLong());
        }
    }

    @Nested
    class FetchAccountDetailsTests {

        @Test
        void shouldFetchAccountDetails_whenAccountExists() {
            AccountDetailsDto expectedDto = new AccountDetailsDto();
            when(accountRepository.findByAccountNumberAndUserId(accountNumber, userId))
                    .thenReturn(Optional.of(account));
            when(accountMapper.toAccountDetailsDto(account)).thenReturn(expectedDto);

            AccountDetailsDto result = accountService.fetchAccountDetails(accountNumber, userId);

            assertNotNull(result);
            assertEquals(expectedDto, result);
        }

        @Test
        void shouldThrowException_whenAccountNotFound() {
            when(accountRepository.findByAccountNumberAndUserId(accountNumber, userId))
                    .thenReturn(Optional.empty());

            assertThrows(AccountNotFoundException.class,
                    () -> accountService.fetchAccountDetails(accountNumber, userId));
        }
    }

    @Nested
    class FetchAccountsForUserTests {

        @Test
        void shouldFetchUserAccounts_whenAccountsExist() {
            List<Account> accounts = List.of(account);
            List<AccountDetailsDto> expectedDtos = List.of(new AccountDetailsDto());

            when(accountRepository.existsByUserId(userId)).thenReturn(true);
            when(accountRepository.findByUserId(userId)).thenReturn(accounts);
            when(accountMapper.toAccountDetailsDto(accounts)).thenReturn(expectedDtos);

            List<AccountDetailsDto> result = accountService.fetchAccountsForUser(userId);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(expectedDtos, result);
        }

        @Test
        void shouldThrowException_whenNoAccountsForUser() {
            when(accountRepository.existsByUserId(userId)).thenReturn(false);

            assertThrows(AccountNotFoundException.class,
                    () -> accountService.fetchAccountsForUser(userId));
        }
    }

    @Nested
    class DeleteAccountTests {

        @Test
        void shouldDeleteAccount_whenAccountExists() {
            when(accountRepository.findByAccountNumber(accountNumber))
                    .thenReturn(Optional.of(account));

            AccountResponse result = accountService.deleteAccount(accountNumber);

            assertNotNull(result);
            assertEquals("DELETED", result.getStatus());
            assertEquals("Account deleted successfully", result.getMessage());
            verify(accountRepository).delete(account);
        }

        @Test
        void shouldThrowException_whenAccountNotFoundForDeletion() {
            when(accountRepository.findByAccountNumber(accountNumber))
                    .thenReturn(Optional.empty());

            assertThrows(AccountNotFoundException.class,
                    () -> accountService.deleteAccount(accountNumber));
        }
    }

    @Nested
    class UpdateAccountBalanceTests {

        @Test
        void shouldUpdateBalance_whenAccountExists() {
            UpdateAccountBalanceRequest request = new UpdateAccountBalanceRequest();
            request.setAccountNumber(accountNumber);
            request.setBalance(BigDecimal.valueOf(2000));

            when(accountRepository.findByAccountNumber(accountNumber))
                    .thenReturn(Optional.of(account));
            when(accountRepository.save(account)).thenReturn(account);
            when(accountMapper.toAccountDto(account)).thenReturn(accountDto);

            AccountDto result = accountService.updateAccountBalance(request);

            assertNotNull(result);
            verify(accountRepository).save(account);
            assertEquals(BigDecimal.valueOf(2000), account.getBalance());
            assertNotNull(account.getUpdatedAt());
        }

        @Test
        void shouldThrowException_whenAccountNotFoundForUpdate() {
            UpdateAccountBalanceRequest request = new UpdateAccountBalanceRequest();
            request.setAccountNumber(accountNumber);

            when(accountRepository.findByAccountNumber(accountNumber))
                    .thenReturn(Optional.empty());

            assertThrows(AccountNotFoundException.class,
                    () -> accountService.updateAccountBalance(request));
        }
    }

    @Nested
    class MarkAccountFrozenTests {

        @Test
        void shouldFreezeAccount_whenAccountExists() {
            when(accountRepository.findByAccountNumber(accountNumber))
                    .thenReturn(Optional.of(account));
            when(accountRepository.save(account)).thenReturn(account);
            when(accountMapper.toAccountDto(account)).thenReturn(accountDto);

            AccountDto result = accountService.markAccountFrozen(accountNumber);

            assertNotNull(result);
            verify(accountRepository).save(account);
            assertEquals(AccountStatus.FROZEN, account.getStatus());
        }

        @Test
        void shouldThrowException_whenAccountNotFoundForFreezing() {
            when(accountRepository.findByAccountNumber(accountNumber))
                    .thenReturn(Optional.empty());

            assertThrows(AccountNotFoundException.class,
                    () -> accountService.markAccountFrozen(accountNumber));
        }
    }

    @Nested
    class DoesUserOwnAccountTests {

        @Test
        void shouldReturnTrue_whenUserOwnsAccount() {
            when(accountRepository.existsByAccountNumberAndUserId(accountNumber, userId))
                    .thenReturn(true);

            boolean result = accountService.doesUserOwnAccount(userId, accountNumber);

            assertTrue(result);
        }

        @Test
        void shouldReturnFalse_whenUserDoesNotOwnAccount() {
            when(accountRepository.existsByAccountNumberAndUserId(accountNumber, userId))
                    .thenReturn(false);

            boolean result = accountService.doesUserOwnAccount(userId, accountNumber);

            assertFalse(result);
        }
    }

}