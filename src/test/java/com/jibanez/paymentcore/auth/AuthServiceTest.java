package com.jibanez.paymentcore.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jibanez.paymentcore.auth.dto.AuthResponse;
import com.jibanez.paymentcore.auth.dto.LoginRequest;
import com.jibanez.paymentcore.auth.dto.RegisterRequest;
import com.jibanez.paymentcore.exception.BusinessExceptions;
import com.jibanez.paymentcore.exception.BusinessExceptions.UsernameTakenException;
import com.jibanez.paymentcore.user.ApplicationUser;
import com.jibanez.paymentcore.user.UserRepository;
import com.jibanez.paymentcore.user.UserRole;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    JwtService jwtService;

    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void register_returnsTokenForNewUser() {
        when(userRepository.existsByUsername("jose")).thenReturn(false);
        when(userRepository.save(any(ApplicationUser.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generate(any(ApplicationUser.class))).thenReturn("token-abc");

        AuthResponse response = service.register(new RegisterRequest("jose", "secreto123"));

        assertThat(response.token()).isEqualTo("token-abc");
        assertThat(response.username()).isEqualTo("jose");
        assertThat(response.role()).isEqualTo(UserRole.USER);
        verify(userRepository).save(any(ApplicationUser.class));
    }

    @Test
    void register_throwsWhenUsernameTaken() {
        when(userRepository.existsByUsername("jose")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("jose", "secreto123")))
                .isInstanceOf(UsernameTakenException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_returnsTokenOnValidCredentials() {
        ApplicationUser user = new ApplicationUser("jose",
                passwordEncoder.encode("secreto123"), UserRole.USER);
        when(userRepository.findByUsername("jose")).thenReturn(Optional.of(user));
        when(jwtService.generate(user)).thenReturn("token-abc");

        AuthResponse response = service.login(new LoginRequest("jose", "secreto123"));

        assertThat(response.token()).isEqualTo("token-abc");
    }

    @Test
    void login_throwsOnWrongPassword() {
        ApplicationUser user = new ApplicationUser("jose",
                passwordEncoder.encode("secreto123"), UserRole.USER);
        when(userRepository.findByUsername("jose")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(new LoginRequest("jose", "incorrecta")))
                .isInstanceOf(BusinessExceptions.InvalidCredentialsException.class);
    }

    @Test
    void login_throwsWhenUserDoesNotExist() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("ghost", "secreto123")))
                .isInstanceOf(BusinessExceptions.InvalidCredentialsException.class);
    }
}