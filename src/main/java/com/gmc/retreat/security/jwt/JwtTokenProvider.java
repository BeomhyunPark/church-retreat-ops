package com.gmc.retreat.security.jwt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmc.retreat.admin.domain.AdminRole;
import com.gmc.retreat.security.auth.AdminPrincipal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtTokenProvider {

    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
    private static final TypeReference<Map<String, Object>> CLAIMS_TYPE = new TypeReference<>() {
    };

    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public JwtTokenProvider(JwtProperties jwtProperties, ObjectMapper objectMapper, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public String createAccessToken(AdminPrincipal principal) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plusSeconds(jwtProperties.accessTokenExpirationMinutes() * 60);

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", String.valueOf(principal.id()));
        payload.put("email", principal.email());
        payload.put("name", principal.name());
        payload.put("role", principal.role().name());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        String headerPart = encodeJson(header);
        String payloadPart = encodeJson(payload);
        String signingInput = headerPart + "." + payloadPart;
        return signingInput + "." + sign(signingInput);
    }

    public JwtAuthenticationClaims validateAccessToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new BadCredentialsException("Invalid JWT.");
        }

        String signingInput = parts[0] + "." + parts[1];
        String expectedSignature = sign(signingInput);
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            throw new BadCredentialsException("Invalid JWT signature.");
        }

        Map<String, Object> claims = decodeJson(parts[1]);
        long expiresAt = requiredLongClaim(claims, "exp");
        if (Instant.now(clock).getEpochSecond() >= expiresAt) {
            throw new BadCredentialsException("JWT has expired.");
        }

        return new JwtAuthenticationClaims(
                requiredLongStringClaim(claims, "sub"),
                requiredStringClaim(claims, "email"),
                requiredStringClaim(claims, "name"),
                requiredRoleClaim(claims, "role")
        );
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to encode JWT.", exception);
        }
    }

    private Map<String, Object> decodeJson(String value) {
        try {
            byte[] decoded = BASE64_URL_DECODER.decode(value);
            return objectMapper.readValue(decoded, CLAIMS_TYPE);
        } catch (Exception exception) {
            throw new BadCredentialsException("Invalid JWT payload.", exception);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    jwtProperties.secret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKey);
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign JWT.", exception);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    private String requiredStringClaim(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        if (!(value instanceof String stringValue) || !StringUtils.hasText(stringValue)) {
            throw new BadCredentialsException("JWT is missing required claim.");
        }
        return stringValue;
    }

    private long requiredLongClaim(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(requiredStringClaim(claims, name));
        } catch (NumberFormatException exception) {
            throw new BadCredentialsException("JWT has invalid claim value.", exception);
        }
    }

    private Long requiredLongStringClaim(Map<String, Object> claims, String name) {
        try {
            return Long.valueOf(requiredStringClaim(claims, name));
        } catch (NumberFormatException exception) {
            throw new BadCredentialsException("JWT has invalid claim value.", exception);
        }
    }

    private AdminRole requiredRoleClaim(Map<String, Object> claims, String name) {
        try {
            return AdminRole.valueOf(requiredStringClaim(claims, name));
        } catch (IllegalArgumentException exception) {
            throw new BadCredentialsException("JWT has invalid role claim.", exception);
        }
    }
}
