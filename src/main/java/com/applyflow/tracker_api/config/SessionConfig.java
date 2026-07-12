package com.applyflow.tracker_api.config;

import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SessionConfig {

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");
        serializer.setUseSecureCookie(true);
        serializer.setSameSite("None");
        serializer.setCookiePath("/");
        serializer.setCookieMaxAge(30 * 24 * 60 * 60); 
        return serializer;
    }
}