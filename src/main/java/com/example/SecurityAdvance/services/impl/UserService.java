package com.example.SecurityAdvance.services.impl;

import com.example.SecurityAdvance.entities.User;
import com.example.SecurityAdvance.repositories.UserRepository;
import com.example.SecurityAdvance.services.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {
    private final UserRepository userRepository;

    @Override
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}
