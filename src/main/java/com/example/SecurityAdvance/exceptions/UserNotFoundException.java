package com.example.SecurityAdvance.exceptions;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends AppException {
    public UserNotFoundException() {
        super("err.user.not_found", HttpStatus.NOT_FOUND);
    }
}
