package com.duong.managefile.security;

import com.duong.managefile.common.TokenType;
import com.duong.managefile.exception.AppException;
import com.duong.managefile.exception.ErrorCode;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "JWT-SERVICE")
public class JwtService {
    @Value("${jwt.secret-key}")
    private String secretKey;

    public String generateAccessToken(String userId, String email){
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);

        Date issueTime = new Date();
        Date expirationTime = Date.from(issueTime.toInstant().plus(30, ChronoUnit.MINUTES));
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(userId)
                .issueTime(issueTime)
                .expirationTime(expirationTime)
                .jwtID(UUID.randomUUID().toString())
                .claim("email", email)
                .claim("type", TokenType.ACCESS.name())
                .build();

        SignedJWT signedJWT = new SignedJWT(header, jwtClaimsSet);
        try {
            signedJWT.sign(new MACSigner(secretKey));
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    public String generateRefreshToken(String userId, int tokenVersion){
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);

        Date issueTime = new Date();
        Date expirationTime = Date.from(issueTime.toInstant().plus(14, ChronoUnit.DAYS));

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(userId)
                .issueTime(issueTime)
                .expirationTime(expirationTime)
                .jwtID(UUID.randomUUID().toString())
                .claim("type", TokenType.REFRESH.name())
                .claim("version", tokenVersion)
                .build();

        SignedJWT signedJWT = new SignedJWT(header, jwtClaimsSet);
        try {
            signedJWT.sign(new MACSigner(secretKey));
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    public SignedJWT validateToken(String token, TokenType tokenType) {
        try {
            if(!StringUtils.hasText(token)){
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }

            SignedJWT signedJWT = SignedJWT.parse(token);
            boolean isValid = signedJWT.verify(new MACVerifier(secretKey));
            if(!isValid){
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }

            TokenType type = TokenType.valueOf(signedJWT.getJWTClaimsSet().getStringClaim("type"));
            if(type != tokenType){
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }

            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            if(expirationTime.before(new Date())){
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }

            Integer version = signedJWT.getJWTClaimsSet().getIntegerClaim("version");
            if(version == null){
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }

            return signedJWT;

        } catch (ParseException | JOSEException e){
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }
}
