package com.applyflow.tracker_api.services.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@Service
public class SupabaseStorageService implements CvStorageService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public byte[] downloadFile(String fileUrl) {
        // If your Supabase items are public, a simple direct GET works perfectly:
        ResponseEntity<byte[]> response = restTemplate.exchange(
                fileUrl, HttpMethod.GET, null, byte[].class);
        return response.getBody();
    }

    @Override
    public boolean supports(String fileUrl) {
        // Identifies URLs coming from your Supabase project storage domain
        return fileUrl != null && fileUrl.contains("supabase.co");
    }
}