package com.backendev.transactionservice.client;

import com.backendev.transactionservice.dto.AccountResponse;
import com.backendev.transactionservice.dto.UpdateAccountBalanceRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "account-service")
public interface AccountServiceClient {

    @GetMapping("/api/v1/accounts/{accountNumber}")
    ResponseEntity<AccountResponse> getAccount(
            @PathVariable Long accountNumber);

    @PutMapping("/api/v1/accounts/{accountNumber}")
    ResponseEntity<AccountResponse> updateAccountBalance(@PathVariable Long accountNumber,
                                                         UpdateAccountBalanceRequest updateRequest);


}
