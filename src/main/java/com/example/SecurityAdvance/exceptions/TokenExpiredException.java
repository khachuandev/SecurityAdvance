package com.example.SecurityAdvance.exceptions;

import org.springframework.http.HttpStatus;

public class TokenExpiredException extends AppException {
    public TokenExpiredException() {
        super("err.token.expired", HttpStatus.GONE);
    }
}
