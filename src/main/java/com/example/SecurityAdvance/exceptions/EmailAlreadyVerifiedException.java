package com.example.SecurityAdvance.exceptions;

import org.springframework.http.HttpStatus;

public class EmailAlreadyVerifiedException extends AppException {
    public EmailAlreadyVerifiedException() {
        super("err.email.already_verified", HttpStatus.BAD_REQUEST);
    }
}
