package com.assignment.orderprocessing.service;

import com.assignment.orderprocessing.domain.Role;
import com.assignment.orderprocessing.domain.User;
import com.assignment.orderprocessing.dto.AuthResponse;
import com.assignment.orderprocessing.dto.LoginRequest;
import com.assignment.orderprocessing.dto.RegisterRequest;
import com.assignment.orderprocessing.repository.UserRepository;
import com.assignment.orderprocessing.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already taken: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered: " + request.email());
        }
        Role role = request.role() != null ? request.role() : Role.CUSTOMER;
        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .email(request.email())
                .fullName(request.fullName())
                .mobileNumber(request.mobileNumber())
                .role(role)
                .enabled(true)
                .build();
        userRepository.save(user);
        String token = jwtService.generateToken(user.getUsername(), role.name());
        return new AuthResponse(token, "Bearer", user.getUsername(), role.name());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, "Bearer", user.getUsername(), user.getRole().name());
    }
}
