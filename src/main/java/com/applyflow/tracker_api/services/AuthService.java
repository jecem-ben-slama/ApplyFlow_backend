package com.applyflow.tracker_api.services;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 1. Invalidate the HttpSession + clear SecurityContext
        // (this is what actually removes the session server-side —
        // the JSESSIONID cookie the browser holds becomes useless after this)
        new SecurityContextLogoutHandler().logout(request, response, auth);

        // 2. Defensive: invalidate manually too, in case a session still exists
        // (SecurityContextLogoutHandler already does this, but explicit is safer
        // if session creation policy or filter order ever changes)
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // 3. Expire the session cookie in the response so the browser drops it
        // immediately rather than waiting for it to become stale
        Cookie cookie = new Cookie("JSESSIONID", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}