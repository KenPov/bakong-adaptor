package com.sbibank.openbanking.accountinquiry.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountInquiryUpstreamResponse(Integer responseCode, String errorCode, String responseMessage, UpstreamData data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UpstreamData(AccountInfo accountInfo) {}
}
