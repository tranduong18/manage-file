package com.duong.managefile.config;

import com.duong.managefile.client.GoogleTokenClient;
import com.duong.managefile.client.GoogleUserInfoClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@EnableConfigurationProperties(GoogleOAuthProperties.class)
public class GoogleClientConfig {
    @Bean
    public GoogleTokenClient googleTokenClient(RestClient.Builder builder) {
        return create(builder, "https://oauth2.googleapis.com", GoogleTokenClient.class);
    }

    @Bean
    public GoogleUserInfoClient googleUserInfoClient(RestClient.Builder builder) {
        return create(builder, "https://openidconnect.googleapis.com", GoogleUserInfoClient.class);
    }

    private <T> T create(RestClient.Builder builder, String baseUrl, Class<T> type) {
        RestClient client = builder.clone().baseUrl(baseUrl).build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(type);
    }
}
