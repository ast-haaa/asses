package com.cynbit.remediation.config;

import com.cynbit.remediation.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;
    private final List<String> allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ObjectMapper objectMapper,
            @Value("${app.cors.allowed-origins:https://www.cynbit.in,https://portal.cynbit.in}") List<String> allowedOrigins) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        // Opt out of XOR token encoding so the frontend can send the exact value from XSRF-TOKEN cookie
        requestHandler.setCsrfRequestAttributeName(null);

        http
            // 1. CORS Configuration: Strict Origin Whitelisting
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 2. CSRF Configuration: CookieCsrfTokenRepository for SPA compatibility
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(requestHandler)
                // Exempt public unauthenticated submission forms and authentication endpoints
                .ignoringRequestMatchers(
                    "/api/v1/inquiries/**",
                    "/api/v1/careers/*/applications",
                    "/api/v1/portal/auth/login",
                    "/api/v1/portal/auth/logout"
                )
            )

            // 3. Defense-in-Depth Security Headers
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self'; object-src 'none'; frame-ancestors 'none'; base-uri 'self';"))
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .contentTypeOptions(Customizer.withDefaults())
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(63072000))
            )

            // 4. Session Management: Stateless
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 5. Exception Handling: RFC 7807 ProblemDetail via ObjectMapper
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/problem+json");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                            HttpStatus.UNAUTHORIZED, "Authentication token missing or invalid.");
                    problem.setTitle("Unauthorized");
                    String uri = request.getRequestURI();
                    if (uri != null && !uri.isBlank()) {
                        problem.setInstance(URI.create(uri));
                    }
                    response.getWriter().write(objectMapper.writeValueAsString(problem));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/problem+json");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                            HttpStatus.FORBIDDEN, "Access denied: Insufficient privileges or CSRF token mismatch.");
                    problem.setTitle("Forbidden");
                    String uri = request.getRequestURI();
                    if (uri != null && !uri.isBlank()) {
                        problem.setInstance(URI.create(uri));
                    }
                    response.getWriter().write(objectMapper.writeValueAsString(problem));
                })
            )

            // 6. Route Authorization Matrix: Strict Method & Path Enforcement
            .authorizeHttpRequests(auth -> auth
                // Public Inquiry Endpoints: Strictly POST only
                .requestMatchers(HttpMethod.POST, "/api/v1/inquiries/**").permitAll()

                // Public Career Endpoints: Strictly POST application and GET status only
                .requestMatchers(HttpMethod.POST, "/api/v1/careers/*/applications").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/careers/*/applications/*").permitAll()

                // Portal Authentication Endpoints
                .requestMatchers(HttpMethod.POST, "/api/v1/portal/auth/login", "/api/v1/portal/auth/logout").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/portal/auth/health").permitAll()
                // NOTE: /api/v1/portal/auth/me is NOT in permitAll; it falls through to authenticated()

                // Protected Administrative Endpoints (RBAC)
                .requestMatchers("/api/v1/portal/documents/**").hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_HR_ADMIN")
                .requestMatchers("/api/v1/portal/interns/**").hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_HR_ADMIN", "ROLE_MENTOR")

                // Any other request requires authentication
                .anyRequest().authenticated()
            )

            // 7. JWT Filter Placement & CSRF Cookie Attachment
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new CsrfCookieFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "X-XSRF-TOKEN"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Ensures that the CSRF token cookie is loaded and written to the response for SPAs.
     */
    public static class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                // Invoking getToken() triggers evaluation and cookie writing in CookieCsrfTokenRepository
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }
}
