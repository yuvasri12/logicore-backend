package com.devbridge.logicore.config;

import com.devbridge.logicore.security.JwtAuthFilter;
import com.devbridge.logicore.security.JwtService;
import com.devbridge.logicore.security.RestAccessDeniedHandler;
import com.devbridge.logicore.security.RestAuthenticationEntryPoint;
import com.devbridge.logicore.security.TrackingRateLimitFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String OPS = "OPS_MANAGER";
    private static final String WAREHOUSE = "WAREHOUSE_STAFF";
    private static final String DISPATCHER = "DISPATCHER";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   RestAuthenticationEntryPoint entryPoint,
                                                   RestAccessDeniedHandler deniedHandler,
                                                   @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver,
                                                   @Value("${app.security.tracking.max-requests:30}") int trackingMaxRequests,
                                                   @Value("${app.security.tracking.window-seconds:60}") int trackingWindowSeconds)
            throws Exception {
        TrackingRateLimitFilter rateLimitFilter =
                new TrackingRateLimitFilter(trackingMaxRequests, trackingWindowSeconds, exceptionResolver);

        http
                .cors(Customizer.withDefaults())
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(deniedHandler)
                .and()
                .authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // public
                .antMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .antMatchers(HttpMethod.GET, "/api/tracking/**").permitAll()
                // any logged-in user
                .antMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                .antMatchers(HttpMethod.GET, "/api/shipments/**").authenticated()
                // user management: admin only
                .antMatchers("/api/users", "/api/users/**").hasRole(ADMIN)
                // audit log: admin only
                .antMatchers("/api/audit", "/api/audit/**").hasRole(ADMIN)
                // organisation (regions, warehouses): admin manages, ops manager can read for pickers
                .antMatchers(HttpMethod.GET, "/api/org/**").hasAnyRole(ADMIN, OPS)
                .antMatchers("/api/org", "/api/org/**").hasRole(ADMIN)
                // customers: admin and ops manager
                .antMatchers("/api/customers", "/api/customers/**").hasAnyRole(ADMIN, OPS)
                // shipments
                .antMatchers(HttpMethod.POST, "/api/shipments", "/api/shipments/").hasAnyRole(ADMIN, OPS)
                .antMatchers(HttpMethod.PUT, "/api/shipments/*").hasAnyRole(ADMIN, OPS)
                .antMatchers(HttpMethod.PATCH, "/api/shipments/*/status").hasRole(ADMIN)
                .antMatchers(HttpMethod.POST, "/api/shipments/*/load").hasAnyRole(ADMIN, OPS, WAREHOUSE)
                .antMatchers(HttpMethod.POST, "/api/shipments/*/deliver").hasAnyRole(ADMIN, OPS, DISPATCHER)
                .antMatchers(HttpMethod.DELETE, "/api/shipments/*").hasAnyRole(ADMIN, OPS)
                // warehouse and dispatch
                .antMatchers(HttpMethod.POST, "/api/warehouse/**").hasAnyRole(ADMIN, OPS, WAREHOUSE)
                .antMatchers(HttpMethod.POST, "/api/dispatch").hasAnyRole(ADMIN, OPS, DISPATCHER)
                // everything else needs a login
                .anyRequest().authenticated()
                .and()
                .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, JwtAuthFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Logins go through /api/auth/login, not Spring's built-in user. This empty service only
     * stops Spring from creating a default user with a generated password.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Use /api/auth/login");
        };
    }
}