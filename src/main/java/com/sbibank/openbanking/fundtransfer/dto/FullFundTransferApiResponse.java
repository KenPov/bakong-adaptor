package com.sbibank.openbanking.fundtransfer.dto;

public record FullFundTransferApiResponse(FullFundTransferStatus status, FullFundTransferData data) {

    public static FullFundTransferApiResponse success(FullFundTransferData data) {
        return new FullFundTransferApiResponse(FullFundTransferStatus.success(), data);
    }

    public static FullFundTransferApiResponse error(String errorMessage) {
        return new FullFundTransferApiResponse(FullFundTransferStatus.error(errorMessage), null);
    }
}
