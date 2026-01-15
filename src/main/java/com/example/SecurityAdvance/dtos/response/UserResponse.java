package com.example.SecurityAdvance.dtos.response;

import com.example.SecurityAdvance.enums.UserStatus;
import lombok.*;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private UserStatus status;
    private Instant createdAt;
}
