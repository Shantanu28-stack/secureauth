package com.shantanu.secureauth.service;

import com.shantanu.secureauth.auth.AuthResponse;
import com.shantanu.secureauth.auth.LoginRequest;
import com.shantanu.secureauth.dto.RegisterRequest;
import com.shantanu.secureauth.entity.Role;
import com.shantanu.secureauth.entity.User;
import com.shantanu.secureauth.exception.BadRequestException;
import com.shantanu.secureauth.repository.RoleRepository;
import com.shantanu.secureauth.repository.UserRepository;
import com.shantanu.secureauth.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                        RoleRepository roleRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public void register(RegisterRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new BadRequestException("Email already registered");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new BadRequestException("USER role not found. Run DataInitializer."));

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of(userRole));

        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new BadRequestException("Account is disabled");
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Account is locked. Try again later.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleFailedLogin(user);
            throw new BadRequestException("Invalid email or password");
        }

        // successful login: reset failure count
        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token);
    }

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
        }

        userRepository.save(user);
    }
}
