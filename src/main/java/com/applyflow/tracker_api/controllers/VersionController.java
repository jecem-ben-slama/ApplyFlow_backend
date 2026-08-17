package com.applyflow.tracker_api.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class VersionController {

    // Hardcode/bump this manually each time you want to verify a deploy.
    // Cheap, unambiguous, no DB or auth dependency.
    private static final String BUILD_TAG = "v2-gmail-api-2026-08-17";

    @GetMapping("/api/version")
    public Map<String, Object> version() {
        return Map.of(
                "buildTag", BUILD_TAG,
                "serverTimeUtc", Instant.now().toString());
    }
}