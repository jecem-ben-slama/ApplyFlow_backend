package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.models.CvVariant;
import com.applyflow.tracker_api.repositories.CvVariantRepository;
import com.applyflow.tracker_api.services.storage.CvStorageFactory;
import com.applyflow.tracker_api.services.storage.CvStorageService;
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final CvStorageFactory storageFactory;
    private final CvVariantRepository cvVariantRepository;

    public void sendApplicationEmail(String userEmail, String accessToken, String recipientEmail, String subject,
            String body, Long cvVariantId) {

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth.mechanisms", "XOAUTH2");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(userEmail, accessToken);
            }
        });

        try {
            MimeMessage mimeMessage = new MimeMessage(session);
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(new InternetAddress(userEmail));
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(body, false);

            if (cvVariantId != null) {
                CvVariant variant = cvVariantRepository.findById(cvVariantId)
                        .orElseThrow(() -> new RuntimeException("CV Variant not found for ID: " + cvVariantId));

                String cvUrl = variant.getFileUrl();

                String originalDbName = variant.getName();
                if (originalDbName != null && !originalDbName.endsWith(".pdf")) {
                    originalDbName += ".pdf";
                }

                if (cvUrl != null && !cvUrl.isBlank()) {
                    CvStorageService storageService = storageFactory.getServiceForUrl(cvUrl);

                    // Simple, decoupled, strategy-based download call
                    byte[] fileBytes = storageService.downloadFile(cvUrl);

                    String filename = (originalDbName != null && !originalDbName.isBlank())
                            ? originalDbName
                            : "CV_" + subject.replaceAll("\\s+", "_") + ".pdf";

                    helper.addAttachment(filename, new ByteArrayResource(fileBytes));
                    log.info("Successfully attached CV from URL: {} for variant ID: {}", cvUrl, cvVariantId);
                }
            }

            Transport.send(mimeMessage);
            log.info("Email successfully dispatched from user account: {} to recipient: {}", userEmail, recipientEmail);

        } catch (Exception e) {
            log.error("SMTP outbound transmission failed during dynamic session execution.", e);
            throw new RuntimeException("Failed to dispatch dynamic email via user OAuth2 ecosystem: " + e.getMessage(),
                    e);
        }
    }
}