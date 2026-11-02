package com.sbibank.openbanking.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("our-bank")
public record OurBankProperties(
        @NotBlank String bic, @NotBlank String appName, @NotBlank String name, PostalAddress postalAddress) {

    public record PostalAddress(String streetName, String buildingNumber, String townName, String country) {}
}
