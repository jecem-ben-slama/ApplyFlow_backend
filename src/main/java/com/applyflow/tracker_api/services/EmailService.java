package com.applyflow.tracker_api.services;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    /**
     * Instantiates an isolated, dynamic SMTP session at runtime using the sender's
     * specific Google OAuth2 access token.
     */
    public void sendApplicationEmailDynamic(String userEmail, String accessToken, String recipientEmail, String subject,
            String body) {

        // 1. Set runtime multi-tenant SMTP properties
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        // Force Jakarta Mail to use OAuth2 access tokens instead of standard passwords
        props.put("mail.smtp.auth.mechanisms", "XOAUTH2");

        // 2. Build an isolated Session context bypassing global system configurations
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                // For XOAUTH2: Username = user's email, Password = active access token
                return new PasswordAuthentication(userEmail, accessToken);
            }
        });

        try {
            // 3. Compile the structural MIME content framework
            MimeMessage mimeMessage = new MimeMessage(session);
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(new InternetAddress(userEmail));
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(body, false); // "false" parses content as plain text (no HTML)

            // 4. Fire the message over the network
            Transport.send(mimeMessage);
            log.info("Email successfully dispatched from user account: {} to recipient: {}", userEmail, recipientEmail);

        } catch (Exception e) {
            log.error("SMTP outbound transmission failed during dynamic session execution.", e);
            throw new RuntimeException("Failed to dispatch dynamic email via user OAuth2 ecosystem: " + e.getMessage(),
                    e);
        }
    }
}