/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.elasticsearch.springsecurity.service;

import com.example.elasticsearch.springsecurity.model.Role;
import com.example.elasticsearch.springsecurity.model.User;
import com.example.elasticsearch.springsecurity.repository.RoleRepository;
import com.example.elasticsearch.springsecurity.repository.UserRepository;
import java.util.Arrays;
import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 *
 * @author teodora
 */
@Component
public class SetupDataLoader implements
        ApplicationListener<ContextRefreshedEvent> {

    boolean alreadySetup = false;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event) {

        if (userRepository.findByEmail("teodoranovkovic@hotmail.com") != null) {
            return;
        }
        createRoleIfNotFound("ROLE_ADMIN");
        Role adminRole = roleRepository.findByName("ROLE_ADMIN");
        User user = new User();
        user.setFirstName("Teodora");
        user.setLastName("Novkovic");
        user.setPassword(passwordEncoder.encode("test"));
        user.setEmail("teodoranovkovic@hotmail.com");
        user.setRoles(Arrays.asList(adminRole));
        userRepository.save(user);
        alreadySetup = true;
    }

    @Transactional
    Role createRoleIfNotFound(
            String name) {

        Role role = roleRepository.findByName(name);
        if (role == null) {
            role = new Role(name);
            roleRepository.save(role);
        }
        return role;
    }
}
