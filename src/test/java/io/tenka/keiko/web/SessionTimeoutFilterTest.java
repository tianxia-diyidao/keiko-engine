package io.tenka.keiko.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SessionTimeoutFilter — instantiates directly with controlled
 * idle window so we don't have to wait 180 minutes in real time.
 */
class SessionTimeoutFilterTest {

    private static final String LAST_SEEN_ATTR = "keiko.lastSeenMillis";

    private static FilterChain chainRecorder(AtomicBoolean reached) {
        return (req, resp) -> reached.set(true);
    }

    @Nested
    class DisabledMode {
        @Test
        void basicAuthOff_isPassThrough() throws Exception {
            SessionTimeoutFilter f = new SessionTimeoutFilter(180, false);
            assertThat(f.isEnabled()).isFalse();
            AtomicBoolean reached = new AtomicBoolean(false);
            f.doFilter(new MockHttpServletRequest("GET", "/"), new MockHttpServletResponse(),
                    chainRecorder(reached));
            assertThat(reached).isTrue();
        }

        @Test
        void basicAuthOff_doesNotTouchSession() throws Exception {
            // Important: when disabled, we shouldn't even create a session — that
            // would cost memory on every request in pure-passthrough deployments.
            SessionTimeoutFilter f = new SessionTimeoutFilter(180, false);
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/");
            f.doFilter(req, new MockHttpServletResponse(), chainRecorder(new AtomicBoolean()));
            assertThat(req.getSession(false)).isNull();
        }
    }

    @Nested
    class EnabledMode {
        @Test
        void freshSession_recordsLastSeenAndPassesThrough() throws Exception {
            SessionTimeoutFilter f = new SessionTimeoutFilter(180, true);
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/");
            req.setSession(new MockHttpSession());
            MockHttpServletResponse resp = new MockHttpServletResponse();
            AtomicBoolean reached = new AtomicBoolean(false);

            long before = System.currentTimeMillis();
            f.doFilter(req, resp, chainRecorder(reached));
            long after = System.currentTimeMillis();

            assertThat(reached).isTrue();
            assertThat(resp.getStatus()).isEqualTo(200);
            HttpSession session = req.getSession(false);
            assertThat(session).isNotNull();
            Long lastSeen = (Long) session.getAttribute(LAST_SEEN_ATTR);
            assertThat(lastSeen).isNotNull();
            assertThat(lastSeen).isBetween(before, after);
        }

        @Test
        void recentSession_updatesLastSeenAndPassesThrough() throws Exception {
            SessionTimeoutFilter f = new SessionTimeoutFilter(180, true);
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/");
            MockHttpSession session = new MockHttpSession();
            // Set lastSeen to 1 minute ago — well within the 180-min window.
            long oneMinAgo = System.currentTimeMillis() - 60_000L;
            session.setAttribute(LAST_SEEN_ATTR, oneMinAgo);
            req.setSession(session);
            MockHttpServletResponse resp = new MockHttpServletResponse();
            AtomicBoolean reached = new AtomicBoolean(false);

            f.doFilter(req, resp, chainRecorder(reached));

            assertThat(reached).isTrue();
            assertThat(resp.getStatus()).isEqualTo(200);
            Long lastSeen = (Long) req.getSession(false).getAttribute(LAST_SEEN_ATTR);
            assertThat(lastSeen).isGreaterThan(oneMinAgo);
        }

        @Test
        void expiredSession_returns401WithRotatedRealm() throws Exception {
            SessionTimeoutFilter f = new SessionTimeoutFilter(180, true);
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/");
            MockHttpSession session = new MockHttpSession();
            // Set lastSeen to 200 min ago — outside the 180-min window.
            long longAgo = System.currentTimeMillis() - (200L * 60_000L);
            session.setAttribute(LAST_SEEN_ATTR, longAgo);
            req.setSession(session);
            MockHttpServletResponse resp = new MockHttpServletResponse();
            AtomicBoolean reached = new AtomicBoolean(false);

            f.doFilter(req, resp, chainRecorder(reached));

            assertThat(reached).isFalse();
            assertThat(resp.getStatus()).isEqualTo(401);
            // Realm must be ROTATED (timestamp-suffixed) so browser drops cached creds.
            String wwwAuth = resp.getHeader("WWW-Authenticate");
            assertThat(wwwAuth).startsWith("Basic realm=\"keiko-engine (re-auth ");
            assertThat(wwwAuth).endsWith(")\"");
            assertThat(session.isInvalid()).isTrue();
        }

        @Test
        void idleTimeoutMillis_matchesConstructorMinutes() {
            SessionTimeoutFilter f = new SessionTimeoutFilter(180, true);
            assertThat(f.idleTimeoutMillis()).isEqualTo(180L * 60_000L);
        }

        @Test
        void shortTimeoutDeployment_canBeConstructed() {
            // Sanity: a 5-minute deployment (e.g., for a sensitive admin route)
            // should be representable. Not currently used but keeps the option open.
            SessionTimeoutFilter f = new SessionTimeoutFilter(5, true);
            assertThat(f.isEnabled()).isTrue();
            assertThat(f.idleTimeoutMillis()).isEqualTo(5L * 60_000L);
        }
    }
}
