package com.sbibank.openbanking.fundtransfer.dto;

/**
 * {@code hash} is legacy naming for the SOAP response's {@code <Ustrd>} element (the original transaction
 * remittance/reference text echoed back by CIFTP) - it is not a cryptographic hash. Kept as-is to match the
 * existing field name consumers already expect.
 */
public record FullFundTransferData(String requestMsgId, String paymentInfoId, String hash) {}
