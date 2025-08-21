package com.backendev.transactionservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(JwtAuthenticationException.class)
    public ResponseEntity<HttpErrorResponse> handleJwtAuthenticationException(JwtAuthenticationException exception){
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(HttpStatus.UNAUTHORIZED, exception.getMessage(), "JWT authentication failed.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(httpErrorResponse);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<HttpErrorResponse> handleInsufficientFundsException(InsufficientFundsException exception){
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(HttpStatus.CONFLICT, exception.getMessage(), "Insufficient Balance.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(httpErrorResponse);
    }

    @ExceptionHandler(InvalidAccountException.class)
    public ResponseEntity<HttpErrorResponse> handleInvalidAccountException(InvalidAccountException exception){
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(HttpStatus.NOT_FOUND, exception.getMessage(), "Account not found. Invalid Account.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(httpErrorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<HttpErrorResponse> handleGenericException(Exception exception) {
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getMessage(), "An unexpected error occurred.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(httpErrorResponse);
    }
}
