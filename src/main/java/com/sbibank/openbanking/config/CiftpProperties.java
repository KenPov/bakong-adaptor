package com.sbibank.openbanking.config;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("ciftp")
public record CiftpProperties(
        @NotBlank String baseUrl,
        String soapPath,
        @NotBlank String apiUser,
        @NotBlank String apiPassword,
        Duration connectTimeout,
        Duration readTimeout) {

    public CiftpProperties {
        if (soapPath == null || soapPath.isBlank()) {
            soapPath = "/ciftp-payment-service/cb-adapter/BakongWebService";
        }
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(5);
        }
        if (readTimeout == null) {
            readTimeout = Duration.ofSeconds(15);
        }
    }

    public String soapEndpoint() {
        return baseUrl + soapPath;
    }
}
