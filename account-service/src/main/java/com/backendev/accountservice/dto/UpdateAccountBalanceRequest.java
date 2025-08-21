package com.backendev.accountservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateAccountBalanceRequest {

    @NotNull
    private Long accountNumber;

    @NotNull
    private BigDecimal balance;
}
