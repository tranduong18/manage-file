package com.duong.managefile.security;

import com.duong.managefile.common.TokenType;
import com.duong.managefile.exception.ErrorCode;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtTokenTypeValidator implements OAuth2TokenValidator<Jwt> {
    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        TokenType tokenType = TokenType.valueOf(token.getClaimAsString("type"));
        if(tokenType != TokenType.ACCESS){
            ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(errorCode.name(), errorCode.getMessage(), null));
        }
        return OAuth2TokenValidatorResult.success();
    }
}
