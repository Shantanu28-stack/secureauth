package com.shantanu.secureauth.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Date;

@Service
public class JwtService {

    // Loaded from application.properties (app.jwt.secret) — must be at least 32 characters
    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    public String generateToken(String email) {
        try {
            Date now = new Date();
            Date expiry = new Date(now.getTime() + expirationMs);

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(email)
                    .issueTime(now)
                    .expirationTime(expiry)
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claims
            );

            signedJWT.sign(new MACSigner(secret.getBytes()));
            return signedJWT.serialize();

        } catch (JOSEException e) {
            throw new RuntimeException("Failed to generate JWT", e);
        }
    }

    public String extractEmail(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            return jwt.getJWTClaimsSet().getSubject();
        } catch (ParseException e) {
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    public boolean isTokenValid(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);

            boolean verified = jwt.verify(new MACVerifier(secret.getBytes()));
            if (!verified) return false;

            Date exp = jwt.getJWTClaimsSet().getExpirationTime();
            return exp != null && exp.after(new Date());

        } catch (Exception e) {
            return false;
        }
    }
}
