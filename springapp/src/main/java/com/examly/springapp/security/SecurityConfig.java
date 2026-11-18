package com.examly.springapp.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.function.Supplier;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtFilter jwtFilter;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(JwtFilter jwtFilter, CustomUserDetailsService userDetailsService) {
        this.jwtFilter = jwtFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepo.setCookiePath("/"); // Ensure accessible everywhere
        // Configure secure based on same properties if necessary, but Spring handles it automatically mostly.
        
        http.cors(org.springframework.security.config.Customizer.withDefaults())
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfRepo)
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
                .ignoringRequestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/refresh") // login/register/refresh might not have CSRF initially
            )
            .headers(headers -> headers
                .xssProtection(xss -> xss.disable()) // Deprecated in modern browsers, CSP is better
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self' 'unsafe-inline' 'unsafe-eval' http://localhost:3000; connect-src 'self' http://localhost:3000 http://localhost:8080; frame-ancestors 'none'; object-src 'none'"))
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(contentType -> {}) // Enables nosniff by default
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    log.warn("Filter-level access denied on {} {}: {} - {} | hasCookieHeader={} | hasXsrfHeader={}",
                            request.getMethod(), request.getRequestURI(),
                            accessDeniedException.getClass().getSimpleName(),
                            accessDeniedException.getMessage(),
                            request.getHeader("Cookie") != null && request.getHeader("Cookie").contains("XSRF-TOKEN"),
                            request.getHeader("X-XSRF-TOKEN") != null);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/problem+json");
                    response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Forbidden\",\"status\":403,\"detail\":\"Access denied or missing/invalid CSRF token.\"}");
                })
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/**",
                    "/api/docs",
                    "/api/docs/**",
                    "/api/v1/health",
                    "/",
                    "/index.html",
                    "/static/**",
                    "/*.ico",
                    "/*.png",
                    "/*.json",
                    "/catalog",
                    "/tickets",
                    "/tickets/**",
                    "/approvals",
                    "/queue",
                    "/queue/**",
                    "/audit-chain",
                    "/notifications",
                    "/notifications/**",
                    "/analytics",
                    "/analytics/**",
                    "/users",
                    "/users/**",
                    "/roles",
                    "/roles/**",
                    "/profiles",
                    "/profiles/**",
                    "/user-role-mappings",
                    "/user-role-mappings/**",
                    "/audit",
                    "/audit/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Spring Security 6 SPA CSRF handler: validates raw X-XSRF-TOKEN header values read from
     * CookieCsrfTokenRepository while preserving XOR BREACH-masked validation for form parameters
     * and MockMvc csrf() requests.
     */
    static final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {
        private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
            this.delegate.handle(request, response, csrfToken);
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
                return super.resolveCsrfTokenValue(request, csrfToken);
            }
            return this.delegate.resolveCsrfTokenValue(request, csrfToken);
        }
    }

    /**
     * Eagerly resolves deferred CSRF tokens on safe requests so CookieCsrfTokenRepository writes
     * the XSRF-TOKEN cookie for SPA clients.
     */
    static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken == null) {
                csrfToken = (CsrfToken) request.getAttribute("_csrf");
            }
            if (csrfToken != null) {
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }
}
