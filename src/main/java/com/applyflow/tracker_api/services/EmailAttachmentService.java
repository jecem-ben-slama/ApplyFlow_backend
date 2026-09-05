package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.services.storage.CvStorageFactory;
import com.applyflow.tracker_api.services.storage.CvStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamSource;
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
        // Google Drive share link..." or "This file is too large...") that
        // we don't want to lose.
        CvStorageService storageService = storageFactory.getServiceForUrl(cvUrl);

        // 2. Get a lazy, re-openable stream source. No network call happens
        // here — validation (link format, access, type, size) already ran
        // inside downloadFile(), and the actual bytes only start flowing
        // once addAttachment() below reads from it.
        InputStreamSource cvSource = storageService.downloadFile(cvUrl);

        // 3. Hand the source straight to JavaMail. Bytes stream from the
        // storage provider's connection into the MIME multipart writer as
        // the email is transmitted — no full-file byte[] is ever held in
        // memory here.
        try {
            helper.addAttachment(attachmentFilename, cvSource);
        } catch (MessagingException e) {
            log.error("Failed to attach CV stream to MIME message. filename={}, url={}", attachmentFilename, cvUrl, e);
            throw new RuntimeException(
                    "Failed to attach CV \"" + attachmentFilename + "\" to the email: " + e.getMessage(), e);
        }
    }
}