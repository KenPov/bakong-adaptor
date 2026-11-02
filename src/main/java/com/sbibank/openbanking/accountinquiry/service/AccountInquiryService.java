package com.sbibank.openbanking.accountinquiry.service;

import com.sbibank.openbanking.accountinquiry.client.BakongObClient;
import com.sbibank.openbanking.accountinquiry.dto.AccountInquiryApiResponse;
import com.sbibank.openbanking.accountinquiry.dto.AccountInquiryUpstreamResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AccountInquiryService {

    private static final Logger log = LoggerFactory.getLogger(AccountInquiryService.class);

    private final BakongObClient bakongObClient;
    private final AccountInquiryResponseMapper mapper;

    public AccountInquiryService(BakongObClient bakongObClient, AccountInquiryResponseMapper mapper) {
        this.bakongObClient = bakongObClient;
        this.mapper = mapper;
    }

    /**
     * Mirrors the legacy {@code faultSequence}: never lets an exception escape to the controller, always
     * returns a well-formed envelope.
     */
    public AccountInquiryApiResponse inquire(String accountNumber, String participantCode) {
        long startTime = System.currentTimeMillis();
        String maskedAccount = maskAccount(accountNumber);
        log.info("account-inquiry START: accountNumber={} participantCode={}", maskedAccount, participantCode);

        try {
            String token = bakongObClient.authenticate();
            AccountInquiryUpstreamResponse upstream = bakongObClient.inquireAccount(accountNumber, participantCode, token);
            AccountInquiryApiResponse response = mapper.toApiResponse(upstream);
            long durationMs = System.currentTimeMillis() - startTime;

            if (response.status() != null && response.status().code() == 0) {
                String accountName = response.data() != null && response.data().accountData() != null
                        ? response.data().accountData().accountName()
                        : "N/A";
                log.info("account-inquiry SUCCESS: accountNumber={} participantCode={} accountName=\"{}\" durationMs={}",
                        maskedAccount, participantCode, accountName, durationMs);
            } else {
                Object errorCode = response.status() != null ? response.status().errorCode() : "N/A";
                String errorMsg = response.status() != null ? response.status().error() : "N/A";
                log.warn("account-inquiry REJECTED: accountNumber={} participantCode={} code={} errorCode={} error=\"{}\" durationMs={}",
                        maskedAccount, participantCode,
                        response.status() != null ? response.status().code() : -1,
                        errorCode, errorMsg, durationMs);
            }

            return response;
        } catch (Exception ex) {
            long durationMs = System.currentTimeMillis() - startTime;
            log.error("account-inquiry FAILED: accountNumber={} participantCode={} durationMs={} error=\"{}\"",
                    maskedAccount, participantCode, durationMs, ex.getMessage(), ex);
            return AccountInquiryResponseMapper.INTERNAL_ERROR_RESPONSE;
        }
    }

    private String maskAccount(String acc) {
        if (acc == null || acc.length() <= 8) {
            return acc;
        }
        return acc.substring(0, 4) + "****" + acc.substring(acc.length() - 4);
    }
}
