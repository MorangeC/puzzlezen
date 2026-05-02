package com.puzzlezen.auth.service;

import com.puzzlezen.auth.model.User;
import com.puzzlezen.auth.repository.UserRepository;
import com.puzzlezen.auth.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public Map<String, String> register(String username, String email, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Ce nom d'utilisateur est déjà pris.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Cet email est déjà utilisé.");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .build();

        userRepository.save(user);
        String token = jwtUtils.generateToken(username, user.getRole().name());
        return Map.of("token", token, "username", username);
    }

    public Map<String, String> login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Identifiants incorrects."));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("Identifiants incorrects.");
        }

        String token = jwtUtils.generateToken(username, user.getRole().name());
        return Map.of("token", token, "username", username);
    }
}
