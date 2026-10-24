package org.entrypulse.entrypulse.service;

import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.dto.auth.AuthResponse;
import org.entrypulse.entrypulse.dto.auth.LoginRequest;
import org.entrypulse.entrypulse.dto.auth.RegisterRequest;
import org.entrypulse.entrypulse.model.User;
import org.entrypulse.entrypulse.repository.UserRepository;
import org.entrypulse.entrypulse.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(User.Role.ORGANISER);

        User savedUser = userRepository.save(user);

        return new AuthResponse(
                jwtService.generateToken(savedUser),
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getRole().name()
        );
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        boolean passwordMatches = passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        );

        if (!passwordMatches) {
            throw new RuntimeException("Invalid email or password");
        }

        return new AuthResponse(
                jwtService.generateToken(user),
                user.getId(),
                user.getName(),
                user.getRole().name()
        );
    }


    public AuthResponse registerSecurity(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(request.getPhoneNumber());

        // Security accounts must be assigned by the backend.
        user.setRole(User.Role.SECURITY);

        User savedUser = userRepository.save(user);

        return new AuthResponse(
                jwtService.generateToken(savedUser),
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getRole().name()
        );
    }

}