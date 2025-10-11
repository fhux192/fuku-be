package com.fukusaku.be.services;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String to, String token) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        String link = "http://localhost:3000/verify-email?token=" + token;
        String content = "<html><body>"
                + "<h3>Welcome to Our Bank App!</h3>"
                + "<p>Thank you for registering. Please click the link below to activate your account:</p>"
                + "<a href=\"" + link + "\">Activate My Account</a>"
                + "<p>If you did not register, please ignore this email.</p>"
                + "</body></html>";

        helper.setTo(to);
        helper.setSubject("Activate Your Account");
        helper.setText(content, true);

        mailSender.send(message);
    }
}