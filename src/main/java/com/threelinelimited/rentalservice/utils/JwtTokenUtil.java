package com.threelinelimited.rentalservice.utils;

import com.threelinelimited.rentalservice.config.PropsReader;
import com.threelinelimited.rentalservice.constant.ApplicationConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Component
@Slf4j
public class JwtTokenUtil {
    private final PropsReader propsReader;
    private final SecretKey signingKey;

    public JwtTokenUtil(PropsReader propsReader) {
        this.propsReader = propsReader;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(propsReader.getJwtSecret()));
    }

    public String generateJwtToken(String username, Map<String, Object> claims) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .claims(claims)
                .issuer(propsReader.getJwtIssuer())
                .issuedAt(new Date(now))
                .expiration(new Date(now + propsReader.getAccessTokenValidityInSeconds() * 1000))
                .signWith(signingKey)
                .compact();
    }

    public Optional<Claims> parseAndValidate(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(propsReader.getJwtIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected JWT: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public boolean validateJwtToken(String token) {
        return parseAndValidate(token).isPresent();
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> resolver) {
        return parseAndValidate(token).map(resolver).orElse(null);
    }

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public static String stripBearer(String headerValue) {
        if (headerValue != null && headerValue.startsWith(ApplicationConstants.BEARER_PREFIX)) {
            String token = headerValue.substring(ApplicationConstants.BEARER_PREFIX.length()).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }
}
