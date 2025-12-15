package com.fuku.be.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.backend.url}")
    private String backendUrl;

    public EmailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    /**
     * Sends an account verification email asynchronously.
     * @param to The recipient's email address
     * @param name The user's name
     * @param token The verification token
     */
    public void sendVerificationEmail(String to, String name, String token) throws MessagingException {
        String link = frontendUrl + "/verify-email?token=" + token;

        // Prepare context variables for the Thymeleaf template
        Context context = new Context();
        context.setVariables(Map.of(
                "name", name,
                "link", link
        ));

        // Render the HTML content
        String htmlContent = templateEngine.process("verification-email", context);

        sendHtmlEmail(to, "Welcome to Fuku Japanese - Verify Account", htmlContent);
    }

    /**
     * Sends a password reset email asynchronously.
     * @param to The recipient's email address
     * @param token The reset token
     */
    public void sendResetPasswordEmail(String to, String token) throws MessagingException {
        String link = frontendUrl + "/reset-password?token=" + token;

        Context context = new Context();
        context.setVariable("link", link);

        String htmlContent = templateEngine.process("reset-password", context);

        sendHtmlEmail(to, "Password Reset Request", htmlContent);
    }

    /**
     * Helper method to send HTML emails.
     */
    private void sendHtmlEmail(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true); // Set to 'true' to enable HTML processing

        mailSender.send(message);
    }
}