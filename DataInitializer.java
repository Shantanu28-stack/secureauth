package com.shantanu.secureauth.config;

import com.shantanu.secureauth.entity.Role;
import com.shantanu.secureauth.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            roleRepository.findByName("USER")
                    .orElseGet(() -> roleRepository.save(new Role("USER")));

            roleRepository.findByName("ADMIN")
                    .orElseGet(() -> roleRepository.save(new Role("ADMIN")));
        };
    }
}

