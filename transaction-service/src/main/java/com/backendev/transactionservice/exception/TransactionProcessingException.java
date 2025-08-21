package com.backendev.transactionservice.exception;

public class TransactionProcessingException extends RuntimeException{

    public TransactionProcessingException(String message){
        super(message);
    }

    public TransactionProcessingException(String message, Throwable ex){
        super(message, ex);
    }
}
