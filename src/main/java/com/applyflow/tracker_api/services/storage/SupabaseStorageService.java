package com.applyflow.tracker_api.services.storage;

import org.springframework.core.io.InputStreamSource;
import org.springframework.stereotype.Service;

/**
 * Supabase storage support isn't implemented yet. validateFile() and
 * downloadFile() both throw IllegalArgumentException — the same exception
 * type every other CvStorageService implementation uses for a rejected
 * link — rather than an unchecked UnsupportedOperationException. Callers
 * (e.g. CvVariantService) rely on the interface's documented contract to
 * treat any implementation interchangeably; throwing a different exception
 * type here would violate that contract (Liskov substitution) and surface
 * as an unhandled 500 instead of the same clean validation error every
 * other unsupported/invalid link produces.
 */
@Service
public class SupabaseStorageService implements CvStorageService {

    @Override
    public InputStreamSource downloadFile(String fileUrl) {
        // Not implemented: no metadata/type/size check exists yet for
        // Supabase, so we refuse rather than fetch and attach an
        // unvalidated file. validateFile() should already have blocked
        // this earlier (at add/edit time), but downloadFile() enforces the
        // same rule independently in case it's ever called directly.
        throw notYetSupportedException();
    }

    @Override
    public boolean supports(String fileUrl) {
        return fileUrl != null && fileUrl.contains("supabase.co");
    }

    @Override
    public void validateFile(String fileUrl) {
        throw notYetSupportedException();
    }

    private IllegalArgumentException notYetSupportedException() {
        return new IllegalArgumentException(
                "Supabase CV links aren't supported yet. "
                        + "Currently only Google Drive share links are supported.");
    }
}