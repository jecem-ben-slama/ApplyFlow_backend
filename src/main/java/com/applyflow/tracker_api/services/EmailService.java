package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.models.ApplicationStatus;
import com.applyflow.tracker_api.models.CvVariant;
import com.applyflow.tracker_api.repositories.CvVariantRepository;
import com.applyflow.tracker_api.services.storage.CvStorageFactory;
import com.applyflow.tracker_api.services.storage.CvStorageService;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final CvStorageFactory storageFactory;
    private final CvVariantRepository cvVariantRepository;
    private final ApplicationService applicationService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.api.base-url}")
    private String baseUrl;

    private static final String GMAIL_SEND_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

    public void sendApplicationEmail(String userEmail, String accessToken, String recipientEmail, String subject,
            String body, Long cvVariantId, Long applicationId) {

        try {
            // Build the MIME message exactly as before — no SMTP session needed anymore
            Session session = Session.getDefaultInstance(new Properties());
            MimeMessage mimeMessage = new MimeMessage(session);
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(new InternetAddress(userEmail));
            helper.setTo(recipientEmail);
            helper.setSubject(subject);

            // Inject the open-tracking pixel right before send, then send as HTML
            String htmlBody = buildHtmlBodyWithTracking(body, applicationId);
            helper.setText(htmlBody, true);

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
                    byte[] fileBytes = storageService.downloadFile(cvUrl);

                    String filename = (originalDbName != null && !originalDbName.isBlank())
                            ? originalDbName
                            : "CV_" + subject.replaceAll("\\s+", "_") + ".pdf";

                    helper.addAttachment(filename, new ByteArrayResource(fileBytes));
                    log.info("Successfully attached CV from URL: {} for variant ID: {}", cvUrl, cvVariantId);
                }
            }

            // Convert the MimeMessage into the raw base64url format Gmail API expects
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            mimeMessage.writeTo(buffer);
            String encodedMessage = Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.toByteArray());

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = Map.of("raw", encodedMessage);
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    GMAIL_SEND_URL, HttpMethod.POST, requestEntity, String.class);

            log.info("Email successfully dispatched via Gmail API from {} to {}. Response: {}",
                    userEmail, recipientEmail, response.getStatusCode());

            if (applicationId != null) {
                try {
                    applicationService.recordSystemStatusEvent(
                            applicationId, ApplicationStatus.SENT, "Email dispatched via Gmail API");
                } catch (Exception statusEx) {
                    // never let a status-tracking failure roll back or mask a successful send
                    log.warn("Email sent successfully but failed to record SENT status for application {}: {}",
                            applicationId, statusEx.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Gmail API outbound transmission failed.", e);
            throw new RuntimeException("Failed to dispatch email via Gmail API: " + e.getMessage(), e);
        }
    }

    private String buildHtmlBodyWithTracking(String plainOrHtmlBody, Long applicationId) {
        // Preserve line breaks if the incoming body is plain text
        String htmlSafeBody = plainOrHtmlBody.contains("<")
                ? plainOrHtmlBody
                : plainOrHtmlBody.replace("\n", "<br/>");

        if (applicationId == null) {
            return htmlSafeBody;
        }

        String trackingPixel = String.format(
                "<img src=\"%s/api/track/open/%d\" width=\"1\" height=\"1\" style=\"display:none\" alt=\"\" />",
                baseUrl, applicationId);

        return htmlSafeBody + trackingPixel;
    }
}