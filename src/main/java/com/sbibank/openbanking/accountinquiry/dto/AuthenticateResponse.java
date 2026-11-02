package com.sbibank.openbanking.accountinquiry.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthenticateResponse(AuthData data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AuthData(String token) {}
}
