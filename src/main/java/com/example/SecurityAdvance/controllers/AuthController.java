package com.example.SecurityAdvance.controllers;

import com.example.SecurityAdvance.dtos.request.UserRegisterRequest;
import com.example.SecurityAdvance.dtos.response.ApiRes;
import com.example.SecurityAdvance.dtos.response.UserResponse;
import com.example.SecurityAdvance.events.RegistrationEvent;
import com.example.SecurityAdvance.services.IAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/auth")
public class AuthController {
    private final IAuthService authService;
    private final ApplicationEventPublisher eventPublisher;

    @PostMapping("/register")
    public ResponseEntity<ApiRes<UserResponse>> register(@Valid @RequestBody UserRegisterRequest request,
                                           HttpServletRequest httpRequest) {
        UserResponse newUser = authService.register(request);
        String appUrl = getApplicationUrl(httpRequest);
        eventPublisher.publishEvent(new RegistrationEvent(newUser.getId(), appUrl));
        return ResponseEntity.ok(ApiRes.created(newUser));
    }

    private String getApplicationUrl(HttpServletRequest request) {
        return request.getScheme() + "://" +
                request.getServerName() + ":" +
                request.getServerPort();
    }
}
