package com.sbibank.openbanking.accountinquiry.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Shape of {@code data.accountInfo} returned by the other bank's account-inquiry endpoint via the
 * Bakong OB hub. {@code localPartCode} is not part of the documented upstream schema (the schema only
 * defines {@code bankCode}/{@code foreignBankCode}) but the legacy Synapse mapper reads it for the bank
 * code, so it is kept here defensively - see {@link com.sbibank.openbanking.accountinquiry.service.AccountInquiryResponseMapper}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountInfo(
        String name,
        String accountNumber,
        String bankCode,
        String foreignBankCode,
        List<String> ccy,
        String type,
        String openingDate,
        String status,
        String kycStatus,
        List<Object> publicOperations,
        String localPartCode) {}
