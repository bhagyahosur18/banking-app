package com.backendev.transactionservice.exception;

public class AccountNumberNotFoundException extends RuntimeException{

    public AccountNumberNotFoundException(String message){
        super(message);
    }
}
