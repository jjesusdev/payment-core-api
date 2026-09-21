package com.jibanez.paymentcore.auth;

import com.jibanez.paymentcore.auth.dto.AuthResponse;
import com.jibanez.paymentcore.auth.dto.LoginRequest;
import com.jibanez.paymentcore.auth.dto.RegisterRequest;
import com.jibanez.paymentcore.exception.BusinessExceptions;
import com.jibanez.paymentcore.user.ApplicationUser;
import com.jibanez.paymentcore.user.UserRepository;
import com.jibanez.paymentcore.user.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessExceptions.UsernameTakenException();
        }
        ApplicationUser user = new ApplicationUser(
                request.username(),
                passwordEncoder.encode(request.password()),
                UserRole.USER);
        ApplicationUser saved = userRepository.save(user);
        return new AuthResponse(jwtService.generate(saved), saved.getUsername(), saved.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        ApplicationUser user = userRepository.findByUsername(request.username())
                .orElseThrow(BusinessExceptions.InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessExceptions.InvalidCredentialsException();
        }
        return new AuthResponse(jwtService.generate(user), user.getUsername(), user.getRole());
    }
}