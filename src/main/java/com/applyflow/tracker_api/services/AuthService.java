package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.config.GuestPrincipal;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    // Same repository Spring Security uses internally for session-based auth.
    // We instantiate it directly since this endpoint isn't behind
    // AbstractAuthenticationProcessingFilter, so nothing else calls
    // saveContext() for us automatically.
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    /**
     * Manually authenticates a guest and persists the context to the session.
     * Also stashes the guest's user id as a session attribute so
     * OAuth2SuccessHandler can find and discard it later if this guest goes
     * on to log in with a real Google account.
     */
    public void establishGuestSession(HttpServletRequest request, HttpServletResponse response, Long guestUserId) {
        GuestPrincipal principal = new GuestPrincipal(guestUserId);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_GUEST")));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        request.getSession(true).setAttribute("guestUserId", guestUserId);
    }

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