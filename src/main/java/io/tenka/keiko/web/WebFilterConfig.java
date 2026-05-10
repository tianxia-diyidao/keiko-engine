package io.tenka.keiko.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Wires the two cross-cutting filters in explicit order.
 *
 * <p>Order matters here: BasicAuthFilter must run FIRST so unauthenticated
 * traffic gets challenged with the standard realm before SessionTimeoutFilter
 * touches a session. Once creds are accepted, SessionTimeoutFilter inspects
 * the (now-authenticated) session and may force a re-auth via a rotated realm.
 *
 * <p>Sequence on first request:
 * <ol>
 *   <li>BasicAuth: no creds → 401, realm "keiko-engine" → browser prompts.</li>
 *   <li>User enters creds → BasicAuth accepts.</li>
 *   <li>SessionTimeout: fresh session → records lastSeen → pass through.</li>
 * </ol>
 *
 * <p>Sequence after 180-min idle:
 * <ol>
 *   <li>Browser auto-replays cached creds → BasicAuth accepts.</li>
 *   <li>SessionTimeout: lastSeen too old → invalidates session, returns 401
 *       with rotated realm "keiko-engine (re-auth &lt;epoch&gt;)" →
 *       browser shows fresh prompt because realm changed.</li>
 * </ol>
 *
 * <p>Both filters are registered as {@link FilterRegistrationBean}s rather
 * than {@code @Component}s so we control order explicitly. Spring Boot would
 * otherwise pick an order based on bean name / declaration, which is
 * brittle when filter behavior depends on sequence.
 */
@Configuration
public class WebFilterConfig {

    @Bean
    public BasicAuthFilter basicAuthFilter(
            @Value("${keiko.basic-auth.user:}") String user,
            @Value("${keiko.basic-auth.password:}") String password) {
        return new BasicAuthFilter(user, password);
    }

    @Bean
    public FilterRegistrationBean<BasicAuthFilter> basicAuthRegistration(BasicAuthFilter filter) {
        FilterRegistrationBean<BasicAuthFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        reg.addUrlPatterns("/*");
        return reg;
    }

    @Bean
    public SessionTimeoutFilter sessionTimeoutFilter(
            BasicAuthFilter basicAuthFilter,
            @Value("${keiko.session-idle-timeout-minutes:180}") int idleMinutes) {
        return new SessionTimeoutFilter(idleMinutes, basicAuthFilter.isEnabled());
    }

    @Bean
    public FilterRegistrationBean<SessionTimeoutFilter> sessionTimeoutRegistration(
            SessionTimeoutFilter filter) {
        FilterRegistrationBean<SessionTimeoutFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        reg.addUrlPatterns("/*");
        return reg;
    }
}
