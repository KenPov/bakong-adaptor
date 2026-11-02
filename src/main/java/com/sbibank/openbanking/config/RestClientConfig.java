package com.sbibank.openbanking.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient bakongObClientRestClient(BakongObClientProperties properties) {
        return RestClient.builder()
                .requestFactory(jdkRequestFactory(properties.connectTimeout(), properties.readTimeout()))
                .build();
    }

    @Bean
    public RestClient ciftpRestClient(CiftpProperties properties) {
        return RestClient.builder()
                .requestFactory(jdkRequestFactory(properties.connectTimeout(), properties.readTimeout()))
                .build();
    }

    private JdkClientHttpRequestFactory jdkRequestFactory(Duration connectTimeout, Duration readTimeout) {
        HttpClient httpClient =
                HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}
