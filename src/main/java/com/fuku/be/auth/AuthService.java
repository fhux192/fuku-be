package com.fuku.be.auth;

import com.fuku.be.dto.LoginResponse;
import com.fuku.be.user.User;
import com.fuku.be.user.UserRepository;
import com.fuku.be.security.JwtUtil;
import com.fuku.be.services.EmailService;

import jakarta.mail.MessagingException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtUtil = jwtUtil;
    }

    public void register(RegistrationRequest request) throws MessagingException {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("Email already in use");
        }
        if( !request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalStateException("Passwords do not match");
        }
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(false);
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        userRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(), token);
    }

    public void verifyUser(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new IllegalStateException("Invalid verification token"));
        if (user.isEnabled()){
            throw new IllegalStateException("Account already activated");
        }
        user.setEnabled(true);
        user.setVerificationToken(null);
        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new IllegalStateException("Account is not activated. Please check your email.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalStateException("Invalid email or password");
        }

        String jwtToken = jwtUtil.generateToken(user);
        return new LoginResponse(jwtToken);
    }

    public void forgotPassword(String email) throws MessagingException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found with this email"));

        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(15));

        userRepository.save(user);

        emailService.sendResetPasswordEmail(user.getEmail(), token);
        System.out.println("Reset Token for " + email + ": " + token);
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new IllegalStateException("Invalid reset token"));

        if (user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Token has expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);

        userRepository.save(user);
    }


}