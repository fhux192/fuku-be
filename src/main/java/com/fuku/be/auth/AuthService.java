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
            throw new IllegalStateException("メールアドレスは既に使用されています (Email already in use)");
        }

        // Check confirm password
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalStateException("パスワードが一致しません (Passwords do not match)");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(false);

        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);

        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), user.getName(), token);
    }

    public void verifyUser(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new IllegalStateException("無効な認証トークンです (Invalid verification token)"));

        if (user.isEnabled()) {
            throw new IllegalStateException("アカウントは既に有効化されています (Account already activated)");
        }

        user.setEnabled(true);
        user.setVerificationToken(null);
        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("メールアドレスまたはパスワードが間違っています (Invalid email or password)"));

        if (!user.isEnabled()) {
            throw new IllegalStateException("アカウントが有効化されていません。メールを確認してください。(Account is not activated. Please check your email.)");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalStateException("メールアドレスまたはパスワードが間違っています (Invalid email or password)");
        }

        String jwtToken = jwtUtil.generateToken(user);
        return new LoginResponse(jwtToken);
    }

    public void forgotPassword(String email) throws MessagingException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("このメールアドレスのユーザーは見つかりません (User not found with this email)"));

        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(15));

        userRepository.save(user);

        emailService.sendResetPasswordEmail(user.getEmail(), token);
    }

    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new IllegalStateException("無効なリセットトークンです (Invalid reset token)"));

        if (user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("トークンの有効期限が切れています (Token has expired)");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("新しいパスワードを現在のパスワードと同じにすることはできません (New password cannot be the same as the current password)");
        }

        user.setPassword(passwordEncoder.encode(newPassword));

        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);

        userRepository.save(user);
    }
}