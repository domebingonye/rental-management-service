package com._line.rentalservice.service;

import com._line.rentalservice.config.PropsReader;
import com._line.rentalservice.constant.ApplicationConstants;
import com._line.rentalservice.dto.request.LoginRequest;
import com._line.rentalservice.dto.response.LoginResponse;
import com._line.rentalservice.enums.AccountStatus;
import com._line.rentalservice.enums.ResponseCodes;
import com._line.rentalservice.exception.RequestException;
import com._line.rentalservice.model.SystemUserEntity;
import com._line.rentalservice.utils.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthenticationService {
    private static final String INVALID_CREDENTIALS = "Invalid username or password";

    private final SystemUserDaoService systemUserDaoService;
    private final JwtTokenUtil jwtTokenUtil;
    private final AuthenticationProvider userAuthProvider;
    private final PropsReader propsReader;

    @Transactional
    public LoginResponse processLogin(HttpServletRequest request, LoginRequest login) {
        log.info("Login attempt for {} from {}", login.getUsername(), request.getRemoteAddr());

        var token = new UsernamePasswordAuthenticationToken(login.getUsername(), login.getPassword());
        token.setDetails(new WebAuthenticationDetails(request));

        Authentication auth;
        try {
            auth = userAuthProvider.authenticate(token);
        } catch (DisabledException e) {
            throw new RequestException(ResponseCodes.FORBIDDEN, "Your merchant account is awaiting approval");
        } catch (LockedException e) {
            throw new RequestException(ResponseCodes.FORBIDDEN, "Your account has been suspended");
        } catch (AuthenticationException e) {
            log.info("Login failed for {}: {}", login.getUsername(), e.getMessage());
            throw new RequestException(ResponseCodes.UNAUTHORIZED, INVALID_CREDENTIALS);
        }

        SystemUserEntity user = systemUserDaoService.findByUsername(auth.getName())
                .orElseThrow(() -> new RequestException(ResponseCodes.UNAUTHORIZED, INVALID_CREDENTIALS));

        if (login.getUserRoleType() != null && login.getUserRoleType() != user.getUserRoleType()) {
            log.info("Login for {} rejected: role {} used on {} portal", user.getUsername(), user.getUserRoleType(), login.getUserRoleType());
            throw new RequestException(ResponseCodes.UNAUTHORIZED, INVALID_CREDENTIALS);
        }

        user.setLastLoginOn(LocalDateTime.now());

        String jwt = jwtTokenUtil.generateJwtToken(user.getUsername(), Map.of(
                ApplicationConstants.ROLE_CLAIM, user.getUserRoleType().name(),
                ApplicationConstants.CODE_CLAIM, user.getCode()));

        log.info("User {} ({}) logged in", user.getUsername(), user.getUserRoleType());
        return toResponse(user, auth)
                .token(ApplicationConstants.BEARER_PREFIX + jwt)
                .expiresInSeconds(propsReader.getAccessTokenValidityInSeconds())
                .build();
    }

    @Transactional(readOnly = true)
    public LoginResponse getUser(String authorizationHeader) {
        String token = JwtTokenUtil.stripBearer(authorizationHeader);
        String username = token == null ? null : jwtTokenUtil.getUsernameFromToken(token);
        if (username == null) {
            throw new RequestException(ResponseCodes.UNAUTHORIZED, "Could not validate token");
        }
        SystemUserEntity user = systemUserDaoService.findByUsername(username)
                .filter(u -> u.getStatus() == AccountStatus.ACTIVE)
                .orElseThrow(() -> new RequestException(ResponseCodes.UNAUTHORIZED, "Could not validate token"));
        return toResponse(user, null).build();
    }

    public void logout() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) log.info("User {} logged out", auth.getName());
        SecurityContextHolder.clearContext();
    }

    private static LoginResponse.LoginResponseBuilder toResponse(SystemUserEntity user, Authentication auth) {
        return LoginResponse.builder()
                .code(user.getCode())
                .email(user.getEmail())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .surname(user.getSurname())
                .userRoleType(user.getUserRoleType())
                .active(user.getStatus() == AccountStatus.ACTIVE)
                .authorities(auth != null
                        ? auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()
                        : java.util.List.of(user.getUserRoleType().name()));
    }
}
