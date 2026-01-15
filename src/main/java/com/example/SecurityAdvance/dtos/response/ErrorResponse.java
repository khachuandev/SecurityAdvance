package com.example.SecurityAdvance.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private int code;
    private String message;
    private Map<String, String> errors;
}
