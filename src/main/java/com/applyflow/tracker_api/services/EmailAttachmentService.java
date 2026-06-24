package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.services.storage.CvStorageFactory;
import com.applyflow.tracker_api.services.storage.CvStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailAttachmentService {

    private final CvStorageFactory storageFactory;

    public void attachCvFromUrl(MimeMessageHelper helper, String cvUrl, String attachmentFilename) {
        try {
            // 1. Route the incoming dynamic SaaS URL to the correct strategy
            CvStorageService storageService = storageFactory.getServiceForUrl(cvUrl);

            // 2. Download raw file bytes seamlessly
            byte[] fileBytes = storageService.downloadFile(cvUrl);

            // 3. Attach to the email MIME message
            helper.addAttachment(attachmentFilename, new ByteArrayResource(fileBytes));

        } catch (Exception e) {
            throw new RuntimeException("Failed to attach CV from URL: " + cvUrl, e);
        }
    }
}