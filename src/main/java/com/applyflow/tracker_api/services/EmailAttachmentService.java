package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.services.storage.CvStorageFactory;
import com.applyflow.tracker_api.services.storage.CvStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailAttachmentService {

    private final CvStorageFactory storageFactory;

    public void attachCvFromUrl(MimeMessageHelper helper, String cvUrl, String attachmentFilename) {
        // 1. Route the incoming dynamic SaaS URL to the correct strategy.
        // Let IllegalArgumentException bubble up as-is: it already carries a
        // clear, user-facing message (e.g. "This doesn't look like a public
        // Google Drive share link...") that we don't want to lose.
        CvStorageService storageService = storageFactory.getServiceForUrl(cvUrl);

        // 2. Download raw file bytes
        byte[] fileBytes = storageService.downloadFile(cvUrl);

        // 3. Attach to the email MIME message
        try {
            helper.addAttachment(attachmentFilename, new ByteArrayResource(fileBytes));
        } catch (MessagingException e) {
            log.error("Failed to attach CV bytes to MIME message. filename={}, url={}", attachmentFilename, cvUrl, e);
            throw new RuntimeException(
                    "Failed to attach CV \"" + attachmentFilename + "\" to the email: " + e.getMessage(), e);
        }
    }
}