package com.backendev.transactionservice.mapper;

import com.backendev.transactionservice.dto.TransactionRequest;
import com.backendev.transactionservice.dto.TransactionResponse;
import com.backendev.transactionservice.dto.TransferRequest;
import com.backendev.transactionservice.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface TransactionMapper {
    @Mapping(target = "timestamp", source = "createdAt")
    @Mapping(target = "accountBalance", ignore = true)
    TransactionResponse toResponse(Transaction transaction);

    // Enhanced mapping with user's account balance only
    @Mapping(target = "timestamp", source = "transaction.createdAt")
    @Mapping(target = "fromAccountNumber", source = "transaction.fromAccountNumber")
    @Mapping(target = "toAccountNumber", source = "transaction.toAccountNumber")
    TransactionResponse toResponseWithBalance(Transaction transaction, BigDecimal accountBalance);

    @Mapping(target = "transactionId", ignore = true)
    @Mapping(target = "type", constant = "TRANSFER")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "updatedAt", ignore = true)
    Transaction fromTransferRequest(TransferRequest request);

    @Mapping(target = "transactionId", ignore = true)
    @Mapping(target = "fromAccountNumber", source = "accountNumber")
    @Mapping(target = "toAccountNumber",  source = "accountNumber")
    @Mapping(target = "type", constant = "DEPOSIT")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "updatedAt", ignore = true)
    Transaction fromDepositRequest(TransactionRequest request);

    @Mapping(target = "transactionId", ignore = true)
    @Mapping(target = "fromAccountNumber", source = "accountNumber")
    @Mapping(target = "toAccountNumber", source = "accountNumber")
    @Mapping(target = "type", constant = "WITHDRAWAL")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "updatedAt", ignore = true)
    Transaction fromWithdrawalRequest(TransactionRequest request);

}
