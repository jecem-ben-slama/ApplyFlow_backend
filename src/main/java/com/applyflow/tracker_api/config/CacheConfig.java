package com.applyflow.tracker_api.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // This supplies the missing Bean your main application context is looking for
        return new ConcurrentMapCacheManager("buckets");
    }
}
