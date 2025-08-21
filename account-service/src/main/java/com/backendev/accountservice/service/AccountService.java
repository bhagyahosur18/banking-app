package com.backendev.accountservice.service;

import com.backendev.accountservice.constants.AccountConstants;
import com.backendev.accountservice.dto.AccountDetailsDto;
import com.backendev.accountservice.dto.AccountDto;
import com.backendev.accountservice.dto.AccountResponse;
import com.backendev.accountservice.dto.CreateAccountRequest;
import com.backendev.accountservice.dto.UpdateAccountBalanceRequest;
import com.backendev.accountservice.entity.Account;
import com.backendev.accountservice.enums.AccountLimits;
import com.backendev.accountservice.enums.AccountStatus;
import com.backendev.accountservice.enums.AccountType;
import com.backendev.accountservice.exception.AccountLimitExceededException;
import com.backendev.accountservice.exception.AccountNotFoundException;
import com.backendev.accountservice.mapper.AccountMapper;
import com.backendev.accountservice.repository.AccountRepository;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
public class AccountService {

    private static final Random RANDOM = new Random();

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    public AccountService(AccountRepository accountRepository, AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.accountMapper = accountMapper;
    }


    public AccountDto createAccount(String userId, CreateAccountRequest accountRequest) {
        AccountType accountType = accountRequest.getAccountType();

        validateAccountLimit(userId, accountType);

        Account account = accountMapper.createNewAccount(accountRequest, userId, generateRandomAccountNumber());
        Account saved = accountRepository.save(account);

        log.info("Account created for user with user Id {}", userId);
        return accountMapper.toAccountDto(saved);
    }

    private void validateAccountLimit(String userId, AccountType accountType) {
        long currentAccountCount = accountRepository.countByUserIdAndType(userId, accountType);
        int maxAccountsAllowed = AccountLimits.getMaxAccountsForType(accountType);

        if (currentAccountCount >= maxAccountsAllowed) {
            throw new AccountLimitExceededException(
                    String.format("User has reached the limit for %s accounts (%d/%d)",
                            accountType, currentAccountCount, maxAccountsAllowed)
            );
        }
    }

    private Long generateRandomAccountNumber() {
        long base = 1000000000L;
        long accountNumber;
        do {
            accountNumber = base + RANDOM.nextInt(900_000_000);
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }

    public AccountDetailsDto fetchAccountDetails(Long accountNumber, String userId) {
        Account account = accountRepository.findByAccountNumberAndUserId(accountNumber, userId)
                .orElseThrow(() -> new AccountNotFoundException(AccountConstants.ACCOUNT_NOT_FOUND + accountNumber +
                        " and user id: " + userId));
        return accountMapper.toAccountDetailsDto(account);
    }

    public List<AccountDetailsDto> fetchAccountsForUser(String userId) {
        if(!accountRepository.existsByUserId(userId)){
            throw new AccountNotFoundException("Account not found for the user id: "+ userId);
        }
        List<Account> userAccounts = accountRepository.findByUserId(userId);
        return accountMapper.toAccountDetailsDto(userAccounts);
    }


    public AccountResponse deleteAccount(Long accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(AccountConstants.ACCOUNT_NOT_FOUND + accountNumber));
        accountRepository.delete(account);
        return new AccountResponse("DELETED", "Account deleted successfully");
    }

// From Transaction service
    public AccountDto updateAccountBalance(UpdateAccountBalanceRequest updateRequest) {
        Account account = accountRepository.findByAccountNumber(updateRequest.getAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(AccountConstants.ACCOUNT_NOT_FOUND + updateRequest.getAccountNumber()));
        account.setBalance(updateRequest.getBalance());
        account.setUpdatedAt(Instant.now());
        accountRepository.save(account);
        return accountMapper.toAccountDto(account);
    }

    public AccountDto markAccountFrozen(@NotNull Long accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(AccountConstants.ACCOUNT_NOT_FOUND + accountNumber));
        account.setStatus(AccountStatus.FROZEN);
        accountRepository.save(account);
        return accountMapper.toAccountDto(account);
    }

    public boolean doesUserOwnAccount(String userId, @NotNull Long accountNumber) {
        return accountRepository.existsByAccountNumberAndUserId(accountNumber, userId);
    }
}
