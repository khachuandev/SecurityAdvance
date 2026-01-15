package com.example.SecurityAdvance.services;

import com.example.SecurityAdvance.entities.User;

public interface IUserService {
    User findById(Long id);
}
