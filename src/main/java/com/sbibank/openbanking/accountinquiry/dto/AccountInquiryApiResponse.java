package com.sbibank.openbanking.accountinquiry.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Field naming intentionally mixes snake_case and camelCase ({@code product_code}, {@code account_data},
 * but {@code kycStatus}/{@code publicOperations}) to match the legacy Synapse payloadFactory template
 * byte-for-byte, since this is a wire contract already consumed by the mobile app.
 */
public record AccountInquiryApiResponse(AccountInquiryStatus status, AccountData data) {

    public static AccountInquiryApiResponse error(AccountInquiryStatus status) {
        return new AccountInquiryApiResponse(status, null);
    }

    public static AccountInquiryApiResponse success(AccountData data) {
        return new AccountInquiryApiResponse(AccountInquiryStatus.success(), data);
    }

    public record AccountData(
            String result,
            @JsonProperty("product_code") String productCode,
            @JsonProperty("account_data") AccountDataBlock accountData,
            List<Object> publicOperations) {}

    public record AccountDataBlock(
            String account,
            String kycStatus,
            @JsonProperty("account_name") String accountName,
            @JsonProperty("account_currency") String accountCurrency,
            @JsonProperty("bank_code") String bankCode,
            @JsonProperty("account_type") String accountType) {}
}
