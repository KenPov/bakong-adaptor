package com.sbibank.openbanking.config;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("bakong.ob-client")
public record BakongObClientProperties(
        @NotBlank String authUrl,
        @NotBlank String accountInquiryUrl,
        @NotBlank String username,
        @NotBlank String password,
        Duration connectTimeout,
        Duration readTimeout) {

    public BakongObClientProperties {
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(5);
        }
        if (readTimeout == null) {
            readTimeout = Duration.ofSeconds(10);
        }
    }
}
