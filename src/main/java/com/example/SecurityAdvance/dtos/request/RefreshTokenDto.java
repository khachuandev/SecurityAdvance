package com.example.SecurityAdvance.dtos.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefreshTokenDto {
    @NotBlank
    private String refreshToken;
}
