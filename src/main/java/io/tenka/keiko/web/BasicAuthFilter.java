package io.tenka.keiko.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * HTTP Basic Auth gate, env-driven and opt-in. Mirrors the Python sibling's
 * {@code flashcards.middleware.BasicAuthMiddleware}.
 *
 * <p>Activates only when BOTH {@code BASIC_AUTH_USER} and {@code BASIC_AUTH_PASS}
 * are set in the environment (read via {@code keiko.basic-auth.user/password}
 * properties in application.yml). Half-set is treated as unset — defensive
 * against accidental blank-password mode if the operator forgets one var.
 *
 * <p>When enabled, every request must carry a matching {@code Authorization:
 * Basic ...} header or receives a 401 with {@code WWW-Authenticate: Basic
 * realm="keiko-engine"}.
 *
 * <p>Local dev: leave both env vars unset; the filter is a no-op pass-through.
 * Fly.io / production: set via {@code fly secrets set BASIC_AUTH_USER=jerry
 * BASIC_AUTH_PASS=...}.
 */
@Component
public class BasicAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(BasicAuthFilter.class);
    private static final String REALM = "keiko-engine";

    private final String user;
    private final String password;
    private final boolean enabled;

    public BasicAuthFilter(@Value("${keiko.basic-auth.user:}") String user,
                           @Value("${keiko.basic-auth.password:}") String password) {
        this.user = user == null ? "" : user;
        this.password = password == null ? "" : password;
        this.enabled = !this.user.isEmpty() && !this.password.isEmpty();
        if (this.enabled) {
            log.info("BasicAuth filter ENABLED — every request requires basic credentials.");
        } else {
            log.info("BasicAuth filter disabled (no env vars set). Local dev pass-through.");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!enabled || authorized(request)) {
            chain.doFilter(request, response);
            return;
        }
        challenge(response);
    }

    private boolean authorized(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Basic ")) return false;
        String decoded;
        try {
            decoded = new String(
                    Base64.getDecoder().decode(header.substring(6).trim()),
                    StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return false;  // malformed base64
        }
        int sep = decoded.indexOf(':');
        if (sep < 0) return false;
        String suppliedUser = decoded.substring(0, sep);
        String suppliedPass = decoded.substring(sep + 1);
        // Timing-safe comparison — don't leak which half matched via short-circuit.
        boolean userOk = MessageDigest.isEqual(
                suppliedUser.getBytes(StandardCharsets.UTF_8),
                this.user.getBytes(StandardCharsets.UTF_8));
        boolean passOk = MessageDigest.isEqual(
                suppliedPass.getBytes(StandardCharsets.UTF_8),
                this.password.getBytes(StandardCharsets.UTF_8));
        return userOk && passOk;
    }

    private void challenge(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Basic realm=\"" + REALM + "\"");
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write("Authentication required.");
    }

    /** Test-only accessor so unit tests can confirm config without sending a request. */
    public boolean isEnabled() {
        return enabled;
    }
}
