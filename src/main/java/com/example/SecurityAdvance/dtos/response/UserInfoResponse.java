package com.example.SecurityAdvance.dtos.response;

import com.example.SecurityAdvance.enums.UserStatus;
import lombok.*;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserInfoResponse {
    private Long id;
    private String username;
    private String email;
    private Set<String> roles;
    private UserStatus status;
}
