package com.backendev.accountservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountAlreadyExistsException.class)
    public ResponseEntity<HttpErrorResponse> handleUserExistsException(AccountAlreadyExistsException exception){
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(HttpStatus.CONFLICT, exception.getMessage(), "The account already exists.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(httpErrorResponse);
    }

    @ExceptionHandler(JwtAuthenticationException.class)
    public ResponseEntity<HttpErrorResponse> handleJwtAuthenticationException(JwtAuthenticationException exception){
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(HttpStatus.UNAUTHORIZED, exception.getMessage(), "JWT authentication failed.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(httpErrorResponse);
    }

    @ExceptionHandler(AccountAccessDeniedException.class)
    public ResponseEntity<HttpErrorResponse> handleAccountAccessDeniedException(AccountAccessDeniedException exception){
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(HttpStatus.FORBIDDEN, exception.getMessage(), "Account access denied.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(httpErrorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<HttpErrorResponse> handleGenericException(Exception exception) {
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getMessage(), "An unexpected error occurred.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(httpErrorResponse);
    }
}
