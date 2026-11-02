package com.sbibank.openbanking.accountinquiry.dto;

/**
 * {@code errorCode} is deliberately {@link Object}, not {@link String}: the legacy payload emits it as a
 * bare JSON number in the "not found" branch ({@code "errorCode": 65}) but as a passthrough string in the
 * generic error branch ({@code "errorCode": "<upstream errorCode>"}). Passing the raw upstream value (or an
 * Integer literal) through lets Jackson serialize each case exactly as legacy did.
 */
public record AccountInquiryStatus(int code, Object errorCode, String error, String warning, String message) {

    public static AccountInquiryStatus success() {
        return new AccountInquiryStatus(0, null, null, null, null);
    }

    public static AccountInquiryStatus error(Object errorCode, String error) {
        return new AccountInquiryStatus(1, errorCode, error, "", "");
    }
}
