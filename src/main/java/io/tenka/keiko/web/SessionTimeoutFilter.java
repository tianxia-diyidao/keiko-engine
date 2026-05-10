package io.tenka.keiko.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 180-minute idle-session timeout for BasicAuth-protected deployments.
 *
 * <p>HTTP Basic Auth is sticky in browsers: once the user enters credentials,
 * the browser keeps replaying the {@code Authorization} header forever. There
 * is no real "log out". This filter forces a re-prompt after a configurable
 * idle window by:
 *
 * <ol>
 *   <li>Tracking {@code lastSeenMillis} on the {@link HttpSession}.</li>
 *   <li>When a request arrives more than {@code idleMinutes} after the last
 *       request, invalidating the session and returning 401 with a
 *       <em>rotated</em> {@code WWW-Authenticate} realm (epoch-suffixed).</li>
 * </ol>
 *
 * <p>The realm rotation is the trick — browsers cache credentials per
 * (origin, realm) tuple, so changing the realm string forces a fresh prompt
 * even though the cached creds are still being sent on the request.
 *
 * <p>Only active when BasicAuth itself is enabled. With BasicAuth disabled
 * (local dev), this filter is a no-op pass-through — there is nothing to
 * "time out" when there are no credentials in the first place.
 *
 * <p>Mirrors the Python sibling's {@code SESSION_IDLE_TIMEOUT_MINUTES}
 * (default 180) — set via {@code SESSION_IDLE_TIMEOUT_MINUTES} env var or
 * {@code keiko.session-idle-timeout-minutes} property.
 */
public class SessionTimeoutFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SessionTimeoutFilter.class);
    private static final String LAST_SEEN_ATTR = "keiko.lastSeenMillis";

    private final long idleTimeoutMillis;
    private final boolean enabled;

    public SessionTimeoutFilter(int idleMinutes, boolean basicAuthEnabled) {
        this.idleTimeoutMillis = idleMinutes * 60_000L;
        this.enabled = basicAuthEnabled;
        if (this.enabled) {
            log.info("SessionTimeout filter ENABLED — {} min idle window.", idleMinutes);
        } else {
            log.info("SessionTimeout filter disabled (BasicAuth off, nothing to time out).");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }
        HttpSession session = request.getSession(true);
        Long lastSeen = (Long) session.getAttribute(LAST_SEEN_ATTR);
        long now = System.currentTimeMillis();
        if (lastSeen != null && (now - lastSeen) > idleTimeoutMillis) {
            long idleMin = (now - lastSeen) / 60_000L;
            log.info("Session {} idle for {} min — invalidating, forcing re-auth.",
                    session.getId(), idleMin);
            session.invalidate();
            // Rotate the realm string so the browser drops its cached creds and re-prompts.
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate",
                    "Basic realm=\"keiko-engine (re-auth " + now + ")\"");
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Session timed out — please re-authenticate.");
            return;
        }
        session.setAttribute(LAST_SEEN_ATTR, now);
        chain.doFilter(request, response);
    }

    /** Test-only accessor. */
    public boolean isEnabled() {
        return enabled;
    }

    /** Test-only accessor. */
    public long idleTimeoutMillis() {
        return idleTimeoutMillis;
    }
}
