package com.puzzlezen.auth.controller;

import com.puzzlezen.auth.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** POST /api/auth/register */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(
            authService.register(req.getUsername(), req.getEmail(), req.getPassword())
        );
    }

    /** POST /api/auth/login */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(
            authService.login(req.getUsername(), req.getPassword())
        );
    }

    // ─── DTOs ────────────────────────────────────────────────────

    @Data
    public static class RegisterRequest {
        @NotBlank @Size(min = 3, max = 30)
        private String username;

        @NotBlank @Email
        private String email;

        @NotBlank @Size(min = 8)
        private String password;
    }

    @Data
    public static class LoginRequest {
        @NotBlank private String username;
        @NotBlank private String password;
    }
}
