package com.duong.managefile.client;

import com.duong.managefile.dto.response.GoogleUserInfoResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface GoogleUserInfoClient {
    @GetExchange("/v1/userinfo")
    GoogleUserInfoResponse getUserInfo(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization);
}
