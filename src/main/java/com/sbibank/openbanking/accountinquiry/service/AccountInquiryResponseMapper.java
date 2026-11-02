package com.sbibank.openbanking.accountinquiry.service;

import com.sbibank.openbanking.accountinquiry.dto.AccountInfo;
import com.sbibank.openbanking.accountinquiry.dto.AccountInquiryApiResponse;
import com.sbibank.openbanking.accountinquiry.dto.AccountInquiryApiResponse.AccountData;
import com.sbibank.openbanking.accountinquiry.dto.AccountInquiryApiResponse.AccountDataBlock;
import com.sbibank.openbanking.accountinquiry.dto.AccountInquiryStatus;
import com.sbibank.openbanking.accountinquiry.dto.AccountInquiryUpstreamResponse;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Ports the response branching from {@code account-inquiry-to-other-bank.xml} (the WSO2 Synapse
 * mediation flow) field-for-field.
 */
@Component
public class AccountInquiryResponseMapper {

    private static final List<String> ACCOUNT_LOCAL_NUMBER_NOT_FOUND_CODES = List.of("015", "15");
    private static final int NOT_FOUND_ERROR_CODE = 65;
    private static final String NOT_FOUND_MESSAGE = "The Account Local Number is not found.";

    public static final AccountInquiryApiResponse INTERNAL_ERROR_RESPONSE =
            AccountInquiryApiResponse.error(AccountInquiryStatus.error("999", "Internal Server Error"));

    public AccountInquiryApiResponse toApiResponse(AccountInquiryUpstreamResponse upstream) {
        boolean responseCodeIsError = upstream.responseCode() == null || upstream.responseCode() != 0;
        boolean errorCodePresent = isPresent(upstream.errorCode());

        if (responseCodeIsError || errorCodePresent) {
            return mapError(upstream);
        }
        return mapSuccess(upstream);
    }

    private AccountInquiryApiResponse mapError(AccountInquiryUpstreamResponse upstream) {
        String errorCode = upstream.errorCode();
        if (errorCode != null && ACCOUNT_LOCAL_NUMBER_NOT_FOUND_CODES.contains(errorCode)) {
            return AccountInquiryApiResponse.error(AccountInquiryStatus.error(NOT_FOUND_ERROR_CODE, NOT_FOUND_MESSAGE));
        }
        return AccountInquiryApiResponse.error(AccountInquiryStatus.error(errorCode, upstream.responseMessage()));
    }

    private AccountInquiryApiResponse mapSuccess(AccountInquiryUpstreamResponse upstream) {
        AccountInfo accountInfo =
                upstream.data() != null && upstream.data().accountInfo() != null ? upstream.data().accountInfo() : new AccountInfo(
                        null, null, null, null, List.of(), null, null, null, null, List.of(), null);

        String kycStatus = mapKycStatus(accountInfo.kycStatus());
        String accountType = mapAccountType(accountInfo.type());
        String bankCode = mapBankCode(accountInfo);

        AccountDataBlock accountDataBlock = new AccountDataBlock(
                accountInfo.accountNumber(), kycStatus, accountInfo.name(), firstOrNull(accountInfo.ccy()), bankCode, accountType);

        AccountData data = new AccountData("success", "BAKONG", accountDataBlock, accountInfo.publicOperations());
        return AccountInquiryApiResponse.success(data);
    }

    private String mapKycStatus(String kycStatus) {
        if ("FULL_KYC".equals(kycStatus)) {
            return "1";
        }
        if ("PARTIAL_KYC".equals(kycStatus)) {
            return "2";
        }
        return "0";
    }

    private String mapAccountType(String type) {
        if ("SAVINGS".equals(type)) {
            return "01";
        }
        if ("CURRENT".equals(type)) {
            return "02";
        }
        if ("WALLET".equals(type)) {
            return "03";
        }
        if ("LOAN".equals(type)) {
            return "04";
        }
        // Legacy default: `var accountType = "01"` is never reassigned for an unrecognized type.
        return "01";
    }

    /**
     * Legacy quirk: the Synapse mapper reads {@code accountInfo.localPartCode}, which is not part of the
     * documented upstream schema (only {@code bankCode}/{@code foreignBankCode} are documented). Preserve
     * legacy behavior defensively: prefer {@code localPartCode} when present, else fall back to
     * {@code bankCode}. Either way, strip a trailing "XXX" participant-code suffix, matching legacy.
     */
    private String mapBankCode(AccountInfo accountInfo) {
        String raw = isPresent(accountInfo.localPartCode()) ? accountInfo.localPartCode() : accountInfo.bankCode();
        if (raw == null) {
            return "";
        }
        return raw.endsWith("XXX") ? raw.substring(0, raw.length() - 3) : raw;
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank() && !"null".equals(value);
    }

    private String firstOrNull(List<String> values) {
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }
}
