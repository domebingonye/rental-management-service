package com._line.rentalservice.filter;

import com._line.rentalservice.config.PropsReader;
import com._line.rentalservice.constant.ApplicationConstants;
import com._line.rentalservice.service.SystemUserService;
import com._line.rentalservice.utils.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class AuthTokenFilter extends OncePerRequestFilter {
    private final JwtTokenUtil jwtUtils;
    private final SystemUserService userDetailsService;
    private final List<String> ignoreUrls;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AuthTokenFilter(JwtTokenUtil jwtUtils, @Lazy SystemUserService userDetailsService, PropsReader propsReader) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
        this.ignoreUrls = Arrays.stream(propsReader.getIgnoreUrls().split(",")).map(String::trim).toList();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String jwt = JwtTokenUtil.stripBearer(request.getHeader(ApplicationConstants.AUTHORIZATION_KEY));
        if (jwt != null) {
            jwtUtils.parseAndValidate(jwt).ifPresent(claims -> authenticate(claims.getSubject(), request));
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String username, HttpServletRequest request) {
        try {
            UserDetails user = userDetailsService.loadUserByUsername(username);
            if (!user.isEnabled() || !user.isAccountNonLocked()) {
                log.info("Token presented for inactive account {}", username);
                return;
            }
            var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (UsernameNotFoundException e) {
            log.info("Token presented for unknown user {}", username);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return ignoreUrls.stream().anyMatch(p -> pathMatcher.match(p, path));
    }
}
