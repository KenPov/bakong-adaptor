package com.sbibank.openbanking.fundtransfer.service;

import com.sbibank.openbanking.config.CiftpProperties;
import com.sbibank.openbanking.config.OurBankProperties;
import com.sbibank.openbanking.fundtransfer.client.CiftpSoapClient;
import com.sbibank.openbanking.fundtransfer.dto.FullFundTransferApiResponse;
import com.sbibank.openbanking.fundtransfer.dto.FullFundTransferRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FullFundTransferService {

    private static final Logger log = LoggerFactory.getLogger(FullFundTransferService.class);

    private final Pain001MessageBuilder pain001MessageBuilder;
    private final SoapEnvelopeBuilder soapEnvelopeBuilder;
    private final SoapResponseParser soapResponseParser;
    private final CiftpSoapClient ciftpSoapClient;
    private final CiftpProperties ciftpProperties;
    private final OurBankProperties ourBankProperties;

    public FullFundTransferService(
            Pain001MessageBuilder pain001MessageBuilder,
            SoapEnvelopeBuilder soapEnvelopeBuilder,
            SoapResponseParser soapResponseParser,
            CiftpSoapClient ciftpSoapClient,
            CiftpProperties ciftpProperties,
            OurBankProperties ourBankProperties) {
        this.pain001MessageBuilder = pain001MessageBuilder;
        this.soapEnvelopeBuilder = soapEnvelopeBuilder;
        this.soapResponseParser = soapResponseParser;
        this.ciftpSoapClient = ciftpSoapClient;
        this.ciftpProperties = ciftpProperties;
        this.ourBankProperties = ourBankProperties;
    }

    /**
     * Never lets an exception escape to the controller. The legacy {@code faultSequence} for this flow was
     * empty (an unhandled exception would have fallen through to WSO2's default 500 handling); this adds a
     * clean fallback envelope instead, a deliberate small improvement over legacy rather than a strict port.
     */
    public FullFundTransferApiResponse transfer(FullFundTransferRequest request) {
        long startTime = System.currentTimeMillis();
        String maskedDebit = maskAccount(request.debitAccountNo());
        String maskedCredit = maskAccount(request.creditAccountNo());

        log.info(
                "bakong-full-fund-transfer START: requestMsgId={} extRefId={} fromAccount={} toAccount={} amount={} {} receiverBank={}",
                request.requestMsgId(),
                request.extRefId(),
                maskedDebit,
                maskedCredit,
                request.txnAmount(),
                request.txnCurrency(),
                request.receiverBankCode());

        try {
            String isoMessage = pain001MessageBuilder.build(request, ourBankProperties);
            String soapEnvelope = soapEnvelopeBuilder.buildMakeFullFundTransfer(request, ciftpProperties, isoMessage);
            String rawResponse = ciftpSoapClient.makeFullFundTransfer(soapEnvelope);
            FullFundTransferApiResponse response = soapResponseParser.parse(rawResponse);
            long durationMs = System.currentTimeMillis() - startTime;

            if (response.status() != null && response.status().code() == 0) {
                String paymentInfoId = response.data() != null ? response.data().paymentInfoId() : request.paymentInfoId();
                log.info(
                        "bakong-full-fund-transfer SUCCESS: requestMsgId={} extRefId={} paymentInfoId={} durationMs={}",
                        request.requestMsgId(),
                        request.extRefId(),
                        paymentInfoId,
                        durationMs);
            } else {
                String errorMsg = response.status() != null ? response.status().errorMessage() : "Unknown error";
                log.warn(
                        "bakong-full-fund-transfer REJECTED: requestMsgId={} extRefId={} errorMessage=\"{}\" durationMs={}",
                        request.requestMsgId(),
                        request.extRefId(),
                        errorMsg,
                        durationMs);
            }

            return response;
        } catch (Exception ex) {
            long durationMs = System.currentTimeMillis() - startTime;
            log.error(
                    "bakong-full-fund-transfer FAILED: requestMsgId={} extRefId={} durationMs={} error=\"{}\"",
                    request.requestMsgId(),
                    request.extRefId(),
                    durationMs,
                    ex.getMessage(),
                    ex);
            return FullFundTransferApiResponse.error("Internal Server Error");
        }
    }

    private String maskAccount(String acc) {
        if (acc == null || acc.length() <= 8) {
            return acc;
        }
        return acc.substring(0, 4) + "****" + acc.substring(acc.length() - 4);
    }
}
