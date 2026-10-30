package com.editflowimg.backend.service;

import com.editflowimg.backend.dto.auth.AuthResponse;
import com.editflowimg.backend.dto.auth.LoginRequest;
import com.editflowimg.backend.dto.auth.RegisterRequest;
import com.editflowimg.backend.entity.UserEntity;
import com.editflowimg.backend.exception.AppException;
import com.editflowimg.backend.repository.UserRepository;
import com.editflowimg.backend.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException("E-mail já cadastrado", HttpStatus.CONFLICT);
        }

        UserEntity user = new UserEntity();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        UserEntity saved = userRepository.save(user);
        String token = jwtUtil.generate(saved.getId());

        return new AuthResponse(saved.getId(), saved.getName(), saved.getEmail(), token);
    }

    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException("E-mail ou senha inválidos", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), user.passwordHashValue())) {
            throw new AppException("E-mail ou senha inválidos", HttpStatus.UNAUTHORIZED);
        }

        String token = jwtUtil.generate(user.getId());

        return new AuthResponse(user.getId(), user.getName(), user.getEmail(), token);
    }
}
