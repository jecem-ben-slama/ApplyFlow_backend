package com.applyflow.tracker_api.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.Serializable; // 1. Added serialization import
import java.util.Collection;
import java.util.Map;

// 2. Added "implements Serializable" to the class signature
public class CustomOAuth2User implements OAuth2User, Serializable {

    // 3. Explicitly defined a unique serialization identity marker version
    private static final long serialVersionUID = 1L;

    private final OAuth2User oAuth2User;
    private final Long id; // Your database User ID

    public CustomOAuth2User(OAuth2User oAuth2User, Long id) {
        this.oAuth2User = oAuth2User;
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oAuth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oAuth2User.getAuthorities();
    }

    @Override
    public String getName() {
        return oAuth2User.getAttribute("name");
    }

    public String getEmail() {
        return oAuth2User.getAttribute("email");
    }
}
