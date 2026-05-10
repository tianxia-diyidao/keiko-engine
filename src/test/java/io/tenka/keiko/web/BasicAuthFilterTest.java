package io.tenka.keiko.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BasicAuthFilter — instantiates directly with controlled
 * credentials so we don't need the Spring context or env-var manipulation.
 */
class BasicAuthFilterTest {

    private static String basicHeader(String user, String pass) {
        String creds = user + ":" + pass;
        return "Basic " + Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
    }

    /** Stand-in chain that just records whether it was invoked. */
    private static FilterChain chainRecorder(AtomicBoolean reached) {
        return (req, resp) -> reached.set(true);
    }

    @Nested
    class DisabledMode {
        @Test
        void bothEmpty_isPassThrough() throws Exception {
            BasicAuthFilter f = new BasicAuthFilter("", "");
            assertThat(f.isEnabled()).isFalse();
            AtomicBoolean reached = new AtomicBoolean(false);
            f.doFilter(new MockHttpServletRequest("GET", "/"), new MockHttpServletResponse(),
                    chainRecorder(reached));
            assertThat(reached).isTrue();
        }

        @Test
        void onlyUserSet_isPassThrough() throws Exception {
            // Half-set is treated as unset — defensive against blank-password mode.
            BasicAuthFilter f = new BasicAuthFilter("u", "");
            assertThat(f.isEnabled()).isFalse();
            AtomicBoolean reached = new AtomicBoolean(false);
            f.doFilter(new MockHttpServletRequest("GET", "/"), new MockHttpServletResponse(),
                    chainRecorder(reached));
            assertThat(reached).isTrue();
        }

        @Test
        void onlyPassSet_isPassThrough() throws Exception {
            BasicAuthFilter f = new BasicAuthFilter("", "p");
            assertThat(f.isEnabled()).isFalse();
            AtomicBoolean reached = new AtomicBoolean(false);
            f.doFilter(new MockHttpServletRequest("GET", "/"), new MockHttpServletResponse(),
                    chainRecorder(reached));
            assertThat(reached).isTrue();
        }
    }

    @Nested
    class EnabledMode {
        private final BasicAuthFilter filter = new BasicAuthFilter("u", "correct");

        @Test
        void noAuthHeader_returns401WithChallenge() throws Exception {
            MockHttpServletResponse resp = new MockHttpServletResponse();
            AtomicBoolean reached = new AtomicBoolean(false);
            filter.doFilter(new MockHttpServletRequest("GET", "/"), resp, chainRecorder(reached));
            assertThat(reached).isFalse();
            assertThat(resp.getStatus()).isEqualTo(401);
            assertThat(resp.getHeader("WWW-Authenticate"))
                    .isEqualTo("Basic realm=\"keiko-engine\"");
        }

        @Test
        void wrongScheme_returns401() throws Exception {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/");
            req.addHeader("Authorization", "Bearer xyz");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            AtomicBoolean reached = new AtomicBoolean(false);
            filter.doFilter(req, resp, chainRecorder(reached));
            assertThat(reached).isFalse();
            assertThat(resp.getStatus()).isEqualTo(401);
        }

        @Test
        void wrongPassword_returns401() throws Exception {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/");
            req.addHeader("Authorization", basicHeader("u", "wrong"));
            MockHttpServletResponse resp = new MockHttpServletResponse();
            AtomicBoolean reached = new AtomicBoolean(false);
            filter.doFilter(req, resp, chainRecorder(reached));
            assertThat(reached).isFalse();
            assertThat(resp.getStatus()).isEqualTo(401);
        }

        @Test
        void wrongUser_returns401() throws Exception {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/");
            req.addHeader("Authorization", basicHeader("v", "correct"));
            MockHttpServletResponse resp = new MockHttpServletResponse();
            AtomicBoolean reached = new AtomicBoolean(false);
            filter.doFilter(req, resp, chainRecorder(reached));
            assertThat(reached).isFalse();
            assertThat(resp.getStatus()).isEqualTo(401);
        }

        @Test
        void correctCreds_passThrough() throws Exception {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/");
            req.addHeader("Authorization", basicHeader("u", "correct"));
            MockHttpServletResponse resp = new MockHttpServletResponse();
            AtomicBoolean reached = new AtomicBoolean(false);
            filter.doFilter(req, resp, chainRecorder(reached));
            assertThat(reached).isTrue();
            assertThat(resp.getStatus()).isEqualTo(200);
        }

        @Test
        void malformedBase64_returns401() throws Exception {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/");
            req.addHeader("Authorization", "Basic not_valid_base64!!!");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            AtomicBoolean reached = new AtomicBoolean(false);
            filter.doFilter(req, resp, chainRecorder(reached));
            assertThat(reached).isFalse();
            assertThat(resp.getStatus()).isEqualTo(401);
        }
    }
}
