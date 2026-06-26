package com.applyflow.tracker_api.services.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.net.URI;

@Service
public class SupabaseStorageService implements CvStorageService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public byte[] downloadFile(String fileUrl) {
        URI uri = URI.create(fileUrl); // treats the URL as already-encoded
        ResponseEntity<byte[]> response = restTemplate.exchange(
                uri, HttpMethod.GET, null, byte[].class);
        return response.getBody();
    }

    @Override
    public boolean supports(String fileUrl) {
        return fileUrl != null && fileUrl.contains("supabase.co");
    }
}