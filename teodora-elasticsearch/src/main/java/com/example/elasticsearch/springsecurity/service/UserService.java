package com.example.elasticsearch.springsecurity.service;

import org.springframework.security.core.userdetails.UserDetailsService;
import com.example.elasticsearch.springsecurity.model.User;

import com.example.elasticsearch.springsecurity.web.dto.UserRegistrationDto;

public interface UserService extends UserDetailsService {

    public User findByEmail(String email);

    public User save(UserRegistrationDto registration);
}
