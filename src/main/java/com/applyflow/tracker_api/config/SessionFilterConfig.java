package com.applyflow.tracker_api.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.Session;
import org.springframework.session.web.http.SessionRepositoryFilter;

import java.util.Set;

@Configuration
public class SessionFilterConfig {

    @Bean
    public FilterRegistrationBean<SessionRepositoryFilter<? extends Session>> sessionRepositoryFilterRegistration(
            SessionRepositoryFilter<? extends Session> filter) {

        FilterRegistrationBean<SessionRepositoryFilter<? extends Session>> registration =
                new FilterRegistrationBean<>(filter);

        registration.setUrlPatterns(Set.of(
                "/",
                "/api/*",
                "/login/*",
                "/oauth2/*"
        ));

        return registration;
    }
}