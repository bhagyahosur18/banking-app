package com.backendev.transactionservice.mapper;

import com.backendev.transactionservice.entity.AccountBalance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface AccountBalanceMapper {

    @Mapping(target = "accountNumber", source = "accountNumber")
    @Mapping(target = "balance", source = "balance")
    @Mapping(target = "lastUpdated", expression = "java(java.time.Instant.now())")
    AccountBalance createAccountBalance(Long accountNumber, BigDecimal balance);
}
