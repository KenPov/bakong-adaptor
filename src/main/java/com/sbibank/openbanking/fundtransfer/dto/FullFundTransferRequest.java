package com.sbibank.openbanking.fundtransfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Mirrors the 15 fields read (via {@code $.xxx} json-eval args) by {@code bakong-full-fund-transfer.xml}.
 *
 * <p>{@code paymentEndToEndId} and {@code paymentInstrId} are accepted and required here for parity with
 * the legacy contract, but - matching legacy exactly - are NOT used when building the outbound ISO 20022
 * message: {@link com.sbibank.openbanking.fundtransfer.service.Pain001MessageBuilder} derives
 * {@code EndToEndId}/{@code InstrId} independently from other fields. Not fixed here since wiring them in
 * would change the wire contract already agreed with CIFTP.
 */
public record FullFundTransferRequest(
        @NotBlank String createDateTime,
        @NotBlank String requestExactDate,
        @NotBlank String requestMsgId,
        @NotNull @Positive BigDecimal txnAmount,
        @NotBlank String receiverBankCode,
        @NotBlank String debitAccountNo,
        @NotBlank String txnCurrency,
        @NotBlank String creditAccountName,
        @NotBlank String paymentEndToEndId,
        @NotBlank String creditAccountNo,
        @NotBlank String debitAccountName,
        @NotBlank String extRefId,
        @NotBlank String paymentInstrId,
        @NotBlank String paymentInfoId,
        @NotBlank String txnDescription) {}
