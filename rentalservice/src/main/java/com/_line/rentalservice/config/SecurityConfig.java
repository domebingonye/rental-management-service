package com._line.rentalservice.config;

import com._line.rentalservice.enums.UserRoleType;
import com._line.rentalservice.filter.AuthTokenFilter;
import com._line.rentalservice.service.SystemUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final SystemUserService userDetailsService;
    private final AuthEntryPointJwt authEntryPointJwt;
    private final AuthTokenFilter authTokenFilter;
    private final PasswordEncoder passwordEncoder;

    /**
     * Username/password provider backed by SYS_USERS.
     * <p>
     * Spring's default checks "disabled/locked" BEFORE verifying the password, which would tell an
     * attacker who only knows a username that the account is pending or suspended. We move the
     * status checks after the password check so only the real owner learns the account state.
     */
    @Bean("userAuthProvider")
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setPreAuthenticationChecks(user -> { });
        provider.setPostAuthenticationChecks(user -> {
            if (!user.isAccountNonLocked()) throw new LockedException("Account is suspended");
            if (!user.isEnabled()) throw new DisabledException("Account is pending approval");
        });
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationProvider userAuthProvider) throws Exception {
        http
                .authenticationManager(new ProviderManager(userAuthProvider))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health",
                                "/api/v1/authentication/login",
                                "/api/v1/authentication/user",
                                "/api/v1/authentication/signup/customer",
                                "/api/v1/authentication/signup/merchant",
                                // Called by the payment provider; authenticated by HMAC signature, not JWT.
                                "/api/v1/webhooks/**",
                                // Simulated external bank/provider. Its requery API checks its own secret key.
                                "/mock-bank/**").permitAll()
                        // Coarse role gates per area; fine-grained rules use @PreAuthorize on the controller.
                        .requestMatchers("/api/v1/staff/**").hasAuthority(UserRoleType.Code.ROLE_STAFF)
                        .requestMatchers("/api/v1/merchant/**").hasAuthority(UserRoleType.Code.ROLE_MERCHANT)
                        .requestMatchers("/api/v1/customer/**").hasAuthority(UserRoleType.Code.ROLE_CUSTOMER)
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authEntryPointJwt))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
