package com.sbibank.openbanking.accountinquiry.client;

import com.sbibank.openbanking.accountinquiry.dto.AccountInquiryUpstreamResponse;

public interface BakongObClient {

    /**
     * Logs into the Bakong OB client hub and returns the bearer token (legacy: {@code bakong_id_token}).
     */
    String authenticate();

    /**
     * Asks the Bakong OB hub to inquire the given account number at the given participant bank.
     */
    AccountInquiryUpstreamResponse inquireAccount(String accountNumber, String participantCode, String bearerToken);
}
