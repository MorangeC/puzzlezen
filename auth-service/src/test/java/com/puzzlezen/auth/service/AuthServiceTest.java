package com.puzzlezen.auth.service;

import com.puzzlezen.auth.model.User;
import com.puzzlezen.auth.repository.UserRepository;
import com.puzzlezen.auth.security.JwtUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — tests unitaires")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtils jwtUtils;
    @InjectMocks private AuthService authService;

    @Test
    @DisplayName("register crée un utilisateur et retourne un token")
    void shouldRegisterAndReturnToken() {
        when(userRepository.existsByUsername("morgan")).thenReturn(false);
        when(userRepository.existsByEmail("morgan@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(jwtUtils.generateToken("morgan", "PLAYER")).thenReturn("jwt-token");

        Map<String, String> result = authService.register("morgan", "morgan@test.com", "password123");

        assertThat(result)
                .containsKey("token")
                .containsEntry("token", "jwt-token")
                .containsEntry("username", "morgan");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register échoue si le username existe déjà")
    void shouldFailIfUsernameAlreadyExists() {
        when(userRepository.existsByUsername("morgan")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("morgan", "new@test.com", "password123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("déjà pris");
    }

    @Test
    @DisplayName("register échoue si l'email existe déjà")
    void shouldFailIfEmailAlreadyExists() {
        when(userRepository.existsByUsername("morgan")).thenReturn(false);
        when(userRepository.existsByEmail("morgan@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("morgan", "morgan@test.com", "password123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("déjà utilisé");
    }

    @Test
    @DisplayName("login retourne un token si les identifiants sont corrects")
    void shouldLoginWithValidCredentials() {
        User user = User.builder()
                .username("morgan").password("hashed").role(User.Role.PLAYER).build();

        when(userRepository.findByUsername("morgan")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtUtils.generateToken("morgan", "PLAYER")).thenReturn("jwt-token");

        Map<String, String> result = authService.login("morgan", "password123");

        assertThat(result).containsEntry("token", "jwt-token");

    }

    @Test
    @DisplayName("login échoue si l'utilisateur est introuvable")
    void shouldFailLoginIfUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("unknown", "pass"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Identifiants incorrects");
    }

    @Test
    @DisplayName("login échoue si le mot de passe est incorrect")
    void shouldFailLoginIfWrongPassword() {
        User user = User.builder()
                .username("morgan").password("hashed").role(User.Role.PLAYER).build();
        when(userRepository.findByUsername("morgan")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("morgan", "wrongpass"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Identifiants incorrects");
    }
}
