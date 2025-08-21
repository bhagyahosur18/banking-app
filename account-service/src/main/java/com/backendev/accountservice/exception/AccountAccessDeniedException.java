package com.backendev.accountservice.exception;

public class AccountAccessDeniedException extends RuntimeException{

    public AccountAccessDeniedException(String message){
        super(message);
    }

    public AccountAccessDeniedException(String message, Throwable ex){
        super(message, ex);
    }
}
